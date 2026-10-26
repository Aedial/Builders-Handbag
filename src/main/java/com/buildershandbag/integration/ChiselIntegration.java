package com.buildershandbag.integration;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import team.chisel.api.carving.CarvingUtils;
import team.chisel.api.carving.ICarvingRegistry;

import com.buildershandbag.storage.HandbagConfiguration;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Chisel carving-registry integration.
 */
public final class ChiselIntegration {

    private ChiselIntegration() {
    }

    public static void addOptions(ItemStack material, List<HandbagConfigurationOption> options) {
        ICarvingRegistry registry = CarvingUtils.getChiselRegistry();
        if (registry == null) return;

        for (ItemStack result : registry.getItemsForChiseling(material.copy())) {
            HandbagConfigurationProvider.addOption(
                options,
                new HandbagConfigurationOption(result, HandbagIntegration.CHISEL));
        }
    }

    @Nullable
    public static HandbagConfiguration pickConfiguration(ItemStack result) {
        ICarvingRegistry registry = CarvingUtils.getChiselRegistry();
        if (registry == null) return null;

        ItemStack fallback = ItemStack.EMPTY;
        for (ItemStack material : registry.getItemsForChiseling(result.copy())) {
            if (!HandbagConfigurationProvider.isConfigurationMaterial(material)
                    || !canProduce(registry, material, result)) continue;

            // Chose first non-Chisel material, if available
            ResourceLocation name = material.getItem().getRegistryName();
            if (name != null && !name.getNamespace().equals(HandbagIntegration.CHISEL_MODID)) {
                return new HandbagConfiguration(material, result, HandbagIntegration.CHISEL, 0);
            }

            // Or Chisel's first valid variant in its existing order (should be the base material)
            if (fallback.isEmpty()) fallback = material;
        }

        return fallback.isEmpty() ? null : new HandbagConfiguration(fallback, result, HandbagIntegration.CHISEL, 0);
    }

    private static boolean canProduce(ICarvingRegistry registry, ItemStack material, ItemStack result) {
        for (ItemStack option : registry.getItemsForChiseling(material.copy())) {
            if (HandbagStorage.sameMaterial(option, result)) return true;
        }

        return false;
    }
}
