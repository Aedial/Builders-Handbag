package com.buildershandbag.mixin.buildinggadgets;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.direwolf20.buildinggadgets.common.entities.BlockBuildEntity;
import com.direwolf20.buildinggadgets.common.items.gadgets.GadgetBuilding;
import com.direwolf20.buildinggadgets.common.tools.InventoryManipulation;

import com.buildershandbag.integration.BuildingGadgetsTarget;


@Mixin(value = GadgetBuilding.class, remap = false)
public abstract class MixinGadgetBuilding {

    @Inject(method = "placeBlock", at = @At("HEAD"), cancellable = true)
    private void buildershandbag$requireConfiguredOutput(World world, EntityPlayer player, BlockPos pos,
            IBlockState state, CallbackInfoReturnable<Boolean> callback) {
        ItemStack output = BuildingGadgetsTarget.get(GadgetBuilding.getGadget(player));
        if (!output.isEmpty() && InventoryManipulation.countItem(output, player, world) <= 0) {
            callback.setReturnValue(false);
        }
    }

    @Redirect(method = "build", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/block/state/IBlockState;getActualState(Lnet/minecraft/world/IBlockAccess;"
            + "Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;", remap = true))
    private IBlockState buildershandbag$retainConfiguredState(IBlockState state, IBlockAccess world, BlockPos pos,
            EntityPlayer player, ItemStack gadget) {
        return BuildingGadgetsTarget.get(gadget).isEmpty() ? state.getActualState(world, pos) : state;
    }

    @Redirect(method = "placeBlock", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z", remap = true))
    private boolean buildershandbag$retainTileData(World world, Entity entity, World placementWorld,
            EntityPlayer player, BlockPos pos, IBlockState state) {
        ItemStack output = BuildingGadgetsTarget.get(GadgetBuilding.getGadget(player));
        if (!output.isEmpty() && entity instanceof BlockBuildEntity
                && !((BlockBuildEntity) entity).getUsingConstructionPaste()) {
            entity.getEntityData().setTag(BuildingGadgetsTarget.NBT_TARGET, output.writeToNBT(new NBTTagCompound()));
        }

        return world.spawnEntity(entity);
    }
}
