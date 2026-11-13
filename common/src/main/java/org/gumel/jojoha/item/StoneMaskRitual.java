package org.gumel.jojoha.item;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.JojohaAdvancements;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.StandRitualEffectPacket;
import org.gumel.jojoha.registry.ModComponents;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.ultimate.PerfectBiology;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class StoneMaskRitual {
    private static final Set<UUID> DUDS = new HashSet<>();

    private static final int SEAT_TICKS = 25;

    private static final int EQUIP_TICKS = 36;

    private static final int DWELL_TICKS = 50;

    private static final int TURN_TICKS = 45;

    private static final int TRANSFORM_TICKS = 150;

    private StoneMaskRitual() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                tick(serverPlayer);
            }
        });
    }

    public static void begin(ServerPlayer player, JojohaPlayerData data) {
        data.stoneMaskRitualTicks = EQUIP_TICKS + DWELL_TICKS + TURN_TICKS + TRANSFORM_TICKS;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.MASK_EQUIP);
    }

    private static void tick(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);

        if (data.stoneMaskRitualTicks <= 0) {
            return;
        }

        int remaining = --data.stoneMaskRitualTicks;
        int elapsed = (EQUIP_TICKS + DWELL_TICKS + TURN_TICKS + TRANSFORM_TICKS) - remaining;

        if (elapsed < SEAT_TICKS && !holdingMask(player, data)) {
            cancel(player, data);
            return;
        }

        if (elapsed == SEAT_TICKS) {
            seat(player, data, aja(data));
        } else if (elapsed == EQUIP_TICKS + DWELL_TICKS) {
            turn(player);
        } else if (elapsed == EQUIP_TICKS + DWELL_TICKS + TURN_TICKS) {
            transform(player, data);
        } else if (remaining <= 0) {
            finish(player, data);
        } else if (elapsed > EQUIP_TICKS + DWELL_TICKS + TURN_TICKS) {
            spawnBloodMotes(player.serverLevel(), player);
        }

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    private static boolean aja(JojohaPlayerData data) {
        return data.vampireStage == VampireStage.PILLAR_MAN;
    }

    private static Item mask(JojohaPlayerData data) {
        return aja(data) ? ModItems.RED_STONE_MASK.get() : ModItems.STONE_MASK.get();
    }

    private static void seat(ServerPlayer player, JojohaPlayerData data, boolean aja) {
        if (consumeMask(player, mask(data)) && aja) {
            DUDS.add(player.getUUID());
        }
        data.stoneMaskWorn = true;

        seatSound(player.serverLevel(), player);
        NetworkHandler.broadcastRitualEffect(player, aja
                ? StandRitualEffectPacket.Effect.AJA_SEATED : StandRitualEffectPacket.Effect.MASK_SEATED);
    }

    private static void seatSound(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.blockPosition(), ModSounds.STONEMASK_STAB.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void turn(ServerPlayer player) {
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.STONE_PLACE,
                SoundSource.PLAYERS, 1.0F, 0.4F);

        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.MASK_TURNING);
    }

    private static void transform(ServerPlayer player, JojohaPlayerData data) {
        if (aja(data) && DUDS.remove(player.getUUID())) {
            fake(player, data);
            return;
        }
        if (aja(data)) {
            data.vampireStage = VampireStage.ULTIMATE_LIFEFORM;
            data.ultimateWings = false;
            data.dropUnequippable();
            PerfectBiology.cleanse(player);
        } else {
            data.vampireStage = VampireStage.VAMPIRE;
            JojohaAdvancements.award(player, "part1/the_change");
        }

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), ModSounds.VAMPIRE_SPEC_CHANGE.get(),
                SoundSource.PLAYERS, 0.7F, 1.4F);

        spawnBloodBurst(level, player);
        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.MASK_AWAKENING);
    }

    private static boolean holdingMask(ServerPlayer player, JojohaPlayerData data) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).is(mask(data));
    }

    private static void fake(ServerPlayer player, JojohaPlayerData data) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.5F);
        level.playSound(null, player.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.75F, 0.05F, 0.05F), 1.2F),
                player.getX(), player.getEyeY(), player.getZ(), 20, 0.25, 0.2, 0.25, 0.02);
        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getEyeY(), player.getZ(), 12, 0.2, 0.2, 0.2, 0.02);
        player.displayClientMessage(Component.translatable("message.jojoha.mask.fake_aja"), true);
        cancel(player, data);
    }

    private static void cancel(ServerPlayer player, JojohaPlayerData data) {
        DUDS.remove(player.getUUID());
        data.stoneMaskRitualTicks = 0;
        data.stoneMaskWorn = false;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.MASK_CANCELLED);
    }

    private static void finish(ServerPlayer player, JojohaPlayerData data) {
        data.stoneMaskWorn = false;

        ServerLevel level = player.serverLevel();

        Vec3 look = player.getLookAngle();
        Vec3 burst = new Vec3(look.x, 0, look.z).normalize()
                .scale(BURST_FORWARD).add(0, BURST_LIFT, 0);

        FallingMask falling = new FallingMask(level,
                player.getX(), player.getEyeY() - 0.15, player.getZ(), burst,
                data.vampireStage == VampireStage.ULTIMATE_LIFEFORM);
        level.addFreshEntity(falling);

        level.playSound(null, player.blockPosition(), SoundEvents.STONE_PLACE,
                SoundSource.PLAYERS, 0.8F, 0.5F);

        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.MASK_SPENT);
    }

    private static final double BURST_FORWARD = 0.24;
    private static final double BURST_LIFT = 0.28;

    private static void spawnBloodBurst(ServerLevel level, ServerPlayer player) {
        double originY = player.getY() + player.getBbHeight() * 0.6;
        var random = level.getRandom();

        for (int i = 0; i < BURST_PARTICLES; i++) {
            double dx = random.nextGaussian();
            double dy = random.nextGaussian();
            double dz = random.nextGaussian();
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 1.0E-4) {
                continue;
            }

            double speed = BURST_MIN_SPEED + random.nextDouble() * BURST_SPEED_SPREAD;
            level.sendParticles(ModRegistries.STAND_AWAKEN_RED.get(),
                    player.getX(), originY, player.getZ(), 0,
                    dx / length * speed, dy / length * speed, dz / length * speed, 1.0);
        }
    }

    private static void spawnBloodMotes(ServerLevel level, ServerPlayer player) {
        var random = level.getRandom();
        for (int i = 0; i < MOTES_PER_TICK; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = MOTE_RADIUS * (0.4 + random.nextDouble() * 0.6);
            level.sendParticles(ModRegistries.BLOOD_MOTE.get(),
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + random.nextDouble() * player.getBbHeight(),
                    player.getZ() + Math.sin(angle) * radius,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static final int MOTES_PER_TICK = 2;
    private static final double MOTE_RADIUS = 1.1;

    private static final int BURST_PARTICLES = 140;
    private static final double BURST_MIN_SPEED = 0.35;
    private static final double BURST_SPEED_SPREAD = 0.55;

    private static boolean consumeMask(ServerPlayer player, Item mask) {
        ItemStack inHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (inHand.is(mask)) {
            boolean defected = Boolean.TRUE.equals(inHand.get(ModComponents.DEFECTED.get()));
            inHand.shrink(1);
            return defected;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(mask)) {
                boolean defected = Boolean.TRUE.equals(stack.get(ModComponents.DEFECTED.get()));
                stack.shrink(1);
                return defected;
            }
        }
        return false;
    }
}
