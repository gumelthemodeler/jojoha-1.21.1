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

public final class SwordFlurrySkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sword_flurry");
    public static final SwordFlurrySkill INSTANCE = new SwordFlurrySkill();

    private static final int COOLDOWN_TICKS = 120;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private SwordFlurrySkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.sword_flurry";
    }

    @Override
    public ResourceLocation replaces() {
        return BarrageSkill.ID;
    }

    @Override
    public boolean supplants() {
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
        StandEntity stand = StandEntity.findFor(player);
        return stand != null && stand.isHeldBarrageRunning();
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        StandEntity stand = Chariot.stand(player);
        return stand != null && stand.isHeldBarrageRunning() ? null : Chariot.rapierless(player);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (!Chariot.is(stand)) {
            return false;
        }
        if (stand.isHeldBarrageRunning()) {
            stand.endHeldBarrage();
        } else {
            stand.beginHeldBarrage(StandEntity.BarrageStyle.SWORD);
        }
        return true;
    }
}
