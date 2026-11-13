package org.gumel.jojoha.ultimate;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.vampire.VampireMove;

public final class AdaptMove implements VampireMove {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "adapt");
    public static final AdaptMove INSTANCE = new AdaptMove();

    public static final double STUDY_RANGE = 16.0;

    private static final int COOLDOWN_TICKS = 100;
    private static final float ENERGY_COST = 15F;

    private AdaptMove() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public VampireStage minimumStage() {
        return VampireStage.ULTIMATE_LIFEFORM;
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        LivingEntity target = AdaptationKit.aimed(player, STUDY_RANGE);
        if (target == null) {
            return "message.jojoha.adapt.no_target";
        }
        Adaptation adaptation = Adaptation.of(target.getType());
        if (adaptation == null) {
            return "message.jojoha.adapt.nothing";
        }
        return AdaptationLibrary.mastery(data, adaptation) >= AdaptationLibrary.MASTERY_MAX
                ? "message.jojoha.adapt.fully_mastered" : null;
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        LivingEntity target = AdaptationKit.aimed(player, STUDY_RANGE);
        if (target == null) {
            return;
        }
        UltimateTraits.study(player, target);
        player.displayClientMessage(Component.translatable("message.jojoha.adapt.studying",
                target.getType().getDescription()), true);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.7F, 0.6F);
    }
}
