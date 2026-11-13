package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public final class ThrownTumble {
    private static final float LIFT_OFF_TICKS = 3F;
    private static final float SETTLE_TICKS = 5F;
    private static final float MAX_TICKS = 100F;

    private static final Map<Integer, Tumble> TUMBLING = new HashMap<>();

    private ThrownTumble() {
    }

    public static void heard(int entityId, float heading, float spin) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        TUMBLING.put(entityId, new Tumble(heading, spin, Minecraft.getInstance().level.getGameTime()));
    }

    public static void clear() {
        TUMBLING.clear();
    }

    public static void apply(LivingEntity entity, PoseStack poseStack, float partialTick) {
        if (TUMBLING.isEmpty()) {
            return;
        }
        Tumble tumble = TUMBLING.get(entity.getId());
        if (tumble == null) {
            return;
        }
        float now = entity.level().getGameTime() + partialTick;
        float age = now - tumble.start;
        if (age < 0F || age > MAX_TICKS || entity.isRemoved()) {
            TUMBLING.remove(entity.getId());
            return;
        }
        float angle;
        if (tumble.landedAt < 0F) {
            angle = age * tumble.spin;
            if (age > LIFT_OFF_TICKS && (entity.onGround() || entity.isInWater())) {
                tumble.landedAt = age;
                tumble.landedAngle = angle;
            }
        } else {
            float settle = Mth.clamp((age - tumble.landedAt) / SETTLE_TICKS, 0F, 1F);
            if (settle >= 1F) {
                TUMBLING.remove(entity.getId());
                return;
            }
            float upright = Math.round(tumble.landedAngle / 360F) * 360F;
            float eased = 1F - (1F - settle) * (1F - settle);
            angle = Mth.lerp(eased, tumble.landedAngle, upright);
        }
        double rad = Math.toRadians(tumble.heading);
        Vector3f axis = new Vector3f((float) Math.sin(rad), 0F, (float) -Math.cos(rad));
        float middle = entity.getBbHeight() * 0.5F;
        poseStack.translate(0F, middle, 0F);
        poseStack.mulPose(Axis.of(axis).rotationDegrees(angle));
        poseStack.translate(0F, -middle, 0F);
    }

    private static final class Tumble {
        private final float heading;
        private final float spin;
        private final long start;
        private float landedAt = -1F;
        private float landedAngle;

        private Tumble(float heading, float spin, long start) {
            this.heading = heading;
            this.spin = spin;
            this.start = start;
        }
    }
}
