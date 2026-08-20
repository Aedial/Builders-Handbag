package com.buildershandbag.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;

import com.buildershandbag.config.HandbagServerConfig;

/**
 * Builds the configuration options for a material block.
 * <p>
 * Every integration stays optional so that the configuration provider
 * can always load, including on a server without those mods.
 */
public final class HandbagConfigurationProvider {

    private static final String CHISEL_MODID = "chisel";
    private static final String ARCHITECTURECRAFT_MODID = "architecturecraft";
    private static final String BLOCKCRAFTERY_MODID = "blockcraftery";

    private HandbagConfigurationProvider() {
    }

    public static List<HandbagConfigurationOption> getOptions(ItemStack material) {
        if (!isConfigurationMaterial(material)) return Collections.emptyList();

        // TODO: maybe make the order configurable?
        List<HandbagConfigurationOption> options = new ArrayList<>();
        if (HandbagServerConfig.integrations.enableChisel && Loader.isModLoaded(CHISEL_MODID)) {
            addChiselOptions(material, options);
        }
        if (HandbagServerConfig.integrations.enableArchitectureCraft && Loader.isModLoaded(ARCHITECTURECRAFT_MODID)) {
            addArchitectureCraftOptions(material, options);
        }
        if (HandbagServerConfig.integrations.enableBlockcraftery && Loader.isModLoaded(BLOCKCRAFTERY_MODID)) {
            addBlockcrafteryOptions(options);
        }

        return options;
    }

    public static boolean isConfigurationMaterial(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemBlock;
    }

    /**
     * Defers resolving the Chisel-only implementation until Chisel is loaded.
     */
    @Optional.Method(modid = CHISEL_MODID)
    private static void addChiselOptions(ItemStack material, List<HandbagConfigurationOption> options) {
        ChiselIntegration.addOptions(material, options);
    }

    /**
     * Defers resolving the ArchitectureCraft-only implementation until it is loaded.
     */
    @Optional.Method(modid = ARCHITECTURECRAFT_MODID)
    private static void addArchitectureCraftOptions(ItemStack material, List<HandbagConfigurationOption> options) {
        ArchitectureCraftIntegration.addOptions(material, options);
    }

    /**
     * Defers resolving the Blockcraftery-only implementation until it is loaded.
     */
    @Optional.Method(modid = BLOCKCRAFTERY_MODID)
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
