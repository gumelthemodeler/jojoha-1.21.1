package org.gumel.jojoha.stand.skill.moves;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A player's own barrage: a run of quick blows landed over the next second or so.
 *
 * <p>Spread across ticks rather than applied at once because a flurry that resolves in a single
 * frame is indistinguishable from one big hit - the point of a barrage is the rhythm. It also means
 * the target can walk out of it, which is what makes the shorter bare-handed reach matter.
 *
 * <p>Aimed at the space in front of the player rather than at one victim, matching how the Stand's
 * own barrage works: each blow sweeps whatever is currently standing there, so stepping into a
 * flurry gets you hit and stepping out of one stops it.
 */
final class PlayerFlurry {
    private static final List<Flurry> ACTIVE = new ArrayList<>();

    private PlayerFlurry() {
    }

    static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    static void begin(ServerPlayer player, int hits, int interval, float damage, double reach) {
        ACTIVE.add(new Flurry(player, hits, interval, interval, damage, reach));
    }

    private static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }

        Iterator<Flurry> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().step()) {
                iterator.remove();
            }
        }
    }

    private static final class Flurry {
        private final ServerPlayer player;
        private final int interval;
        private final float damage;
        private final double reach;

        private int hitsLeft;
        private int untilNext;

        private Flurry(ServerPlayer player, int hitsLeft, int interval,
                       int untilNext, float damage, double reach) {
            this.player = player;
            this.hitsLeft = hitsLeft;
            this.interval = interval;
            this.untilNext = untilNext;
            this.damage = damage;
            this.reach = reach;
        }

        /** @return true when this flurry is finished and should be dropped. */
        private boolean step() {
            if (hitsLeft <= 0 || !player.isAlive()) {
                return true;
            }

            if (--untilNext > 0) {
                return false;
            }

            untilNext = interval;
            hitsLeft--;

            Vec3 look = player.getLookAngle();
            Vec3 centre = player.position().add(0, player.getBbHeight() * 0.5, 0)
                    .add(look.scale(reach * 0.5));
            AABB area = new AABB(centre, centre).inflate(reach * 0.5);

            for (LivingEntity victim : player.serverLevel().getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != player && entity.isAlive() && !(entity instanceof StandEntity))) {

                Vec3 toVictim = victim.position().subtract(player.position());
                if (toVictim.lengthSqr() > 1.0E-4 && look.dot(toVictim.normalize()) < 0.35) {
                    continue;
                }

                victim.hurt(player.serverLevel().damageSources().playerAttack(player), damage);
                // Cleared so the rapid hits all land instead of most being swallowed by the
                // invulnerability window a normal attack leaves behind.
                victim.invulnerableTime = 0;
            }

            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_WEAK,
                    SoundSource.PLAYERS, 0.7F, 1.5F);
            return hitsLeft <= 0;
        }
    }
}
