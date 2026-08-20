package com.buildershandbag.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import com.buildershandbag.Tags;


/**
 * Network registration for Handbag actions and container state.
 */
public final class HandbagNetwork {

    public static SimpleNetworkWrapper INSTANCE;

    private HandbagNetwork() {
    }

    public static void init() {
        INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MODID);
        int packetId = 0;

        // Server -> client: synchronizes handbag NBT after a server-side mutation.
        INSTANCE.registerMessage(PacketHandbagSync.Handler.class, PacketHandbagSync.class, packetId++, Side.CLIENT);
        // Server -> client: supplies configuration options for the material slot.
        INSTANCE.registerMessage(PacketHandbagOptionsSync.Handler.class, PacketHandbagOptionsSync.class, packetId++, Side.CLIENT);
        // Server -> client: shows a localized overlay success or error message.
        INSTANCE.registerMessage(PacketOverlayMessage.Handler.class, PacketOverlayMessage.class, packetId++, Side.CLIENT);

        // Client -> server: removes a configuration and returns its stored material.
        INSTANCE.registerMessage(PacketRemoveHandbagConfiguration.Handler.class, PacketRemoveHandbagConfiguration.class, packetId++, Side.SERVER);
        // Client -> server: moves a configuration by insertion within the ordered list.
        INSTANCE.registerMessage(PacketMoveHandbagConfiguration.Handler.class, PacketMoveHandbagConfiguration.class, packetId++, Side.SERVER);
        // Client -> server: adds a validated material/result configuration.
        INSTANCE.registerMessage(PacketAddHandbagConfiguration.Handler.class, PacketAddHandbagConfiguration.class, packetId++, Side.SERVER);
        // Client -> server: cycles the selected configuration for sneak-scroll.
        INSTANCE.registerMessage(PacketCycleHandbagConfiguration.Handler.class, PacketCycleHandbagConfiguration.class, packetId++, Side.SERVER);
    }
}
