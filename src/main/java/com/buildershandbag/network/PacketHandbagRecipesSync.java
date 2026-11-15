package com.buildershandbag.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.buildershandbag.integration.FutureMcRecipeCache;


/**
 * Server recipe results and yields for Future-MC configuration availability and tooltips.
 */
public final class PacketHandbagRecipesSync implements IMessage {

    private NBTTagCompound data = new NBTTagCompound();

    public PacketHandbagRecipesSync() {
    }

    public PacketHandbagRecipesSync(NBTTagCompound data) {
        this.data = data.copy();
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        NBTTagCompound received = ByteBufUtils.readTag(buffer);
        data = received == null ? new NBTTagCompound() : received;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeTag(buffer, data);
    }

    public static final class Handler implements IMessageHandler<PacketHandbagRecipesSync, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketHandbagRecipesSync message, MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() -> FutureMcRecipeCache.applySnapshot(message.data));
            return null;
        }
    }
}
