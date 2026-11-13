package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ThornLashFx {
    private static final double SLACK_START = 2.2;

    private static final float WIND_ON_TICKS = 4F;

    private static final double TURNS = 2.5;

    private static final double COIL_RISE = 0.34;

    private static final double COIL_CLEARANCE = 0.16;
    private static final double COIL_OPEN = 2.1;

    private static final List<Lash> ACTIVE = new ArrayList<>();

    private ThornLashFx() {
    }

    public static void begin(int fromId, int toId, int ticks, int coilAfter) {
        ACTIVE.removeIf(lash -> lash.fromId == fromId);
        if (ticks > 0) {
            ACTIVE.add(new Lash(fromId, toId, ticks, coilAfter));
        }
    }

    public static void tick() {
        Iterator<Lash> it = ACTIVE.iterator();
        while (it.hasNext()) {
            if (--it.next().remaining <= 0) {
                it.remove();
            }
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                              float partialTick) {
        if (ACTIVE.isEmpty() || Minecraft.getInstance().level == null) {
            return;
        }

        Vec3 cameraPos = camera.getPosition();

        for (Lash lash : ACTIVE) {
            Entity from = Minecraft.getInstance().level.getEntity(lash.fromId);
            Entity to = Minecraft.getInstance().level.getEntity(lash.toId);
            if (from == null || to == null) {
                continue;
            }

            Vec3 start = from instanceof Player player
                    ? player.getRopeHoldPosition(partialTick)
                    : at(from, partialTick).add(0, from.getBbHeight() * 0.6, 0);

            Vec3 end = at(to, partialTick).add(0, to.getBbHeight() * 0.55, 0);

            float age = lash.ticks - lash.remaining + partialTick;
            float loose = lash.coilAfter >= 0
                    ? 1F - age / Math.max(1F, lash.coilAfter)
                    : (lash.remaining - partialTick) / lash.ticks;
            double slack = SLACK_START * Mth.clamp(loose, 0F, 1F);

            poseStack.pushPose();
            poseStack.translate(start.x - cameraPos.x, start.y - cameraPos.y, start.z - cameraPos.z);

            Vec3 span = end.subtract(start);
            ThornRope.draw(poseStack, buffers, from.level(), span, start, span.length() + slack,
                    HermitSkins.of(from));

            poseStack.popPose();

            wrap(poseStack, buffers, cameraPos, lash, to, end, age, HermitSkins.of(from));
        }

        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
    }

    private static void wrap(PoseStack poseStack, MultiBufferSource buffers, Vec3 cameraPos,
                             Lash lash, Entity to, Vec3 middle, float age, int skin) {
        if (lash.coilAfter < 0 || age < lash.coilAfter) {
            return;
        }

        float on = Mth.clamp((age - lash.coilAfter) / WIND_ON_TICKS, 0F, 1F);
        double snug = to.getBbWidth() * 0.5 + COIL_CLEARANCE;

        poseStack.pushPose();
        poseStack.translate(middle.x - cameraPos.x, middle.y - cameraPos.y, middle.z - cameraPos.z);
        ThornRope.coil(poseStack, buffers, to.level(), Vec3.ZERO,
                Mth.lerp(on, snug * COIL_OPEN, snug),
                to.getBbHeight() * COIL_RISE,
                TURNS * Mth.lerp(on, 0.35F, 1F),
                middle, skin);
        poseStack.popPose();
    }

    private static Vec3 at(Entity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }

    private static final class Lash {
        private final int fromId;
        private final int toId;
        private final int ticks;

        private final int coilAfter;
        private int remaining;

        private Lash(int fromId, int toId, int ticks, int coilAfter) {
            this.fromId = fromId;
            this.toId = toId;
            this.ticks = Math.max(1, ticks);
            this.coilAfter = coilAfter;
            this.remaining = this.ticks;
        }
    }
}
