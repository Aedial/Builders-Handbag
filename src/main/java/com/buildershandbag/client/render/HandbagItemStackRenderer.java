package com.buildershandbag.client.render;

import javax.annotation.Nonnull;

import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.buildershandbag.tile.TileHandbag;


/**
 * Reuses the handbag TESR for item contexts so the held, inventory, and JEI
 * views match the placed handbag render.
 */
@SideOnly(Side.CLIENT)
public final class HandbagItemStackRenderer extends TileEntityItemStackRenderer {

    private static final TileHandbag ITEM_TILE = new TileHandbag();

    @Override
    public void renderByItem(@Nonnull ItemStack stack, float partialTicks) {
        if (stack.isEmpty()) return;

        ITEM_TILE.setHandbagStack(stack);
        TileEntityRendererDispatcher.instance.render(ITEM_TILE, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks);
    }
}