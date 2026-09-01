package org.gumel.jojoha.item;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.registry.ModRegistries;

/**
 * The Stone Mask after it lets go, falling.
 *
 * <p>A real entity rather than something drawn inside the player's render, and that is the whole
 * point of it. Drawn on the player, the mask could only ever be translated downward in the player's
 * own model space - it would follow them if they walked, pass through the floor, and wink out at a
 * fixed time having never arrived anywhere. As an entity it has its own position: it falls under its
 * own gravity, stops on whatever is beneath it, and breaks where it lands rather than where the
 * player happened to be standing.
 *
 * <p>It is not an item and cannot be picked up. The mask is spent - what it does here is land and
 * come apart, and leaving a collectable behind would undo the fact that it has been used.
 */
public class FallingMask extends Entity {
    /** Ordinary block gravity, applied per tick. */
    private static final double GRAVITY = 0.04;

    /** How fast it turns as it goes. Fixed rather than random, so every mask falls the same way. */
    private static final float SPIN_DEGREES_PER_TICK = 14F;

    /** A backstop, in ticks, for a mask that finds nothing to land on. */
    private static final int MAX_LIFETIME = 200;

    /** How many chips it breaks into, and how far they scatter. */
    private static final int SHARDS = 40;
    private static final double SHARD_SPREAD = 0.22;

    /** Set on the tick it lands, so the break happens once even if it settles restlessly. */
    private boolean broken;

    public FallingMask(EntityType<? extends FallingMask> type, Level level) {
        super(type, level);
    }

    public FallingMask(Level level, double x, double y, double z, Vec3 push) {
        this(ModRegistries.FALLING_MASK.get(), level);
        this.setPos(x, y, z);
        this.setDeltaMovement(push);

        // The client interpolates from the previous position, so a fresh entity whose "previous"
        // is the world origin streaks across the map on its first frame.
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > MAX_LIFETIME) {
            this.discard();
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x * 0.98, (motion.y - GRAVITY) * 0.98, motion.z * 0.98);
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (!this.level().isClientSide && this.onGround() && !this.broken) {
            this.broken = true;
            shatter((ServerLevel) this.level());
            this.discard();
        }
    }

    /** How far through its tumble it is, for the renderer to turn it by. */
    public float spinDegrees(float partialTick) {
        return (this.tickCount + partialTick) * SPIN_DEGREES_PER_TICK;
    }

    /**
     * It comes apart where it landed.
     *
     * <p>Vanilla's block-break particles carry the block's own texture, so these are literally
     * chips of stone rather than a coloured approximation of them.
     */
    private void shatter(ServerLevel level) {
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
                this.getX(), this.getY() + 0.1, this.getZ(), SHARDS,
                SHARD_SPREAD, SHARD_SPREAD, SHARD_SPREAD, 0.06);

        level.playSound(null, this.blockPosition(), SoundEvents.STONE_BREAK,
                SoundSource.PLAYERS, 1.0F, 0.65F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Nothing to sync. Where it is and which way it is turning are both derived - position from
        // the ordinary entity tracking, spin from the age every client already has.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
