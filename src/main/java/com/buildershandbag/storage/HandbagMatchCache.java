package com.buildershandbag.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import com.buildershandbag.config.HandbagServerConfig;
import com.buildershandbag.integration.HandbagIntegration;
import com.buildershandbag.item.HandbagItemHandler;


/**
 * Enabled material and result lookups owned by the handbag stack's item handler.
 */
public final class HandbagMatchCache {

    private final ItemStack handbag;
    private final Map<HandbagStackKey, List<Integer>> materials = new HashMap<>();
    private final Map<HandbagStackKey, List<Integer>> results = new HashMap<>();
    private final List<MaterialAmount> returnedMaterials = new ArrayList<>();
    private int enabledMask = -1;
    private boolean cleanupFrames;

    public HandbagMatchCache(ItemStack handbag) {
        this.handbag = handbag;
    }

    @Nullable
    public static HandbagMatchCache get(ItemStack handbag) {
        IItemHandler handler = handbag.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        return handler instanceof HandbagItemHandler ? ((HandbagItemHandler) handler).getMatches() : null;
    }

    public static void invalidate(ItemStack handbag) {
        HandbagMatchCache cache = get(handbag);
        if (cache != null) cache.enabledMask = -1;
    }

    public List<Integer> getMaterials(HandbagStackKey key) {
        refresh();
        return materials.getOrDefault(key, Collections.emptyList());
    }

    public List<Integer> getResults(HandbagStackKey key) {
        refresh();
        return results.getOrDefault(key, Collections.emptyList());
    }

    @Nullable
    public MaterialAmount getAmount(int index, HandbagStackKey material, HandbagStackKey result,
            boolean includeMaterial) {
        refresh();
        if (includeMaterial && materials.getOrDefault(material, Collections.emptyList()).contains(index)) {
            return MaterialAmount.BLOCK;
        }

        if (!results.getOrDefault(result, Collections.emptyList()).contains(index)) return null;

        return returnedMaterials.get(index);
    }

    private void refresh() {
        int mask = HandbagIntegration.getEnabledMask();
        boolean cleanup = HandbagServerConfig.isFrameCleanupEnabled();
        if (enabledMask == mask && cleanupFrames == cleanup) return;

        materials.clear();
        results.clear();
        returnedMaterials.clear();
        List<HandbagConfiguration> configurations = HandbagStorage.getConfigurations(handbag);
        for (int index = 0; index < configurations.size(); index++) {
            HandbagConfiguration configuration = configurations.get(index);
            returnedMaterials.add(null);
            if (!configuration.isAvailable()) continue;

            ItemStack material = configuration.getMaterial();
            ItemStack result = configuration.getResult();
            add(materials, HandbagStackKey.material(material), index);
            MaterialAmount returned = configuration.getIntegration().getReturnedMaterial(material, result);
            returnedMaterials.set(index, returned);
            if (returned == null) continue;

            add(results, HandbagStackKey.output(result), index);
            add(results, HandbagStackKey.output(HandbagOutputs.getResult(configuration)), index);
        }

        enabledMask = mask;
        cleanupFrames = cleanup;
    }

    private static void add(Map<HandbagStackKey, List<Integer>> matches, HandbagStackKey key, int index) {
        List<Integer> indices = matches.computeIfAbsent(key, unused -> new ArrayList<>());
        if (!indices.contains(index)) indices.add(index);
    }
}
