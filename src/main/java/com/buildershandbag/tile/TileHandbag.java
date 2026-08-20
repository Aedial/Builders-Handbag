package com.buildershandbag.tile;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;


/**
 * Stores the original handbag stack so its selected configuration can be
 * rendered on both sides and returned intact when the block is broken.
 */
public class TileHandbag extends TileEntity {

    private static final String NBT_HANDBAG = "Handbag";

    private ItemStack handbag = ItemStack.EMPTY;

    @Nonnull
    public ItemStack getHandbagStack() {
        return handbag.copy();
    }

    public void setHandbagStack(ItemStack stack) {
        handbag = stack == null ? ItemStack.EMPTY : stack.copy();
        if (!handbag.isEmpty()) handbag.setCount(1);

        markDirty();
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        super.readFromNBT(compound);
        handbag = compound.hasKey(NBT_HANDBAG, Constants.NBT.TAG_COMPOUND)
            ? new ItemStack(compound.getCompoundTag(NBT_HANDBAG))
            : ItemStack.EMPTY;
        if (!handbag.isEmpty()) handbag.setCount(1);
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (!handbag.isEmpty()) compound.setTag(NBT_HANDBAG, handbag.writeToNBT(new NBTTagCompound()));
        return compound;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    @Nonnull
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(@Nonnull NetworkManager net, SPacketUpdateTileEntity packet) {
        handleUpdateTag(packet.getNbtCompound());
    }

    @Override
    public void handleUpdateTag(@Nonnull NBTTagCompound tag) {
        readFromNBT(tag);
    }
}
