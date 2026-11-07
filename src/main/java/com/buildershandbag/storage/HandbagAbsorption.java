package com.buildershandbag.storage;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.buildershandbag.item.ItemHandbag;


/**
 * Ordered absorption of exact materials and configured results across carried handbags.
 */
public final class HandbagAbsorption {

    private HandbagAbsorption() {
    }

    public static int absorb(List<ItemStack> handbags, ItemStack stack, boolean includeMaterial) {
        if (handbags.isEmpty() || stack.isEmpty() || stack.getItem() instanceof ItemHandbag) return 0;

        HandbagStackKey material = includeMaterial ? HandbagStackKey.probeMaterial(stack) : null;
        HandbagStackKey result = HandbagStackKey.probeOutput(stack);
        int accepted = 0;
        for (ItemStack handbag : handbags) {
            HandbagMatchCache cache = HandbagMatchCache.get(handbag);
            if (cache == null) continue;

            List<Integer> materials = includeMaterial ? cache.getMaterials(material) : Collections.emptyList();
            List<Integer> results = cache.getResults(result);
            int materialIndex = 0;
            int resultIndex = 0;
            while (materialIndex < materials.size() || resultIndex < results.size()) {
                if (accepted >= stack.getCount()) break;

                int source = materialIndex < materials.size() ? materials.get(materialIndex) : Integer.MAX_VALUE;
                int output = resultIndex < results.size() ? results.get(resultIndex) : Integer.MAX_VALUE;
                int index = Math.min(source, output);
                if (source == index) materialIndex++;
                if (output == index) resultIndex++;

                MaterialAmount amount = cache.getAmount(index, material, result, includeMaterial);
                accepted += HandbagStorage.insertMaterial(
                    handbag, index, stack.getCount() - accepted, amount, false);
            }
            if (accepted == stack.getCount()) break;
        }

        return accepted;
    }

    public static void sync(EntityPlayer player) {
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
    }
}
