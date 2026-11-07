package com.buildershandbag.storage;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import com.buildershandbag.integration.HandbagIntegration;


/**
 * Immutable snapshots and transient probes for count-independent material and output lookups.
 * Probes borrow their stack while the caller leaves its contents unchanged.
 */
public final class HandbagStackKey {

    private final ItemStack stack;
    private final int hash;

    private HandbagStackKey(ItemStack stack) {
        this.stack = stack;
        NBTTagCompound tag = stack.getTagCompound();
        int itemHash = 31 * stack.getItem().hashCode() + stack.getMetadata();
        hash = 31 * itemHash + (tag == null ? 0 : tag.hashCode());
    }

    public static HandbagStackKey material(ItemStack stack) {
        return new HandbagStackKey(stack.copy());
    }

    public static HandbagStackKey probeMaterial(ItemStack stack) {
        return new HandbagStackKey(stack);
    }

    public static HandbagStackKey probeOutput(ItemStack stack) {
        ResourceLocation name = stack.getItem().getRegistryName();
        NBTTagCompound tag = stack.getTagCompound();
        if (name != null && name.getNamespace().equals(HandbagIntegration.BLOCKCRAFTERY_MODID)) {
            return stack.getSubCompound("BlockEntityTag") == null ? new HandbagStackKey(stack) : output(stack);
        }

        if (tag != null && (tag.isEmpty() || tag.hasKey("BlockEntityTag")
                || name != null && name.getNamespace().equals(HandbagIntegration.ARCHITECTURECRAFT_MODID)
                    && tag.hasKey("Disconnected"))) return output(stack);

        return new HandbagStackKey(stack);
    }

    public static HandbagStackKey output(ItemStack stack) {
        ResourceLocation name = stack.getItem().getRegistryName();
        if (name == null || !name.getNamespace().equals(HandbagIntegration.BLOCKCRAFTERY_MODID)) {
            return new HandbagStackKey(HandbagOutputs.withoutTile(stack));
        }

        ItemStack copy = stack.copy();
        NBTTagCompound tile = copy.getSubCompound("BlockEntityTag");
        if (tile != null) {
            ItemStack material = HandbagOutputs.getBlockcrafteryMaterial(tile);
            if (!material.isEmpty()) material.setCount(1);

            copy.setTagCompound(material.writeToNBT(new NBTTagCompound()));
        }

        return new HandbagStackKey(copy);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof HandbagStackKey)) return false;

        return HandbagStorage.sameMaterial(stack, ((HandbagStackKey) other).stack);
    }
}
