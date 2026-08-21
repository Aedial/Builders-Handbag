package com.buildershandbag;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.buildershandbag.client.render.HandbagItemStackRenderer;
import com.buildershandbag.client.render.RenderHandbag;
import com.buildershandbag.config.HandbagClientConfig;
import com.buildershandbag.item.ItemRegistry;
import com.buildershandbag.tile.TileHandbag;


public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        HandbagClientConfig.refreshRenderingColors();
        ClientRegistry.bindTileEntitySpecialRenderer(TileHandbag.class, new RenderHandbag());
        ItemRegistry.HANDBAG.setTileEntityItemStackRenderer(new HandbagItemStackRenderer());
    }
}
