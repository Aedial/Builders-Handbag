package com.buildershandbag.integration;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.direwolf20.buildinggadgets.common.items.gadgets.GadgetBuilding;
import com.direwolf20.buildinggadgets.common.tools.GadgetUtils;


/**
 * Selected output stack and tile data retained on the Building Gadget.
 */
public final class BuildingGadgetsTarget {

    public static final String NBT_TARGET = "BuildersHandbagTarget";

    private BuildingGadgetsTarget() {
    }

    public static ItemStack get(ItemStack gadget) {
        if (!(gadget.getItem() instanceof GadgetBuilding) || !gadget.hasTagCompound()) return ItemStack.EMPTY;

        ItemStack target = new ItemStack(gadget.getTagCompound().getCompoundTag(NBT_TARGET));
        if (target.isEmpty() || Block.getBlockFromItem(target.getItem())
                != GadgetUtils.getToolBlock(gadget).getBlock()) return ItemStack.EMPTY;

        return target;
    }

    public static ItemStack resolve(ItemStack requested, EntityPlayer player) {
        ItemStack target = get(GadgetBuilding.getGadget(player));
        if (target.isEmpty()) return requested;
        if (requested.isEmpty()) return target;
        if (!ItemStack.areItemsEqual(target, requested)) return requested;

        if (requested.hasTagCompound() && !requested.getTagCompound().isEmpty()) return requested;

        target.setCount(requested.getCount());
        return target;
    }

    public static void set(ItemStack gadget, ItemStack target) {
        if (!gadget.hasTagCompound()) gadget.setTagCompound(new NBTTagCompound());

        gadget.getTagCompound().setTag(NBT_TARGET, target.writeToNBT(new NBTTagCompound()));
    }
}
