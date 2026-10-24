package com.buildershandbag.storage;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.buildershandbag.integration.HandbagIntegration;


/**
 * Immutable configured output together with the source material stored for it.
 */
public final class HandbagConfiguration {

    private final ItemStack material;
    private final ItemStack result;
    // Keep serialized stacks intact while their items are unavailable
    private final NBTTagCompound materialData;
    private final NBTTagCompound resultData;
    private final HandbagIntegration integration;
    private final int materialCount;

    public HandbagConfiguration(ItemStack material, ItemStack result, HandbagIntegration integration, int materialCount) {
        this(copySingle(material).writeToNBT(new NBTTagCompound()),
            copySingle(result).writeToNBT(new NBTTagCompound()), integration, materialCount);
    }

    public HandbagConfiguration(NBTTagCompound materialData, NBTTagCompound resultData,
            HandbagIntegration integration, int materialCount) {
        this.materialData = materialData.copy();
        this.resultData = resultData.copy();
        this.material = copySingle(new ItemStack(materialData.copy()));
        this.result = copySingle(new ItemStack(resultData.copy()));
        this.integration = integration;
        this.materialCount = Math.max(0, materialCount);
    }

    public ItemStack getMaterial() {
        return material.copy();
    }

    public ItemStack getResult() {
        return result.copy();
    }

    public NBTTagCompound getMaterialData() {
        return materialData.copy();
    }

    public NBTTagCompound getResultData() {
        return resultData.copy();
    }

    public boolean isAvailable() {
        return !material.isEmpty() && !result.isEmpty() && integration.isEnabled();
    }

    public HandbagIntegration getIntegration() {
        return integration;
    }

    public int getMaterialCount() {
        return materialCount;
    }

    public HandbagConfiguration withMaterialCount(int count) {
        return new HandbagConfiguration(materialData, resultData, integration, count);
    }

    private static ItemStack copySingle(ItemStack stack) {
        ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
        if (!copy.isEmpty()) copy.setCount(1);
        return copy;
    }
}
