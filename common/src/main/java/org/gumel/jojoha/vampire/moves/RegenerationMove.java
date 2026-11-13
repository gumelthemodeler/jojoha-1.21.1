package org.gumel.jojoha.vampire.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.vampire.VampireFx;
import org.gumel.jojoha.vampire.VampireMove;
import org.gumel.jojoha.vampire.VampireState;

public final class RegenerationMove implements VampireMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "regeneration");
    public static final RegenerationMove INSTANCE = new RegenerationMove();

    public static final int PRIMED_TICKS = 200;

    public static final float REFORM_HEALTH = 6.0F;
    public static final double REFORM_DISTANCE = 5.0;

    public static final int GRACE_TICKS = 40;

    private static final int COOLDOWN_TICKS = 600;
    private static final float ENERGY_COST = 25F;

    private RegenerationMove() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
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
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        ServerLevel level = player.serverLevel();
        VampireState.primeReform(player, PRIMED_TICKS);

        VampireFx.aura(level, player.getX(), player.getY() + 1.0, player.getZ(), 14);
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS, 0.35F, 1.9F);
    }
}
