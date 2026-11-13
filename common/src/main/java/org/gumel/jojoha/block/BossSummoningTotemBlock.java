package org.gumel.jojoha.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.registry.ModItems;

import java.util.HashSet;
import java.util.Set;

public class BossSummoningTotemBlock extends Block {
    public static final MapCodec<BossSummoningTotemBlock> CODEC = simpleCodec(BossSummoningTotemBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final TagKey<Item> OFFERINGS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "boss_offerings"));
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(3, 3, 3, 13, 12, 13),
            Block.box(5, 12, 5, 11, 19, 11),
            Block.box(6, 19, 6, 10, 24, 10));
    private static final double FLAME_Y = 24.5 / 16.0;
    private static final int FIZZLE_TICKS = 40;
    private static final Set<GlobalPos> FIZZLING = new HashSet<>();

    public BossSummoningTotemBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(LIT) || !stack.is(OFFERINGS)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server) {
            if (stack.is(ModItems.DEFECTED_AJA.get())) {
                FIZZLING.add(GlobalPos.of(server.dimension(), pos.immutable()));
                server.scheduleTick(pos, this, FIZZLE_TICKS);
            }
            stack.consume(1, player);
            server.setBlock(pos, state.setValue(LIT, true), 3);
            double x = pos.getX() + 0.5, y = pos.getY() + FLAME_Y, z = pos.getZ() + 0.5;
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 30, 0.15, 0.2, 0.15, 0.04);
            server.sendParticles(ParticleTypes.SOUL, x, y, z, 8, 0.3, 0.3, 0.3, 0.03);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 10, 0.2, 0.2, 0.2, 0.02);
            server.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.6F);
            server.playSound(null, pos, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 1.6F, 0.7F);
            server.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 0.6F);
            player.displayClientMessage(Component.translatable("block.jojoha.boss_summoning_totem.lit"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!FIZZLING.remove(GlobalPos.of(level.dimension(), pos)) || !state.getValue(LIT)) {
            return;
        }
        level.setBlock(pos, state.setValue(LIT, false), 3);
        double x = pos.getX() + 0.5, y = pos.getY() + FLAME_Y, z = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 16, 0.15, 0.2, 0.15, 0.02);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.8F, 1.4F);
        Block.popResource(level, pos.above(), new ItemStack(ModItems.DEFECTED_AJA.get()));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!state.getValue(LIT)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("block.jojoha.boss_summoning_totem.waiting"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.1;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.1;
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, pos.getY() + FLAME_Y, z, 0.0, 0.01, 0.0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + FLAME_Y + 0.15, z, 0.0, 0.02, 0.0);
        }
    }
}
