package org.gumel.jojoha.fortune;

import dev.architectury.event.EventResult;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.packet.FlairCuePacket;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandTypes;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Cursed {
    public static final int BLINDED = 1;

    public static final int FRAGILE = 2;

    public static final int LEGLESS = 4;

    public static final int[] ALL = {BLINDED, FRAGILE, LEGLESS};

    public static final int SCENT = 8;

    public static final int HEARING = 16;

    public static final int[] SENSES = {SCENT, HEARING};

    public static final float HALF_HEART = 1F;

    public static final int GHOST_TICKS = 1200;

    public static final float FRAGILE_DAMAGE = 2F;

    public static final int PAIN_DELAY_TICKS = 40;

    private static final double HAUNT_RANGE = 12.0;

    private static final int HAUNT_EVERY = 20;

    private static final int HAUNT_TICKS = 60;

    private static final double HEARING_RANGE = 20.0;

    private static final int LISTEN_EVERY = 5;

    private static final int NOISE_TICKS = 20;

    private static final double STEP = 1.0E-4;

    private static final int GHOST_SYNC_EVERY = 20;

    private record Pain(long at, float amount, DamageSource source) {
    }

    private static final Map<UUID, ArrayDeque<Pain>> PAIN = new HashMap<>();

    private static final Map<UUID, UUID> KILLERS = new HashMap<>();

    private static final Set<UUID> FADING = new HashSet<>();

    private static final Set<UUID> WATCHED = new HashSet<>();

    private Cursed() {
    }

    public static String name(int curse) {
        return switch (curse) {
            case BLINDED -> "blinded";
            case FRAGILE -> "fragile";
            default -> "legless";
        };
    }

    public static int byName(String name) {
        for (int curse : ALL) {
            if (name(curse).equals(name)) {
                return curse;
            }
        }
        return 0;
    }

    public static String senseName(int sense) {
        return sense == SCENT ? "scent" : "hearing";
    }

    public static int senseByName(String name) {
        for (int sense : SENSES) {
            if (senseName(sense).equals(name)) {
                return sense;
            }
        }
        return 0;
    }

    public static String senseKey(int sense) {
        return "fortune.jojoha.sense." + senseName(sense);
    }

    public static int sense(JojohaPlayerData data) {
        return data.curses & (SCENT | HEARING);
    }

    public static boolean lean(ServerPlayer player, int sense) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!has(data, BLINDED) || sense == 0 || sense(data) != 0) {
            return false;
        }
        data.curses |= sense;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.PLAYERS, 0.8F, 1.2F);
        Fortunes.whisper(player, Component.translatable("message.jojoha.curse.sense_taken",
                Component.translatable(senseKey(sense))));
        return true;
    }

    private static void offerSense(ServerPlayer player, JojohaPlayerData data) {
        if (!has(data, BLINDED) || sense(data) != 0) {
            return;
        }
        MutableComponent line = Component.translatable("message.jojoha.curse.sense_offer").withStyle(ChatFormatting.GRAY);
        for (int sense : SENSES) {
            line.append(" ").append(Component.literal("[")
                    .append(Component.translatable(senseKey(sense)))
                    .append("]")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/fortune sense " + senseName(sense)))));
        }
        player.sendSystemMessage(line);
    }

    public static String nameKey(int curse) {
        return "fortune.jojoha.curse." + name(curse);
    }

    public static boolean cursed(JojohaPlayerData data) {
        return Fortunes.on() && Fortune.has(data, Fortune.CURSED);
    }

    public static boolean has(JojohaPlayerData data, int curse) {
        return cursed(data) && (data.curses & curse) != 0;
    }

    public static boolean ghost(JojohaPlayerData data) {
        return data.ghostTicks > 0;
    }

    public static boolean ghost(Player player) {
        return ghost(PlayerDataAccess.get(player));
    }

    public static int roll(RandomSource random) {
        return ALL[random.nextInt(ALL.length)];
    }

    public static int ghostLength(JojohaPlayerData data) {
        return has(data, FRAGILE) ? GHOST_TICKS * 2 : GHOST_TICKS;
    }

    public static boolean take(ServerPlayer player, int curse) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!cursed(data) || ghost(data) || curse == 0 || (data.curses & curse) != 0) {
            return false;
        }
        data.curses = curse;
        PAIN.remove(player.getUUID());
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.PLAYERS, 1.0F, 0.6F);
        Fortunes.whisper(player, Component.translatable("message.jojoha.curse.taken",
                Component.translatable(nameKey(curse))));
        offerSense(player, data);
        return true;
    }

    public static void offer(ServerPlayer player, JojohaPlayerData data) {
        if (!cursed(data)) {
            return;
        }
        offerSense(player, data);
        MutableComponent line = Component.translatable("message.jojoha.curse.offer").withStyle(ChatFormatting.GRAY);
        boolean any = false;
        for (int curse : ALL) {
            if ((data.curses & curse) != 0) {
                continue;
            }
            any = true;
            line.append(" ").append(Component.literal("[")
                    .append(Component.translatable(nameKey(curse)))
                    .append("]")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/fortune curse " + name(curse)))));
        }
        if (any) {
            player.sendSystemMessage(line);
        }
    }

    public static float incoming(ServerPlayer victim, JojohaPlayerData data, float amount) {
        return has(data, FRAGILE) ? amount * FRAGILE_DAMAGE : amount;
    }

    public static boolean refusesBlow(DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) {
            return false;
        }
        JojohaPlayerData data = PlayerDataAccess.get(attacker);
        if (!ghost(data)) {
            return false;
        }
        return !has(data, FRAGILE) || !StandCombatHandler.standBlow(source)
                && source.getDirectEntity() == attacker;
    }

    public static float survive(LivingEntity self, DamageSource source, float before, float after) {
        if (!(self instanceof ServerPlayer player) || after >= before) {
            return after;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!cursed(data) && !ghost(data)) {
            return after;
        }
        if (FADING.contains(player.getUUID()) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return after;
        }
        if (ghost(data)) {
            return before;
        }
        if (has(data, LEGLESS)) {
            PAIN.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>())
                    .addLast(new Pain(player.level().getGameTime() + PAIN_DELAY_TICKS, before - after, source));
            return before;
        }
        return floor(before, after);
    }

    private static float floor(float before, float after) {
        return before > HALF_HEART ? Math.max(after, HALF_HEART) : after;
    }

    public static EventResult died(ServerPlayer player, DamageSource source) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (FADING.contains(player.getUUID()) || ghost(data) || !cursed(data)) {
            return EventResult.pass();
        }
        linger(player, data, source);
        return EventResult.interruptFalse();
    }

    private static void linger(ServerPlayer player, JojohaPlayerData data, DamageSource source) {
        PAIN.remove(player.getUUID());
        data.ghostTicks = ghostLength(data);
        Entity killer = source.getEntity();
        if (killer != null && killer != player) {
            KILLERS.put(player.getUUID(), killer.getUUID());
        } else {
            KILLERS.remove(player.getUUID());
        }
        player.setHealth(player.getMaxHealth());
        player.clearFire();
        if (has(data, FRAGILE)) {
            if (data.stand.isPresent() && StandTypes.hasBody(data)) {
                StandSummonHandler.forceSummon(player, data);
            }
        } else if (data.standSummoned) {
            StandSummonHandler.dismiss(player, data);
        }
        PlayerDataAccess.set(player, data);
        haunting(player, true);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.PLAYERS, 1.2F, 0.5F);
        player.serverLevel().sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0, player.getZ(),
                24, 0.4, 0.6, 0.4, 0.04);
        player.sendSystemMessage(Component.translatable("message.jojoha.curse.linger", data.ghostTicks / 20)
                .withStyle(ChatFormatting.GRAY)
                .append(" ")
                .append(Component.translatable("message.jojoha.curse.pass_on")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_AQUA)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/fortune pass")))));
        PlayerDataAccess.sync(player);
    }

    private static void haunting(ServerPlayer player, boolean on) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (player.getAbilities().mayfly != on || player.getAbilities().invulnerable != on) {
            player.getAbilities().mayfly = on;
            player.getAbilities().invulnerable = on;
            if (!on) {
                player.getAbilities().flying = false;
            }
            player.onUpdateAbilities();
        }
    }

    public static void passOn(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (ghost(data)) {
            fade(player, data);
        }
    }

    private static void fade(ServerPlayer player, JojohaPlayerData data) {
        data.ghostTicks = 0;
        PlayerDataAccess.set(player, data);
        haunting(player, false);
        player.removeEffect(MobEffects.INVISIBILITY);
        KILLERS.remove(player.getUUID());
        WATCHED.remove(player.getUUID());
        PlayerDataAccess.sync(player);
        FADING.add(player.getUUID());
        try {
            player.kill();
        } finally {
            FADING.remove(player.getUUID());
        }
    }

    public static boolean summonRefused(ServerPlayer player, JojohaPlayerData data, boolean summoning) {
        if (!ghost(data)) {
            return false;
        }
        boolean fragile = has(data, FRAGILE);
        if (summoning == fragile) {
            return false;
        }
        player.displayClientMessage(Component.translatable(summoning
                ? "message.jojoha.curse.no_stand" : "message.jojoha.curse.stand_bound"), true);
        return true;
    }

    public static boolean pickupRefused(Player player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        return ghost(data) && has(data, FRAGILE);
    }

    public static void tick(ServerPlayer player, JojohaPlayerData data, boolean active) {
        if (ghost(data)) {
            haunt(player, data);
            return;
        }
        if (!active) {
            PAIN.remove(player.getUUID());
            return;
        }
        if (has(data, BLINDED)) {
            listen(player, sense(data) == 0);
        }
        feel(player);
    }

    private static void haunt(ServerPlayer player, JojohaPlayerData data) {
        haunting(player, true);
        boolean fragile = has(data, FRAGILE);
        MobEffectInstance hidden = player.getEffect(MobEffects.INVISIBILITY);
        if (hidden == null || hidden.getDuration() < 20) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false, false));
        }
        if (!fragile && player.tickCount % 4 == 0) {
            player.serverLevel().sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0,
                    player.getZ(), 1, 0.25, 0.5, 0.25, 0.01);
        }
        if (player.tickCount % HAUNT_EVERY == 0) {
            UUID killerId = KILLERS.get(player.getUUID());
            Entity killer = killerId == null ? null : player.serverLevel().getEntity(killerId);
            if (killer instanceof LivingEntity living && living.isAlive() && living.distanceTo(player) <= HAUNT_RANGE) {
                living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, HAUNT_TICKS, 0, false, false, true));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, HAUNT_TICKS, 0, false, false, true));
                if (living instanceof ServerPlayer haunted && WATCHED.add(player.getUUID())) {
                    Fortunes.whisper(haunted, Component.translatable("message.jojoha.curse.haunted"));
                }
            }
        }
        if (fragile && data.stand.isPresent() && StandTypes.hasBody(data) && !data.standSummoned) {
            fade(player, data);
            return;
        }
        data.ghostTicks--;
        if (data.ghostTicks <= 0) {
            fade(player, data);
            return;
        }
        PlayerDataAccess.set(player, data);
        if (data.ghostTicks % GHOST_SYNC_EVERY == 0) {
            PlayerDataAccess.syncSoon(player);
        }
    }

    private static void listen(ServerPlayer player, boolean ears) {
        MobEffectInstance blind = player.getEffect(MobEffects.BLINDNESS);
        if (!ears) {
            if (blind != null && blind.isAmbient()) {
                player.removeEffect(MobEffects.BLINDNESS);
            }
            return;
        }
        if (blind == null || blind.getDuration() < 40) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, true, false, false));
        }
        if (player.tickCount % LISTEN_EVERY != 0) {
            return;
        }
        for (LivingEntity body : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(HEARING_RANGE),
                entity -> entity != player && entity.isAlive() && !entity.isSpectator())) {
            if (noisy(body)) {
                NetworkManager.sendToPlayer(player, new FlairCuePacket(body.getId(), FlairCuePacket.NOISE, NOISE_TICKS));
            }
        }
    }

    private static boolean noisy(LivingEntity body) {
        if (body.hurtTime > 0 || body.swinging) {
            return true;
        }
        if (body.isSteppingCarefully() || body.isShiftKeyDown()) {
            return false;
        }
        double dx = body.getX() - body.xo;
        double dz = body.getZ() - body.zo;
        return dx * dx + dz * dz > STEP || body.getY() - body.yo > 0.05;
    }

    private static void feel(ServerPlayer player) {
        ArrayDeque<Pain> pain = PAIN.get(player.getUUID());
        if (pain == null || pain.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        Iterator<Pain> each = pain.iterator();
        while (each.hasNext()) {
            Pain due = each.next();
            if (due.at() > now) {
                break;
            }
            each.remove();
            float before = player.getHealth();
            float after = floor(before, before - due.amount());
            player.setHealth(after);
            player.serverLevel().getChunkSource().broadcastAndSend(player, new ClientboundHurtAnimationPacket(player));
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT,
                    SoundSource.PLAYERS, 0.8F, 0.7F);
            if (player.isDeadOrDying()) {
                player.die(due.source());
                return;
            }
        }
    }

    public static void forget(Player player) {
        PAIN.remove(player.getUUID());
        WATCHED.remove(player.getUUID());
    }
}
