package com.buildershandbag.mixin.bbw;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import portablejim.bbw.shims.BasicPlayerShim;

import com.buildershandbag.integration.BbwHandbagSupply;
import com.buildershandbag.storage.HandbagOutputs;


@Mixin(value = BasicPlayerShim.class, remap = false, priority = 1100)
public abstract class MixinFixBasicPlayerShim {

    @Shadow
    private EntityPlayer player;

    @Redirect(method = {"countItems", "useItem"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/item/ItemStack;isItemEqual(Lnet/minecraft/item/ItemStack;)Z", remap = true))
    private boolean buildershandbag$matchConfiguredOutput(ItemStack requested, ItemStack available) {
        return HandbagOutputs.requiresExactMatch(requested)
            ? HandbagOutputs.sameOutput(requested, available) : requested.isItemEqual(available);
    }

    @Redirect(method = {"countItems", "useItem"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/item/ItemStack;areItemStackTagsEqual("
            + "Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z", remap = true))
    private boolean buildershandbag$matchConfiguredTags(ItemStack requested, ItemStack available) {
        return HandbagOutputs.requiresExactMatch(requested)
            ? HandbagOutputs.sameOutput(requested, available) : ItemStack.areItemStackTagsEqual(requested, available);
    }

    @Inject(method = "useItem", cancellable = true, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/entity/player/EntityPlayer;getHeldItemOffhand()Lnet/minecraft/item/ItemStack;",
        remap = true))
    private void buildershandbag$useOffhand(ItemStack requested, CallbackInfoReturnable<ItemStack> callback,
            @Local(ordinal = 0) int remaining) {
        if (BbwHandbagSupply.extractOffhand(player, requested, remaining)) callback.setReturnValue(requested.copy());
    }
}
