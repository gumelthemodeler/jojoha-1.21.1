package org.gumel.jojoha.pillar.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.ClackerPhysics;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.pillar.PillarAim;
import org.gumel.jojoha.pillar.PillarFx;
import org.gumel.jojoha.pillar.PillarFxKind;
import org.gumel.jojoha.pillar.PillarMove;
import org.gumel.jojoha.pillar.PillarPoses;
import org.gumel.jojoha.stand.skill.StandBeat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TornadoMove extends PillarMove {
    public static final ResourceLocation ID = id("tornado");
    public static final TornadoMove INSTANCE = new TornadoMove();

    public static final int LAUNCH_TICKS = PillarPoses.TORNADO_RELEASE_TICKS;
    public static final int TRAVEL_TICKS = 70;
    public static final double SPEED = 0.28;
    public static final double RADIUS = 1.6;
    public static final double HEIGHT = 3.5;
    public static final float DAMAGE = 3.0F;
    public static final int HIT_EVERY = 20;
    private static final double LIFT = 0.32;
    private static final double SWIRL = 0.22;
    private static final double PULL = 0.08;
    public static final double STEER_REACH = 24.0;
    private static final double ARRIVE = 0.4;

    private static final Map<UUID, Long> STEERING = new HashMap<>();

    private TornadoMove() {
        super("tornado", 240, 30F);
    }

    @Override
    public boolean isSustained() {
        return true;
    }

    @Override
    public boolean isSustainActive(ServerPlayer player) {
        return steering(player);
    }

    @Override
    public void release(ServerPlayer player, JojohaPlayerData data) {
        STEERING.remove(player.getUUID());
    }

    private static boolean steering(ServerPlayer player) {
        Long until = STEERING.get(player.getUUID());
        return until != null && player.level().getGameTime() <= until;
    }

    private static Vec3 aim(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        return player.level().clip(new ClipContext(eye,
                eye.add(player.getLookAngle().scale(STEER_REACH)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getLocation();
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        STEERING.put(player.getUUID(), player.level().getGameTime() + LAUNCH_TICKS + TRAVEL_TICKS);
        MovementFx.pose(player, PillarPoses.TORNADO);
        PillarFx.charge(player, PillarFx.WIND_STYLE, LAUNCH_TICKS);
        NetworkHandler.sendFirstPersonPose(player, MovementFx.CLACKER_SPIN);
        ServerLevel level = player.serverLevel();
        StandBeat.during(ClackerPhysics.WHIRL_START_TICKS, LAUNCH_TICKS - 1, player, tick -> {
            Vec3 hand = PillarAim.hand(player);
            PillarFx.spray(level, PillarFx.gust(), hand, 1, 0.15, 0.02);
            PillarFx.spray(level, PillarFx.sand(), hand, 2, 0.2, 0.08);
        });
        StandBeat.at(LAUNCH_TICKS, player, () -> launch(player));
    }

    private static void launch(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 first = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3[] heading = {first};
        Vec3 start = ground(level, player.position().add(first.scale(1.5)));
        Vec3[] at = {start != null ? start : player.position().add(first.scale(1.5))};
        boolean[] spent = {false};
        Map<Integer, Long> struck = new HashMap<>();

        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.5F);
        StandBeat.during(1, TRAVEL_TICKS, player, tick -> {
            if (spent[0]) {
                return;
            }
            boolean steered = steering(player);
            Vec3 step = heading[0].scale(SPEED);
            if (steered) {
                Vec3 aim = aim(player);
                Vec3 toward = new Vec3(aim.x - at[0].x, 0, aim.z - at[0].z);
                if (toward.lengthSqr() < ARRIVE * ARRIVE) {
                    step = Vec3.ZERO;
                } else {
                    heading[0] = toward.normalize();
                    step = heading[0].scale(SPEED);
                }
            }
            Vec3 next = ground(level, at[0].add(step));
            if (next == null && steered) {
                next = at[0];
                step = Vec3.ZERO;
            }
            if (tick >= TRAVEL_TICKS) {
                STEERING.remove(player.getUUID());
            }
            if (next == null) {
                spent[0] = true;
                PillarFx.spray(level, PillarFx.gust(), at[0].add(0, 1.0, 0), 8, 0.6, 0.1);
                PillarFx.spray(level, PillarFx.sand(), at[0].add(0, 1.0, 0), 20, 0.8, 0.2);
                return;
            }
            at[0] = next;
            long now = level.getGameTime();
            for (LivingEntity target : PillarAim.around(player, at[0].add(0, HEIGHT * 0.5, 0),
                    Math.max(RADIUS, HEIGHT * 0.5))) {
                Vec3 offset = target.position().subtract(at[0]);
                if (Math.sqrt(offset.x * offset.x + offset.z * offset.z) > RADIUS + target.getBbWidth() * 0.5
                        || offset.y < -0.5 || offset.y > HEIGHT) {
                    continue;
                }
                Vec3 inward = new Vec3(-offset.x, 0, -offset.z);
                Vec3 around = new Vec3(-offset.z, 0, offset.x);
                Vec3 spin = around.lengthSqr() < 1.0E-4 ? Vec3.ZERO : around.normalize().scale(SWIRL);
                target.setDeltaMovement(step.add(spin).add(inward.scale(PULL))
                        .add(0, Math.max(target.getDeltaMovement().y, LIFT), 0));
                target.hurtMarked = true;
                target.fallDistance = 0F;
                Long last = struck.get(target.getId());
                if (last == null || now - last >= HIT_EVERY) {
                    struck.put(target.getId(), now);
                    target.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, target, DAMAGE));
                }
            }
            NetworkHandler.sendPillarFx(level, PillarFxKind.TORNADO, player.getId(), 0, TRAVEL_TICKS - tick, at[0],
                    Vec3.ZERO);
            if (tick % 15 == 1) {
                level.playSound(null, BlockPos.containing(at[0]), SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS,
                        1.0F, 0.7F);
            }
        });
    }

    private static Vec3 ground(ServerLevel level, Vec3 at) {
        BlockPos feet = BlockPos.containing(at);
        for (int dy = 1; dy >= -3; dy--) {
            BlockPos pos = feet.above(dy);
            BlockPos below = pos.below();
            if (!level.getBlockState(pos).isCollisionShapeFullBlock(level, pos)
                    && !level.getBlockState(pos.above()).isCollisionShapeFullBlock(level, pos.above())
                    && level.getBlockState(below).isCollisionShapeFullBlock(level, below)) {
                return new Vec3(at.x, pos.getY(), at.z);
            }
        }
        return null;
    }
}
