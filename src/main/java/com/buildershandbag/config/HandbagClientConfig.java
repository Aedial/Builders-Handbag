package com.buildershandbag.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.buildershandbag.Tags;


/**
 * Client-only rendering options for handbags.
 */
@Config(modid = Tags.MODID, name = Tags.MODID + "/client", category = "client")
@Config.LangKey("buildershandbag.config.client")
public final class HandbagClientConfig {

    private static final String PREFIX = "buildershandbag.config.client.";
    private static final String RENDERING_PREFIX = PREFIX + "rendering";
    private static final String DEFAULT_CORE_FACE_COLOR_STR = "#8440E0D0";
    private static final String DEFAULT_CORE_EDGE_COLOR_STR = "#84000000";

    private static int coreFaceColor = parseColor(DEFAULT_CORE_FACE_COLOR_STR, 0);
    private static int coreEdgeColor = parseColor(DEFAULT_CORE_EDGE_COLOR_STR, 0);

    @Config.LangKey(RENDERING_PREFIX)
    public static final Rendering rendering = new Rendering();

    private HandbagClientConfig() {
    }

    public static int getCoreFaceColor() {
        return coreFaceColor;
    }

    public static int getCoreEdgeColor() {
        return coreEdgeColor;
    }

    public static void refreshRenderingColors() {
        coreFaceColor = parseColor(rendering.coreFaceColor, coreFaceColor);
        coreEdgeColor = parseColor(rendering.coreEdgeColor, coreEdgeColor);
    }

    private static int parseColor(String value, int fallback) {
        if (value == null) return fallback;

        String hexadecimal = value.trim();
        if (hexadecimal.startsWith("#")) hexadecimal = hexadecimal.substring(1);
        if (hexadecimal.length() != 8) return fallback;

        try {
            return (int) Long.parseLong(hexadecimal, 16);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static final class Rendering {

        @Config.Comment("Scale of the handbag core")
        @Config.RangeDouble(min = 0.1D, max = 10.0D)
        @Config.LangKey(RENDERING_PREFIX + ".core_scale")
        public double coreScale = 1.0D;

        @Config.Comment("Color of the faces on the default icosidodecahedron core, as #AARRGGBB")
        @Config.LangKey(RENDERING_PREFIX + ".core_face_color")
        public String coreFaceColor = DEFAULT_CORE_FACE_COLOR_STR;

        @Config.Comment("Color of the edges on the default icosidodecahedron core, as #AARRGGBB")
        @Config.LangKey(RENDERING_PREFIX + ".core_edge_color")
        public String coreEdgeColor = DEFAULT_CORE_EDGE_COLOR_STR;

        @Config.Comment("Rotation speed multiplier for the core")
        @Config.RangeDouble(min = 0.0D, max = 10.0D)
        @Config.LangKey(RENDERING_PREFIX + ".core_rotation_speed")
        public double coreRotationSpeed = 1.0D;

        @Config.Comment("Whether to render the contents overlay above the experience bar")
        @Config.LangKey(RENDERING_PREFIX + ".render_contents_overlay")
        public boolean renderContentsOverlay = true;

        @Config.Comment("Vertical gap in pixels between the contents overlay and the experience bar")
        @Config.RangeInt(min = 0, max = 256)
        @Config.LangKey(RENDERING_PREFIX + ".content_overlay_y_offset")
        public int contentOverlayYOffset = 5;
    }

    @Mod.EventBusSubscriber(modid = Tags.MODID)
    public static final class ConfigSyncHandler {

        private ConfigSyncHandler() {
        }

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (Tags.MODID.equals(event.getModID())) {
                ConfigManager.sync(Tags.MODID, Config.Type.INSTANCE);
                refreshRenderingColors();
            }
        }
    }
}
