package com.buildershandbag.gui;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.buildershandbag.Tags;
import com.buildershandbag.item.HandbagMode;


/**
 * The 12-pixel control for the handbag's current placement mode.
 */
@SideOnly(Side.CLIENT)
public final class HandbagModeButton extends GuiButton {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID,
        "textures/guis/modes.png");
    private static final int BUTTON_SIZE = 12;
    private static final int TEXTURE_CELL_SIZE = 24;
    private static final int TEXTURE_WIDTH = 128;
    private static final int TEXTURE_HEIGHT = 64;

    private final Supplier<HandbagMode> mode;

    public HandbagModeButton(int buttonId, Supplier<HandbagMode> mode) {
        super(buttonId, 0, 0, BUTTON_SIZE, BUTTON_SIZE, "");
        this.mode = mode;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public HandbagMode getMode() {
        return mode.get();
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        int textureX = getMode().getTextureColumn() * TEXTURE_CELL_SIZE;
        int textureY = hovered ? TEXTURE_CELL_SIZE : 0;

        mc.getTextureManager().bindTexture(TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        drawScaledCustomSizeModalRect(x, y, textureX, textureY,
            TEXTURE_CELL_SIZE, TEXTURE_CELL_SIZE, width, height, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        mouseDragged(mc, mouseX, mouseY);
    }
}
