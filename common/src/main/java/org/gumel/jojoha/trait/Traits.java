package org.gumel.jojoha.trait;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModTraits;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Traits {
    public static final float LEGENDARY_CHANCE = 0.03F;

    public static final int HIGH_WORTHINESS = 50;

    public static final int LUCK_LEVELS = 4;
    public static final int LUCK_PAYS_AT = 2;

    private Traits() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(TraitEffects::tick);
        PlayerEvent.PLAYER_QUIT.register(TraitEffects::forget);
        EntityEvent.ADD.register((entity, level) -> {
            TraitEffects.steadyHand(entity);
            return EventResult.pass();
        });
    }

    public static boolean has(JojohaPlayerData data, ModTraits.Trait trait) {
        return trait != null && data.trait != null && data.trait.equals(trait.id());
    }

    public static boolean has(Player player, ModTraits.Trait trait) {
        return player instanceof ServerPlayer server && has(PlayerDataAccess.get(server), trait);
    }

    public static boolean rollIfUnset(ServerPlayer player, JojohaPlayerData data) {
        if (data.trait != null) {
            return false;
        }
        return draw(player, data, null, "message.jojoha.trait.rolled", 1F) != null;
    }

    public static ModTraits.Trait reroll(ServerPlayer player, JojohaPlayerData data, float rareOdds) {
        return draw(player, data, data.trait, null, rareOdds);
    }

    private static ModTraits.Trait draw(ServerPlayer player, JojohaPlayerData data,
                                ResourceLocation excluded, String message,
                                float rareOdds) {
        List<ModTraits.Trait> ordinary = new ArrayList<>();
        List<ModTraits.Trait> legendary = new ArrayList<>();
        for (ModTraits.Trait trait : ModTraits.all().values()) {
            if (trait.id().equals(excluded)) {
                continue;
            }
            (trait.legendary() ? legendary : ordinary).add(trait);
        }
        if (ordinary.isEmpty() && legendary.isEmpty()) {
            return null;
        }

        List<ModTraits.Trait> pool = ordinary;
        for (ModTraits.Trait rare : legendary) {
            if (player.getRandom().nextFloat() < JojohaServerConfig.LEGENDARY_TRAIT.chance() * rareOdds) {
                pool = List.of(rare);
                break;
            }
        }
        if (pool.isEmpty()) {
            pool = legendary;
        }

        ModTraits.Trait rolled = pool.get(player.getRandom().nextInt(pool.size()));
        data.trait = rolled.id();

        if (message == null) {
            return rolled;
        }
        player.sendSystemMessage(Component.translatable(
                message,
                Component.translatable(rolled.nameKey())
                        .withStyle(ChatFormatting.GOLD),
                Component.translatable(rolled.descriptionKey())));
        return rolled;
    }

    public static int killersLuck(Player player) {
        long day = player.level().getDayTime() / 24000L;
        return new Random(day * 1000003L ^ player.getUUID().getMostSignificantBits())
                .nextInt(LUCK_LEVELS);
    }

    public static int luckPayout(Player player) {
        return Math.max(0, killersLuck(player) - LUCK_PAYS_AT + 1);
    }
}
