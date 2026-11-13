package org.gumel.jojoha.pillar;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.entity.VampireEntity;
import org.gumel.jojoha.network.packet.ReformPacket;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.vampire.VampireFx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class PillarBirth {
    public static final float START_CHANCE = 0.01F;
    public static final int REFORM_TICKS = 30 * 20;

    private static final int SHOW_EVERY = 10;
    private static final double SHOW_RADIUS = 96.0;
    private static final int SEEP_EVERY = 4;
    private static final int SQUELCH_EVERY = 25;
    private static final int CALM_EVERY = 20;
    private static final double CALM_RADIUS = 24.0;
    private static final double HOLD_DRIFT = 0.01;
    private static final int GROUND_SEARCH = 64;

    private static final Map<UUID, Vec3> PILES = new HashMap<>();

    public static Predicate<Player> clientView = player -> false;

    private PillarBirth() {
    }

    public static void init() {
        EntityEvent.LIVING_DEATH.register(PillarBirth::onDeath);
        EntityEvent.LIVING_HURT.register((victim, source, amount) ->
                victim instanceof ServerPlayer player && reforming(player)
                        && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                        ? EventResult.interruptFalse() : EventResult.pass());
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer served) {
                tick(served);
            }
        });
        PlayerEvent.PLAYER_JOIN.register(PillarBirth::show);
        PlayerEvent.PLAYER_QUIT.register(player -> PILES.remove(player.getUUID()));
        PlayerEvent.CHANGE_DIMENSION.register((player, from, to) -> PILES.remove(player.getUUID()));

        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> reforming(player)
                ? CompoundEventResult.interruptFalse(player.getItemInHand(hand))
                : CompoundEventResult.pass());
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) ->
                reforming(player) ? EventResult.interruptFalse() : EventResult.pass());
        InteractionEvent.LEFT_CLICK_BLOCK.register((player, hand, pos, face) ->
                reforming(player) ? EventResult.interruptFalse() : EventResult.pass());
        InteractionEvent.INTERACT_ENTITY.register((player, target, hand) ->
                reforming(player) ? EventResult.interruptFalse() : EventResult.pass());
        PlayerEvent.ATTACK_ENTITY.register((player, level, target, hand, hit) ->
                reforming(player) ? EventResult.interruptFalse() : EventResult.pass());
        BlockEvent.PLACE.register((level, pos, state, placer) ->
                placer instanceof Player player && reforming(player) ? EventResult.interruptFalse() : EventResult.pass());
        BlockEvent.BREAK.register((level, pos, state, player, xp) ->
                reforming(player) ? EventResult.interruptFalse() : EventResult.pass());
    }

    public static boolean reforming(Player player) {
        if (player.level().isClientSide()) {
            return clientView.test(player);
        }
        return player instanceof ServerPlayer served && PlayerDataAccess.get(served).reform > 0;
    }

    public static void rollIfUnset(ServerPlayer player, JojohaPlayerData data) {
        if (data.bornRolled) {
            return;
        }
        data.bornRolled = true;
        if (data.vampireStage != VampireStage.NONE
                || player.getRandom().nextFloat() >= JojohaServerConfig.PILLAR_START.chance()) {
            return;
        }
        data.vampireStage = VampireStage.PILLAR_MAN;
        player.sendSystemMessage(Component.translatable("message.jojoha.pillar_birth.born")
                .withStyle(ChatFormatting.GRAY));
    }

    public static boolean mark(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.vampireStage != VampireStage.NONE) {
            player.displayClientMessage(Component.translatable("message.jojoha.progenitor_mask.unworthy"), true);
            return false;
        }
        if (data.progenitor || data.reform > 0) {
            player.displayClientMessage(Component.translatable("message.jojoha.progenitor_mask.already"), true);
            return false;
        }
        data.progenitor = true;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        ServerLevel level = player.serverLevel();
        VampireFx.blood(level, player.getX(), player.getEyeY(), player.getZ(), 14, 0.2, 0.04);
        PillarFx.spray(level, PillarFx.flesh(), player.getEyePosition(), 8, 0.2, 0.02);
        level.playSound(null, player.blockPosition(), ModSounds.STONEMASK_STAB.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 0.8F, 0.5F);
        player.displayClientMessage(Component.translatable("message.jojoha.progenitor_mask.worn"), false);
        return true;
    }

    private static EventResult onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayer player)) {
            return EventResult.pass();
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.reform > 0) {
            data.reform = 0;
            data.progenitor = true;
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            PILES.remove(player.getUUID());
            show(player);
            return EventResult.pass();
        }
        if (!data.progenitor || data.vampireStage != VampireStage.NONE
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !byVampire(source.getEntity())) {
            return EventResult.pass();
        }

        data.progenitor = false;
        data.reform = REFORM_TICKS;
        StandSummonHandler.dismiss(player, data);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        player.setHealth(player.getMaxHealth());
        player.clearFire();
        player.stopRiding();
        player.setDeltaMovement(Vec3.ZERO);
        Vec3 pile = ground(player);
        PILES.put(player.getUUID(), pile);
        player.teleportTo(pile.x, pile.y, pile.z);

        ServerLevel level = player.serverLevel();
        for (double up = 0.0; up <= 1.6; up += 0.4) {
            VampireFx.blood(level, pile.x, pile.y + up, pile.z, 12, 0.3, 0.1);
        }
        PillarFx.spray(level, PillarFx.flesh(), pile.add(0, 0.6, 0), 16, 0.4, 0.08);
        level.playSound(null, player.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.2F, 0.4F);
        level.playSound(null, player.blockPosition(), SoundEvents.HONEY_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 0.5F);
        player.displayClientMessage(Component.translatable("message.jojoha.pillar_birth.collapsed"), false);
        show(player);
        return EventResult.interruptFalse();
    }

    private static boolean byVampire(Entity killer) {
        if (killer instanceof VampireEntity) {
            return true;
        }
        return killer instanceof ServerPlayer striker && Fate.undead(PlayerDataAccess.get(striker));
    }

    private static Vec3 ground(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos.MutableBlockPos at = player.blockPosition().mutable();
        for (int i = 0; i < GROUND_SEARCH && at.getY() > level.getMinBuildHeight(); i++) {
            BlockPos below = at.below();
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                return new Vec3(player.getX(), at.getY(), player.getZ());
            }
            at.move(0, -1, 0);
        }
        return player.position();
    }

    private static void tick(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.reform <= 0) {
            return;
        }
        Vec3 pile = PILES.computeIfAbsent(player.getUUID(), id -> ground(player));
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0F;
        player.setAirSupply(player.getMaxAirSupply());
        if (player.position().distanceToSqr(pile) > HOLD_DRIFT) {
            player.teleportTo(pile.x, pile.y, pile.z);
        }

        ServerLevel level = player.serverLevel();
        int left = --data.reform;
        float progress = 1F - (float) left / REFORM_TICKS;
        if (left % SEEP_EVERY == 0) {
            VampireFx.blood(level, pile.x, pile.y + 0.1 + progress * 1.4, pile.z, 2, 0.35 - progress * 0.15, 0.01);
        }
        if (left % SQUELCH_EVERY == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS,
                    0.6F, 0.4F + progress * 0.4F);
        }
        if (left % CALM_EVERY == 0) {
            AABB around = player.getBoundingBox().inflate(CALM_RADIUS);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, around, mob -> mob.getTarget() == player)) {
                mob.setTarget(null);
            }
        }
        if (left <= 0) {
            finish(player, data, pile);
            return;
        }
        PlayerDataAccess.set(player, data);
        if (left % SHOW_EVERY == 0) {
            show(player);
        }
    }

    private static void finish(ServerPlayer player, JojohaPlayerData data, Vec3 pile) {
        data.reform = 0;
        data.vampireStage = VampireStage.PILLAR_MAN;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        PILES.remove(player.getUUID());
        player.setHealth(player.getMaxHealth());

        ServerLevel level = player.serverLevel();
        PillarFx.spray(level, PillarFx.flesh(), pile.add(0, 1.0, 0), 24, 0.35, 0.06);
        VampireFx.ring(level, pile.x, pile.y + 0.15, pile.z, 1.8, 2);
        level.playSound(null, player.blockPosition(), ModSounds.VAMPIRE_REANIMATE.get(), SoundSource.PLAYERS, 1.0F, 0.5F);
        level.playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.8F, 0.6F);
        player.displayClientMessage(Component.translatable("message.jojoha.pillar_birth.reformed"), false);
        show(player);
    }

    public static void show(ServerPlayer player) {
        ReformPacket packet = new ReformPacket(player.getId(), Math.max(0, PlayerDataAccess.get(player).reform),
                REFORM_TICKS);
        double radius = SHOW_RADIUS * SHOW_RADIUS;
        for (ServerPlayer viewer : player.serverLevel().players()) {
            if (viewer == player || viewer.distanceToSqr(player) <= radius) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }
}
