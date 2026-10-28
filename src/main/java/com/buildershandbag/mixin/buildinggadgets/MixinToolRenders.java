package com.buildershandbag.mixin.buildinggadgets;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.direwolf20.buildinggadgets.common.tools.ToolRenders;

import com.buildershandbag.client.render.BuildingGadgetPreview;
import com.buildershandbag.integration.BuildingGadgetsTarget;


@Mixin(value = ToolRenders.class, remap = false)
public abstract class MixinToolRenders {

    @Unique
    private static final ThreadLocal<BuildingGadgetPreview> buildershandbag$preview = new ThreadLocal<>();
    @Unique
    private static final ThreadLocal<BlockPos> buildershandbag$exchangerPosition = new ThreadLocal<>();

    @Redirect(method = "renderBuilderOverlay", at = @At(value = "INVOKE",
        target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"))
    private static void buildershandbag$renderPreviews(List<BlockPos> positions, Consumer<BlockPos> render,
            RenderWorldLastEvent event, EntityPlayer player, ItemStack gadget) {
        BuildingGadgetPreview previous = buildershandbag$preview.get();
        BuildingGadgetPreview preview = new BuildingGadgetPreview(player, gadget);
        buildershandbag$preview.set(preview);
        try {
            positions.forEach(pos -> {
                preview.setPosition(pos);
                render.accept(pos);
            });
        } finally {
            if (previous == null) buildershandbag$preview.remove();
            else buildershandbag$preview.set(previous);
        }
    }

    @Redirect(method = "lambda$renderBuilderOverlay$1", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/BlockRendererDispatcher;renderBlockBrightness("
            + "Lnet/minecraft/block/state/IBlockState;F)V", remap = true))
    private static void buildershandbag$renderConfiguredBlock(BlockRendererDispatcher dispatcher,
            IBlockState state, float brightness) {
        BuildingGadgetPreview preview = buildershandbag$preview.get();
        if (preview == null || !preview.render(dispatcher, state, brightness)) {
            dispatcher.renderBlockBrightness(state, brightness);
        }
    }

    @Redirect(method = "renderExchangerOverlay", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/BlockRendererDispatcher;renderBlockBrightness("
            + "Lnet/minecraft/block/state/IBlockState;F)V", ordinal = 0, remap = true))
    private static void buildershandbag$renderExchangerBlock(BlockRendererDispatcher dispatcher,
            IBlockState state, float brightness, RenderWorldLastEvent event, EntityPlayer player, ItemStack gadget) {
        if (BuildingGadgetsTarget.get(gadget).isEmpty()) {
            dispatcher.renderBlockBrightness(state, brightness);
            return;
        }

        BuildingGadgetPreview preview = new BuildingGadgetPreview(player, gadget);
        BlockPos pos = buildershandbag$exchangerPosition.get();
        buildershandbag$exchangerPosition.remove();
        preview.setPosition(pos == null ? player.getPosition() : pos);
        if (!preview.render(dispatcher, state, brightness)) {
            dispatcher.renderBlockBrightness(state, brightness);
        }
    }

    @Redirect(method = "renderExchangerOverlay", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/block/state/IBlockState;getActualState(Lnet/minecraft/world/IBlockAccess;"
            + "Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;", remap = true))
    private static IBlockState buildershandbag$retainExchangerState(IBlockState state, IBlockAccess world,
            BlockPos pos, RenderWorldLastEvent event, EntityPlayer player, ItemStack gadget) {
        if (BuildingGadgetsTarget.get(gadget).isEmpty()) return state.getActualState(world, pos);

        buildershandbag$exchangerPosition.set(pos);
        return state;
    }
}
