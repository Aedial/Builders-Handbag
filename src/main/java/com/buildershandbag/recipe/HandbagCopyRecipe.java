package com.buildershandbag.recipe;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

import com.buildershandbag.Tags;
import com.buildershandbag.item.ItemRegistry;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Crafting recipe for copying configurations into an unconfigured handbag.
 */
public final class HandbagCopyRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    private static final int INGREDIENT_COUNT = 2;

    public HandbagCopyRecipe() {
        setRegistryName(Tags.MODID, "handbag_copy");
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return getSourceSlot(inventory) >= 0;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        int sourceSlot = getSourceSlot(inventory);
        if (sourceSlot < 0) return ItemStack.EMPTY;

        ItemStack source = inventory.getStackInSlot(sourceSlot);
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack target = inventory.getStackInSlot(slot);
            if (slot != sourceSlot && !target.isEmpty()) {
                return HandbagStorage.copyConfigurations(source, target);
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventory) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(inventory.getSizeInventory(), ItemStack.EMPTY);
        int sourceSlot = getSourceSlot(inventory);
        if (sourceSlot == -1) return remaining;

        ItemStack source = inventory.getStackInSlot(sourceSlot).copy();
        source.setCount(1);
        remaining.set(sourceSlot, source);

        return remaining;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= INGREDIENT_COUNT;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    private static int getSourceSlot(InventoryCrafting inventory) {
        int sourceSlot = -1;
        boolean targetFound = false;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() != ItemRegistry.HANDBAG || stack.getCount() != 1) return -1;

            // source = handbag to copy, target = handbag to write into
            if (HandbagStorage.hasConfigurations(stack)) {
                if (sourceSlot >= 0) return -1;

                sourceSlot = slot;
            } else {
                if (targetFound) return -1;

                targetFound = true;
            }
        }

        return targetFound ? sourceSlot : -1;
    }
}
