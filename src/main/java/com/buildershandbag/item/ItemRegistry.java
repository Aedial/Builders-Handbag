package com.buildershandbag.item;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.buildershandbag.Tags;


/**
 * Registry for Decoration Handbag items.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class ItemRegistry {

    public static ItemHandbag HANDBAG;

    private ItemRegistry() {
    }

    public static void init() {
        HANDBAG = new ItemHandbag();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(HANDBAG);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(
            HANDBAG,
            0,
            new ModelResourceLocation(HANDBAG.getRegistryName(), "inventory"));
    }
}
