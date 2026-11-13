package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.heaven.GreatestHigh;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class GreatestHighSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "greatest_high");
    public static final GreatestHighSkill INSTANCE = new GreatestHighSkill();

    private static final int COOLDOWN_TICKS = 1800;
    private static final float ENERGY_COST = EnergyWeight.HEAVY.cost();

    private GreatestHighSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.greatest_high";
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
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        return GreatestHigh.available(data) ? null : "message.jojoha.heaven.greatest_high_locked";
    }

    @Override
    public boolean isRunning(JojohaPlayerData data) {
        return GreatestHigh.active(data);
    }

    @Override
    public boolean canBeCalledOff(JojohaPlayerData data) {
        return GreatestHigh.active(data);
    }

    @Override
    public void callOff(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        GreatestHigh.end(player, data);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (stand == null) {
            return false;
        }
        GreatestHigh.begin(player, data);
        return true;
    }
}
