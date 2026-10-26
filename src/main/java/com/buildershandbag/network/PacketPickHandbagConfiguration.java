package com.buildershandbag.network;

import java.util.List;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.buildershandbag.integration.HandbagBlockPicker;
import com.buildershandbag.storage.HandbagConfiguration;
import com.buildershandbag.storage.HandbagOutputs;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Server validation for configurations picked from targeted world blocks.
 */
public class PacketPickHandbagConfiguration implements IMessage {

    private BlockPos position;
    private Vec3d hit;

    public PacketPickHandbagConfiguration() {
        position = BlockPos.ORIGIN;
        hit = Vec3d.ZERO;
    }

    public PacketPickHandbagConfiguration(RayTraceResult target) {
        this.position = target.getBlockPos();
        this.hit = target.hitVec;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        position = BlockPos.fromLong(buffer.readLong());
        hit = new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(position.toLong());
        buffer.writeDouble(hit.x);
        buffer.writeDouble(hit.y);
        buffer.writeDouble(hit.z);
    }

    public static class Handler implements IMessageHandler<PacketPickHandbagConfiguration, IMessage> {

        @Override
        public IMessage onMessage(PacketPickHandbagConfiguration message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> pickConfiguration(message, player));
            return null;
        }

        private static void pickConfiguration(PacketPickHandbagConfiguration message, EntityPlayerMP player) {
            ItemStack handbag = HandbagPacketHelper.getHeldHandbag(player, EnumHand.MAIN_HAND);
            World world = player.getServerWorld();
            if (handbag == null || player.isSpectator() || !world.isBlockLoaded(message.position)) return;

            double reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
            Vec3d start = player.getPositionEyes(1.0F);
            Vec3d direction = message.hit.subtract(start);
            double distance = direction.lengthSquared();
            if (!Double.isFinite(distance) || distance <= 0.0D) return;

            // Check reach and obstruction along the client's targeted direction
            Vec3d end = start.add(direction.normalize().scale(reach));
            RayTraceResult target = world.rayTraceBlocks(start, end, false, false, true);
            if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK
                    || !message.position.equals(target.getBlockPos())) return;

            HandbagConfiguration picked = HandbagBlockPicker.getConfiguration(world, target, player);
            if (picked == null || !picked.isAvailable()) {
                HandbagMessages.error(player, "message.buildershandbag.invalid_pick");
                return;
            }

            List<HandbagConfiguration> configurations = HandbagStorage.getConfigurations(handbag);
            ItemStack output = HandbagOutputs.getResult(picked);
            for (int index = 0; index < configurations.size(); index++) {
                HandbagConfiguration existing = configurations.get(index);
                if (!existing.isAvailable() || !HandbagOutputs.matches(existing, output)) continue;

                // TODO: Should we report that the configuration already exists?
                //       Instead of silently selecting the existing configuration.
                selectConfiguration(player, handbag, index);
                HandbagMessages.success(player, "message.buildershandbag.picked");
                return;
            }

            if (configurations.size() >= HandbagStorage.CONFIGURATION_COUNT) {
                HandbagMessages.error(player, "message.buildershandbag.full");
                return;
            }

            if (!HandbagStorage.addConfiguration(handbag, picked)) return;

            selectConfiguration(player, handbag, configurations.size());
            HandbagMessages.success(player, "message.buildershandbag.added");
        }

        private static void selectConfiguration(EntityPlayerMP player, ItemStack handbag, int index) {
            HandbagStorage.setSelected(handbag, index);
            player.inventory.markDirty();
            HandbagPacketHelper.sync(player, EnumHand.MAIN_HAND);
        }
    }
}
