package org.gumel.jojoha.stand.workshop;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.stand.StandTypes;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum BarrageShout {
    OWN("own", false),
    ORA("ora", false, StandTypes.STAR_PLATINUM_ID),
    MUDA("muda", false, StandTypes.THE_WORLD_ID),
    HORA("hora", false, StandTypes.SILVER_CHARIOT_ID),
    HIEROPHANT("hierophant", false, StandTypes.HIEROPHANT_GREEN_ID),
    MAGICIAN("magician", false, StandTypes.MAGICIANS_RED_ID),
    KICK("kick", true, StandTypes.THE_WORLD_ID),
    OVER_HEAVEN("over_heaven", false, StandTypes.SECRET_THE_WORLD_ID),
    DUEL("duel", false, StandTypes.STAR_PLATINUM_ID, StandTypes.THE_WORLD_ID),
    SILENT("silent", false);

    private final String key;
    private final boolean kick;
    private final List<ResourceLocation> voices;

    BarrageShout(String key, boolean kick, ResourceLocation... voices) {
        this.key = key;
        this.kick = kick;
        this.voices = List.of(voices);
    }

    public String key() {
        return key;
    }

    public boolean kick() {
        return kick;
    }

    public List<ResourceLocation> voices() {
        return voices;
    }

    @Nullable
    public ResourceLocation voice(int turn) {
        return voices.isEmpty() ? null : voices.get(Math.floorMod(turn, voices.size()));
    }

    public static BarrageShout of(int index) {
        BarrageShout[] all = values();
        return index >= 0 && index < all.length ? all[index] : OWN;
    }
}
