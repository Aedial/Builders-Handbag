package com.buildershandbag.client.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.elytradev.architecture.client.proxy.ClientProxy;
import com.elytradev.architecture.client.render.target.RenderTargetBaked;
import com.elytradev.architecture.common.helpers.Trans3;
import com.elytradev.architecture.common.helpers.Vector3;
import com.elytradev.architecture.common.tile.TileShape;

import com.buildershandbag.integration.ArchitectureCraftIntegration;


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

        return createModel(state, shape);
    }

    public static IBakedModel createPlacementModel(IBlockState state, World world, BlockPos position,
            ItemStack output, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
        TileShape shape = new TileShape();
        shape.setWorld(world);
        shape.setPos(position);
        shape.readFromItemStack(output);
        if (!shape.hasShape() || !shape.hasBaseBlockState()) return null;

        Vec3i direction = side.getDirectionVec();
        Vector3 hit = new Vector3(hitX - direction.getX() - 0.5,
            hitY - direction.getY() - 0.5, hitZ - direction.getZ() - 0.5);
        BlockPos neighbour = position.offset(side.getOpposite());
        shape.getShape().orientOnPlacement(player, shape, neighbour, world.getBlockState(neighbour),
            world.getTileEntity(neighbour), side, hit);
        ArchitectureCraftIntegration.applyTextureOrientation(shape, output);

        ItemStack preview = output.copy();
        preview.setTagInfo("BlockEntityTag", shape.writeToNBT(new NBTTagCompound()));
        return createModel(state, world, position, preview);
    }

    private static IBakedModel createModel(IBlockState state, TileShape shape) {
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
