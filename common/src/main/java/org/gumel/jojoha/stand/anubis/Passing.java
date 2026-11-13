package org.gumel.jojoha.stand.anubis;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModSounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Passing {
    static final int LET_GO_TICKS = 60;

    private static final Map<UUID, Long> ASKED = new HashMap<>();

    private Passing() {
    }

    static void init() {
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity.level() instanceof ServerLevel level) {
                fell(level, entity);
            }
            return EventResult.pass();
        });
    }

    public static ItemStack sealed() {
        return new ItemStack(ModItems.ANUBIS.get());
    }

    static boolean bond(ServerPlayer player, ItemStack stack) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (Anubis.is(data)) {
            player.displayClientMessage(Component.translatable("message.jojoha.anubis.already"), true);
            return false;
        }
        if (Fate.barredFromStands(data)) {
            player.displayClientMessage(Component.translatable("message.jojoha.ultimate.no_stand"), true);
            return false;
        }
        if (data.discipline == Discipline.HAMON) {
            player.displayClientMessage(Component.translatable("message.jojoha.anubis.hamon"), true);
            return false;
        }
        stack.shrink(1);
        Anubis.bond(data);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        player.displayClientMessage(Component.translatable("message.jojoha.anubis.bonded"), true);
        player.serverLevel().playSound(null, player.blockPosition(), ModSounds.ANUBIS_SPEC_CHANGE.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    public static void letGo(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        Long asked = ASKED.remove(player.getUUID());
        if (asked == null || now - asked > LET_GO_TICKS || now < asked) {
            ASKED.put(player.getUUID(), now);
            player.displayClientMessage(Component.translatable("message.jojoha.anubis.let_go_ask"), true);
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!Anubis.is(data)) {
            return;
        }
        release(player, data);
        handBack(player);
        player.displayClientMessage(Component.translatable("message.jojoha.anubis.let_go"), true);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN.value(),
                SoundSource.PLAYERS, 0.7F, 0.8F);
    }

    static void handBack(ServerPlayer player) {
        ItemStack blade = sealed();
        if (!player.getInventory().add(blade)) {
            drop(player.serverLevel(), player.position(), blade);
        }
    }

    private static void release(ServerPlayer player, JojohaPlayerData data) {
        Anubis.unbond(player, data);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    private static void fell(ServerLevel level, LivingEntity dead) {
        if (!(dead.getKillCredit() instanceof Player killer) || killer == dead
                || Anubis.is(PlayerDataAccess.get(killer)) || !(dead instanceof ServerPlayer player)) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!Anubis.is(data)) {
            return;
        }
        release(player, data);
        drop(level, dead.position(), sealed());
        killer.displayClientMessage(Component.translatable("message.jojoha.anubis.won", dead.getDisplayName()), true);
        level.playSound(null, dead.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                1.0F, 0.7F);
    }

    static void drop(ServerLevel level, Vec3 at, ItemStack blade) {
        ItemEntity item = new ItemEntity(level, at.x, at.y + 0.5, at.z, blade);
        item.setDefaultPickUpDelay();
        item.setUnlimitedLifetime();
        level.addFreshEntity(item);
    }

    static void forget(Player player) {
        ASKED.remove(player.getUUID());
    }
}
