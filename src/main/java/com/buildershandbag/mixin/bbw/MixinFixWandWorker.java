package com.buildershandbag.mixin.bbw;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import portablejim.bbw.core.WandWorker;

import com.buildershandbag.storage.HandbagOutputs;


@Mixin(value = WandWorker.class, remap = false, priority = 1100)
public abstract class MixinFixWandWorker {

    @Redirect(method = "placeBlocks", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/item/ItemBlock;placeBlockAt(Lnet/minecraft/item/ItemStack;"
            + "Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;FFF"
            + "Lnet/minecraft/block/state/IBlockState;)Z"))
    private boolean buildershandbag$applyTileData(ItemBlock block, ItemStack output, EntityPlayer player,
            World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState state) {
        boolean placed = block.placeBlockAt(output, player, world, pos, side, hitX, hitY, hitZ, state);
        if (placed) HandbagOutputs.applyTile(world, pos, output);

        return placed;
    }
}
