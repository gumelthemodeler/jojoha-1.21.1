package org.gumel.jojoha.stand.chariot;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.accessory.Charms;
import org.gumel.jojoha.accessory.Relics;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.stand.skill.moves.DeflectionSkill;
import org.gumel.jojoha.stand.skill.moves.SabreCycleSkill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Chariot {
    public static final int UNARMORED_FLURRY_QUICKER = 1;
    public static final double UNARMORED_SPEED = 0.2;
    public static final double UNARMORED_ATTACK_SPEED = 0.2;
    public static final float UNARMORED_TAKEN = 1.25F;

    public static final int ARMOR_BURST_TICKS = 10;
    private static final double ARMOR_BURST_RADIUS = 3.0;
    private static final double ARMOR_BURST_SHOVE = 0.55;

    public static final int AFTERIMAGE_COUNT = 5;
    public static final int AFTERIMAGE_TICKS = 200;
    public static final int AFTERIMAGE_SPLIT_TICKS = 10;
    public static final float AFTERIMAGE_DODGE = 0.4F;

    public static final int DEFLECT_MOST_TICKS = 60;
    public static final float DEFLECT_ENERGY = 1.5F;
    private static final double DEFLECT_SHOVE = 0.7;
    private static final double DEFLECT_LIFT = 0.2;
    private static final double DEFLECTED_LEAST_SPEED = 0.6;

    public static final int RIPOSTE_WINDOW_TICKS = 12;
    public static final double RIPOSTE_REACH = 8.0;
    private static final double RIPOSTE_BEHIND = 1.2;
    public static final float RIPOSTE_DAMAGE = 6.0F;
    public static final int RIPOSTE_STUN_TICKS = 18;
    private static final int RIPOSTE_LINGER_TICKS = 16;

    public static final int SABRE_FULL_TICKS = 40;
    public static final int SABRE_HELD_MOST_TICKS = 80;

    private static final ResourceLocation UNARMORED =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "chariot_unarmored");

    private static final Map<UUID, Long> DEFLECTING = new HashMap<>();
    private static final Map<UUID, Long> RIPOSTE_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> AFTERIMAGES_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> SABRE_FROM = new HashMap<>();
    private static final Map<UUID, ChariotBlade> BLADES = new HashMap<>();

    private Chariot() {
    }

    public static boolean is(StandType type) {
        return type != null && StandTypes.SILVER_CHARIOT_ID.equals(type.id());
    }

    public static boolean is(StandEntity stand) {
        return stand != null && is(stand.getStandType());
    }

    public static StandEntity stand(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.standSummoned || data.summonedStandEntityUuid == null) {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(data.summonedStandEntityUuid);
        return entity instanceof StandEntity stand && is(stand) && !stand.isDismissing() ? stand : null;
    }

    public static StandEntity standFor(Player player) {
        StandEntity stand = StandEntity.findFor(player);
        return is(stand) ? stand : null;
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tick(player);
            }
        });
        PlayerEvent.PLAYER_QUIT.register(Chariot::forget);
    }

    private static void forget(ServerPlayer player) {
        UUID id = player.getUUID();
        DEFLECTING.remove(id);
        RIPOSTE_UNTIL.remove(id);
        AFTERIMAGES_UNTIL.remove(id);
        SABRE_FROM.remove(id);
        BLADES.remove(id);
        unarmored(player, false);
    }

    private static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        StandEntity stand = player.isAlive() ? stand(player) : null;
        long now = player.level().getGameTime();

        unarmored(player, stand != null && stand.armorOff());

        if (stand == null) {
            DEFLECTING.remove(id);
            RIPOSTE_UNTIL.remove(id);
            SABRE_FROM.remove(id);
            AFTERIMAGES_UNTIL.remove(id);
            return;
        }

        Long until = AFTERIMAGES_UNTIL.get(id);
        if (stand.afterimages() > 0 && (!stand.armorOff() || until == null || now > until)) {
            stand.setAfterimages(0);
            AFTERIMAGES_UNTIL.remove(id);
        }

        ChariotBlade blade = BLADES.get(id);
        boolean lost = blade != null && !blade.isRemoved();
        if (!lost && blade != null) {
            BLADES.remove(id);
        }
        if (stand.bladeOut() != lost) {
            stand.setBladeOut(lost);
        }

        Long deflectFrom = DEFLECTING.get(id);
        if (deflectFrom != null) {
            if (now - deflectFrom > DEFLECT_MOST_TICKS) {
                endDeflect(player);
            } else {
                stand.keepStance();
            }
        }

        Long riposte = RIPOSTE_UNTIL.get(id);
        if (riposte != null) {
            if (now > riposte) {
                closeRiposte(player, stand);
            } else {
                stand.keepStance();
            }
        }

        Long sabre = SABRE_FROM.get(id);
        if (sabre != null) {
            if (now - sabre > SABRE_HELD_MOST_TICKS) {
                SabreCycleSkill.release(player, stand);
            } else {
                stand.keepStance();
                SabreCycleSkill.charging(player, now - sabre);
            }
        }
    }

    private static void unarmored(ServerPlayer player, boolean on) {
        apply(player.getAttribute(Attributes.MOVEMENT_SPEED), on,
                StandTuning.value("chariot_unarmored_speed", (float) UNARMORED_SPEED));
        apply(player.getAttribute(Attributes.ATTACK_SPEED), on,
                StandTuning.value("chariot_unarmored_attack_speed", (float) UNARMORED_ATTACK_SPEED));
    }

    private static void apply(AttributeInstance instance, boolean on, double amount) {
        if (instance == null) {
            return;
        }
        boolean has = instance.hasModifier(UNARMORED);
        if (on && !has) {
            instance.addTransientModifier(new AttributeModifier(UNARMORED, amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else if (!on && has) {
            instance.removeModifier(UNARMORED);
        }
    }

    public static float takenScale(ServerPlayer player) {
        StandEntity stand = stand(player);
        return stand != null && stand.armorOff()
                ? StandTuning.value("chariot_unarmored_taken", UNARMORED_TAKEN)
                : 1F;
    }

    public static void cool(ServerPlayer player, StandSkill skill) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.setMoveCooldown(skill.id(), player.level().getGameTime(), StandSkills.scaledCooldown(skill, data));
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static float blow(ServerPlayer player, LivingEntity target, String key, float base) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        float raw = StandTuning.damage(key, base) * data.stand.powerScale();
        return Math.min(10F, StandPassives.scaleOutgoing(player, data, target, raw));
    }

    public static boolean strike(ServerPlayer player, LivingEntity target, float amount) {
        HitWindow.open(target);
        boolean hurt = target.hurt(player.damageSources().playerAttack(player),
                DamageScale.stand(player, target, amount));
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.combatTicks = StandCombatHandler.COMBAT_TIMER_TICKS;
        PlayerDataAccess.set(player, data);
        return hurt;
    }

    public static void stun(LivingEntity target, int ticks, int tier) {
        target.addEffect(new MobEffectInstance(ModEffects.stun(), ticks, tier, false, false, true));
    }

    public static void thrust(StandEntity stand, Player owner) {
        if (!(stand.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 look = owner.getLookAngle();
        double reach = 1.4 + level.random.nextDouble() * 1.4;
        Vec3 tip = stand.position().add(0.0, stand.getBbHeight() * 0.6, 0.0).add(look.scale(reach))
                .add((level.random.nextDouble() - 0.5) * 0.7, (level.random.nextDouble() - 0.5) * 0.6,
                        (level.random.nextDouble() - 0.5) * 0.7);
        level.sendParticles(ParticleTypes.CRIT, tip.x, tip.y, tip.z, 2, 0.08, 0.08, 0.08, 0.05);
        level.playSound(null, stand.blockPosition(), ModSounds.SC_POKE.get(), SoundSource.PLAYERS,
                0.5F, 0.95F + level.random.nextFloat() * 0.1F);
    }

    public static void shedArmor(ServerPlayer player, StandEntity stand) {
        ServerLevel level = player.serverLevel();
        stand.setArmorOff(true);
        Vec3 at = stand.position().add(0.0, stand.getBbHeight() * 0.55, 0.0);
        ItemParticleOption plate = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_CHESTPLATE));
        level.sendParticles(plate, at.x, at.y, at.z, 26, 0.35, 0.45, 0.35, 0.32);
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 10, 0.3, 0.4, 0.3, 0.08);
        level.playSound(null, stand.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 1.5F);
        level.playSound(null, stand.blockPosition(), ModSounds.SC_ARMOR_OFF.get(), SoundSource.PLAYERS,
                1.0F, 1.0F);
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(at, at).inflate(ARMOR_BURST_RADIUS),
                entity -> entity != player && entity.isAlive() && !(entity instanceof StandEntity)
                        && StandHits.hittable(player, entity))) {
            Vec3 away = near.position().subtract(stand.position());
            Vec3 flat = new Vec3(away.x, 0.0, away.z);
            if (flat.lengthSqr() > 1.0E-6) {
                Vec3 shove = flat.normalize().scale(ARMOR_BURST_SHOVE);
                near.setDeltaMovement(near.getDeltaMovement().add(shove.x, 0.25, shove.z));
                near.hurtMarked = true;
            }
        }
    }

    public static void rearm(ServerPlayer player, StandEntity stand) {
        stand.setArmorOff(false);
        stand.setAfterimages(0);
        AFTERIMAGES_UNTIL.remove(player.getUUID());
        ServerLevel level = player.serverLevel();
        Vec3 at = stand.position().add(0.0, stand.getBbHeight() * 0.55, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 12, 0.3, 0.45, 0.3, 0.02);
        level.playSound(null, stand.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS,
                1.0F, 1.2F);
    }

    public static void split(ServerPlayer player, StandEntity stand) {
        stand.setAfterimages(StandTuning.ticks("chariot_afterimage_count", AFTERIMAGE_COUNT)
                + Relics.extraAfterimages(player));
        AFTERIMAGES_UNTIL.put(player.getUUID(), player.level().getGameTime()
                + StandTuning.ticks("chariot_afterimage_ticks", AFTERIMAGE_TICKS));
        ServerLevel level = player.serverLevel();
        Vec3 at = stand.position().add(0.0, stand.getBbHeight() * 0.5, 0.0);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 14, 0.9, 0.4, 0.9, 0.02);
        level.playSound(null, stand.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS,
                1.0F, 1.1F);
    }

    public static void launched(ServerPlayer player, ChariotBlade blade) {
        BLADES.put(player.getUUID(), blade);
    }

    public static boolean holds(ServerPlayer player, ChariotBlade blade) {
        return BLADES.get(player.getUUID()) == blade;
    }

    public static final String NO_RAPIER = "message.jojoha.chariot.bladeless";

    public static String rapierless(ServerPlayer player) {
        ChariotBlade blade = BLADES.get(player.getUUID());
        return blade != null && !blade.isRemoved() ? NO_RAPIER : null;
    }

    public static boolean deflecting(ServerPlayer player) {
        return DEFLECTING.containsKey(player.getUUID());
    }

    public static void beginDeflect(ServerPlayer player, StandEntity stand) {
        DEFLECTING.put(player.getUUID(), player.level().getGameTime());
        stand.holdChariotPose(StandEntity.HOLD_DEFLECT);
        stand.keepStance();
        player.serverLevel().playSound(null, stand.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.6F, 1.5F);
    }

    public static void endDeflect(ServerPlayer player) {
        if (DEFLECTING.remove(player.getUUID()) == null) {
            return;
        }
        StandEntity stand = stand(player);
        if (stand != null && stand.chariotHold() == StandEntity.HOLD_DEFLECT) {
            stand.holdChariotPose(StandEntity.HOLD_NONE);
        }
        cool(player, DeflectionSkill.INSTANCE);
    }

    public static boolean riposteOpen(ServerPlayer player) {
        return RIPOSTE_UNTIL.containsKey(player.getUUID());
    }

    public static void openRiposte(ServerPlayer player, StandEntity stand) {
        RIPOSTE_UNTIL.put(player.getUUID(), player.level().getGameTime()
                + Charms.parryWindow(player,
                        StandTuning.ticks("chariot_riposte_window", RIPOSTE_WINDOW_TICKS)));
        stand.holdChariotPose(StandEntity.HOLD_RIPOSTE);
        stand.keepStance();
        player.serverLevel().playSound(null, stand.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN.value(),
                SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    private static void closeRiposte(ServerPlayer player, StandEntity stand) {
        RIPOSTE_UNTIL.remove(player.getUUID());
        if (stand != null && stand.chariotHold() == StandEntity.HOLD_RIPOSTE) {
            stand.holdChariotPose(StandEntity.HOLD_NONE);
        }
    }

    public static boolean sabreCharging(ServerPlayer player) {
        return SABRE_FROM.containsKey(player.getUUID());
    }

    public static void beginSabre(ServerPlayer player, StandEntity stand) {
        SABRE_FROM.put(player.getUUID(), player.level().getGameTime());
        stand.holdChariotPose(StandEntity.HOLD_SABRE);
        stand.keepStance();
    }

    public static float endSabre(ServerPlayer player, StandEntity stand) {
        Long from = SABRE_FROM.remove(player.getUUID());
        if (stand != null && stand.chariotHold() == StandEntity.HOLD_SABRE) {
            stand.holdChariotPose(StandEntity.HOLD_NONE);
        }
        if (from == null) {
            return 0F;
        }
        long held = player.level().getGameTime() - from;
        return (float) Math.min(1.0, Math.max(0.0, held / (double) SABRE_FULL_TICKS));
    }

    public static boolean refuses(LivingEntity victim, DamageSource source) {
        if (!(victim instanceof ServerPlayer player) || !Defence.guardable(source)
                || source.getEntity() == player) {
            return false;
        }
        StandEntity stand = stand(player);
        if (stand == null) {
            return false;
        }
        return riposte(player, stand, source) || deflect(player, stand, source) || dodge(player, stand, source);
    }

    private static boolean riposte(ServerPlayer player, StandEntity stand, DamageSource source) {
        if (!riposteOpen(player) || !(source.getEntity() instanceof LivingEntity attacker)
                || !attacker.isAlive() || attacker.distanceTo(player) > RIPOSTE_REACH
                || !StandHits.hittable(player, attacker)) {
            return false;
        }
        closeRiposte(player, stand);

        Vec3 facing = Vec3.directionFromRotation(0F, attacker.getYRot());
        Vec3 flat = new Vec3(facing.x, 0.0, facing.z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3 behind = attacker.position().subtract(flat.scale(attacker.getBbWidth() * 0.5 + RIPOSTE_BEHIND));
        stand.strikeFrom(behind, attacker, "deflection");

        ServerLevel level = player.serverLevel();
        strike(player, attacker, blow(player, attacker, "riposte", RIPOSTE_DAMAGE));
        Relics.riposteLanded(player);
        stun(attacker, RIPOSTE_STUN_TICKS, Stun.TIER_ONE);
        Vec3 at = attacker.position().add(0.0, attacker.getBbHeight() * 0.6, 0.0);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        Impacts.landed(level, attacker, flat,
                ImpactSpec.HEAVY.silent(), Impacts.STEEL);
        level.playSound(null, attacker.blockPosition(), ModSounds.SC_CLEAVE.get(), SoundSource.PLAYERS,
                1.0F, 1.0F);
        level.playSound(null, attacker.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
                0.35F, 1.8F);

        StandBeat.after(RIPOSTE_LINGER_TICKS, player, player, (owner, self) -> {
            StandEntity back = stand(owner);
            if (back != null) {
                back.clearWork();
            }
        });
        return true;
    }

    private static boolean deflect(ServerPlayer player, StandEntity stand, DamageSource source) {
        if (!deflecting(player)) {
            return false;
        }
        Entity from = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        if (from == null || Defence.facing(player, from) == Defence.Facing.BACK || !payDeflect(player)) {
            return false;
        }
        if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker) {
            Vec3 away = attacker.position().subtract(player.position());
            Vec3 flat = new Vec3(away.x, 0.0, away.z);
            if (flat.lengthSqr() > 1.0E-6) {
                Vec3 shove = flat.normalize().scale(DEFLECT_SHOVE);
                attacker.setDeltaMovement(attacker.getDeltaMovement().add(shove.x, DEFLECT_LIFT, shove.z));
                attacker.hurtMarked = true;
            }
        }
        clang(player, stand, from.position().add(0.0, from.getBbHeight() * 0.6, 0.0));
        return true;
    }

    public static ProjectileDeflection deflectProjectile(ServerPlayer player, Projectile projectile) {
        if (!deflecting(player)) {
            return null;
        }
        StandEntity stand = stand(player);
        Vec3 velocity = projectile.getDeltaMovement();
        Vec3 from = velocity.lengthSqr() < 1.0E-6 ? projectile.position()
                : projectile.position().subtract(velocity.normalize().scale(3.0));
        if (stand == null || Defence.facing(player, from) == Defence.Facing.BACK || !payDeflect(player)) {
            return null;
        }
        clang(player, stand, projectile.position());
        return (thrown, by, random) -> {
            Entity shooter = thrown.getOwner();
            Vec3 back = shooter != null && shooter != player
                    ? shooter.getEyePosition().subtract(thrown.position())
                    : thrown.getDeltaMovement().scale(-1.0);
            if (back.lengthSqr() < 1.0E-6) {
                back = player.getLookAngle();
            }
            double speed = Math.max(thrown.getDeltaMovement().length(), DEFLECTED_LEAST_SPEED);
            thrown.setDeltaMovement(back.normalize().scale(speed * Defence.PARRIED_SPEED_KEPT));
            thrown.hasImpulse = true;
            thrown.setOwner(player);
        };
    }

    private static boolean payDeflect(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        float cost = StandTuning.value("chariot_deflect_energy", DEFLECT_ENERGY);
        if (data.standEnergy < cost) {
            endDeflect(player);
            return false;
        }
        data.standEnergy -= cost;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        return true;
    }

    private static void clang(ServerPlayer player, StandEntity stand, Vec3 at) {
        ServerLevel level = player.serverLevel();
        Vec3 guard = player.position().add(0.0, player.getBbHeight() * 0.6, 0.0);
        Vec3 toward = at.subtract(guard);
        Vec3 clash = toward.lengthSqr() < 1.0E-6 ? guard
                : guard.add(toward.normalize().scale(Math.min(1.1, toward.length() * 0.5)));
        level.sendParticles(ParticleTypes.CRIT, clash.x, clash.y, clash.z, 8, 0.1, 0.1, 0.1, 0.25);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, clash.x, clash.y, clash.z, 4, 0.05, 0.05, 0.05, 0.2);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.35F, 1.9F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                0.6F, 1.4F);
        stand.replayHold();
        stand.keepStance();
    }

    private static boolean dodge(ServerPlayer player, StandEntity stand, DamageSource source) {
        int left = stand.afterimages();
        if (left <= 0 || !stand.armorOff()
                || player.getRandom().nextFloat() >= StandTuning.value("chariot_afterimage_dodge", AFTERIMAGE_DODGE)) {
            return false;
        }
        stand.setAfterimages(left - 1);
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 10, 0.35, 0.5, 0.35, 0.03);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS,
                0.9F, 1.3F);
        return true;
    }
}
