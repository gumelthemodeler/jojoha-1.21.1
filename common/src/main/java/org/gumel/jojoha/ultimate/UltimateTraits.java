package org.gumel.jojoha.ultimate;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.StandRelease;
import org.gumel.jojoha.stand.anubis.Anubis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class UltimateTraits {
    public static final int STUDY_TICKS = 60;

    private static final double STUDY_LEASH = 24.0;

    private static final double STUDY_GAZE = 0.8;

    private static final int GAZE_GRACE_TICKS = 10;

    private static final Map<UUID, Study> STUDIES = new HashMap<>();

    private static final Set<UUID> SHOWN = new HashSet<>();

    private static final int WINGS_REFRESH = 20;

    private UltimateTraits() {
    }

    private static final class Study {
        final UUID target;
        int ticks;
        int unseen;

        Study(UUID target) {
            this.target = target;
        }
    }

    public static void study(ServerPlayer player, LivingEntity target) {
        STUDIES.put(player.getUUID(), new Study(target.getUUID()));
    }

    public static void forget(UUID playerId) {
        STUDIES.remove(playerId);
        SHOWN.remove(playerId);
        AdaptiveFlightMove.forget(playerId);
        AdaptationLibrary.forget(playerId);
        AdaptiveDefence.forget(playerId);
    }

    public static void tick(ServerPlayer player, JojohaPlayerData data) {
        standless(player, data);
        wings(player, data);
        studying(player, data);
        AdaptiveDefence.tick(player, data);
    }

    private static void standless(ServerPlayer player, JojohaPlayerData data) {
        boolean lost = StandRelease.release(player, data);
        if (data.anubis) {
            Anubis.unbond(player, data);
            lost = true;
        }
        if (!lost) {
            return;
        }
        player.displayClientMessage(Component.translatable("message.jojoha.ultimate.stand_lost"), true);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 0.8F, 0.6F);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    private static void wings(ServerPlayer player, JojohaPlayerData data) {
        boolean was = SHOWN.contains(player.getUUID());
        if (data.ultimateWings != was || (data.ultimateWings && player.tickCount % WINGS_REFRESH == 0)) {
            if (data.ultimateWings) {
                SHOWN.add(player.getUUID());
            } else {
                SHOWN.remove(player.getUUID());
            }
            NetworkHandler.broadcastWings(player, data.ultimateWings, false);
        }
        if (!data.ultimateWings) {
            return;
        }
        player.fallDistance = 0F;
        if (!AdaptiveFlightMove.gliding(player)) {
            return;
        }
        if (player.isFallFlying() && player.isShiftKeyDown()) {
            player.stopFallFlying();
        }
        data.specEnergy = Math.max(0F, data.specEnergy - JojohaServerConfig.WING_DRAIN.value() / 20F);
        if (data.specEnergy <= 0F) {
            data.ultimateWings = false;
            AdaptiveFlightMove.folded(player, data);
            player.displayClientMessage(Component.translatable("message.jojoha.adapt.wings_spent"), true);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA.value(),
                    SoundSource.PLAYERS, 0.8F, 1.2F);
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
        }
    }

    private static void studying(ServerPlayer player, JojohaPlayerData data) {
        Study study = STUDIES.get(player.getUUID());
        if (study == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(study.target);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()
                || target.distanceTo(player) > STUDY_LEASH) {
            lost(player);
            return;
        }

        if (watching(player, target)) {
            study.unseen = 0;
            study.ticks++;
        } else if (++study.unseen > GAZE_GRACE_TICKS) {
            lost(player);
            return;
        }

        if (study.ticks % 5 == 0) {
            Vec3 centre = target.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.ENCHANT, centre.x, centre.y + target.getBbHeight() * 0.5, centre.z,
                    6, target.getBbWidth() * 0.5, target.getBbHeight() * 0.3, target.getBbWidth() * 0.5, 0.4);
        }

        if (study.ticks < STUDY_TICKS) {
            return;
        }
        STUDIES.remove(player.getUUID());
        Adaptation adaptation = Adaptation.of(target.getType());
        if (adaptation == null) {
            return;
        }
        AdaptationLibrary.Learned learned = AdaptationLibrary.learn(player, data, target, adaptation);
        Component creature = target.getType().getDescription();
        Component ability = Component.translatable(adaptation.translationKey());
        switch (learned) {
            case MASTERED -> {
                player.displayClientMessage(Component.translatable("message.jojoha.adapt.mastered", ability), true);
                return;
            }
            case SAME_CREATURE -> {
                player.displayClientMessage(Component.translatable("message.jojoha.adapt.same_creature", creature), true);
                return;
            }
            case NEW -> player.displayClientMessage(Component.translatable("message.jojoha.adapt.learned", creature, ability), true);
            case DEEPER -> player.displayClientMessage(Component.translatable("message.jojoha.adapt.deeper", ability,
                    AdaptationLibrary.mastery(data, adaptation)), true);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(),
                30, 0.4, 0.6, 0.4, 0.6);
    }

    private static boolean watching(ServerPlayer player, LivingEntity target) {
        Vec3 toward = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        if (toward.lengthSqr() < 1.0E-4) {
            return true;
        }
        return toward.normalize().dot(player.getLookAngle()) >= STUDY_GAZE && player.hasLineOfSight(target);
    }

    private static void lost(ServerPlayer player) {
        STUDIES.remove(player.getUUID());
        player.displayClientMessage(Component.translatable("message.jojoha.adapt.lost"), true);
    }
}
