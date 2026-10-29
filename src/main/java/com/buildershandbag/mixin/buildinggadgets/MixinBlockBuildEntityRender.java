package com.buildershandbag.mixin.buildinggadgets;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.direwolf20.buildinggadgets.common.entities.BlockBuildEntity;
import com.direwolf20.buildinggadgets.common.entities.BlockBuildEntityRender;

import com.buildershandbag.client.render.BuildingGadgetPreview;
import com.buildershandbag.integration.BuildingGadgetsEntity;


@Mixin(value = BlockBuildEntityRender.class, remap = false)
public abstract class MixinBlockBuildEntityRender {

    @Redirect(method = "doRender(Lcom/direwolf20/buildinggadgets/common/entities/BlockBuildEntity;DDDFF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BlockRendererDispatcher;"
            + "renderBlockBrightness(Lnet/minecraft/block/state/IBlockState;F)V", remap = true))
    private void buildershandbag$renderConfiguredBlock(BlockRendererDispatcher dispatcher, IBlockState state,
            float brightness, BlockBuildEntity entity, double x, double y, double z, float yaw, float partialTicks) {
        ItemStack output = ((BuildingGadgetsEntity) entity).buildershandbag$getOutput();
        if (entity.getToolMode() == 1 && !entity.getUsingConstructionPaste() && !output.isEmpty()) {
            BuildingGadgetPreview preview = new BuildingGadgetPreview(entity.getEntityWorld(), output);
            preview.setPosition(entity.getPosition());
            if (preview.render(dispatcher, state, brightness)) return;
        }

        dispatcher.renderBlockBrightness(state, brightness);
    }
}
