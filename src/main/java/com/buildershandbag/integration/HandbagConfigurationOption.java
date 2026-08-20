package com.buildershandbag.integration;

import net.minecraft.item.ItemStack;

import com.buildershandbag.storage.HandbagConfiguration;


/**
 * The output options offered by the configuration material slot.
 */
public final class HandbagConfigurationOption {

    private final ItemStack result;
    private final HandbagIntegration integration;

    public HandbagConfigurationOption(ItemStack result, HandbagIntegration integration) {
        this.result = result == null ? ItemStack.EMPTY : result.copy();
        if (!this.result.isEmpty()) this.result.setCount(1);
        this.integration = integration;
    }

    public ItemStack getResult() {
        return result.copy();
    }

    public HandbagIntegration getIntegration() {
        return integration;
    }

    public HandbagConfiguration createConfiguration(ItemStack material) {
        return new HandbagConfiguration(material, result, integration, 0);
    }

    public boolean matches(HandbagConfigurationOption other) {
        return other != null
            && integration == other.integration
            && ItemStack.areItemsEqual(result, other.result)
            && ItemStack.areItemStackTagsEqual(result, other.result);
    }
}
