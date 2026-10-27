package com.buildershandbag.client.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.elytradev.architecture.client.proxy.ClientProxy;
import com.elytradev.architecture.client.render.target.RenderTargetBaked;
import com.elytradev.architecture.common.helpers.Trans3;
import com.elytradev.architecture.common.helpers.Vector3;
import com.elytradev.architecture.common.tile.TileShape;


@SideOnly(Side.CLIENT)
public final class ArchitectureCraftGadgetPreview {

    // FIXME: The "grow" animation still renders the block in broken texture (although preview works fine)

    private ArchitectureCraftGadgetPreview() {
    }

    public static boolean render(BlockRendererDispatcher dispatcher, IBlockState state, float brightness,
            EntityPlayer player, BlockPos position, ItemStack output) {
        if (!output.hasTagCompound() || !output.getTagCompound().hasKey("BlockEntityTag")) return false;

        TileShape shape = new PreviewShape(state);
        shape.readFromNBT(output.getTagCompound().getCompoundTag("BlockEntityTag"));
        shape.setWorld(player.world);
        shape.setPos(position);
        if (!shape.hasShape() || !shape.hasBaseBlockState()) return false;

        RenderTargetBaked target = new RenderTargetBaked();
        Trans3 transform = Trans3.blockCenter.t(shape.localToGlobalTransformation(Vector3.zero, state));
        ClientProxy.SHAPE_RENDER_DISPATCHER.renderShapeTE(shape, target, transform,
            true, shape.hasSecondaryBlockState(), -1, -1);
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        dispatcher.getBlockModelRenderer().renderModelBrightnessColor(state,
            target.getBakedModel(), brightness, 1.0F, 1.0F, 1.0F);
        return true;
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
