package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class RecallStandSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "recall_stand");
    public static final RecallStandSkill INSTANCE = new RecallStandSkill();

    private RecallStandSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.recall_stand";
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public float energyCost() {
        return 0F;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        throw new UnsupportedOperationException("recall is applied in StandSkills, not cast");
    }
}
