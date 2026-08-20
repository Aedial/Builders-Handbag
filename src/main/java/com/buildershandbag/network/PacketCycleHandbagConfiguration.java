package com.buildershandbag.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.buildershandbag.storage.HandbagConfiguration;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Sneak-scroll selection cycling for a held handbag.
 */
public class PacketCycleHandbagConfiguration implements IMessage {

    private EnumHand hand;
    private boolean forward;

    public PacketCycleHandbagConfiguration() {
        hand = EnumHand.MAIN_HAND;
    }

    public PacketCycleHandbagConfiguration(EnumHand hand, boolean forward) {
        this.hand = hand;
        this.forward = forward;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        hand = buffer.readBoolean() ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        forward = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeBoolean(hand == EnumHand.OFF_HAND);
        buffer.writeBoolean(forward);
    }

    public static class Handler implements IMessageHandler<PacketCycleHandbagConfiguration, IMessage> {

        @Override
        public IMessage onMessage(PacketCycleHandbagConfiguration message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack handbag = HandbagPacketHelper.getHeldHandbag(player, message.hand);
                if (handbag == null) return;

                int selected = HandbagStorage.cycleSelected(handbag, message.forward);
                HandbagConfiguration configuration = HandbagStorage.getConfiguration(handbag, selected);
                if (configuration != null) {
                    ITextComponent text = configuration.getResult().getTextComponent();
                    text.getStyle().setColor(TextFormatting.GREEN);

                    player.sendStatusMessage( text, true);
                }

                player.inventory.markDirty();
                HandbagPacketHelper.sync(player, message.hand);
            });
            return null;
        }
    }
}
