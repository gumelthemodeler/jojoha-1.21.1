package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.Exposed;

final class Whiff {
    private static final double AHEAD = 1.6;

    private Whiff() {
    }

    static void body(ServerPlayer player) {
        swept(player, 0.7F);
        Exposed.open(player);
    }

    static void stand(ServerPlayer player) {
        swept(player, 0.55F);
        Exposed.open(player);
    }

    static Vec3 thrownTo(ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 out = eye.add(player.getLookAngle().scale(reach));

        HitResult wall = player.level().clip(
                new ClipContext(eye, out,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, player));
        return wall.getType() == HitResult.Type.MISS ? out : wall.getLocation();
    }

    private static void swept(ServerPlayer player, float pitch) {
        ServerLevel level = player.serverLevel();
        Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(AHEAD));

        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.6F, pitch);
        level.sendParticles(ParticleTypes.POOF,
                at.x, at.y, at.z, 2, 0.12, 0.12, 0.12, 0.01);
    }
}
