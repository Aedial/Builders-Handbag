package com.buildershandbag.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.buildershandbag.Tags;


/**
 * Client-only rendering options for placed handbags.
 */
@Config(modid = Tags.MODID, name = Tags.MODID + "/client", category = "client")
@Config.LangKey("buildershandbag.config.client")
public final class HandbagClientConfig {

    private static final String PREFIX = "buildershandbag.config.client.";
    private static final String RENDERING_PREFIX = PREFIX + "rendering";

    @Config.LangKey(RENDERING_PREFIX)
    public static final Rendering rendering = new Rendering();

    private HandbagClientConfig() {
    }

    public static final class Rendering {

        @Config.Comment({
            "Uniform scale of the rotating handbag core.",
            "0.875 (14/16) fills the 14x14x14 space enclosed by the frame."
        })
        @Config.RangeDouble(min = 0.05D, max = 0.875D)
        @Config.LangKey(RENDERING_PREFIX + ".core_scale")
        public double coreScale = 0.875D;
    }

    @Mod.EventBusSubscriber(modid = Tags.MODID)
    public static final class ConfigSyncHandler {

        private ConfigSyncHandler() {
        }

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (Tags.MODID.equals(event.getModID())) ConfigManager.sync(Tags.MODID, Config.Type.INSTANCE);
        }
    }
}
