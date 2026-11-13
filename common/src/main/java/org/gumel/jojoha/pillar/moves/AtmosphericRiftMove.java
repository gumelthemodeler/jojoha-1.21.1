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

public final class AtmosphericRiftMove extends PillarMove {
    public static final ResourceLocation ID = id("atmospheric_rift");
    public static final AtmosphericRiftMove INSTANCE = new AtmosphericRiftMove();

    public static final double RANGE = 10.0;
    public static final double DOT = 0.55;
    public static final float DAMAGE = 10.0F;
    public static final float HEALTH_COST = 6.0F;
    private static final double KNOCK = 0.6;
    private static final int BLADES = 9;
    private static final double FAN_DEGREES = 70.0;

    private AtmosphericRiftMove() {
        super("atmospheric_rift", 1200, 45F);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        MovementFx.pose(player, PillarPoses.ATMOSPHERIC_RIFT);
        PillarFx.charge(player, PillarFx.WIND_STYLE, PillarPoses.ATMOSPHERIC_RIFT_RELEASE_TICKS);
        NetworkHandler.sendFirstPersonPose(player, MovementFx.GATHER);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_INHALE, SoundSource.PLAYERS, 1.0F, 0.5F);
        StandBeat.during(1, PillarPoses.ATMOSPHERIC_RIFT_RELEASE_TICKS - 1, player, tick -> {
            if (tick % 2 != 0) {
                return;
            }
            double angle = tick * 0.9;
            Vec3 chest = player.position().add(0, 1.1, 0);
            Vec3 out = new Vec3(Math.cos(angle) * 2.2, Math.sin(tick * 0.5) * 0.6, Math.sin(angle) * 2.2);
            PillarFx.fling(level, PillarFx.gust(), chest.add(out), out.scale(-0.12));
            PillarFx.fling(level, PillarFx.sand(), chest.add(out.scale(1.2)), out.scale(-0.15));
        });
        StandBeat.at(PillarPoses.ATMOSPHERIC_RIFT_RELEASE_TICKS, player, () -> rift(player));
    }

    private static void rift(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        player.setHealth(Math.max(1F, player.getHealth() - HEALTH_COST));
        Vec3 chest = player.position().add(0, 1.1, 0);
        VampireFx.blood(level, chest.x, chest.y, chest.z, 24, 0.35, 0.2);

        Vec3 look = player.getLookAngle();
        for (LivingEntity target : PillarAim.cone(player, RANGE, DOT, 0)) {
            target.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, target, DAMAGE));
            Bleed.apply(target);
            Vec3 away = target.position().subtract(player.position()).normalize();
            target.setDeltaMovement(away.scale(KNOCK).add(0, 0.2, 0));
            target.hurtMarked = true;
            Impacts.landed(level, target, away, ImpactSpec.MEDIUM.silent(), Impacts.BLOOD);
        }

        Vec3 from = player.getEyePosition().add(0, -0.3, 0);
        NetworkHandler.sendPillarFx(level, PillarFxKind.RIFT, player.getId(), BLADES, (int) FAN_DEGREES, from,
                look.scale(RANGE));
        PillarFx.spray(level, PillarFx.flesh(), chest, 10, 0.3, 0.2);
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 1.2F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                1.0F, 0.6F);
    }
}
