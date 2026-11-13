package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.chariot.Chariot;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class RiposteSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "riposte");
    public static final RiposteSkill INSTANCE = new RiposteSkill();

    private static final int COOLDOWN_TICKS = 160;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private RiposteSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.riposte";
    }

    @Override
    public boolean isAnswerToBeingHit() {
        return true;
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
        return Chariot.rapierless(player);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (!Chariot.is(stand) || Chariot.riposteOpen(player)) {
            return false;
        }
        Chariot.openRiposte(player, stand);
        return true;
    }
}
