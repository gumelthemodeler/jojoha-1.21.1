package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Clairvoyance {
    private static final double BLOCK_RANGE = 24.0;
    private static final double ENTITY_RANGE = 48.0;

    private static final int SCAN_INTERVAL = 20;

    private static final TagKey<Block> VALUABLES = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "clairvoyant"));

    private static final TagKey<Block> OBJECTIVES = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "clairvoyant_objectives"));

    private static final float[] VALUABLE_TINT = {1.00F, 0.74F, 0.22F};
    private static final float[] OBJECTIVE_TINT = {0.72F, 0.42F, 1.00F};
    private static final int HOSTILE_COLOUR = 0xE03A3A;
    private static final int CREATURE_COLOUR = 0x7FE8FF;

    private static final float BLOCK_ALPHA_MIN = 0.16F;
    private static final float BLOCK_ALPHA_MAX = 0.34F;

    private static final double SWELL = 0.02;

    private static final float PULSE_RATE = 2.0F;
    private static final float PULSE_SPREAD = 0.7F;

    private static final ResourceLocation WHITE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private record Seen(BlockPos pos, boolean objective) {
    }

    private static final List<Seen> BLOCKS = new ArrayList<>();
    private static final Set<UUID> CREATURES = new HashSet<>();
    private static final Set<UUID> HOSTILES = new HashSet<>();

    private static long until;
    private static int sinceScan;

    private static ClientLevel castIn;

    private Clairvoyance() {
    }

    public static void begin(int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        castIn = minecraft.level;
        until = minecraft.level.getGameTime() + ticks;

        sinceScan = 0;
    }

    public static boolean reveals(Entity entity) {
        return active() && CREATURES.contains(entity.getUUID());
    }

    public static int colour(Entity entity) {
        if (!active() || !CREATURES.contains(entity.getUUID())) {
            return -1;
        }
        return HOSTILES.contains(entity.getUUID()) ? HOSTILE_COLOUR : CREATURE_COLOUR;
    }

    public static void clear() {
        until = 0;
        castIn = null;
        BLOCKS.clear();
        CREATURES.clear();
        HOSTILES.clear();
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null || minecraft.player == null || !active()) {
            if (!BLOCKS.isEmpty() || !CREATURES.isEmpty()) {
                BLOCKS.clear();
                CREATURES.clear();
                HOSTILES.clear();
            }
            return;
        }

        if (--sinceScan > 0) {
            return;
        }
        sinceScan = SCAN_INTERVAL;

        scanBlocks(level, minecraft.player.position());
        scanCreatures(level, minecraft.player);
    }

    private static void scanBlocks(ClientLevel level, Vec3 eye) {
        BLOCKS.clear();

        BlockPos centre = BlockPos.containing(eye);
        int reach = (int) BLOCK_RANGE;
        double rangeSqr = BLOCK_RANGE * BLOCK_RANGE;

        int minY = Math.max(level.getMinBuildHeight(), centre.getY() - reach);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, centre.getY() + reach);

        int fromX = SectionPos.blockToSectionCoord(centre.getX() - reach);
        int toX = SectionPos.blockToSectionCoord(centre.getX() + reach);
        int fromZ = SectionPos.blockToSectionCoord(centre.getZ() - reach);
        int toZ = SectionPos.blockToSectionCoord(centre.getZ() + reach);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int chunkX = fromX; chunkX <= toX; chunkX++) {
            for (int chunkZ = fromZ; chunkZ <= toZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (chunk == null) {
                    continue;
                }

                LevelChunkSection[] sections = chunk.getSections();

                for (int index = level.getSectionIndex(minY);
                     index <= level.getSectionIndex(maxY); index++) {
                    if (index < 0 || index >= sections.length) {
                        continue;
                    }

                    LevelChunkSection section = sections[index];
                    if (section.hasOnlyAir()
                            || !section.maybeHas(Clairvoyance::interesting)) {
                        continue;
                    }

                    int baseY = SectionPos.sectionToBlockCoord(
                            level.getSectionYFromSectionIndex(index));
                    scanSection(section, chunk, baseY, minY, maxY, centre, reach, rangeSqr, eye,
                            cursor);
                }
            }
        }
    }

    private static void scanSection(LevelChunkSection section, LevelChunk chunk, int baseY,
                                    int minY, int maxY, BlockPos centre, int reach,
                                    double rangeSqr, Vec3 eye, BlockPos.MutableBlockPos cursor) {
        int chunkMinX = SectionPos.sectionToBlockCoord(chunk.getPos().x);
        int chunkMinZ = SectionPos.sectionToBlockCoord(chunk.getPos().z);

        int x0 = Math.max(0, centre.getX() - reach - chunkMinX);
        int x1 = Math.min(15, centre.getX() + reach - chunkMinX);
        int z0 = Math.max(0, centre.getZ() - reach - chunkMinZ);
        int z1 = Math.min(15, centre.getZ() + reach - chunkMinZ);
        int y0 = Math.max(0, minY - baseY);
        int y1 = Math.min(15, maxY - baseY);

        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    BlockState state = section.getBlockState(x, y, z);
                    boolean objective = state.is(OBJECTIVES);
                    if (!objective && !state.is(VALUABLES)) {
                        continue;
                    }

                    cursor.set(chunkMinX + x, baseY + y, chunkMinZ + z);
                    if (cursor.getCenter().distanceToSqr(eye) > rangeSqr) {
                        continue;
                    }

                    BLOCKS.add(new Seen(cursor.immutable(), objective));
                }
            }
        }
    }

    private static boolean interesting(BlockState state) {
        return state.is(VALUABLES) || state.is(OBJECTIVES);
    }

    private static void scanCreatures(ClientLevel level, Entity self) {
        CREATURES.clear();
        HOSTILES.clear();

        AABB box = self.getBoundingBox().inflate(ENTITY_RANGE);
        for (Entity entity : level.getEntities(self, box)) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            if (living.distanceToSqr(self) > ENTITY_RANGE * ENTITY_RANGE) {
                continue;
            }

            CREATURES.add(living.getUUID());

            if (living instanceof Enemy) {
                HOSTILES.add(living.getUUID());
            }
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                              float partialTick) {
        if (BLOCKS.isEmpty() || !active()) {
            return;
        }

        Vec3 cameraPos = camera.getPosition();
        float seconds = (System.currentTimeMillis() % 100000L) / 1000F;
        float fade = fade();

        VertexConsumer buffer = buffers.getBuffer(RenderType.textSeeThrough(WHITE));

        for (Seen seen : BLOCKS) {
            BlockPos pos = seen.pos();
            float phase = (pos.getX() * 0.7F + pos.getY() * 1.3F + pos.getZ() * 0.4F) * PULSE_SPREAD;
            float breath = (Mth.sin(seconds * PULSE_RATE + phase) + 1F) * 0.5F;

            AABB box = new AABB(pos).inflate(SWELL)
                    .move(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            GlowBox.fillLit(poseStack, buffer, box, seen.objective() ? OBJECTIVE_TINT : VALUABLE_TINT,
                    Mth.lerp(breath, BLOCK_ALPHA_MIN, BLOCK_ALPHA_MAX) * fade);
        }

        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
    }

    private static float fade() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0F;
        }

        long left = until - minecraft.level.getGameTime();
        return left >= 20L ? 1F : Math.max(0F, left / 20F);
    }

    private static boolean active() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null
                && minecraft.level == castIn
                && minecraft.level.getGameTime() < until;
    }

}
