package com.buildershandbag.item;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * Placement modes for the handbag's right-click behavior.
 */
public enum HandbagMode {
    NORMAL("normal", 0),
    RANDOM("random", 1);

    private final String key;
    private final int textureColumn;

    private HandbagMode(String key, int textureColumn) {
        this.key = key;
        this.textureColumn = textureColumn;
    }

    public String getKey() {
        return key;
    }

    public int getTextureColumn() {
        return textureColumn;
    }

    public HandbagMode next() {
        HandbagMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }

    @SideOnly(Side.CLIENT)
    public String getTranslatedName() {
        return I18n.format("mode.buildershandbag." + key);
    }

    @SideOnly(Side.CLIENT)
    public String getTranslatedDescription() {
        return I18n.format("mode.buildershandbag." + key + ".description");
    }

    public static HandbagMode fromKey(String key) {
        for (HandbagMode mode : values()) {
            if (mode.key.equals(key)) return mode;
        }

        return NORMAL;
    }
}
