package org.gumel.jojoha.fortune;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.level.StructureSeek;

final class Pulls {
    static final TagKey<Structure> FATED = TagKey.create(Registries.STRUCTURE, Fortunes.id("fated"));

    static final int STRUCTURE_CHUNKS = 32;

    static final int BIOME_RADIUS = 256;

    static final int BIOME_STEP = 32;

    static final int BIOME_HEIGHT_STEP = 64;

    static final double TOO_CLOSE = 48.0;

    private Pulls() {
    }

    static boolean structure(ServerPlayer player, String key) {
        BlockPos found = StructureSeek.nearest(player.serverLevel(), FATED, player.blockPosition(), STRUCTURE_CHUNKS);
        return tell(player, found, key);
    }

    static boolean biome(ServerPlayer player, String key) {
        ServerLevel level = player.serverLevel();
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(Pulls::ours, player.blockPosition(),
                BIOME_RADIUS, BIOME_STEP, BIOME_HEIGHT_STEP);
        return tell(player, found == null ? null : found.getFirst(), key);
    }

    private static boolean ours(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> key.location().getNamespace().equals(Jojoha.MOD_ID)).orElse(false);
    }

    private static boolean tell(ServerPlayer player, BlockPos found, String key) {
        if (found == null) {
            return false;
        }
        Vec3 at = Vec3.atCenterOf(found);
        Vec3 from = player.position();
        if (new Vec3(at.x - from.x, 0, at.z - from.z).length() < TOO_CLOSE) {
            return false;
        }
        Fortunes.whisper(player, Component.translatable(key, Fortunes.direction(from, at)));
        return true;
    }
}
