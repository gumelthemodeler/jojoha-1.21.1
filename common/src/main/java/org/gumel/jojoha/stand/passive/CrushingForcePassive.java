package org.gumel.jojoha.stand.passive;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.StandFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;

public final class CrushingForcePassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "crushing_force");
    public static final CrushingForcePassive INSTANCE = new CrushingForcePassive();

    public static final float HEALTH_THRESHOLD = 0.25F;

    public static final float DAMAGE_MULTIPLIER = 1.25F;

    public static final int EXTRA_GUARD_STRAIN = 0;

    private CrushingForcePassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.crushing_force";
    }

    @Override
    public float onOutgoingDamage(ServerPlayer player, JojohaPlayerData data, LivingEntity target,
                                  float amount) {
        if (player.getHealth() > player.getMaxHealth() * HEALTH_THRESHOLD || !isGuarding(target)) {
            return amount;
        }
        crush(player, target);
        return amount * DAMAGE_MULTIPLIER;
    }

    public static boolean isGuarding(LivingEntity target) {
        if (target.isBlocking()) {
            return true;
        }
        if (target instanceof ServerPlayer victim) {
            return PlayerDataAccess.get(victim).standGuarding;
        }
        return false;
    }

    private static void crush(ServerPlayer player, LivingEntity target) {
        if (target instanceof Player victim && victim.isBlocking()) {
            victim.disableShield();
        }

        if (target instanceof ServerPlayer victim) {
            JojohaPlayerData theirs = PlayerDataAccess.get(victim);
            if (theirs.standGuarding) {
                theirs.blockedHitsSinceBreak += EXTRA_GUARD_STRAIN;
                PlayerDataAccess.set(victim, theirs);
            }
        }

        ServerLevel level = player.serverLevel();
        Vec3 at = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z,
                14, 0.3, 0.3, 0.3, 0.3);
        StandFx.ring(level, at, 1.4, 1);
        level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                0.5F, 0.6F);
    }
}
