package com.buildershandbag.integration;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;

import com.buildershandbag.storage.MaterialInventoryHelper;


/**
 * Material refilling from bags equipped in Baubles slots.
 */
public final class BaublesIntegration {

    private BaublesIntegration() {
    }

    public static ItemStack extractMaterial(EntityPlayer player, ItemStack material, int amount) {
        return extractMaterial(player, material, amount, false);
    }

    public static ItemStack extractMaterial(EntityPlayer player, ItemStack material, int amount, boolean simulate) {
        IBaublesItemHandler inventory = BaublesApi.getBaublesHandler(player);
        return MaterialInventoryHelper.extractFromBags(
            inventory, material, amount, simulate, slot -> inventory.setChanged(slot, true));
    }
}
