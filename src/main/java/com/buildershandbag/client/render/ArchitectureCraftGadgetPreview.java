package com.buildershandbag.client.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.elytradev.architecture.client.proxy.ClientProxy;
import com.elytradev.architecture.client.render.target.RenderTargetBaked;
import com.elytradev.architecture.common.helpers.Trans3;
import com.elytradev.architecture.common.helpers.Vector3;
import com.elytradev.architecture.common.tile.TileShape;


@SideOnly(Side.CLIENT)
public final class ArchitectureCraftGadgetPreview {

    private ArchitectureCraftGadgetPreview() {
    }

    public static boolean render(BlockRendererDispatcher dispatcher, IBlockState state, float brightness,
            World world, BlockPos position, ItemStack output) {
        IBakedModel model = createModel(state, world, position, output);
        if (model == null) return false;

        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        dispatcher.getBlockModelRenderer().renderModelBrightnessColor(state,
            model, brightness, 1.0F, 1.0F, 1.0F);
        return true;
    }

    public static IBakedModel createModel(IBlockState state, World world, BlockPos position, ItemStack output) {
        if (!output.hasTagCompound() || !output.getTagCompound().hasKey("BlockEntityTag")) return null;

        TileShape shape = new PreviewShape(state);
        shape.readFromNBT(output.getTagCompound().getCompoundTag("BlockEntityTag"));
        shape.setWorld(world);
        shape.setPos(position);
        if (!shape.hasShape() || !shape.hasBaseBlockState()) return null;

        RenderTargetBaked target = new RenderTargetBaked();
        Trans3 transform = Trans3.blockCenter.t(shape.localToGlobalTransformation(Vector3.zero, state));
        ClientProxy.SHAPE_RENDER_DISPATCHER.renderShapeTE(shape, target, transform,
            true, shape.hasSecondaryBlockState(), -1, -1);
        return target.getBakedModel();
    }

    private static final class PreviewShape extends TileShape {

        private final IBlockState state;

        private PreviewShape(IBlockState state) {
            this.state = state;
        }

        @Override
        public Trans3 localToGlobalTransformation(Vector3 origin) {
            return super.localToGlobalTransformation(origin, state);
        }
    }
}
