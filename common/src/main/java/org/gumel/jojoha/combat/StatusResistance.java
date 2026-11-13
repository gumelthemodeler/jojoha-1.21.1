package org.gumel.jojoha.combat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.registry.ModEffects;

import java.util.Map;
import java.util.WeakHashMap;

public final class StatusResistance {
    public static final TagKey<EntityType<?>> RESISTANT = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "status_resistant"));

    public static final TagKey<MobEffect> STATUSES = TagKey.create(Registries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "statuses"));

    public static final int REPEAT_WINDOW_TICKS = 60;

    public static final float REPEAT_PERCENT = 50F;

    public static final float BOSS_PERCENT = 50F;

    public static final int BRIEF_TICKS = 10;

    private static final Map<LivingEntity, Long> STUN_ENDS = new WeakHashMap<>();

    private StatusResistance() {
    }

    public static MobEffectInstance adjust(LivingEntity entity, MobEffectInstance incoming) {
        if (entity.level().isClientSide() || incoming.isInfiniteDuration()
                || !incoming.getEffect().is(STATUSES)) {
            return incoming;
        }

        boolean fresh = !entity.hasEffect(incoming.getEffect());
        float scale = 1F;
        if (fresh && entity.getType().is(RESISTANT)) {
            scale *= JojohaServerConfig.BOSS_STATUS.value() / 100F;
        }

        if (incoming.getEffect().value() == ModEffects.STUN.get() && incoming.getDuration() > BRIEF_TICKS) {
            long now = entity.level().getGameTime();
            Long ended = STUN_ENDS.get(entity);
            if (fresh && ended != null && now >= ended
                    && now < ended + JojohaServerConfig.STUN_REPEAT_WINDOW.whole()) {
                scale *= JojohaServerConfig.STUN_REPEAT.value() / 100F;
            }
            long end = now + Math.max(1, Math.round(incoming.getDuration() * scale));
            STUN_ENDS.merge(entity, end, Math::max);
        }

        if (scale >= 1F) {
            return incoming;
        }
        return new MobEffectInstance(incoming.getEffect(), Math.max(1, Math.round(incoming.getDuration() * scale)),
                incoming.getAmplifier(), incoming.isAmbient(), incoming.isVisible(), incoming.showIcon());
    }
}
