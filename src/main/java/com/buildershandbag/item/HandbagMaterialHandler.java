package com.buildershandbag.item;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import com.buildershandbag.storage.HandbagConfiguration;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Input handler for inserting into the handbag when placed. Each virtual slot
 * only accepts its configured material.
 */
public final class HandbagMaterialHandler implements IItemHandler {

    private final ItemStack handbag;

    public HandbagMaterialHandler(ItemStack handbag) {
        this.handbag = handbag;
    }

    @Override
    public int getSlots() {
        return HandbagStorage.CONFIGURATION_COUNT;
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        HandbagConfiguration configuration = HandbagStorage.getConfiguration(handbag, slot);
        if (configuration == null || configuration.getMaterialCount() <= 0) return ItemStack.EMPTY;

        ItemStack displayed = configuration.getMaterial();
        displayed.setCount(Math.min(displayed.getMaxStackSize(), configuration.getMaterialCount()));
        return displayed;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (slot < 0 || slot >= getSlots() || stack.isEmpty()) return stack;

        int inserted = HandbagStorage.insertMaterial(handbag, slot, stack, simulate);
        if (inserted <= 0) return stack;

        ItemStack remainder = stack.copy();
        remainder.shrink(inserted);
        return remainder;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        // TODO: Make the limit configurable?
        //       Maybe multiple tiers of handbags with different capacities?
        //       Should only really matter if the player has no AE2 access.
        return HandbagStorage.MATERIAL_CAPACITY;
    }
}
