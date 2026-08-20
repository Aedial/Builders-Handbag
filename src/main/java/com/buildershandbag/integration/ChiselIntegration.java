package com.buildershandbag.integration;

import java.util.List;

import net.minecraft.item.ItemStack;

import team.chisel.api.carving.CarvingUtils;
import team.chisel.api.carving.ICarvingRegistry;


/**
 * Chisel carving-registry integration.
 */
final class ChiselIntegration {

    private ChiselIntegration() {
    }

    static void addOptions(ItemStack material, List<HandbagConfigurationOption> options) {
        ICarvingRegistry registry = CarvingUtils.getChiselRegistry();
        if (registry == null) return;

        for (ItemStack result : registry.getItemsForChiseling(material.copy())) {
            HandbagConfigurationProvider.addOption(
                options,
                new HandbagConfigurationOption(result, HandbagIntegration.CHISEL));
        }
    }
}
