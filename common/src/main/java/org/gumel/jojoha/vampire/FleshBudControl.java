package org.gumel.jojoha.vampire;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class FleshBudControl {
    private static final double RANGE = 32.0;

    private static final int SEEK_EVERY = 10;

    private FleshBudControl() {
    }

    public static void tick(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        long now = level.getGameTime();

        List<Integer> buds = VampireState.budsOf(caster, now);
        if (buds.isEmpty()) {
            return;
        }

        List<Mob> thralls = new ArrayList<>(buds.size());
        List<ServerPlayer> puppets = new ArrayList<>();
        for (int id : buds) {
            Entity budded = level.getEntity(id);
            if (budded instanceof Mob mob && mob.isAlive() && mob.distanceToSqr(caster) < RANGE * RANGE) {
                thralls.add(mob);
            } else if (budded instanceof ServerPlayer player && player != caster && player.isAlive()
                    && !player.isSpectator() && player.distanceToSqr(caster) < RANGE * RANGE) {
                puppets.add(player);
            }
        }
        if (thralls.isEmpty() && puppets.isEmpty()) {
            return;
        }

        LivingEntity marked = marked(caster, now);
        for (ServerPlayer puppet : puppets) {
            drive(puppet, caster, marked, now);
        }

        for (Mob thrall : thralls) {
            if (thrall.getTarget() == caster) {
                thrall.setTarget(null);
            }
        }

        if (caster.tickCount % SEEK_EVERY != 0) {
            return;
        }

        for (Mob thrall : thralls) {
            LivingEntity threat = marked != null && marked != thrall ? marked : threatTo(level, caster, thrall, thralls);
            if (threat != null) {
                thrall.setTarget(threat);
            }
        }
    }

    private static final int MARK_TICKS = 200;
    private static final double REACH = 3.0;
    private static final double PULL = 0.42;
    private static final int STEER_EVERY = 3;
    private static final int SWING_EVERY = 12;

    private static LivingEntity marked(ServerPlayer caster, long now) {
        LivingEntity hit = caster.getLastHurtMob();
        if (hit != null && hit.isAlive() && caster.tickCount - caster.getLastHurtMobTimestamp() <= MARK_TICKS
                && hit.distanceToSqr(caster) < RANGE * RANGE * 4) {
            return hit;
        }
        LivingEntity attacker = caster.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && attacker != caster
                && attacker.distanceToSqr(caster) < RANGE * RANGE) {
            return attacker;
        }
        return null;
    }

    private static void drive(ServerPlayer puppet, ServerPlayer caster, LivingEntity marked, long now) {
        if (marked == null || marked == puppet || marked == caster || !marked.isAlive()) {
            return;
        }
        if (now % STEER_EVERY == 0) {
            puppet.connection.send(new ClientboundPlayerLookAtPacket(
                    EntityAnchorArgument.Anchor.EYES, marked,
                    EntityAnchorArgument.Anchor.EYES));
            Vec3 toward = marked.position().subtract(puppet.position());
            Vec3 flat = new Vec3(toward.x, 0.0, toward.z);
            if (flat.lengthSqr() > REACH * REACH * 0.6) {
                Vec3 push = flat.normalize().scale(PULL);
                puppet.setDeltaMovement(push.x, puppet.getDeltaMovement().y, push.z);
                puppet.hurtMarked = true;
            }
        }
        if (now % SWING_EVERY == 0 && puppet.distanceToSqr(marked) <= REACH * REACH && puppet.hasLineOfSight(marked)) {
            puppet.lookAt(EntityAnchorArgument.Anchor.EYES, marked.getEyePosition());
            puppet.swing(InteractionHand.MAIN_HAND, true);
            puppet.attack(marked);
            puppet.resetAttackStrengthTicker();
        }
    }

    private static LivingEntity threatTo(ServerLevel level, ServerPlayer caster, Mob thrall,
                                         List<Mob> thralls) {
        LivingEntity best = null;
        double nearest = Double.MAX_VALUE;

        for (Mob candidate : level.getEntitiesOfClass(Mob.class,
                caster.getBoundingBox().inflate(RANGE),
                mob -> mob.isAlive() && mob.getTarget() == caster)) {
            if (candidate == thrall || thralls.contains(candidate)) {
                continue;
            }

            double distance = candidate.distanceToSqr(thrall);
            if (distance < nearest) {
                nearest = distance;
                best = candidate;
            }
        }

        if (best == null) {
            return null;
        }

        Entity current = thrall.getTarget();
        boolean busy = current instanceof LivingEntity && current != caster && !current.isRemoved();
        if (busy && current.distanceToSqr(thrall) <= nearest) {
            return null;
        }
        return best;
    }
}
