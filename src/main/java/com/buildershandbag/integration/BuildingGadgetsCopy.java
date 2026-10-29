package com.buildershandbag.integration;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.Optional;

import com.direwolf20.buildinggadgets.common.items.gadgets.GadgetCopyPaste;
import com.direwolf20.buildinggadgets.common.network.PacketRotateMirror;
import com.direwolf20.buildinggadgets.common.tools.GadgetUtils;
import com.direwolf20.buildinggadgets.common.tools.WorldSave;

import com.buildershandbag.storage.HandbagOutputSupply;
import com.buildershandbag.storage.HandbagOutputs;


/**
 * Configured outputs associated with copied block positions.
 */
public final class BuildingGadgetsCopy {

    public static final String NBT_OUTPUTS = "BuildersHandbagOutputs";
    private static final String NBT_PALETTE = "BuildersHandbagPalette";

    private BuildingGadgetsCopy() {
    }

    public static ItemStack capture(World world, BlockPos pos, EntityPlayer player) {
        ResourceLocation name = world.getBlockState(pos).getBlock().getRegistryName();
        if (name == null) return ItemStack.EMPTY;
        if (!name.getNamespace().equals(HandbagIntegration.ARCHITECTURECRAFT_MODID)
                && !name.getNamespace().equals(HandbagIntegration.BLOCKCRAFTERY_MODID)) return ItemStack.EMPTY;

        if (name.getNamespace().equals(HandbagIntegration.ARCHITECTURECRAFT_MODID)) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile == null) return ItemStack.EMPTY;

            String secondary = tile.writeToNBT(new NBTTagCompound()).getString("Name2");
            if (!secondary.isEmpty() && !secondary.equals("minecraft:air")) return ItemStack.EMPTY;
        }

        ItemStack output = HandbagOutputs.capture(world, pos, player);
        return HandbagOutputSupply.hasConfiguration(player, output) ? output : ItemStack.EMPTY;
    }

    public static void captureOutputs(NBTTagCompound map, World world, BlockPos start, EntityPlayer player) {
        NBTTagCompound outputs = new NBTTagCompound();
        List<ItemStack> palette = new ArrayList<>();
        for (int relative : map.getIntArray("posIntArray")) {
            BlockPos pos = GadgetUtils.relIntToPos(start, relative);
            ItemStack output = capture(world, pos, player);
            if (output.isEmpty()) continue;

            int index = 0;
            while (index < palette.size() && !ItemStack.areItemStacksEqual(palette.get(index), output)) index++;
            if (index == palette.size()) palette.add(output);
            outputs.setInteger(Integer.toString(relative), index);
        }

        if (!outputs.isEmpty()) {
            NBTTagList saved = new NBTTagList();
            for (ItemStack output : palette) saved.appendTag(output.writeToNBT(new NBTTagCompound()));
            map.setTag(NBT_OUTPUTS, outputs);
            map.setTag(NBT_PALETTE, saved);
        }
    }

    public static ItemStack getOutput(World world, EntityPlayer player, BlockPos pos) {
        ItemStack gadget = GadgetCopyPaste.getGadget(player);
        if (gadget.isEmpty()) return ItemStack.EMPTY;

        BlockPos start = GadgetUtils.getPOSFromNBT(gadget, "lastBuild");
        String uuid = ((GadgetCopyPaste) gadget.getItem()).getUUID(gadget);
        if (start == null || uuid == null) return ItemStack.EMPTY;

        NBTTagCompound map = WorldSave.getWorldSave(world).getCompoundFromUUID(uuid);
        if (map == null) return ItemStack.EMPTY;

        return getOutput(map, GadgetUtils.relPosToInt(start, pos));
    }

    public static ItemStack getOutput(NBTTagCompound map, int relative) {
        NBTTagCompound outputs = map.getCompoundTag(NBT_OUTPUTS);
        String key = Integer.toString(relative);
        if (!outputs.hasKey(key, Constants.NBT.TAG_INT)) return ItemStack.EMPTY;

        NBTTagList palette = map.getTagList(NBT_PALETTE, Constants.NBT.TAG_COMPOUND);
        int index = outputs.getInteger(key);
        if (index < 0 || index >= palette.tagCount()) return ItemStack.EMPTY;

        return new ItemStack(palette.getCompoundTagAt(index));
    }

    public static void transform(NBTTagCompound map, EntityPlayer player, PacketRotateMirror.Operation operation) {
        NBTTagCompound outputs = map.getCompoundTag(NBT_OUTPUTS);
        if (outputs.isEmpty()) return;

        NBTTagList palette = map.getTagList(NBT_PALETTE, Constants.NBT.TAG_COMPOUND);
        NBTTagList transformedPalette = new NBTTagList();
        for (int index = 0; index < palette.tagCount(); index++) {
            ItemStack output = new ItemStack(palette.getCompoundTagAt(index));
            ResourceLocation name = output.getItem().getRegistryName();
            if (name != null && name.getNamespace().equals(HandbagIntegration.ARCHITECTURECRAFT_MODID)) {
                if (operation == PacketRotateMirror.Operation.ROTATE) rotateArchitecture(output);
                else mirrorArchitecture(output, player.getHorizontalFacing().getAxis());
            }

            transformedPalette.appendTag(output.writeToNBT(new NBTTagCompound()));
        }
        map.setTag(NBT_PALETTE, transformedPalette);

        NBTTagCompound transformed = new NBTTagCompound();
        for (String key : outputs.getKeySet()) {
            BlockPos pos = GadgetUtils.relIntToPos(BlockPos.ORIGIN, Integer.parseInt(key));
            int x = pos.getX();
            int z = pos.getZ();
            if (operation == PacketRotateMirror.Operation.MIRROR) {
                if (player.getHorizontalFacing().getAxis() == EnumFacing.Axis.X) z = -z;
                else x = -x;
            } else {
                x = -pos.getZ();
                z = pos.getX();
            }

            int relative = GadgetUtils.relPosToInt(BlockPos.ORIGIN, new BlockPos(x, pos.getY(), z));
            transformed.setInteger(Integer.toString(relative), outputs.getInteger(key));
        }
        map.setTag(NBT_OUTPUTS, transformed);
    }

    @Optional.Method(modid = HandbagIntegration.ARCHITECTURECRAFT_MODID)
    private static void mirrorArchitecture(ItemStack output, EnumFacing.Axis facingAxis) {
        ArchitectureCraftMirror.apply(output, facingAxis);
    }

    private static void rotateArchitecture(ItemStack output) {
        if (!output.hasTagCompound()) return;

        NBTTagCompound tile = output.getTagCompound().getCompoundTag("BlockEntityTag");
        int side = tile.getByte("side");
        int turn = tile.getByte("turn");
        if (side == 0) turn = (turn + 3) & 3;
        else if (side == 1) turn = (turn + 1) & 3;
        else side = EnumFacing.byHorizontalIndex((EnumFacing.byIndex(side).getHorizontalIndex() + 1) & 3)
            .ordinal();

        int disconnected = tile.getInteger("Disconnected");
        int rotated = disconnected & 3;
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            if ((disconnected & (1 << facing.ordinal())) == 0) continue;

            EnumFacing next = EnumFacing.byHorizontalIndex((facing.getHorizontalIndex() + 1) & 3);
            rotated |= 1 << next.ordinal();
        }

        tile.setByte("side", (byte) side);
        tile.setByte("turn", (byte) turn);

        if (disconnected != 0) tile.setInteger("Disconnected", rotated);
    }
}
