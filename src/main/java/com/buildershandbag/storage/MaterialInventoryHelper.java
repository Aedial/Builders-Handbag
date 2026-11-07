package com.buildershandbag.storage;

import java.util.function.IntConsumer;
import java.util.function.Predicate;

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
        int extractedCount = extractMatching(inventory,
            stack -> HandbagStorage.sameMaterial(material, stack), amount, simulate);
        return withCount(material, extractedCount);
    }

    public static int extractMatching(IItemHandler inventory, Predicate<ItemStack> matches,
            int amount, boolean simulate) {
        int extractedCount = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (extractedCount >= amount) break;
            if (!matches.test(inventory.getStackInSlot(slot))) continue;

            ItemStack available = inventory.extractItem(slot, amount - extractedCount, true);
            if (!matches.test(available)) continue;

            ItemStack extracted = simulate ? available : inventory.extractItem(slot, available.getCount(), false);
            if (matches.test(extracted)) extractedCount += extracted.getCount();
        }

        return extractedCount;
    }

    public static ItemStack extractFromBags(IItemHandler inventory, ItemStack material, int amount,
            IntConsumer onChanged) {
        return extractFromBags(inventory, material, amount, false, onChanged);
    }

    public static ItemStack extractFromBags(IItemHandler inventory, ItemStack material, int amount,
            boolean simulate, IntConsumer onChanged) {
        int extractedCount = extractMatchingFromBags(inventory,
            stack -> HandbagStorage.sameMaterial(material, stack), amount, simulate, onChanged);
        return withCount(material, extractedCount);
    }

    public static int extractMatchingFromBags(IItemHandler inventory, Predicate<ItemStack> matches, int amount,
            boolean simulate, IntConsumer onChanged) {
        int extractedCount = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (extractedCount >= amount) break;

            ItemStack bag = inventory.getStackInSlot(slot);
            if (bag.isEmpty() || bag.getItem() instanceof ItemHandbag) continue;

            IItemHandler contents = bag.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (contents == null) continue;

            int extracted = extractMatching(contents, matches, amount - extractedCount, simulate);
            if (extracted <= 0) continue;

            if (!simulate) onChanged.accept(slot);
            extractedCount += extracted;
        }

        return extractedCount;
    }

    private static ItemStack withCount(ItemStack material, int extractedCount) {
        if (extractedCount <= 0) return ItemStack.EMPTY;

        ItemStack extracted = material.copy();
        extracted.setCount(extractedCount);
        return extracted;
    }
}
