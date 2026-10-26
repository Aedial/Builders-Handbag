package com.buildershandbag.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.buildershandbag.container.HandbagLayout;
import com.buildershandbag.integration.HandbagIntegration;


/**
 * Configuration storage for the handbag item. The storage is serialized to the NBT of the ItemStack.
 */
public final class HandbagStorage {

    public static final int CONFIGURATION_COUNT = HandbagLayout.CONFIGURATION_COUNT;
    public static final int MATERIAL_CAPACITY = 64;

    private static final String NBT_ROOT = "Handbag";
    private static final String NBT_CONFIGURATIONS = "Configurations";
    private static final String NBT_SELECTED = "Selected";
    private static final String NBT_MATERIAL = "Material";
    private static final String NBT_RESULT = "Result";
    private static final String NBT_INTEGRATION = "Integration";
    private static final String NBT_MATERIAL_COUNT = "MaterialCount";

    private HandbagStorage() {
    }

    public static List<HandbagConfiguration> getConfigurations(ItemStack handbag) {
        NBTTagCompound root = getRoot(handbag, false);
        if (root == null || !root.hasKey(NBT_CONFIGURATIONS, Constants.NBT.TAG_LIST)) return Collections.emptyList();

        NBTTagList serialized = root.getTagList(NBT_CONFIGURATIONS, Constants.NBT.TAG_COMPOUND);
        List<HandbagConfiguration> configurations = new ArrayList<>();
        for (int index = 0; index < serialized.tagCount() && configurations.size() < CONFIGURATION_COUNT; index++) {
            HandbagConfiguration configuration = readConfiguration(serialized.getCompoundTagAt(index));
            if (configuration != null) configurations.add(configuration);
        }

        return configurations;
    }

    @Nullable
    public static HandbagConfiguration getConfiguration(ItemStack handbag, int index) {
        List<HandbagConfiguration> configurations = getConfigurations(handbag);
        return index >= 0 && index < configurations.size() ? configurations.get(index) : null;
    }

    public static int getSelected(ItemStack handbag) {
        int selected = getSavedSelected(handbag);
        if (selected < 0) return -1;

        HandbagConfiguration configuration = getConfiguration(handbag, selected);
        return configuration != null && configuration.isAvailable() ? selected : -1;
    }

    private static int getSavedSelected(ItemStack handbag) {
        NBTTagCompound root = getRoot(handbag, false);
        if (root == null || !root.hasKey(NBT_SELECTED, Constants.NBT.TAG_INT)) return -1;

        return root.getInteger(NBT_SELECTED);
    }

    /**
     * Reads the selected configuration while deserializing the configuration
     * list at most once. Renderers use this when a handbag stack changes, not
     * once per frame.
     * Retains unavailable entries for the render cache to check current settings.
     */
    @Nullable
    public static HandbagConfiguration getSelectedConfiguration(ItemStack handbag) {
        NBTTagCompound root = getRoot(handbag, false);
        if (root == null || !root.hasKey(NBT_SELECTED, Constants.NBT.TAG_INT)) return null;

        int selected = root.getInteger(NBT_SELECTED);
        if (selected < 0) return null;

        List<HandbagConfiguration> configurations = getConfigurations(handbag);
        return selected < configurations.size() ? configurations.get(selected) : null;
    }

    public static void setSelected(ItemStack handbag, int index) {
        NBTTagCompound root = getRoot(handbag, true);
        if (root == null) return;

        int size = getConfigurations(handbag).size();
        root.setInteger(NBT_SELECTED, index >= 0 && index < size ? index : -1);
    }

    public static boolean addConfiguration(ItemStack handbag, HandbagConfiguration configuration) {
        if (configuration == null || !configuration.isAvailable()) return false;

        List<HandbagConfiguration> configurations = new ArrayList<>(getConfigurations(handbag));
        if (configurations.size() >= CONFIGURATION_COUNT) return false;

        configurations.add(configuration.withMaterialCount(0));
        writeConfigurations(handbag, configurations);

        return true;
    }

    @Nullable
    public static HandbagConfiguration removeConfiguration(ItemStack handbag, int index) {
        List<HandbagConfiguration> configurations = new ArrayList<>(getConfigurations(handbag));
        if (index < 0 || index >= configurations.size()) return null;

        HandbagConfiguration removed = configurations.remove(index);
        int selected = getSavedSelected(handbag);
        if (selected == index) {
            selected = -1;
        } else if (selected > index) {
            selected--;
        }

        writeConfigurations(handbag, configurations);
        setSelected(handbag, selected);

        return removed;
    }

    /**
     * Moves an entry by insertion, preserving the order of the other entries.
     * @return The new index of the moved entry, or -1 if the source index was invalid.
     */
    public static int moveConfiguration(ItemStack handbag, int from, int target) {
        List<HandbagConfiguration> configurations = new ArrayList<>(getConfigurations(handbag));
        if (from < 0 || from >= configurations.size()) return -1;

        int destination = Math.max(0, Math.min(target, configurations.size() - 1));
        if (from == destination) return from;

        int selected = getSavedSelected(handbag);
        HandbagConfiguration moved = configurations.remove(from);
        configurations.add(destination, moved);
        writeConfigurations(handbag, configurations);

        if (selected == from) {
            selected = destination;
        } else if (from < selected && destination >= selected) {
            selected--;
        } else if (from > selected && destination <= selected) {
            selected++;
        }
        setSelected(handbag, selected);

        return destination;
    }

