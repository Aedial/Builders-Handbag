package com.buildershandbag.integration;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

import com.buildershandbag.storage.HandbagConfiguration;


/**
 * Integration access for configurations picked from world blocks.
 */
public final class HandbagBlockPicker {

    private HandbagBlockPicker() {
    }

    @Nullable
    public static HandbagConfiguration getConfiguration(World world, RayTraceResult target, EntityPlayer player) {
        BlockPos pos = target.getBlockPos();
        IBlockState state = world.getBlockState(pos);
        ResourceLocation name = state.getBlock().getRegistryName();
        if (name == null) return null;

        if (name.getNamespace().equals(HandbagIntegration.ARCHITECTURECRAFT_MODID)) {
            return HandbagIntegration.ARCHITECTURECRAFT.isEnabled()
                ? pickArchitectureCraft(world, pos) : null;
        }

        ItemStack result = state.getBlock().getPickBlock(state, target, world, pos, player);
        if (!HandbagConfigurationProvider.isConfigurationMaterial(result)) return null;

        if (name.getNamespace().equals(HandbagIntegration.BLOCKCRAFTERY_MODID)) {
            return HandbagIntegration.BLOCKCRAFTERY.isEnabled()
                ? pickBlockcraftery(world, pos, result) : null;
        }

        if (HandbagIntegration.CHISEL.isEnabled()) {
            HandbagConfiguration chiselConfig = pickChisel(result);
            if (chiselConfig != null) return chiselConfig;
        }

        // + any other future integrations

        return null;
    }

    @Nullable
    @Optional.Method(modid = HandbagIntegration.CHISEL_MODID)
    private static HandbagConfiguration pickChisel(ItemStack result) {
        return ChiselIntegration.pickConfiguration(result);
    }

    @Nullable
    @Optional.Method(modid = HandbagIntegration.ARCHITECTURECRAFT_MODID)
    private static HandbagConfiguration pickArchitectureCraft(World world, BlockPos pos) {
        return ArchitectureCraftIntegration.pickConfiguration(world, pos);
    }

    @Nullable
    @Optional.Method(modid = HandbagIntegration.BLOCKCRAFTERY_MODID)
    private static HandbagConfiguration pickBlockcraftery(World world, BlockPos pos, ItemStack result) {
        return BlockcrafteryIntegration.pickConfiguration(world, pos, result);
    }
}
