package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.heaven.RealityBend;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class RealityBendSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "reality_bend");
    public static final RealityBendSkill INSTANCE = new RealityBendSkill();

    private RealityBendSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.reality_bend";
    }

    @Override
    public boolean passiveOnly() {
        return true;
    }

    @Override
    public boolean requiresStand() {
        return false;
    }

    @Override
    public int cooldownTicks() {
        return RealityBend.COOLDOWN_SECONDS * 20;
    }

    @Override
    public float energyCost() {
        return 0F;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        return false;
    }
}
