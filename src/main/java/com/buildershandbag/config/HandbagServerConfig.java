package com.buildershandbag.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.buildershandbag.Tags;
import com.buildershandbag.integration.HandbagIntegration;


/**
 * Server-side switches for optional integrations, automatic material recovery, and mixins.
 */
@Config(modid = Tags.MODID, name = Tags.MODID + "/server", category = "server")
@Config.LangKey("buildershandbag.config.server")
public final class HandbagServerConfig {

    private static final String PREFIX = "buildershandbag.config.server.";
    private static final String INTEGRATIONS_PREFIX = PREFIX + "integrations";
    private static final String BEHAVIOR_PREFIX = PREFIX + "behavior";
    private static final String MIXIN_PREFIX = PREFIX + "mixin";

    @Config.LangKey(INTEGRATIONS_PREFIX)
    public static final Integrations integrations = new Integrations();

    @Config.LangKey(BEHAVIOR_PREFIX)
    public static final Behavior behavior = new Behavior();

    @Config.LangKey(MIXIN_PREFIX)
    public static final Mixin mixin = new Mixin();

    private static boolean clientCleanupFrames = true;
    private static boolean clientPickupAbsorption = true;
    private static boolean clientInventoryCleanup = true;

    private HandbagServerConfig() {
    }

    public static boolean isFrameCleanupEnabled() {
        return FMLCommonHandler.instance().getEffectiveSide().isServer()
            ? behavior.cleanupBlockcrafteryFrames : clientCleanupFrames;
    }

    public static boolean isPickupAbsorptionEnabled() {
        return FMLCommonHandler.instance().getEffectiveSide().isServer()
            ? behavior.enablePickupAbsorption : clientPickupAbsorption;
    }

    public static boolean isInventoryCleanupEnabled() {
        return FMLCommonHandler.instance().getEffectiveSide().isServer()
            ? behavior.enableInventoryCleanup : clientInventoryCleanup;
    }

    @SideOnly(Side.CLIENT)
    public static void setClientBehavior(boolean cleanupFrames, boolean pickupAbsorption, boolean inventoryCleanup) {
        clientCleanupFrames = cleanupFrames;
        clientPickupAbsorption = pickupAbsorption;
        clientInventoryCleanup = inventoryCleanup;
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
                if (integration.isEnabled()) enabledIntegrations.add(integration);
            }

            return enabledIntegrations;
        }
    }

    public static final class Behavior {

        @Config.Comment("Absorb configured materials and exact configured blocks when the player picks them up")
        @Config.LangKey(BEHAVIOR_PREFIX + ".pickup_absorption")
        public boolean enablePickupAbsorption = true;

        @Config.Comment("Absorb exact configured blocks from the player inventory every 30 seconds")
        @Config.LangKey(BEHAVIOR_PREFIX + ".inventory_cleanup")
        public boolean enableInventoryCleanup = true;

        @Config.Comment("Absorb and discard matching Blockcraftery frames (clean-up)")
        @Config.LangKey(BEHAVIOR_PREFIX + ".cleanup_blockcraftery_frames")
        public boolean cleanupBlockcrafteryFrames = true;

        @Config.Comment("Allow to use right-click when not targeting a block to open the handbag GUI, instead of shift + right-click only")
        @Config.LangKey(BEHAVIOR_PREFIX + ".use_old_open")
        public boolean useOldOpen = false;
    }

    /**
     * Startup switches for Building Gadgets and Better Builders Wands mixins.
     */
    public static final class Mixin {

        @Config.Comment("Enable the Building Gadgets handbag integration mixins")
        @Config.LangKey(MIXIN_PREFIX + ".buildinggadgets")
        @Config.RequiresMcRestart
        public boolean enableBuildingGadgets = true;

        @Config.Comment("Enable the Better Builders Wands handbag integration mixins, including Better Builders Wands Fix")
        @Config.LangKey(MIXIN_PREFIX + ".betterbuilderswands")
        @Config.RequiresMcRestart
        public boolean enableBetterBuildersWands = true;
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
