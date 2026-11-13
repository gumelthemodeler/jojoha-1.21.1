package org.gumel.jojoha.stand;

import net.minecraft.util.Mth;

public final class StandLimbFlow {
    private static final float NEAR_RESPONSE = 0.45F;
    private static final float MID_RESPONSE = 0.22F;
    private static final float FAR_RESPONSE = 0.12F;

    private static final double DEADZONE = 0.004;

    private static final float MAX_SPEED = 0.9F;

    private final Axis forward = new Axis();
    private final Axis lateral = new Axis();

    public void tick(double dx, double dz, float yawDegrees) {
        float yaw = (float) Math.toRadians(yawDegrees);

        double along = -dx * Math.sin(yaw) + dz * Math.cos(yaw);
        double across = dx * Math.cos(yaw) + dz * Math.sin(yaw);

        forward.tick(clean(along));
        lateral.tick(clean(across));
    }

    private static float clean(double travel) {
        if (Math.abs(travel) < DEADZONE) {
            return 0F;
        }
        return Mth.clamp((float) travel, -MAX_SPEED, MAX_SPEED);
    }

    public Axis forward() {
        return forward;
    }

    public Axis lateral() {
        return lateral;
    }

    public static final class Axis {
        private final Stage near = new Stage(NEAR_RESPONSE);
        private final Stage mid = new Stage(MID_RESPONSE);
        private final Stage far = new Stage(FAR_RESPONSE);

        private void tick(float target) {
            near.tick(target);
            mid.tick(near.value);
            far.tick(mid.value);
        }

        public float far(float partialTick) {
            return far.at(partialTick);
        }
    }

    private static final class Stage {
        private final float response;
        private float value;
        private float previous;

        private Stage(float response) {
            this.response = response;
        }

        private void tick(float target) {
            previous = value;
            value += (target - value) * response;
        }

        private float at(float partialTick) {
            return Mth.lerp(partialTick, previous, value);
        }
    }
}
