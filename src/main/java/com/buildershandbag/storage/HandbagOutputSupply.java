package com.buildershandbag.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.buildershandbag.item.ItemHandbag;


/**
 * Configured outputs available across the player's carried handbags.
 */
public final class HandbagOutputSupply {

    private HandbagOutputSupply() {
    }

    public static List<ItemStack> getHandbags(EntityPlayer player) {
        List<ItemStack> handbags = new ArrayList<>();
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.getItem() instanceof ItemHandbag) handbags.add(stack);
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            if (stack.getItem() instanceof ItemHandbag) handbags.add(stack);
        }

        return handbags;
    }

    public static boolean isFirstHandbag(EntityPlayer player, ItemStack handbag) {
        List<ItemStack> handbags = getHandbags(player);
        return !handbags.isEmpty() && handbags.get(0) == handbag;
    }

    public static boolean hasConfiguration(EntityPlayer player, ItemStack requested) {
        for (ItemStack handbag : getHandbags(player)) {
            for (HandbagConfiguration configuration : HandbagStorage.getConfigurations(handbag)) {
                if (HandbagOutputs.matches(configuration, requested)) return true;
            }
        }

        return false;
    }

    public static int count(EntityPlayer player, ItemStack requested) {
        if (requested.isEmpty()) return 0;
        if (player.world.isRemote) return HandbagSupplyCache.getCount(player, requested);

        return countAvailable(player, requested, true);
    }

    public static int countInventory(EntityPlayer player, ItemStack requested) {
        return countAvailable(player, requested, false);
    }

    private static int countAvailable(EntityPlayer player, ItemStack requested, boolean includeNetwork) {
        long count = 0;
        List<ItemStack> materials = new ArrayList<>();
        List<Set<String>> links = new ArrayList<>();
        for (ItemStack handbag : getHandbags(player)) {
            for (HandbagConfiguration configuration : HandbagStorage.getConfigurations(handbag)) {
                if (!HandbagOutputs.matches(configuration, requested)) continue;

                count += configuration.getMaterialCount();
                ItemStack material = configuration.getMaterial();
                int materialIndex = findMaterial(materials, material);
                if (materialIndex < 0) {
                    materialIndex = materials.size();
                    materials.add(material);
                    links.add(new HashSet<>());
                    // The tools already count loose output items and other containers
                    if (!HandbagStorage.sameMaterial(material, requested)) {
                        count += HandbagMaterialSupply.extractInventory(player, material, Integer.MAX_VALUE, true);
                    }
                }

                String link = HandbagMaterialSupply.getNetworkLink(handbag);
                if (includeNetwork && !link.isEmpty() && !links.get(materialIndex).contains(link)) {
                    int available = HandbagMaterialSupply.extractNetwork(
                        player, handbag, material, Integer.MAX_VALUE, true);
                    if (available > 0) {
                        links.get(materialIndex).add(link);
                        count += available;
                    }
                }
            }
        }

        return (int) Math.min(Integer.MAX_VALUE, count);
    }

    private static int findMaterial(List<ItemStack> materials, ItemStack material) {
        for (int index = 0; index < materials.size(); index++) {
            if (HandbagStorage.sameMaterial(materials.get(index), material)) return index;
        }

        return -1;
    }

    public static int extract(EntityPlayer player, ItemStack requested, int amount) {
        if (amount <= 0 || player.world.isRemote) return 0;

        int extracted = 0;
        for (ItemStack handbag : getHandbags(player)) {
            List<HandbagConfiguration> configurations = HandbagStorage.getConfigurations(handbag);
            for (int index = 0; index < configurations.size() && extracted < amount; index++) {
                if (!HandbagOutputs.matches(configurations.get(index), requested)) continue;

                extracted += extractConfiguration(player, handbag, index, amount - extracted, false);
            }
            if (extracted >= amount) break;
        }

        return extracted;
    }

    public static int extractConfiguration(EntityPlayer player, ItemStack handbag, int index,
            int amount, boolean simulate) {
        if (amount <= 0) return 0;

        HandbagConfiguration configuration = HandbagStorage.getConfiguration(handbag, index);
        if (configuration == null || !configuration.isAvailable()) return 0;

        if (simulate) {
            if (player != null && player.world.isRemote) {
                ItemStack result = HandbagOutputs.getResult(configuration);
                return Math.min(amount, HandbagSupplyCache.getCount(player, result));
            }

            long available = configuration.getMaterialCount();
            if (player != null && !player.world.isRemote) {
                ItemStack material = configuration.getMaterial();
                available += HandbagMaterialSupply.extractNetwork(player, handbag, material, amount, true);
                available += HandbagMaterialSupply.extractInventory(player, material, amount, true);
            }

            return (int) Math.min(amount, available);
        }

        if (player != null && player.world.isRemote) return 0;

        int extracted = 0;
        while (extracted < amount) {
            if (player != null) HandbagMaterialSupply.refill(player, handbag, index);

            int consumed = HandbagStorage.consumeMaterial(handbag, index, amount - extracted);
            if (consumed <= 0) break;

            extracted += consumed;
        }

        if (extracted > 0 && player != null) {
            HandbagMaterialSupply.refill(player, handbag, index);
            player.inventory.markDirty();
            player.inventoryContainer.detectAndSendChanges();
        }

        return extracted;
    }
}
