package com.buildershandbag.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;

import com.buildershandbag.config.HandbagServerConfig;
import com.buildershandbag.item.ItemHandbag;


/**
 * Builds the configuration options for a material block.
 * <p>
 * Every integration stays optional so that the configuration provider
 * can always load, including on a server without those mods.
 */
public final class HandbagConfigurationProvider {

    private HandbagConfigurationProvider() {
    }

    public static List<HandbagConfigurationOption> getOptions(ItemStack material) {
        if (!isConfigurationMaterial(material)) return Collections.emptyList();

        // TODO: maybe make the order configurable?
        List<HandbagConfigurationOption> options = new ArrayList<>();
        if (HandbagServerConfig.integrations.isIntegrationEnabled(HandbagIntegration.CHISEL)) {
            addChiselOptions(material, options);
        }
        if (HandbagServerConfig.integrations.isIntegrationEnabled(HandbagIntegration.ARCHITECTURECRAFT)) {
            addArchitectureCraftOptions(material, options);
        }
        if (HandbagServerConfig.integrations.isIntegrationEnabled(HandbagIntegration.BLOCKCRAFTERY)) {
            addBlockcrafteryOptions(options);
        }

        return options;
    }

    public static boolean isConfigurationMaterial(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemBlock
                && !(stack.getItem() instanceof ItemHandbag);
    }

    @Optional.Method(modid = HandbagIntegration.CHISEL_MODID)
    private static void addChiselOptions(ItemStack material, List<HandbagConfigurationOption> options) {
        ChiselIntegration.addOptions(material, options);
    }

    @Optional.Method(modid = HandbagIntegration.ARCHITECTURECRAFT_MODID)
    private static void addArchitectureCraftOptions(ItemStack material, List<HandbagConfigurationOption> options) {
        ArchitectureCraftIntegration.addOptions(material, options);
    }

    @Optional.Method(modid = HandbagIntegration.BLOCKCRAFTERY_MODID)
    private static void addBlockcrafteryOptions(List<HandbagConfigurationOption> options) {
        BlockcrafteryIntegration.addOptions(options);
    }

    static void addOption(List<HandbagConfigurationOption> options, HandbagConfigurationOption option) {
        if (option.getResult().isEmpty()) return;

        for (HandbagConfigurationOption existing : options) {
            if (existing.matches(option)) return;
        }

        options.add(option);
    }
}
