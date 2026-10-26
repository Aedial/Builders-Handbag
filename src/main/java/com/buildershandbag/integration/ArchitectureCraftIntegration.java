package com.buildershandbag.integration;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.elytradev.architecture.common.shape.EnumShape;
import com.elytradev.architecture.common.tile.TileShape;

import com.buildershandbag.storage.HandbagConfiguration;


/**
 * ArchitectureCraft shape integration.
 */
public final class ArchitectureCraftIntegration {

    private ArchitectureCraftIntegration() {
    }

    // TODO: If there is demand, we might consider fractional material cost
    //       (aligning with ArchitectureCraft's own cost calculations),
    //       which would require custom rendering, decoupling ItemStack
    //       from count, and custom slot count rendering. Also, I'm not
    //       sure how ArchitectureCraft calculates the cost of a shape.

    public static void addOptions(ItemStack material, List<HandbagConfigurationOption> options) {
        if (!(material.getItem() instanceof ItemBlock)) return;

        Block source = ((ItemBlock) material.getItem()).getBlock();

        for (EnumShape shape : EnumShape.values()) {
            if (shape.isCladding()) continue;

            ItemStack result = shape.kind.newStack(shape, source, material.getMetadata(), 1);
            HandbagConfigurationProvider.addOption(
                options,
                new HandbagConfigurationOption(result, HandbagIntegration.ARCHITECTURECRAFT));
        }
    }

    @Nullable
    public static HandbagConfiguration pickConfiguration(World world, BlockPos pos) {
        TileShape tile = TileShape.get(world, pos);
        if (tile == null || !tile.hasShape() || !tile.hasBaseBlockState() || tile.hasSecondaryBlockState()) return null;

        EnumShape shape = tile.getShape();
        if (shape.isCladding()) return null;

        IBlockState state = tile.getBaseBlockState();
        Block block = state.getBlock();
        ItemStack material = new ItemStack(block, 1, block.getMetaFromState(state));
        if (!HandbagConfigurationProvider.isConfigurationMaterial(material)) return null;

        ItemStack result = shape.kind.newStack(shape, block, material.getMetadata(), 1);
        return new HandbagConfiguration(material, result, HandbagIntegration.ARCHITECTURECRAFT, 0);
    }
}
