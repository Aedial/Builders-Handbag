package com.buildershandbag.storage;

import java.util.function.IntConsumer;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import com.buildershandbag.item.ItemHandbag;


/**
 * Shared material extraction for player inventories and carried bags.
 */
public final class MaterialInventoryHelper {

    private MaterialInventoryHelper() {
    }

    public static ItemStack extractMaterial(IItemHandler inventory, ItemStack material, int amount) {
        return extractMaterial(inventory, material, amount, false);
    }

    public static ItemStack extractMaterial(IItemHandler inventory, ItemStack material, int amount, boolean simulate) {
        int extractedCount = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (extractedCount >= amount) break;
            if (!HandbagStorage.sameMaterial(material, inventory.getStackInSlot(slot))) continue;

            ItemStack available = inventory.extractItem(slot, amount - extractedCount, true);
            if (!HandbagStorage.sameMaterial(material, available)) continue;

            ItemStack extracted = simulate ? available : inventory.extractItem(slot, available.getCount(), false);
            if (HandbagStorage.sameMaterial(material, extracted)) extractedCount += extracted.getCount();
        }

        if (extractedCount <= 0) return ItemStack.EMPTY;

        ItemStack extracted = material.copy();
        extracted.setCount(extractedCount);
        return extracted;
    }

    public static ItemStack extractFromBags(IItemHandler inventory, ItemStack material, int amount,
            IntConsumer onChanged) {
        return extractFromBags(inventory, material, amount, false, onChanged);
    }

    public static ItemStack extractFromBags(IItemHandler inventory, ItemStack material, int amount,
            boolean simulate, IntConsumer onChanged) {
        int extractedCount = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (extractedCount >= amount) break;

            ItemStack bag = inventory.getStackInSlot(slot);
            if (bag.isEmpty() || bag.getItem() instanceof ItemHandbag) continue;

            IItemHandler contents = bag.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (contents == null) continue;

            ItemStack extracted = extractMaterial(contents, material, amount - extractedCount, simulate);
            if (extracted.isEmpty()) continue;

            if (!simulate) onChanged.accept(slot);
            extractedCount += extracted.getCount();
        }

        if (extractedCount <= 0) return ItemStack.EMPTY;

        ItemStack extracted = material.copy();
        extracted.setCount(extractedCount);
        return extracted;
    }
}
