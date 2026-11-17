package com.buildershandbag.integration.jei;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.wrapper.ICustomCraftingRecipeWrapper;

import com.buildershandbag.item.ItemRegistry;
import com.buildershandbag.recipe.HandbagCopyRecipe;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Crafting preview with tooltips for the source, target, and copied configurations.
 */
@SideOnly(Side.CLIENT)
public final class HandbagCopyRecipeWrapper implements ICustomCraftingRecipeWrapper {

    private static final int OUTPUT_SLOT = 0;
    private static final int SOURCE_SLOT = 4;
    private static final int TARGET_SLOT = 5;
    private static final int TITLE_COLOR = 0x404040;

    private final HandbagCopyRecipe recipe;

    public HandbagCopyRecipeWrapper(HandbagCopyRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputs(VanillaTypes.ITEM,
            Arrays.asList(new ItemStack(ItemRegistry.HANDBAG), new ItemStack(ItemRegistry.HANDBAG)));
        ingredients.setOutput(VanillaTypes.ITEM, new ItemStack(ItemRegistry.HANDBAG));
    }

    @Override
    public void setRecipe(IRecipeLayout layout, IIngredients ingredients) {
        IGuiItemStackGroup stacks = layout.getItemStacks();
        List<List<ItemStack>> inputs = ingredients.getInputs(VanillaTypes.ITEM);
        ItemStack source = inputs.get(0).get(0);
        ItemStack target = inputs.get(1).get(0);
        ItemStack output = ingredients.getOutputs(VanillaTypes.ITEM).get(0).get(0);

        IFocus<?> focus = layout.getFocus();
        if (focus != null && focus.getValue() instanceof ItemStack) {
            ItemStack focused = (ItemStack) focus.getValue();
            if (focused.getItem() == ItemRegistry.HANDBAG && HandbagStorage.hasConfigurations(focused)) {
                source = focused.copy();
                output = HandbagStorage.copyConfigurations(source, target);
            }
        }

        stacks.set(SOURCE_SLOT, source);
        stacks.set(TARGET_SLOT, target);
        stacks.set(OUTPUT_SLOT, output);

        stacks.addTooltipCallback((slot, input, stack, tooltip) -> {
            tooltip.add("");
            if (slot == SOURCE_SLOT) {
                tooltip.add(TextFormatting.YELLOW + I18n.format("jei.buildershandbag.copy.source"));
            } else if (slot == TARGET_SLOT) {
                tooltip.add(TextFormatting.YELLOW + I18n.format("jei.buildershandbag.copy.target"));
            } else if (slot == OUTPUT_SLOT) {
                tooltip.add(TextFormatting.YELLOW + I18n.format("jei.buildershandbag.copy.result"));
            }
        });

        layout.setShapeless();
    }

    @Override
    @Nullable
    public ResourceLocation getRegistryName() {
        return recipe.getRegistryName();
    }
}
