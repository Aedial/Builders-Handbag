package com.buildershandbag.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;


/**
 * Server-side bridge to the overlay-message system.
 */
public final class HandbagMessages {

    private HandbagMessages() {
    }

    public static void error(EntityPlayer player, String translationKey, Object... arguments) {
        send(player, PacketOverlayMessage.Type.ERROR, translationKey, arguments);
    }

    public static void success(EntityPlayer player, String translationKey, Object... arguments) {
        send(player, PacketOverlayMessage.Type.SUCCESS, translationKey, arguments);
    }

    private static void send(EntityPlayer player, PacketOverlayMessage.Type type, String translationKey,
            Object... arguments) {
        if (!(player instanceof EntityPlayerMP)) return;

        HandbagNetwork.INSTANCE.sendTo(new PacketOverlayMessage(type, translationKey, arguments), (EntityPlayerMP) player);
    }
}
