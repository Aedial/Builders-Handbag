package com.buildershandbag.mixin.buildinggadgets;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.direwolf20.buildinggadgets.common.entities.BlockBuildEntity;

import com.buildershandbag.integration.BuildingGadgetsTarget;
import com.buildershandbag.storage.HandbagOutputs;


@Mixin(value = BlockBuildEntity.class, remap = false)
public abstract class MixinBlockBuildEntity {

    @Shadow
    private World world;
    @Shadow
    private BlockPos setPos;

    @Inject(method = "setDespawning", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/block/Block;neighborChanged(Lnet/minecraft/block/state/IBlockState;"
            + "Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;"
            + "Lnet/minecraft/util/math/BlockPos;)V", remap = true))
    private void buildershandbag$applyTileData(CallbackInfo callback) {
        Entity entity = (Entity) (Object) this;
        ItemStack output = new ItemStack(entity.getEntityData().getCompoundTag(BuildingGadgetsTarget.NBT_TARGET));
        HandbagOutputs.applyTile(world, setPos, output);
    }
}
