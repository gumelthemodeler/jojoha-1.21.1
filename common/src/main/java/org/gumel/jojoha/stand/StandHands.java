package org.gumel.jojoha.stand;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.accessory.Charms;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.moves.PreciseEyesSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StandHands {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_hands");

    public static final double CLOSE_REACH = 15.0;
    public static final double LONG_REACH = 30.0;

    public static double reachFor(JojohaPlayerData data) {
        return StandTypes.byIdOrDefault(data.stand.standId()).range() == StandRange.LONG
                ? LONG_REACH
                : CLOSE_REACH;
    }

    private static final int COOLDOWN_TICKS = 4;

    private static final float ENERGY_COST = EnergyWeight.TRIVIAL.cost();

    private static final double AUDIBLE_RANGE = 8.0;

    private static final int HOLD_TICKS = 10;

    private static final float DIG_HARDNESS_STEP = 1.5F;

    private StandHands() {
    }

    public static void handleUseRequest(ServerPlayer player, Optional<BlockPos> stretchAnchor,
                                        int turns) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        int turned = Math.max(0, Math.min(turns, PlacementTurn.TURNS - 1));

        if (!data.standSummoned || !data.stand.isPresent() || !data.stand.trust().canActAtRange()) {
            return;
        }

        if (!data.standMode.handlesItems()) {
            return;
        }

        if (data.isTimeStopFrozen()) {
            return;
        }

        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand == null) {
            return;
        }

        long now = player.level().getGameTime();
        if (data.isMoveOnCooldown(ID, now)) {
            return;
        }

        if (data.standEnergy < cost(player, data)) {
            player.displayClientMessage(Component.translatable("message.jojoha.skill.no_energy"), true);
            return;
        }

        if (StandUtilityWork.isBusy(player)) {
            return;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            if (!canDelegate(player, player.getItemInHand(hand))) {
                continue;
            }

            double reach = reachFor(data);
            BlockHitResult hit = aimFrom(player, player.getEyePosition(),
                    player.getViewVector(1.0F), reach);
            data.setMoveCooldown(ID, now, COOLDOWN_TICKS);
            PlayerDataAccess.set(player, data);

            if (DiscToss.plays(player.getItemInHand(hand))) {
                DiscToss.toss(player, stand, hand, reach);
                return;
            }

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos aimed = new BlockPlaceContext(
                        player, hand, player.getItemInHand(hand), hit).getClickedPos();
                StandUtilityWork.queue(player, stand, run(player, stretchAnchor, hit, hand, reach),
                        hit.getDirection(), PlacementRun.grip(hit, aimed), turned, hand);
                return;
            }

            if (stretchAnchor.isPresent() && fills(player.getItemInHand(hand))
                    && data(player).buildMode.stretches()
                    && !data(player).buildMode.fillsAVolume()) {
                List<BlockPos> pillar = reachable(player,
                        PlacementRun.between(stretchAnchor.get(),
                                PlacementRun.farEnd(stretchAnchor.get(), player.getEyePosition(),
                                        player.getViewVector(1.0F), reach, data(player).buildMode),
                                data(player).buildMode),
                        reach);
                if (!pillar.isEmpty()) {
                    StandUtilityWork.queue(player, stand, pillar, Direction.UP, PlacementRun.CENTRE,
                            turned, hand);
                    return;
                }
            }

            performQueued(player, stand, hand, hit, turned);
            return;
        }
    }

    private static void echo(ServerPlayer player, ItemStack used, BlockPos cell) {
        BlockState placed = player.level().getBlockState(cell);

        SoundEvent sound;
        if (used.getItem() instanceof BucketItem) {
            boolean poured = !placed.getFluidState().isEmpty();
            boolean lava = poured && placed.getFluidState().is(FluidTags.LAVA);
            sound = poured
                    ? (lava ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY)
                    : SoundEvents.BUCKET_FILL;
        } else if (!placed.isAir()) {
            sound = placed.getSoundType().getPlaceSound();
        } else {
            return;
        }

        SoundType type = placed.getSoundType();

        Vec3 at = player.getEyePosition();
        Vec3 toCell = Vec3.atCenterOf(cell).subtract(at);
        double distance = toCell.length();
        Vec3 source = distance <= AUDIBLE_RANGE || distance < 1.0E-4
                ? Vec3.atCenterOf(cell)
                : at.add(toCell.scale(AUDIBLE_RANGE / distance));

        player.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                SoundSource.BLOCKS,
                source.x, source.y, source.z,
                (type.getVolume() + 1F) / 2F, type.getPitch() * 0.8F,
                player.level().getRandom().nextLong()));
    }

    private static List<BlockPos> run(ServerPlayer player, Optional<BlockPos> anchor,
                                                BlockHitResult hit, InteractionHand hand, double reach) {
        List<BlockPos> cells = shape(player, anchor, hit, hand, reach);
        if (!digging(player, hand)) {
            return cells;
        }
        List<BlockPos> solid = new ArrayList<>();
        for (BlockPos cell : cells) {
            BlockState state = player.level().getBlockState(cell);
            if (!state.isAir() && state.getDestroySpeed(player.level(), cell) >= 0F) {
                solid.add(cell);
            }
        }
        return solid.isEmpty() ? List.of(hit.getBlockPos()) : solid;
    }

    private static List<BlockPos> shape(ServerPlayer player, Optional<BlockPos> anchor,
                                                  BlockHitResult hit, InteractionHand hand, double reach) {
        BlockPos far = digging(player, hand) ? hit.getBlockPos()
                : new BlockPlaceContext(
                        player, hand, player.getItemInHand(hand), hit).getClickedPos();

        if (anchor.isEmpty() || !data(player).buildMode.stretches()) {
            return List.of(far);
        }

        if (data(player).buildMode.fillsAVolume()) {
            List<BlockPos> volume = reachable(player,
                    PlacementRun.box(anchor.get(), far), reach);
            return volume.isEmpty() ? List.of(far) : volume;
        }

        List<BlockPos> cells = reachable(player,
                PlacementRun.between(anchor.get(), far, data(player).buildMode), reach);
        return cells.isEmpty() ? List.of(far) : cells;
    }

    private static JojohaPlayerData data(ServerPlayer player) {
        return PlayerDataAccess.get(player);
    }

    private static List<BlockPos> reachable(ServerPlayer player,
                                                      List<BlockPos> cells, double reach) {
        List<BlockPos> kept = new ArrayList<>();
        double reachSqr = reach * reach;
        for (BlockPos cell : cells) {
            if (player.getEyePosition().distanceToSqr(Vec3.atCenterOf(cell)) <= reachSqr) {
                kept.add(cell);
            }
        }
        return kept;
    }

    public static boolean canAfford(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        return data.standEnergy >= cost(player, data);
    }

    private static float cost(ServerPlayer player, JojohaPlayerData data) {
        return Charms.freeUtility(player, data) ? 0F : ENERGY_COST;
    }

    public static boolean fills(ItemStack stack) {
        return stack.getItem() instanceof BlockItem
                || stack.getItem() instanceof BucketItem;
    }

    public static boolean digs(ItemStack stack) {
        return stack.getItem() instanceof DiggerItem
                && !(stack.getItem() instanceof HoeItem)
                || stack.getItem() instanceof ShearsItem;
    }

    public static boolean shapes(ItemStack stack) {
        return fills(stack) || digs(stack);
    }

    private static boolean digging(Player player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND && digs(player.getItemInHand(hand));
    }

    private static void dig(ServerPlayer player, StandEntity stand, BlockPos cell) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(cell);
        float hardness = state.getDestroySpeed(level, cell);
        if (state.isAir() || hardness < 0F) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        float price = cost(player, data) * Math.max(1F, hardness / DIG_HARDNESS_STEP);
        if (data.standEnergy < price) {
            player.displayClientMessage(Component.translatable("message.jojoha.skill.no_energy"), true);
            return;
        }
        ItemStack shown = player.getMainHandItem().copy();
        stand.reachTowards(Vec3.atCenterOf(cell));
        Runnable steady = PreciseEyesSkill.focused(player)
                ? PreciseEyesSkill.steady(level, cell) : null;
        if (!player.gameMode.destroyBlock(cell)) {
            return;
        }
        if (steady != null) {
            steady.run();
        }
        stand.showHeldItem(shown, HOLD_TICKS);
        data = PlayerDataAccess.get(player);
        data.standEnergy = Math.max(0F, data.standEnergy - price);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static void performQueued(ServerPlayer player, StandEntity stand, InteractionHand hand,
                                     BlockHitResult hit, int turns) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.standEnergy < cost(player, data) || !canDelegate(player, player.getItemInHand(hand))) {
            return;
        }

        if (digging(player, hand)) {
            if (hit.getType() == HitResult.Type.BLOCK) {
                dig(player, stand, hit.getBlockPos());
            }
            return;
        }

        ItemStack shown = player.getItemInHand(hand).copy();

        BlockPos cell = hit.getType() == HitResult.Type.BLOCK
                ? new BlockPlaceContext(player, hand, shown, hit)
                        .getClickedPos()
                : null;
        if (!perform(player, stand, hand, hit).consumesAction()) {
            return;
        }

        stand.showHeldItem(shown, HOLD_TICKS);
        if (cell != null) {
            PlacementTurn.apply(player.serverLevel(), cell, shown, turns);
            echo(player, shown, cell);
        }
        data.standEnergy -= cost(player, data);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    private static boolean canDelegate(ServerPlayer player, ItemStack stack) {
        return !stack.isEmpty() && stack.getUseDuration(player) <= 0;
    }

    private static InteractionResult perform(ServerPlayer player, StandEntity stand,
                                             InteractionHand hand, BlockHitResult hit) {
        ServerLevel level = player.serverLevel();

        AttributeInstance reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        double homeReach = reach == null ? 0 : reach.getBaseValue();
        Vec3 home = player.position();
        boolean homeBlocksBuilding = player.blocksBuilding;
        float homeYRot = player.getYRot();
        float homeXRot = player.getXRot();

        if (reach != null) {
            reach.setBaseValue(LONG_REACH);
        }

        player.setPos(stand.getX(), stand.getEyeY() - player.getEyeHeight(), stand.getZ());

        player.blocksBuilding = false;

        if (hit.getType() == HitResult.Type.BLOCK
                && !(player.getItemInHand(hand).getItem() instanceof BlockItem)) {
            lookAt(player, hit.getLocation());
        }

        try {
            stand.reachTowards(hit.getType() == HitResult.Type.BLOCK
                    ? Vec3.atCenterOf(hit.getBlockPos())
                    : player.getEyePosition().add(player.getViewVector(1.0F).scale(LONG_REACH)));

            if (hit.getType() == HitResult.Type.BLOCK) {
                InteractionResult onBlock = player.getItemInHand(hand)
                        .useOn(new UseOnContext(player, hand, hit));
                if (onBlock != InteractionResult.PASS) {
                    return onBlock;
                }
            }

            ItemStack inHand = player.getItemInHand(hand);
            if (inHand.isEmpty()) {
                return InteractionResult.PASS;
            }

            InteractionResult thrown = player.gameMode.useItem(player, level, inHand, hand);
            if (thrown.consumesAction() && empower(player, stand)) {
                stand.triggerThrowPose();
            }
            return thrown;
        } finally {
            player.setPos(home.x, home.y, home.z);
            player.blocksBuilding = homeBlocksBuilding;
            player.setYRot(homeYRot);
            player.setXRot(homeXRot);
            if (reach != null) {
                reach.setBaseValue(homeReach);
            }
        }
    }

    private static void lookAt(ServerPlayer player, Vec3 target) {
        Vec3 delta = target.subtract(player.getEyePosition());
        double flat = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        player.setYRot((float) (Mth.atan2(delta.z, delta.x) * (180F / Math.PI)) - 90F);
        player.setXRot((float) (-(Mth.atan2(delta.y, flat) * (180F / Math.PI))));
    }

    private static final double THROW_POWER = 2.4;

    private static boolean empower(ServerPlayer player, StandEntity stand) {
        boolean threw = false;
        for (Projectile projectile : player.serverLevel().getEntitiesOfClass(Projectile.class,
                stand.getBoundingBox().inflate(3.0),
                candidate -> candidate.tickCount == 0 && candidate.getOwner() == player)) {

            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(THROW_POWER));
            projectile.hasImpulse = true;
            player.serverLevel().getChunkSource().broadcast(projectile,
                    new ClientboundSetEntityMotionPacket(projectile));
            threw = true;
        }
        return threw;
    }

    public static BlockHitResult aimFrom(Player player, Vec3 eye, Vec3 look, double reach) {
        Vec3 end = eye.add(look.scale(reach));

        BlockHitResult solid = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (solid.getType() == HitResult.Type.BLOCK) {
            return solid;
        }

        BlockHitResult wet = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (wet.getType() == HitResult.Type.BLOCK) {
            return wet;
        }

        return nearby(player, end);
    }

    private static BlockHitResult nearby(Player player, Vec3 end) {
        BlockPos at = BlockPos.containing(end);

        for (Direction side : SNAP_ORDER) {
            BlockPos neighbour = at.relative(side);
            if (!player.level().getBlockState(neighbour).isSolidRender(player.level(), neighbour)) {
                continue;
            }
            return new BlockHitResult(Vec3.atCenterOf(neighbour), side.getOpposite(), neighbour, false);
        }

        return BlockHitResult.miss(end, Direction.UP, at);
    }

    private static final Direction[] SNAP_ORDER = {
            Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
            Direction.UP};
}
