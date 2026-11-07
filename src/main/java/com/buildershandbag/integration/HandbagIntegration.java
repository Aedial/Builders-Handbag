package com.buildershandbag.integration;

import javax.annotation.Nullable;

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
    BLOCKCRAFTERY("blockcraftery") {
        @Override
        @Nullable
        public MaterialAmount getReturnedMaterial(ItemStack material, ItemStack result) {
            // BlockCraftery returns frame (BlockCraftery) + original block (not BlockCraftery)
            // This integration only matches the frame part, ignoring the original block
            return HandbagServerConfig.isFrameCleanupEnabled() ? MaterialAmount.ZERO : null;
        }
    };

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

    @Nullable
    public MaterialAmount getReturnedMaterial(ItemStack material, ItemStack result) {
        return getMaterialCost(material, result);
    }

    public static int getEnabledMask() {
        int mask = 0;
        for (HandbagIntegration integration : values()) {
            if (integration.isEnabled()) mask |= 1 << integration.ordinal();
        }

        return mask;
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
