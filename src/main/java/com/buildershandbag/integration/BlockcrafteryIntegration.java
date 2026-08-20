package com.buildershandbag.integration;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import epicsquid.blockcraftery.block.IEditableBlock;
import epicsquid.blockcraftery.tile.TileEditableBlock;

/**
 * Blockcraftery editable-block integration.
 */
public final class BlockcrafteryIntegration {

    private BlockcrafteryIntegration() {
    }

    static void addOptions(List<HandbagConfigurationOption> options) {
        for (Block block : ForgeRegistries.BLOCKS.getValuesCollection()) {
            if (!(block instanceof IEditableBlock)) continue;

            Item item = Item.getItemFromBlock(block);
            if (item != null) {
                HandbagConfigurationProvider.addOption(
                    options,
                    new HandbagConfigurationOption(new ItemStack(item), HandbagIntegration.BLOCKCRAFTERY));
            }
        }
    }

    /**
     * Configures the Blockcraftery editable block placed for the selected result.
     * <p>
     * The placed result is the editable block itself, so the player neither
     * supplies nor consumes a separate Blockcraftery frame item. This can fail
     * if the API rejects the configuration or the player is out of range.
     *
     * @return true if the configuration was successful, false otherwise
     */
    public static boolean configure(World world, BlockPos position, EntityPlayer player, EnumFacing side,
            float hitX, float hitY, float hitZ, ItemStack material) {
        TileEntity tile = world.getTileEntity(position);
        if (!(tile instanceof TileEditableBlock)) return false;

        ItemStack originalMainHand = player.getHeldItemMainhand();
        ItemStack temporaryMaterial = material.copy();
        temporaryMaterial.setCount(1);
        player.setHeldItem(EnumHand.MAIN_HAND, temporaryMaterial);

        try {
            boolean configured = ((TileEditableBlock) tile).activate(
                world, position, world.getBlockState(position),
                player, EnumHand.MAIN_HAND, side,
                hitX, hitY, hitZ);

            // Clean the previously placed frame if the configuration failed.
            // Can't leave frames behind like a bad guest at a party.
            if (!configured) world.setBlockToAir(position);

            return configured;
        } finally {
            player.setHeldItem(EnumHand.MAIN_HAND, originalMainHand);
        }
    }
}
