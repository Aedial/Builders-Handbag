package com.buildershandbag.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.buildershandbag.integration.FutureMcRecipeCache;


/**
 * Batched requests for server recipe details used by visible handbag configurations.
 */
public final class PacketRequestHandbagRecipes implements IMessage {

    private NBTTagCompound data = new NBTTagCompound();

    public PacketRequestHandbagRecipes() {
    }

    public PacketRequestHandbagRecipes(NBTTagCompound data) {
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

    public static final class Handler implements IMessageHandler<PacketRequestHandbagRecipes, IMessage> {

        @Override
        public IMessage onMessage(PacketRequestHandbagRecipes message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> HandbagNetwork.INSTANCE.sendTo(
                new PacketHandbagRecipesSync(FutureMcRecipeCache.resolveRequests(message.data)), player));
            return null;
        }
    }
}
