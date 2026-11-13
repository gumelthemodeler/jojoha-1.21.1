package org.gumel.jojoha.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.gumel.jojoha.registry.ModRegistries;

public final class RespawnTotemBlockEntity extends BlockEntity {
    public static final float OPEN_TICKS = 5F;

    private boolean seen;
    private boolean open;
    private long turnedAt;
    private int shownLives;

    public RespawnTotemBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.RESPAWN_TOTEM_BLOCK_ENTITY.get(), pos, state);
    }

    public float opening(int lives, long now, float partialTick) {
        boolean awake = lives > 0;
        if (awake) {
            shownLives = lives;
        }
        if (!seen) {
            seen = true;
            open = awake;
            turnedAt = now - 1000L;
        } else if (awake != open) {
            open = awake;
            turnedAt = now;
        }
        float t = Mth.clamp((now - turnedAt + partialTick) / OPEN_TICKS, 0F, 1F);
        float eased = 0.5F * (t + 3F * t * t - 2F * t * t * t);
        return open ? eased : 1F - eased;
    }

    public int shownLives() {
        return shownLives;
    }
}
