package com.buildershandbag.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * Applies the finalized BlockCraftery tile data, then schedules its chunk for
 * rebuilding. This is kept dependency-free so the packet doesn't crash
 * when BlockCraftery is not present.
 */
public class PacketBlockcrafteryTileSync implements IMessage {

    private BlockPos position;
    private NBTTagCompound data;

    public PacketBlockcrafteryTileSync() {
        position = BlockPos.ORIGIN;
        data = new NBTTagCompound();
    }

    public PacketBlockcrafteryTileSync(BlockPos position, NBTTagCompound data) {
        this.position = position.toImmutable();
        this.data = data == null ? new NBTTagCompound() : data.copy();
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        position = BlockPos.fromLong(buffer.readLong());
        NBTTagCompound received = ByteBufUtils.readTag(buffer);
        data = received == null ? new NBTTagCompound() : received;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(position.toLong());
        ByteBufUtils.writeTag(buffer, data);
    }

    public static class Handler implements IMessageHandler<PacketBlockcrafteryTileSync, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketBlockcrafteryTileSync message, MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() -> apply(message));
            return null;
        }

        @SideOnly(Side.CLIENT)
        private static void apply(PacketBlockcrafteryTileSync message) {
            World world = Minecraft.getMinecraft().world;
            if (world == null || !world.isBlockLoaded(message.position)) return;

            TileEntity tile = world.getTileEntity(message.position);
            if (tile == null) return;

            tile.handleUpdateTag(message.data);
            world.markBlockRangeForRenderUpdate(message.position, message.position);
        }
    }
}
