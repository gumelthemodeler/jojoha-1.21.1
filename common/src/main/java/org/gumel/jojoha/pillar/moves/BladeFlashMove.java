package org.gumel.jojoha.pillar.moves;

import org.gumel.jojoha.pillar.PillarFxKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.pillar.PillarAim;
import org.gumel.jojoha.pillar.PillarFx;
import org.gumel.jojoha.pillar.PillarMove;
import org.gumel.jojoha.pillar.PillarPoses;
import org.gumel.jojoha.stand.skill.StandBeat;

public final class BladeFlashMove extends PillarMove {
    public static final ResourceLocation ID = id("blade_flash");
    public static final BladeFlashMove INSTANCE = new BladeFlashMove();

    public static final double RANGE = 10.0;
    public static final double FACING_DOT = 0.5;
    public static final int BLIND_TICKS = 60;

    private BladeFlashMove() {
        super("blade_flash", 200, 16F);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        MovementFx.pose(player, PillarPoses.BLINDING_FLASH);
        PillarFx.charge(player, PillarFx.LIGHT_STYLE, PillarPoses.BLINDING_FLASH_RELEASE_TICKS);
        NetworkHandler.sendFirstPersonPose(player, MovementFx.PALM);
        StandBeat.at(PillarPoses.BLINDING_FLASH_RELEASE_TICKS, player, () -> flash(player));
    }

    private static void flash(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 at = PillarAim.hand(player);
        NetworkHandler.sendPillarFx(level, PillarFxKind.FLASH, player.getId(), 0, 0, at, Vec3.ZERO);
        PillarFx.spray(level, PillarFx.glint(), at, 16, 0.3, 0.25);

        Vec3 eye = player.getEyePosition();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RANGE),
                entity -> PillarAim.struckable(player, entity))) {
            Vec3 toMe = eye.subtract(target.getEyePosition());
            if (toMe.length() > RANGE || PillarAim.boss(target) || !player.hasLineOfSight(target)
                    || target.getLookAngle().dot(toMe.normalize()) < FACING_DOT) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_TICKS, 0), player);
            if (target instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BLIND_TICKS, 1), player);
            }
        }

        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS,
                1.0F, 1.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
    }
}
