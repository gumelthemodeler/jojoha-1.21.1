package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.BuildMode;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.Locale;

public final class BuildModeSkill implements StandSkill {
    public static final BuildModeSkill SINGLE = new BuildModeSkill(BuildMode.SINGLE);
    public static final BuildModeSkill ROW = new BuildModeSkill(BuildMode.ROW);
    public static final BuildModeSkill COLUMN = new BuildModeSkill(BuildMode.COLUMN);
    public static final BuildModeSkill FREE = new BuildModeSkill(BuildMode.FREE);
    public static final BuildModeSkill SELECTION = new BuildModeSkill(BuildMode.SELECTION);

    private final BuildMode mode;
    private final ResourceLocation id;

    private BuildModeSkill(BuildMode mode) {
        this.mode = mode;
        this.id = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                "build_" + mode.name().toLowerCase(Locale.ROOT));
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

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        throw new UnsupportedOperationException("build modes are applied in StandSkills, not cast");
    }
}
