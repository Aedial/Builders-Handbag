package com.buildershandbag.client.render;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.buildershandbag.Tags;
import com.buildershandbag.config.HandbagClientConfig;
import com.buildershandbag.integration.HandbagIntegration;
import com.buildershandbag.storage.HandbagConfiguration;
import com.buildershandbag.storage.HandbagStorage;
import com.buildershandbag.tile.TileHandbag;


/**
 * Renders the placed handbag as a one-pixel wire-frame cube around a rotating
 * configuration. State touched by the renderer is captured and restored so a
 * translucent default core cannot affect later tile/entity renders.
 */
@SideOnly(Side.CLIENT)
public class RenderHandbag extends TileEntitySpecialRenderer<TileHandbag> {

    private static final ResourceLocation FRAME_TEXTURE = new ResourceLocation(Tags.MODID,
        "textures/blocks/frame.png");

    /** Size of one pixel in block space */
    private static final double UNIT = 1.0D / 16.0D;
    /** Size of the default core (corner-to-corner), in block space */
    private static final double DEFAULT_CORE_SCALE = 14 * UNIT;
    /** Minimum UV coordinates for the frame texture's outer ring */
    private static final double FRAME_EDGE_UV_MIN = 0.5D * UNIT;
    /** Maximum UV coordinates for the frame texture's outer ring */
    private static final double FRAME_EDGE_UV_MAX = 15.5D * UNIT;
    /** Cached buffer for cross products */
    private static final double[][] FRAME_STRUTS = createFrameStruts();
    /** Cached buffer for the default icosidodecahedron core's faces */
    private static final List<double[][]> ICOSIDODECAHEDRON_FACES = createIcosidodecahedronFaces();
    /** Cached buffer for the default icosidodecahedron core's edges */
    private static final List<double[][]> ICOSIDODECAHEDRON_EDGES = createIcosidodecahedronEdges(
        ICOSIDODECAHEDRON_FACES);
    /** Scale factor between a block's side and its diagonal */
    private static final float BLOCK_CORE_DIAGONAL_SCALE = (float) (1.0D / Math.sqrt(2.0D));

    @Override
    public void render(@Nonnull TileHandbag tile, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        RenderState state = RenderState.capture();
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        try {
            GlStateManager.translate(x, y, z);
            renderFrame();
            renderCore(tile, partialTicks);
        } finally {
            GlStateManager.popAttrib();
            GlStateManager.popMatrix();
            state.restore();
        }
    }

    private void renderFrame() {
        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        textureManager.bindTexture(FRAME_TEXTURE);

        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        for (double[] strut : FRAME_STRUTS) {
            addFrameStrut(buffer, strut[0], strut[1], strut[2], strut[3], strut[4], strut[5]);
        }
        tessellator.draw();
    }

