package com.buildershandbag.client.render;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import epicsquid.blockcraftery.block.IEditableBlock;


/**
 * Creates a material-aware Blockcraftery item-preview model.
 * <p>
 * Blockcraftery stores the material state on its placed tile entity, while
 * its ordinary item model always draws an undecorated frame. This factory
 * supplies the same extended state that the placed block renderer consumes.
 * The returned preview must be rendered with {@link Preview#getRenderStack()}
 * so Minecraft applies the material item's normal tint colors.
 */
@SideOnly(Side.CLIENT)
public final class BlockcrafteryPreviewModel {

    private BlockcrafteryPreviewModel() {
    }

    /**
     * Creates a preview without mutating the configuration's frame or material
     * stacks. Returning {@code null} tells callers to render the normal frame.
     */
    @SuppressWarnings("deprecation")
    @Nullable
    public static Preview create(RenderItem itemRender, ItemStack frame, ItemStack material) {
        if (frame.isEmpty() || material.isEmpty()) return null;

        Block frameBlock = Block.getBlockFromItem(frame.getItem());
        Block materialBlock = Block.getBlockFromItem(material.getItem());
        if (!(frameBlock instanceof IEditableBlock) || materialBlock == Blocks.AIR) return null;

        IBlockState frameState = frameBlock.getStateFromMeta(frame.getMetadata());
        if (!(frameState instanceof IExtendedBlockState)) return null;

        IBlockState materialState = materialBlock.getStateFromMeta(material.getMetadata());
        IExtendedBlockState previewState = ((IExtendedBlockState) frameState).withProperty(
            ((IEditableBlock) frameBlock).getStateProperty(),
            materialState);

        ItemStack renderStack = material.copy();
        renderStack.setCount(1);
        IBakedModel frameModel = itemRender.getItemModelWithOverrides(frame, null, null);
        return new Preview(
            renderStack,
            new MaterialPreviewModel(frameModel, previewState, materialBlock.getRenderLayer()));
    }

    /**
     * Model and stack pair used by any client renderer, including GUI and held
     * item rendering. The render stack deliberately remains the material.
     */
    public static final class Preview {

        private final ItemStack renderStack;
        private final IBakedModel model;

        private Preview(ItemStack renderStack, IBakedModel model) {
            this.renderStack = renderStack;
            this.model = model;
        }

        public ItemStack getRenderStack() {
            return renderStack.copy();
        }

        public IBakedModel getModel() {
            return model;
        }
    }

    /**
     * Reuses Blockcraftery's baked geometry, substituting the configured
     * material state where its placed-tile renderer would normally supply it.
     */
    private static final class MaterialPreviewModel extends BakedModelWrapper<IBakedModel> {

        private final IExtendedBlockState previewState;
        private final BlockRenderLayer materialLayer;

        private MaterialPreviewModel(IBakedModel originalModel, IExtendedBlockState previewState,
                BlockRenderLayer materialLayer) {
            super(originalModel);
            this.previewState = previewState;
            this.materialLayer = materialLayer;
        }

        @Override
        @Nonnull
        public List<BakedQuad> getQuads(@Nullable IBlockState ignoredState, @Nullable EnumFacing side, long rand) {
            BlockRenderLayer previousLayer = MinecraftForgeClient.getRenderLayer();
            ForgeHooksClient.setRenderLayer(materialLayer);
            try {
                return originalModel.getQuads(previewState, side, rand);
            } finally {
                ForgeHooksClient.setRenderLayer(previousLayer);
            }
        }

        @Override
        @Nonnull
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(
                @Nonnull ItemCameraTransforms.TransformType cameraTransformType) {
            Pair<? extends IBakedModel, Matrix4f> perspective = originalModel.handlePerspective(cameraTransformType);
            return Pair.of(this, perspective.getRight());
        }
    }
}
