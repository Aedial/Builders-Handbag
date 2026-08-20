package com.buildershandbag.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.buildershandbag.storage.HandbagConfiguration;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Client-to-server removal of a configuration and return of its stored material.
 */
public class PacketRemoveHandbagConfiguration implements IMessage {

    private EnumHand hand;
    private int index;

    public PacketRemoveHandbagConfiguration() {
        hand = EnumHand.MAIN_HAND;
    }

    public PacketRemoveHandbagConfiguration(EnumHand hand, int index) {
        this.hand = hand;
        this.index = index;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        hand = buffer.readBoolean() ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        index = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeBoolean(hand == EnumHand.OFF_HAND);
        buffer.writeInt(index);
    }

    public static class Handler implements IMessageHandler<PacketRemoveHandbagConfiguration, IMessage> {

        @Override
        public IMessage onMessage(PacketRemoveHandbagConfiguration message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (HandbagPacketHelper.getOpenContainer(player, message.hand) == null) return;

                ItemStack handbag = HandbagPacketHelper.getHeldHandbag(player, message.hand);
                if (handbag == null) return;

                HandbagConfiguration removed = HandbagStorage.removeConfiguration(handbag, message.index);
                if (removed == null) return;

                HandbagPacketHelper.returnStoredMaterial(player, removed);
                player.inventory.markDirty();
                HandbagPacketHelper.sync(player, message.hand);
            });
            return null;
        }
    }
}
