package com.buildershandbag.mixin.bbw;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import portablejim.bbw.core.WandWorker;

import com.buildershandbag.storage.HandbagOutputs;


@Mixin(value = WandWorker.class, remap = false)
public abstract class MixinBaseWandWorker {

    @Redirect(method = "placeBlocks", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/block/Block;onBlockPlacedBy(Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;"
            + "Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;)V", remap = true))
    private void buildershandbag$applyTileData(Block block, World world, BlockPos pos, IBlockState state,
            EntityLivingBase player, ItemStack output) {
        if (output == null) return;

        block.onBlockPlacedBy(world, pos, state, player, output);
        HandbagOutputs.applyTile(world, pos, output);
    }
}
