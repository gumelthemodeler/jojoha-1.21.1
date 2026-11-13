package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.chariot.Chariot;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class AfterimagesSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "afterimages");
    public static final AfterimagesSkill INSTANCE = new AfterimagesSkill();

    private static final int COOLDOWN_TICKS = 400;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private AfterimagesSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.afterimages";
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
        StandEntity stand = Chariot.stand(player);
        return stand != null && !stand.armorOff() ? "message.jojoha.chariot.armored" : null;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (!Chariot.is(stand) || !stand.armorOff()) {
            return false;
        }
        stand.triggerStrikeForward("afterimage");
        StandBeat.at(Chariot.AFTERIMAGE_SPLIT_TICKS, player, () -> {
            StandEntity splitting = Chariot.stand(player);
            if (splitting != null && splitting.armorOff()) {
                Chariot.split(player, splitting);
            }
        });
        return true;
    }
}
