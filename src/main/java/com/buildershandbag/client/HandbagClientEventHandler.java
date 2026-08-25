package com.buildershandbag.client;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import com.buildershandbag.Tags;
import com.buildershandbag.client.overlay.OverlayMessageRenderer;
import com.buildershandbag.client.render.BlockcrafteryPreviewModel;
import com.buildershandbag.config.HandbagClientConfig;
import com.buildershandbag.integration.HandbagIntegration;
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
    private static final float CONTENT_SMOOTHING_RATE = 18.0F;
    private static final float MAX_SMOOTHING_DELTA_SECONDS = 0.25F;

    private static final int[] interpolatedConfigurationIndices = new int[CONTENT_SLOT_X.length];
    private static final int[] nextConfigurationIndices = new int[CONTENT_SLOT_X.length];
    private static final float[] interpolatedContentX = new float[CONTENT_SLOT_X.length];
    private static final float[] nextContentX = new float[CONTENT_SLOT_X.length];
    private static int interpolatedConfigurationCount = -1;
    private static int interpolatedSelected = -1;
    private static long lastContentInterpolationTime;

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
        if (!HandbagClientConfig.rendering.renderContentsOverlay) {
            resetContentInterpolation();
            return;
        }

        EnumHand hand = getHeldHandbagHand(player);
        if (hand == null) {
            resetContentInterpolation();
            return;
        }

        ItemStack handbag = player.getHeldItem(hand);
        List<HandbagConfiguration> configurations = HandbagStorage.getConfigurations(handbag);
        int selected = HandbagStorage.getSelected(handbag);
        if (selected < 0) {
            resetContentInterpolation();
            return;
        }

        int displayedCount = Math.min(configurations.size(), CONTENT_SLOT_X.length);
        // Integer division gives the left side the extra configuration for even counts
        int leftCount = displayedCount / 2;
        int rightCount = displayedCount - leftCount - 1;
        updateContentInterpolation(configurations.size(), selected, leftCount, rightCount);
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

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        for (int relativeIndex = -leftCount; relativeIndex <= rightCount; relativeIndex++) {
            int configurationIndex = getConfigurationIndex(selected, relativeIndex, configurations.size());
            int contentSlot = SELECTED_CONTENT_SLOT + relativeIndex;
            HandbagConfiguration configuration = configurations.get(configurationIndex);

            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(interpolatedContentX[contentSlot] - CONTENT_SLOT_X[contentSlot], 0.0F, 0.0F);
                renderConfigurationResult(
                    minecraft.getRenderItem(),
                    configuration,
                    x + CONTENT_SLOT_X[contentSlot],
                    y + CONTENT_SLOT_Y);
            } finally {
                GlStateManager.popMatrix();
            }
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
    }

    private static void renderConfigurationResult(RenderItem itemRender, HandbagConfiguration configuration, int x,
            int y) {
        if (!renderBlockcrafteryPreview(itemRender, configuration, x, y)) {
            itemRender.renderItemAndEffectIntoGUI(configuration.getResult(), x, y);
        }
    }

    private static boolean renderBlockcrafteryPreview(RenderItem itemRender, HandbagConfiguration configuration,
            int x, int y) {
        if (configuration.getIntegration() != HandbagIntegration.BLOCKCRAFTERY
                || !HandbagIntegration.BLOCKCRAFTERY.isModLoaded()) {
            return false;
        }

        return renderBlockcrafteryPreview(itemRender, configuration.getResult(), configuration.getMaterial(), x, y);
    }

    @Optional.Method(modid = HandbagIntegration.BLOCKCRAFTERY_MODID)
    private static boolean renderBlockcrafteryPreview(RenderItem itemRender, ItemStack frame, ItemStack material,
            int x, int y) {
        BlockcrafteryPreviewModel.Preview preview = BlockcrafteryPreviewModel.create(itemRender, frame, material);
        if (preview == null) return false;

        itemRender.renderItemModelIntoGUI(preview.getRenderStack(), x, y, preview.getModel());
        return true;
    }

    private static void updateContentInterpolation(int configurationCount, int selected, int leftCount,
            int rightCount) {
        long currentTime = Minecraft.getSystemTime();
        if (interpolatedConfigurationCount != configurationCount || interpolatedSelected < 0) {
            resetContentInterpolation();
            for (int relativeIndex = -leftCount; relativeIndex <= rightCount; relativeIndex++) {
                int contentSlot = SELECTED_CONTENT_SLOT + relativeIndex;
                interpolatedConfigurationIndices[contentSlot] = getConfigurationIndex(
                    selected, relativeIndex, configurationCount);
                interpolatedContentX[contentSlot] = CONTENT_SLOT_X[contentSlot];
            }
        } else if (interpolatedSelected != selected) {
            updateChangedSelection(configurationCount, selected, leftCount, rightCount, currentTime);
        } else {
            float smoothingFactor = getSmoothingFactor(currentTime);
            for (int relativeIndex = -leftCount; relativeIndex <= rightCount; relativeIndex++) {
                int contentSlot = SELECTED_CONTENT_SLOT + relativeIndex;
                interpolatedContentX[contentSlot] = smoothTowards(
                    interpolatedContentX[contentSlot], CONTENT_SLOT_X[contentSlot], smoothingFactor);
            }
        }

        interpolatedConfigurationCount = configurationCount;
        interpolatedSelected = selected;
        lastContentInterpolationTime = currentTime;
    }

    private static void updateChangedSelection(int configurationCount, int selected, int leftCount,
            int rightCount, long currentTime) {
        Arrays.fill(nextConfigurationIndices, -1);
        int selectionChange = getSelectionChange(selected, interpolatedSelected, configurationCount);
        float smoothingFactor = getSmoothingFactor(currentTime);
        for (int relativeIndex = -leftCount; relativeIndex <= rightCount; relativeIndex++) {
            int contentSlot = SELECTED_CONTENT_SLOT + relativeIndex;
            int configurationIndex = getConfigurationIndex(selected, relativeIndex, configurationCount);
            int previousContentSlot = findInterpolatedContentSlot(configurationIndex);
            float initialX = previousContentSlot >= 0
                ? interpolatedContentX[previousContentSlot]
                : getContentSlotX(contentSlot + selectionChange);

            nextConfigurationIndices[contentSlot] = configurationIndex;
            nextContentX[contentSlot] = smoothTowards(initialX, CONTENT_SLOT_X[contentSlot], smoothingFactor);
        }

        System.arraycopy(nextConfigurationIndices, 0, interpolatedConfigurationIndices, 0, CONTENT_SLOT_X.length);
        System.arraycopy(nextContentX, 0, interpolatedContentX, 0, CONTENT_SLOT_X.length);
    }

    private static int getConfigurationIndex(int selected, int relativeIndex, int configurationCount) {
        return (selected + relativeIndex + configurationCount) % configurationCount;
    }

    private static int findInterpolatedContentSlot(int configurationIndex) {
        for (int contentSlot = 0; contentSlot < interpolatedConfigurationIndices.length; contentSlot++) {
            if (interpolatedConfigurationIndices[contentSlot] == configurationIndex) return contentSlot;
        }

        return -1;
    }

    private static int getSelectionChange(int selected, int previousSelected, int configurationCount) {
        int change = selected - previousSelected;
        int halfway = configurationCount / 2;
        if (change > halfway) return change - configurationCount;
        return change < -halfway ? change + configurationCount : change;
    }

    private static float getContentSlotX(int contentSlot) {
        if (contentSlot < 0) return CONTENT_SLOT_X[0] + contentSlot * (CONTENT_SLOT_X[1] - CONTENT_SLOT_X[0]);

        int finalContentSlot = CONTENT_SLOT_X.length - 1;
        if (contentSlot > finalContentSlot) {
            return CONTENT_SLOT_X[finalContentSlot]
                + (contentSlot - finalContentSlot)
                    * (CONTENT_SLOT_X[finalContentSlot] - CONTENT_SLOT_X[finalContentSlot - 1]);
        }

        return CONTENT_SLOT_X[contentSlot];
    }

    private static float getSmoothingFactor(long currentTime) {
        float elapsedSeconds = Math.min(
            MAX_SMOOTHING_DELTA_SECONDS,
            Math.max(0.0F, (currentTime - lastContentInterpolationTime) / 1000.0F));
        return 1.0F - (float) Math.exp(-CONTENT_SMOOTHING_RATE * elapsedSeconds);
    }

    private static float smoothTowards(float current, float target, float smoothingFactor) {
        return current + (target - current) * smoothingFactor;
    }

    private static void resetContentInterpolation() {
        Arrays.fill(interpolatedConfigurationIndices, -1);
        interpolatedConfigurationCount = -1;
        interpolatedSelected = -1;
        lastContentInterpolationTime = 0L;
    }

    private static EnumHand getHeldHandbagHand(EntityPlayer player) {
        ItemStack mainHand = player.getHeldItemMainhand();
        if (mainHand.getItem() == ItemRegistry.HANDBAG) return EnumHand.MAIN_HAND;

        ItemStack offHand = player.getHeldItemOffhand();
        return offHand.getItem() == ItemRegistry.HANDBAG ? EnumHand.OFF_HAND : null;
    }
}
