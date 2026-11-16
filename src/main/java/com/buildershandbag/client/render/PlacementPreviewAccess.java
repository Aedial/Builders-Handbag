package com.buildershandbag.client.render;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * Read-only block access for the current block and its temporary tile data.
 */
@SideOnly(Side.CLIENT)
public final class PlacementPreviewAccess implements IBlockAccess {

    private final World world;
    private final BlockPos position;
    private final IBlockState state;
    @Nullable
    private final TileEntity tile;

    public PlacementPreviewAccess(World world, BlockPos position, IBlockState state, ItemStack output) {
        this.world = world;
        this.position = position;
        this.state = state;
        this.tile = state.getBlock().createTileEntity(world, state);
        if (tile == null) return;

        tile.setWorld(world);
        tile.setPos(position);
        NBTTagCompound data = tile.writeToNBT(new NBTTagCompound());
        NBTTagCompound supplied = output.getSubCompound("BlockEntityTag");
        if (supplied != null) data.merge(supplied);

        data.setInteger("x", position.getX());
        data.setInteger("y", position.getY());
        data.setInteger("z", position.getZ());
        tile.readFromNBT(data);
    }

    @Override
    @Nullable
    public TileEntity getTileEntity(BlockPos pos) {
        return position.equals(pos) ? tile : world.getTileEntity(pos);
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        return world.getCombinedLight(pos, lightValue);
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        return position.equals(pos) ? state : world.getBlockState(pos);
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        IBlockState blockState = getBlockState(pos);
        return blockState.getBlock().isAir(blockState, this, pos);
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return world.getBiome(pos);
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return getBlockState(pos).getStrongPower(this, pos, direction);
    }

    @Override
    public WorldType getWorldType() {
        return world.getWorldType();
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean defaultValue) {
        return position.equals(pos) ? state.isSideSolid(this, pos, side)
            : world.isSideSolid(pos, side, defaultValue);
    }
}
