package com.buildershandbag.mixin.buildinggadgets;

import net.minecraft.block.state.IBlockState;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.direwolf20.buildinggadgets.common.config.SyncedConfig;
import com.direwolf20.buildinggadgets.common.items.gadgets.GadgetBuilding;
import com.direwolf20.buildinggadgets.common.tools.GadgetUtils;
import com.direwolf20.buildinggadgets.common.tools.VectorTools;

import com.buildershandbag.integration.BuildingGadgetsTarget;
import com.buildershandbag.storage.HandbagOutputs;
import com.buildershandbag.storage.HandbagOutputSupply;


@Mixin(value = GadgetUtils.class, remap = false)
public abstract class MixinGadgetUtils {

    @Shadow
    private static void setToolBlock(ItemStack gadget, IBlockState state) {
        throw new AssertionError();
    }

    @Shadow
    private static void setToolActualBlock(ItemStack gadget, IBlockState state) {
        throw new AssertionError();
    }

    @Inject(method = "selectBlock", at = @At("HEAD"), cancellable = true)
    private static void buildershandbag$selectConfiguredBlock(ItemStack gadget, EntityPlayer player,
            CallbackInfo callback) {
        if (!(gadget.getItem() instanceof GadgetBuilding)) return;

        RayTraceResult lookingAt = VectorTools.getLookingAt(player, false);
        if (lookingAt == null || lookingAt.typeOfHit != RayTraceResult.Type.BLOCK) return;

        BlockPos pos = lookingAt.getBlockPos();
        IBlockState state = HandbagOutputs.getPlacementState(player.world, pos);
        if (SyncedConfig.blockBlacklist.contains(state.getBlock())) return;

        ItemStack output = HandbagOutputs.capture(player.world, pos, player);
        if (!HandbagOutputSupply.hasConfiguration(player, output)) return;

        setToolBlock(gadget, state);
        setToolActualBlock(gadget, state.getActualState(player.world, pos));
        BuildingGadgetsTarget.set(gadget, output);
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        callback.cancel();
    }

    @Inject(method = "setToolBlock", at = @At("HEAD"))
    private static void buildershandbag$clearPreviousTarget(ItemStack gadget, IBlockState state,
            CallbackInfo callback) {
        if (!gadget.hasTagCompound()) return;

        ItemStack output = new ItemStack(gadget.getTagCompound().getCompoundTag(BuildingGadgetsTarget.NBT_TARGET));
        if (state == null || Block.getBlockFromItem(output.getItem()) != state.getBlock()) {
            gadget.getTagCompound().removeTag(BuildingGadgetsTarget.NBT_TARGET);
        }
    }
}
