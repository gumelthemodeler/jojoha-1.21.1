package org.gumel.jojoha.pillar.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.Burning;
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

public final class VesselGrabMove extends PillarMove {
    public static final ResourceLocation ID = id("vessel_grab");
    public static final VesselGrabMove INSTANCE = new VesselGrabMove();

    public static final double REACH = 8.0;
    private static final double RADIUS = 0.9;
    private static final double SLACK = 2.0;
    public static final float DAMAGE = 2.0F;
    private static final int GRIP_TICKS = 4;
    private static final double PULL = 1.2;
    private static final double PULL_PER_BLOCK = 0.25;
    private static final double LIFT = 0.25;

    private VesselGrabMove() {
        super("vessel_grab", 100, 14F);
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        String busy = super.unavailable(player, data);
        if (busy != null) {
            return busy;
        }
        return PillarAim.first(player, REACH, RADIUS) == null ? "message.jojoha.vessel_grab.nothing" : null;
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        LivingEntity target = PillarAim.first(player, REACH, RADIUS);
        if (target == null) {
            return;
        }
        MovementFx.pose(player, PillarPoses.VESSEL_GRAB);
        PillarFx.charge(player, PillarFx.HEAT_STYLE, PillarPoses.VESSEL_GRAB_CONTACT_TICKS);
        StandBeat.after(PillarPoses.VESSEL_GRAB_CONTACT_TICKS, player, target, VesselGrabMove::grab);
    }

    private static void grab(ServerPlayer player, LivingEntity target) {
        if (player.distanceTo(target) > REACH + SLACK) {
            return;
        }
        ServerLevel level = player.serverLevel();
        NetworkHandler.sendPillarFx(level, PillarFxKind.VESSEL_GRAB, player.getId(), target.getId(), GRIP_TICKS);
        NetworkHandler.sendPillarFx(level, PillarFxKind.BOIL, player.getId(), target.getId(), GRIP_TICKS + 10);
        PillarFx.spray(level, PillarFx.steam(), target.getBoundingBox().getCenter(), 3, 0.2, 0.03);

        target.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, target, DAMAGE));
        Burning.apply(target);
        if (!PillarAim.boss(target)) {
            Vec3 toward = player.position().subtract(target.position());
            double distance = toward.length();
            if (distance > 1.0E-3) {
                Vec3 pull = toward.normalize().scale(Math.min(PULL, distance * PULL_PER_BLOCK));
                target.setDeltaMovement(pull.add(0, LIFT, 0));
                target.hurtMarked = true;
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS,
                1.0F, 0.6F);
        level.playSound(null, target.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.4F);
    }
}
