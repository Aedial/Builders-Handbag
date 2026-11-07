package com.buildershandbag.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.buildershandbag.config.HandbagServerConfig;
import com.buildershandbag.integration.HandbagIntegration;


/**
 * Server integration and behavior settings for client selection, rendering, and tooltips.
 */
public final class PacketHandbagIntegrationsSync implements IMessage {

    private int enabledIntegrations;
    private boolean cleanupFrames;
    private boolean pickupAbsorption;
    private boolean inventoryCleanup;

    public PacketHandbagIntegrationsSync() {
    }

    public PacketHandbagIntegrationsSync(int enabledIntegrations, boolean cleanupFrames,
            boolean pickupAbsorption, boolean inventoryCleanup) {
        this.enabledIntegrations = enabledIntegrations;
        this.cleanupFrames = cleanupFrames;
        this.pickupAbsorption = pickupAbsorption;
        this.inventoryCleanup = inventoryCleanup;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        enabledIntegrations = buffer.readInt();
        cleanupFrames = buffer.readBoolean();
        pickupAbsorption = buffer.readBoolean();
        inventoryCleanup = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(enabledIntegrations);
        buffer.writeBoolean(cleanupFrames);
        buffer.writeBoolean(pickupAbsorption);
        buffer.writeBoolean(inventoryCleanup);
    }

    public static final class Handler implements IMessageHandler<PacketHandbagIntegrationsSync, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketHandbagIntegrationsSync message, MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                HandbagIntegration.setClientEnabledIntegrations(message.enabledIntegrations);
                HandbagServerConfig.setClientBehavior(
                    message.cleanupFrames, message.pickupAbsorption, message.inventoryCleanup);
            });
            return null;
        }
    }
}
