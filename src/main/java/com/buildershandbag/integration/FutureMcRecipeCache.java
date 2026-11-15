package com.buildershandbag.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Optional;

import com.buildershandbag.storage.HandbagStackKey;


/**
 * Session recipe lookups and server sync for materials used by Future-MC configurations.
 */
public final class FutureMcRecipeCache {

    private static final Map<HandbagStackKey, Entry> SERVER = new LinkedHashMap<>();
    private static final Map<HandbagStackKey, Entry> CLIENT = new LinkedHashMap<>();
    private static final List<Entry> PENDING = new ArrayList<>();
    private static final Map<HandbagStackKey, ItemStack> REQUESTS = new LinkedHashMap<>();
    private static final Set<HandbagStackKey> REQUESTED = new HashSet<>();
    private static int serverVersion;
    private static int clientVersion;

    private FutureMcRecipeCache() {
    }

    public static List<HandbagConfigurationOption> getOptions(ItemStack material) {
        Entry entry = getEntry(material);
        return entry == null ? Collections.emptyList() : entry.options;
    }

    @Nullable
    private static Entry getEntry(ItemStack material) {
        boolean server = FMLCommonHandler.instance().getEffectiveSide().isServer();
        Map<HandbagStackKey, Entry> entries = server ? SERVER : CLIENT;

        Entry entry = entries.get(HandbagStackKey.probeMaterial(material));
        if (entry == null && server && HandbagIntegration.FUTUREMC.isModLoaded()) {
            entry = new Entry(material, loadOptions(material));
            entries.put(HandbagStackKey.material(material), entry);
            PENDING.add(entry);
        } else if (entry == null && !server && !material.isEmpty()
                && !REQUESTED.contains(HandbagStackKey.probeMaterial(material))) {
            HandbagStackKey key = HandbagStackKey.material(material);
            REQUESTED.add(key);
            REQUESTS.put(key, material.copy());
        }

        return entry;
    }

    public static int getOutputCount(ItemStack material, ItemStack result) {
        Entry entry = getEntry(material);
        if (entry == null) return -1;

        return entry.outputCounts.getOrDefault(HandbagStackKey.probeMaterial(result), 0);
    }

    public static int getVersion() {
        return FMLCommonHandler.instance().getEffectiveSide().isServer() ? serverVersion : clientVersion;
    }

    public static boolean hasUpdates() {
        return !PENDING.isEmpty();
    }

    public static boolean hasRequests() {
        return !REQUESTS.isEmpty();
    }

    public static NBTTagCompound takeRequests() {
        NBTTagList materials = new NBTTagList();
        for (ItemStack material : REQUESTS.values()) materials.appendTag(material.writeToNBT(new NBTTagCompound()));

        REQUESTS.clear();
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("Materials", materials);
        return data;
    }

    public static NBTTagCompound resolveRequests(NBTTagCompound data) {
        List<Entry> resolved = new ArrayList<>();
        NBTTagList materials = data.getTagList("Materials", Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < materials.tagCount(); index++) {
            ItemStack material = new ItemStack(materials.getCompoundTagAt(index));
            if (!HandbagConfigurationProvider.isConfigurationMaterial(material)) continue;

            Entry entry = getEntry(material);
            if (entry != null) resolved.add(entry);
        }

        return writeEntries(resolved);
    }

    public static NBTTagCompound getSnapshot() {
        return writeEntries(new ArrayList<>(SERVER.values()));
    }

    public static NBTTagCompound takeUpdates() {
        NBTTagCompound data = writeEntries(PENDING);
        PENDING.clear();
        return data;
    }

    public static void applySnapshot(NBTTagCompound data) {
        NBTTagList entries = data.getTagList("Materials", Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < entries.tagCount(); index++) {
            NBTTagCompound serialized = entries.getCompoundTagAt(index);
            ItemStack material = new ItemStack(serialized.getCompoundTag("Material"));
            if (material.isEmpty()) continue;

            List<HandbagConfigurationOption> options = new ArrayList<>();
            NBTTagList results = serialized.getTagList("Results", Constants.NBT.TAG_COMPOUND);
            for (int resultIndex = 0; resultIndex < results.tagCount(); resultIndex++) {
                NBTTagCompound result = results.getCompoundTagAt(resultIndex);
                ItemStack stack = new ItemStack(result.getCompoundTag("Stack"));
                int outputCount = result.getInteger("OutputCount");
                if (stack.isEmpty() || outputCount <= 0) continue;

                options.add(new HandbagConfigurationOption(stack, HandbagIntegration.FUTUREMC, outputCount));
            }

            CLIENT.put(HandbagStackKey.material(material), new Entry(material, options));
            REQUESTS.remove(HandbagStackKey.probeMaterial(material));
            REQUESTED.remove(HandbagStackKey.probeMaterial(material));
        }

        clientVersion++;
    }

    public static void clearServer() {
        SERVER.clear();
        PENDING.clear();
        serverVersion++;
    }

    public static void clearClient() {
        CLIENT.clear();
        REQUESTS.clear();
        REQUESTED.clear();
        clientVersion++;
    }

    @Optional.Method(modid = HandbagIntegration.FUTUREMC_MODID)
    private static List<HandbagConfigurationOption> loadOptions(ItemStack material) {
        return FutureMcIntegration.getOptions(material);
    }

    private static NBTTagCompound writeEntries(List<Entry> entries) {
        NBTTagList materials = new NBTTagList();
        for (Entry entry : entries) {
            NBTTagCompound serialized = new NBTTagCompound();
            serialized.setTag("Material", entry.material.writeToNBT(new NBTTagCompound()));
            NBTTagList results = new NBTTagList();
            for (HandbagConfigurationOption option : entry.options) {
                NBTTagCompound result = new NBTTagCompound();
                result.setTag("Stack", option.getResult().writeToNBT(new NBTTagCompound()));
                result.setInteger("OutputCount", option.getOutputCount());
                results.appendTag(result);
            }

            serialized.setTag("Results", results);
            materials.appendTag(serialized);
        }

        NBTTagCompound data = new NBTTagCompound();
        data.setTag("Materials", materials);
        return data;
    }

    private static final class Entry {

        private final ItemStack material;
        private final List<HandbagConfigurationOption> options;
        private final Map<HandbagStackKey, Integer> outputCounts = new HashMap<>();

        private Entry(ItemStack material, List<HandbagConfigurationOption> options) {
            this.material = material.copy();
            this.material.setCount(1);
            this.options = Collections.unmodifiableList(new ArrayList<>(options));
            for (HandbagConfigurationOption option : options) {
                outputCounts.put(HandbagStackKey.material(option.getResult()), option.getOutputCount());
            }
        }
    }
}
