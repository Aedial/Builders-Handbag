package com.buildershandbag.integration;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.buildershandbag.item.ItemHandbag;
import com.buildershandbag.storage.HandbagOutputSupply;


/**
 * Off-hand handbag supply for Better Builders Wands.
 */
public final class BbwHandbagSupply {

    private BbwHandbagSupply() {
    }

    public static boolean extractOffhand(EntityPlayer player, ItemStack requested, int amount) {
        if (amount <= 0) return false;
        ItemStack offhand = player.getHeldItemOffhand();
        if (!(offhand.getItem() instanceof ItemHandbag) || !HandbagOutputSupply.isFirstHandbag(player, offhand)) {
            return false;
        }
        if (HandbagOutputSupply.count(player, requested) < amount) return false;

        return HandbagOutputSupply.extract(player, requested, amount) == amount;
    }
}
