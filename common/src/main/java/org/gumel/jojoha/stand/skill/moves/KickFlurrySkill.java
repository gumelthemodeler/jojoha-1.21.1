package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class KickFlurrySkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "kick_flurry");
    public static final KickFlurrySkill INSTANCE = new KickFlurrySkill();

    private static final int COOLDOWN_TICKS = 140;
    private static final float ENERGY_COST = EnergyWeight.HEAVY.cost();

    private KickFlurrySkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.kick_flurry";
    }

    @Override
    public ResourceLocation replaces() {
        return BarrageSkill.ID;
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
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (stand == null) {
            return false;
        }
        if (stand.isHeldBarrageRunning()) {
            stand.endHeldBarrage();
        } else {
            stand.beginHeldBarrage(StandEntity.BarrageStyle.KICKS);
        }
        return true;
    }
}
