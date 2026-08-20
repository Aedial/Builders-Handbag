package com.buildershandbag.integration;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * The origin of a configuration option, used to identify the integration that produced it.
 */
public enum HandbagIntegration {
    CHISEL("chisel"),
    ARCHITECTURECRAFT("architecturecraft"),
    BLOCKCRAFTERY("blockcraftery");

    public static final String CHISEL_MODID = "chisel";
    public static final String ARCHITECTURECRAFT_MODID = "architecturecraft";
    public static final String BLOCKCRAFTERY_MODID = "blockcraftery";

    private final String modid;

    private HandbagIntegration(String modid) {
        this.modid = modid;
    }

    public String getModID() {
        return modid;
    }

    public boolean isModLoaded() {
        return Loader.isModLoaded(modid);
    }

    @SideOnly(Side.CLIENT)
    public String getTranslatedName() {
        return I18n.format("integration.buildershandbag." + modid);
    }

    public static HandbagIntegration fromOrdinal(int ordinal) {
        HandbagIntegration[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }
}
