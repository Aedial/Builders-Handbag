package com.buildershandbag.storage;

import java.util.function.IntConsumer;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;


/**
 * Shared material extraction for player inventories and carried bags.
 */
public final class MaterialInventoryHelper {

    private MaterialInventoryHelper() {
    }

    public static ItemStack extractMaterial(IItemHandler inventory, ItemStack material, int amount) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (!HandbagStorage.sameMaterial(material, inventory.getStackInSlot(slot))) continue;

            ItemStack available = inventory.extractItem(slot, amount, true);
            if (!HandbagStorage.sameMaterial(material, available)) continue;

            ItemStack extracted = inventory.extractItem(slot, available.getCount(), false);
            if (!extracted.isEmpty()) return extracted;
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack extractFromBags(IItemHandler inventory, ItemStack material, int amount,
            IntConsumer onChanged) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack bag = inventory.getStackInSlot(slot);
            if (bag.isEmpty()) continue;

            IItemHandler contents = bag.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (contents == null) continue;

            ItemStack extracted = extractMaterial(contents, material, amount);
            if (extracted.isEmpty()) continue;

            onChanged.accept(slot);
            return extracted;
        }

        return ItemStack.EMPTY;
    }
}
