package org.gumel.jojoha.stand.heaven;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.HeavenFxPacket;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Overwrite {
    public static final int CYCLE_TICKS = 120;
    public static final int DAY_STEP = 200;
    public static final int LIFT_AT = 120;
    public static final int DROP_AT = 160;
    public static final int FLASH_AT = 120;
    public static final int END_AT = 170;
    public static final int FLASH_TICKS = 50;
    public static final double GRAVITY_RADIUS = 40.0;
    public static final double REWRITE_RADIUS = 56.0;
    public static final int REWRITE_SECONDS = 30;
    public static final int REWRITE_WAVE_TICKS = 24;
    public static final double REACH = 64.0;
    public static final int DISABLE_SECONDS = 30;
    public static final float SKIN_CHANCE = 0.3F;
    private static final double SLAM = -1.4;
    private static final double PILLAR_SHARE = 0.5;

    private static final Map<UUID, Long> RUNNING = new HashMap<>();
    private static final Map<UUID, Long> DISABLED = new HashMap<>();

    private Overwrite() {
    }

    static void init() {
        TickEvent.SERVER_POST.register(Overwrite::tick);
    }

    public static boolean running(ServerPlayer player) {
        return RUNNING.containsKey(player.getUUID());
    }

    public static boolean disabled(ServerPlayer player) {
        Long until = DISABLED.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (player.serverLevel().getGameTime() >= until) {
            DISABLED.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static void disable(ServerPlayer player, int ticks) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent()) {
            return;
        }
        if (data.standSummoned) {
            StandSummonHandler.dismiss(player, data);
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
        }
        long until = player.serverLevel().getGameTime() + ticks;
        DISABLED.merge(player.getUUID(), until, Math::max);
    }

    public static void begin(ServerPlayer player) {
        RUNNING.put(player.getUUID(), player.serverLevel().getGameTime());
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4F, 0.5F);
        HeavenShow.on(player, HeavenFxPacket.PILLARS, (float) (Heaven.range(GRAVITY_RADIUS) * PILLAR_SHARE), END_AT);
        int hold = REWRITE_SECONDS * 20;
        HeavenShow.everywhere(level, HeavenFxPacket.REWRITE, player.getId(), FLASH_AT,
                FLASH_AT + REWRITE_WAVE_TICKS + hold);

        player.displayClientMessage(Component.translatable("message.jojoha.heaven.overwrite")
                .withStyle(ChatFormatting.WHITE), true);
        JojohaPlayerData data = PlayerDataAccess.get(player);
        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand != null) {
            stand.triggerTimeStopPose();
        }
    }

    private static void tick(MinecraftServer server) {
        if (RUNNING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Long>> iterator = RUNNING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            ServerPlayer user = server.getPlayerList().getPlayer(entry.getKey());
            if (user == null || !user.isAlive()) {
                iterator.remove();
                continue;
            }
            int t = (int) (user.serverLevel().getGameTime() - entry.getValue());
            step(user, t);
            if (t >= END_AT) {
                Ascension.spend(user);
                iterator.remove();
            }
        }
    }

    private static void step(ServerPlayer user, int t) {
        ServerLevel level = user.serverLevel();
        if (t < CYCLE_TICKS) {
            level.setDayTime(level.getDayTime() + DAY_STEP);
            level.getServer().getPlayerList().broadcastAll(new ClientboundSetTimePacket(
                    level.getGameTime(), level.getDayTime(),
                    level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)), level.dimension());
            int every = Math.max(1, 8 - t * 7 / CYCLE_TICKS);
            if (t % every == 0) {
                level.playSound(null, user.blockPosition(), ModSounds.CLOCK_SINGLE.get(), SoundSource.PLAYERS,
                        0.9F + t / (float) CYCLE_TICKS, 0.5F + t / (float) CYCLE_TICKS);
            }
            if (t % 20 == 0) {
                level.playSound(null, user.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS,
                        1.5F, 0.5F + t / (float) CYCLE_TICKS);
            }
        }
        if (t == LIFT_AT) {
            for (LivingEntity caught : caught(user)) {
                caught.addEffect(new MobEffectInstance(MobEffects.LEVITATION, DROP_AT - LIFT_AT, 2, false, false, true));
                HeavenShow.on(caught, HeavenFxPacket.LIFT, 0F, DROP_AT - LIFT_AT);
            }
            level.playSound(null, user.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
        if (t == DROP_AT) {
            for (LivingEntity caught : caught(user)) {
                caught.removeEffect(MobEffects.LEVITATION);
                Vec3 motion = caught.getDeltaMovement();
                caught.setDeltaMovement(motion.x, SLAM, motion.z);
                caught.hurtMarked = true;
            }
            HeavenShow.on(user, HeavenFxPacket.SLAM, user.position(), (float) Heaven.range(GRAVITY_RADIUS), 0);
            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 0.5F);
            level.playSound(null, user.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 0.7F);
        }
        if (t == FLASH_AT) {
            flash(user, level);
        }
    }

    private static Iterable<LivingEntity> caught(ServerPlayer user) {
        return user.serverLevel().getEntitiesOfClass(LivingEntity.class,
                user.getBoundingBox().inflate(Heaven.range(GRAVITY_RADIUS)),
                entity -> entity != user && entity.isAlive() && !(entity instanceof StandEntity)
                        && !entity.isSpectator());
    }

    private static void flash(ServerPlayer user, ServerLevel level) {
        level.playSound(null, user.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.2F, 0.6F);
        HeavenShow.full(level, HeavenFxPacket.REALM, -1, REWRITE_WAVE_TICKS, user.position(),
                (float) Heaven.range(REWRITE_RADIUS), REWRITE_SECONDS * 20);
        if (level.isRaining()) {
            level.setWeatherParameters(12000, 0, false, false);
        } else {
            level.setWeatherParameters(0, 6000, true, true);
        }
        level.sendParticles(ParticleTypes.FLASH, user.getX(), user.getY() + 1.0, user.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
        long until = level.getGameTime() + DISABLE_SECONDS * 20L;
        for (ServerPlayer other : level.players()) {
            NetworkHandler.sendHeavenFx(other, HeavenFxPacket.FLASH, FLASH_TICKS);
            if (other == user) {
                continue;
            }
            JojohaPlayerData data = PlayerDataAccess.get(other);
            if (!data.stand.isPresent()) {
                continue;
            }
            if (data.standSummoned) {
                StandSummonHandler.dismiss(other, data);
            }
            DISABLED.put(other.getUUID(), until);
            other.displayClientMessage(Component.translatable("message.jojoha.heaven.stand_overwritten"), true);
            reskin(other, data);
            PlayerDataAccess.set(other, data);
            PlayerDataAccess.sync(other);
        }
    }

    private static void reskin(ServerPlayer player, JojohaPlayerData data) {
        StandType type = StandTypes.byId(data.stand.standId());
        if (type == null || type.rollableSkinCount() < 2
                || player.getRandom().nextFloat() >= SKIN_CHANCE) {
            return;
        }
        List<Integer> choices = new ArrayList<>();
        for (int other = 0; other < type.skinCount(); other++) {
            if (other != data.stand.skin() && type.rollableSkin(other)) {
                choices.add(other);
            }
        }
        if (choices.isEmpty()) {
            return;
        }
        int skin = choices.get(player.getRandom().nextInt(choices.size()));
        data.commitSkin(data.stand.standId(), skin);
        data.stand = data.stand.withSkin(skin);
        player.sendSystemMessage(Component.translatable("message.jojoha.heaven.skin_overwritten",
                Component.translatable(type.skinNameKey(skin))).withStyle(ChatFormatting.GRAY));
    }
}
