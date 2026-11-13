package org.gumel.jojoha.stand.skill.moves;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandHands;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ChoreSkill implements StandSkill {
    private enum Kind {
        FETCH,
        HARVEST,
        REACH,
        FELL,
        CARRY
    }

    public static final ChoreSkill FETCH = new ChoreSkill(Kind.FETCH, "stand_fetch", 40,
            EnergyWeight.LIGHT.cost());
    public static final ChoreSkill HARVEST = new ChoreSkill(Kind.HARVEST, "stand_harvest", 60,
            EnergyWeight.LIGHT.cost());
    public static final ChoreSkill REACH = new ChoreSkill(Kind.REACH, "stand_reach", 4,
            EnergyWeight.TRIVIAL.cost());
    public static final ChoreSkill FELL = new ChoreSkill(Kind.FELL, "stand_fell", 100,
            EnergyWeight.LIGHT.cost());
    public static final ChoreSkill CARRY = new ChoreSkill(Kind.CARRY, "stand_carry", 10,
            EnergyWeight.TRIVIAL.cost());

    private static final int FETCH_LIMIT = 64;
    private static final int HARVEST_RADIUS = 5;
    private static final int HARVEST_DEPTH = 2;
    private static final int HARVEST_LIMIT = 64;

    private static final int FELL_LIMIT = 32;

    private static final double CARRY_FORGIVENESS = 0.8;
    private static final double CARRY_WIDEST = 1.5;

    private static final Set<UUID> CARRYING = new HashSet<>();

    private final Kind kind;
    private final ResourceLocation id;
    private final int cooldownTicks;
    private final float energyCost;

    private ChoreSkill(Kind kind, String path, int cooldownTicks, float energyCost) {
        this.kind = kind;
        this.id = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
        this.cooldownTicks = cooldownTicks;
        this.energyCost = energyCost;
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> CARRYING.removeIf(holder -> setDown(server, holder)));
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha." + id.getPath();
    }

    @Override
    public boolean isUnlocked(JojohaPlayerData data) {
        return data.stand.isPresent();
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
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        return switch (kind) {
            case FETCH -> dropped(player, data).isEmpty() ? "message.jojoha.chore.nothing_dropped" : null;
            case HARVEST -> grown(player, data).isEmpty() ? "message.jojoha.chore.nothing_grown" : null;
            case REACH -> switchAt(player, data) == null ? "message.jojoha.chore.nothing_to_reach" : null;
            case FELL -> tree(player, data).isEmpty() ? "message.jojoha.chore.not_a_tree" : null;
            case CARRY -> carrier(player) != null || liftable(player, data) != null
                    ? null : "message.jojoha.chore.nothing_to_carry";
        };
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        return switch (kind) {
            case FETCH -> fetch(player, data, stand);
            case HARVEST -> harvest(player, data, stand);
            case REACH -> reach(player, data, stand);
            case FELL -> fell(player, data, stand);
            case CARRY -> carry(player, data, stand);
        };
    }

    static BlockHitResult aim(ServerPlayer player, JojohaPlayerData data) {
        return StandHands.aimFrom(player, player.getEyePosition(), player.getViewVector(1.0F),
                StandHands.reachFor(data));
    }

    private static List<ItemEntity> dropped(ServerPlayer player, JojohaPlayerData data) {
        double reach = StandHands.reachFor(data);
        List<ItemEntity> found = player.level().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(reach),
                item -> item.isAlive() && item.distanceToSqr(player) <= reach * reach);
        return found.size() > FETCH_LIMIT ? found.subList(0, FETCH_LIMIT) : found;
    }

    private static boolean fetch(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        List<ItemEntity> items = dropped(player, data);
        if (items.isEmpty()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        if (stand != null) {
            stand.reachTowards(items.get(0).position());
        }
        for (ItemEntity item : items) {
            item.teleportTo(player.getX(), player.getY() + 0.2, player.getZ());
            item.setDeltaMovement(Vec3.ZERO);
            item.setNoPickUpDelay();
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 0.8F);
        return true;
    }

    private static boolean ripe(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        return state.getBlock() instanceof NetherWartBlock
                && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
    }

    private static BlockState sown(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.getStateForAge(0);
        }
        return state.setValue(NetherWartBlock.AGE, 0);
    }

    private static List<BlockPos> grown(ServerPlayer player, JojohaPlayerData data) {
        BlockHitResult hit = aim(player, data);
        List<BlockPos> out = new ArrayList<>();
        if (hit.getType() != HitResult.Type.BLOCK) {
            return out;
        }
        BlockPos centre = hit.getBlockPos();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-HARVEST_RADIUS, -HARVEST_DEPTH, -HARVEST_RADIUS),
                centre.offset(HARVEST_RADIUS, HARVEST_DEPTH, HARVEST_RADIUS))) {
            if (ripe(player.level().getBlockState(pos))) {
                out.add(pos.immutable());
                if (out.size() >= HARVEST_LIMIT) {
                    break;
                }
            }
        }
        return out;
    }

    private static boolean harvest(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        List<BlockPos> crops = grown(player, data);
        if (crops.isEmpty()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        if (stand != null) {
            stand.reachTowards(Vec3.atCenterOf(crops.get(0)));
        }
        for (BlockPos pos : crops) {
            BlockState state = level.getBlockState(pos);
            ItemStack seed = state.getBlock().getCloneItemStack(level, pos, state);
            List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, ItemStack.EMPTY);
            boolean replanted = false;
            for (ItemStack drop : drops) {
                if (!replanted && !seed.isEmpty() && ItemStack.isSameItem(drop, seed)) {
                    drop.shrink(1);
                    replanted = true;
                }
            }
            level.setBlock(pos, replanted ? sown(state) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, 2, 0.25, 0.2, 0.25, 0.0);
            for (ItemStack drop : drops) {
                if (!drop.isEmpty() && !player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
            }
        }
        level.playSound(null, crops.get(0), SoundEvents.CROP_BREAK, SoundSource.PLAYERS, 1F, 1F);
        level.playSound(null, crops.get(0), SoundEvents.CROP_PLANTED, SoundSource.PLAYERS, 1F, 1F);
        return true;
    }

    static boolean workable(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof DoorBlock door) {
            return door.type().canOpenByHand();
        }
        if (block instanceof TrapDoorBlock) {
            return !state.is(Blocks.IRON_TRAPDOOR);
        }
        return block instanceof LeverBlock || block instanceof ButtonBlock || block instanceof FenceGateBlock
                || block instanceof BellBlock || block instanceof NoteBlock || block instanceof RepeaterBlock
                || block instanceof ComparatorBlock || block instanceof DaylightDetectorBlock;
    }

    private static BlockHitResult switchAt(ServerPlayer player, JojohaPlayerData data) {
        BlockHitResult hit = aim(player, data);
        if (hit.getType() != HitResult.Type.BLOCK || !workable(player.level().getBlockState(hit.getBlockPos()))) {
            return null;
        }
        return hit;
    }

    private static boolean reach(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        BlockHitResult hit = switchAt(player, data);
        if (hit == null) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (stand != null) {
            stand.reachTowards(Vec3.atCenterOf(hit.getBlockPos()));
        }
        return state.useWithoutItem(level, player, hit).consumesAction();
    }

    private static boolean natural(BlockState state) {
        return state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT)
                && !state.getValue(LeavesBlock.PERSISTENT);
    }

    private static List<BlockPos> tree(ServerPlayer player, JojohaPlayerData data) {
        BlockHitResult hit = aim(player, data);
        List<BlockPos> logs = new ArrayList<>();
        if (hit.getType() != HitResult.Type.BLOCK) {
            return logs;
        }
        ServerLevel level = player.serverLevel();
        BlockState first = level.getBlockState(hit.getBlockPos());
        if (!first.is(BlockTags.LOGS)) {
            return logs;
        }
        Block kind = first.getBlock();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(hit.getBlockPos().immutable());
        seen.add(hit.getBlockPos().immutable());
        boolean leafy = false;
        while (!open.isEmpty() && logs.size() < FELL_LIMIT) {
            BlockPos at = open.poll();
            logs.add(at);
            for (BlockPos next : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                BlockState state = level.getBlockState(next);
                if (!leafy && natural(state)) {
                    leafy = true;
                }
                if (state.is(kind) && seen.add(next.immutable())) {
                    open.add(next.immutable());
                }
            }
        }
        if (!leafy) {
            logs.clear();
        }
        return logs;
    }

    private static boolean fell(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        List<BlockPos> logs = tree(player, data);
        if (logs.isEmpty()) {
            return false;
        }
        logs.sort(Comparator.comparingInt((BlockPos pos) -> pos.getY()).reversed());
        ServerLevel level = player.serverLevel();
        if (stand != null) {
            stand.reachTowards(Vec3.atCenterOf(logs.get(logs.size() - 1)));
        }
        int felled = 0;
        for (BlockPos pos : logs) {
            if (player.gameMode.destroyBlock(pos)) {
                felled++;
            }
        }
        if (felled > 0) {
            level.playSound(null, logs.get(logs.size() - 1), SoundEvents.AXE_STRIP, SoundSource.PLAYERS, 1F, 0.6F);
        }
        return felled > 0;
    }

    private static Mob liftable(ServerPlayer player, JojohaPlayerData data) {
        LivingEntity seen = VineHaul.look(player, StandHands.reachFor(data), CARRY_FORGIVENESS);
        if (seen instanceof Mob mob && !(mob instanceof Enemy)
                && mob.getBbWidth() <= CARRY_WIDEST && !mob.isPassenger() && !mob.isVehicle()) {
            return mob;
        }
        return null;
    }

    private static StandEntity carrier(ServerPlayer player) {
        StandEntity stand = StandEntity.findFor(player);
        return stand != null && stand.isCarrying() ? stand : null;
    }

    private static boolean carry(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();
        StandEntity holder = carrier(player);
        if (holder != null) {
            LivingEntity held = holder.heldEntity();
            holder.releaseHeld(Vec3.ZERO);
            CARRYING.remove(player.getUUID());
            if (held != null) {
                level.playSound(null, held.blockPosition(), SoundEvents.LEASH_KNOT_BREAK, SoundSource.PLAYERS, 0.7F, 1F);
            }
            return true;
        }
        Mob mob = liftable(player, data);
        if (mob == null || stand == null || !stand.carry(mob)) {
            return false;
        }
        CARRYING.add(player.getUUID());
        level.playSound(null, mob.blockPosition(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 0.8F, 1F);
        return true;
    }

    private static boolean setDown(MinecraftServer server, UUID holder) {
        ServerPlayer player = server.getPlayerList().getPlayer(holder);
        if (player == null) {
            return true;
        }
        StandEntity stand = carrier(player);
        if (stand == null) {
            return true;
        }
        if (!player.isAlive() || !PlayerDataAccess.get(player).inUtilityStance()) {
            stand.releaseHeld(Vec3.ZERO);
            return true;
        }
        return false;
    }
}
