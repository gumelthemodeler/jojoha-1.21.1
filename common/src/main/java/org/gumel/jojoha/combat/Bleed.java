package org.gumel.jojoha.combat;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.body.Limbs;
import org.gumel.jojoha.hamon.HamonState;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.FlairCuePacket;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.speech.Speech;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class Bleed {
    public static final int TICKS = 100;

    public static final int MAX_AMPLIFIER = 3;

    public static final int MOB_CAP = 2;

    private static final float[] TOLL = {3.0F, 6.0F, 9.0F, 10.0F};

    public static final int NICK_TICKS = 10;

    public static final int BITE_TICKS = 30;

    public static final int EASE_TICKS = 20;

    private static final Map<LivingEntity, Long> NICKED = new WeakHashMap<>();

    private static final Map<LivingEntity, Long> BITTEN = new WeakHashMap<>();

    private static final Map<LivingEntity, Long> EASED = new WeakHashMap<>();

    private static final Map<LivingEntity, Boolean> EASING = new WeakHashMap<>();

    private Bleed() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> {
            if (EASING.isEmpty()) {
                return;
            }
            List<LivingEntity> due = new ArrayList<>(EASING.keySet());
            EASING.clear();
            for (LivingEntity victim : due) {
                lighten(victim);
            }
        });
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer server && HamonState.isBreathing(server)) {
                ease(server);
            }
        });
    }

    public static int apply(LivingEntity victim) {
        MobEffectInstance open = victim.getEffect(ModEffects.bleed());

        if (open == null) {
            victim.addEffect(new MobEffectInstance(ModEffects.bleed(), TICKS, 0, false, false, true));
            spurt(victim, 0);
            return 0;
        }

        int deeper = Math.min(MAX_AMPLIFIER, open.getAmplifier() + 1);
        int remaining = open.getDuration();
        if (deeper == open.getAmplifier()) {
            return open.getAmplifier();
        }

        victim.addEffect(new MobEffectInstance(ModEffects.bleed(), remaining, deeper,
                false, false, true));
        spurt(victim, deeper);
        if (deeper == MAX_AMPLIFIER && victim instanceof ServerPlayer player) {
            warn(player);
        }
        return deeper;
    }

    public static void bite(LivingEntity victim, int cap) {
        MobEffectInstance open = victim.getEffect(ModEffects.bleed());
        if (open != null && open.getAmplifier() >= cap) {
            return;
        }
        if (gated(BITTEN, victim, BITE_TICKS)) {
            return;
        }
        apply(victim);
    }

    private static void warn(ServerPlayer player) {
        Speech.say(player, "speech.jojoha.bleed.out", ChatFormatting.RED);
        player.displayClientMessage(Component.translatable("message.jojoha.bleed.out")
                .withStyle(ChatFormatting.RED), true);
        player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    private static void spurt(LivingEntity victim, int amplifier) {
        NetworkHandler.broadcastFlair(victim,
                FlairCuePacket.SPURT, amplifier);
    }

    public static void nick(LivingEntity victim) {
        long now = victim.level().getGameTime();
        Long last = NICKED.get(victim);
        if (last != null && now - last < NICK_TICKS) {
            return;
        }
        NICKED.put(victim, now);
        apply(victim);
    }

    private static boolean gated(Map<LivingEntity, Long> marks, LivingEntity victim, int gap) {
        long now = victim.level().getGameTime();
        Long last = marks.get(victim);
        if (last != null && now - last < gap) {
            return true;
        }
        marks.put(victim, now);
        return false;
    }

    public static void ease(LivingEntity victim) {
        if (victim.level().isClientSide() || !victim.hasEffect(ModEffects.bleed())) {
            return;
        }
        if (gated(EASED, victim, EASE_TICKS)) {
            return;
        }
        EASING.put(victim, Boolean.TRUE);
    }

    private static void lighten(LivingEntity victim) {
        MobEffectInstance open = victim.getEffect(ModEffects.bleed());
        if (open == null || !victim.isAlive()) {
            return;
        }
        victim.removeEffect(ModEffects.bleed());
        boolean stopped = open.getAmplifier() == 0;
        if (!stopped) {
            victim.addEffect(new MobEffectInstance(ModEffects.bleed(), open.getDuration(), open.getAmplifier() - 1,
                    false, false, true));
        }
        if (victim instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(stopped
                    ? "message.jojoha.bleed.stopped" : "message.jojoha.bleed.eased"), true);
        }
    }

    public static float toll(int amplifier) {
        return TOLL[Math.min(amplifier, TOLL.length - 1)];
    }

    public static void comeDue(LivingEntity victim, int amplifier) {
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }

        victim.hurt(level.damageSources().magic(), DamageScale.combat(null, victim, toll(amplifier)));
        Limbs.bled(victim, amplifier);

        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(),
                8 + amplifier * 6, 0.3, 0.35, 0.3, 0.05);
        level.playSound(null, victim.blockPosition(), SoundEvents.PLAYER_HURT,
                SoundSource.PLAYERS, 0.8F, 0.6F);
    }
}
