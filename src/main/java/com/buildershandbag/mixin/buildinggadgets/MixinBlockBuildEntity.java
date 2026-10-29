package com.buildershandbag.mixin.buildinggadgets;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.direwolf20.buildinggadgets.common.entities.BlockBuildEntity;

import com.buildershandbag.integration.BuildingGadgetsTarget;
import com.buildershandbag.integration.BuildingGadgetsEntity;
import com.buildershandbag.storage.HandbagOutputs;


@Mixin(value = BlockBuildEntity.class, remap = false)
public abstract class MixinBlockBuildEntity implements BuildingGadgetsEntity {

    @Unique
    private static final DataParameter<NBTTagCompound> buildershandbag$output =
        EntityDataManager.createKey(BlockBuildEntity.class, DataSerializers.COMPOUND_TAG);

    @Shadow
    private World world;
    @Shadow
    private BlockPos setPos;

    @Inject(method = "entityInit", at = @At("RETURN"), remap = true)
    private void buildershandbag$registerOutput(CallbackInfo callback) {
        ((Entity) (Object) this).getDataManager().register(buildershandbag$output, new NBTTagCompound());
    }

    @Inject(method = "readEntityFromNBT", at = @At("RETURN"), remap = true)
    private void buildershandbag$restoreOutput(NBTTagCompound compound, CallbackInfo callback) {
        Entity entity = (Entity) (Object) this;
        if (entity.getEntityData().hasKey(BuildingGadgetsTarget.NBT_TARGET)) {
            buildershandbag$setOutput(new ItemStack(
                entity.getEntityData().getCompoundTag(BuildingGadgetsTarget.NBT_TARGET)));
        }
    }

    @Override
    public ItemStack buildershandbag$getOutput() {
        return new ItemStack(((Entity) (Object) this).getDataManager().get(buildershandbag$output));
    }

    @Override
    public void buildershandbag$setOutput(ItemStack output) {
        ((Entity) (Object) this).getDataManager().set(buildershandbag$output, output.writeToNBT(new NBTTagCompound()));
    }

    @Redirect(method = "setDespawning", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z", ordinal = 1,
        remap = true))
    private boolean buildershandbag$transferTileData(World world, Entity next) {
        Entity current = (Entity) (Object) this;
        if (current.getEntityData().hasKey(BuildingGadgetsTarget.NBT_TARGET)) {
            next.getEntityData().setTag(BuildingGadgetsTarget.NBT_TARGET,
                current.getEntityData().getTag(BuildingGadgetsTarget.NBT_TARGET).copy());
            ((BuildingGadgetsEntity) next).buildershandbag$setOutput(new ItemStack(
                current.getEntityData().getCompoundTag(BuildingGadgetsTarget.NBT_TARGET)));
        }

        return world.spawnEntity(next);
    }

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
