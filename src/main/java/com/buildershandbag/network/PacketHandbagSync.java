package com.buildershandbag.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.buildershandbag.item.ItemRegistry;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Synchronizes the handbag's NBT after a server-authoritative mutation.
 */
public class PacketHandbagSync implements IMessage {

    private EnumHand hand;
    private NBTTagCompound data;

    public PacketHandbagSync() {
        hand = EnumHand.MAIN_HAND;
        data = new NBTTagCompound();
    }

    public PacketHandbagSync(EnumHand hand, NBTTagCompound data) {
        this.hand = hand;
        this.data = data == null ? new NBTTagCompound() : data.copy();
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        hand = buffer.readBoolean() ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        NBTTagCompound received = ByteBufUtils.readTag(buffer);
        data = received == null ? new NBTTagCompound() : received;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeBoolean(hand == EnumHand.OFF_HAND);
        ByteBufUtils.writeTag(buffer, data);
    }

    public static class Handler implements IMessageHandler<PacketHandbagSync, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketHandbagSync message, MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().player == null) return;

                ItemStack handbag = Minecraft.getMinecraft().player.getHeldItem(message.hand);
                if (handbag.getItem() == ItemRegistry.HANDBAG) {
                    HandbagStorage.applyData(handbag, message.data);
                }
            });
            return null;
        }
    }
}
