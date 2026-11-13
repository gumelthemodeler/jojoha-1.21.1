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

public final class ArmorReleaseSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "armor_release");
    public static final ArmorReleaseSkill INSTANCE = new ArmorReleaseSkill();

    private static final int COOLDOWN_TICKS = 200;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private ArmorReleaseSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.armor_release";
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
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (!Chariot.is(stand)) {
            return false;
        }
        if (stand.armorOff()) {
            Chariot.rearm(player, stand);
            return true;
        }
        stand.triggerStrikeForward("armor_release");
        StandBeat.at(Chariot.ARMOR_BURST_TICKS, player, () -> {
            StandEntity shedding = Chariot.stand(player);
            if (shedding != null && !shedding.armorOff()) {
                Chariot.shedArmor(player, shedding);
            }
        });
        return true;
    }
}
