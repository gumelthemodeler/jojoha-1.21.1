package org.gumel.jojoha.item;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.JojohaAdvancements;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandData;
import org.gumel.jojoha.data.StatPerk;
import org.gumel.jojoha.family.FamilyLine;
import org.gumel.jojoha.fortune.Gambler;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.StandRitualEffectPacket;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.Spirit;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandManifestation;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;

import java.util.List;

public final class StandArrowRitual {
    private static final float REJECTION_DAMAGE = 4.0F;

    private static final int STAB_DURATION_TICKS = 93;
    private static final int AWAKEN_DELAY_TICKS = 12;
    private static final int AWAKEN_TICK = STAB_DURATION_TICKS + AWAKEN_DELAY_TICKS;

    public static final int REVEAL_PAUSE_TICKS = 26;

    public static final int AWAKEN_RAYS_TICKS = 43;
    public static final int AWAKEN_GLOW_FADE_TICKS = 14;

    public static final int BOOM_AFTER_AWAKEN_TICKS = AWAKEN_RAYS_TICKS + AWAKEN_GLOW_FADE_TICKS;

    public static final int SKIN_RITUAL_TICKS = AWAKEN_TICK + BOOM_AFTER_AWAKEN_TICKS;

    public static final int RITUAL_DURATION_TICKS = AWAKEN_TICK + StandManifestation.TOTAL_TICKS;

    public static final int STRIKE_TICK = 43;

    public static final int SOUL_FLIGHT_TICKS = AWAKEN_TICK - STRIKE_TICK;

    private static final int SPIRAL_UNTIL_TICK = AWAKEN_TICK + StandManifestation.SKELETON_AT;

    private static final int CRUMBLE_SHARDS = 24;

    private static final int FLOAT_RISE_TICKS = 14;
    private static final double FLOAT_RISE_SPEED = 0.06;

    private static final int LAND_WATCH_TICKS = 40;

