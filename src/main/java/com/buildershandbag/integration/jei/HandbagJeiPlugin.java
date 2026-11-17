package com.buildershandbag.integration.jei;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;

import com.buildershandbag.recipe.HandbagCopyRecipe;


/**
 * Optional JEI support for the handbag's custom crafting recipes.
 */
@JEIPlugin
@SideOnly(Side.CLIENT)
public final class HandbagJeiPlugin implements IModPlugin {

    @Override
    public void register(IModRegistry registry) {
        registry.handleRecipes(HandbagCopyRecipe.class, HandbagCopyRecipeWrapper::new,
            VanillaRecipeCategoryUid.CRAFTING);
    }
}