    private void renderCore(TileHandbag tile, float partialTicks) {
        HandbagConfiguration configuration = getSelectedConfiguration(tile);

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(0.5D, 0.5D, 0.5D);

            // Wrap each axis independently so the slower Z spin stays continuous.
            float speed = (float) HandbagClientConfig.rendering.coreRotationSpeed;
            if (speed > 0.0F) {
                float ticks = getAnimationTicks(tile, partialTicks);
                GlStateManager.rotate(wrapDegrees( ticks * 2.0F * speed), 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(wrapDegrees(ticks * 0.74F * speed), 0.0F, 0.0F, 1.0F);
            }

            float scale = (float) (HandbagClientConfig.rendering.coreScale * DEFAULT_CORE_SCALE);
            GlStateManager.scale(scale, scale, scale);

            if (configuration == null) {
                renderDefaultCore();
            } else {
                renderConfiguredCore(configuration);
            }
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private HandbagConfiguration getSelectedConfiguration(TileHandbag tile) {
        ItemStack handbag = tile.getHandbagStack();
        return HandbagStorage.getConfiguration(handbag, HandbagStorage.getSelected(handbag));
    }

    private float getAnimationTicks(TileHandbag tile, float partialTicks) {
        return tile.hasWorld()
            ? tile.getWorld().getTotalWorldTime() + partialTicks
            : Minecraft.getSystemTime() / 50.0F;
    }

    private static float wrapDegrees(float angle) {
        return angle % 360.0F;
    }

    private void renderDefaultCore() {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int faceColor = HandbagClientConfig.getCoreFaceColor();
        int edgeColor = HandbagClientConfig.getCoreEdgeColor();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (double[][] face : ICOSIDODECAHEDRON_FACES) {
            for (int index = 1; index < face.length - 1; index++) {
                addColoredVertex(buffer, face[0], faceColor);
                addColoredVertex(buffer, face[index], faceColor);
                addColoredVertex(buffer, face[index + 1], faceColor);
            }
        }
        tessellator.draw();

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (double[][] edge : ICOSIDODECAHEDRON_EDGES) {
            addColoredVertex(buffer, edge[0], edgeColor);
            addColoredVertex(buffer, edge[1], edgeColor);
        }
        tessellator.draw();
    }

    private void renderConfiguredCore(HandbagConfiguration configuration) {
        ItemStack result = configuration.getResult();
        if (result.isEmpty()) {
            renderDefaultCore();
            return;
        }

        GlStateManager.pushMatrix();
        try {
            // Scale the core down to fit inside the frame's diagonal, so it doesn't clip the frame
            GlStateManager.scale(BLOCK_CORE_DIAGONAL_SCALE, BLOCK_CORE_DIAGONAL_SCALE, BLOCK_CORE_DIAGONAL_SCALE);

            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            RenderHelper.enableStandardItemLighting();
            try {
                if (!renderBlockcrafteryConfiguration(configuration)) {
                    Minecraft.getMinecraft().getRenderItem().renderItem(result, TransformType.NONE);
                }
            } finally {
                RenderHelper.disableStandardItemLighting();
            }
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private boolean renderBlockcrafteryConfiguration(HandbagConfiguration configuration) {
        if (configuration.getIntegration() != HandbagIntegration.BLOCKCRAFTERY
                || !HandbagIntegration.BLOCKCRAFTERY.isModLoaded()) {
            return false;
        }

        return renderBlockcrafteryConfiguration(configuration.getResult(), configuration.getMaterial());
    }

    @Optional.Method(modid = HandbagIntegration.BLOCKCRAFTERY_MODID)
    private boolean renderBlockcrafteryConfiguration(ItemStack frame, ItemStack material) {
        RenderItem itemRenderer = Minecraft.getMinecraft().getRenderItem();
        BlockcrafteryPreviewModel.Preview preview = BlockcrafteryPreviewModel.create(itemRenderer, frame, material);
        if (preview == null) return false;

        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        GlStateManager.pushMatrix();
        try {
            itemRenderer.renderItem(
                preview.getRenderStack(),
                ForgeHooksClient.handleCameraTransforms(preview.getModel(), TransformType.NONE, false));
            GlStateManager.cullFace(GlStateManager.CullFace.BACK);
        } finally {
            GlStateManager.popMatrix();
        }

        return true;
    }

    /**
     * Sample the struts from the outer ring of the frame texture, to make it render
     * as a solid 1px wire-frame cube.
     */
    private static void addFrameStrut(BufferBuilder buffer, double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        double xSize = maxX - minX;
        double ySize = maxY - minY;
        double zSize = maxZ - minZ;

        if (xSize >= ySize && xSize >= zSize) {
            addVerticalStripQuad(
                buffer,
                minX, minY, minZ,
                minX, maxY, minZ,
                maxX, maxY, minZ,
                maxX, minY, minZ,
                FRAME_EDGE_UV_MIN);
            addVerticalStripQuad(
                buffer,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                minX, maxY, maxZ,
                minX, minY, maxZ,
                FRAME_EDGE_UV_MAX);
            addHorizontalStripQuad(
                buffer,
                minX, maxY, minZ,
                minX, maxY, maxZ,
                maxX, maxY, maxZ,
                maxX, maxY, minZ,
                FRAME_EDGE_UV_MIN);
            addHorizontalStripQuad(
                buffer,
                minX, minY, maxZ,
                minX, minY, minZ,
                maxX, minY, minZ,
                maxX, minY, maxZ,
                FRAME_EDGE_UV_MAX);
            return;
        }

        if (ySize >= xSize && ySize >= zSize) {
            addVerticalStripQuad(
                buffer,
                minX, minY, maxZ,
                minX, maxY, maxZ,
                minX, maxY, minZ,
                minX, minY, minZ,
                FRAME_EDGE_UV_MIN);
            addVerticalStripQuad(
                buffer,
                maxX, minY, minZ,
                maxX, maxY, minZ,
                maxX, maxY, maxZ,
                maxX, minY, maxZ,
                FRAME_EDGE_UV_MAX);
            addVerticalStripQuad(
                buffer,
                minX, minY, minZ,
                minX, maxY, minZ,
                maxX, maxY, minZ,
                maxX, minY, minZ,
                FRAME_EDGE_UV_MIN);
            addVerticalStripQuad(
                buffer,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                minX, maxY, maxZ,
                minX, minY, maxZ,
                FRAME_EDGE_UV_MAX);
            return;
        }

        addVerticalStripQuad(
            buffer,
            minX, minY, maxZ,
            minX, maxY, maxZ,
            minX, maxY, minZ,
            minX, minY, minZ,
            FRAME_EDGE_UV_MIN);
        addVerticalStripQuad(
            buffer,
            maxX, minY, minZ,
            maxX, maxY, minZ,
            maxX, maxY, maxZ,
            maxX, minY, maxZ,
            FRAME_EDGE_UV_MAX);
        addHorizontalStripQuad(
            buffer,
            minX, maxY, minZ,
            minX, maxY, maxZ,
            maxX, maxY, maxZ,
            maxX, maxY, minZ,
            FRAME_EDGE_UV_MIN);
        addHorizontalStripQuad(
            buffer,
            minX, minY, maxZ,
            minX, minY, minZ,
            maxX, minY, minZ,
            maxX, minY, maxZ,
            FRAME_EDGE_UV_MAX);
    }

    private static void addVerticalStripQuad(BufferBuilder buffer, double firstX, double firstY, double firstZ,
            double secondX, double secondY, double secondZ, double thirdX, double thirdY, double thirdZ,
            double fourthX, double fourthY, double fourthZ, double u) {
        buffer.pos(firstX, firstY, firstZ).tex(u, FRAME_EDGE_UV_MAX).endVertex();
        buffer.pos(secondX, secondY, secondZ).tex(u, FRAME_EDGE_UV_MIN).endVertex();
        buffer.pos(thirdX, thirdY, thirdZ).tex(u, FRAME_EDGE_UV_MIN).endVertex();
        buffer.pos(fourthX, fourthY, fourthZ).tex(u, FRAME_EDGE_UV_MAX).endVertex();
    }

    private static void addHorizontalStripQuad(BufferBuilder buffer, double firstX, double firstY, double firstZ,
            double secondX, double secondY, double secondZ, double thirdX, double thirdY, double thirdZ,
            double fourthX, double fourthY, double fourthZ, double v) {
        buffer.pos(firstX, firstY, firstZ).tex(FRAME_EDGE_UV_MIN, v).endVertex();
        buffer.pos(secondX, secondY, secondZ).tex(FRAME_EDGE_UV_MIN, v).endVertex();
        buffer.pos(thirdX, thirdY, thirdZ).tex(FRAME_EDGE_UV_MAX, v).endVertex();
        buffer.pos(fourthX, fourthY, fourthZ).tex(FRAME_EDGE_UV_MAX, v).endVertex();
    }

    private static void addColoredVertex(BufferBuilder buffer, double[] vertex, int color) {
        buffer.pos(vertex[0], vertex[1], vertex[2])
            .color(
                (color >>> 16 & 0xFF) / 255.0F,
                (color >>> 8 & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                (color >>> 24 & 0xFF) / 255.0F)
            .endVertex();
    }

    private static double[][] createFrameStruts() {
        return new double[][] {
            box(0, 0, 0, 1, 16, 1), box(15, 0, 0, 16, 16, 1),
            box(0, 0, 15, 1, 16, 16), box(15, 0, 15, 16, 16, 16),
            box(1, 0, 0, 15, 1, 1), box(1, 0, 15, 15, 1, 16),
            box(0, 0, 1, 1, 1, 15), box(15, 0, 1, 16, 1, 15),
            box(1, 15, 0, 15, 16, 1), box(1, 15, 15, 15, 16, 16),
            box(0, 15, 1, 1, 16, 15), box(15, 15, 1, 16, 16, 15)
        };
    }

    private static double[] box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return new double[] {
            minX * UNIT, minY * UNIT, minZ * UNIT,
            maxX * UNIT, maxY * UNIT, maxZ * UNIT
        };
    }

    /**
     * Builds the rectified icosahedron at class load: its twenty original
     * triangular faces become triangles and its twelve original vertices
     * become pentagons, yielding an icosidodecahedron.
     */
    private static List<double[][]> createIcosidodecahedronFaces() {
        double phi = (1.0D + Math.sqrt(5.0D)) / 2.0D;
        double[][] icosahedron = {
            { 0,  1,  phi }, { 0, -1,  phi }, { 0,  1, -phi }, { 0, -1, -phi },
            { 1,  phi, 0 }, {-1,  phi, 0 }, { 1, -phi, 0 }, {-1, -phi, 0 },
            { phi, 0,  1 }, { phi, 0, -1 }, {-phi, 0,  1 }, {-phi, 0, -1 }
        };

        double edgeLengthSquared = Double.MAX_VALUE;
        for (int first = 0; first < icosahedron.length; first++) {
            for (int second = first + 1; second < icosahedron.length; second++) {
                edgeLengthSquared = Math.min(
                    edgeLengthSquared, distanceSquared(icosahedron[first], icosahedron[second]));
            }
        }

        List<Edge> edges = new ArrayList<>();
        for (int first = 0; first < icosahedron.length; first++) {
            for (int second = first + 1; second < icosahedron.length; second++) {
                if (Math.abs(distanceSquared(icosahedron[first], icosahedron[second]) - edgeLengthSquared) < 1.0E-6D) {
                    edges.add(new Edge(first, second, midpoint(icosahedron[first], icosahedron[second])));
                }
            }
        }

        normalizeToRadius(edges);
        List<double[][]> faces = new ArrayList<>();
        for (int first = 0; first < icosahedron.length; first++) {
            for (int second = first + 1; second < icosahedron.length; second++) {
                for (int third = second + 1; third < icosahedron.length; third++) {
                    Edge firstSecond = findEdge(edges, first, second);
                    Edge secondThird = findEdge(edges, second, third);
                    Edge thirdFirst = findEdge(edges, third, first);
                    if (firstSecond != null && secondThird != null && thirdFirst != null) {
                        faces.add(new double[][] { firstSecond.midpoint, secondThird.midpoint, thirdFirst.midpoint });
                    }
                }
            }
        }

        for (int vertex = 0; vertex < icosahedron.length; vertex++) {
            List<Edge> adjacent = new ArrayList<>();
            for (Edge edge : edges) {
                if (edge.first == vertex || edge.second == vertex) adjacent.add(edge);
            }
            sortAroundVertex(adjacent, icosahedron[vertex]);

            double[][] pentagon = new double[adjacent.size()][];
            for (int index = 0; index < adjacent.size(); index++) {
                pentagon[index] = adjacent.get(index).midpoint;
            }

            faces.add(pentagon);
        }

        return Collections.unmodifiableList(faces);
    }

    /**
     * Build each geometric edge once. Faces share their vertex arrays, so
     * identity is sufficient and avoids drawing transparent edges twice.
     */
    private static List<double[][]> createIcosidodecahedronEdges(List<double[][]> faces) {
        IdentityHashMap<double[], Integer> vertexIndices = new IdentityHashMap<>();
        Set<Long> edgeIndices = new HashSet<>();
        List<double[][]> edges = new ArrayList<>();

        for (double[][] face : faces) {
            for (int index = 0; index < face.length; index++) {
                double[] first = face[index];
                double[] second = face[(index + 1) % face.length];
                int firstIndex = getVertexIndex(vertexIndices, first);
                int secondIndex = getVertexIndex(vertexIndices, second);
                int minimum = Math.min(firstIndex, secondIndex);
                int maximum = Math.max(firstIndex, secondIndex);
                long edgeIndex = (long) minimum << 32 | maximum & 0xFFFFFFFFL;

                if (edgeIndices.add(edgeIndex)) edges.add(new double[][] { first, second });
            }
        }

        return Collections.unmodifiableList(edges);
    }

    private static int getVertexIndex(IdentityHashMap<double[], Integer> vertexIndices, double[] vertex) {
        Integer index = vertexIndices.get(vertex);
        if (index != null) return index;

        int nextIndex = vertexIndices.size();
        vertexIndices.put(vertex, nextIndex);
        return nextIndex;
    }

    private static void normalizeToRadius(List<Edge> edges) {
        double radius = 0.0D;
        for (Edge edge : edges) {
            radius = Math.max(radius, Math.sqrt(dot(edge.midpoint, edge.midpoint)));
        }

        double scale = 0.5D / radius;
        for (Edge edge : edges) {
            edge.midpoint[0] *= scale;
            edge.midpoint[1] *= scale;
            edge.midpoint[2] *= scale;
        }
    }

    private static void sortAroundVertex(List<Edge> edges, double[] vertex) {
        final double[] normal = normalize(vertex);
        double[] reference = Math.abs(normal[0]) < 0.9D ? new double[] { 1, 0, 0 } : new double[] { 0, 1, 0 };
        final double[] horizontal = normalize(cross(normal, reference));
        final double[] vertical = cross(normal, horizontal);

        edges.sort((first, second) -> Double.compare(
            angleAroundVertex(second.midpoint, normal, horizontal, vertical),
            angleAroundVertex(first.midpoint, normal, horizontal, vertical)));
    }

    private static double angleAroundVertex(double[] point, double[] normal, double[] horizontal, double[] vertical) {
        double projection = dot(point, normal);
        double[] tangent = {
            point[0] - normal[0] * projection,
            point[1] - normal[1] * projection,
            point[2] - normal[2] * projection
        };

        return Math.atan2(dot(tangent, vertical), dot(tangent, horizontal));
    }

    private static Edge findEdge(List<Edge> edges, int first, int second) {
        for (Edge edge : edges) {
            if (edge.connects(first, second)) return edge;
        }

        return null;
    }

    private static double[] midpoint(double[] first, double[] second) {
        return new double[] {
            (first[0] + second[0]) / 2.0D,
            (first[1] + second[1]) / 2.0D,
            (first[2] + second[2]) / 2.0D
        };
    }

    private static double distanceSquared(double[] first, double[] second) {
        double x = first[0] - second[0];
        double y = first[1] - second[1];
        double z = first[2] - second[2];

        return x * x + y * y + z * z;
    }

    private static double[] normalize(double[] vector) {
        double length = Math.sqrt(dot(vector, vector));
        return new double[] { vector[0] / length, vector[1] / length, vector[2] / length };
    }

    private static double[] cross(double[] first, double[] second) {
        return new double[] {
            first[1] * second[2] - first[2] * second[1],
            first[2] * second[0] - first[0] * second[2],
            first[0] * second[1] - first[1] * second[0]
        };
    }

    private static double dot(double[] first, double[] second) {
        return first[0] * second[0] + first[1] * second[1] + first[2] * second[2];
    }

    private static final class Edge {

        private final int first;
        private final int second;
        private final double[] midpoint;

        private Edge(int first, int second, double[] midpoint) {
            this.first = first;
            this.second = second;
            this.midpoint = midpoint;
        }

        private boolean connects(int firstVertex, int secondVertex) {
            return first == firstVertex && second == secondVertex
                || first == secondVertex && second == firstVertex;
        }
    }

    /**
     * GlStateManager keeps its own state cache, so popAttrib alone is not
     * enough after changing a TESR's GL state. Restoring through the manager
     * brings the cache and OpenGL back into agreement.
     */
    private static final class RenderState {

        private final boolean texture2d;
        private final boolean blend;
        private final boolean cull;
        private final boolean depth;
        private final boolean lighting;
        private final boolean alphaTest;
        private final boolean rescaleNormal;
        private final boolean normalize;
        private final boolean depthMask;
        private final int texture;
        private final int blendSourceRgb;
        private final int blendDestinationRgb;
        private final int blendSourceAlpha;
        private final int blendDestinationAlpha;
        private final float red;
        private final float green;
        private final float blue;
        private final float alpha;

        private RenderState() {
            texture2d = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            blend = GL11.glIsEnabled(GL11.GL_BLEND);
            cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
            alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
            rescaleNormal = GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
            normalize = GL11.glIsEnabled(GL11.GL_NORMALIZE);
            depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            blendSourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            blendDestinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            blendSourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            blendDestinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);

            FloatBuffer color = BufferUtils.createFloatBuffer(16);
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
            red = color.get(0);
            green = color.get(1);
            blue = color.get(2);
            alpha = color.get(3);
        }

        private static RenderState capture() {
            return new RenderState();
        }

        private void restore() {
            setEnabled(texture2d, State.TEXTURE_2D);
            setEnabled(blend, State.BLEND);
            setEnabled(cull, State.CULL);
            setEnabled(depth, State.DEPTH);
            setEnabled(lighting, State.LIGHTING);
            setEnabled(alphaTest, State.ALPHA_TEST);
            setEnabled(rescaleNormal, State.RESCALE_NORMAL);
            setEnabled(normalize, State.NORMALIZE);
            GlStateManager.depthMask(depthMask);
            GlStateManager.tryBlendFuncSeparate(
                blendSourceRgb, blendDestinationRgb, blendSourceAlpha, blendDestinationAlpha);
            GlStateManager.bindTexture(texture);
            GlStateManager.color(red, green, blue, alpha);
        }

        private static void setEnabled(boolean enabled, State state) {
            if (enabled) {
                state.enable();
            } else {
                state.disable();
            }
        }
    }

    private enum State {
        TEXTURE_2D {
            @Override
            void enable() {
                GlStateManager.enableTexture2D();
            }

            @Override
            void disable() {
                GlStateManager.disableTexture2D();
            }
        },
        BLEND {
            @Override
            void enable() {
                GlStateManager.enableBlend();
            }

            @Override
            void disable() {
                GlStateManager.disableBlend();
            }
        },
        CULL {
            @Override
            void enable() {
                GlStateManager.enableCull();
            }

            @Override
            void disable() {
                GlStateManager.disableCull();
            }
        },
        DEPTH {
            @Override
            void enable() {
                GlStateManager.enableDepth();
            }

            @Override
            void disable() {
                GlStateManager.disableDepth();
            }
        },
        LIGHTING {
            @Override
            void enable() {
                GlStateManager.enableLighting();
            }

            @Override
            void disable() {
                GlStateManager.disableLighting();
            }
        },
        ALPHA_TEST {
            @Override
            void enable() {
                GlStateManager.enableAlpha();
            }

            @Override
            void disable() {
                GlStateManager.disableAlpha();
            }
        },
        RESCALE_NORMAL {
            @Override
            void enable() {
                GlStateManager.enableRescaleNormal();
            }

            @Override
            void disable() {
                GlStateManager.disableRescaleNormal();
            }
        },
        NORMALIZE {
            @Override
            void enable() {
                GlStateManager.enableNormalize();
            }

            @Override
            void disable() {
                GlStateManager.disableNormalize();
            }
        };

        abstract void enable();

        abstract void disable();
    }
}
