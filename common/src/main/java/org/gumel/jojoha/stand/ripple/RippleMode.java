package org.gumel.jojoha.stand.ripple;

public enum RippleMode {
    AMBIENT(1F, 1F, 1F, 1F, 1F),

    SPLASH(0.6F, 3.0F, 1.2F, 1.1F, 1.4F),

    BARRIER(1.3F, 0.35F, 1.1F, 1F, 0.6F),

    TENDRIL(1.25F, 1.6F, 1F, 1F, 1.2F),

    DEFENSIVE(1.2F, 0.8F, 0.75F, 1F, 0.7F),

    CHARGED(1F, 1.5F, 1.3F, 1.1F, 1.3F);

    private final float frequency;
    private final float speed;
    private final float glow;
    private final float width;
    private final float jitter;

    RippleMode(float frequency, float speed, float glow, float width, float jitter) {
        this.frequency = frequency;
        this.speed = speed;
        this.glow = glow;
        this.width = width;
        this.jitter = jitter;
    }

    public float frequency() {
        return frequency;
    }

    public float speed() {
        return speed;
    }

    public float glow() {
        return glow;
    }

    public float width() {
        return width;
    }

    public float jitter() {
        return jitter;
    }
}
