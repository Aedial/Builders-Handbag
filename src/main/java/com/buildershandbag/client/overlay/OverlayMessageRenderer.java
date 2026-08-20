package com.buildershandbag.client.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;


/**
 * Action-bar overlay renderer for displaying messages to the player.
 * This allows to show messages when a GUI is open, and the chat is not visible.
 */
public final class OverlayMessageRenderer {

    private static final int BORDER_COLOR = 0xFF555555;
    private static final float BACKGROUND_OPACITY = 0.75F;

    private static OverlayMessage currentMessage;

    private OverlayMessageRenderer() {
    }

    public static void setMessage(String text, MessageType type) {
        currentMessage = new OverlayMessage(text, type);
    }

    public static void render(int screenWidth, int screenHeight) {
        if (currentMessage == null) return;
        if (currentMessage.isExpired()) {
            currentMessage = null;
            return;
        }

        float alpha = currentMessage.getAlpha();
        if (alpha <= 0.01F) return;

        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        String text = currentMessage.getText();
        int textWidth = fontRenderer.getStringWidth(text);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 59;
        int alphaByte = (int) (alpha * 255.0F);
        int textColor = alphaByte << 24 | currentMessage.getType().getColor();
        int background = (int) (alphaByte * BACKGROUND_OPACITY) << 24;
        int border = (int) (alphaByte * BACKGROUND_OPACITY) << 24 | BORDER_COLOR & 0x00FFFFFF;

        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();

        int padding = 4;
        int left = x - padding;
        int top = y - padding;
        int right = x + textWidth + padding;
        int bottom = y + fontRenderer.FONT_HEIGHT + padding;
        Gui.drawRect(left - 1, top - 1, right + 1, bottom + 1, border);
        Gui.drawRect(left, top, right, bottom, background);

        GlStateManager.enableTexture2D();
        fontRenderer.drawStringWithShadow(text, x, y, textColor);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
