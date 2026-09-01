package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.BuildMode;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.StandSkill;

/**
 * A slot that chooses a build shape rather than throwing a move.
 *
 * <p>Utility swaps the whole combat bar out for these - see {@code StandSkills.movesetFor}. A Stand
 * that is laying blocks has no use for a barrage, and the eight keys the player already has under
 * their fingers are the obvious place to put the tools it does need.
 *
 * <p>Free in every sense: no energy, no cooldown, no trust gate. Picking a shape is not an action
 * the Stand performs, it is the player saying what the next action means - and charging for a
 * setting would make people avoid changing it, which is the opposite of the point. Handled ahead of
 * the ordinary gauntlet in {@code StandSkills.handleUseRequest} for exactly that reason.
 */
public final class BuildModeSkill implements StandSkill {
    public static final BuildModeSkill SINGLE = new BuildModeSkill(BuildMode.SINGLE);
    public static final BuildModeSkill ROW = new BuildModeSkill(BuildMode.ROW);
    public static final BuildModeSkill COLUMN = new BuildModeSkill(BuildMode.COLUMN);
    public static final BuildModeSkill FREE = new BuildModeSkill(BuildMode.FREE);

    private final BuildMode mode;
    private final ResourceLocation id;

    private BuildModeSkill(BuildMode mode) {
        this.mode = mode;
        this.id = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                "build_" + mode.name().toLowerCase(java.util.Locale.ROOT));
    }

    public BuildMode mode() {
        return mode;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public String translationKey() {
        return mode.translationKey();
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
     * <p>{@code StandSkills.handleUseRequest} recognises this type and applies the mode before any
     * of the machinery that would call this. Left throwing rather than quietly returning true, so
     * that a future caller which routes around that branch fails loudly instead of silently doing
     * nothing.
     */
    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        throw new UnsupportedOperationException("build modes are applied in StandSkills, not cast");
    }
}
