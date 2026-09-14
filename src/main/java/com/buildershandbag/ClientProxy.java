package com.buildershandbag;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

import com.buildershandbag.client.render.HandbagItemStackRenderer;
import com.buildershandbag.client.render.RenderHandbag;
import com.buildershandbag.config.HandbagClientConfig;
import com.buildershandbag.item.ItemRegistry;
import com.buildershandbag.tile.TileHandbag;


public class ClientProxy extends CommonProxy {

    private final RenderHandbag handbagRenderer = new RenderHandbag();

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        HandbagClientConfig.refreshRenderingColors();
        ClientRegistry.bindTileEntitySpecialRenderer(TileHandbag.class, handbagRenderer);
        MinecraftForge.EVENT_BUS.register(handbagRenderer);
        ItemRegistry.HANDBAG.setTileEntityItemStackRenderer(new HandbagItemStackRenderer());
    }
}
