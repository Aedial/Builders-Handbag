package com.buildershandbag.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;


/**
 * Configured outputs available across the player's carried handbags.
 */
public final class HandbagOutputSupply {

    private HandbagOutputSupply() {
    }

    public static List<ItemStack> getHandbags(EntityPlayer player) {
        return CarriedHandbags.get(player);
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
        List<AvailableMaterial> materials = new ArrayList<>();
        for (ItemStack handbag : getHandbags(player)) {
            for (HandbagConfiguration configuration : HandbagStorage.getConfigurations(handbag)) {
                if (!HandbagOutputs.matches(configuration, requested)) continue;

                ItemStack material = configuration.getMaterial();
                AvailableMaterial available = findMaterial(materials, material);
                if (available == null) {
                    available = new AvailableMaterial(material);
                    materials.add(available);
                    // The tools already count loose output items and other containers
                    if (!HandbagStorage.sameMaterial(material, requested)) {
                        available.inventoryCount = HandbagMaterialSupply.extractInventory(
                            player, material, Integer.MAX_VALUE, true);
                    }
                }

                String link = HandbagMaterialSupply.getNetworkLink(handbag);
                if (includeNetwork && !link.isEmpty() && !available.networkCounts.containsKey(link)) {
                    available.networkCounts.put(link, (long) HandbagMaterialSupply.extractNetwork(
                        player, handbag, material, Integer.MAX_VALUE, true));
                }

                long networkCount = includeNetwork ? available.networkCounts.getOrDefault(link, 0L) : 0;
                MaterialAmount balance = configuration.getMaterialAmount();
                MaterialAmount cost = configuration.getMaterialCost();
                int outputs = balance.add(networkCount + available.inventoryCount).getOutputCount(cost);
                if (outputs > 0) {
                    // Account for source items that refill this entry after its final output
                    if (networkCount > 0) available.networkCounts.put(link, 0L);

                    available.inventoryCount = 0;
                }

                count += outputs;
                if (count >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
            }
        }

        return (int) Math.min(Integer.MAX_VALUE, count);
    }

    private static AvailableMaterial findMaterial(List<AvailableMaterial> materials, ItemStack material) {
        for (AvailableMaterial available : materials) {
            if (HandbagStorage.sameMaterial(available.material, material)) return available;
        }

        return null;
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

            return countConfiguration(player, handbag, configuration, amount);
        }

        if (player != null && player.world.isRemote) return 0;

        int available = countConfiguration(player, handbag, configuration, amount);
        int extracted = 0;
        while (extracted < available) {
            if (player != null) HandbagMaterialSupply.refill(player, handbag, index);

            int consumed = HandbagStorage.consumeMaterial(handbag, index, available - extracted);
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

    private static int countConfiguration(EntityPlayer player, ItemStack handbag,
            HandbagConfiguration configuration, int amount) {
        MaterialAmount balance = configuration.getMaterialAmount();
        MaterialAmount cost = configuration.getMaterialCost();
        if (player == null) return Math.min(amount, balance.getOutputCount(cost));

        ItemStack material = configuration.getMaterial();
        long requested = balance.getRequiredMaterial(cost, amount);
        int network = HandbagMaterialSupply.extractNetwork(
            player, handbag, material, (int) Math.min(Integer.MAX_VALUE, requested), true);
        int inventory = HandbagMaterialSupply.extractInventory(
            player, material, (int) Math.min(Integer.MAX_VALUE, requested - network), true);
        return Math.min(amount, balance.add((long) network + inventory).getOutputCount(cost));
    }

    private static final class AvailableMaterial {

        private final ItemStack material;
        private final Map<String, Long> networkCounts = new HashMap<>();
        private long inventoryCount;

        private AvailableMaterial(ItemStack material) {
            this.material = material;
        }
    }
}
