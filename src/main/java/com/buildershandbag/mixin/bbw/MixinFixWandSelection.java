package com.buildershandbag.mixin.bbw;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.ForgeHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import portablejim.bbw.BetterBuildersWandsMod;
import portablejim.bbw.basics.Point3d;
import portablejim.bbw.basics.ReplacementTriplet;
import portablejim.bbw.core.WandWorker;
import portablejim.bbw.core.conversion.CustomMapping;
import portablejim.bbw.shims.IPlayerShim;
import portablejim.bbw.shims.IWorldShim;

import com.buildershandbag.item.ItemHandbag;


@Mixin(value = WandWorker.class, remap = false, priority = 900)
public abstract class MixinFixWandSelection {

    @Invoker("getEquivalentItemStack")
    public abstract ReplacementTriplet buildershandbag$getEquivalentItemStack(Point3d pos);

    @Inject(method = "getProperItemStack", at = @At(value = "INVOKE",
        target = "Lportablejim/bbw/core/conversion/CustomMappingManager;"
            + "getMappings(Lnet/minecraft/block/Block;I)Ljava/util/ArrayList;"), cancellable = true)
    private void buildershandbag$selectWithOffhandHandbag(IWorldShim world, IPlayerShim player, Point3d pos,
            float hitX, float hitY, float hitZ, CallbackInfoReturnable<ReplacementTriplet> callback) {
        if (!(player.getPlayer().getHeldItemOffhand().getItem() instanceof ItemHandbag)) return;

        callback.setReturnValue(buildershandbag$selectBlock(world, player, pos, hitX, hitY, hitZ));
    }

    @Unique
    private ReplacementTriplet buildershandbag$selectBlock(IWorldShim world, IPlayerShim player, Point3d pos,
            float hitX, float hitY, float hitZ) {
        Block block = world.getBlock(pos);
        IBlockState state = world.getWorld().getBlockState(pos.toBlockPos());
        int meta = world.getMetadata(pos);
        for (CustomMapping mapping : BetterBuildersWandsMod.instance.mappingManager.getMappings(block, meta)) {
            if (player.countItems(mapping.getItems()) > 0) {
                IBlockState look = mapping.getLookBlock().getStateFromMeta(mapping.getMeta());
                IBlockState target = mapping.getPlaceBlock().getStateFromMeta(mapping.getPlaceMeta());
                return new ReplacementTriplet(look, mapping.getItems(), target);
            }
        }

        if (state.getBlock() instanceof BlockSlab) {
            Item item = state.getBlock().getItemDropped(state, world.rand(), 0);
            ItemStack dropped = new ItemStack(item, state.getBlock().quantityDropped(world.rand()),
                state.getBlock().damageDropped(state));
            if (player.countItems(dropped) > 0) return new ReplacementTriplet(state, dropped, state);
        }

        RayTraceResult trace = ForgeHooks.rayTraceEyes(player.getPlayer(), player.getReachDistance());
        if (trace == null) return null;

        ItemStack picked = block.getPickBlock(state, trace, world.getWorld(), pos.toBlockPos(), player.getPlayer());
        if (player.countItems(picked) > 0) {
            if (!(picked.getItem() instanceof ItemBlock)) return null;

            Block targetBlock = ((ItemBlock) picked.getItem()).getBlock();
            EnumFacing facing = player.getPlayer().getHorizontalFacing();
            IBlockState target = targetBlock.getStateForPlacement(world.getWorld(), pos.toBlockPos(),
                facing, hitX, hitY, hitZ, meta, player.getPlayer(), EnumHand.MAIN_HAND);
            return new ReplacementTriplet(state, picked, target);
        }

        return buildershandbag$getEquivalentItemStack(pos);
    }
}
