package com.buildershandbag.recipe;

import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.buildershandbag.Tags;


/**
 * Registration for the handbag's custom crafting recipes.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class RecipeRegistry {

    private RecipeRegistry() {
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new HandbagCopyRecipe());
    }
}
