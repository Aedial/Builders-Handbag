package com.buildershandbag.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.buildershandbag.item.ItemHandbag;


/**
 * Handbag slots discovered during normal item updates on each logical side.
 * Slot numbers keep the player cache independent of the stacks' capability handlers.
 */
public final class CarriedHandbags {

    private static final Map<EntityPlayer, SortedSet<Integer>> SERVER_SLOTS = new WeakHashMap<>();
    private static final Map<EntityPlayer, SortedSet<Integer>> CLIENT_SLOTS = new WeakHashMap<>();

    private CarriedHandbags() {
    }

    public static void register(EntityPlayer player, ItemStack handbag, int slot) {
        if (slot < 0) return;

        int inventorySlot;
        if (slot < player.inventory.mainInventory.size() && player.inventory.mainInventory.get(slot) == handbag) {
            inventorySlot = slot;
        } else if (slot < player.inventory.offHandInventory.size()
                && player.inventory.offHandInventory.get(slot) == handbag) {
            inventorySlot = player.inventory.mainInventory.size() + player.inventory.armorInventory.size() + slot;
        } else {
            return;
        }

        getCache(player).computeIfAbsent(player, unused -> new TreeSet<>()).add(inventorySlot);
    }

    public static List<ItemStack> get(EntityPlayer player) {
        Map<EntityPlayer, SortedSet<Integer>> cache = getCache(player);
        SortedSet<Integer> slots = cache.get(player);
        if (slots == null) return Collections.emptyList();

        List<ItemStack> handbags = new ArrayList<>();
        Iterator<Integer> iterator = slots.iterator();
        while (iterator.hasNext()) {
            ItemStack stack = player.inventory.getStackInSlot(iterator.next());
            if (stack.getItem() instanceof ItemHandbag) {
                handbags.add(stack);
            } else {
                iterator.remove();
            }
        }
        if (slots.isEmpty()) cache.remove(player);

        return handbags;
    }

    private static Map<EntityPlayer, SortedSet<Integer>> getCache(EntityPlayer player) {
        return player.world.isRemote ? CLIENT_SLOTS : SERVER_SLOTS;
    }
}
