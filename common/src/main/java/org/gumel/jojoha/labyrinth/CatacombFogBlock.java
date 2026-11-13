package org.gumel.jojoha.labyrinth;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CatacombFogBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CatacombFogBlock> CODEC = simpleCodec(CatacombFogBlock::new);
    private static final double SHEET = 2.0 / 16.0;
    private static final double SLACK = 1.0E-3;
    private static final VoxelShape NORTH = Block.box(0, 0, 0, 16, 16, 2);
    private static final VoxelShape SOUTH = Block.box(0, 0, 14, 16, 16, 16);
    private static final VoxelShape WEST = Block.box(0, 0, 0, 2, 16, 16);
    private static final VoxelShape EAST = Block.box(14, 0, 0, 16, 16, 16);

    public CatacombFogBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext held && holds(held.getEntity(), state.getValue(FACING), pos)) {
            return sheet(state.getValue(FACING));
        }
        return Shapes.empty();
    }

    private static boolean holds(Entity entity, Direction out, BlockPos pos) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        if (!(entity instanceof Player player)) {
            return true;
        }
        return !player.isCreative() && !player.isSpectator() && inside(out, pos, player.getBoundingBox());
    }

    public static boolean inside(Direction out, BlockPos pos, AABB box) {
        return switch (out) {
            case SOUTH -> box.maxZ <= pos.getZ() + 1 - SHEET + SLACK;
            case NORTH -> box.minZ >= pos.getZ() + SHEET - SLACK;
            case EAST -> box.maxX <= pos.getX() + 1 - SHEET + SLACK;
            case WEST -> box.minX >= pos.getX() + SHEET - SLACK;
            default -> false;
        };
    }

    private static VoxelShape sheet(Direction out) {
        return switch (out) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction out = state.getValue(FACING);
        double depth = 1.0 - SHEET * 0.5;
        double x = pos.getX() + (out.getStepX() == 0 ? random.nextDouble() : 0.5 + out.getStepX() * (depth - 0.5));
        double z = pos.getZ() + (out.getStepZ() == 0 ? random.nextDouble() : 0.5 + out.getStepZ() * (depth - 0.5));
        double y = pos.getY() + random.nextDouble();
        level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0.0, 0.02, 0.0);
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.END_ROD, x, y, z, -out.getStepX() * 0.01, 0.015, -out.getStepZ() * 0.01);
        }
    }
}
