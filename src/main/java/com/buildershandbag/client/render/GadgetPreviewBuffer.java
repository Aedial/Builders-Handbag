package com.buildershandbag.client.render;

import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.buildershandbag.integration.HandbagIntegration;


@SideOnly(Side.CLIENT)
public final class GadgetPreviewBuffer {

    private GadgetPreviewBuffer() {
    }

    public static boolean append(BlockRendererDispatcher dispatcher, IBlockState state, ItemStack output,
            World world, BlockPos source, BlockPos relative, BufferBuilder buffer) {
        ResourceLocation name = state.getBlock().getRegistryName();
        if (name == null || output.isEmpty()) return false;

        if (name.getNamespace().equals(HandbagIntegration.BLOCKCRAFTERY_MODID)
                && HandbagIntegration.BLOCKCRAFTERY.isModLoaded()) {
            return BlockcrafteryGadgetPreview.append(dispatcher, state, output, world, source, relative, buffer);
        }

        if (name.getNamespace().equals(HandbagIntegration.ARCHITECTURECRAFT_MODID)
                && HandbagIntegration.ARCHITECTURECRAFT.isModLoaded()) {
            IBakedModel model = ArchitectureCraftGadgetPreview.createModel(state, world, source, output);
            if (model == null) return false;

            appendModel(model, state, null, world, source, relative, buffer);
            return true;
        }

        return false;
    }

    public static void appendModel(IBakedModel model, IBlockState state, IBlockState material,
            World world, BlockPos source, BlockPos relative, BufferBuilder buffer) {
        for (EnumFacing face : EnumFacing.values()) {
            appendQuads(model.getQuads(state, face, 0L), face, material, world, source, relative, buffer);
        }

        appendQuads(model.getQuads(state, null, 0L), null, material, world, source, relative, buffer);
    }

    private static void appendQuads(List<BakedQuad> quads, EnumFacing fallback, IBlockState material,
            World world, BlockPos source, BlockPos relative, BufferBuilder buffer) {
        int light = world.getCombinedLight(source, 0);
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertexData();
            int stride = vertices.length / 4;
            int[] blockVertices = new int[28];
            for (int vertex = 0; vertex < 4; vertex++) {
                System.arraycopy(vertices, vertex * stride, blockVertices, vertex * 7, 6);
            }

            buffer.addVertexData(blockVertices);
            buffer.putBrightness4(light, light, light, light);

            EnumFacing face = quad.getFace() == null ? fallback : quad.getFace();
            float shade = quad.shouldApplyDiffuseLighting() && face != null ? LightUtil.diffuseLight(face) : 1.0F;
            int color = material != null && quad.hasTintIndex()
                ? Minecraft.getMinecraft().getBlockColors().colorMultiplier(material, world, source, quad.getTintIndex())
                : 0xFFFFFF;
            float red = shade * ((color >> 16) & 255) / 255.0F;
            float green = shade * ((color >> 8) & 255) / 255.0F;
            float blue = shade * (color & 255) / 255.0F;
            for (int vertex = 1; vertex <= 4; vertex++) {
                buffer.putColorMultiplier(red, green, blue, vertex);
            }

            buffer.putPosition(relative.getX(), relative.getY(), relative.getZ());
        }
    }
}
