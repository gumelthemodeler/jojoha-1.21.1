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

public final class WallCrawlMove implements VampireMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "wall_crawl");
    public static final WallCrawlMove INSTANCE = new WallCrawlMove();

    public static final double CLIMB_SPEED = 0.22;

    public static final float DRAIN_PER_TICK = 0.35F;

    private static final float ENERGY_COST = 8F;

    private WallCrawlMove() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int cooldownTicks() {
        return 0;
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
    public boolean isSustainActive(ServerPlayer player) {
        return VampireState.isCrawling(player);
    }

    @Override
    public void release(ServerPlayer player, JojohaPlayerData data) {
        VampireState.setCrawling(player, false);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        ServerLevel level = player.serverLevel();
        VampireState.setCrawling(player, true);

        VampireFx.blood(level, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.3, 0.06);
        level.playSound(null, player.blockPosition(), SoundEvents.SPIDER_STEP,
                SoundSource.PLAYERS, 0.7F, 0.6F);
    }
}
