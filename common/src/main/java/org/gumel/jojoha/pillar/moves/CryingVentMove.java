package org.gumel.jojoha.pillar.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.pillar.PillarFx;
import org.gumel.jojoha.pillar.PillarFxKind;
import org.gumel.jojoha.pillar.PillarMove;
import org.gumel.jojoha.pillar.PillarPoses;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.vampire.VampireFx;

import java.util.List;

public final class CryingVentMove extends PillarMove {
    public static final ResourceLocation ID = id("crying_vent");
    public static final CryingVentMove INSTANCE = new CryingVentMove();

    public static final int ROOT_TICKS = 30;
    public static final float HEAL = 4.0F;
    private static final int ROOT_SLOWNESS = 9;

    private CryingVentMove() {
        super("crying_vent", 400, 20F);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        ServerLevel level = player.serverLevel();
        MovementFx.pose(player, PillarPoses.CRYING_VENT);
        PillarFx.charge(player, PillarFx.HEAT_STYLE, ROOT_TICKS);
        NetworkHandler.sendFirstPersonPose(player, MovementFx.HEAL);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ROOT_TICKS, ROOT_SLOWNESS,
                false, false, false));
        level.playSound(null, player.blockPosition(), SoundEvents.GHAST_HURT, SoundSource.PLAYERS, 0.7F, 0.5F);

        StandBeat.during(1, PillarPoses.CRYING_VENT_RELEASE_TICKS - 1, player, tick -> {
            if (tick % 2 != 0) {
                return;
            }
            for (int side = -1; side <= 1; side += 2) {
                Vec3 eye = VampireFx.eye(player, side);
                VampireFx.blood(level, eye.x, eye.y - 0.05, eye.z, 1, 0.01, 0.0);
            }
            PillarFx.fling(level, PillarFx.steam(), new Vec3(player.getX(), player.getEyeY() + 0.3, player.getZ()),
                    new Vec3(0, 0.05, 0));
        });
        StandBeat.at(PillarPoses.CRYING_VENT_RELEASE_TICKS, player, () -> vent(player));
    }

    private static void vent(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        NetworkHandler.clearFirstPersonPose(player);

        List<MobEffectInstance> active = List.copyOf(player.getActiveEffects());
        for (MobEffectInstance effect : active) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                player.removeEffect(effect.getEffect());
            }
        }
        player.clearFire();
        player.heal(HEAL);
        NetworkHandler.sendPillarFx(level, PillarFxKind.VENT, player.getId(), 0, 0);

        Vec3 top = new Vec3(player.getX(), player.getEyeY() + 0.2, player.getZ());
        for (int jet = 0; jet < 12; jet++) {
            double angle = jet * Math.PI * 2 / 12;
            PillarFx.fling(level, PillarFx.steam(), top, new Vec3(Math.cos(angle) * 0.25, 0.3, Math.sin(angle) * 0.25));
        }
        PillarFx.spray(level, PillarFx.steam(), player.position().add(0, 1.0, 0), 10, 0.4, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.8F);
    }
}
