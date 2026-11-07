package com.buildershandbag.item;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import com.buildershandbag.storage.HandbagMatchCache;
import com.buildershandbag.storage.HandbagStackKey;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Input handler for inserting into the handbag. Each virtual slot
 * only accepts its configured material and result.
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
        return ItemStack.EMPTY;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (slot < 0 || slot >= getSlots() || stack.isEmpty() || stack.getItem() instanceof ItemHandbag) return stack;

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
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        if (slot < 0 || slot >= getSlots() || stack.isEmpty() || stack.getItem() instanceof ItemHandbag) return false;

        HandbagMatchCache cache = HandbagMatchCache.get(handbag);
        return cache != null && cache.getAmount(slot, HandbagStackKey.probeMaterial(stack),
            HandbagStackKey.probeOutput(stack), true) != null;
    }

    @Override
    public int getSlotLimit(int slot) {
        // TODO: Make the limit configurable?
        //       Maybe multiple tiers of handbags with different capacities?
        //       Should only really matter if the player has no AE2 access.
        return HandbagStorage.MATERIAL_CAPACITY;
    }
}