    public static int cycleSelected(ItemStack handbag, boolean forward) {
        List<HandbagConfiguration> configurations = getConfigurations(handbag);
        int size = configurations.size();
        if (size == 0) {
            setSelected(handbag, -1);
            return -1;
        }

        int selected = getSavedSelected(handbag);
        if (selected >= size) selected = -1;

        int next = selected < 0 ? 0 : (selected + (forward ? 1 : size - 1)) % size;
        for (int checked = 0; checked < size; checked++) {
            if (configurations.get(next).isAvailable()) {
                setSelected(handbag, next);
                return next;
            }

            next = (next + (forward ? 1 : size - 1)) % size;
        }

        setSelected(handbag, -1);
        return -1;
    }

    public static int insertMaterial(ItemStack handbag, int index, ItemStack material, boolean simulate) {
        HandbagConfiguration configuration = getConfiguration(handbag, index);
        if (configuration == null || !sameMaterial(configuration.getMaterial(), material)) return 0;

        int accepted = Math.min(material.getCount(), MATERIAL_CAPACITY - configuration.getMaterialCount());
        if (accepted <= 0 || simulate) return Math.max(0, accepted);

        replaceConfiguration(handbag, index, configuration.withMaterialCount(configuration.getMaterialCount() + accepted));
        return accepted;
    }

    public static boolean consumeMaterial(ItemStack handbag, int index) {
        return consumeMaterial(handbag, index, 1) == 1;
    }

    public static int consumeMaterial(ItemStack handbag, int index, int amount) {
        HandbagConfiguration configuration = getConfiguration(handbag, index);
        if (configuration == null || !configuration.isAvailable()
                || amount <= 0 || configuration.getMaterialCount() <= 0) return 0;

        int consumed = Math.min(amount, configuration.getMaterialCount());
        int newCount = configuration.getMaterialCount() - consumed;
        replaceConfiguration(handbag, index, configuration.withMaterialCount(newCount));

        return consumed;
    }

    public static boolean sameMaterial(ItemStack first, ItemStack second) {
        return !first.isEmpty()
            && !second.isEmpty()
            && ItemStack.areItemsEqual(first, second)
            && ItemStack.areItemStackTagsEqual(first, second);
    }

    public static NBTTagCompound copyData(ItemStack handbag) {
        NBTTagCompound root = getRoot(handbag, false);
        return root == null ? new NBTTagCompound() : root.copy();
    }

    public static void applyData(ItemStack handbag, NBTTagCompound data) {
        if (handbag.isEmpty()) return;

        NBTTagCompound stackTag = handbag.getTagCompound();
        if (stackTag == null) stackTag = new NBTTagCompound();
        stackTag.setTag(NBT_ROOT, data == null ? new NBTTagCompound() : data.copy());
        handbag.setTagCompound(stackTag);
    }

    @Nullable
    private static HandbagConfiguration readConfiguration(NBTTagCompound serialized) {
        HandbagIntegration integration = HandbagIntegration.fromModId(serialized.getString(NBT_INTEGRATION));
        if (integration == null) return null;

        return new HandbagConfiguration(
            serialized.getCompoundTag(NBT_MATERIAL),
            serialized.getCompoundTag(NBT_RESULT),
            integration,
            Math.min(MATERIAL_CAPACITY, Math.max(0, serialized.getInteger(NBT_MATERIAL_COUNT))));
    }

    private static void replaceConfiguration(ItemStack handbag, int index, HandbagConfiguration replacement) {
        List<HandbagConfiguration> configurations = new ArrayList<>(getConfigurations(handbag));
        if (index < 0 || index >= configurations.size()) return;

        configurations.set(index, replacement);
        writeConfigurations(handbag, configurations);
    }

    private static void writeConfigurations(ItemStack handbag, List<HandbagConfiguration> configurations) {
        NBTTagCompound root = getRoot(handbag, true);
        if (root == null) return;

        NBTTagList serialized = new NBTTagList();
        for (HandbagConfiguration configuration : configurations) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag(NBT_MATERIAL, configuration.getMaterialData());
            tag.setTag(NBT_RESULT, configuration.getResultData());
            tag.setString(NBT_INTEGRATION, configuration.getIntegration().getModID());
            tag.setInteger(NBT_MATERIAL_COUNT, Math.min(MATERIAL_CAPACITY, configuration.getMaterialCount()));
            serialized.appendTag(tag);
        }

        root.setTag(NBT_CONFIGURATIONS, serialized);
        if (!root.hasKey(NBT_SELECTED, Constants.NBT.TAG_INT)
                || root.getInteger(NBT_SELECTED) >= configurations.size()) {
            root.setInteger(NBT_SELECTED, -1);
        }
    }

    @Nullable
    private static NBTTagCompound getRoot(ItemStack handbag, boolean create) {
        if (handbag.isEmpty()) return null;

        NBTTagCompound stackTag = handbag.getTagCompound();
        if (stackTag == null) {
            if (!create) return null;
            stackTag = new NBTTagCompound();
            handbag.setTagCompound(stackTag);
        }

        if (!stackTag.hasKey(NBT_ROOT, Constants.NBT.TAG_COMPOUND)) {
            if (!create) return null;
            stackTag.setTag(NBT_ROOT, new NBTTagCompound());
        }

        return stackTag.getCompoundTag(NBT_ROOT);
    }
}
