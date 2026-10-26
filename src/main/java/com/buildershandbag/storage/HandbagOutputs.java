package com.buildershandbag.storage;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import com.buildershandbag.integration.HandbagIntegration;
import com.buildershandbag.network.HandbagNetwork;


/**
 * Configured output stacks and tile data used by external building tools.
 */
public final class HandbagOutputs {

    private static final String TILE_TAG = "BlockEntityTag";

    private HandbagOutputs() {
    }

    @SuppressWarnings("deprecation")
    public static ItemStack getResult(HandbagConfiguration configuration) {
        ItemStack result = configuration.getResult();
        if (configuration.getIntegration() != HandbagIntegration.BLOCKCRAFTERY) return result;

        ItemStack material = configuration.getMaterial();
        Block block = Block.getBlockFromItem(material.getItem());
        IBlockState state = block.getStateFromMeta(material.getMetadata());
        NBTTagCompound tile = new NBTTagCompound();
        tile.setTag("state", NBTUtil.writeBlockState(new NBTTagCompound(), state));
        tile.setTag("stack", material.writeToNBT(new NBTTagCompound()));
        result.setTagInfo(TILE_TAG, tile);

        return result;
    }

    public static boolean matches(HandbagConfiguration configuration, ItemStack requested) {
        if (!configuration.isAvailable() || requested.isEmpty()) return false;

        return sameOutput(getResult(configuration), requested);
    }

    public static boolean sameOutput(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty() || !ItemStack.areItemsEqual(first, second)) return false;

        ResourceLocation name = first.getItem().getRegistryName();
        if (name != null && name.getNamespace().equals(HandbagIntegration.BLOCKCRAFTERY_MODID)) {
            if (!first.hasTagCompound() || !second.hasTagCompound()
                    || !first.getTagCompound().hasKey(TILE_TAG, Constants.NBT.TAG_COMPOUND)
                    || !second.getTagCompound().hasKey(TILE_TAG, Constants.NBT.TAG_COMPOUND)) return false;

            return HandbagStorage.sameMaterial(
                getBlockcrafteryMaterial(first.getTagCompound().getCompoundTag(TILE_TAG)),
                getBlockcrafteryMaterial(second.getTagCompound().getCompoundTag(TILE_TAG)));
        }

        return HandbagStorage.sameMaterial(withoutTile(first), withoutTile(second));
    }

    private static ItemStack withoutTile(ItemStack stack) {
        ItemStack result = stack.copy();
        result.removeSubCompound(TILE_TAG);
        ResourceLocation name = result.getItem().getRegistryName();
        if (name != null && name.getNamespace().equals(HandbagIntegration.ARCHITECTURECRAFT_MODID)
                && result.hasTagCompound()) result.getTagCompound().removeTag("Disconnected");
        if (result.hasTagCompound() && result.getTagCompound().isEmpty()) result.setTagCompound(null);
        return result;
    }

    public static boolean requiresExactMatch(ItemStack output) {
        if (output.isEmpty() || !output.hasTagCompound()) return false;

        ResourceLocation name = output.getItem().getRegistryName();
        return name != null && (name.getNamespace().equals(HandbagIntegration.ARCHITECTURECRAFT_MODID)
            || name.getNamespace().equals(HandbagIntegration.BLOCKCRAFTERY_MODID));
    }

    public static IBlockState getPlacementState(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        ResourceLocation name = state.getBlock().getRegistryName();
        if (name == null || !name.getNamespace().equals(HandbagIntegration.BLOCKCRAFTERY_MODID)) return state;

        for (IProperty<?> property : state.getPropertyKeys()) {
            if (property instanceof PropertyBool && property.getName().equals("light")) {
                state = state.withProperty((PropertyBool) property, false);
            }
        }

        return state;
    }

    public static ItemStack capture(World world, BlockPos pos, EntityPlayer player) {
        IBlockState state = world.getBlockState(pos);
        ResourceLocation name = state.getBlock().getRegistryName();
        if (name == null) return ItemStack.EMPTY;

        boolean architecture = name.getNamespace().equals(HandbagIntegration.ARCHITECTURECRAFT_MODID);
        boolean blockcraftery = name.getNamespace().equals(HandbagIntegration.BLOCKCRAFTERY_MODID);
        if (!architecture && !blockcraftery) return ItemStack.EMPTY;

        TileEntity tile = world.getTileEntity(pos);
        if (tile == null) return ItemStack.EMPTY;

        NBTTagCompound data = tile.writeToNBT(new NBTTagCompound());
        ItemStack result = state.getBlock().getPickBlock(state,
            new RayTraceResult(new Vec3d(pos), EnumFacing.UP, pos), world, pos, player);
        if (result.isEmpty()) return ItemStack.EMPTY;
        if (blockcraftery && getBlockcrafteryMaterial(data).isEmpty()) return ItemStack.EMPTY;

        NBTTagCompound supplied = new NBTTagCompound();
        String[] fields = architecture
            ? new String[] {"Shape", "BaseName", "BaseData", "side", "turn", "offsetX", "Disconnected"}
            : new String[] {"state", "stack"};
        for (String field : fields) {
            if (data.hasKey(field)) supplied.setTag(field, data.getTag(field).copy());
        }
        result.setTagInfo(TILE_TAG, supplied);
        return result;
    }

    public static ItemStack getBlockcrafteryMaterial(NBTTagCompound tile) {
        ItemStack material = new ItemStack(tile.getCompoundTag("stack"));
        if (!material.isEmpty()) return material;

        IBlockState state = NBTUtil.readBlockState(tile.getCompoundTag("state"));
        return new ItemStack(state.getBlock(), 1, state.getBlock().damageDropped(state));
    }

    public static boolean applyTile(World world, BlockPos pos, ItemStack output) {
        if (world.isRemote || !requiresExactMatch(output)
                || !output.getTagCompound().hasKey(TILE_TAG, Constants.NBT.TAG_COMPOUND)) return false;

        TileEntity tile = world.getTileEntity(pos);
        Block block = Block.getBlockFromItem(output.getItem());
        if (tile == null || block != world.getBlockState(pos).getBlock()) return false;

        NBTTagCompound data = tile.writeToNBT(new NBTTagCompound());
        NBTTagCompound supplied = output.getTagCompound().getCompoundTag(TILE_TAG).copy();
        supplied.removeTag("id");
        supplied.removeTag("x");
        supplied.removeTag("y");
        supplied.removeTag("z");
        data.merge(supplied);

        tile.readFromNBT(data);
        tile.markDirty();

        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        HandbagNetwork.syncPlacedTile(world, pos, tile);

        return true;
    }
}
