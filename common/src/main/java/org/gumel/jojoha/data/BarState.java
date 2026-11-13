package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public record BarState(List<String> playerEquipped, boolean halfSummon,
                       Map<ResourceLocation, List<Integer>> committedSkins,
                       DreamMemory dream, TotemBinding totem, boolean anubis,
                       SpiritData spirit, FortuneData fortune, HeavenData heaven,
                       StandPlacement placement, boolean timeSkipDash, UltimateData ultimate,
                       AnubisHold anubisHold, WorkshopData workshop) {

    public static final BarState DEFAULT =
            new BarState(List.of(), false, Map.of(), DreamMemory.NONE, TotemBinding.NONE, false,
                    SpiritData.NONE, FortuneData.NONE, HeavenData.NONE, StandPlacement.NONE, false, UltimateData.NONE,
                    AnubisHold.NONE, WorkshopData.NONE);

    public BarState(List<String> playerEquipped, boolean halfSummon) {
        this(playerEquipped, halfSummon, Map.of(), DreamMemory.NONE, TotemBinding.NONE, false, SpiritData.NONE,
                FortuneData.NONE, HeavenData.NONE, StandPlacement.NONE, false, UltimateData.NONE, AnubisHold.NONE,
                WorkshopData.NONE);
    }

    public static final Codec<BarState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("player_equipped", List.of())
                    .forGetter(BarState::playerEquipped),
            Codec.BOOL.optionalFieldOf("half_summon", false).forGetter(BarState::halfSummon),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT.listOf())
                    .optionalFieldOf("committed_skins", Map.of())
                    .forGetter(BarState::committedSkins),
            DreamMemory.CODEC.optionalFieldOf("dream", DreamMemory.NONE).forGetter(BarState::dream),
            TotemBinding.CODEC.optionalFieldOf("totem", TotemBinding.NONE).forGetter(BarState::totem),
            Codec.BOOL.optionalFieldOf("anubis", false).forGetter(BarState::anubis),
            SpiritData.CODEC.optionalFieldOf("spirit", SpiritData.NONE).forGetter(BarState::spirit),
            FortuneData.CODEC.optionalFieldOf("fortune", FortuneData.NONE).forGetter(BarState::fortune),
            HeavenData.CODEC.optionalFieldOf("heaven", HeavenData.NONE).forGetter(BarState::heaven),
            StandPlacement.CODEC.optionalFieldOf("placement", StandPlacement.NONE).forGetter(BarState::placement),
            Codec.BOOL.optionalFieldOf("time_skip_dash", false).forGetter(BarState::timeSkipDash),
            UltimateData.CODEC.optionalFieldOf("ultimate", UltimateData.NONE).forGetter(BarState::ultimate),
            AnubisHold.CODEC.optionalFieldOf("anubis_hold", AnubisHold.NONE).forGetter(BarState::anubisHold),
            WorkshopData.CODEC.optionalFieldOf("workshop", WorkshopData.NONE).forGetter(BarState::workshop)
    ).apply(instance, BarState::new));

}
