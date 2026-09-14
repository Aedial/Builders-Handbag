package com.buildershandbag.tile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

import com.buildershandbag.storage.HandbagConfiguration;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Stores the original handbag stack so its selected configuration can be
 * rendered on both sides and returned intact when the block is broken.
 */
public class TileHandbag extends TileEntity {

    private static final String NBT_HANDBAG = "Handbag";

    private ItemStack handbag = ItemStack.EMPTY;
    @Nullable
    private HandbagConfiguration selectedConfiguration;

    @Nonnull
    public ItemStack getHandbagStack() {
        return handbag.copy();
    }

    /**
     * Immutable render data, refreshed when the stored handbag stack changes.
     * This keeps NBT decoding and ItemStack copies off the TESR hot path.
     */
    @Nullable
    public HandbagConfiguration getSelectedConfiguration() {
        return selectedConfiguration;
    }

    public void setHandbagStack(ItemStack stack) {
        ItemStack replacement = stack == null ? ItemStack.EMPTY : stack;
        if (!hasSameRenderData(handbag, replacement)) {
            handbag = replacement.copy();
            if (!handbag.isEmpty()) handbag.setCount(1);
            refreshSelectedConfiguration();
        }

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
        refreshSelectedConfiguration();
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

    private void refreshSelectedConfiguration() {
        selectedConfiguration = HandbagStorage.getSelectedConfiguration(handbag);
    }

    private static boolean hasSameRenderData(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) return first.isEmpty() && second.isEmpty();

        return ItemStack.areItemsEqual(first, second) && ItemStack.areItemStackTagsEqual(first, second);
    }
}
