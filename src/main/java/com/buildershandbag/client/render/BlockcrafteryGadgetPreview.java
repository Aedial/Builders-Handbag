package com.buildershandbag.client.render;

import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import epicsquid.blockcraftery.block.IEditableBlock;


@SideOnly(Side.CLIENT)
public final class BlockcrafteryGadgetPreview {

    private BlockcrafteryGadgetPreview() {
    }

    public static boolean render(BlockRendererDispatcher dispatcher, IBlockState state, float brightness,
            ItemStack output) {
        if (!(state.getBlock() instanceof IEditableBlock) || !(state instanceof IExtendedBlockState)) return false;

        IBlockState material = getMaterial(output);
        IBlockState preview = getPreview(state, material);
        BlockRenderLayer layer = material.getBlock() == Blocks.AIR
            ? BlockRenderLayer.CUTOUT_MIPPED : material.getBlock().getRenderLayer();

        BlockRenderLayer previous = MinecraftForgeClient.getRenderLayer();
        ForgeHooksClient.setRenderLayer(layer);
        try {
            GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
            renderModel(dispatcher.getModelForState(state), preview, brightness);
        } finally {
            ForgeHooksClient.setRenderLayer(previous);
        }

        return true;
    }

    public static boolean append(BlockRendererDispatcher dispatcher, IBlockState state, ItemStack output,
            World world, BlockPos source, BlockPos relative, BufferBuilder buffer) {
        if (!(state.getBlock() instanceof IEditableBlock) || !(state instanceof IExtendedBlockState)
                || !output.hasTagCompound()) return false;

        IBlockState material = getMaterial(output);
        if (material.getBlock() == Blocks.AIR) return false;

        IBlockState preview = getPreview(state, material);
        BlockRenderLayer previous = MinecraftForgeClient.getRenderLayer();
        ForgeHooksClient.setRenderLayer(material.getBlock().getRenderLayer());
        try {
            GadgetPreviewBuffer.appendModel(dispatcher.getModelForState(state), preview, material,
                world, source, relative, buffer);
        } finally {
            ForgeHooksClient.setRenderLayer(previous);
        }

        return true;
    }

    private static IBlockState getMaterial(ItemStack output) {
        if (!output.hasTagCompound()) return Blocks.AIR.getDefaultState();

        return NBTUtil.readBlockState(output.getTagCompound()
            .getCompoundTag("BlockEntityTag").getCompoundTag("state"));
    }

    private static IBlockState getPreview(IBlockState state, IBlockState material) {
        if (material.getBlock() == Blocks.AIR) return state;

        IUnlistedProperty<IBlockState> property = ((IEditableBlock) state.getBlock()).getStateProperty();
        return ((IExtendedBlockState) state).withProperty(property, material);
    }

    // MysticalLib builds the editable geometry without a quad orientation
    // (resulting in NPE on getFace()), so we render our own quads
    private static void renderModel(IBakedModel model, IBlockState preview, float brightness) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        for (EnumFacing facing : EnumFacing.values()) {
            renderQuads(buffer, model.getQuads(preview, facing, 0L), facing, brightness);
        }

        renderQuads(buffer, model.getQuads(preview, null, 0L), null, brightness);
    }

    private static void renderQuads(BufferBuilder buffer, List<BakedQuad> quads, EnumFacing fallback, float brightness) {
        Tessellator tessellator = Tessellator.getInstance();
        for (BakedQuad quad : quads) {
            EnumFacing face = quad.getFace() != null ? quad.getFace() : fallback;
            if (face == null) continue;

            buffer.begin(7, DefaultVertexFormats.ITEM);
            buffer.addVertexData(quad.getVertexData());
            buffer.putColorRGB_F4(brightness, brightness, brightness);
            Vec3i normal = face.getDirectionVec();
            buffer.putNormal(normal.getX(), normal.getY(), normal.getZ());
            tessellator.draw();
        }
    }
}
