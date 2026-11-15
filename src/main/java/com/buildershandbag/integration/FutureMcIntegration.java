package com.buildershandbag.integration;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import thedarkcolour.futuremc.config.FConfig;
import thedarkcolour.futuremc.recipe.SimpleRecipe;
import thedarkcolour.futuremc.recipe.stonecutter.StonecutterRecipes;

import com.buildershandbag.storage.HandbagConfiguration;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Future-MC stonecutter recipes for block configurations and world picking.
 */
public final class FutureMcIntegration {

    private FutureMcIntegration() {
    }

    public static boolean isEnabled() {
        FConfig.VillageAndPillage.Stonecutter config = FConfig.INSTANCE.getVillageAndPillage().stonecutter;
        return config.enabled && config.functionality;
    }

    public static List<HandbagConfigurationOption> getOptions(ItemStack material) {
        List<HandbagConfigurationOption> options = new ArrayList<>();
        for (SimpleRecipe recipe : StonecutterRecipes.INSTANCE.getRecipes(material)) {
            ItemStack result = recipe.getOutput();
            if (!HandbagConfigurationProvider.isConfigurationMaterial(result)) continue;

            HandbagConfigurationProvider.addOption(options,
                new HandbagConfigurationOption(result, HandbagIntegration.FUTUREMC, result.getCount()));
        }

        return options;
    }

    @Nullable
    public static HandbagConfiguration pickConfiguration(ItemStack result) {
        for (SimpleRecipe recipe : StonecutterRecipes.INSTANCE.getRecipes()) {
            if (!HandbagStorage.sameMaterial(recipe.getOutput(), result)) continue;

            for (ItemStack material : recipe.getInput().getMatchingStacks()) {
                if (!HandbagConfigurationProvider.isConfigurationMaterial(material)
                        || HandbagStorage.sameMaterial(material, result)
                        || !recipe.getInput().apply(material)) continue;

                for (HandbagConfigurationOption option : FutureMcRecipeCache.getOptions(material)) {
                    if (HandbagStorage.sameMaterial(option.getResult(), result)) {
                        return option.createConfiguration(material);
                    }
                }
            }
        }

        return null;
    }
}
