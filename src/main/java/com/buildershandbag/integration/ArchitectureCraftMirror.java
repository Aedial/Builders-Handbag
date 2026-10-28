package com.buildershandbag.integration;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants;

import com.elytradev.architecture.common.helpers.Matrix3;


/**
 * Mirrored orientation and tile data for copied ArchitectureCraft shapes.
 */
public final class ArchitectureCraftMirror {

    private ArchitectureCraftMirror() {
    }

    public static void apply(ItemStack output, EnumFacing.Axis facingAxis) {
        if (!output.hasTagCompound()
                || !output.getTagCompound().hasKey("BlockEntityTag", Constants.NBT.TAG_COMPOUND)) return;

        NBTTagCompound tile = output.getTagCompound().getCompoundTag("BlockEntityTag");
        int side = tile.getByte("side");
        int turn = tile.getByte("turn");
        if (side < 0 || side >= 6 || turn < 0 || turn >= 4) return;

        int mirrorRow = facingAxis == EnumFacing.Axis.X ? 2 : 0;
        Matrix3 original = Matrix3.sideTurnRotations[side][turn];
        for (int newSide = 0; newSide < 6; newSide++) {
            for (int newTurn = 0; newTurn < 4; newTurn++) {
                if (!matches(original, Matrix3.sideTurnRotations[newSide][newTurn], mirrorRow)) continue;

                tile.setByte("side", (byte) newSide);
                tile.setByte("turn", (byte) newTurn);
                if (tile.hasKey("offsetX")) tile.setByte("offsetX", (byte) -tile.getByte("offsetX"));
                mirrorConnections(tile, facingAxis == EnumFacing.Axis.X ? EnumFacing.Axis.Z : EnumFacing.Axis.X);
                return;
            }
        }
    }

    private static boolean matches(Matrix3 original, Matrix3 candidate, int mirrorRow) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                long value = Math.round(original.m[row][column]);
                if (row == mirrorRow) value = -value;
                if (column == 0) value = -value;
                if (value != Math.round(candidate.m[row][column])) return false;
            }
        }

        return true;
    }

    private static void mirrorConnections(NBTTagCompound tile, EnumFacing.Axis mirrorAxis) {
        if (!tile.hasKey("Disconnected")) return;

        int disconnected = tile.getInteger("Disconnected");
        int mirrored = disconnected & ~0x3F;
        for (EnumFacing facing : EnumFacing.values()) {
            if ((disconnected & (1 << facing.ordinal())) == 0) continue;

            EnumFacing target = facing.getAxis() == mirrorAxis ? facing.getOpposite() : facing;
            mirrored |= 1 << target.ordinal();
        }
        tile.setInteger("Disconnected", mirrored);
    }
}
