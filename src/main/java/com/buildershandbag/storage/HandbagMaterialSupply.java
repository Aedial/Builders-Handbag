package com.buildershandbag.storage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import com.buildershandbag.integration.Ae2Integration;
import com.buildershandbag.integration.BaublesIntegration;


/**
 * Material sources shared by handheld placement and external building tools.
 */
public final class HandbagMaterialSupply {

    private static final String AE2_MODID = "appliedenergistics2";
    private static final String BAUBLES_MODID = "baubles";

    private HandbagMaterialSupply() {
    }

    public static boolean refill(EntityPlayer player, ItemStack handbag, int index) {
        HandbagConfiguration configuration = HandbagStorage.getConfiguration(handbag, index);
        if (configuration == null || !configuration.isAvailable()) return false;
        if (configuration.getMaterialCount() > 0) return true;
        if (player == null || player.world.isRemote) return false;

        ItemStack material = configuration.getMaterial();
        int requested = HandbagStorage.MATERIAL_CAPACITY - configuration.getMaterialCount();
        int pulled = extractNetwork(player, handbag, material, requested, false);
        pulled += extractInventory(player, material, requested - pulled, false);
        if (pulled <= 0) return false;

        material.setCount(pulled);
        HandbagStorage.insertMaterial(handbag, index, material, false);
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();

        return true;
    }

    public static int extractInventory(EntityPlayer player, ItemStack material, int amount, boolean simulate) {
        if (player == null || amount <= 0) return 0;

        IItemHandler inventory = new InvWrapper(player.inventory);
        int pulled = MaterialInventoryHelper.extractMaterial(inventory, material, amount, simulate).getCount();
        pulled += MaterialInventoryHelper.extractFromBags(
            inventory, material, amount - pulled, simulate, slot -> player.inventory.markDirty()).getCount();
        if (pulled < amount && Loader.isModLoaded(BAUBLES_MODID)) {
            pulled += extractBaubles(player, material, amount - pulled, simulate);
        }

        return pulled;
    }

    public static int extractNetwork(EntityPlayer player, ItemStack handbag, ItemStack material,
            int amount, boolean simulate) {
        if (player == null || player.world.isRemote || amount <= 0 || !Loader.isModLoaded(AE2_MODID)) return 0;

        return extractAe2(player, handbag, material, amount, simulate);
    }

    public static String getNetworkLink(ItemStack handbag) {
        return handbag.hasTagCompound() ? handbag.getTagCompound().getString("encryptionKey") : "";
    }

    @Optional.Method(modid = AE2_MODID)
    private static int extractAe2(EntityPlayer player, ItemStack handbag, ItemStack material,
            int amount, boolean simulate) {
        return Ae2Integration.refill(player, handbag, material, amount, simulate);
    }

    @Optional.Method(modid = BAUBLES_MODID)
    private static int extractBaubles(EntityPlayer player, ItemStack material, int amount, boolean simulate) {
        return BaublesIntegration.extractMaterial(player, material, amount, simulate).getCount();
    }
}
