package com.retro.eaglevision;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public class GlowingMultiBufferSource implements MultiBufferSource {
    private final MultiBufferSource original;
    private final float red, green, blue, alpha;

    public GlowingMultiBufferSource(MultiBufferSource original, float red, float green, float blue, float alpha) {
        this.original = original;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        VertexConsumer originalBuffer = original.getBuffer(renderType);
        return new ColoredVertexConsumer(originalBuffer, red, green, blue, alpha);
    }

    private static class ColoredVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int red, green, blue, alpha;

        public ColoredVertexConsumer(VertexConsumer delegate, float red, float green, float blue, float alpha) {
            this.delegate = delegate;
            // Convertir en int et clamper entre 0-255
            this.red = Math.min(255, Math.max(0, (int)(red * 255)));
            this.green = Math.min(255, Math.max(0, (int)(green * 255)));
            this.blue = Math.min(255, Math.max(0, (int)(blue * 255)));
            this.alpha = Math.min(255, Math.max(0, (int)(alpha * 255)));
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            return delegate.vertex(x, y, z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            // Forcer notre couleur au lieu de la couleur originale
            return delegate.color(this.red, this.green, this.blue, this.alpha);
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return delegate.uv(u, v);
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return delegate.overlayCoords(u, v);
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            // Lumière maximale pour que ce soit visible
            return delegate.uv2(15728880, 0);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return delegate.normal(x, y, z);
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            // Utiliser notre couleur par défaut
            delegate.defaultColor(this.red, this.green, this.blue, this.alpha);
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
    }
}