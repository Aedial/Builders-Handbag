package com.buildershandbag.item;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import com.buildershandbag.storage.HandbagConfiguration;
import com.buildershandbag.storage.HandbagOutputs;
import com.buildershandbag.storage.HandbagOutputSupply;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Material input slots followed by extractable configured output slots.
 */
public final class HandbagItemHandler implements IItemHandler {

    private final ItemStack handbag;
    private final HandbagMaterialHandler input;
    @Nullable
    private EntityPlayer player;

    public HandbagItemHandler(ItemStack handbag) {
        this.handbag = handbag;
        this.input = new HandbagMaterialHandler(handbag);
    }

    public void setPlayer(@Nullable EntityPlayer player) {
        this.player = player;
    }

    public IItemHandler getInput() {
        return input;
    }

    @Nullable
    private EntityPlayer getPlayer() {
        if (player != null && !HandbagOutputSupply.getHandbags(player).contains(handbag)) player = null;

        return player;
    }

    @Override
    public int getSlots() {
        return input.getSlots() * 2;
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
        if (slot < input.getSlots()) return ItemStack.EMPTY;

        int index = slot - input.getSlots();
        HandbagConfiguration configuration = HandbagStorage.getConfiguration(handbag, index);
        if (configuration == null || !configuration.isAvailable()) return ItemStack.EMPTY;

        int count = HandbagOutputSupply.extractConfiguration(
            getPlayer(), handbag, index, HandbagStorage.MATERIAL_CAPACITY, true);
        if (count <= 0) return ItemStack.EMPTY;

        ItemStack result = HandbagOutputs.getResult(configuration);
        result.setCount(Math.min(count, result.getMaxStackSize()));
        return result;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return slot >= 0 && slot < input.getSlots() ? input.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < input.getSlots() || slot >= getSlots() || amount <= 0) return ItemStack.EMPTY;

        int index = slot - input.getSlots();
        HandbagConfiguration configuration = HandbagStorage.getConfiguration(handbag, index);
        if (configuration == null || !configuration.isAvailable()) return ItemStack.EMPTY;

        ItemStack result = HandbagOutputs.getResult(configuration);
        int extracted = HandbagOutputSupply.extractConfiguration(
            getPlayer(), handbag, index, Math.min(amount, result.getMaxStackSize()), simulate);
        if (extracted <= 0) return ItemStack.EMPTY;

        result.setCount(extracted);
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return input.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        if (slot < 0 || slot >= input.getSlots()) return false;

        HandbagConfiguration configuration = HandbagStorage.getConfiguration(handbag, slot);
        return configuration != null && HandbagStorage.sameMaterial(configuration.getMaterial(), stack);
    }
}
