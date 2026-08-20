package com.buildershandbag.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import com.buildershandbag.gui.GuiHandbag;
import com.buildershandbag.item.ItemRegistry;


/**
 * Resolves the handheld Handbag GUI.
 */
public class GuiHandler implements IGuiHandler {

    public static final int GUI_HANDBAG = 0;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != GUI_HANDBAG) return null;

        EnumHand hand = x == EnumHand.OFF_HAND.ordinal() ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        if (player.getHeldItem(hand).getItem() != ItemRegistry.HANDBAG) return null;

        return new ContainerHandbag(player.inventory, hand);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != GUI_HANDBAG) return null;

        EnumHand hand = x == EnumHand.OFF_HAND.ordinal() ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        if (player.getHeldItem(hand).getItem() != ItemRegistry.HANDBAG) return null;

        return new GuiHandbag(new ContainerHandbag(player.inventory, hand));
    }
}
