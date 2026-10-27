package com.buildershandbag.mixin.bbw;

import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import portablejim.bbw.containers.ContainerManager;
import portablejim.bbw.shims.BasicPlayerShim;

import com.buildershandbag.item.ItemHandbag;
import com.buildershandbag.integration.BbwHandbagSupply;
import com.buildershandbag.storage.HandbagOutputs;


@Mixin(value = BasicPlayerShim.class, remap = false)
public abstract class MixinBasicPlayerShim {

    @Shadow
    private EntityPlayer player;

    @Redirect(method = {"countItems", "useItem"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/item/ItemStack;isItemEqual(Lnet/minecraft/item/ItemStack;)Z", remap = true))
    private boolean buildershandbag$matchConfiguredOutput(ItemStack requested, ItemStack available) {
        return HandbagOutputs.requiresExactMatch(requested)
            ? HandbagOutputs.sameOutput(requested, available) : requested.isItemEqual(available);
    }

    @Inject(method = "useItem", at = @At("RETURN"), cancellable = true)
    private void buildershandbag$returnOutputStack(ItemStack requested, CallbackInfoReturnable<ItemStack> callback) {
        ItemStack supplied = callback.getReturnValue();
        if (supplied != null && supplied.getItem() instanceof ItemHandbag) callback.setReturnValue(requested.copy());
    }

    @Inject(method = "useItem", at = @At("TAIL"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private void buildershandbag$useOffhand(ItemStack requested, CallbackInfoReturnable<ItemStack> callback,
            ContainerManager manager, Map<?, ?> state, int remaining, List<ItemStack> providers) {
        if (BbwHandbagSupply.extractOffhand(player, requested, remaining)) callback.setReturnValue(requested.copy());
    }
}
