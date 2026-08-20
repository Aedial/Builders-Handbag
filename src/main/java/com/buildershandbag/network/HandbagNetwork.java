package com.buildershandbag.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import com.buildershandbag.Tags;


/**
 * Network registration for Handbag actions, container state, and deferred
 * placed-tile render synchronization.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class HandbagNetwork {

    public static SimpleNetworkWrapper INSTANCE;
    private static final List<PendingTileSync> PENDING_TILE_SYNCS = new ArrayList<>();

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

        // Server -> client: applies placed tile data and refreshes its rendered chunk.
        INSTANCE.registerMessage(PacketBlockTileSync.Handler.class, PacketBlockTileSync.class, packetId++, Side.CLIENT);
    }

    /**
     * Defers the render sync until the end of this server tick, after vanilla has sent
     * the placed block state to nearby clients.
     */
    public static void syncPlacedTile(World world, BlockPos position, TileEntity tile) {
        if (world.isRemote) return;

        PENDING_TILE_SYNCS.add(new PendingTileSync(
            world,
            position.toImmutable(),
            tile,
            tile.getUpdateTag().copy()));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_TILE_SYNCS.isEmpty()) return;

        for (PendingTileSync sync : PENDING_TILE_SYNCS) {
            if (sync.world.getTileEntity(sync.position) != sync.tile) continue;

            INSTANCE.sendToAllTracking(
                new PacketBlockTileSync(sync.position, sync.data),
                new NetworkRegistry.TargetPoint(
                    sync.world.provider.getDimension(),
                    sync.position.getX() + 0.5D,
                    sync.position.getY() + 0.5D,
                    sync.position.getZ() + 0.5D,
                    0.0D));
        }
        PENDING_TILE_SYNCS.clear();
    }

    private static final class PendingTileSync {

        private final World world;
        private final BlockPos position;
        private final TileEntity tile;
        private final NBTTagCompound data;

        private PendingTileSync(World world, BlockPos position, TileEntity tile, NBTTagCompound data) {
            this.world = world;
            this.position = position;
            this.tile = tile;
            this.data = data;
        }
    }
}
