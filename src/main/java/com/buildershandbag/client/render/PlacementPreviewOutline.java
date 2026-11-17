package com.buildershandbag.client.render;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;


/**
 * Shape edges for the held Handbag's placement preview.
 */
@SideOnly(Side.CLIENT)
public final class PlacementPreviewOutline {

    private static final float OUTLINE_ALPHA = 0.6F;
    private static final float OUTLINE_WIDTH = 1.0F;
    private static final double POSITION_SCALE = 4096.0D;
    private static final double POSITION_EPSILON_SQUARED = 1.0D / (POSITION_SCALE * POSITION_SCALE);
    private static final double COPLANAR_DOT = 0.9999D;

    private final Collection<Edge> edges;

    private PlacementPreviewOutline(Collection<Edge> edges) {
        this.edges = edges;
    }

    public static PlacementPreviewOutline capture(BufferBuilder buffer) {
        ByteBuffer vertices = buffer.getByteBuffer();
        int stride = buffer.getVertexFormat().getSize();
        int positionOffset = buffer.getVertexFormat().getOffset(0);
        List<Edge> segments = new ArrayList<>();
        Map<Vec3d, List<Vec3d>> neighbors = new HashMap<>();
        for (int vertex = 0; vertex + 3 < buffer.getVertexCount(); vertex += 4) {
            Vec3d[] quad = new Vec3d[4];
            for (int corner = 0; corner < quad.length; corner++) {
                int offset = (vertex + corner) * stride + positionOffset;
                quad[corner] = new Vec3d(roundPosition(vertices.getFloat(offset)),
                    roundPosition(vertices.getFloat(offset + 4)), roundPosition(vertices.getFloat(offset + 8)));
            }

            Vec3d normal = getNormal(quad);
            if (normal == Vec3d.ZERO) continue;

            for (int corner = 0; corner < quad.length; corner++) {
                Vec3d from = quad[corner];
                Vec3d to = quad[(corner + 1) % quad.length];
                if (from.equals(to)) continue;

                segments.add(new Edge(from, to, normal));
                neighbors.computeIfAbsent(from, point -> new ArrayList<>()).add(to);
                neighbors.computeIfAbsent(to, point -> new ArrayList<>()).add(from);
            }
        }

        Map<EdgeKey, Edge> edges = new HashMap<>();
        for (Edge segment : segments) appendEdge(edges, neighbors, segment);

        return new PlacementPreviewOutline(edges.values());
    }

    public void render(Tessellator tessellator) {
        if (edges.isEmpty()) return;

        float previousWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        GlStateManager.glLineWidth(OUTLINE_WIDTH);
        GlStateManager.disableTexture2D();
        try {
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            for (Edge edge : edges) {
                if (edge.seam && !edge.crease) continue;

                buffer.pos(edge.from.x, edge.from.y, edge.from.z).color(0.0F, 0.0F, 0.0F, OUTLINE_ALPHA).endVertex();
                buffer.pos(edge.to.x, edge.to.y, edge.to.z).color(0.0F, 0.0F, 0.0F, OUTLINE_ALPHA).endVertex();
            }

            tessellator.draw();
        } finally {
            GlStateManager.enableTexture2D();
            GlStateManager.glLineWidth(previousWidth);
        }
    }

    private static double roundPosition(float coordinate) {
        // Match vertices that differ through model rounding
        return Math.round(coordinate * POSITION_SCALE) / POSITION_SCALE;
    }

    private static Vec3d getNormal(Vec3d[] quad) {
        // Triangle models can repeat a vertex to fill the quad
        for (int corner = 1; corner < quad.length - 1; corner++) {
            Vec3d normal = quad[corner].subtract(quad[0]).crossProduct(quad[corner + 1].subtract(quad[0]));
            double lengthSquared = normal.lengthSquared();
            if (lengthSquared > 0.0D) return normal.scale(1.0D / Math.sqrt(lengthSquared));
        }

        return Vec3d.ZERO;
    }

    private static void appendEdge(Map<EdgeKey, Edge> edges, Map<Vec3d, List<Vec3d>> neighbors, Edge segment) {
        Vec3d from = segment.from;
        Vec3d direction = segment.to.subtract(from);
        double lengthSquared = direction.lengthSquared();
        double previous = 0.0D;
        while (!from.equals(segment.to)) {
            Vec3d to = segment.to;
            double nearest = 1.0D;
            // Split shared edges where adjacent faces use shorter segments
            Vec3d[] endpoints = { from, segment.to };
            for (Vec3d endpoint : endpoints) {
                for (Vec3d candidate : neighbors.get(endpoint)) {
                    double progress = candidate.subtract(segment.from).dotProduct(direction) / lengthSquared;
                    if (progress <= previous || progress >= nearest) continue;

                    Vec3d projected = segment.from.add(direction.scale(progress));
                    if (candidate.squareDistanceTo(projected) > POSITION_EPSILON_SQUARED) continue;

                    to = candidate;
                    nearest = progress;
                }
            }

            EdgeKey key = new EdgeKey(from, to);
            Edge edge = edges.get(key);
            if (edge == null) {
                edges.put(key, new Edge(from, to, segment.normal));
            } else {
                edge.addFace(segment);
            }

            from = to;
            previous = nearest;
        }
    }

    private static final class Edge {

        private final Vec3d from;
        private final Vec3d to;
        private final Vec3d normal;
        private final Vec3d inward;
        private boolean seam;
        private boolean crease;

        private Edge(Vec3d from, Vec3d to, Vec3d normal) {
            this.from = from;
            this.to = to;
            this.normal = normal;
            this.inward = normal.crossProduct(to.subtract(from));
        }

        private void addFace(Edge face) {
            if (Math.abs(normal.dotProduct(face.normal)) < COPLANAR_DOT) {
                crease = true;
            } else if (inward.dotProduct(face.inward) < 0.0D) {
                // Coplanar faces on opposite sides of the edge share a flat seam
                seam = true;
            }
        }
    }

    private static final class EdgeKey {

        private final Vec3d from;
        private final Vec3d to;

        private EdgeKey(Vec3d from, Vec3d to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof EdgeKey)) return false;

            EdgeKey edge = (EdgeKey) object;
            return from.equals(edge.from) && to.equals(edge.to) || from.equals(edge.to) && to.equals(edge.from);
        }

        @Override
        public int hashCode() {
            return from.hashCode() + to.hashCode();
        }
    }
}
