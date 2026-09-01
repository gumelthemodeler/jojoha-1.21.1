package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.StandSkill;

/**
 * Calls the Stand back from wherever it is working.
 *
 * <p>The stance already brings it home two ways - leaving Utility, or five seconds of nobody asking
 * it for anything - and neither is any use in the moment you actually want it back. Leaving the
 * stance means giving up the tool you were using to say "come here", and waiting out an idle timer
 * is not an instruction, it is an accident that happens to have the right outcome.
 *
 * <p>So this is the third way, and the only deliberate one. It says nothing about what the Stand
 * should do next; it only ends the errand.
 *
 * <p>Free, like the build shapes it sits beside. Recalling something is not an act the Stand
 * performs on the world, and charging for it would make a player who ran their pool dry laying a
 * floor unable to ask for their Stand back - which is exactly the moment they would want to.
 */
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

    /**
     * Never reached.
     *
     * <p>Applied in {@code StandSkills.handleUseRequest} ahead of the machinery that would call
     * this, for the same reason the build shapes are. Throws rather than quietly succeeding so that
     * a future caller routing around that branch fails loudly instead of doing nothing.
     */
    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        throw new UnsupportedOperationException("recall is applied in StandSkills, not cast");
    }
}
