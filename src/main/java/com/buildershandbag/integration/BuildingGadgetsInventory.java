package com.buildershandbag.integration;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import com.direwolf20.buildinggadgets.common.integration.IItemAccess;
import com.direwolf20.buildinggadgets.common.tools.InventoryManipulation;

import com.buildershandbag.storage.HandbagOutputSupply;
import com.buildershandbag.storage.HandbagOutputs;


/**
 * Player-aware access to the combined supply of carried handbags.
 */
public final class BuildingGadgetsInventory implements IItemAccess {

    public BuildingGadgetsInventory() {
    }

    public static int countItems(IItemHandler inventory, ItemStack requested, EntityPlayer player) {
        if (inventory == null) return 0;
        if (inventory instanceof IItemAccess) {
            return ((IItemAccess) inventory).getItemsForExtraction(requested, player);
        }
        if (!HandbagOutputs.requiresExactMatch(requested)) {
            return InventoryManipulation.countInContainer(inventory, requested.getItem(), requested.getMetadata());
        }

        long count = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack available = inventory.getStackInSlot(slot);
            if (HandbagOutputs.sameOutput(available, requested)) count += available.getCount();
        }

        return (int) Math.min(Integer.MAX_VALUE, count);
    }

    public static int extractExact(IItemHandler inventory, ItemStack requested, int amount) {
        if (inventory == null || amount <= 0) return 0;

        int extracted = 0;
        for (int slot = 0; slot < inventory.getSlots() && extracted < amount; slot++) {
            if (!HandbagOutputs.sameOutput(inventory.getStackInSlot(slot), requested)) continue;

            ItemStack available = inventory.extractItem(slot, amount - extracted, true);
            if (!HandbagOutputs.sameOutput(available, requested)) continue;

            extracted += inventory.extractItem(slot, available.getCount(), false).getCount();
        }

        return extracted;
    }

    @Override
    public int getItemsForExtraction(ItemStack requested, EntityPlayer player) {
        return HandbagOutputSupply.count(player, requested);
    }

    @Override
    public int extractItems(ItemStack requested, int amount, EntityPlayer player) {
        return HandbagOutputSupply.extract(player, requested, amount);
    }

    @Override
    public int getSlots() {
        return 0;
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 0;
    }
}
