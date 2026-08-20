package com.buildershandbag.block;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import com.buildershandbag.Tags;
import com.buildershandbag.tile.TileHandbag;


/**
 * Registry for the placed handbag and its tile entity.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class BlockRegistry {

    public static final BlockHandbag HANDBAG = new BlockHandbag();

    private static boolean tileEntityRegistered;

    private BlockRegistry() {
    }

    public static void init() {
        if (tileEntityRegistered) return;

        GameRegistry.registerTileEntity(TileHandbag.class, new ResourceLocation(Tags.MODID, "handbag"));
        tileEntityRegistered = true;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(HANDBAG);
    }
}
