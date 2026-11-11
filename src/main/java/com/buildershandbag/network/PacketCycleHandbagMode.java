package com.buildershandbag.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.buildershandbag.storage.HandbagStorage;


/**
 * Client-to-server mode cycling from the handbag GUI.
 */
public class PacketCycleHandbagMode implements IMessage {

    private EnumHand hand;

    public PacketCycleHandbagMode() {
        hand = EnumHand.MAIN_HAND;
    }

    public PacketCycleHandbagMode(EnumHand hand) {
        this.hand = hand;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        hand = buffer.readBoolean() ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeBoolean(hand == EnumHand.OFF_HAND);
    }

    public static class Handler implements IMessageHandler<PacketCycleHandbagMode, IMessage> {

        @Override
        public IMessage onMessage(PacketCycleHandbagMode message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (HandbagPacketHelper.getOpenContainer(player, message.hand) == null) return;

                ItemStack handbag = HandbagPacketHelper.getHeldHandbag(player, message.hand);
                if (handbag == null) return;

                HandbagStorage.setMode(handbag, HandbagStorage.getMode(handbag).next());
                player.inventory.markDirty();
                HandbagPacketHelper.sync(player, message.hand);
            });
            return null;
        }
    }
}
