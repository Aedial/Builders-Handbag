package com.buildershandbag.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.buildershandbag.integration.HandbagIntegration;


/**
 * Server integration settings for client selection and rendering.
 */
public final class PacketHandbagIntegrationsSync implements IMessage {

    private int enabledIntegrations;

    public PacketHandbagIntegrationsSync() {
    }

    public PacketHandbagIntegrationsSync(int enabledIntegrations) {
        this.enabledIntegrations = enabledIntegrations;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        enabledIntegrations = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(enabledIntegrations);
    }

    public static final class Handler implements IMessageHandler<PacketHandbagIntegrationsSync, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketHandbagIntegrationsSync message, MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(
                () -> HandbagIntegration.setClientEnabledIntegrations(message.enabledIntegrations));
            return null;
        }
    }
}
