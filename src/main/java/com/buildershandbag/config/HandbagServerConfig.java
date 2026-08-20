package com.buildershandbag.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.buildershandbag.Tags;
import com.buildershandbag.integration.HandbagIntegration;


/**
 * Server-side switches for optional integrations.
 */
@Config(modid = Tags.MODID, name = Tags.MODID + "/server", category = "server")
@Config.LangKey("buildershandbag.config.server")
public final class HandbagServerConfig {

    private static final String PREFIX = "buildershandbag.config.server.";
    private static final String INTEGRATIONS_PREFIX = PREFIX + "integrations";

    @Config.LangKey(INTEGRATIONS_PREFIX)
    public static final Integrations integrations = new Integrations();

    private HandbagServerConfig() {
    }

    public static final class Integrations {

        @Config.Comment("Allow configurations supplied by Chisel when it is installed.")
        @Config.LangKey(INTEGRATIONS_PREFIX + ".chisel")
        public boolean enableChisel = true;

        @Config.Comment("Allow configurations supplied by ArchitectureCraft when it is installed")
        @Config.LangKey(INTEGRATIONS_PREFIX + ".architecturecraft")
        public boolean enableArchitectureCraft = true;

        @Config.Comment("Allow configurations supplied by Blockcraftery when it is installed")
        @Config.LangKey(INTEGRATIONS_PREFIX + ".blockcraftery")
        public boolean enableBlockcraftery = true;

        @Config.Comment("Refill an empty selected configuration through its AE2 link, if available")
        @Config.LangKey(INTEGRATIONS_PREFIX + ".ae2_refill")
        public boolean enableAe2Refill = true;


        public boolean isIntegrationEnabled(HandbagIntegration integration) {
            if (!integration.isModLoaded()) return false;

            switch (integration) {
                case CHISEL:
                    return enableChisel;
                case ARCHITECTURECRAFT:
                    return enableArchitectureCraft;
                case BLOCKCRAFTERY:
                    return enableBlockcraftery;
                default:
                    return false;
            }
        }

        public List<HandbagIntegration> getEnabledIntegrations() {
            List<HandbagIntegration> enabledIntegrations = new ArrayList<>();
            for (HandbagIntegration integration : HandbagIntegration.values()) {
                if (isIntegrationEnabled(integration)) enabledIntegrations.add(integration);
            }

            return enabledIntegrations;
        }
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
