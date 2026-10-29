package com.buildershandbag.mixin.buildinggadgets;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.direwolf20.buildinggadgets.common.tools.GadgetUtils;
import com.direwolf20.buildinggadgets.common.tools.PasteToolBufferBuilder;

import com.buildershandbag.client.render.GadgetPreviewBuffer;
import com.buildershandbag.integration.BuildingGadgetsCopy;


@Mixin(value = PasteToolBufferBuilder.class, remap = false)
public abstract class MixinPasteToolBufferBuilder {

    @Redirect(method = "addMapToBuffer", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/BlockModelRenderer;renderModelFlat("
            + "Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;"
            + "Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z", remap = true))
    private static boolean buildershandbag$appendConfiguredBlock(BlockModelRenderer renderer, IBlockAccess access,
            IBakedModel model, IBlockState state, BlockPos relative, BufferBuilder buffer, boolean checkSides,
            long random, String uuid) {
        NBTTagCompound map = PasteToolBufferBuilder.getTagFromUUID(uuid);
        if (map != null && access instanceof World) {
            int packed = GadgetUtils.relPosToInt(BlockPos.ORIGIN, relative);
            ItemStack output = BuildingGadgetsCopy.getOutput(map, packed);
            BlockPos start = GadgetUtils.getPOSFromNBT(map, "startPos");
            if (!output.isEmpty() && start != null) {
                World world = (World) access;
                if (GadgetPreviewBuffer.append(Minecraft.getMinecraft().getBlockRendererDispatcher(), state, output,
                        world, start.add(relative), relative, buffer)) return true;
            }
        }

        return renderer.renderModelFlat(access, model, state, relative, buffer, checkSides, random);
    }
}
