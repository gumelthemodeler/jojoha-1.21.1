package org.gumel.jojoha.ultimate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.vampire.VampireMove;

import java.util.List;

public final class AdaptedAbilityMove implements VampireMove {
    public static final AdaptedAbilityMove INSTANCE = new AdaptedAbilityMove(0, "adapted_ability");
    public static final AdaptedAbilityMove SECOND = new AdaptedAbilityMove(1, "adapted_ability_2");
    public static final AdaptedAbilityMove THIRD = new AdaptedAbilityMove(2, "adapted_ability_3");
    public static final List<AdaptedAbilityMove> SLOTS = List.of(INSTANCE, SECOND, THIRD);

    public static final ResourceLocation ID = INSTANCE.id;

    private static final int FALLBACK_COOLDOWN_TICKS = 20;
    private static final float FALLBACK_ENERGY_COST = 10F;

    private final int slot;
    private final ResourceLocation id;

    private AdaptedAbilityMove(int slot, String path) {
        this.slot = slot;
        this.id = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    public int slot() {
        return slot;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public int cooldownTicks() {
        return FALLBACK_COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return FALLBACK_ENERGY_COST;
    }

    @Override
    public int cooldownTicks(JojohaPlayerData data) {
        Adaptation adaptation = AdaptationLibrary.active(data, slot);
        return adaptation == null ? FALLBACK_COOLDOWN_TICKS : AdaptationLibrary.cooldownTicks(data, adaptation);
    }

    @Override
    public float energyCost(JojohaPlayerData data) {
        Adaptation adaptation = AdaptationLibrary.active(data, slot);
        return adaptation == null ? FALLBACK_ENERGY_COST : adaptation.energyCost();
    }

    @Override
    public VampireStage minimumStage() {
        return VampireStage.ULTIMATE_LIFEFORM;
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        if (slot >= AdaptationLibrary.activeSlots(data)) {
            return "message.jojoha.adapt.slot_locked";
        }
        Adaptation adaptation = AdaptationLibrary.active(data, slot);
        return adaptation == null ? "message.jojoha.adapt.none" : adaptation.unavailable(player);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        Adaptation adaptation = AdaptationLibrary.active(data, slot);
        if (adaptation != null) {
            adaptation.use(player);
        }
    }
}
