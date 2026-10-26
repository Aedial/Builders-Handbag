package com.buildershandbag.mixin.bbw;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import portablejim.bbw.basics.Point3d;
import portablejim.bbw.BetterBuildersWandsMod;
import portablejim.bbw.basics.ReplacementTriplet;
import portablejim.bbw.core.WandWorker;
import portablejim.bbw.shims.IPlayerShim;
import portablejim.bbw.shims.IWorldShim;

import com.buildershandbag.storage.HandbagOutputs;
import com.buildershandbag.storage.HandbagOutputSupply;


@Mixin(value = WandWorker.class, remap = false)
public abstract class MixinWandWorker {

    @Inject(method = "getProperItemStack", at = @At("HEAD"), cancellable = true)
    private void buildershandbag$selectConfiguredBlock(IWorldShim world, IPlayerShim player, Point3d pos,
            float hitX, float hitY, float hitZ, CallbackInfoReturnable<ReplacementTriplet> callback) {
        IBlockState state = HandbagOutputs.getPlacementState(world.getWorld(), pos.toBlockPos());
        String block = String.format("%s/%s", state.getBlock().getRegistryName(), world.getMetadata(pos));
        if (BetterBuildersWandsMod.instance.configValues.HARD_BLACKLIST_SET.contains(block)) return;

        ItemStack output = HandbagOutputs.capture(world.getWorld(), pos.toBlockPos(), player.getPlayer());
        if (!HandbagOutputSupply.hasConfiguration(player.getPlayer(), output)
                || player.countItems(output) <= 0) return;

        callback.setReturnValue(new ReplacementTriplet(state, output, state));
    }

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
