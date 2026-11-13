package org.gumel.jojoha.fortune;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.item.ThrownDagger;
import org.gumel.jojoha.network.packet.TrickTellPacket;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.skill.Harmless;
import org.gumel.jojoha.stand.hierophant.EmeraldSplash;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Trickster {
    public static final double TELL_RANGE = 24.0;

    public static final double TELL_AIM = 0.85;

    public static final int TELL_TICKS = 16;

    public static final int THROW_STUN_TICKS = 10;

    public static final int THROW_STUN_AGAIN_TICKS = 40;

    public static final int PARRY_BONUS_TICKS = 1;

    public static final int FIGHT_GAP_TICKS = 200;

    public static final int OPENING_HITS = 3;

    public static final float OPENING_SCALE = 0.8F;

    private static final class Fight {
        long last;
        int opening;
        boolean tricked;
        boolean warned;
    }

    private static final Map<UUID, Fight> FIGHTS = new HashMap<>();

    private static final Map<UUID, Long> STUNNED = new HashMap<>();

    private Trickster() {
    }

    private static Fight fight(ServerPlayer player) {
        long now = player.level().getGameTime();
        Fight fight = FIGHTS.computeIfAbsent(player.getUUID(), id -> new Fight());
        if (now - fight.last > FIGHT_GAP_TICKS) {
            fight.opening = 0;
            fight.tricked = false;
            fight.warned = false;
        }
        fight.last = now;
        return fight;
    }

    private static boolean is(ServerPlayer player) {
        return Fortunes.on() && Fortune.has(PlayerDataAccess.get(player), Fortune.TRICKSTER);
    }

    public static void cast(ServerPlayer caster, ResourceLocation move) {
        if (move == null || Harmless.is(move) || !Fortunes.on()) {
            return;
        }
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        for (ServerPlayer reader : caster.serverLevel().players()) {
            if (reader == caster || reader.distanceToSqr(caster) > TELL_RANGE * TELL_RANGE || !is(reader)) {
                continue;
            }
            Vec3 toward = reader.getBoundingBox().getCenter().subtract(eye).normalize();
            if (toward.dot(look) < TELL_AIM) {
                continue;
            }
            NetworkManager.sendToPlayer(reader, new TrickTellPacket(caster.getId(), move, TELL_TICKS));
            Fight fight = fight(reader);
            if (!fight.warned) {
                fight.warned = true;
                Fortunes.whisper(reader, Component.translatable("message.jojoha.fortune.trickster.next_line"));
            }
        }
    }

    public static int parryBonus(ServerPlayer player) {
        return is(player) ? PARRY_BONUS_TICKS : 0;
    }

    public static void tricked(ServerPlayer player) {
        if (is(player)) {
            fight(player).tricked = true;
        }
    }

    static float dealt(ServerPlayer attacker, JojohaPlayerData data, DamageSource source, float amount) {
        if (!Fortune.has(data, Fortune.TRICKSTER) || source.getDirectEntity() != attacker) {
            return amount;
        }
        Fight fight = fight(attacker);
        if (fight.tricked || fight.opening >= OPENING_HITS) {
            return amount;
        }
        fight.opening++;
        return amount * OPENING_SCALE;
    }

    public static void landed(Projectile projectile, HitResult result) {
        if (projectile.level().isClientSide() || !(result instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity victim)
                || !(projectile.getOwner() instanceof ServerPlayer thrower) || victim == thrower
                || !thrown(projectile) || !is(thrower)) {
            return;
        }
        tricked(thrower);
        long now = thrower.level().getGameTime();
        Long last = STUNNED.get(victim.getUUID());
        if (last != null && now - last < THROW_STUN_AGAIN_TICKS) {
            return;
        }
        STUNNED.put(victim.getUUID(), now);
        STUNNED.values().removeIf(at -> now - at > THROW_STUN_AGAIN_TICKS);
        victim.addEffect(new MobEffectInstance(ModEffects.stun(), THROW_STUN_TICKS, Stun.TIER_ONE,
                false, false, true));
    }

    private static boolean thrown(Projectile projectile) {
        if (projectile instanceof EmeraldSplash || projectile instanceof ThrownEnderpearl) {
            return false;
        }
        return projectile instanceof ThrowableItemProjectile || projectile instanceof ThrownTrident
                || projectile instanceof ThrownDagger;
    }

    static void forget(Player player) {
        FIGHTS.remove(player.getUUID());
    }
}
