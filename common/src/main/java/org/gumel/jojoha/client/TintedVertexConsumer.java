package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Passes geometry straight through but overrides every vertex colour with one fixed value.
 *
 * <p>Item models can't simply be handed a colour: {@code ItemRenderer} pulls a per-quad tint out of
 * the stack's own colour handlers as it walks the model, so any colour set from outside is
 * overwritten quad by quad. Intercepting at the vertex consumer is the one point downstream of all
 * of that, which makes it possible to re-run vanilla's own model rendering - transforms, quads and
 * all - and still dictate the colour that comes out the far end.
 */
public final class TintedVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;

    public TintedVertexConsumer(VertexConsumer delegate, int rgb, int alpha) {
        this.delegate = delegate;
        this.red = (rgb >> 16) & 0xFF;
        this.green = (rgb >> 8) & 0xFF;
        this.blue = rgb & 0xFF;
        this.alpha = alpha;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        delegate.setColor(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(x, y, z);
        return this;
    }
}
