package com.buildershandbag;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import com.buildershandbag.container.GuiHandler;
import com.buildershandbag.block.BlockRegistry;
import com.buildershandbag.item.ItemRegistry;
import com.buildershandbag.network.HandbagNetwork;


@Mod(
    modid = Tags.MODID,
    name = Tags.MODNAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.12.2]"
)
public class Handbag {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MODID);

    @SidedProxy(
        clientSide = "com.buildershandbag.ClientProxy",
        serverSide = "com.buildershandbag.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.Instance(Tags.MODID)
    public static Handbag instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        BlockRegistry.init();
        ItemRegistry.init();
        HandbagNetwork.init();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());
        proxy.init(event);
    }
}
