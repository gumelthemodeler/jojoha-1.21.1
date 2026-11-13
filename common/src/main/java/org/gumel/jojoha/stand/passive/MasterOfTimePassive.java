package org.gumel.jojoha.stand.passive;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.PlayerDataAccess;

public final class MasterOfTimePassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "master_of_time");
    public static final MasterOfTimePassive INSTANCE = new MasterOfTimePassive();

    public static final TagKey<MobEffect> TIME_BASED = TagKey.create(Registries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "time_based"));

    public static final float DURATION_KEPT = 0.8F;

    private MasterOfTimePassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.master_of_time";
    }

    public static MobEffectInstance shorten(ServerPlayer player, MobEffectInstance incoming) {
        if (incoming.isInfiniteDuration() || !incoming.getEffect().is(TIME_BASED)
                || !StandPassives.grants(PlayerDataAccess.get(player), ID)) {
            return incoming;
        }
        int kept = Math.max(1, Math.round(incoming.getDuration() * DURATION_KEPT));
        return new MobEffectInstance(incoming.getEffect(), kept, incoming.getAmplifier(),
                incoming.isAmbient(), incoming.isVisible(), incoming.showIcon());
    }
}
