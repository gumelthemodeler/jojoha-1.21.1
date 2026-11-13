package org.gumel.jojoha.hamon;

public final class BubbleFlight {
    public enum Kind {
        DRIFT(0.94),

        SHOT(0.86);

        private final double drag;

        Kind(double drag) {
            this.drag = drag;
        }

        public double drag() {
            return drag;
        }
    }

    public static final double MIN_SPEED_SQ = 0.0016;

    public static final double MAX_TRAVEL = 26.0;

    public static final float PINK_R = 1.0F;
    public static final float PINK_G = 0.44F;
    public static final float PINK_B = 0.82F;
    public static final float CYAN_R = 0.38F;
    public static final float CYAN_G = 0.86F;
    public static final float CYAN_B = 1.0F;

    private BubbleFlight() {
    }

    public static int lifetime(double speed, Kind kind) {
        double travelled = 0.0;
        int ticks = 0;
        while (speed * speed >= MIN_SPEED_SQ && travelled < MAX_TRAVEL && ticks < 400) {
            travelled += speed;
            speed *= kind.drag();
            ticks++;
        }
        return Math.max(1, ticks);
    }

    public static float mix(float from, float to, float through) {
        return from + (to - from) * through;
    }
}
