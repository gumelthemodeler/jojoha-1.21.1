package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.chariot.Chariot;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class DeflectionSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "deflection");
    public static final DeflectionSkill INSTANCE = new DeflectionSkill();

    private static final int COOLDOWN_TICKS = 120;
    private static final float ENERGY_COST = EnergyWeight.UPKEEP.cost();

    private DeflectionSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.deflection";
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
    public boolean isSustained() {
        return true;
    }

    @Override
    public boolean isSustainActive(Player player) {
        if (player instanceof ServerPlayer server) {
            return Chariot.deflecting(server);
        }
        StandEntity stand = Chariot.standFor(player);
        return stand != null && stand.holdPose() == StandEntity.HOLD_DEFLECT;
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        return isSustainActive(player) ? null : Chariot.rapierless(player);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (Chariot.deflecting(player)) {
            Chariot.endDeflect(player);
            return true;
        }
        if (!Chariot.is(stand)) {
            return false;
        }
        Chariot.beginDeflect(player, stand);
        return true;
    }
}
