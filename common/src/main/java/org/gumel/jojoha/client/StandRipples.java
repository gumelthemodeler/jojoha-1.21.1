package org.gumel.jojoha.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandMovementState;
import org.gumel.jojoha.stand.ripple.RippleMode;
import org.gumel.jojoha.stand.ripple.RippleProfiles;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class StandRipples {
    private static final float RESTING = 1F;

    private static final float BARRAGE_LIFT = 0.9F;
    private static final float ATTACK_LIFT = 0.55F;
    private static final float STANCE_LIFT = 0.3F;
    private static final float SPEED_LIFT = 2.2F;

    private static final float MAX_INTENSITY = 2.4F;

    private static final float EASE = 0.18F;

    private static final float MODE_EASE = 0.25F;

    private static final float MODE_DIP = 0.55F;

    private static final double TRACK_RANGE = 48.0;

    private static final Map<Integer, Live> LIVE = new HashMap<>();

    private StandRipples() {
    }

    public static final class Live {
        private float intensity = RESTING;
        private RippleMode mode = RippleMode.AMBIENT;
        private RippleMode asked;
        private long askedUntil;
        private float blend = 1F;

        private Live() {
        }

        public float intensity() {
            return intensity;
        }

        public RippleMode mode() {
            return mode;
        }

        public float blend() {
            return blend;
        }

        public float settled() {
            return intensity * (MODE_DIP + (1F - MODE_DIP) * blend);
        }
    }

    @Nullable
    public static Live of(StandEntity stand) {
        if (!RippleProfiles.has(stand.getStandType().id())) {
            return null;
        }
        return LIVE.computeIfAbsent(stand.getId(), id -> new Live());
    }

    public static void swell(StandEntity stand, RippleMode mode, float seconds) {
        Live live = of(stand);
        if (live == null) {
            return;
        }

        live.asked = mode;
        live.askedUntil = Util.getMillis() + (long) (Math.max(0F, seconds) * 1000F);
    }

    public static double seconds() {
        return Util.getMillis() / 1000.0;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }

        long now = Util.getMillis();
        Vec3 eye = minecraft.gameRenderer.getMainCamera().getPosition();

        Iterator<Map.Entry<Integer, Live>> it = LIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Live> entry = it.next();
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (!(entity instanceof StandEntity stand) || !stand.isAlive()
                    || stand.position().distanceToSqr(eye) > TRACK_RANGE * TRACK_RANGE) {
                it.remove();
                continue;
            }

            Live live = entry.getValue();
            live.intensity = Mth.lerp(EASE, live.intensity, target(stand));

            RippleMode wanted = live.asked != null && now < live.askedUntil ? live.asked : derived(stand);
            if (wanted != live.mode) {
                live.blend = Math.max(0F, live.blend - MODE_EASE);
                if (live.blend <= 0F) {
                    live.mode = wanted;
                }
            } else {
                live.blend = Math.min(1F, live.blend + MODE_EASE);
            }

            if (live.asked != null && now >= live.askedUntil) {
                live.asked = null;
            }
        }
    }

    private static RippleMode derived(StandEntity stand) {
        StandMovementState state = stand.movementState();
        if (state == StandMovementState.GUARDING) {
            return RippleMode.DEFENSIVE;
        }
        if (state == StandMovementState.BARRAGE || state == StandMovementState.ATTACK) {
            return RippleMode.CHARGED;
        }
        return RippleMode.AMBIENT;
    }

    private static float target(StandEntity stand) {
        float lift = RESTING;
        StandMovementState state = stand.movementState();
        if (stand.isBarraging() || state == StandMovementState.BARRAGE) {
            lift += BARRAGE_LIFT;
        } else if (state == StandMovementState.ATTACK) {
            lift += ATTACK_LIFT;
        } else if (state.isCombatStance()) {
            lift += STANCE_LIFT;
        }

        double travel = stand.position().distanceTo(new Vec3(stand.xo, stand.yo, stand.zo));
        lift += (float) Math.min(0.6, travel * SPEED_LIFT);

        return Math.min(MAX_INTENSITY, lift);
    }

    public static void clear() {
        LIVE.clear();
        RippleTexture.forget();
    }
}
