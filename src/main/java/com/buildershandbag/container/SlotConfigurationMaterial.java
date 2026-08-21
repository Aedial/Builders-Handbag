package com.buildershandbag.container;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.buildershandbag.integration.HandbagConfigurationProvider;

import javax.annotation.Nonnull;


public class SlotConfigurationMaterial extends Slot {

    public SlotConfigurationMaterial(IInventory inventory, int slotIndex, int xPosition, int yPosition) {
        super(inventory, slotIndex, xPosition, yPosition);
    }

    @Override
    public boolean isItemValid(@Nonnull ItemStack stack) {
        return HandbagConfigurationProvider.isConfigurationMaterial(stack);
    }

    @Override
    public int getSlotStackLimit() {
        return 64;
    }
}
