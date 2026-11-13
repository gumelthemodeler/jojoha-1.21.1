package org.gumel.jojoha.stand;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public final class StandSpread extends SavedData {
    private static final String NAME = "jojoha_stand_spread";

    private static final Factory<StandSpread> FACTORY = new Factory<>(StandSpread::new, StandSpread::load, null);

    private final Map<ResourceLocation, Integer> held = new HashMap<>();

    public static StandSpread of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static StandSpread load(CompoundTag tag, HolderLookup.Provider registries) {
        StandSpread spread = new StandSpread();
        for (String key : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                spread.held.put(id, Math.max(0, tag.getInt(key)));
            }
        }
        return spread;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        held.forEach((id, count) -> tag.putInt(id.toString(), count));
        return tag;
    }

    public int held(ResourceLocation standId) {
        return held.getOrDefault(standId, 0);
    }

    public void gained(ResourceLocation standId) {
        held.merge(standId, 1, Integer::sum);
        setDirty();
    }

    public void lost(ResourceLocation standId) {
        held.computeIfPresent(standId, (id, count) -> count <= 1 ? null : count - 1);
        setDirty();
    }
}
