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

/**
 * The vine that the whip and the gut punch throw, drawn as an actual rope.
 *
 * <p>Both moves used to draw their reach as a line of motes, which was honest about being a
 * placeholder: a Stand whose entire identity is a thorned vine had two moves where the vine was a
 * dotted line. This draws the same rope the grapple does, off the same code, so all four of Hermit
 * Purple's throws are visibly the same thing.
 *
 * <h2>Told, not spawned</h2>
 *
 * <p>The grapple has an entity because it stays out and has to be simulated. A whip is over inside a
 * tick, and an entity that exists for one tick costs a spawn, a tracker and a despawn to be seen
 * once. So the server sends two entity ids and a duration, and the client draws it.
 *
 * <p>Ids rather than positions, because both ends move while it is on screen - the thrower turns and
 * the victim is being dragged - and coordinates captured on one server tick would leave the rope
 * hanging between where the two of them used to be.
 *
 * <h2>The slack tells you which way it is going</h2>
 *
 * <p>A lash starts loose and pulls taut, which is the difference between a rope being thrown and a
 * rope being reeled. Reading it off the age costs nothing and means the whip and the gut punch look
 * like what they are without either of them having to say so.
 */
public final class ThornLashFx {
    /** How much longer than the gap the vine is drawn at its loosest, in blocks. */
    private static final double SLACK_START = 2.2;

    private static final List<Lash> ACTIVE = new ArrayList<>();

    private ThornLashFx() {
    }

    /** Called when a packet lands. A new lash from the same thrower replaces the old one. */
    public static void begin(int fromId, int toId, int ticks) {
        ACTIVE.removeIf(lash -> lash.fromId == fromId);
        ACTIVE.add(new Lash(fromId, toId, ticks));
    }

    public static void tick() {
        Iterator<Lash> it = ACTIVE.iterator();
        while (it.hasNext()) {
            if (--it.next().remaining <= 0) {
                it.remove();
            }
        }
    }

    /** Dropped on disconnect and dimension change, like everything else keyed by entity id. */
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

            // Off the hand, the same place the grapple and the lasso leave from, so all of this
            // Stand's throws come out of one point.
            Vec3 start = from instanceof Player player
                    ? player.getRopeHoldPosition(partialTick)
                    : at(from, partialTick).add(0, from.getBbHeight() * 0.6, 0);

            Vec3 end = at(to, partialTick).add(0, to.getBbHeight() * 0.55, 0);

            // Loose at the throw and taut by the end - see the class note.
            float left = (lash.remaining - partialTick) / lash.ticks;
            double slack = SLACK_START * Mth.clamp(left, 0F, 1F);

            poseStack.pushPose();
            poseStack.translate(start.x - cameraPos.x, start.y - cameraPos.y, start.z - cameraPos.z);

            Vec3 span = end.subtract(start);
            ThornRope.draw(poseStack, buffers, from.level(), span, start, span.length() + slack,
                    HermitSkins.of(from));

            poseStack.popPose();
        }

        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
    }

    /** Where an entity is drawn this frame. */
    private static Vec3 at(Entity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }

    /** One vine in flight: who threw it at whom, and how long is left of it. */
    private static final class Lash {
        private final int fromId;
        private final int toId;
        private final int ticks;
        private int remaining;

        private Lash(int fromId, int toId, int ticks) {
            this.fromId = fromId;
            this.toId = toId;
            this.ticks = Math.max(1, ticks);
            this.remaining = this.ticks;
        }
    }
}
