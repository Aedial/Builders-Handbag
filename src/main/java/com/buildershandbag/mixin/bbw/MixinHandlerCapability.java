package com.buildershandbag.mixin.bbw;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import portablejim.bbw.containers.handlers.HandlerCapability;

import com.buildershandbag.item.ItemHandbag;
import com.buildershandbag.storage.HandbagOutputSupply;
import com.buildershandbag.storage.HandbagOutputs;


@Mixin(value = HandlerCapability.class, remap = false)
public abstract class MixinHandlerCapability {

    @Redirect(method = {"countItems", "useItems"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/item/ItemStack;isItemEqual(Lnet/minecraft/item/ItemStack;)Z", remap = true))
    private boolean buildershandbag$matchConfiguredOutput(ItemStack first, ItemStack second) {
        return HandbagOutputs.requiresExactMatch(first) || HandbagOutputs.requiresExactMatch(second)
            ? HandbagOutputs.sameOutput(first, second) : first.isItemEqual(second);
    }

    @Inject(method = "countItems", at = @At("HEAD"), cancellable = true)
    private void buildershandbag$countOutputs(EntityPlayer player, ItemStack requested, ItemStack inventoryStack,
            CallbackInfoReturnable<Integer> callback) {
        if (!(inventoryStack.getItem() instanceof ItemHandbag)) return;

        callback.setReturnValue(HandbagOutputSupply.isFirstHandbag(player, inventoryStack)
            ? HandbagOutputSupply.count(player, requested) : 0);
    }

    @Inject(method = "useItems", at = @At("HEAD"), cancellable = true)
    private void buildershandbag$extractOutputs(EntityPlayer player, ItemStack requested, ItemStack inventoryStack,
            int count, CallbackInfoReturnable<Integer> callback) {
        if (!(inventoryStack.getItem() instanceof ItemHandbag)) return;

        int extracted = HandbagOutputSupply.isFirstHandbag(player, inventoryStack)
            ? HandbagOutputSupply.extract(player, requested, count) : 0;
        callback.setReturnValue(count - extracted);
    }
}
