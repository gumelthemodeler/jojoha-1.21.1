package org.gumel.jojoha.hamon.moves;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.EnergySystem;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.skill.Harmless;
import org.gumel.jojoha.skill.MoveNames;
import org.gumel.jojoha.stand.skill.StandBeat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NextLineMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "next_line");
    public static final NextLineMove INSTANCE = new NextLineMove();

    public static final int READING_TICKS = 100;
    public static final int MISS_TICKS = 40;
    public static final double RANGE = 24.0;
    public static final double HEARD_RANGE = 48.0;
    public static final float DRAIN_SHARE = 0.1F;
    public static final int GUH_DELAY_TICKS = 6;

    private static final int COOLDOWN_TICKS = 400;
    private static final float ENERGY_COST = 18F;

    private record Miss(UUID from, long until) {
    }

    private static final Map<UUID, Long> READING_UNTIL = new HashMap<>();
    private static final Map<UUID, Miss> MISSES = new HashMap<>();

    private NextLineMove() {
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
        READING_UNTIL.put(player.getUUID(), player.level().getGameTime() + READING_TICKS);
        MovementFx.pose(player, MovementFx.GUARD);
        NetworkHandler.broadcastMenace(player);
        Vec3 hand = HamonFx.mainHand(player);
        HamonFx.fineSpark(player.serverLevel(), hand.x, hand.y, hand.z, 4, 0.15, 0.02);
        return true;
    }

    public static boolean reading(ServerPlayer player) {
        Long until = READING_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (player.level().getGameTime() > until) {
            READING_UNTIL.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static void cast(ServerPlayer caster, ResourceLocation move) {
        if (READING_UNTIL.isEmpty() || move == null || move.equals(ID) || Harmless.is(move)) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(caster);
        Component name = Component.translatable(MoveNames.key(data, move));
        for (ServerPlayer reader : caster.serverLevel().players()) {
            if (reader != caster && reading(reader) && reader.distanceToSqr(caster) <= RANGE * RANGE) {
                call(reader, caster, name);
                return;
            }
        }
    }

    public static boolean dodges(ServerPlayer guard, DamageSource source) {
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (!(attacker instanceof LivingEntity foe) || foe == guard
                || !(direct == foe || direct instanceof Projectile)) {
            return false;
        }
        long now = guard.level().getGameTime();
        Miss miss = MISSES.get(guard.getUUID());
        if (miss != null && miss.from.equals(foe.getUUID()) && now <= miss.until) {
            MISSES.remove(guard.getUUID());
            slip(guard, foe);
            return true;
        }
        if (!reading(guard)) {
            return false;
        }
        call(guard, foe, plainName(foe));
        MISSES.remove(guard.getUUID());
        slip(guard, foe);
        return true;
    }

    private static void call(ServerPlayer reader, LivingEntity foe, Component name) {
        READING_UNTIL.remove(reader.getUUID());
        long now = reader.level().getGameTime();
        MISSES.put(reader.getUUID(), new Miss(foe.getUUID(), now + MISS_TICKS));
        say(reader, Component.translatable("move.jojoha.next_line.reveal", name));
        NetworkHandler.broadcastMenace(reader);
        if (foe instanceof ServerPlayer caught) {
            StandBeat.at(GUH_DELAY_TICKS, caught,
                    () -> say(caught, Component.translatable("move.jojoha.next_line.caught", name)));
            drain(caught);
        }
    }

    private static void drain(ServerPlayer caught) {
        JojohaPlayerData data = PlayerDataAccess.get(caught);
        float floor = EnergySystem.spentAt(data);
        if (data.standEnergy > floor) {
            data.standEnergy = Math.max(floor, data.standEnergy - data.maxStandEnergy() * DRAIN_SHARE);
        }
        data.specEnergy = Math.max(0F, data.specEnergy - JojohaPlayerData.MAX_SPEC_ENERGY * DRAIN_SHARE);
        PlayerDataAccess.set(caught, data);
        PlayerDataAccess.sync(caught);
    }

    private static void slip(ServerPlayer guard, LivingEntity foe) {
        ServerLevel level = guard.serverLevel();
        level.playSound(null, guard.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS,
                0.8F, 1.3F);
        Vec3 at = guard.position().add(0.0, guard.getBbHeight() * 0.6, 0.0);
        HamonFx.fineSpark(level, at.x, at.y, at.z, 6, 0.3, 0.03);
    }

    private static Component plainName(LivingEntity foe) {
        if (foe instanceof Player) {
            ItemStack held = foe.getMainHandItem();
            return held.isEmpty() ? Component.translatable("move.jojoha.next_line.punch") : held.getHoverName();
        }
        return Component.translatable("move.jojoha.next_line.attack");
    }

    private static void say(ServerPlayer speaker, Component text) {
        Component line = Component.translatable("chat.type.text", speaker.getDisplayName(), text);
        for (ServerPlayer listener : speaker.serverLevel().players()) {
            if (listener.distanceToSqr(speaker) <= HEARD_RANGE * HEARD_RANGE) {
                listener.sendSystemMessage(line);
            }
        }
    }

    public static void forget(UUID player) {
        READING_UNTIL.remove(player);
        MISSES.remove(player);
    }
}
