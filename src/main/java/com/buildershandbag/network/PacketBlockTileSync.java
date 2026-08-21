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
 * Applies the finalized tile data, then schedules its chunk for rebuilding.
 * This is kept dependency-free so any integration can use it for forcing
 * a render update after placing a block with a tile entity.
 */
public class PacketBlockTileSync implements IMessage {

    private BlockPos position;
    private NBTTagCompound data;

    public PacketBlockTileSync() {
        position = BlockPos.ORIGIN;
        data = new NBTTagCompound();
    }

    public PacketBlockTileSync(BlockPos position, NBTTagCompound data) {
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

    public static class Handler implements IMessageHandler<PacketBlockTileSync, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketBlockTileSync message, MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() -> apply(message));
            return null;
        }

        @SideOnly(Side.CLIENT)
        private static void apply(PacketBlockTileSync message) {
            World world = Minecraft.getMinecraft().world;
            if (world == null || !world.isBlockLoaded(message.position)) return;

            TileEntity tile = world.getTileEntity(message.position);
            if (tile == null) return;

            tile.handleUpdateTag(message.data);
            world.markBlockRangeForRenderUpdate(message.position, message.position);
        }
    }
}
