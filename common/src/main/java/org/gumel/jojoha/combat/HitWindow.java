package org.gumel.jojoha.combat;

import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HitWindow {
    public static final int TICKS = 10;
    public static final float BUDGET = DamageScale.HIT_CEILING;

    private record Window(long start, float spent) {
    }

    private static final Map<UUID, Window> WINDOWS = new HashMap<>();
    private static final Map<UUID, Long> OPENED = new HashMap<>();

    private HitWindow() {
    }

    public static void init() {
        PlayerEvent.PLAYER_QUIT.register(player -> {
            WINDOWS.remove(player.getUUID());
            OPENED.remove(player.getUUID());
        });
    }

    public static void open(LivingEntity victim) {
        if (!(victim instanceof Player player) || victim.level().isClientSide()) {
            victim.invulnerableTime = 0;
            return;
        }
        Window window = live(player);
        if (window == null || window.spent() < BUDGET) {
            victim.invulnerableTime = 0;
            OPENED.put(player.getUUID(), player.level().getGameTime());
        }
    }

    public static float allowed(LivingEntity victim, float amount) {
        if (!(victim instanceof Player player)) {
            return amount;
        }
        Long opened = OPENED.remove(player.getUUID());
        if (opened == null || opened != player.level().getGameTime()) {
            return amount;
        }
        Window window = live(player);
        return window == null ? amount : Math.max(0F, Math.min(amount, BUDGET - window.spent()));
    }

    public static void landed(LivingEntity victim, float amount) {
        if (!(victim instanceof Player player) || amount <= 0F) {
            return;
        }
        Window window = live(player);
        WINDOWS.put(player.getUUID(), window == null
                ? new Window(player.level().getGameTime(), amount)
                : new Window(window.start(), window.spent() + amount));
    }

    private static Window live(Player player) {
        Window window = WINDOWS.get(player.getUUID());
        if (window != null && player.level().getGameTime() - window.start() >= TICKS) {
            WINDOWS.remove(player.getUUID());
            return null;
        }
        return window;
    }
}
