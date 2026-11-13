package org.gumel.jojoha.hamon.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.accessory.Charms;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonCombat;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RippleRebukeMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ripple_rebuke");
    public static final RippleRebukeMove INSTANCE = new RippleRebukeMove();

    public static final int WINDOW_TICKS = 20;
    public static final float DAMAGE = 5.0F;
    public static final float UNDEAD_DAMAGE = 8.0F;
    public static final int SLOW_TICKS = 40;
    private static final double SHOVE = 0.8;
    private static final double LIFT = 0.3;

    private static final int COOLDOWN_TICKS = 120;
    private static final float ENERGY_COST = 16F;

    private static final Map<UUID, Long> GUARDING_UNTIL = new HashMap<>();

    private RippleRebukeMove() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data) {
        int window = Charms.parryWindow(player, WINDOW_TICKS);
        GUARDING_UNTIL.put(player.getUUID(), player.level().getGameTime() + window);
        MovementFx.pose(player, MovementFx.GUARD);
        NetworkHandler.sendFirstPersonPose(player, "guard", window);
        Vec3 hand = HamonFx.mainHand(player);
        HamonFx.fineSpark(player.serverLevel(), hand.x, hand.y, hand.z, 6, 0.2, 0.02);
        return true;
    }

    public static boolean guarding(ServerPlayer player) {
        Long until = GUARDING_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (player.level().getGameTime() > until) {
            GUARDING_UNTIL.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static boolean rebuke(ServerPlayer player, DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity assailant) || source.getDirectEntity() != assailant
                || assailant == player || !guarding(player)) {
            return false;
        }
        GUARDING_UNTIL.remove(player.getUUID());

        ServerLevel level = player.serverLevel();
        float damage = HamonCombat.isUndead(assailant) ? UNDEAD_DAMAGE : DAMAGE;
        assailant.hurt(level.damageSources().playerAttack(player),
                DamageScale.hamon(player, assailant, HamonCombat.scaled(player, damage)));
        assailant.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 1));

        Vec3 away = new Vec3(assailant.getX() - player.getX(), 0.0, assailant.getZ() - player.getZ());
        Vec3 shove = away.lengthSqr() < 1.0E-6 ? Vec3.ZERO : away.normalize().scale(SHOVE);
        assailant.setDeltaMovement(assailant.getDeltaMovement().add(shove.x, LIFT, shove.z));
        assailant.hurtMarked = true;

        Vec3 at = assailant.position().add(0.0, assailant.getBbHeight() * 0.5, 0.0);
        HamonFx.spark(level, at.x, at.y, at.z, 10, 0.3, 0.06);
        HamonFx.ring(level, at.x, at.y, at.z, 1.3, 2);
        Impacts.landed(level, assailant, away,
                ImpactSpec.PARRY, Impacts.HAMON);
        level.playSound(null, player.blockPosition(), ModSounds.HAMON_CRASH.get(), SoundSource.PLAYERS,
                0.4F, 1.8F);
        HamonFx.sound(level, player, 0.9F, 1.2F);
        return true;
    }

    public static void forget(UUID player) {
        GUARDING_UNTIL.remove(player);
    }
}
