package org.gumel.jojoha.stand;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.gumel.jojoha.data.StandData;
import org.gumel.jojoha.registry.ModSounds;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public final class StandVoices {

    private record Voice(@Nullable Supplier<SoundEvent> barrageCry,
                         @Nullable Supplier<SoundEvent> timeStopCall,
                         @Nullable Supplier<SoundEvent> summon,
                         @Nullable Supplier<SoundEvent> dismiss,
                         @Nullable Supplier<SoundEvent> heavy,
                         @Nullable Supplier<SoundEvent> heavyHit,
                         @Nullable Supplier<SoundEvent> kickCry) {
    }

    private static final Map<ResourceLocation, Voice> VOICES = Map.of(
            StandTypes.STAR_PLATINUM_ID, new Voice(ModSounds.SP_BARRAGE, ModSounds.SP_TIMESTOP,
                    ModSounds.SP_SUMMON, null, ModSounds.SP_STAR_BREAKER, null, null),
            StandTypes.THE_WORLD_ID, new Voice(ModSounds.TW_BARRAGE, ModSounds.TW_TIME_STOP_CALL,
                    ModSounds.TW_SUMMON, ModSounds.TW_DESUMMON, ModSounds.TW_CHARGE, ModSounds.TW_CHARGE_HIT,
                    ModSounds.TW_KICK),
            StandTypes.SECRET_THE_WORLD_ID, new Voice(ModSounds.SECRET_TW_BARRAGE, ModSounds.SECRET_TW_TIME_STOP,
                    ModSounds.SECRET_TW_SUMMON, ModSounds.TW_DESUMMON, ModSounds.TW_CHARGE, ModSounds.TW_CHARGE_HIT,
                    ModSounds.TW_KICK),
            StandTypes.HIEROPHANT_GREEN_ID, new Voice(ModSounds.HG_BARRAGE, null,
                    ModSounds.HG_SUMMON, null, null, null, null),
            StandTypes.SILVER_CHARIOT_ID, new Voice(ModSounds.SC_BARRAGE, null,
                    ModSounds.SC_SUMMON, null, ModSounds.SC_HEAVY, null, null),
            StandTypes.MAGICIANS_RED_ID, new Voice(ModSounds.MR_BARRAGE, null,
                    ModSounds.MR_SUMMON, null, ModSounds.MR_HEAVY, null, null));

    private static final Map<ResourceLocation, Voice> SHADES = Map.of(
            StandTypes.THE_WORLD_ID, new Voice(null, ModSounds.TW_SHADOW_TIME_STOP,
                    ModSounds.TW_SHADOW_LAUGH, null, ModSounds.TW_SHADOW_WARBLE, ModSounds.TW_SHADOW_ZAP, null));

    private static final Map<ResourceLocation, Supplier<SoundEvent>> RESUMES = Map.of(
            StandTypes.SECRET_THE_WORLD_ID, ModSounds.SECRET_TW_TIME_RESUME);

    private static final Map<ResourceLocation, Supplier<SoundEvent>> SUMMON_CALLS = Map.of(
            StandTypes.THE_WORLD_ID, ModSounds.TW_SUMMON_CALL);

    private StandVoices() {
    }

    @Nullable
    public static SoundEvent summonCall(@Nullable StandType type, boolean shadow) {
        return shadow || type == null ? null : pick(SUMMON_CALLS.get(type.id()));
    }

    public static SoundEvent timeResume(@Nullable StandType type) {
        Supplier<SoundEvent> own = type == null ? null : RESUMES.get(type.id());
        return own != null ? own.get() : ModSounds.TIME_RESUME.get();
    }

    @Nullable
    private static Voice of(@Nullable StandType type) {
        return type == null ? null : VOICES.get(type.id());
    }

    @Nullable
    private static Voice shade(@Nullable StandType type, boolean shadow) {
        return shadow && type != null ? SHADES.get(type.id()) : null;
    }

    public static boolean shadowed(StandData stand) {
        return StandShadowForm.of(stand.standId(), stand.skin(), stand.trust()) != null;
    }

    @Nullable
    private static SoundEvent pick(@Nullable Supplier<SoundEvent> sound) {
        return sound == null ? null : sound.get();
    }

    @Nullable
    public static SoundEvent barrageCry(@Nullable StandType type) {
        Voice voice = of(type);
        return voice == null ? null : pick(voice.barrageCry());
    }

    @Nullable
    public static SoundEvent kickCry(@Nullable StandType type) {
        Voice voice = of(type);
        SoundEvent kick = voice == null ? null : pick(voice.kickCry());
        return kick != null ? kick : barrageCry(type);
    }

    public static SoundEvent timeStopCast(@Nullable StandType type, boolean shadow) {
        Voice shade = shade(type, shadow);
        SoundEvent own = shade == null ? null : pick(shade.timeStopCall());
        return own != null ? own : timeStopCast(type);
    }

    public static SoundEvent timeStopCast(@Nullable StandType type) {
        Voice voice = of(type);
        SoundEvent call = voice == null ? null : pick(voice.timeStopCall());
        return call != null ? call : ModSounds.TIME_STOP_CAST.get();
    }

    public static SoundEvent summon(@Nullable StandType type, boolean shadow) {
        Voice shade = shade(type, shadow);
        SoundEvent own = shade == null ? null : pick(shade.summon());
        return own != null ? own : summon(type);
    }

    public static SoundEvent summon(@Nullable StandType type) {
        Voice voice = of(type);
        SoundEvent summon = voice == null ? null : pick(voice.summon());
        return summon != null ? summon : ModSounds.STAND_SUMMON.get();
    }

    public static SoundEvent dismiss(@Nullable StandType type) {
        Voice voice = of(type);
        SoundEvent dismiss = voice == null ? null : pick(voice.dismiss());
        return dismiss != null ? dismiss : ModSounds.STAND_DISMISS.get();
    }

    @Nullable
    public static SoundEvent heavy(@Nullable StandType type, boolean shadow) {
        Voice shade = shade(type, shadow);
        SoundEvent own = shade == null ? null : pick(shade.heavy());
        return own != null ? own : heavy(type);
    }

    @Nullable
    public static SoundEvent heavy(@Nullable StandType type) {
        Voice voice = of(type);
        return voice == null ? null : pick(voice.heavy());
    }

    @Nullable
    public static SoundEvent heavyHit(@Nullable StandType type, boolean shadow) {
        Voice shade = shade(type, shadow);
        SoundEvent own = shade == null ? null : pick(shade.heavyHit());
        return own != null ? own : heavyHit(type);
    }

    @Nullable
    public static SoundEvent heavyHit(@Nullable StandType type) {
        Voice voice = of(type);
        return voice == null ? null : pick(voice.heavyHit());
    }
}
