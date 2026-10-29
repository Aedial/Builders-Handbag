package com.buildershandbag.mixin.buildinggadgets;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.direwolf20.buildinggadgets.common.entities.BlockBuildEntity;
import com.direwolf20.buildinggadgets.common.items.gadgets.GadgetExchanger;
import com.direwolf20.buildinggadgets.common.tools.InventoryManipulation;

import com.buildershandbag.integration.BuildingGadgetsTarget;
import com.buildershandbag.integration.BuildingGadgetsEntity;


@Mixin(value = GadgetExchanger.class, remap = false)
public abstract class MixinGadgetExchanger {

    @Inject(method = "exchangeBlock", at = @At("HEAD"), cancellable = true)
    private void buildershandbag$requireConfiguredOutput(World world, EntityPlayer player, BlockPos pos,
            IBlockState state, CallbackInfoReturnable<Boolean> callback) {
        ItemStack output = BuildingGadgetsTarget.get(GadgetExchanger.getGadget(player));
        if (!output.isEmpty() && InventoryManipulation.countItem(output, player, world) <= 0) {
            callback.setReturnValue(false);
        }
    }

    @Redirect(method = "exchange", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/block/state/IBlockState;getActualState(Lnet/minecraft/world/IBlockAccess;"
            + "Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;", remap = true))
    private IBlockState buildershandbag$retainConfiguredState(IBlockState state, IBlockAccess world, BlockPos pos,
            EntityPlayer player, ItemStack gadget) {
        return BuildingGadgetsTarget.get(gadget).isEmpty() ? state.getActualState(world, pos) : state;
    }

    @Redirect(method = "exchangeBlock", at = @At(value = "INVOKE",
        target = "Lcom/direwolf20/buildinggadgets/common/tools/InventoryManipulation;"
            + "getSilkTouchDrop(Lnet/minecraft/block/state/IBlockState;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack buildershandbag$getConfiguredDrop(IBlockState state, World world, EntityPlayer player,
            BlockPos pos, IBlockState setBlock) {
        ItemStack output = BuildingGadgetsTarget.get(GadgetExchanger.getGadget(player));
        return output.isEmpty() ? InventoryManipulation.getSilkTouchDrop(state) : output;
    }

    @Redirect(method = "exchangeBlock", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/block/Block;getPickBlock(Lnet/minecraft/block/state/IBlockState;"
            + "Lnet/minecraft/util/math/RayTraceResult;Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/EntityPlayer;)"
            + "Lnet/minecraft/item/ItemStack;", remap = false))
    private ItemStack buildershandbag$getConfiguredPick(Block block, IBlockState state, RayTraceResult target,
            World world, BlockPos pos, EntityPlayer player) {
        ItemStack output = BuildingGadgetsTarget.get(GadgetExchanger.getGadget(player));
        return output.isEmpty() ? block.getPickBlock(state, target, world, pos, player) : output;
    }

    @Redirect(method = "exchangeBlock", at = @At(value = "INVOKE",
        target = "Lcom/direwolf20/buildinggadgets/common/tools/InventoryManipulation;"
            + "countPaste(Lnet/minecraft/entity/player/EntityPlayer;)I"))
    private int buildershandbag$countPaste(EntityPlayer player) {
        return BuildingGadgetsTarget.get(GadgetExchanger.getGadget(player)).isEmpty()
            ? InventoryManipulation.countPaste(player) : 0;
    }

    @Redirect(method = "exchangeBlock", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z", remap = true))
    private boolean buildershandbag$retainTileData(World world, Entity entity, World placementWorld,
            EntityPlayer player, BlockPos pos, IBlockState state) {
        ItemStack output = BuildingGadgetsTarget.get(GadgetExchanger.getGadget(player));
        if (!output.isEmpty() && entity instanceof BlockBuildEntity
                && !((BlockBuildEntity) entity).getUsingConstructionPaste()) {
            entity.getEntityData().setTag(BuildingGadgetsTarget.NBT_TARGET, output.writeToNBT(new NBTTagCompound()));
            ((BuildingGadgetsEntity) entity).buildershandbag$setOutput(output);
        }

        return world.spawnEntity(entity);
    }
}
