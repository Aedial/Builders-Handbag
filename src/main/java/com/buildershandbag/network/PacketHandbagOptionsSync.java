package com.buildershandbag.network;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.buildershandbag.container.ContainerHandbag;
import com.buildershandbag.integration.HandbagConfigurationOption;
import com.buildershandbag.integration.HandbagIntegration;


/**
 * Server-generated configuration options for the material currently in the real slot.
 */
public class PacketHandbagOptionsSync implements IMessage {

    private EnumHand hand;
    private List<HandbagConfigurationOption> options;

    public PacketHandbagOptionsSync() {
        hand = EnumHand.MAIN_HAND;
        options = new ArrayList<>();
    }

    public PacketHandbagOptionsSync(EnumHand hand, List<HandbagConfigurationOption> options) {
        this.hand = hand;
        this.options = options == null ? new ArrayList<>() : new ArrayList<>(options);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        hand = buffer.readBoolean() ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        int count = buffer.readUnsignedShort();
        options = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            HandbagIntegration integration = HandbagIntegration.fromOrdinal(buffer.readUnsignedByte());
            ItemStack result = ByteBufUtils.readItemStack(buffer);
            if (integration != null && !result.isEmpty()) {
                options.add(new HandbagConfigurationOption(result, integration));
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeBoolean(hand == EnumHand.OFF_HAND);
        buffer.writeShort(options.size());
        for (HandbagConfigurationOption option : options) {
            buffer.writeByte(option.getIntegration().ordinal());
            ByteBufUtils.writeItemStack(buffer, option.getResult());
        }
    }

    public static class Handler implements IMessageHandler<PacketHandbagOptionsSync, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketHandbagOptionsSync message, MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().player == null) return;

                Container openContainer = Minecraft.getMinecraft().player.openContainer;
                if (!(openContainer instanceof ContainerHandbag)) return;

                ContainerHandbag container = (ContainerHandbag) openContainer;
                if (container.getHand() == message.hand) container.setClientOptions(message.options);
            });
            return null;
        }
    }
}
