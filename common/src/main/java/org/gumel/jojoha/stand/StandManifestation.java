package org.gumel.jojoha.stand;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.JojohaAdvancements;
import org.gumel.jojoha.data.StandData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.function.Supplier;

public final class StandManifestation {
    public static final int SOUL_TICKS = 60;
    public static final int SKELETON_TICKS = 50;
    public static final int ARMOR_TICKS = 60;
    public static final int POSE_TICKS = 80;
    public static final int DISSOLVE_TICKS = 30;

    public static final int SKELETON_AT = SOUL_TICKS;
    public static final int ARMOR_AT = SKELETON_AT + SKELETON_TICKS;
    public static final int FULL_AT = ARMOR_AT + ARMOR_TICKS;
    public static final int DISSOLVE_AT = FULL_AT + POSE_TICKS;
    public static final int TOTAL_TICKS = DISSOLVE_AT + DISSOLVE_TICKS;

    public static final int EXIT_TICKS = 16;
    public static final int MATERIALIZE_TICKS = DISSOLVE_TICKS;
    public static final int MATERIALIZED_AT = EXIT_TICKS + MATERIALIZE_TICKS;
    public static final int FILL_TICKS = 44;
    public static final int SNAP_FROM = ARMOR_AT + 6;
    public static final int SNAP_UNTIL = FULL_AT - 6;
    public static final int SNAP_POP_TICKS = 5;
    public static final int FLASH_TICKS = 14;
    public static final int POSE_SNAP_TICKS = 6;
    public static final int NAME_AFTER_FULL_TICKS = 20;
    public static final int NAME_AT = FULL_AT + NAME_AFTER_FULL_TICKS;

    private static final double DISTANCE = 2.75;
    private static final double MIN_DISTANCE = 1.4;
    private static final double WALL_MARGIN = 0.6;
    private static final double LIFT = 0.35;
    private static final double FIGURE_HEIGHT = 2.0;
    private static final double FIGURE_HALF_WIDTH = 0.3;

    private static final double MOTE_GLIDE = 12.5;
    private static final int RETURN_MOTES_PER_TICK = 4;
    private static final int BURST_MOTES = 90;
    private static final double BURST_MIN_SPEED = 0.25;
    private static final double BURST_SPEED_SPREAD = 0.35;

    public static final String AWAKENED_ADVANCEMENT = "part3/stand_proud";

    private StandManifestation() {
    }

    public static boolean begin(ServerPlayer user, StandData stand) {
        StandType type = StandTypes.byIdOrDefault(stand.standId());
        if (!type.form().isFreeStanding()) {
            return false;
        }

        ServerLevel level = user.serverLevel();
        StandEntity vision = new StandEntity(ModRegistries.STAND.get(), level);
        vision.setStandType(stand.standId());
        vision.setSkin(stand.skin());
        vision.manifestFor(user);

        Vec3 at = spot(user);
        float yaw = facing(at, user.position());
        vision.moveTo(at.x, at.y, at.z, yaw, 0F);
        vision.setYHeadRot(yaw);
        vision.setYBodyRot(yaw);
        vision.setInvisible(false);
        level.addFreshEntity(vision);
        return true;
    }

    public static void tick(StandEntity vision) {
        Player user = vision.manifestUser();
        if (user == null || !user.isAlive() || user.level() != vision.level()) {
            if (!vision.level().isClientSide()) {
                if (user instanceof ServerPlayer player) {
                    JojohaAdvancements.award(player, AWAKENED_ADVANCEMENT);
                }
                vision.discard();
            }
            return;
        }

        float yaw = facing(vision.position(), user.position());
        vision.setYRot(yaw);
        vision.setYBodyRot(yaw);
        vision.setYHeadRot(yaw);
        vision.setDeltaMovement(Vec3.ZERO);

        if (!(vision.level() instanceof ServerLevel level) || !(user instanceof ServerPlayer player)) {
            return;
        }

        int t = vision.tickCount;
        if (t == SKELETON_AT) {
            level.playSound(null, vision.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 1.0F, 0.5F);
        }
        if (t == FULL_AT) {
            burst(level, vision);
            NetworkHandler.broadcastStandNamed(player, vision.getStandType().id());
            JojohaAdvancements.award(player, AWAKENED_ADVANCEMENT);
        }
        if (t == NAME_AT) {
            speak(level, vision);
        }
        if (t >= DISSOLVE_AT) {
            stream(level, chest(user), vision, RETURN_MOTES_PER_TICK);
        }
        if (t >= TOTAL_TICKS) {
            vision.discard();
        }
    }

    public static boolean outlined(float ticks) {
        return ticks >= MATERIALIZED_AT && ticks < FULL_AT;
    }

    public static boolean posed(float ticks) {
        return ticks >= FULL_AT;
    }

    private static Vec3 spot(ServerPlayer user) {
        float yaw = user.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 eye = user.getEyePosition();

        HitResult hit = user.level().clip(new ClipContext(eye, eye.add(forward.scale(DISTANCE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
        double distance = hit.getType() == HitResult.Type.MISS
                ? DISTANCE
                : Math.max(MIN_DISTANCE, hit.getLocation().distanceTo(eye) - WALL_MARGIN);

        return user.position().add(forward.scale(distance)).add(0.0, LIFT, 0.0);
    }

    private static float facing(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90F;
    }

    private static Vec3 chest(Player user) {
        return new Vec3(user.getX(), user.getY() + user.getBbHeight() * 0.6, user.getZ());
    }

    private static void stream(ServerLevel level, Vec3 chest, StandEntity vision, int motes) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < motes; i++) {
            Vec3 body = vision.position().add(
                    (random.nextDouble() * 2.0 - 1.0) * FIGURE_HALF_WIDTH,
                    random.nextDouble() * FIGURE_HEIGHT,
                    (random.nextDouble() * 2.0 - 1.0) * FIGURE_HALF_WIDTH);
            Vec3 path = chest.subtract(body).scale(1.0 / MOTE_GLIDE);

            level.sendParticles(mote(i), body.x, body.y, body.z, 0, path.x, path.y, path.z, 1.0);
        }
    }

    private static SimpleParticleType mote(int i) {
        return switch (i % 3) {
            case 0 -> ModRegistries.STAND_AWAKEN_BLUE.get();
            case 1 -> ModRegistries.STAND_AWAKEN_PINK.get();
            default -> ModRegistries.STAND_AWAKEN.get();
        };
    }

    private static void burst(ServerLevel level, StandEntity vision) {
        RandomSource random = level.getRandom();
        double centreY = vision.getY() + FIGURE_HEIGHT * 0.5;

        for (int i = 0; i < BURST_MOTES; i++) {
            double theta = random.nextDouble() * Math.PI * 2.0;
            double z = random.nextDouble() * 2.0 - 1.0;
            double r = Math.sqrt(1.0 - z * z);
            double speed = BURST_MIN_SPEED + random.nextDouble() * BURST_SPEED_SPREAD;

            level.sendParticles(mote(i), vision.getX(), centreY, vision.getZ(),
                    0, Math.cos(theta) * r * speed, z * speed, Math.sin(theta) * r * speed, 1.0);
        }

        level.playSound(null, vision.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    private static void speak(ServerLevel level, StandEntity vision) {
        Supplier<SoundEvent> voice = vision.getStandType().voice();
        if (voice != null && voice.get() != null) {
            level.playSound(null, vision.blockPosition(), voice.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
