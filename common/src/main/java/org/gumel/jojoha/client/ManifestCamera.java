package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandManifestation;

public final class ManifestCamera {
    public static final float EASE_IN_TICKS = 16F;
    private static final float DETACH_FROM = 0.05F;
    private static final double WALL_GAP = 0.25;
    private static final double SIDE_PROBE = 4.5;
    private static final double CHEST = 1.1;
    private static final double HEAD = 1.75;

    private static final double OPEN_BACK_FROM = 2.8;
    private static final double OPEN_BACK_TO = 2.0;
    private static final double OPEN_SIDE_FROM = 1.4;
    private static final double OPEN_SIDE_TO = 1.1;
    private static final double OPEN_RISE_FROM = 2.1;
    private static final double OPEN_RISE_TO = 1.8;

    private static final double PROFILE_BIAS = 0.6;
    private static final double PROFILE_RISE = 1.0;
    private static final double PROFILE_LIFT = 0.3;
    private static final double PROFILE_FROM = 4.6;
    private static final double PROFILE_TO = 3.8;
    private static final double PROFILE_ORBIT_FROM = -0.25;
    private static final double PROFILE_ORBIT_TO = 0.1;

    private static final double LOW_FROM = 2.7;
    private static final double LOW_TO = 2.1;
    private static final double LOW_SIDE = 0.9;
    private static final double LOW_RISE = 0.45;
    private static final double LOW_AIM_DROP = 0.2;

    private static final double REVEAL_FROM = 1.7;
    private static final double REVEAL_TO = 3.3;
    private static final double REVEAL_SIDE_FROM = 0.35;
    private static final double REVEAL_SIDE_TO = 1.3;
    private static final double REVEAL_RISE_FROM = 1.75;
    private static final double REVEAL_RISE_TO = 1.6;

    private static final double CHASE_RADIUS = 3.6;
    private static final float CHASE_TURN_SHARE = 0.35F;
    private static final double CHASE_FOLLOW = 0.7;
    private static final double CHASE_RISE = 0.3;
    private static final double FOCUS_PULL = 0.7;
    private static final float HANDOFF_WAIT_TICKS = 6F;

    private static StandEntity vision;
    private static double side = 1.0;
    private static Shot handoff;

    private ManifestCamera() {
    }

