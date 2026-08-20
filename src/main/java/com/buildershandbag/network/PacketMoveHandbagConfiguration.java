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
 * Client-to-server insertion move for the configuration selected for reordering.
 */
public class PacketMoveHandbagConfiguration implements IMessage {

    private EnumHand hand;
    private int from;
    private int target;

    public PacketMoveHandbagConfiguration() {
        hand = EnumHand.MAIN_HAND;
    }

    public PacketMoveHandbagConfiguration(EnumHand hand, int from, int target) {
        this.hand = hand;
        this.from = from;
        this.target = target;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        hand = buffer.readBoolean() ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        from = buffer.readInt();
        target = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeBoolean(hand == EnumHand.OFF_HAND);
        buffer.writeInt(from);
        buffer.writeInt(target);
    }

    public static class Handler implements IMessageHandler<PacketMoveHandbagConfiguration, IMessage> {

        @Override
        public IMessage onMessage(PacketMoveHandbagConfiguration message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (HandbagPacketHelper.getOpenContainer(player, message.hand) == null) return;

                ItemStack handbag = HandbagPacketHelper.getHeldHandbag(player, message.hand);
                if (handbag == null || HandbagStorage.moveConfiguration(handbag, message.from, message.target) < 0) {
                    return;
                }

                player.inventory.markDirty();
                HandbagPacketHelper.sync(player, message.hand);
            });
            return null;
        }
    }
}
