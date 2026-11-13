package org.gumel.jojoha.labyrinth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.Entity;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.config.JojohaServerConfig;

import java.util.function.BiConsumer;

public class TntBarrelBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public static final float MAX_DAMAGE = 10.0F;
    public static final float POWER = 3.0F;
    public static final int FUSE = 20;
    public static final int STRUCK_FUSE = 6;
    public static final int CHAIN_RADIUS = 3;
    public static final int CHAIN_FUSE = 4;

    public TntBarrelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel server) {
            prime(server, pos, state, FUSE);
        }
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel server) {
            prime(server, hit.getBlockPos(), state, STRUCK_FUSE);
        }
    }

    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> drops) {
        if (level instanceof ServerLevel server) {
            prime(server, pos, state, CHAIN_FUSE + server.random.nextInt(CHAIN_FUSE));
        }
    }

    public void prime(ServerLevel level, BlockPos pos, BlockState state, int fuse) {
        if (!state.is(this) || state.getValue(LIT)) {
            return;
        }
        level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.1F);
        level.gameEvent(null, GameEvent.PRIME_FUSE, pos);
        level.scheduleTick(pos, this, Math.max(1, fuse));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        level.removeBlock(pos, false);
        level.explode(null, null, new Blast(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                POWER, false, Level.ExplosionInteraction.NONE);
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-CHAIN_RADIUS, -CHAIN_RADIUS, -CHAIN_RADIUS),
                pos.offset(CHAIN_RADIUS, CHAIN_RADIUS, CHAIN_RADIUS))) {
            BlockState other = level.getBlockState(near);
            if (other.is(this)) {
                prime(level, near.immutable(), other, CHAIN_FUSE + random.nextInt(CHAIN_FUSE));
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                    0.0, 0.05, 0.0);
            level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                    0.0, 0.02, 0.0);
        }
    }

    public static final class Blast extends ExplosionDamageCalculator {
        @Override
        public float getEntityDamageAmount(Explosion explosion, Entity entity) {
            float dealt = DamageScale.combat(null, entity, super.getEntityDamageAmount(explosion, entity));
            return Math.min(dealt, JojohaServerConfig.BARREL_DAMAGE.value());
        }
    }
}
