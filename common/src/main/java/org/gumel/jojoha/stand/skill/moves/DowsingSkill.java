package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class DowsingSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "dowsing");

    public static final DowsingSkill INSTANCE = new DowsingSkill();

    public static final int RANGE = 32;

    private static final int COOLDOWN_TICKS = 200;

    private static final int SHOW_TICKS = 160;

    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("c", "ores"));

    private DowsingSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.dowsing";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();
        Block wanted = wanted(player);
        BlockPos from = player.blockPosition();
        BlockPos found = nearest(level, from, wanted == null ? null : family(wanted));

        if (found == null) {
            player.displayClientMessage(wanted == null
                    ? Component.translatable("message.jojoha.dowsing.none", RANGE)
                    : Component.translatable("message.jojoha.dowsing.none_of", wanted.getName(), RANGE), true);
            level.playSound(null, from, SoundEvents.CAVE_VINES_BREAK, SoundSource.PLAYERS, 0.7F, 0.7F);
            return true;
        }

        NetworkHandler.sendDowsing(level, player, found, SHOW_TICKS);

        Component name = level.getBlockState(found).getBlock().getName();
        int away = (int) Math.round(Math.sqrt(Math.pow(found.getX() - from.getX(), 2)
                + Math.pow(found.getZ() - from.getZ(), 2)));
        int rise = found.getY() - from.getY();
        player.displayClientMessage(rise < 0
                ? Component.translatable("message.jojoha.dowsing.below", name, away, -rise)
                : rise > 0
                ? Component.translatable("message.jojoha.dowsing.above", name, away, rise)
                : Component.translatable("message.jojoha.dowsing.level", name, away), true);

        level.playSound(null, from, SoundEvents.CAVE_VINES_PLACE, SoundSource.PLAYERS, 1F, 0.7F);
        level.playSound(null, from, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 0.6F);
        return true;
    }

    private static Block wanted(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().is(ORES)) {
                return item.getBlock();
            }
        }
        return null;
    }

    private static String family(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath()
                .replace("deepslate_", "").replace("nether_", "");
    }

    private static BlockPos nearest(ServerLevel level, BlockPos centre, String family) {
        int minY = Math.max(level.getMinBuildHeight(), centre.getY() - RANGE);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, centre.getY() + RANGE);
        long rangeSqr = (long) RANGE * RANGE;

        BlockPos best = null;
        long bestSqr = Long.MAX_VALUE;

        for (int chunkX = SectionPos.blockToSectionCoord(centre.getX() - RANGE);
             chunkX <= SectionPos.blockToSectionCoord(centre.getX() + RANGE); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(centre.getZ() - RANGE);
                 chunkZ <= SectionPos.blockToSectionCoord(centre.getZ() + RANGE); chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                LevelChunkSection[] sections = chunk.getSections();
                int baseX = SectionPos.sectionToBlockCoord(chunkX);
                int baseZ = SectionPos.sectionToBlockCoord(chunkZ);

                for (int index = level.getSectionIndex(minY); index <= level.getSectionIndex(maxY); index++) {
                    if (index < 0 || index >= sections.length) {
                        continue;
                    }
                    LevelChunkSection section = sections[index];
                    if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(ORES))) {
                        continue;
                    }
                    int baseY = SectionPos.sectionToBlockCoord(level.getSectionYFromSectionIndex(index));

                    for (int y = 0; y < 16; y++) {
                        int worldY = baseY + y;
                        if (worldY < minY || worldY > maxY) {
                            continue;
                        }
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                BlockState state = section.getBlockState(x, y, z);
                                if (!state.is(ORES) || family != null && !family.equals(family(state.getBlock()))) {
                                    continue;
                                }
                                long dx = baseX + x - centre.getX();
                                long dy = worldY - centre.getY();
                                long dz = baseZ + z - centre.getZ();
                                long sqr = dx * dx + dy * dy + dz * dz;
                                if (sqr <= rangeSqr && sqr < bestSqr) {
                                    bestSqr = sqr;
                                    best = new BlockPos(baseX + x, worldY, baseZ + z);
                                }
                            }
                        }
                    }
                }
            }
        }
        return best;
    }
}
