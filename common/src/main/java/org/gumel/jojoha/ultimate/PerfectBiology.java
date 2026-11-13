package org.gumel.jojoha.ultimate;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.PlayerDataAccess;

import java.util.List;

public final class PerfectBiology {
    public static final List<Holder<MobEffect>> SHRUGGED = List.of(
            MobEffects.POISON, MobEffects.WITHER, MobEffects.HUNGER, MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.WEAKNESS, MobEffects.CONFUSION, MobEffects.DIG_SLOWDOWN, MobEffects.BLINDNESS,
            MobEffects.INFESTED, MobEffects.OOZING);

    private PerfectBiology() {
    }

    public static boolean shrugs(LivingEntity entity, Holder<MobEffect> effect) {
        return entity instanceof ServerPlayer player && SHRUGGED.contains(effect)
                && Fate.ultimate(PlayerDataAccess.get(player));
    }

    public static void cleanse(ServerPlayer player) {
        for (Holder<MobEffect> effect : SHRUGGED) {
            player.removeEffect(effect);
        }
    }
}
