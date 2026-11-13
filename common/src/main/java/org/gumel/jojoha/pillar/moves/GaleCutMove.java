package org.gumel.jojoha.pillar.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
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

import java.util.List;

public final class GaleCutMove extends PillarMove {
    public static final ResourceLocation ID = id("gale_cut");
    public static final GaleCutMove INSTANCE = new GaleCutMove();

    public static final double RANGE = 14.0;
    public static final float DAMAGE = 4.0F;
    private static final double RADIUS = 0.6;
    private static final float BURST = 1.2F;

    private GaleCutMove() {
        super("gale_cut", 50, 12F);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        MovementFx.pose(player, PillarPoses.GALE_CUT);
        PillarFx.charge(player, PillarFx.WIND_STYLE, PillarPoses.GALE_CUT_RELEASE_TICKS);
        StandBeat.at(PillarPoses.GALE_CUT_RELEASE_TICKS, player, () -> cut(player));
    }

    private static void cut(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 from = player.getEyePosition();
        Vec3 heading = player.getLookAngle();
        Vec3 end = from.add(heading.scale(RANGE));
        HitResult wall = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                player));
        Vec3 stop = wall.getType() == HitResult.Type.MISS ? end : wall.getLocation();

        List<LivingEntity> line = PillarAim.along(player, from, heading, from.distanceTo(stop), RADIUS);
        if (!line.isEmpty()) {
            LivingEntity target = line.get(0);
            stop = target.getBoundingBox().getCenter();
            target.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, target, DAMAGE));
            Impacts.landed(level, target, heading, ImpactSpec.LIGHT.silent(), Impacts.FLESH);
        }

        Vec3 start = from.add(heading.scale(0.8));
        NetworkHandler.sendPillarFx(level, PillarFxKind.GALE, player.getId(), 0, 0, start, stop.subtract(start));
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.9F, 1.2F);
        level.explode(null, null, AbstractWindCharge.EXPLOSION_DAMAGE_CALCULATOR, stop.x, stop.y, stop.z, BURST,
                false, Level.ExplosionInteraction.TRIGGER, PillarFx.gust(), PillarFx.gust(),
                SoundEvents.WIND_CHARGE_BURST);
        PillarFx.spray(level, PillarFx.gust(), stop, 10, 0.6, 0.15);
    }
}
