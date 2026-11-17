package com.buildershandbag.client.render;

import java.nio.ByteBuffer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import org.lwjgl.opengl.GL11;

import com.buildershandbag.Tags;
import com.buildershandbag.config.HandbagClientConfig;
import com.buildershandbag.integration.HandbagIntegration;
import com.buildershandbag.item.HandbagMode;
import com.buildershandbag.item.ItemHandbag;
import com.buildershandbag.item.ItemRegistry;
import com.buildershandbag.storage.HandbagConfiguration;
import com.buildershandbag.storage.HandbagOutputs;
import com.buildershandbag.storage.HandbagStorage;


/**
 * Held Handbag placement preview using the selected block's placement and connection rules.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class HandbagPlacementPreview {

    private static final float PREVIEW_ALPHA = 0.4F;
    private static final float POLYGON_OFFSET = -1.0F;

    private HandbagPlacementPreview() {
    }

    @SubscribeEvent
    public static void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = event.getPlayer();
        RayTraceResult hit = event.getTarget();
        if (minecraft.currentScreen != null || player.isSpectator() || hit == null
            || hit.typeOfHit != RayTraceResult.Type.BLOCK) return;

        EnumHand hand = player.getHeldItemMainhand().getItem() == ItemRegistry.HANDBAG
            ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
        ItemStack handbag = player.getHeldItem(hand);
        if (handbag.getItem() != ItemRegistry.HANDBAG || HandbagStorage.getMode(handbag) == HandbagMode.RANDOM) return;

        HandbagConfiguration configuration = HandbagStorage.getSelectedConfiguration(handbag);
        if (configuration == null || !configuration.isAvailable()) return;

        ItemStack output = HandbagOutputs.getResult(configuration);
        if (!(output.getItem() instanceof ItemBlock)) return;

        World world = player.world;
        ItemBlock item = (ItemBlock) output.getItem();
        BlockPos position = getPlacementPosition(world, item, output, hit);
        IBlockState previous = world.getBlockState(position);
        boolean mergeSlab = item instanceof ItemSlab && item.getBlock() instanceof BlockSlab
            && canMergeSlab((BlockSlab) item.getBlock(), previous, output);
        if (position.getY() < 0 || position.getY() >= world.getHeight()
                || !world.getWorldBorder().contains(position) || !world.isBlockModifiable(player, position)
                || !player.canPlayerEdit(position, hit.sideHit, output)) return;

        if (item instanceof ItemSlab
                && !player.canPlayerEdit(hit.getBlockPos().offset(hit.sideHit), hit.sideHit, output)) return;

        if (mergeSlab ? !world.checkNoEntityCollision(Block.FULL_BLOCK_AABB.offset(position))
                : !world.mayPlace(item.getBlock(), position, false, hit.sideHit, player)) return;

        float hitX = (float) (hit.hitVec.x - hit.getBlockPos().getX());
        float hitY = (float) (hit.hitVec.y - hit.getBlockPos().getY());
        float hitZ = (float) (hit.hitVec.z - hit.getBlockPos().getZ());
        IBlockState state;
        player.setHeldItem(hand, output);
        try {
            if (mergeSlab) {
                // Render the half added to the existing slab at the merge position
                BlockSlab.EnumBlockHalf half = previous.getValue(BlockSlab.HALF);
                state = previous.withProperty(BlockSlab.HALF, half == BlockSlab.EnumBlockHalf.BOTTOM
                    ? BlockSlab.EnumBlockHalf.TOP : BlockSlab.EnumBlockHalf.BOTTOM);
            } else {
                state = item.getBlock().getStateForPlacement(world, position, hit.sideHit,
                    hitX, hitY, hitZ, item.getMetadata(output.getMetadata()), player, hand);
            }
        } finally {
            player.setHeldItem(hand, handbag);
        }

        render(minecraft, player, configuration, output, state, position, hit, event.getPartialTicks());
    }

    private static BlockPos getPlacementPosition(World world, ItemBlock item, ItemStack output, RayTraceResult hit) {
        if (item instanceof ItemSlab && item.getBlock() instanceof BlockSlab) {
            BlockSlab slab = (BlockSlab) item.getBlock();
            IBlockState clicked = world.getBlockState(hit.getBlockPos());
            if (canMergeSlab(slab, clicked, output)) {
                BlockSlab.EnumBlockHalf half = clicked.getValue(BlockSlab.HALF);
                if (hit.sideHit == EnumFacing.UP && half == BlockSlab.EnumBlockHalf.BOTTOM
                        || hit.sideHit == EnumFacing.DOWN && half == BlockSlab.EnumBlockHalf.TOP) {
                    return hit.getBlockPos();
                }
            }

            BlockPos adjacent = hit.getBlockPos().offset(hit.sideHit);
            if (canMergeSlab(slab, world.getBlockState(adjacent), output)) return adjacent;
        }

        return ItemHandbag.getPlacementPosition(world, hit.getBlockPos(), hit.sideHit);
    }

    private static boolean canMergeSlab(BlockSlab slab, IBlockState state, ItemStack output) {
        return !slab.isDouble() && state.getBlock() == slab
            && state.getValue(slab.getVariantProperty()) == slab.getTypeForItem(output);
    }

    private static void render(Minecraft minecraft, EntityPlayer player, HandbagConfiguration configuration,
            ItemStack output, IBlockState state, BlockPos position, RayTraceResult hit, float partialTicks) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        PlacementPreviewAccess access;
        if (configuration.getIntegration() == HandbagIntegration.BLOCKCRAFTERY
                && HandbagIntegration.BLOCKCRAFTERY.isModLoaded()) {
            access = createBlockcrafteryAccess(player, position, state, output, hit);
            state = access.getBlockState(position);
        } else {
            access = new PlacementPreviewAccess(player.world, position, state, output);
        }

        BlockRenderLayer previousLayer = MinecraftForgeClient.getRenderLayer();
        double cameraX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double cameraY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double cameraZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        minecraft.entityRenderer.enableLightmap();
        GlStateManager.pushMatrix();
        GlStateManager.translate(-cameraX, -cameraY, -cameraZ);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.enableAlpha();
        GlStateManager.depthMask(false);
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(POLYGON_OFFSET, POLYGON_OFFSET);
        PlacementPreviewOutline outline = null;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        try {
            BlockRendererDispatcher dispatcher = minecraft.getBlockRendererDispatcher();
            if (configuration.getIntegration() == HandbagIntegration.ARCHITECTURECRAFT
                    && HandbagIntegration.ARCHITECTURECRAFT.isModLoaded()) {
                appendArchitectureCraft(state, player, position, output, hit, buffer);
            } else if (configuration.getIntegration() == HandbagIntegration.BLOCKCRAFTERY
                    && HandbagIntegration.BLOCKCRAFTERY.isModLoaded()) {
                appendBlockcraftery(dispatcher, state, output, access, position, buffer);
            } else {
                for (BlockRenderLayer layer : BlockRenderLayer.values()) {
                    if (!state.getBlock().canRenderInLayer(state, layer)) continue;

                    ForgeHooksClient.setRenderLayer(layer);
                    dispatcher.renderBlock(state, position, access, buffer);
                }
            }

            applyTransparency(buffer);
            if (HandbagClientConfig.rendering.renderPlacementPreviewOutline) {
                outline = PlacementPreviewOutline.capture(buffer);
            }
        } finally {
            try {
                tessellator.draw();
                if (outline != null) outline.render(tessellator);
            } finally {
                ForgeHooksClient.setRenderLayer(previousLayer);
                GlStateManager.doPolygonOffset(0.0F, 0.0F);
                GlStateManager.disablePolygonOffset();
                GlStateManager.depthMask(true);
                GlStateManager.disableAlpha();
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
                minecraft.entityRenderer.disableLightmap();
            }
        }
    }

    @Optional.Method(modid = HandbagIntegration.ARCHITECTURECRAFT_MODID)
    private static void appendArchitectureCraft(IBlockState state, EntityPlayer player, BlockPos position,
            ItemStack output, RayTraceResult hit, BufferBuilder buffer) {
        IBakedModel model = ArchitectureCraftGadgetPreview.createPlacementModel(state, player.world, position,
            output, player, hit.sideHit, (float) (hit.hitVec.x - hit.getBlockPos().getX()),
            (float) (hit.hitVec.y - hit.getBlockPos().getY()), (float) (hit.hitVec.z - hit.getBlockPos().getZ()));
        if (model != null) {
            GadgetPreviewBuffer.appendModel(model, state, null, player.world, position, position, buffer);
        }
    }

    @Optional.Method(modid = HandbagIntegration.BLOCKCRAFTERY_MODID)
    private static PlacementPreviewAccess createBlockcrafteryAccess(EntityPlayer player, BlockPos position,
            IBlockState state, ItemStack output, RayTraceResult hit) {
        return BlockcrafteryGadgetPreview.createPlacementAccess(player.world, position, state, output,
            player, hit.sideHit, (float) (hit.hitVec.x - hit.getBlockPos().getX()),
            (float) (hit.hitVec.y - hit.getBlockPos().getY()), (float) (hit.hitVec.z - hit.getBlockPos().getZ()));
    }

    @Optional.Method(modid = HandbagIntegration.BLOCKCRAFTERY_MODID)
    private static void appendBlockcraftery(BlockRendererDispatcher dispatcher, IBlockState state, ItemStack output,
            PlacementPreviewAccess access, BlockPos position, BufferBuilder buffer) {
        BlockcrafteryGadgetPreview.appendPlacement(dispatcher, state, output, access, position, buffer);
    }

    private static void applyTransparency(BufferBuilder buffer) {
        ByteBuffer vertices = buffer.getByteBuffer();
        int stride = buffer.getVertexFormat().getSize();
        int alphaOffset = buffer.getVertexFormat().getColorOffset() + 3;
        for (int vertex = 0; vertex < buffer.getVertexCount(); vertex++) {
            int offset = vertex * stride + alphaOffset;
            vertices.put(offset, (byte) ((vertices.get(offset) & 255) * PREVIEW_ALPHA));
        }
    }
}
