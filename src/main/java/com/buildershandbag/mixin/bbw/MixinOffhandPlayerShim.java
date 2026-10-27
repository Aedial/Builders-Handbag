package com.buildershandbag.mixin.bbw;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;

import portablejim.bbw.shims.BasicPlayerShim;

import com.buildershandbag.item.ItemHandbag;
import com.buildershandbag.storage.HandbagOutputSupply;


@Mixin(value = BasicPlayerShim.class, remap = false, priority = 1100)
public abstract class MixinOffhandPlayerShim {

    @Shadow
    private EntityPlayer player;

    @ModifyVariable(method = "countItems", ordinal = 0,
        slice = @Slice(from = @At(value = "INVOKE",
            target = "Lportablejim/bbw/containers/ContainerManager;finalCount(Ljava/util/Map;)I",
            shift = At.Shift.AFTER)),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getCount()I", ordinal = 0, remap = true))
    private int buildershandbag$countOffhand(int total, ItemStack requested) {
        ItemStack offhand = player.getHeldItemOffhand();
        if (!(offhand.getItem() instanceof ItemHandbag) || !HandbagOutputSupply.isFirstHandbag(player, offhand)) {
            return total;
        }

        long combined = (long) total + HandbagOutputSupply.count(player, requested);
        return (int) Math.min(Integer.MAX_VALUE, combined);
    }
}