    private StandArrowRitual() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(StandArrowRitual::onPlayerTick);
    }

    public static ArrowStrike struck(ServerPlayer player, StandArrowItem arrow) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        ServerLevel level = player.serverLevel();
        if (data.stand.isPresent() || data.standArrowRitualTicks > 0) {
            return ArrowStrike.NOTHING;
        }
        if (Fate.barredFromStands(data)) {
            player.displayClientMessage(Component.translatable("message.jojoha.ultimate.no_stand"), true);
            return ArrowStrike.REFUSED;
        }
        if (data.worthiness < arrow.requiredWorthiness()) {
            player.hurt(level.damageSources().magic(), arrow.unworthyDamage(data.worthiness));
            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
            player.displayClientMessage(Component.translatable("message.jojoha.stand_arrow.unworthy",
                    arrow.requiredWorthiness()), true);
            return ArrowStrike.REFUSED;
        }
        float chance = arrow.oddsFor(player, data);
        if (chance < 1F && level.getRandom().nextFloat() >= chance) {
            boolean recognised = StatPerk.RECOGNISED.has(data);
            player.hurt(level.damageSources().magic(),
                    recognised ? REJECTION_DAMAGE * RECOGNISED_WOUND_SHARE : REJECTION_DAMAGE);
            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.7F);
            player.displayClientMessage(Component.translatable(recognised
                    ? "message.jojoha.stand_arrow.rejected_recognised" : "message.jojoha.stand_arrow.rejected"), true);
            return ArrowStrike.REFUSED;
        }
        arrive(player, data, "message.jojoha.stand_arrow.shot", rollStand(player, data));
        return ArrowStrike.AWAKENED;
    }

    public static void begin(ServerPlayer player, JojohaPlayerData data) {
        data.standArrowRitualTicks = duration(data);
        PlayerDataAccess.set(player, data);

        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.STAB);
    }

    private static int duration(JojohaPlayerData data) {
        return data.standArrowRitualSkin ? SKIN_RITUAL_TICKS : RITUAL_DURATION_TICKS;
    }

    private static void onPlayerTick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverPlayer);

        if (data.standArrowLandWatchTicks > 0) {
            tickLandWatch(serverPlayer, data);
        }

        if (data.standSkinSwapTicks > 0) {
            tickSkinSwap(serverPlayer, data);
        }

        if (data.standArrowRitualTicks <= 0) {
            return;
        }

        data.standArrowRitualTicks--;

        int remaining = data.standArrowRitualTicks;
        int duration = duration(data);
        int elapsed = duration - remaining;

        if (elapsed == STRIKE_TICK) {
            if (!data.standArrowRitualSkin && !pierce(serverPlayer, data)) {
                PlayerDataAccess.set(serverPlayer, data);
                return;
            }
            NetworkHandler.broadcastRitualEffect(serverPlayer, StandRitualEffectPacket.Effect.PIERCED);
            if (!data.standArrowRitualSkin) {
                NetworkHandler.broadcastRitualEffect(serverPlayer, StandRitualEffectPacket.Effect.SOUL_CALLED);
            }
            serverPlayer.serverLevel().playSound(null, serverPlayer.blockPosition(), SoundEvents.PLAYER_HURT,
                    SoundSource.PLAYERS, 0.7F, 0.7F);
        }

        if (elapsed >= STRIKE_TICK && (data.standArrowRitualSkin || elapsed < SPIRAL_UNTIL_TICK)) {
            spawnTransformSpiral(serverPlayer, elapsed, duration);
        }

        if (data.standArrowRitualSkin && elapsed == STRIKE_TICK) {
            StandSummonHandler.forceSummon(serverPlayer, data);
            PlayerDataAccess.sync(serverPlayer);
        }

        if (data.standArrowRitualSkin && elapsed == AWAKEN_TICK) {
            StandEntity burning = StandSummonHandler.findStand(serverPlayer, data);
            if (burning != null) {
                burning.beginSkinSwap();
            }
        }

        if (data.standArrowRitualSkin && elapsed > AWAKEN_TICK) {
            StandEntity shedding = StandSummonHandler.findStand(serverPlayer, data);
            if (shedding != null) {
                spawnSkinMotes(serverPlayer.serverLevel(), shedding, elapsed - AWAKEN_TICK);
            }
        }

        if (elapsed == AWAKEN_TICK) {
            NetworkHandler.broadcastRitualEffect(serverPlayer,
                    data.standArrowRitualSkin
                            ? StandRitualEffectPacket.Effect.SKIN_RAYS
                            : StandRitualEffectPacket.Effect.AWAKENING_RAYS);
            serverPlayer.serverLevel().playSound(null, serverPlayer.blockPosition(), ModSounds.STAND_AWAKEN.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!data.standArrowRitualSkin) {
                awaken(serverPlayer, data);
            }
        }

        if (elapsed == AWAKEN_TICK) {
            beginFloat(serverPlayer);
        } else if (elapsed > AWAKEN_TICK) {
            tickFloat(serverPlayer, elapsed - AWAKEN_TICK);
        }

        if (remaining == 0) {
            endFloat(serverPlayer, data);
            if (data.standArrowRitualSkin) {
                beginSkinSwap(serverPlayer, data);
            }
        }

        PlayerDataAccess.set(serverPlayer, data);
    }

    private static boolean pierce(ServerPlayer player, JojohaPlayerData data) {
        StandArrowItem source = sourceItem(data);
        float chance = source.oddsFor(player, data);
        boolean again = Gambler.secondRoll(player);
        if (chance >= 1F || player.serverLevel().getRandom().nextFloat() < chance
                || again && player.serverLevel().getRandom().nextFloat() < chance) {
            return true;
        }

        boolean kept = favourAnswers(player, data);
        if (!kept) {
            consumeArrow(player, data);
        }
        crumble(player, source);
        data.standArrowRitualTicks = 0;
        rejected(player, data);
        return false;
    }

    private static void crumble(ServerPlayer player, StandArrowItem source) {
        player.serverLevel().sendParticles(
                new ItemParticleOption(
                        ParticleTypes.ITEM, new ItemStack(source)),
                player.getX(), player.getY() + player.getBbHeight() * 0.6, player.getZ(),
                CRUMBLE_SHARDS, 0.2, 0.2, 0.2, 0.08);
    }

    private static void beginFloat(ServerPlayer player) {
        player.setNoGravity(true);
        player.fallDistance = 0F;
    }

    private static void tickFloat(ServerPlayer player, int floatTick) {
        double rise = floatTick <= FLOAT_RISE_TICKS ? FLOAT_RISE_SPEED : 0.0;
        Vec3 motion = player.getDeltaMovement();
        Vec3 held = new Vec3(motion.x, rise, motion.z);

        player.setDeltaMovement(held);
        player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), held));
        player.fallDistance = 0F;
    }

    private static void endFloat(ServerPlayer player, JojohaPlayerData data) {
        player.setNoGravity(false);
        player.fallDistance = 0F;
        data.standArrowLandWatchTicks = LAND_WATCH_TICKS;
    }

    private static void tickLandWatch(ServerPlayer player, JojohaPlayerData data) {
        data.standArrowLandWatchTicks--;

        if (player.onGround()) {
            data.standArrowLandWatchTicks = 0;
            NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.LAND);
        } else if (player.isInWater()) {
            data.standArrowLandWatchTicks = 0;
        }

        PlayerDataAccess.set(player, data);
    }

    private static void awaken(ServerPlayer player, JojohaPlayerData data) {
        if (data.stand.isPresent() || Fate.barredFromStands(data)) {
            return;
        }

        consumeArrow(player, data);
        data.standArrowRitualShard = false;
        arrive(player, data, null, rollStand(player, data));
        FamilyLine.bloodlineStirs(player);
    }

    public static void grant(ServerPlayer player, String message) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.stand.isPresent()) {
            return;
        }
        arrive(player, data, message, rollStand(player, data));
    }

    public static void grant(ServerPlayer player, ResourceLocation standId, String message) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.stand.isPresent()) {
            return;
        }
        arrive(player, data, message, standId);
    }

    private static void arrive(ServerPlayer player, JojohaPlayerData data, String message,
                               ResourceLocation standId) {
        if (Fate.barredFromStands(data)) {
            return;
        }
        data.stand = new StandData(standId, 0, 5, 5, 5, 5, 5, 0);
        Spirit.arrived(player, data, standId);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        if (message != null) {
            player.sendSystemMessage(Component.translatable(message));
        }

        if (StandManifestation.begin(player, data.stand)) {
            if (data.standArrowRitualTicks <= 0) {
                player.serverLevel().playSound(null, player.blockPosition(), ModSounds.STAND_AWAKEN.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return;
        }

        JojohaAdvancements.award(player, StandManifestation.AWAKENED_ADVANCEMENT);
        spawnAwakeningEffects(player.serverLevel(), player);
        NetworkHandler.broadcastStandNamed(player, data.stand.standId());
    }

    private static ResourceLocation rollStand(ServerPlayer player, JojohaPlayerData data) {
        List<ResourceLocation> pool = Spirit.pool();
        if (pool.isEmpty()) {
            return StandTypes.STAR_PLATINUM_ID;
        }
        return Spirit.choose(player, data, pool);
    }

    public static final int SKIN_SWAP_GAP_TICKS = 16;

    private static final int SKIN_MOTES_PER_TICK = 7;
    private static final double SKIN_MOTE_RADIUS = 0.75;
    private static final double SKIN_MOTE_DRIFT = 0.06;

    private static final int SKIN_BURST_PARTICLES = 110;
    private static final double SKIN_BURST_MIN_SPEED = 0.3;
    private static final double SKIN_BURST_SPEED_SPREAD = 0.5;

    private static void beginSkinSwap(ServerPlayer player, JojohaPlayerData data) {
        consumeSkinArrow(player);
        data.standArrowRitualSkin = false;
        data.standSkinSwapTicks = SKIN_SWAP_GAP_TICKS;

        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand != null) {
            spawnSkinBurst(player.serverLevel(), stand);
        }

        StandSummonHandler.dismissImmediately(player, data);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    private static void tickSkinSwap(ServerPlayer player, JojohaPlayerData data) {
        data.standSkinSwapTicks--;
        if (data.standSkinSwapTicks > 0) {
            return;
        }

        data.stand = data.stand.withSkin(rollSkin(player, data));
        StandSummonHandler.forceSummon(player, data);

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.SKIN_NAMED);
    }

    private static int rollSkin(ServerPlayer player, JojohaPlayerData data) {
        StandType type = StandTypes.byIdOrDefault(data.stand.standId());
        int count = type.skinCount();
        if (type.rollableSkinCount() < 2) {
            return data.stand.skin();
        }

        int current = Mth.clamp(data.stand.skin(), 0, count - 1);

        float total = 0F;
        for (int skin = 0; skin < count; skin++) {
            if (skin != current) {
                total += type.skinWeight(skin);
            }
        }

        if (total <= 0F) {
            return current;
        }

        float roll = player.serverLevel().getRandom().nextFloat() * total;
        int last = current;
        for (int skin = 0; skin < count; skin++) {
            if (skin == current || !type.rollableSkin(skin)) {
                continue;
            }
            last = skin;
            roll -= type.skinWeight(skin);
            if (roll <= 0F) {
                return skin;
            }
        }

        return last;
    }

    private static void spawnSkinMotes(ServerLevel level, StandEntity stand, int elapsed) {
        RandomSource random = level.getRandom();
        float through = Mth.clamp(elapsed / (float) BOOM_AFTER_AWAKEN_TICKS, 0F, 1F);
        double radius = SKIN_MOTE_RADIUS * (1F - through * 0.6F);

        for (int i = 0; i < SKIN_MOTES_PER_TICK; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double height = random.nextDouble() * stand.getBbHeight();

            level.sendParticles(
                    i % 2 == 0 ? ModRegistries.STAND_AWAKEN_BLUE.get() : ModRegistries.STAND_AWAKEN_PINK.get(),
                    stand.getX() + Math.cos(angle) * radius,
                    stand.getY() + height,
                    stand.getZ() + Math.sin(angle) * radius,
                    1, 0, SKIN_MOTE_DRIFT, 0, 0.02);
        }
    }

    private static void spawnSkinBurst(ServerLevel level, StandEntity stand) {
        RandomSource random = level.getRandom();
        double centreY = stand.getY() + stand.getBbHeight() * 0.5;

        for (int i = 0; i < SKIN_BURST_PARTICLES; i++) {
            double theta = random.nextDouble() * Math.PI * 2.0;
            double z = random.nextDouble() * 2.0 - 1.0;
            double r = Math.sqrt(1.0 - z * z);
            double speed = SKIN_BURST_MIN_SPEED + random.nextDouble() * SKIN_BURST_SPEED_SPREAD;

            level.sendParticles(
                    i % 2 == 0 ? ModRegistries.STAND_AWAKEN_BLUE.get() : ModRegistries.STAND_AWAKEN_PINK.get(),
                    stand.getX(), centreY, stand.getZ(),
                    0, Math.cos(theta) * r * speed, z * speed, Math.sin(theta) * r * speed, 1);
        }

        level.playSound(null, stand.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 0.7F, 1.6F);
    }

    private static void consumeSkinArrow(ServerPlayer player) {
        ItemStack inHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (inHand.is(ModItems.FRACTURED_SKIN_ARROW.get())) {
            inHand.shrink(1);
            return;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.FRACTURED_SKIN_ARROW.get())) {
                stack.shrink(1);
                return;
            }
        }
    }

    private static StandArrowItem sourceItem(JojohaPlayerData data) {
        return (StandArrowItem) (data.standArrowRitualShard
                ? ModItems.STAND_ARROW_SHARD.get()
                : ModItems.STAND_ARROW.get());
    }

    private static final float RECOGNISED_WOUND_SHARE = 0.4F;

    private static void rejected(ServerPlayer player, JojohaPlayerData data) {
        NetworkHandler.broadcastRitualEffect(player, StandRitualEffectPacket.Effect.AWAKENING_REFUSED);
        data.standArrowRitualShard = false;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        boolean recognised = StatPerk.RECOGNISED.has(data);
        player.hurt(player.serverLevel().damageSources().magic(),
                recognised ? REJECTION_DAMAGE * RECOGNISED_WOUND_SHARE : REJECTION_DAMAGE);

        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK,
                SoundSource.PLAYERS, 1.0F, 0.7F);
        player.displayClientMessage(Component.translatable(
                StatPerk.HEAVENS_FAVOUR.has(data) ? "message.jojoha.stand_arrow.rejected_kept"
                        : recognised ? "message.jojoha.stand_arrow.rejected_recognised"
                        : "message.jojoha.stand_arrow.rejected"), true);
    }

    private static boolean favourAnswers(ServerPlayer player, JojohaPlayerData data) {
        if (!StatPerk.HEAVENS_FAVOUR.has(data)) {
            return false;
        }
        long now = player.level().getGameTime();
        if (data.isMoveOnCooldown(StandArrowItem.FAVOUR_SPENT, now)) {
            return false;
        }
        data.setMoveCooldown(StandArrowItem.FAVOUR_SPENT, now, DAY_TICKS);
        return true;
    }

    private static final int DAY_TICKS = 24000;

    private static void consumeArrow(ServerPlayer player, JojohaPlayerData data) {
        Item spent = sourceItem(data);

        ItemStack inHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (inHand.is(spent)) {
            inHand.shrink(1);
            return;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(spent)) {
                stack.shrink(1);
                return;
            }
        }
    }

    private static final int BURST_PARTICLES = 140;
    private static final double BURST_MIN_SPEED = 0.35;
    private static final double BURST_SPEED_SPREAD = 0.45;

    private static final int SPIRAL_MOTES_PER_TICK = 5;
    private static final double SPIRAL_RADIUS = 0.85;

    private static void spawnTransformSpiral(ServerPlayer player, int elapsed, int duration) {
        ServerLevel level = player.serverLevel();
        RandomSource random = level.getRandom();

        float intensity = Mth.clamp(elapsed / (float) duration, 0.25F, 1F);
        int motes = Math.max(1, Math.round(SPIRAL_MOTES_PER_TICK * intensity));

        for (int i = 0; i < motes; i++) {
            double angle = (elapsed * SPIRAL_MOTES_PER_TICK + i) * 2.39996;
            double radius = SPIRAL_RADIUS * (0.55 + random.nextDouble() * 0.45);

            double x = player.getX() + Math.cos(angle) * radius;
            double y = player.getY() + random.nextDouble() * 0.4;
            double z = player.getZ() + Math.sin(angle) * radius;

            level.sendParticles(ModRegistries.STAND_TRANSFORM.get(), x, y, z, 0, angle, 0.0, radius, 1.0);
        }
    }

    private static void spawnAwakeningEffects(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();
        SimpleParticleType burst = ModRegistries.STAND_AWAKEN.get();
        double originY = player.getY() + player.getBbHeight() * 0.5;

        for (int i = 0; i < BURST_PARTICLES; i++) {
            double dx = random.nextGaussian();
            double dy = random.nextGaussian();
            double dz = random.nextGaussian();
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 1.0E-4) {
                continue;
            }

            double speed = BURST_MIN_SPEED + random.nextDouble() * BURST_SPEED_SPREAD;
            dx = dx / length * speed;
            dy = dy / length * speed;
            dz = dz / length * speed;

            level.sendParticles(burst, player.getX(), originY, player.getZ(), 0, dx, dy, dz, 1.0);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
