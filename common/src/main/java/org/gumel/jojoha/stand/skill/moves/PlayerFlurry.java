package org.gumel.jojoha.stand.skill.moves;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.BarrageBank;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.combat.Hitbox;
import org.gumel.jojoha.combat.JojohaUndead;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.stand.StandEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class PlayerFlurry {
    private static final List<Flurry> ACTIVE = new ArrayList<>();

    private static final double LANE_HALF_WIDTH = 0.8;

    private PlayerFlurry() {
    }

    static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    public interface HitEffect {
        void at(ServerLevel level, LivingEntity victim);

        default void last(ServerLevel level, LivingEntity victim) {
        }
    }

    public static void begin(ServerPlayer player, int hits, int interval, float damage, double reach) {
        begin(player, hits, interval, damage, reach, 1F, null);
    }

    public static void begin(ServerPlayer player, int hits, int interval, float damage, double reach,
                             float undeadMultiplier, HitEffect onHit) {
        begin(player, hits, interval, damage, reach, undeadMultiplier, onHit, -1, false,
                DamageScale.Kind.STAND);
    }

    public static void begin(ServerPlayer player, int hits, int interval, float damage, double reach,
                             float undeadMultiplier, HitEffect onHit, int colour, boolean finale,
                             DamageScale.Kind kind) {
        ACTIVE.add(new Flurry(player, hits, interval, interval, damage, reach,
                undeadMultiplier, onHit, colour, finale, kind));
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
        private final float undeadMultiplier;
        private final HitEffect onHit;
        private final int colour;
        private final boolean finale;
        private final DamageScale.Kind kind;

        private int hitsLeft;
        private int untilNext;

        private Flurry(ServerPlayer player, int hitsLeft, int interval,
                       int untilNext, float damage, double reach,
                       float undeadMultiplier, HitEffect onHit, int colour, boolean finale,
                       DamageScale.Kind kind) {
            this.player = player;
            this.kind = kind;
            this.colour = colour;
            this.finale = finale;
            this.hitsLeft = hitsLeft;
            this.interval = interval;
            this.untilNext = untilNext;
            this.damage = damage;
            this.reach = reach;
            this.undeadMultiplier = undeadMultiplier;
            this.onHit = onHit;
        }

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
                if (!Hitbox.inLane(player.getEyePosition(), look, reach, LANE_HALF_WIDTH,
                        victim)) {
                    continue;
                }

                float dealt = undeadMultiplier != 1F
                        && JojohaUndead.is(victim)
                        ? damage * undeadMultiplier : damage;
                BarrageBank.during(player, () -> victim.hurt(
                        player.serverLevel().damageSources().playerAttack(player),
                        DamageScale.scale(kind, player, victim, dealt)));

                Impacts.landed(player.serverLevel(), victim, look,
                        finale && hitsLeft == 0
                                ? ImpactSpec.FINISHER
                                : ImpactSpec.LIGHT,
                        colour >= 0 ? colour : Impacts.colourOf(player));

                if (onHit != null) {
                    onHit.at(player.serverLevel(), victim);
                    if (hitsLeft == 0) {
                        onHit.last(player.serverLevel(), victim);
                    }
                }

                HitWindow.open(victim);
            }

            player.swing(InteractionHand.MAIN_HAND, true);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_WEAK,
                    SoundSource.PLAYERS, 0.7F, 1.5F);
            return hitsLeft <= 0;
        }
    }
}
