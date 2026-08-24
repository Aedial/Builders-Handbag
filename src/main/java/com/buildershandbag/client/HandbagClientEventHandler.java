package com.buildershandbag.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import com.buildershandbag.Tags;
import com.buildershandbag.client.overlay.OverlayMessageRenderer;
import com.buildershandbag.config.HandbagClientConfig;
import com.buildershandbag.item.ItemRegistry;
import com.buildershandbag.network.HandbagNetwork;
import com.buildershandbag.network.PacketCycleHandbagConfiguration;
import com.buildershandbag.storage.HandbagConfiguration;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Client-only held-item scrolling and non-GUI overlay rendering.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class HandbagClientEventHandler {

    private static final ResourceLocation CONTENT_OVERLAY = new ResourceLocation(
        Tags.MODID, "textures/overlays/content_overlay.png");
    private static final int CONTENT_OVERLAY_TEXTURE_WIDTH = 256;
    private static final int CONTENT_OVERLAY_TEXTURE_HEIGHT = 32;
    private static final int CONTENT_OVERLAY_WIDTH = 170;
    private static final int CONTENT_OVERLAY_HEIGHT = 24;
    private static final int XP_BAR_TOP_OFFSET = 32;
    private static final int SELECTED_CONTENT_SLOT = 4;
    private static final int CONTENT_SLOT_Y = 4;
    private static final float CONTENT_BACKGROUND_ALPHA = 0.5F;
    private static final int[] CONTENT_SLOT_X = {4, 22, 40, 58, 77, 96, 114, 132, 150};

    private HandbagClientEventHandler() {
    }

    @SubscribeEvent
    public static void onMouseInput(MouseEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (event.getDwheel() == 0 || player == null || !player.isSneaking() || minecraft.currentScreen != null) return;

        EnumHand hand = getHeldHandbagHand(player);
        if (hand == null || HandbagStorage.getConfigurations(player.getHeldItem(hand)).isEmpty()) return;

        HandbagNetwork.INSTANCE.sendToServer(new PacketCycleHandbagConfiguration(hand, event.getDwheel() < 0));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onOverlayRender(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen != null) return;

        int screenWidth = event.getResolution().getScaledWidth();
        int screenHeight = event.getResolution().getScaledHeight();

        EntityPlayer player = minecraft.player;
        if (player != null) renderContentsOverlay(minecraft, player, screenWidth, screenHeight);

        OverlayMessageRenderer.render(screenWidth, screenHeight);
    }

    private static void renderContentsOverlay(Minecraft minecraft, EntityPlayer player, int screenWidth,
            int screenHeight) {
        if (!HandbagClientConfig.rendering.renderContentsOverlay) return;

        EnumHand hand = getHeldHandbagHand(player);
        if (hand == null) return;

        ItemStack handbag = player.getHeldItem(hand);
        List<HandbagConfiguration> configurations = HandbagStorage.getConfigurations(handbag);
        int selected = HandbagStorage.getSelected(handbag);
        if (selected < 0) return;

        int displayedCount = Math.min(configurations.size(), CONTENT_SLOT_X.length);
        // Integer division gives the left side the extra configuration for even counts
        int leftCount = displayedCount / 2;
        int rightCount = displayedCount - leftCount - 1;
        int x = (screenWidth - CONTENT_OVERLAY_WIDTH) / 2;
        int y = screenHeight - XP_BAR_TOP_OFFSET - CONTENT_OVERLAY_HEIGHT
            - HandbagClientConfig.rendering.contentOverlayYOffset;

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        minecraft.getTextureManager().bindTexture(CONTENT_OVERLAY);
        GlStateManager.color(1.0F, 1.0F, 1.0F, CONTENT_BACKGROUND_ALPHA);
        Gui.drawModalRectWithCustomSizedTexture(
            x,
            y,
            0.0F,
            0.0F,
            CONTENT_OVERLAY_WIDTH,
            CONTENT_OVERLAY_HEIGHT,
            CONTENT_OVERLAY_TEXTURE_WIDTH,
            CONTENT_OVERLAY_TEXTURE_HEIGHT);

        // TODO: Should we add some fade-in/out effect for the items when scrolling?
        //       1.12 ItemStack rendering doesn't play well with alpha.
        //       Maybe some interpolation with exponential smoothing
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        for (int relativeIndex = -leftCount; relativeIndex <= rightCount; relativeIndex++) {
            int configurationIndex = (selected + relativeIndex + configurations.size()) % configurations.size();
            int contentSlot = SELECTED_CONTENT_SLOT + relativeIndex;
            minecraft.getRenderItem().renderItemAndEffectIntoGUI(
                configurations.get(configurationIndex).getResult(),
                x + CONTENT_SLOT_X[contentSlot],
                y + CONTENT_SLOT_Y);
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
    }

    private static EnumHand getHeldHandbagHand(EntityPlayer player) {
        ItemStack mainHand = player.getHeldItemMainhand();
        if (mainHand.getItem() == ItemRegistry.HANDBAG) return EnumHand.MAIN_HAND;

        ItemStack offHand = player.getHeldItemOffhand();
        return offHand.getItem() == ItemRegistry.HANDBAG ? EnumHand.OFF_HAND : null;
    }
}
