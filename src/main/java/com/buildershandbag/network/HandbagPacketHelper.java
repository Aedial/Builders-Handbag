package com.buildershandbag.network;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

import com.buildershandbag.container.ContainerHandbag;
import com.buildershandbag.item.ItemHandbag;
import com.buildershandbag.item.ItemRegistry;
import com.buildershandbag.storage.HandbagConfiguration;


/**
 * Shared validation and inventory-return rules for client-to-server handbag packets.
 */
final class HandbagPacketHelper {

    private HandbagPacketHelper() {
    }

    @Nullable
    static ItemStack getHeldHandbag(EntityPlayer player, EnumHand hand) {
        ItemStack handbag = player.getHeldItem(hand);
        return handbag.getItem() == ItemRegistry.HANDBAG ? handbag : null;
    }

    @Nullable
    static ContainerHandbag getOpenContainer(EntityPlayerMP player, EnumHand hand) {
        if (!(player.openContainer instanceof ContainerHandbag)) return null;

        ContainerHandbag container = (ContainerHandbag) player.openContainer;
        return container.getHand() == hand ? container : null;
    }

    static void returnStoredMaterial(EntityPlayer player, HandbagConfiguration configuration) {
        int remaining = configuration.getMaterialCount();
        while (remaining > 0) {
            ItemStack material = configuration.getMaterial();
            int count = Math.min(material.getMaxStackSize(), remaining);
            material.setCount(count);
            player.inventory.placeItemBackInInventory(player.world, material);
            remaining -= count;
        }
    }

    static void sync(EntityPlayerMP player, EnumHand hand) {
        ItemHandbag.syncToClient(player, hand);
    }
}
