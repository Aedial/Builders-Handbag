package com.buildershandbag.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.buildershandbag.client.overlay.MessageType;
import com.buildershandbag.client.overlay.OverlayMessageRenderer;


/**
 * Server-to-client localized feedback for handbag actions.
 */
public class PacketOverlayMessage implements IMessage {

    public enum Type {
        SUCCESS,
        ERROR
    }

    private Type type;
    private String translationKey;
    private String[] arguments;

    public PacketOverlayMessage() {
        type = Type.ERROR;
        translationKey = "";
        arguments = new String[0];
    }

    public PacketOverlayMessage(Type type, String translationKey, Object... arguments) {
        this.type = type;
        this.translationKey = translationKey == null ? "" : translationKey;
        this.arguments = new String[arguments == null ? 0 : arguments.length];
        for (int index = 0; index < this.arguments.length; index++) {
            this.arguments[index] = String.valueOf(arguments[index]);
        }
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        int ordinal = buffer.readUnsignedByte();
        type = ordinal < Type.values().length ? Type.values()[ordinal] : Type.ERROR;
        translationKey = ByteBufUtils.readUTF8String(buffer);
        int count = buffer.readUnsignedByte();
        arguments = new String[count];
        for (int index = 0; index < count; index++) {
            arguments[index] = ByteBufUtils.readUTF8String(buffer);
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeByte(type.ordinal());
        ByteBufUtils.writeUTF8String(buffer, translationKey);
        buffer.writeByte(Math.min(16, arguments.length));
        for (int index = 0; index < arguments.length && index < 16; index++) {
            ByteBufUtils.writeUTF8String(buffer, arguments[index]);
        }
    }

    public static class Handler implements IMessageHandler<PacketOverlayMessage, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketOverlayMessage message, MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() -> OverlayMessageRenderer.setMessage(
                I18n.format(message.translationKey, (Object[]) message.arguments),
                message.type == Type.SUCCESS ? MessageType.SUCCESS : MessageType.ERROR));
            return null;
        }
    }
}
