package org.gumel.jojoha.stand.heaven;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.packet.HeavenFxPacket;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.skill.Harmless;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.moves.HeavenTimeStopSkill;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class Heaven {
    public static final ResourceLocation ID = StandTypes.SECRET_THE_WORLD_ID;

    public static final float STAMINA_SCALE = 1.5F;
    public static final float DRAIN_SCALE = 0.35F;
    public static final float COST_SCALE = 0.6F;
    public static final float TAKEN_SCALE = 1.15F;
    public static final float STUN_SCALE = 0.5F;
    public static final float RANGE_SCALE = 1.15F;

    public static final float ASCENDED_PUNCH_SCALE = 0.7F;
    public static final float ASCENDED_COOLDOWN_SCALE = 0.85F;
    public static final int TRANSCENDENT_GUARD_TICKS = 30;
    public static final int TRANSCENDENT_GUARD_LEVEL = 1;

    public static final int ANNOUNCE_GAP_TICKS = 1200;
    public static final int ANNOUNCE_COLOUR = 0xAF4BFF;

    private static final int GLOW_TICKS = 40;
    private static final int HALO_EVERY = 20;
    private static final int HALO_TICKS = 30;
    private static final int SYNC_EVERY = 10;
    private static final int HIGH_EVERY = 10;
    private static final int HIGH_TICKS = 16;
    private static final double PULL_SPEED = 0.45;

    private static final Map<UUID, Long> ANNOUNCED = new HashMap<>();
    private static final Map<LivingEntity, Vec3> PULLS = new HashMap<>();

    private static MinecraftServer server;

    private Heaven() {
    }

    public static MinecraftServer server() {
        return server;
    }

    public static void init() {
        LifecycleEvent.SERVER_STARTED.register(started -> server = started);
        LifecycleEvent.SERVER_STOPPED.register(stopped -> server = null);
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                tick(serverPlayer);
            }
        });
        TickEvent.SERVER_POST.register(Heaven::serverTick);
        PlayerEvent.PLAYER_QUIT.register(player -> {
            ANNOUNCED.remove(player.getUUID());
            HeavenTimeStop.release(player.getUUID());
        });
        Ascension.init();
        RealityBend.init();
        Servitude.init();
        FallenMobs.init();
        Overwrite.init();
        WorldPatch.init();
        HeavenTimeStop.init();
    }

    public static boolean is(JojohaPlayerData data) {
        return data != null && data.stand != null && data.stand.isPresent() && ID.equals(data.stand.standId());
    }

    public static boolean is(Entity entity) {
        return entity instanceof ServerPlayer player && is(PlayerDataAccess.get(player));
    }

    public static boolean out(JojohaPlayerData data) {
        return is(data) && data.standSummoned;
    }

    public static ServerPlayer userOf(Entity attacker) {
        if (attacker instanceof StandEntity stand && stand.getOwner() instanceof ServerPlayer owner) {
            return is(owner) ? owner : null;
        }
        return is(attacker) ? (ServerPlayer) attacker : null;
    }

    public static float staminaScale(JojohaPlayerData data) {
        return is(data) ? STAMINA_SCALE : 1F;
    }

    public static float drainScale(JojohaPlayerData data) {
        return is(data) ? DRAIN_SCALE : 1F;
    }

    public static float costScale(JojohaPlayerData data) {
        return is(data) ? COST_SCALE : 1F;
    }

    public static double range(double base) {
        return base * RANGE_SCALE;
    }

    public static int punchTicks(JojohaPlayerData data, int base) {
        return out(data) && Ascension.reached(data, Ascension.ASCENDED)
                ? Math.max(1, Math.round(base * ASCENDED_PUNCH_SCALE)) : base;
    }

    public static float cooldownScale(JojohaPlayerData data) {
        return out(data) && Ascension.reached(data, Ascension.ASCENDED) ? ASCENDED_COOLDOWN_SCALE : 1F;
    }

    public static MobEffectInstance steady(ServerPlayer player, MobEffectInstance incoming) {
        if (incoming.isInfiniteDuration() || incoming.getEffect().value() != ModEffects.STUN.get()
                || !out(PlayerDataAccess.get(player))) {
            return incoming;
        }
        int kept = Math.max(1, Math.round(incoming.getDuration() * STUN_SCALE));
        return new MobEffectInstance(incoming.getEffect(), kept, incoming.getAmplifier(),
                incoming.isAmbient(), incoming.isVisible(), incoming.showIcon());
    }

    public static boolean refuses(LivingEntity victim, DamageSource source) {
        if (RealityBend.rewinding(victim)) {
            return true;
        }
        Entity attacker = source.getEntity();
        if (attacker == null) {
            return false;
        }
        Entity body = attacker instanceof StandEntity stand && stand.getOwner() != null ? stand.getOwner() : attacker;
        if (HeavenTimeStop.holding(body.getUUID())) {
            return true;
        }
        return Servitude.servesAgainst(attacker, victim);
    }

    public static float incoming(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof ServerPlayer player)) {
            return amount;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!out(data)) {
            return amount;
        }
        if (source.getEntity() != null && (player.isBlocking() || data.standGuarding)) {
            Ascension.add(player, Ascension.PER_BLOCK);
        }
        return Math.min(amount * TAKEN_SCALE, Float.MAX_VALUE);
    }

    public static float outgoing(ServerPlayer player, JojohaPlayerData data, LivingEntity target, float amount) {
        if (!is(data)) {
            return amount;
        }
        Ascension.add(player, amount * Ascension.PER_DAMAGE);
        if (Ascension.reached(data, Ascension.ASCENDED)) {
            PULLS.put(target, player.position());
        }
        if (GreatestHigh.active(data)) {
            HeavenShow.at(player.serverLevel(), HeavenFxPacket.HIGH_HIT,
                    target.position().add(0.0, target.getBbHeight() * 0.6, 0.0), 0F, 0);
        }
        return amount * GreatestHigh.multiplier(data);
    }

    public static void pull(LivingEntity target, Vec3 toward) {
        PULLS.put(target, toward);
    }

    public static void cast(ServerPlayer player, JojohaPlayerData data, StandSkill skill) {
        if (!is(data)) {
            return;
        }
        Ascension.add(player, Ascension.PER_MOVE);
        if (Ascension.reached(data, Ascension.TRANSCENDENT)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, TRANSCENDENT_GUARD_TICKS,
                    TRANSCENDENT_GUARD_LEVEL, false, false, true));
        }
    }

    public static String unavailable(ServerPlayer player, StandSkill skill) {
        if (HeavenTimeStop.holding(player.getUUID()) && skill != HeavenTimeStopSkill.INSTANCE
                && !Harmless.is(skill.id())) {
            return "message.jojoha.heaven.no_attacks";
        }
        return null;
    }

    public static boolean summonRefused(ServerPlayer player) {
        if (!Overwrite.disabled(player)) {
            return false;
        }
        player.displayClientMessage(Component.translatable("message.jojoha.heaven.stand_overwritten"), true);
        return true;
    }

    public static void summoned(ServerPlayer player, JojohaPlayerData data) {
        if (!is(data)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        HeavenShow.on(player, HeavenFxPacket.SUMMON, 0F, 0);
        long now = server.overworld().getGameTime();
        Long last = ANNOUNCED.get(player.getUUID());
        if (last != null && now - last < ANNOUNCE_GAP_TICKS) {
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 1.2F, 0.6F);
            return;
        }
        ANNOUNCED.put(player.getUUID(), now);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.PLAYERS, 1.0F, 0.7F);
        Component line = Component.translatable("announce.jojoha.heaven_awoken")
                .withStyle(style -> style.withColor(ANNOUNCE_COLOUR).withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, player.getDisplayName())));
        server.getPlayerList().broadcastSystemMessage(line, false);
    }

    private static void tick(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!is(data)) {
            if (data.ascension > 0F || data.greatestHighTicks > 0 || data.heavenFightTicks > 0) {
                data.ascension = 0F;
                data.greatestHighTicks = 0;
                data.heavenFightTicks = 0;
                PlayerDataAccess.set(player, data);
                PlayerDataAccess.sync(player);
            }
            return;
        }

        float ascension = data.ascension;
        int greatestHigh = data.greatestHighTicks;
        boolean wasHeaven = Ascension.reached(data, Ascension.HEAVEN);

        Ascension.tick(player, data);
        GreatestHigh.tick(player, data);

        if (data.standSummoned) {
            if (!player.hasEffect(MobEffects.GLOWING) || player.getEffect(MobEffects.GLOWING).getDuration() < GLOW_TICKS / 2) {
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, true, false, false));
            }
            if (Ascension.reached(data, Ascension.HEAVEN)) {
                aura(player);
            }
            dressStand(player, data);
        }

        boolean changed = ascension != data.ascension || greatestHigh != data.greatestHighTicks;
        PlayerDataAccess.set(player, data);
        boolean crossed = wasHeaven != Ascension.reached(data, Ascension.HEAVEN)
                || (greatestHigh > 0) != (data.greatestHighTicks > 0);
        if (changed && (crossed || player.tickCount % SYNC_EVERY == 0)) {
            PlayerDataAccess.sync(player);
        }
    }

    public static int tier(JojohaPlayerData data) {
        return Ascension.reached(data, Ascension.HEAVEN) ? 3 : Ascension.reached(data, Ascension.TRANSCENDENT) ? 2
                : Ascension.reached(data, Ascension.ASCENDED) ? 1 : 0;
    }

    private static final int TIER_EVERY = 10;
    private static final int SKY_EVERY = 20;
    private static final int SKY_TICKS = 50;

    public static void announce(ServerPlayer player, String key) {
        Component line = Component.translatable(key)
                .withStyle(style -> style.withColor(ANNOUNCE_COLOUR).withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, player.getDisplayName())));
        for (ServerPlayer other : player.serverLevel().players()) {
            other.sendSystemMessage(line);
        }
    }

    public static void telegraph(ServerPlayer player) {
        StandEntity stand = StandSummonHandler.findStand(player, PlayerDataAccess.get(player));
        if (stand != null) {
            HeavenShow.on(stand, HeavenFxPacket.TELEGRAPH, 0F, TELEGRAPH_TICKS);
        }
    }

    private static final int TELEGRAPH_TICKS = 12;

    private static void dressStand(ServerPlayer player, JojohaPlayerData data) {
        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand == null) {
            return;
        }
        if (player.tickCount % TIER_EVERY == 0) {
            HeavenShow.on(stand, HeavenFxPacket.STAND_TIER, tier(data), TIER_EVERY * 3);
        }
        if (player.tickCount % SKY_EVERY == 0) {
            HeavenShow.everywhere(player.serverLevel(), HeavenFxPacket.SKY,
                    stand.getId(), tier(data), SKY_TICKS);
        }
        if (stand.getSkin() != data.stand.skin()) {
            stand.setSkin(data.stand.skin());
        }
        if (data.greatestHighTicks > 0 && player.tickCount % HIGH_EVERY == 0) {
            HeavenShow.on(stand, HeavenFxPacket.HIGH, 0F, HIGH_TICKS);
        }
    }

    private static void aura(ServerPlayer player) {
        if (player.tickCount % HALO_EVERY == 0) {
            HeavenShow.on(player, HeavenFxPacket.HALO, 0F, HALO_TICKS);
        }
    }

    private static void serverTick(MinecraftServer server) {
        if (PULLS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<LivingEntity, Vec3>> iterator = PULLS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LivingEntity, Vec3> pull = iterator.next();
            iterator.remove();
            LivingEntity target = pull.getKey();
            if (!target.isAlive()) {
                continue;
            }
            Vec3 toward = pull.getValue().subtract(target.position());
            Vec3 flat = new Vec3(toward.x, 0.0, toward.z);
            if (flat.lengthSqr() < 1.0E-4) {
                continue;
            }
            Vec3 motion = target.getDeltaMovement();
            double speed = Math.max(PULL_SPEED, Math.sqrt(motion.x * motion.x + motion.z * motion.z));
            Vec3 pulled = flat.normalize().scale(speed);
            target.setDeltaMovement(pulled.x, motion.y, pulled.z);
            target.hurtMarked = true;
        }
    }

    public static boolean isHeavenUser(Player player) {
        return player instanceof ServerPlayer serverPlayer && is(PlayerDataAccess.get(serverPlayer));
    }
}
