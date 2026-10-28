package com.buildershandbag.mixin.buildinggadgets;

import java.util.Map;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.direwolf20.buildinggadgets.common.entities.BlockBuildEntity;
import com.direwolf20.buildinggadgets.common.items.gadgets.GadgetCopyPaste;
import com.direwolf20.buildinggadgets.common.network.PacketRotateMirror;
import com.direwolf20.buildinggadgets.common.tools.InventoryManipulation;
import com.direwolf20.buildinggadgets.common.tools.UniqueItem;
import com.direwolf20.buildinggadgets.common.tools.WorldSave;

import com.buildershandbag.integration.BuildingGadgetsCopy;
import com.buildershandbag.integration.BuildingGadgetsTarget;
import com.buildershandbag.storage.HandbagOutputs;


@Mixin(value = GadgetCopyPaste.class, remap = false)
public abstract class MixinGadgetCopyPaste {

    // FIXME: BlockCraftery doesn't show preview and ArchitectureCraft shows broken textures preview,
    //        but I feel they are related to their respective render() methods.

    @Redirect(method = "findBlocks", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/World;getTileEntity(Lnet/minecraft/util/math/BlockPos;)"
            + "Lnet/minecraft/tileentity/TileEntity;", ordinal = 0, remap = true))
    private static TileEntity buildershandbag$includeConfiguredBlock(World world, BlockPos pos,
            World copyWorld, BlockPos start, BlockPos end, ItemStack gadget, EntityPlayer player,
            GadgetCopyPaste tool) {
        return BuildingGadgetsCopy.capture(world, pos, player).isEmpty() ? world.getTileEntity(pos) : null;
    }

    @Redirect(method = "findBlocks", at = @At(value = "INVOKE",
        target = "Lcom/direwolf20/buildinggadgets/common/tools/InventoryManipulation;getSpecificStates("
            + "Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;"
            + "Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/item/ItemStack;)Lnet/minecraft/block/state/IBlockState;"))
    private static IBlockState buildershandbag$copyConfiguredState(IBlockState state, World world,
            EntityPlayer player, BlockPos pos, ItemStack gadget) {
        return BuildingGadgetsCopy.capture(world, pos, player).isEmpty()
            ? InventoryManipulation.getSpecificStates(state, world, player, pos, gadget)
            : HandbagOutputs.getPlacementState(world, pos);
    }

    @Redirect(method = "findBlocks", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/block/state/IBlockState;getActualState(Lnet/minecraft/world/IBlockAccess;"
            + "Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;", remap = true))
    private static IBlockState buildershandbag$retainConfiguredState(IBlockState state, IBlockAccess access,
            BlockPos pos, World world, BlockPos start, BlockPos end, ItemStack gadget, EntityPlayer player,
            GadgetCopyPaste tool) {
        return BuildingGadgetsCopy.capture(world, pos, player).isEmpty() ? state.getActualState(access, pos) : state;
    }

    @Redirect(method = "findBlocks", at = @At(value = "INVOKE",
        target = "Lcom/direwolf20/buildinggadgets/common/tools/WorldSave;"
            + "addToMap(Ljava/lang/String;Lnet/minecraft/nbt/NBTTagCompound;)V"))
    private static void buildershandbag$saveOutputs(WorldSave save, String uuid, NBTTagCompound map,
            World world, BlockPos start, BlockPos end, ItemStack gadget, EntityPlayer player, GadgetCopyPaste tool) {
        BuildingGadgetsCopy.captureOutputs(map, world, start, player);
        save.addToMap(uuid, map);
    }

    @Redirect(method = "rotateOrMirrorBlocks", at = @At(value = "INVOKE",
        target = "Lcom/direwolf20/buildinggadgets/common/tools/WorldSave;"
            + "addToMap(Ljava/lang/String;Lnet/minecraft/nbt/NBTTagCompound;)V"))
    private static void buildershandbag$transformOutputs(WorldSave save, String uuid, NBTTagCompound map,
            ItemStack gadget, EntityPlayer player, PacketRotateMirror.Operation operation) {
        BuildingGadgetsCopy.transform(map, player, operation);
        save.addToMap(uuid, map);
    }

    @Redirect(method = "placeBlock", at = @At(value = "INVOKE",
        target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object buildershandbag$getOutputItem(Map<IBlockState, UniqueItem> stacks, Object key,
            World world, BlockPos pos, EntityPlayer player, IBlockState state, Map<IBlockState, UniqueItem> items) {
        ItemStack output = BuildingGadgetsCopy.getOutput(world, player, pos);
        return output.isEmpty() ? stacks.get(key) : new UniqueItem(output.getItem(), output.getMetadata());
    }

    @Redirect(method = "placeBlock", at = @At(value = "INVOKE",
        target = "Lcom/direwolf20/buildinggadgets/common/tools/InventoryManipulation;"
            + "countItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;"
            + "Lnet/minecraft/world/World;)I"))
    private int buildershandbag$countOutput(ItemStack requested, EntityPlayer player, World world,
            World placementWorld, BlockPos pos, EntityPlayer placingPlayer, IBlockState state,
            Map<IBlockState, UniqueItem> stacks) {
        ItemStack output = BuildingGadgetsCopy.getOutput(world, player, pos);
        return InventoryManipulation.countItem(output.isEmpty() ? requested : output, player, world);
    }

    @Redirect(method = "placeBlock", at = @At(value = "INVOKE",
        target = "Lcom/direwolf20/buildinggadgets/common/tools/InventoryManipulation;"
            + "countPaste(Lnet/minecraft/entity/player/EntityPlayer;)I"))
    private int buildershandbag$countPaste(EntityPlayer player, World world, BlockPos pos,
            EntityPlayer placingPlayer, IBlockState state, Map<IBlockState, UniqueItem> stacks) {
        return BuildingGadgetsCopy.getOutput(world, player, pos).isEmpty()
            ? InventoryManipulation.countPaste(player) : 0;
    }

    @Redirect(method = "placeBlock", at = @At(value = "INVOKE",
        target = "Lcom/direwolf20/buildinggadgets/common/tools/InventoryManipulation;"
            + "useItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;"
            + "ILnet/minecraft/world/World;)Z"))
    private boolean buildershandbag$useOutput(ItemStack requested, EntityPlayer player, int amount, World world,
            World placementWorld, BlockPos pos, EntityPlayer placingPlayer, IBlockState state,
            Map<IBlockState, UniqueItem> stacks) {
        ItemStack output = BuildingGadgetsCopy.getOutput(world, player, pos);
        return InventoryManipulation.useItem(output.isEmpty() ? requested : output, player, amount, world);
    }

    @Redirect(method = "placeBlock", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z", remap = true))
    private boolean buildershandbag$retainTileData(World world, Entity entity, World placementWorld,
            BlockPos pos, EntityPlayer player, IBlockState state, Map<IBlockState, UniqueItem> stacks) {
        ItemStack output = BuildingGadgetsCopy.getOutput(world, player, pos);
        if (!output.isEmpty() && entity instanceof BlockBuildEntity
                && !((BlockBuildEntity) entity).getUsingConstructionPaste()) {
            entity.getEntityData().setTag(BuildingGadgetsTarget.NBT_TARGET, output.writeToNBT(new NBTTagCompound()));
        }

        return world.spawnEntity(entity);
    }
}
