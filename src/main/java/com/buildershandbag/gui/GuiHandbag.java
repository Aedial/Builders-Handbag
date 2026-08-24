package com.buildershandbag.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.buildershandbag.Tags;
import com.buildershandbag.client.overlay.OverlayMessageRenderer;
import com.buildershandbag.client.render.BlockcrafteryPreviewModel;
import com.buildershandbag.container.ContainerHandbag;
import com.buildershandbag.container.HandbagLayout;
import com.buildershandbag.integration.HandbagConfigurationOption;
import com.buildershandbag.integration.HandbagConfigurationProvider;
import com.buildershandbag.integration.HandbagIntegration;
import com.buildershandbag.network.HandbagNetwork;
import com.buildershandbag.network.PacketAddHandbagConfiguration;
import com.buildershandbag.network.PacketMoveHandbagConfiguration;
import com.buildershandbag.network.PacketRemoveHandbagConfiguration;
import com.buildershandbag.storage.HandbagConfiguration;
import com.buildershandbag.storage.HandbagStorage;


/**
 * The 176x248 handbag GUI, split in 3 sections :
 * <ul>
 *   <li>4x9 configuration slots (client-rendered)</li>
 *   <li>1 block slot + 3x7 option slots (server-rendered)</li>
 *   <li>player inventory slots</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public class GuiHandbag extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(Tags.MODID,
        "textures/guis/handbag.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 248;
    private static final int SLOT_SIZE = HandbagLayout.SLOT_SIZE;

    private static final int CONFIGURATION_X = 7;
    private static final int CONFIGURATION_Y = 7;
    private static final int CONFIGURATION_COLUMNS = HandbagLayout.CONFIGURATION_COLUMNS;
    private static final int CONFIGURATION_ROWS = HandbagLayout.CONFIGURATION_ROWS;

    private static final int MATERIAL_X = 8;
    private static final int MATERIAL_Y = 112;

    private static final int OPTION_X = 43;
    private static final int OPTION_Y = 93;
    private static final int OPTION_COLUMNS = 7;
    private static final int OPTION_ROWS = 3;
    private static final int OPTIONS_PER_PAGE = OPTION_COLUMNS * OPTION_ROWS;
    private static final int PAGE_Y = 149;
    private static final int PAGE_HEIGHT = 14;
    private static final int PAGE_BUTTON_WIDTH = 12;
    private static final int PREVIOUS_PAGE_BUTTON = 0;
    private static final int NEXT_PAGE_BUTTON = 1;
    private static final int RIGHT_BUTTON_X = OPTION_X + OPTION_COLUMNS * SLOT_SIZE - PAGE_BUTTON_WIDTH;

    /**
     * A vanilla button whose lower edge remains visible when its height is less
     * than the 20-pixel height of the vanilla button texture.
     */
    private static final class SmallVanillaButton extends GuiButton {

        private SmallVanillaButton(int buttonId, int x, int y, int width, int height, String text) {
            super(buttonId, x, y, width, height, text);
        }

        private SmallVanillaButton(int buttonId, int width, int height, String text) {
            this(buttonId, 0, 0, width, height, text);
        }

        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;

            FontRenderer fontRenderer = mc.fontRenderer;
            mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            int hoverState = getHoverState(hovered);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
            GlStateManager.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

            int leftWidth = width / 2;
            int rightWidth = width - leftWidth;
            int bottomHeight = Math.min(2, height);
            int middleHeight = height - bottomHeight;
            int textureY = 46 + hoverState * 20;

            drawTexturedModalRect(x, y, 0, textureY, leftWidth, middleHeight);
            drawTexturedModalRect(x + leftWidth, y, 200 - rightWidth, textureY, rightWidth, middleHeight);
            drawTexturedModalRect(x, y + middleHeight, 0, textureY + 20 - bottomHeight, leftWidth, bottomHeight);
            drawTexturedModalRect(
                x + leftWidth,
                y + middleHeight,
                200 - rightWidth,
                textureY + 20 - bottomHeight,
                rightWidth,
                bottomHeight);

            mouseDragged(mc, mouseX, mouseY);

            int textColor = 0xE0E0E0;
            if (packedFGColour != 0) {
                textColor = packedFGColour;
            } else if (!enabled) {
                textColor = 0xA0A0A0;
            } else if (hovered) {
                textColor = 0xFFFFA0;
            }
            drawCenteredString(fontRenderer, displayString, x + width / 2, y + (height - 8) / 2, textColor);
        }
    }

    /** Current page of options being displayed. */
    private int optionPage;
    /** Client-only selection used to choose the configuration being reordered. */
    private int movingConfiguration = -1;

    private final ContainerHandbag container;
    private SmallVanillaButton previousPageButton = new SmallVanillaButton(
        PREVIOUS_PAGE_BUTTON, PAGE_BUTTON_WIDTH, PAGE_HEIGHT, "<");
    private SmallVanillaButton nextPageButton = new SmallVanillaButton(
        NEXT_PAGE_BUTTON, PAGE_BUTTON_WIDTH, PAGE_HEIGHT, ">");


    public GuiHandbag(ContainerHandbag container) {
        super(container);
        this.container = container;
        xSize = GUI_WIDTH;
        ySize = GUI_HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();

        previousPageButton.setPosition(guiLeft + OPTION_X, guiTop + PAGE_Y);
        nextPageButton.setPosition(guiLeft + RIGHT_BUTTON_X, guiTop + PAGE_Y);

        buttonList.add(previousPageButton);
        buttonList.add(nextPageButton);

        updatePageButtons();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawDefaultBackground();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        ItemStack handbag = getHandbagStack();
        drawConfigurations(handbag);
        drawOptions();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawHoveredTooltip(mouseX, mouseY);
        OverlayMessageRenderer.render(width, height);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int localX = mouseX - guiLeft;
        int localY = mouseY - guiTop;

        int configurationIndex = getConfigurationIndex(localX, localY);
        if (configurationIndex >= 0) {
            handleConfigurationClick(configurationIndex, mouseButton);
            return;
        }

        int optionIndex = getOptionIndex(localX, localY);
        if (optionIndex >= 0 && mouseButton == 0) {
            List<HandbagConfigurationOption> options = container.getClientOptions();
            int absoluteOption = optionPage * OPTIONS_PER_PAGE + optionIndex;
            if (absoluteOption < options.size()) {
                HandbagNetwork.INSTANCE.sendToServer(new PacketAddHandbagConfiguration(
                    container.getHand(),
                    options.get(absoluteOption)));
            }
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == PREVIOUS_PAGE_BUTTON) {
            optionPage = Math.max(0, optionPage - 1);
            updatePageButtons();
            return;
        }

        if (button.id == NEXT_PAGE_BUTTON) {
            optionPage = Math.min(getOptionPageCount() - 1, optionPage + 1);
            updatePageButtons();
            return;
        }

        super.actionPerformed(button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        ItemStack handbag = getHandbagStack();

        int size = HandbagStorage.getConfigurations(handbag).size();
        int target = getMoveTarget(keyCode, movingConfiguration, size);
        if (target != Integer.MIN_VALUE) {
            if (movingConfiguration >= 0 && target != movingConfiguration) {
                HandbagNetwork.INSTANCE.sendToServer(
                    new PacketMoveHandbagConfiguration(container.getHand(), movingConfiguration, target));
                movingConfiguration = target;
            }
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void drawConfigurations(ItemStack handbag) {
        List<HandbagConfiguration> configurations = HandbagStorage.getConfigurations(handbag);

        for (int index = 0; index < configurations.size(); index++) {
            HandbagConfiguration configuration = configurations.get(index);
            int x = CONFIGURATION_X + index % CONFIGURATION_COLUMNS * SLOT_SIZE;
            int y = CONFIGURATION_Y + index / CONFIGURATION_COLUMNS * SLOT_SIZE;
            String count = configuration.getMaterialCount() > 0 ? String.valueOf(configuration.getMaterialCount()) : "";

            renderStack(
                configuration.getResult(),
                configuration.getMaterial(),
                configuration.getIntegration(),
                x + 1,
                y + 1,
                count);
            if (movingConfiguration == index) drawSelectedWireframe(x, y);
        }
    }

    private void drawOptions() {
        List<HandbagConfigurationOption> options = container.getClientOptions();
        optionPage = Math.max(0, Math.min(optionPage, getOptionPageCount() - 1));
        updatePageButtons();

        int firstOption = optionPage * OPTIONS_PER_PAGE;
        for (int displayIndex = 0; displayIndex < OPTIONS_PER_PAGE; displayIndex++) {
            int optionIndex = firstOption + displayIndex;
            if (optionIndex >= options.size()) break;

            int x = OPTION_X + displayIndex % OPTION_COLUMNS * SLOT_SIZE;
            int y = OPTION_Y + displayIndex / OPTION_COLUMNS * SLOT_SIZE;
            HandbagConfigurationOption option = options.get(optionIndex);
            renderStack(
                option.getResult(),
                container.getConfigurationMaterial(),
                option.getIntegration(),
                x + 1,
                y + 1,
                "");
        }

        if (options.size() > OPTIONS_PER_PAGE) {
            String pageText = (optionPage + 1) + "/" + getOptionPageCount();
            int pageTextWidth = fontRenderer.getStringWidth(pageText);
            int pageTextX = OPTION_X + (OPTION_COLUMNS * SLOT_SIZE - pageTextWidth) / 2;
            int pageTextY = PAGE_Y + (PAGE_HEIGHT - fontRenderer.FONT_HEIGHT) / 2;
            fontRenderer.drawString(pageText, pageTextX, pageTextY, 0xFF000000);
        }
    }

    private void handleConfigurationClick(int index, int mouseButton) {
        List<HandbagConfiguration> configurations = HandbagStorage.getConfigurations(getHandbagStack());
        if (index >= configurations.size()) return;

        if (mouseButton == 0) {
            movingConfiguration = movingConfiguration == index ? -1 : index;
        } else if (mouseButton == 1) {
            HandbagNetwork.INSTANCE.sendToServer(new PacketRemoveHandbagConfiguration(container.getHand(), index));
            if (movingConfiguration == index) {
                movingConfiguration = -1;
            } else if (movingConfiguration > index) {
                movingConfiguration--;
            }
        }
    }

    private int getMoveTarget(int keyCode, int selected, int size) {
        if (selected < 0 || size <= 0) return Integer.MIN_VALUE;

        if (keyCode == Keyboard.KEY_LEFT) return Math.max(0, selected - 1);
        if (keyCode == Keyboard.KEY_RIGHT) return Math.min(size - 1, selected + 1);
        if (keyCode == Keyboard.KEY_UP) return Math.max(0, selected - CONFIGURATION_COLUMNS);
        if (keyCode == Keyboard.KEY_DOWN) return Math.min(size - 1, selected + CONFIGURATION_COLUMNS);
        if (keyCode == Keyboard.KEY_HOME || keyCode == Keyboard.KEY_PRIOR) return 0;
        if (keyCode == Keyboard.KEY_END || keyCode == Keyboard.KEY_NEXT) return size - 1;

        return Integer.MIN_VALUE;
    }

    private int getConfigurationIndex(int x, int y) {
        if (x < CONFIGURATION_X || x >= CONFIGURATION_X + CONFIGURATION_COLUMNS * SLOT_SIZE
                || y < CONFIGURATION_Y || y >= CONFIGURATION_Y + CONFIGURATION_ROWS * SLOT_SIZE) {
            return -1;
        }

        return (y - CONFIGURATION_Y) / SLOT_SIZE * CONFIGURATION_COLUMNS + (x - CONFIGURATION_X) / SLOT_SIZE;
    }

    private int getOptionIndex(int x, int y) {
        if (x < OPTION_X || x >= OPTION_X + OPTION_COLUMNS * SLOT_SIZE
                || y < OPTION_Y || y >= OPTION_Y + OPTION_ROWS * SLOT_SIZE) {
            return -1;
        }

        return (y - OPTION_Y) / SLOT_SIZE * OPTION_COLUMNS + (x - OPTION_X) / SLOT_SIZE;
    }

    private int getOptionPageCount() {
        return Math.max(1, (container.getClientOptions().size() + OPTIONS_PER_PAGE - 1) / OPTIONS_PER_PAGE);
    }

    private void updatePageButtons() {
        if (previousPageButton == null || nextPageButton == null) return;

        boolean paged = container.getClientOptions().size() > OPTIONS_PER_PAGE;
        previousPageButton.visible = paged;
        previousPageButton.enabled = paged && optionPage > 0;
        nextPageButton.visible = paged;
        nextPageButton.enabled = paged && optionPage < getOptionPageCount() - 1;
    }

    private ItemStack getHandbagStack() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        return player == null ? ItemStack.EMPTY : player.getHeldItem(container.getHand());
    }

    private void renderStack(ItemStack stack, ItemStack material, HandbagIntegration integration, int x, int y,
            String overlay) {
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();

        if (!renderBlockcrafteryPreview(stack, material, integration, x, y)) {
            itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        }

        itemRender.renderItemOverlayIntoGUI(fontRenderer, stack, x, y, overlay);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private boolean renderBlockcrafteryPreview(ItemStack frame, ItemStack material, HandbagIntegration integration,
            int x, int y) {
        if (integration != HandbagIntegration.BLOCKCRAFTERY || !HandbagIntegration.BLOCKCRAFTERY.isModLoaded()) return false;

        return renderBlockcrafteryPreview(frame, material, x, y);
    }

    @Optional.Method(modid = HandbagIntegration.BLOCKCRAFTERY_MODID)
    private boolean renderBlockcrafteryPreview(ItemStack frame, ItemStack material, int x, int y) {
        BlockcrafteryPreviewModel.Preview preview = BlockcrafteryPreviewModel.create(itemRender, frame, material);
        if (preview == null) return false;

        itemRender.renderItemModelIntoGUI(preview.getRenderStack(), x, y, preview.getModel());
        return true;
    }

    /**
     * Draws a green open wireframe over the configuration selected for moving.
     */
    private void drawSelectedWireframe(int x, int y) {
        int color = 0xFF55FF55;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawRect(x, y, x + 6, y + 1, color);
        Gui.drawRect(x + 12, y, x + 18, y + 1, color);
        Gui.drawRect(x, y + 17, x + 6, y + 18, color);
        Gui.drawRect(x + 12, y + 17, x + 18, y + 18, color);
        Gui.drawRect(x, y, x + 1, y + 6, color);
        Gui.drawRect(x, y + 12, x + 1, y + 18, color);
        Gui.drawRect(x + 17, y, x + 18, y + 6, color);
        Gui.drawRect(x + 17, y + 12, x + 18, y + 18, color);
    }

    private void drawHoveredTooltip(int mouseX, int mouseY) {
        if (drawFakeSlotTooltip(mouseX, mouseY)) return;

        drawInventorySlotTooltip(mouseX, mouseY);
    }

    private boolean drawFakeSlotTooltip(int mouseX, int mouseY) {
        int localX = mouseX - guiLeft;
        int localY = mouseY - guiTop;

        int configurationIndex = getConfigurationIndex(localX, localY);
        List<HandbagConfiguration> configurations = HandbagStorage.getConfigurations(getHandbagStack());
        if (configurationIndex >= 0 && configurationIndex < configurations.size()) {
            HandbagConfiguration configuration = configurations.get(configurationIndex);
            drawConfigurationTooltip(configuration, configurationIndex, mouseX, mouseY);
            return true;
        }

        int optionIndex = getOptionIndex(localX, localY);
        int absoluteOption = optionPage * OPTIONS_PER_PAGE + optionIndex;
        if (optionIndex >= 0 && absoluteOption < container.getClientOptions().size()) {
            drawOptionTooltip(container.getClientOptions().get(absoluteOption), mouseX, mouseY);
            return true;
        }

        if (localX >= MATERIAL_X && localX < MATERIAL_X + SLOT_SIZE
                && localY >= MATERIAL_Y && localY < MATERIAL_Y + SLOT_SIZE) {
            drawMaterialTooltip(mouseX, mouseY);
            return true;
        }

        if (previousPageButton.isMouseOver()) {
            String text = I18n.format("gui.buildershandbag.page.previous");
            drawHoveringText(Collections.singletonList(text), mouseX, mouseY);
            return true;
        }

        if (nextPageButton.isMouseOver()) {
            String text = I18n.format("gui.buildershandbag.page.next");
            drawHoveringText(Collections.singletonList(text), mouseX, mouseY);
            return true;
        }

        return false;
    }

    private void drawInventorySlotTooltip(int mouseX, int mouseY) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || !player.inventory.getItemStack().isEmpty()) return;

        Slot slot = getSlotAtPosition(mouseX, mouseY);
        if (slot == null || !slot.getHasStack()) return;

        List<String> lines = getItemTooltip(slot.getStack());
        if (slot.slotNumber != 0 && HandbagConfigurationProvider.isConfigurationMaterial(slot.getStack())) {
            lines.add("");
            lines.add(I18n.format("gui.buildershandbag.material.shift_click_block"));
        }

        drawHoveringText(lines, mouseX, mouseY);
    }

    private void drawConfigurationTooltip(HandbagConfiguration configuration, int index, int mouseX, int mouseY) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        List<String> lines = getItemTooltip(configuration.getResult());
        lines.add("");
        lines.add(I18n.format(
            "gui.buildershandbag.configuration.stored",
            configuration.getMaterialCount(),
            configuration.getMaterial().getDisplayName()));

        if (movingConfiguration == index) {
            lines.add(I18n.format("gui.buildershandbag.configuration.deselect"));
            lines.add(I18n.format("gui.buildershandbag.configuration.remove"));
            lines.add(I18n.format("gui.buildershandbag.configuration.move"));
            lines.add(I18n.format("gui.buildershandbag.configuration.move_edge"));
        } else {
            lines.add(I18n.format("gui.buildershandbag.configuration.select"));
            lines.add(I18n.format("gui.buildershandbag.configuration.remove"));
        }

        drawHoveringText(lines, mouseX, mouseY);
    }

    private void drawOptionTooltip(HandbagConfigurationOption option, int mouseX, int mouseY) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        List<String> lines = getItemTooltip(option.getResult());
        lines.add("");
        lines.add(I18n.format("gui.buildershandbag.option.add"));

        drawHoveringText(lines, mouseX, mouseY);
    }

    private void drawMaterialTooltip(int mouseX, int mouseY) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        List<String> lines = new ArrayList<>();
        ItemStack material = container.getConfigurationMaterial();
        if (material.isEmpty()) {
            lines.add(I18n.format("gui.buildershandbag.material.title"));
            lines.add("");
            lines.add(I18n.format("gui.buildershandbag.material.description"));
            lines.add(I18n.format("gui.buildershandbag.material.shift_click"));
        } else {
            lines.addAll(getItemTooltip(material));
            lines.add("");
            lines.add(I18n.format("gui.buildershandbag.material.choose_option"));
        }

        drawHoveringText(lines, mouseX, mouseY);
    }

    private List<String> getItemTooltip(ItemStack stack) {
        return new ArrayList<>(getItemToolTip(stack));
    }
}
