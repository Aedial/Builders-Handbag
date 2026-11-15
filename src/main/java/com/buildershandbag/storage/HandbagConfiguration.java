package com.buildershandbag.storage;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.buildershandbag.integration.HandbagIntegration;
import com.buildershandbag.integration.FutureMcRecipeCache;


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
    private final MaterialAmount materialAmount;
    private final int outputCount;

    public HandbagConfiguration(ItemStack material, ItemStack result, HandbagIntegration integration, int materialCount) {
        this(material, result, integration, materialCount, 1);
    }

    public HandbagConfiguration(ItemStack material, ItemStack result, HandbagIntegration integration,
            int materialCount, int outputCount) {
        this(copySingle(material).writeToNBT(new NBTTagCompound()),
            copySingle(result).writeToNBT(new NBTTagCompound()), integration,
            new MaterialAmount(Math.max(0, materialCount), 1), outputCount);
    }

    public HandbagConfiguration(NBTTagCompound materialData, NBTTagCompound resultData,
            HandbagIntegration integration, int materialCount) {
        this(materialData, resultData, integration, new MaterialAmount(Math.max(0, materialCount), 1));
    }

    public HandbagConfiguration(NBTTagCompound materialData, NBTTagCompound resultData,
            HandbagIntegration integration, MaterialAmount materialAmount) {
        this(materialData, resultData, integration, materialAmount, 1);
    }

    public HandbagConfiguration(NBTTagCompound materialData, NBTTagCompound resultData,
            HandbagIntegration integration, MaterialAmount materialAmount, int outputCount) {
        this.outputCount = Math.max(1, outputCount);
        this.materialData = materialData.copy();
        this.resultData = resultData.copy();
        this.material = copySingle(new ItemStack(materialData.copy()));
        this.result = copySingle(new ItemStack(resultData.copy()));
        this.integration = integration;
        this.materialAmount = materialAmount;
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
        return !material.isEmpty() && !result.isEmpty() && integration.isEnabled()
            && getRecipeStatus() == RecipeStatus.AVAILABLE;
    }

    public HandbagIntegration getIntegration() {
        return integration;
    }

    public float getMaterialCount() {
        return materialAmount.getCount();
    }

    public MaterialAmount getMaterialAmount() {
        return materialAmount;
    }

    public MaterialAmount getMaterialCost() {
        return integration == HandbagIntegration.FUTUREMC
            ? new MaterialAmount(1, outputCount) : integration.getMaterialCost(getMaterial(), getResult());
    }

    public int getRecipeOutputCount() {
        return outputCount;
    }

    public RecipeStatus getRecipeStatus() {
        if (integration != HandbagIntegration.FUTUREMC) return RecipeStatus.AVAILABLE;

        int current = FutureMcRecipeCache.getOutputCount(material, result);
        if (current == -1) return RecipeStatus.PENDING;
        if (current == 0) return RecipeStatus.MISSING;

        return current == outputCount ? RecipeStatus.AVAILABLE : RecipeStatus.CHANGED;
    }

    public HandbagConfiguration withRecipeOutputCount(int count) {
        MaterialAmount balance = materialAmount.roundDown(count);
        return new HandbagConfiguration(materialData, resultData, integration, balance, count);
    }

    public int getOutputCount() {
        return materialAmount.getOutputCount(getMaterialCost());
    }

    public HandbagConfiguration withMaterialCount(int count) {
        return withMaterialAmount(new MaterialAmount(Math.max(0, count), 1));
    }

    public HandbagConfiguration withMaterialAmount(MaterialAmount amount) {
        return new HandbagConfiguration(materialData, resultData, integration, amount, outputCount);
    }

    public enum RecipeStatus {
        AVAILABLE,
        PENDING,
        MISSING,
        CHANGED
    }

    private static ItemStack copySingle(ItemStack stack) {
        ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
        if (!copy.isEmpty()) copy.setCount(1);
        return copy;
    }
}
