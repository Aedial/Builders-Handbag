package com.buildershandbag.integration;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.elytradev.architecture.common.shape.EnumShape;
import com.elytradev.architecture.common.tile.TileShape;

import com.buildershandbag.storage.HandbagConfiguration;
import com.buildershandbag.storage.MaterialAmount;


/**
 * ArchitectureCraft shape integration.
 */
public final class ArchitectureCraftIntegration {

    private ArchitectureCraftIntegration() {
    }

    public static MaterialAmount getMaterialCost(ItemStack material, ItemStack result) {
        if (!result.hasTagCompound() || !result.getTagCompound().hasKey("Shape")) return MaterialAmount.BLOCK;

        int shapeId = result.getTagCompound().getInteger("Shape");
        for (EnumShape shape : EnumShape.values()) {
            if (shape.id != shapeId || shape.materialUsed <= 0 || shape.itemsProduced <= 0) continue;

            int factor = Block.getBlockFromItem(material.getItem()) instanceof BlockSlab ? 2 : 1;
            return new MaterialAmount((long) factor * shape.materialUsed, shape.itemsProduced);
        }

        return MaterialAmount.BLOCK;
    }

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
