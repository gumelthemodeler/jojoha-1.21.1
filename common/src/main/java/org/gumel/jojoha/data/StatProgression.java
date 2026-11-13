package org.gumel.jojoha.data;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public final class StatProgression {
    private static final float WORTH_PER_HEALTH = 0.25F;

    private static final int WORTH_PER_POINT = 30;

    private StatProgression() {
    }

    public static void init() {
        EntityEvent.LIVING_DEATH.register(StatProgression::onDeath);
    }

    private static EventResult onDeath(LivingEntity victim, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer killer)) {
            return EventResult.pass();
        }

        int worth = worthOf(victim);
        if (worth <= 0) {
            return EventResult.pass();
        }

        award(killer, worth);
        return EventResult.pass();
    }

    private static int worthOf(LivingEntity victim) {
        if (!(victim instanceof Enemy) && !(victim instanceof Player)) {
            return 0;
        }

        return Math.max(1, Math.round(victim.getMaxHealth() * WORTH_PER_HEALTH));
    }

    public static void awardUse(ServerPlayer player, int worth) {
        award(player, worth);
    }

    private static void award(ServerPlayer player, int worth) {
        JojohaPlayerData data = PlayerDataAccess.get(player);

        int total = data.killProgress + worth;
        int earned = total / WORTH_PER_POINT;

        data.killProgress = total % WORTH_PER_POINT;

        if (earned > 0) {
            data.availableStatPoints += earned;
        }

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        if (earned <= 0) {
            return;
        }

        player.displayClientMessage(Component.literal(
                "+" + earned + " stat point" + (earned == 1 ? "" : "s"))
                .withStyle(ChatFormatting.GOLD), true);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.35F, 1.6F);
    }

    public static float towardsNextPoint(JojohaPlayerData data) {
        return Math.min(1F, data.killProgress / (float) WORTH_PER_POINT);
    }
}
