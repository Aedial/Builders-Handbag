package com.buildershandbag.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.buildershandbag.container.ContainerHandbag;
import com.buildershandbag.integration.HandbagConfigurationOption;
import com.buildershandbag.integration.HandbagConfigurationProvider;
import com.buildershandbag.integration.HandbagIntegration;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Adds a server-validated output from the current configuration-material options.
 */
public class PacketAddHandbagConfiguration implements IMessage {

    private EnumHand hand;
    private HandbagIntegration integration;
    private ItemStack result;

    public PacketAddHandbagConfiguration() {
        hand = EnumHand.MAIN_HAND;
        result = ItemStack.EMPTY;
    }

    public PacketAddHandbagConfiguration(EnumHand hand, HandbagConfigurationOption option) {
        this.hand = hand;
        this.integration = option.getIntegration();
        this.result = option.getResult();
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        hand = buffer.readBoolean() ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        integration = HandbagIntegration.fromOrdinal(buffer.readUnsignedByte());
        result = ByteBufUtils.readItemStack(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeBoolean(hand == EnumHand.OFF_HAND);
        buffer.writeByte(integration == null ? 255 : integration.ordinal());
        ByteBufUtils.writeItemStack(buffer, result);
    }

    public static class Handler implements IMessageHandler<PacketAddHandbagConfiguration, IMessage> {

        @Override
        public IMessage onMessage(PacketAddHandbagConfiguration message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> addConfiguration(message, player));
            return null;
        }

        private static void addConfiguration(PacketAddHandbagConfiguration message, EntityPlayerMP player) {
            ContainerHandbag container = HandbagPacketHelper.getOpenContainer(player, message.hand);
            ItemStack handbag = HandbagPacketHelper.getHeldHandbag(player, message.hand);
            if (container == null || handbag == null || message.integration == null) return;

            if (HandbagStorage.getConfigurations(handbag).size() >= HandbagStorage.CONFIGURATION_COUNT) {
                HandbagMessages.error(player, "message.buildershandbag.full");
                return;
            }

            HandbagConfigurationOption requested = new HandbagConfigurationOption(message.result, message.integration);
            ItemStack configurationMaterial = container.getConfigurationMaterial();

            for (HandbagConfigurationOption option : HandbagConfigurationProvider.getOptions(configurationMaterial)) {
                if (!option.matches(requested)) continue;

                int configurationIndex = HandbagStorage.getConfigurations(handbag).size();
                if (!HandbagStorage.addConfiguration(handbag, option.createConfiguration(configurationMaterial))) return;

                int transferred = HandbagStorage.insertMaterial(
                    handbag, configurationIndex, configurationMaterial, false);
                container.consumeConfigurationMaterial(transferred);
                if (configurationIndex == 0) HandbagStorage.setSelected(handbag, configurationIndex);
                player.inventory.markDirty();

                HandbagMessages.success(player, "message.buildershandbag.added");
                HandbagPacketHelper.sync(player, message.hand);
                return;
            }

            HandbagMessages.error(player, "message.buildershandbag.invalid_option");
        }
    }
}
