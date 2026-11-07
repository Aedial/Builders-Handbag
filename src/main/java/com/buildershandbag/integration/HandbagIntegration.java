package com.buildershandbag.integration;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.buildershandbag.config.HandbagServerConfig;
import com.buildershandbag.storage.MaterialAmount;


/**
 * The origin of a configuration option, used to identify the integration that produced it.
 * Material costs for the supported configuration integrations.
 */
public enum HandbagIntegration {
    CHISEL("chisel"),
    ARCHITECTURECRAFT("architecturecraft") {
        @Override
        public MaterialAmount getMaterialCost(ItemStack material, ItemStack result) {
            if (!isModLoaded()) return MaterialAmount.BLOCK;

            return getArchitectureCraftCost(material, result);
        }
    },
    BLOCKCRAFTERY("blockcraftery");

    public static final String CHISEL_MODID = "chisel";
    public static final String ARCHITECTURECRAFT_MODID = "architecturecraft";
    public static final String BLOCKCRAFTERY_MODID = "blockcraftery";

    private static int clientEnabledIntegrations;

    private final String modid;

    private HandbagIntegration(String modid) {
        this.modid = modid;
    }

    public String getModID() {
        return modid;
    }

    public MaterialAmount getMaterialCost(ItemStack material, ItemStack result) {
        return MaterialAmount.BLOCK;
    }

    @Optional.Method(modid = ARCHITECTURECRAFT_MODID)
    private static MaterialAmount getArchitectureCraftCost(ItemStack material, ItemStack result) {
        return ArchitectureCraftIntegration.getMaterialCost(material, result);
    }

    public boolean isModLoaded() {
        return Loader.isModLoaded(modid);
    }

    public boolean isEnabled() {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            return HandbagServerConfig.integrations.isIntegrationEnabled(this);
        }

        return isModLoaded() && (clientEnabledIntegrations & (1 << ordinal())) != 0;
    }

    @SideOnly(Side.CLIENT)
    public static void setClientEnabledIntegrations(int enabledIntegrations) {
        clientEnabledIntegrations = enabledIntegrations;
    }

    @SideOnly(Side.CLIENT)
    public String getTranslatedName() {
        return I18n.format("integration.buildershandbag." + modid);
    }

    public static HandbagIntegration fromOrdinal(int ordinal) {
        HandbagIntegration[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    public static HandbagIntegration fromModId(String modid) {
        if (modid == null) return null;

        for (HandbagIntegration integration : values()) {
            if (integration.modid.equals(modid)) return integration;
        }

        return null;
    }
}
