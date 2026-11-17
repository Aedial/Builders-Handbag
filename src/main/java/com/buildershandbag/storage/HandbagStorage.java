package com.buildershandbag.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.buildershandbag.container.HandbagLayout;
import com.buildershandbag.integration.HandbagIntegration;
import com.buildershandbag.integration.FutureMcRecipeCache;
import com.buildershandbag.config.HandbagServerConfig;
import com.buildershandbag.item.HandbagMode;


/**
 * Configuration storage for the handbag item. The storage is serialized to the NBT of the ItemStack.
 */
public final class HandbagStorage {

    public static final int CONFIGURATION_COUNT = HandbagLayout.CONFIGURATION_COUNT;
    public static final int MATERIAL_CAPACITY = 64;

    private static final String NBT_ROOT = "Handbag";
    private static final String NBT_CONFIGURATIONS = "Configurations";
    private static final String NBT_SELECTED = "Selected";
    private static final String NBT_MODE = "Mode";
    private static final String NBT_MATERIAL = "Material";
    private static final String NBT_RESULT = "Result";
    private static final String NBT_INTEGRATION = "Integration";
    private static final String NBT_MATERIAL_COUNT = "MaterialCount";
    private static final String NBT_MATERIAL_REMAINDER = "MaterialRemainder";
    private static final String NBT_MATERIAL_DENOMINATOR = "MaterialDenominator";
    private static final String NBT_OUTPUT_COUNT = "OutputCount";

    private HandbagStorage() {
    }

    public static HandbagMode getMode(ItemStack handbag) {
        NBTTagCompound root = getRoot(handbag, false);
        return root == null ? HandbagMode.NORMAL : HandbagMode.fromKey(root.getString(NBT_MODE));
    }

    public static void setMode(ItemStack handbag, HandbagMode mode) {
        NBTTagCompound root = getRoot(handbag, true);
        if (root == null) return;

        root.setString(NBT_MODE, mode.getKey());
    }

    public static boolean hasConfigurations(ItemStack handbag) {
        NBTTagCompound root = getRoot(handbag, false);
        return root != null && !root.getTagList(NBT_CONFIGURATIONS, Constants.NBT.TAG_COMPOUND).isEmpty();
    }

