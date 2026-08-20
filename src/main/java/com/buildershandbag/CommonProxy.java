package com.buildershandbag;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.buildershandbag.integration.Ae2Integration;


public class CommonProxy {

    private static final String AE2_MODID = "appliedenergistics2";

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
        if (Loader.isModLoaded(AE2_MODID)) registerWirelessHandler();
    }

    @Optional.Method(modid = AE2_MODID)
    private static void registerWirelessHandler() {
        Ae2Integration.registerWirelessHandler();
    }
}
