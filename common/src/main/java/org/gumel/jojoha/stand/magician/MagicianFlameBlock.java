package org.gumel.jojoha.stand.magician;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class MagicianFlameBlock extends BaseFireBlock {
    public static final MapCodec<MagicianFlameBlock> CODEC = simpleCodec(MagicianFlameBlock::new);

    public static final int MAX_SPREAD = 4;
    public static final int BURN_STEPS = 7;
    public static final IntegerProperty SPREAD = IntegerProperty.create("spread", 0, MAX_SPREAD);
    public static final IntegerProperty BURN = BlockStateProperties.AGE_7;

    public static final int STEP_TICKS = 80;
    private static final int STEP_SPREAD = 20;
    public static final float SPREAD_CHANCE = 0.55F;

    public MagicianFlameBlock(Properties properties) {
        super(properties, 1.0F);
        registerDefaultState(stateDefinition.any().setValue(SPREAD, 0).setValue(BURN, 0));
    }

    @Override
    protected MapCodec<? extends BaseFireBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPREAD, BURN);
    }

    @Override
    protected boolean canBurn(BlockState state) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    public static boolean room(BlockGetter level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(pos).isAir() && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState neighbour, LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!old.is(this)) {
            MagicianFlames.placed(level, pos);
            level.scheduleTick(pos, this, STEP_SPREAD + level.random.nextInt(STEP_SPREAD));
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!next.is(this)) {
            MagicianFlames.removed(level, pos);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos) || level.isRainingAt(pos)) {
            level.removeBlock(pos, false);
            return;
        }
        int spread = state.getValue(SPREAD);
        if (spread > 0 && random.nextFloat() < SPREAD_CHANCE) {
            Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            for (BlockPos at : new BlockPos[] {pos.relative(side), pos.relative(side).above(), pos.relative(side).below()}) {
                if (room(level, at)) {
                    level.setBlock(at, defaultBlockState().setValue(SPREAD, spread - 1), Block.UPDATE_ALL);
                    break;
                }
            }
        }
        int burn = state.getValue(BURN);
        if (burn >= BURN_STEPS) {
            level.removeBlock(pos, false);
            return;
        }
        level.setBlock(pos, state.setValue(BURN, burn + 1), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, this, STEP_TICKS / 2 + random.nextInt(STEP_TICKS));
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || entity instanceof Player player && MagicianFlames.immune(player)) {
            return;
        }
        super.entityInside(state, level, pos, entity);
    }
}
