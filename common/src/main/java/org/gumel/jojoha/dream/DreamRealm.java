package org.gumel.jojoha.dream;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.DreamMemory;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.fortune.Devout;
import org.gumel.jojoha.stand.StandSummonHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DreamRealm {
    public static final ResourceKey<Level> DREAM = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "dream"));

    static final ResourceLocation AGAIN =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "dream/again");

    static final int BETWEEN_DREAMS_TICKS = 24000 * 3;

    public static final float CHANCE = 0.08F;

    static final int ROLL_AT_SLEEP_TICKS = 96;

    private static final int ROOMS = 4096;
    private static final int ROOMS_PER_ROW = 64;
    private static final int ROOM_SPACING = 512;
    public static final int FLOOR_Y = 64;

    private static final double CAUGHT_BELOW = 1.5;

    public static final int FLOOR_RADIUS = 40;

    private static final int WALL_THICKNESS = 2;
    private static final int WALL_HEIGHT = 3;

    private static final float FACING_NORTH = 180F;

    private static final int NEAR_CANDLES = 14;
    private static final double NEAR_RADIUS = 5.5;
    private static final int FAR_CANDLES = 22;
    private static final double FAR_RADIUS = 10.0;
    private static final int OUTER_CANDLES = 34;
    private static final double OUTER_RADIUS = 17.0;
    private static final double CANDLE_JITTER = 0.9;

    private static final int CLUSTERS = 18;
    private static final double CLUSTERS_FROM = 22.0;
    private static final double CLUSTERS_TO = 36.0;
    private static final double CLUSTER_SPREAD = 1.6;

    private static final double LEASH = FLOOR_RADIUS + WALL_THICKNESS + 2.0;

    private static final Map<UUID, DreamSession> SESSIONS = new HashMap<>();

    private DreamRealm() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(DreamRealm::tick);

        BlockEvent.BREAK.register((level, pos, state, player, xp) ->
                level.dimension().equals(DREAM) ? EventResult.interruptFalse() : EventResult.pass());
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) ->
                player.level().dimension().equals(DREAM) ? EventResult.interruptFalse() : EventResult.pass());
    }

    private static void tick(Player player) {
        if (!(player instanceof ServerPlayer asleep)) {
            return;
        }

        if (asleep.level().dimension().equals(DREAM)) {
            DreamSession session = SESSIONS.get(asleep.getUUID());
            if (session == null) {
                sendHome(asleep, null);
                return;
            }
            hold(asleep);
            session.tick(asleep);
            return;
        }

        if (asleep.isSleeping() && asleep.getSleepTimer() == ROLL_AT_SLEEP_TICKS) {
            roll(asleep);
        }
    }

    private static void roll(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        long now = player.level().getGameTime();
        if (lucid(player, data, now) || data.isMoveOnCooldown(AGAIN, now)) {
            return;
        }
        if (player.getRandom().nextFloat() >= JojohaServerConfig.DREAM_CHANCE.chance() * Devout.dreamOdds(data)) {
            return;
        }
        take(player, data, now);
    }

    public static final ResourceLocation LUCID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "lucid_lily");
    private static final int LUCID_TICKS = Integer.MAX_VALUE / 2;

    public static boolean eatLily(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.setMoveCooldown(LUCID, player.level().getGameTime(), LUCID_TICKS);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        player.displayClientMessage(Component.translatable("message.jojoha.lucid_lily"), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1F, 0.6F);
        return true;
    }

    private static boolean lucid(ServerPlayer player, JojohaPlayerData data, long now) {
        if (!data.isMoveOnCooldown(LUCID, now)) {
            return false;
        }
        data.moveCooldowns.remove(LUCID);
        take(player, data, now);
        return true;
    }

    public static void take(ServerPlayer player, JojohaPlayerData data, long now) {
        ServerLevel dream = player.server.getLevel(DREAM);
        if (dream == null) {
            return;
        }

        BlockPos bed = player.getSleepingPos().orElse(player.blockPosition());
        ResourceKey<Level> from = player.level().dimension();

        if (data.standSummoned) {
            StandSummonHandler.dismissImmediately(player, data);
        }

        player.stopSleepInBed(false, true);

        BlockPos centre = room(player.getUUID());
        floor(dream, centre);
        candles(dream, centre, player.getUUID());

        player.teleportTo(dream, centre.getX() + 0.5, FLOOR_Y, centre.getZ() + 0.5,
                FACING_NORTH, 0F);
        player.setInvulnerable(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0F;

        long today = player.server.overworld().getDayTime() / 24000L;
        DreamMemory before = data.dreamMemory;
        String stand = data.stand.isPresent() ? data.stand.standId().toString() : "";
        data.dreamMemory = before.arrived(today, stand);
        SESSIONS.put(player.getUUID(), new DreamSession(from, bed, before, today));

        data.setMoveCooldown(AGAIN, now, BETWEEN_DREAMS_TICKS);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        dream.playSound(null, player.blockPosition(),
                SoundEvents.PORTAL_TRAVEL, SoundSource.AMBIENT, 0.25F, 0.5F);
    }

    public static void sendHome(ServerPlayer player, DreamSession session) {
        ResourceKey<Level> key = session != null ? session.from() : player.getRespawnDimension();
        BlockPos bed = session != null ? session.bed() : player.getRespawnPosition();

        ServerLevel back = key == null ? null : player.server.getLevel(key);
        if (back == null) {
            back = player.server.overworld();
            bed = null;
        }
        if (bed == null) {
            bed = back.getSharedSpawnPos();
        }

        List<ItemStack> left = session != null ? session.close(player) : List.of();
        SESSIONS.remove(player.getUUID());

        player.setInvulnerable(false);
        player.teleportTo(back, bed.getX() + 0.5, bed.getY() + 0.6, bed.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0F;

        for (ItemStack stack : left) {
            player.getInventory().placeItemBackInInventory(stack);
        }

        if (back.getBlockState(bed).getBlock() instanceof BedBlock) {
            player.startSleepInBed(bed);
        }
    }

    public static void wake(ServerPlayer player) {
        sendHome(player, SESSIONS.get(player.getUUID()));
    }

    public static DreamSession session(ServerPlayer player) {
        return SESSIONS.get(player.getUUID());
    }

    public static boolean dreaming(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static void answer(ServerPlayer player, int turn, int index) {
        DreamSession session = SESSIONS.get(player.getUUID());
        if (session != null) {
            session.answer(player, turn, index);
        }
    }

    public static void allowAgain(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.moveCooldowns.remove(AGAIN);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static void forget(ServerPlayer player) {
        DreamSession session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            session.release(player);
        }
    }

    private static void hold(ServerPlayer player) {
        BlockPos centre = room(player.getUUID());
        double dx = player.getX() - (centre.getX() + 0.5);
        double dz = player.getZ() - (centre.getZ() + 0.5);
        BlockPos under = BlockPos.containing(player.getX(), FLOOR_Y - 1, player.getZ());
        if (dx * dx + dz * dz <= FLOOR_RADIUS * FLOOR_RADIUS
                && !player.serverLevel().getBlockState(under).is(Blocks.BARRIER)) {
            Jojoha.LOGGER.warn("The dream's floor was missing under {} at {}; laying it again",
                    player.getName().getString(), under);
            floor(player.serverLevel(), centre);
        }
        if (dx * dx + dz * dz > LEASH * LEASH || player.getY() < FLOOR_Y - CAUGHT_BELOW) {
            player.teleportTo(centre.getX() + 0.5, FLOOR_Y, centre.getZ() + 0.5);
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0F;
        }
    }

    static AABB area(ServerPlayer player) {
        BlockPos centre = room(player.getUUID());
        double reach = LEASH + 4;
        return new AABB(centre.getX() + 0.5 - reach, player.level().getMinBuildHeight(),
                centre.getZ() + 0.5 - reach, centre.getX() + 0.5 + reach, FLOOR_Y + 48,
                centre.getZ() + 0.5 + reach);
    }

    public static BlockPos room(UUID player) {
        int index = Math.floorMod(player.hashCode(), ROOMS);
        return new BlockPos((index % ROOMS_PER_ROW) * ROOM_SPACING, FLOOR_Y,
                (index / ROOMS_PER_ROW) * ROOM_SPACING);
    }

    private static void floor(ServerLevel dream, BlockPos centre) {
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int reach = FLOOR_RADIUS + WALL_THICKNESS;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double out = Math.sqrt(dx * dx + dz * dz);
                if (out > reach) {
                    continue;
                }
                lay(dream, at.set(centre.getX() + dx, FLOOR_Y - 1, centre.getZ() + dz), barrier);
                if (out > FLOOR_RADIUS) {
                    for (int up = 0; up < WALL_HEIGHT; up++) {
                        lay(dream, at.set(centre.getX() + dx, FLOOR_Y + up, centre.getZ() + dz), barrier);
                    }
                }
            }
        }
    }

    private static void lay(ServerLevel dream, BlockPos at, BlockState state) {
        if (!dream.getBlockState(at).is(state.getBlock())) {
            dream.setBlock(at, state, Block.UPDATE_CLIENTS);
        }
    }

    private static void candles(ServerLevel dream, BlockPos centre, UUID player) {
        RandomSource random = RandomSource.create(player.getLeastSignificantBits());
        ring(dream, centre, random, NEAR_CANDLES, NEAR_RADIUS);
        ring(dream, centre, random, FAR_CANDLES, FAR_RADIUS);
        ring(dream, centre, random, OUTER_CANDLES, OUTER_RADIUS);
        clusters(dream, centre, random);
    }

    private static void ring(ServerLevel dream, BlockPos centre, RandomSource random, int count, double radius) {
        for (Wick wick : wicks(centre, random, count, radius)) {
            candle(dream, wick.at(), Blocks.WHITE_CANDLE, wick.candles(), true);
        }
    }

    record Wick(BlockPos at, int candles) {
    }

    private static List<Wick> wicks(BlockPos centre, RandomSource random, int count, double radius) {
        List<Wick> wicks = new ArrayList<>();
        double offset = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < count; i++) {
            double angle = offset + i * Math.PI * 2 / count + (random.nextDouble() - 0.5) * 0.25;
            double reach = radius + (random.nextDouble() - 0.5) * 2 * CANDLE_JITTER;
            wicks.add(new Wick(BlockPos.containing(centre.getX() + 0.5 + Math.cos(angle) * reach, FLOOR_Y,
                    centre.getZ() + 0.5 + Math.sin(angle) * reach), 1 + random.nextInt(4)));
        }
        return wicks;
    }

    static List<Wick> nearWicks(UUID player) {
        return wicks(room(player), RandomSource.create(player.getLeastSignificantBits()), NEAR_CANDLES, NEAR_RADIUS);
    }

    static void tally(ServerPlayer player, float met, float beyond) {
        List<Wick> wicks = nearWicks(player.getUUID());
        int lit = Math.round(wicks.size() * Math.max(0F, Math.min(1F, met)));
        int gold = Math.round(wicks.size() * Math.max(0F, Math.min(1F, beyond)));
        for (int i = 0; i < wicks.size(); i++) {
            Wick wick = wicks.get(i);
            candle(player.serverLevel(), wick.at(), i < gold ? Blocks.YELLOW_CANDLE : Blocks.WHITE_CANDLE,
                    wick.candles(), i < lit);
        }
    }

    private static void clusters(ServerLevel dream, BlockPos centre, RandomSource random) {
        for (int i = 0; i < CLUSTERS; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double reach = CLUSTERS_FROM + random.nextDouble() * (CLUSTERS_TO - CLUSTERS_FROM);
            double x = centre.getX() + 0.5 + Math.cos(angle) * reach;
            double z = centre.getZ() + 0.5 + Math.sin(angle) * reach;
            int count = 2 + random.nextInt(4);
            for (int c = 0; c < count; c++) {
                double turn = random.nextDouble() * Math.PI * 2;
                double off = random.nextDouble() * CLUSTER_SPREAD;
                candle(dream, BlockPos.containing(x + Math.cos(turn) * off, FLOOR_Y, z + Math.sin(turn) * off),
                        random);
            }
        }
    }

    private static void candle(ServerLevel dream, BlockPos at, RandomSource random) {
        candle(dream, at, Blocks.WHITE_CANDLE, 1 + random.nextInt(4), true);
    }

    private static void candle(ServerLevel dream, BlockPos at, Block kind, int candles, boolean lit) {
        BlockState candle = kind.defaultBlockState()
                .setValue(CandleBlock.CANDLES, candles)
                .setValue(CandleBlock.LIT, lit);
        if (!dream.getBlockState(at).equals(candle)) {
            dream.setBlockAndUpdate(at, candle);
        }
    }
}
