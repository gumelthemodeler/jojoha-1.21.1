package org.gumel.jojoha.pillar;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.vampire.VampireMove;

public abstract class PillarMove implements VampireMove {
    private final ResourceLocation id;
    private final int cooldownTicks;
    private final float energyCost;

    protected PillarMove(String path, int cooldownTicks, float energyCost) {
        this.id = id(path);
        this.cooldownTicks = cooldownTicks;
        this.energyCost = energyCost;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public int cooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public float energyCost() {
        return energyCost;
    }

    @Override
    public boolean pillar() {
        return true;
    }

    @Override
    public VampireStage minimumStage() {
        return VampireStage.PILLAR_MAN;
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        return PillarMen.hiding(player) ? "message.jojoha.pillar.inside" : null;
    }
}
