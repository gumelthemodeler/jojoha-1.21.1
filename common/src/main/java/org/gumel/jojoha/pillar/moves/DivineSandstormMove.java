package org.gumel.jojoha.pillar.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

public final class DivineSandstormMove extends PillarMove {
    public static final ResourceLocation ID = id("divine_sandstorm");
    public static final DivineSandstormMove INSTANCE = new DivineSandstormMove();

    public static final double RANGE = 8.0;
    public static final double RADIUS = 1.3;
    public static final float DAMAGE = 7.0F;
    private static final double PUSH = 0.9;
    private static final double LIFT = 0.25;
    private static final int STORM_TICKS = 6;

    private DivineSandstormMove() {
        super("divine_sandstorm", 160, 28F);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        MovementFx.pose(player, PillarPoses.DIVINE_SANDSTORM);
        PillarFx.charge(player, PillarFx.WIND_STYLE, PillarPoses.DIVINE_SANDSTORM_CONTACT_TICKS);
        NetworkHandler.sendFirstPersonPose(player, MovementFx.ZOOM_PUNCH);
        StandBeat.at(PillarPoses.DIVINE_SANDSTORM_CONTACT_TICKS, player, () -> storm(player));
    }

    private static void storm(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 heading = player.getLookAngle();
        Vec3 from = player.getEyePosition().add(0, -0.3, 0).add(heading.scale(0.6));

        for (LivingEntity target : PillarAim.along(player, from, heading, RANGE, RADIUS)) {
            target.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, target, DAMAGE));
            Vec3 across = new Vec3(-heading.z, 0, heading.x).normalize()
                    .scale(level.getRandom().nextBoolean() ? 0.3 : -0.3);
            target.setDeltaMovement(heading.scale(PUSH).add(across).add(0, LIFT, 0));
            target.hurtMarked = true;
            Impacts.landed(level, target, heading, ImpactSpec.MEDIUM.silent(), Impacts.FLESH);
            PillarFx.spray(level, PillarFx.sand(), target.getBoundingBox().getCenter(), 12, 0.3, 0.2);
        }

        Vec3 to = from.add(heading.scale(RANGE));
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, new AABB(from, to).inflate(RADIUS))) {
            if (shot.getOwner() == player) {
                continue;
            }
            Vec3 at = shot.position();
            double t = Math.max(0.0, Math.min(RANGE, at.subtract(from).dot(heading)));
            if (from.add(heading.scale(t)).distanceTo(at) <= RADIUS + 0.5) {
                PillarFx.spray(level, PillarFx.gust(), at, 2, 0.1, 0.05);
                shot.discard();
            }
        }

        NetworkHandler.sendPillarFx(level, PillarFxKind.SANDSTORM, player.getId(), 0, STORM_TICKS, from,
                heading.scale(RANGE));
        PillarFx.spray(level, PillarFx.gust(), to, 4, 0.4, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS,
                0.8F, 0.7F);
    }
}