    /**
     * Creates a copy of the target with the source's configurations and empty material balances.
     * TODO: We may consider copy into non-empty targets as well. It would require merging configurations
     *       and deciding how we handle handbags with no space left.
     */
    public static ItemStack copyConfigurations(ItemStack source, ItemStack target) {
        if (!hasConfigurations(source) || target.isEmpty() || hasConfigurations(target)) return ItemStack.EMPTY;

        ItemStack result = target.copy();
        result.setCount(1);

        NBTTagCompound sourceRoot = getRoot(source, false);
        NBTTagList configurations = sourceRoot.getTagList(NBT_CONFIGURATIONS, Constants.NBT.TAG_COMPOUND).copy();
        for (int index = 0; index < configurations.tagCount(); index++) {
            NBTTagCompound configuration = configurations.getCompoundTagAt(index);
            configuration.setInteger(NBT_MATERIAL_COUNT, 0);
            configuration.removeTag(NBT_MATERIAL_REMAINDER);
            configuration.removeTag(NBT_MATERIAL_DENOMINATOR);
        }
        NBTTagCompound resultRoot = getRoot(result, true);
        resultRoot.setTag(NBT_CONFIGURATIONS, configurations);

        int selected = getSavedSelected(source);
        resultRoot.setInteger(NBT_SELECTED, selected >= 0 && selected < configurations.tagCount() ? selected : -1);
        HandbagMatchCache.invalidate(result);

        return result;
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
        HandbagMatchCache.invalidate(handbag);

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
        HandbagMatchCache.invalidate(handbag);
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
        HandbagMatchCache.invalidate(handbag);

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

    public static int selectRandom(ItemStack handbag, Random random) {
        List<HandbagConfiguration> configurations = getConfigurations(handbag);
        List<Integer> available = new ArrayList<>();
        for (int index = 0; index < configurations.size(); index++) {
            if (configurations.get(index).isAvailable()) available.add(index);
        }

        int selected = available.isEmpty() ? -1 : available.get(random.nextInt(available.size()));
        setSelected(handbag, selected);
        return selected;
    }

    public static int insertMaterial(ItemStack handbag, int index, ItemStack material, boolean simulate) {
        if (material.isEmpty()) return 0;

        HandbagMatchCache cache = HandbagMatchCache.get(handbag);
        if (cache == null) return 0;

        MaterialAmount amount = cache.getAmount(index, HandbagStackKey.probeMaterial(material),
            HandbagStackKey.probeOutput(material), true);
        return insertMaterial(handbag, index, material.getCount(), amount, simulate);
    }

    public static int insertMaterial(ItemStack handbag, int index, int count,
            @Nullable MaterialAmount amount, boolean simulate) {
        if (count <= 0 || amount == null) return 0;

        HandbagConfiguration configuration = getConfiguration(handbag, index);
        if (configuration == null || !configuration.isAvailable()) return 0;
        if (amount.isEmpty()) return count;

        MaterialAmount balance = configuration.getMaterialAmount();
        int accepted = Math.min(count, balance.getInsertCount(amount, MATERIAL_CAPACITY));
        if (accepted <= 0 || simulate) return Math.max(0, accepted);

        replaceConfiguration(handbag, index, configuration.withMaterialAmount(balance.add(amount, accepted)));
        return accepted;
    }

    public static boolean consumeMaterial(ItemStack handbag, int index) {
        return consumeMaterial(handbag, index, 1) == 1;
    }

    /**
     * Deducts the material cost for up to the requested number of configured outputs.
     */
    public static int consumeMaterial(ItemStack handbag, int index, int amount) {
        HandbagConfiguration configuration = getConfiguration(handbag, index);
        if (configuration == null || !configuration.isAvailable()
                || amount <= 0) return 0;

        int consumed = Math.min(amount, configuration.getOutputCount());
        if (consumed <= 0) return 0;

        MaterialAmount remaining = configuration.getMaterialAmount().consume(configuration.getMaterialCost(), consumed);
        replaceConfiguration(handbag, index, configuration.withMaterialAmount(remaining));

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
        HandbagMatchCache.invalidate(handbag);
    }

    public static boolean validateRecipes(ItemStack handbag) {
        HandbagMatchCache cache = HandbagMatchCache.get(handbag);
        return cache != null && cache.validateRecipes();
    }

    public static boolean refreshRecipes(ItemStack handbag) {
        if (!HandbagIntegration.FUTUREMC.isEnabled()) return false;

        List<HandbagConfiguration> configurations = new ArrayList<>(getConfigurations(handbag));
        boolean changed = false;
        for (int index = 0; index < configurations.size(); index++) {
            HandbagConfiguration configuration = configurations.get(index);
            if (configuration.getIntegration() != HandbagIntegration.FUTUREMC) continue;

            ItemStack material = configuration.getMaterial();
            ItemStack result = configuration.getResult();
            if (material.isEmpty() || result.isEmpty()) continue;

            int outputCount = FutureMcRecipeCache.getOutputCount(material, result);
            if (outputCount == -1 || outputCount == configuration.getRecipeOutputCount()
                    || !HandbagServerConfig.integrations.autoMigrateChangedRatios) continue;

            configurations.set(index, configuration.withRecipeOutputCount(outputCount));
            changed = true;
        }

        if (changed) {
            writeConfigurations(handbag, configurations);
            HandbagMatchCache.invalidate(handbag);
        }

        return changed;
    }

    @Nullable
    private static HandbagConfiguration readConfiguration(NBTTagCompound serialized) {
        HandbagIntegration integration = HandbagIntegration.fromModId(serialized.getString(NBT_INTEGRATION));
        if (integration == null) return null;

        int count = Math.min(MATERIAL_CAPACITY, Math.max(0, serialized.getInteger(NBT_MATERIAL_COUNT)));
        int denominator = Math.max(1, serialized.getInteger(NBT_MATERIAL_DENOMINATOR));
        int remainder = Math.min(denominator - 1, Math.max(0, serialized.getInteger(NBT_MATERIAL_REMAINDER)));

        return new HandbagConfiguration(
            serialized.getCompoundTag(NBT_MATERIAL),
            serialized.getCompoundTag(NBT_RESULT),
            integration,
            new MaterialAmount((long) count * denominator + remainder, denominator),
            Math.max(1, serialized.getInteger(NBT_OUTPUT_COUNT)));
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
            if (configuration.getIntegration() == HandbagIntegration.FUTUREMC) {
                tag.setInteger(NBT_OUTPUT_COUNT, configuration.getRecipeOutputCount());
            }
            MaterialAmount amount = configuration.getMaterialAmount();
            tag.setInteger(NBT_MATERIAL_COUNT, Math.min(MATERIAL_CAPACITY, amount.getWholeCount()));
            if (amount.getDenominator() > 1) {
                tag.setInteger(NBT_MATERIAL_REMAINDER, amount.getRemainder());
                tag.setInteger(NBT_MATERIAL_DENOMINATOR, amount.getDenominator());
            }
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
