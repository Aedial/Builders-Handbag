package com.buildershandbag.mixin;

import java.util.Arrays;
import java.util.List;

import net.minecraftforge.fml.common.Loader;

import zone.rong.mixinbooter.ILateMixinLoader;


public final class HandbagLateMixinLoader implements ILateMixinLoader {

    private static final String PREFIX = "mixins.buildershandbag.";

    public HandbagLateMixinLoader() {
    }

    @Override
    public List<String> getMixinConfigs() {
        return Arrays.asList(PREFIX + "buildinggadgets.json", PREFIX + "bbw.json",
            PREFIX + "bbwbase.json", PREFIX + "bbwfix.json");
    }

    @Override
    public boolean shouldMixinConfigQueue(String config) {
        if (config.equals(PREFIX + "buildinggadgets.json")) return Loader.isModLoaded("buildinggadgets");
        if (config.equals(PREFIX + "bbw.json")) return Loader.isModLoaded("betterbuilderswands");
        if (config.equals(PREFIX + "bbwbase.json")) {
            return Loader.isModLoaded("betterbuilderswands") && !Loader.isModLoaded("betterbuilderswandsfix");
        }
        if (config.equals(PREFIX + "bbwfix.json")) {
            return Loader.isModLoaded("betterbuilderswands") && Loader.isModLoaded("betterbuilderswandsfix");
        }

        return false;
    }
}
