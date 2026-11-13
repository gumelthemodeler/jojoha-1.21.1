package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.heaven.Ascension;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.heaven.Overwrite;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class OverwriteSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "overwrite");
    public static final OverwriteSkill INSTANCE = new OverwriteSkill();

    private static final int COOLDOWN_TICKS = 2400;
    private static final float ENERGY_COST = EnergyWeight.ULTIMATE.cost();

    private OverwriteSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.overwrite";
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
        if (Overwrite.running(player)) {
            return "message.jojoha.heaven.overwrite_running";
        }
        return Ascension.reached(data, Ascension.HEAVEN) ? null : "message.jojoha.heaven.overwrite_locked";
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        Heaven.telegraph(player);
        Heaven.announce(player, "announce.jojoha.heaven_overwrite");
        Overwrite.begin(player);
        return true;
    }
}
