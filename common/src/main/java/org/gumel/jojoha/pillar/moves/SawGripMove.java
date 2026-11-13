package org.gumel.jojoha.pillar.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.pillar.PillarAim;
import org.gumel.jojoha.pillar.PillarFx;
import org.gumel.jojoha.pillar.PillarFxKind;
import org.gumel.jojoha.pillar.PillarMove;
import org.gumel.jojoha.pillar.PillarPoses;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.vampire.VampireFx;

public final class SawGripMove extends PillarMove {
    public static final ResourceLocation ID = id("saw_grip");
    public static final SawGripMove INSTANCE = new SawGripMove();

    public static final double REACH = 3.0;
    private static final double RADIUS = 0.9;
    private static final double SLACK = 1.5;
    public static final int HITS = 3;
    public static final float DAMAGE = 3.0F;
    public static final int EVERY = 11;
    public static final int HOLD_TICKS = 30;
    private static final double HOLD_DISTANCE = 1.1;
    private static final double RELEASE_PUSH = 0.6;

    private SawGripMove() {
        super("saw_grip", 160, 20F);
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        String busy = super.unavailable(player, data);
        if (busy != null) {
            return busy;
        }
        return PillarAim.first(player, REACH, RADIUS) == null ? "message.jojoha.saw_grip.nothing" : null;
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        LivingEntity target = PillarAim.first(player, REACH, RADIUS);
        if (target == null) {
            return;
        }
        MovementFx.pose(player, PillarPoses.SAW_GRIP);
        PillarFx.charge(player, PillarFx.LIGHT_STYLE, PillarPoses.SAW_GRIP_CONTACT_TICKS);
        StandBeat.after(PillarPoses.SAW_GRIP_CONTACT_TICKS, player, target, SawGripMove::grip);
    }

    private static void grip(ServerPlayer player, LivingEntity target) {
        if (player.distanceTo(target) > REACH + SLACK) {
            return;
        }
        ServerLevel level = player.serverLevel();
        boolean held = !PillarAim.boss(target);
        NetworkHandler.sendPillarFx(level, PillarFxKind.SAW, player.getId(), target.getId(), HOLD_TICKS);
        StandBeat.during(1, HOLD_TICKS, player, tick -> {
            if (!target.isAlive()) {
                return;
            }
            if (held) {
                Vec3 look = player.getLookAngle();
                Vec3 flat = new Vec3(look.x, 0, look.z);
                Vec3 ahead = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(HOLD_DISTANCE);
                Vec3 spot = player.position().add(ahead);
                target.teleportTo(spot.x, player.getY(), spot.z);
                target.setDeltaMovement(Vec3.ZERO);
                target.fallDistance = 0F;
            }
            int hit = (tick - 1) / EVERY;
            if ((tick - 1) % EVERY == 0 && hit < HITS) {
                saw(level, player, target, hit == HITS - 1);
            }
            if (tick == HOLD_TICKS && held) {
                Vec3 away = target.position().subtract(player.position());
                Vec3 push = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(RELEASE_PUSH);
                target.setDeltaMovement(push.add(0, 0.2, 0));
                target.hurtMarked = true;
            }
        });
    }

    private static void saw(ServerLevel level, ServerPlayer player, LivingEntity target, boolean last) {
        target.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, target, DAMAGE));
        if (last) {
            Bleed.apply(target);
        }
        Vec3 at = target.getBoundingBox().getCenter();
        Impacts.landed(level, target, at.subtract(player.position()), (last ? ImpactSpec.MEDIUM : ImpactSpec.LIGHT).silent(),
                Impacts.BLOOD);
        PillarFx.spray(level, PillarFx.glint(), at, 8, 0.3, 0.3);
        PillarFx.spray(level, PillarFx.flesh(), at, 3, 0.2, 0.15);
        VampireFx.blood(level, at.x, at.y, at.z, 6, 0.2, 0.08);
        level.playSound(null, target.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.8F, 1.3F);
    }
}