    public record Shot(Vec3 position, float yaw, float pitch) {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            vision = null;
            return;
        }
        if (vision != null && (vision.isRemoved() || vision.level() != minecraft.level
                || vision.tickCount >= StandManifestation.TOTAL_TICKS)) {
            vision = null;
        }
        if (vision != null) {
            return;
        }
        if (StraySoul.flightOf(minecraft.player.getUUID()) == null) {
            handoff = null;
        }
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof StandEntity stand && stand.isManifesting()
                    && stand.manifestUser() == minecraft.player
                    && stand.tickCount < StandManifestation.TOTAL_TICKS) {
                vision = stand;
                side = pickSide(minecraft.player, stand);
                return;
            }
        }
    }

    public static void clear() {
        vision = null;
        handoff = null;
    }

    public static boolean active() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getCameraEntity() != minecraft.player) {
            return false;
        }
        return vision != null || StraySoul.flightOf(minecraft.player.getUUID()) != null;
    }

    public static boolean detaches(float partialTick) {
        if (!active()) {
            return false;
        }
        if (vision != null) {
            return blend(vision.tickCount + partialTick) > DETACH_FROM;
        }
        Minecraft minecraft = Minecraft.getInstance();
        return flightBlend(StraySoul.flightOf(minecraft.player.getUUID()), now(minecraft, partialTick)) > DETACH_FROM;
    }

    public static Shot frame(Vec3 from, float fromYaw, float fromPitch, float partialTick) {
        if (!active()) {
            return null;
        }
        Player user = Minecraft.getInstance().player;
        if (vision == null) {
            return chase(user, StraySoul.flightOf(user.getUUID()), from, fromYaw, fromPitch, partialTick);
        }
        float t = vision.tickCount + partialTick;

        Vec3 p = user.getPosition(partialTick);
        Vec3 s = vision.getPosition(partialTick);
        Vec3 d = new Vec3(s.x - p.x, 0.0, s.z - p.z);
        if (d.lengthSqr() < 1.0E-4) {
            d = Vec3.directionFromRotation(0F, user.getYRot());
        }
        d = d.normalize();
        Vec3 across = new Vec3(-d.z, 0.0, d.x).scale(side);
        Vec3 standChest = s.add(0.0, CHEST, 0.0);
        Vec3 standHead = s.add(0.0, HEAD, 0.0);

        Vec3 eye;
        Vec3 focus;
        if (t < StandManifestation.SKELETON_AT) {
            float k = smooth(t / StandManifestation.SKELETON_AT);
            eye = p.add(d.scale(-Mth.lerp(k, OPEN_BACK_FROM, OPEN_BACK_TO)))
                    .add(across.scale(Mth.lerp(k, OPEN_SIDE_FROM, OPEN_SIDE_TO)))
                    .add(0.0, Mth.lerp(k, OPEN_RISE_FROM, OPEN_RISE_TO), 0.0);
            focus = standChest;
        } else if (t < StandManifestation.ARMOR_AT) {
            float k = (t - StandManifestation.SKELETON_AT) / StandManifestation.SKELETON_TICKS;
            Vec3 mid = p.lerp(s, PROFILE_BIAS).add(0.0, PROFILE_RISE, 0.0);
            double angle = Mth.lerp(k, PROFILE_ORBIT_FROM, PROFILE_ORBIT_TO);
            Vec3 out = across.scale(Math.cos(angle)).add(d.scale(Math.sin(angle)));
            eye = mid.add(out.scale(Mth.lerp(k, PROFILE_FROM, PROFILE_TO))).add(0.0, PROFILE_LIFT, 0.0);
            focus = mid;
        } else if (t < StandManifestation.FULL_AT) {
            float k = (t - StandManifestation.ARMOR_AT) / StandManifestation.ARMOR_TICKS;
            eye = s.add(d.scale(-Mth.lerp(k, LOW_FROM, LOW_TO))).add(across.scale(LOW_SIDE))
                    .add(0.0, LOW_RISE, 0.0);
            focus = standHead.add(0.0, -LOW_AIM_DROP, 0.0);
        } else {
            float k = smooth((t - StandManifestation.FULL_AT) / StandManifestation.POSE_TICKS);
            eye = s.add(d.scale(-Mth.lerp(k, REVEAL_FROM, REVEAL_TO)))
                    .add(across.scale(Mth.lerp(k, REVEAL_SIDE_FROM, REVEAL_SIDE_TO)))
                    .add(0.0, Mth.lerp(k, REVEAL_RISE_FROM, REVEAL_RISE_TO), 0.0);
            focus = standHead.lerp(standChest, k);
        }

        eye = clear(user, focus, eye);
        Shot shot = new Shot(eye, yawTo(eye, focus), pitchTo(eye, focus));

        float blend = blend(t);
        if (blend < 1F && t < EASE_IN_TICKS && handoff != null) {
            return mix(handoff, shot, blend);
        }
        return fromPlayer(user, shot, blend, from, fromYaw, fromPitch, partialTick);
    }

    private static Shot chase(Player user, StraySoul.Flight flight, Vec3 from, float fromYaw, float fromPitch,
                              float partialTick) {
        float time = now(Minecraft.getInstance(), partialTick);
        float u = StraySoul.travel((time - flight.startTick()) / StraySoul.FLIGHT_TICKS);
        Vec3 chest = StraySoul.chest(user, partialTick);
        Vec3 soul = StraySoul.path(flight, user, partialTick, u);
        Vec3 focus = new Vec3(chest.x, soul.y, chest.z).lerp(soul, FOCUS_PULL);

        double angle = StraySoul.angle(flight, u * CHASE_TURN_SHARE);
        Vec3 eye = new Vec3(chest.x + Math.cos(angle) * CHASE_RADIUS,
                Mth.lerp(CHASE_FOLLOW, chest.y, soul.y) + CHASE_RISE,
                chest.z + Math.sin(angle) * CHASE_RADIUS);
        eye = clear(user, focus, eye);
        Shot shot = new Shot(eye, yawTo(eye, focus), pitchTo(eye, focus));
        handoff = shot;
        return fromPlayer(user, shot, flightBlend(flight, time), from, fromYaw, fromPitch, partialTick);
    }

    private static float flightBlend(StraySoul.Flight flight, float time) {
        if (flight == null) {
            return 0F;
        }
        float age = time - flight.startTick();
        if (age < EASE_IN_TICKS) {
            return smooth(age / EASE_IN_TICKS);
        }
        float settled = StraySoul.FLIGHT_TICKS + HANDOFF_WAIT_TICKS;
        if (age <= settled) {
            return 1F;
        }
        return 1F - smooth((age - settled) / (StraySoul.FLIGHT_TICKS + StraySoul.HOLD_TICKS - settled));
    }

    private static Shot fromPlayer(Player user, Shot shot, float blend, Vec3 from, float fromYaw, float fromPitch,
                                   float partialTick) {
        if (blend >= 1F) {
            return shot;
        }
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            from = user.getEyePosition(partialTick);
            fromYaw = user.getViewYRot(partialTick);
            fromPitch = user.getViewXRot(partialTick);
        }
        return mix(new Shot(from, fromYaw, fromPitch), shot, blend);
    }

    private static Shot mix(Shot from, Shot to, float blend) {
        return new Shot(from.position().lerp(to.position(), blend), Mth.rotLerp(blend, from.yaw(), to.yaw()),
                Mth.lerp(blend, from.pitch(), to.pitch()));
    }

    private static float now(Minecraft minecraft, float partialTick) {
        return minecraft.level == null ? 0F : (float) minecraft.level.getGameTime() + partialTick;
    }

    private static float blend(float t) {
        if (t < EASE_IN_TICKS) {
            return smooth(t / EASE_IN_TICKS);
        }
        if (t >= StandManifestation.DISSOLVE_AT) {
            return 1F - smooth((t - StandManifestation.DISSOLVE_AT) / StandManifestation.DISSOLVE_TICKS);
        }
        return 1F;
    }

    private static double pickSide(Player user, StandEntity stand) {
        Vec3 p = user.position();
        Vec3 s = stand.position();
        Vec3 d = new Vec3(s.x - p.x, 0.0, s.z - p.z);
        if (d.lengthSqr() < 1.0E-4) {
            d = Vec3.directionFromRotation(0F, user.getYRot());
        }
        d = d.normalize();
        Vec3 across = new Vec3(-d.z, 0.0, d.x);
        Vec3 mid = p.lerp(s, 0.5).add(0.0, CHEST, 0.0);
        double right = room(user, mid, mid.add(across.scale(SIDE_PROBE)));
        double left = room(user, mid, mid.add(across.scale(-SIDE_PROBE)));
        return left > right ? -1.0 : 1.0;
    }

    private static double room(Player user, Vec3 from, Vec3 to) {
        HitResult hit = user.level().clip(new ClipContext(from, to, ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE, user));
        return hit.getType() == HitResult.Type.MISS ? from.distanceTo(to) : from.distanceTo(hit.getLocation());
    }

    private static Vec3 clear(Player user, Vec3 focus, Vec3 eye) {
        HitResult hit = user.level().clip(new ClipContext(focus, eye, ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE, user));
        if (hit.getType() == HitResult.Type.MISS) {
            return eye;
        }
        Vec3 back = focus.subtract(eye);
        double length = back.length();
        if (length < 1.0E-4) {
            return eye;
        }
        return hit.getLocation().add(back.scale(WALL_GAP / length));
    }

    private static float yawTo(Vec3 eye, Vec3 focus) {
        return (float) (Mth.atan2(focus.z - eye.z, focus.x - eye.x) * Mth.RAD_TO_DEG) - 90F;
    }

    private static float pitchTo(Vec3 eye, Vec3 focus) {
        double dx = focus.x - eye.x;
        double dz = focus.z - eye.z;
        return (float) (-(Mth.atan2(focus.y - eye.y, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG));
    }

    private static float smooth(float x) {
        float c = Mth.clamp(x, 0F, 1F);
        return c * c * (3F - 2F * c);
    }
}
