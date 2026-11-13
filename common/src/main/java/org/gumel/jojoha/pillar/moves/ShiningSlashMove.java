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

public final class ShiningSlashMove extends PillarMove {
    public static final ResourceLocation ID = id("shining_slash");
    public static final ShiningSlashMove INSTANCE = new ShiningSlashMove();

    public static final double DISTANCE = 6.0;
    public static final float DAMAGE = 8.0F;
    private static final double LAUNCH = 1.6;
    private static final double LIFT = 0.15;
    private static final double HIT_RADIUS = 1.1;
    private static final int AFTERIMAGE_TICKS = 10;
    private static final int GLEAM_ARGB = 0xFFFFF4C8;

    private ShiningSlashMove() {
        super("shining_slash", 140, 22F);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        MovementFx.pose(player, PillarPoses.SHINING_SLASH);
        PillarFx.charge(player, PillarFx.LIGHT_STYLE, PillarPoses.SHINING_SLASH_CONTACT_TICKS);
        StandBeat.at(PillarPoses.SHINING_SLASH_CONTACT_TICKS, player, () -> slash(player));
    }

    private static void slash(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        Vec3 look = player.getLookAngle();
        Vec3 heading = new Vec3(look.x, Math.min(look.y, 0.2), look.z).normalize();
        Vec3 from = player.position().add(0, 1.0, 0);

        player.setDeltaMovement(heading.scale(LAUNCH).add(0, LIFT, 0));
        player.hurtMarked = true;
        NetworkHandler.broadcastAfterimages(player, GLEAM_ARGB, AFTERIMAGE_TICKS);

        for (LivingEntity target : PillarAim.along(player, from, heading, DISTANCE, HIT_RADIUS)) {
            target.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, target, DAMAGE));
            Bleed.apply(target);
            Impacts.landed(level, target, heading, ImpactSpec.MEDIUM.silent(), Impacts.BLOOD);
        }

        NetworkHandler.sendPillarFx(level, PillarFxKind.SLASH, player.getId(), 0, 0, from, heading.scale(DISTANCE));
        PillarFx.spray(level, PillarFx.glint(), from.add(heading.scale(DISTANCE * 0.5)), 10, 1.0, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.8F, 1.3F);
    }
}
