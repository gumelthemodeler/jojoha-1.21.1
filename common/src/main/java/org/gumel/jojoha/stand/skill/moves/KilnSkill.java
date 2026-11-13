package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandHands;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class KilnSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "kiln");

    public static final KilnSkill INSTANCE = new KilnSkill();

    private static final int COOLDOWN_TICKS = 100;
    private static final double ITEM_RADIUS = 2.5;
    private static final int HEAT_RADIUS = 2;
    private static final int MOST_ITEMS = 16;
    private static final int COOK_TICKS = 40;
    private static final int FLAME_EVERY = 4;

    private KilnSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.kiln";
    }

    @Override
    public boolean isUnlocked(JojohaPlayerData data) {
        return data.stand.isPresent();
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return EnergyWeight.LIGHT.cost();
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        Vec3 focus = focus(player, data);
        ServerLevel level = player.serverLevel();
        return raw(level, focus).isEmpty() && heated(level, player, focus).isEmpty()
                ? "message.jojoha.kiln.nothing" : null;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();
        Vec3 focus = focus(player, data);
        List<ItemEntity> raw = raw(level, focus);
        List<BlockPos> heated = heated(level, player, focus);
        if (raw.isEmpty() && heated.isEmpty()) {
            return false;
        }
        if (stand != null) {
            stand.reachTowards(focus);
        }
        for (BlockPos pos : heated) {
            heat(level, pos);
        }
        if (!raw.isEmpty()) {
            for (ItemEntity item : raw) {
                item.setPickUpDelay(COOK_TICKS + 2);
            }
            StandBeat.during(1, COOK_TICKS, player, tick -> {
                if (tick % FLAME_EVERY == 0) {
                    for (ItemEntity item : raw) {
                        if (item.isAlive()) {
                            level.sendParticles(ParticleTypes.FLAME, item.getX(), item.getY() + 0.2, item.getZ(),
                                    2, 0.15, 0.1, 0.15, 0.01);
                        }
                    }
                }
                if (tick >= COOK_TICKS) {
                    cook(level, raw);
                }
            });
        }
        level.playSound(null, BlockPos.containing(focus), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.1F);
        return true;
    }

    private static Vec3 focus(ServerPlayer player, JojohaPlayerData data) {
        BlockHitResult hit = ChoreSkill.aim(player, data);
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getLocation();
        }
        return player.getEyePosition().add(player.getLookAngle().scale(StandHands.reachFor(data)));
    }

    private static Optional<RecipeHolder<SmeltingRecipe>> recipe(ServerLevel level, ItemStack stack) {
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
    }

    private static List<ItemEntity> raw(ServerLevel level, Vec3 focus) {
        List<ItemEntity> out = new ArrayList<>();
        int count = 0;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(focus, focus).inflate(ITEM_RADIUS),
                item -> item.isAlive())) {
            if (count >= MOST_ITEMS || recipe(level, item.getItem()).isEmpty()) {
                continue;
            }
            out.add(item);
            count += item.getItem().getCount();
        }
        return out;
    }

    private static void cook(ServerLevel level, List<ItemEntity> raw) {
        int budget = MOST_ITEMS;
        for (ItemEntity item : raw) {
            if (!item.isAlive() || budget <= 0) {
                continue;
            }
            ItemStack stack = item.getItem();
            Optional<RecipeHolder<SmeltingRecipe>> recipe = recipe(level, stack);
            if (recipe.isEmpty()) {
                continue;
            }
            int cooked = Math.min(budget, stack.getCount());
            budget -= cooked;
            ItemStack result = recipe.get().value().getResultItem(level.registryAccess()).copy();
            result.setCount(result.getCount() * cooked);
            float xp = recipe.get().value().getExperience() * cooked;
            int orbs = Mth.floor(xp) + (level.getRandom().nextFloat() < Mth.frac(xp) ? 1 : 0);
            if (orbs > 0) {
                ExperienceOrb.award(level, item.position(), orbs);
            }
            if (cooked >= stack.getCount()) {
                item.setItem(result);
            } else {
                stack.shrink(cooked);
                item.setItem(stack.copy());
                ItemEntity done = new ItemEntity(level, item.getX(), item.getY(), item.getZ(), result);
                done.setDefaultPickUpDelay();
                level.addFreshEntity(done);
            }
            level.sendParticles(ParticleTypes.SMOKE, item.getX(), item.getY() + 0.2, item.getZ(),
                    4, 0.15, 0.1, 0.15, 0.01);
        }
        if (!raw.isEmpty()) {
            level.playSound(null, raw.get(0).blockPosition(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.PLAYERS,
                    1F, 1F);
        }
    }

    private static boolean heatable(ServerLevel level, BlockState state) {
        if (state.getBlock() instanceof CampfireBlock) {
            return CampfireBlock.canLight(state);
        }
        if (state.getBlock() instanceof CandleBlock) {
            return CandleBlock.canLight(state);
        }
        if (state.getBlock() instanceof CandleCakeBlock) {
            return CandleCakeBlock.canLight(state);
        }
        return state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE);
    }

    private static List<BlockPos> heated(ServerLevel level, ServerPlayer player, Vec3 focus) {
        List<BlockPos> out = new ArrayList<>();
        BlockPos centre = BlockPos.containing(focus);
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-HEAT_RADIUS, -HEAT_RADIUS, -HEAT_RADIUS),
                centre.offset(HEAT_RADIUS, HEAT_RADIUS, HEAT_RADIUS))) {
            if (heatable(level, level.getBlockState(pos)) && level.mayInteract(player, pos)) {
                out.add(pos.immutable());
            }
        }
        return out;
    }

    private static void heat(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.LIT)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL_IMMEDIATE);
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.8F, 1F);
        } else if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
            level.setBlock(pos, level.dimensionType().ultraWarm() ? Blocks.AIR.defaultBlockState()
                    : Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                3, 0.2, 0.15, 0.2, 0.01);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                2, 0.2, 0.1, 0.2, 0.01);
    }
}
