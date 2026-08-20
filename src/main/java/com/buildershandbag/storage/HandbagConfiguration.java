package com.buildershandbag.storage;

import net.minecraft.item.ItemStack;

import com.buildershandbag.integration.HandbagIntegration;


/**
 * Immutable configured output together with the source material stored for it.
 */
public final class HandbagConfiguration {

    private final ItemStack material;
    private final ItemStack result;
    private final HandbagIntegration integration;
    private final int materialCount;

    public HandbagConfiguration(ItemStack material, ItemStack result, HandbagIntegration integration, int materialCount) {
        this.material = copySingle(material);
        this.result = copySingle(result);
        this.integration = integration;
        this.materialCount = Math.max(0, materialCount);
    }

    public ItemStack getMaterial() {
        return material.copy();
    }

    public ItemStack getResult() {
        return result.copy();
    }

    public HandbagIntegration getIntegration() {
        return integration;
    }

    public int getMaterialCount() {
        return materialCount;
    }

    public HandbagConfiguration withMaterialCount(int count) {
        return new HandbagConfiguration(material, result, integration, count);
    }

    private static ItemStack copySingle(ItemStack stack) {
        ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
        if (!copy.isEmpty()) copy.setCount(1);
        return copy;
    }
}
