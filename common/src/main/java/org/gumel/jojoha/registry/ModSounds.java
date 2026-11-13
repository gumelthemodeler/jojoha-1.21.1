package org.gumel.jojoha.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.gumel.jojoha.Jojoha;

import static org.gumel.jojoha.registry.ModRegistries.SOUND_EVENTS;

public final class ModSounds {
    public static final RegistrySupplier<SoundEvent> STAND_SUMMON = SOUND_EVENTS.register("stand_summon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_summon")));

    public static final RegistrySupplier<SoundEvent> STAND_DISMISS = SOUND_EVENTS.register("stand_dismiss",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_dismiss")));

    public static final RegistrySupplier<SoundEvent> STAND_AWAKEN = SOUND_EVENTS.register("stand_awaken",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_awaken")));

    public static final RegistrySupplier<SoundEvent> MUSIC_DISC_AWAKEN = SOUND_EVENTS.register("music_disc.awaken",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "music_disc.awaken")));

    public static final RegistrySupplier<SoundEvent> MUSIC_DISC_BLOODY_STREAM = SOUND_EVENTS.register("music_disc.bloody_stream",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "music_disc.bloody_stream")));

    public static final RegistrySupplier<SoundEvent> MUSIC_DISC_LAST_TRAIN_HOME = SOUND_EVENTS.register("music_disc.last_train_home",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "music_disc.last_train_home")));

    public static final RegistrySupplier<SoundEvent> MUSIC_DISC_OVERDRIVE = SOUND_EVENTS.register("music_disc.overdrive",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "music_disc.overdrive")));

    public static final RegistrySupplier<SoundEvent> MUSIC_DISC_PERSISTENCE_INNOCENT_SCREAM = SOUND_EVENTS.register("music_disc.persistence_innocent_scream",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "music_disc.persistence_innocent_scream")));

    public static final RegistrySupplier<SoundEvent> MUSIC_DISC_ROUNDABOUT = SOUND_EVENTS.register("music_disc.roundabout",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "music_disc.roundabout")));

    public static final RegistrySupplier<SoundEvent> MUSIC_DISC_SONO_CHI_NO_KIOKU = SOUND_EVENTS.register("music_disc.sono_chi_no_kioku",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "music_disc.sono_chi_no_kioku")));

    public static final RegistrySupplier<SoundEvent> MUSIC_DISC_SONO_CHI_NO_SADAME = SOUND_EVENTS.register("music_disc.sono_chi_no_sadame",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "music_disc.sono_chi_no_sadame")));

    public static final RegistrySupplier<SoundEvent> MUSIC_DISC_STAND_PROUD = SOUND_EVENTS.register("music_disc.stand_proud",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "music_disc.stand_proud")));

    public static final RegistrySupplier<SoundEvent> MUSIC_DISC_STARDUST_CRUSADERS = SOUND_EVENTS.register("music_disc.stardust_crusaders",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "music_disc.stardust_crusaders")));

    public static final RegistrySupplier<SoundEvent> MUSIC_DISC_WALK_LIKE_AN_EGYPTIAN = SOUND_EVENTS.register("music_disc.walk_like_an_egyptian",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "music_disc.walk_like_an_egyptian")));

    public static final RegistrySupplier<SoundEvent> AWAKEN_STAND_SOUND = SOUND_EVENTS.register("awaken_stand_sound",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "awaken_stand_sound")));

    public static final RegistrySupplier<SoundEvent> STAND_ARROW = SOUND_EVENTS.register("stand_arrow",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_arrow")));

    public static final RegistrySupplier<SoundEvent> SP_STAR_PLATINUM = SOUND_EVENTS.register("sp_starplatinum",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sp_starplatinum")));

    public static final RegistrySupplier<SoundEvent> HG_HIEROPHANT_GREEN = SOUND_EVENTS.register("hg_hierophantgreen",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hg_hierophantgreen")));

    public static final RegistrySupplier<SoundEvent> SC_SILVER_CHARIOT = SOUND_EVENTS.register("sc_silverchariot",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sc_silverchariot")));

    public static final RegistrySupplier<SoundEvent> HG_EMERALD_START = SOUND_EVENTS.register("hg_emeraldstart",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hg_emeraldstart")));

    public static final RegistrySupplier<SoundEvent> HG_EMERALD_CONTINUE = SOUND_EVENTS.register("hg_emeraldcontinue",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hg_emeraldcontinue")));

    public static final RegistrySupplier<SoundEvent> HG_EMERALD_SPLASH = SOUND_EVENTS.register("hg_emeraldsplash",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hg_emeraldsplash")));

    public static final RegistrySupplier<SoundEvent> HG_YELL = SOUND_EVENTS.register("hg_yell",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hg_yell")));

    public static final RegistrySupplier<SoundEvent> SP_TIMESTOP = SOUND_EVENTS.register("sp_timestop",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sp_timestop")));

    public static final RegistrySupplier<SoundEvent> TW_TIMESTOP = SOUND_EVENTS.register("tw_timestop",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_timestop")));

    public static final RegistrySupplier<SoundEvent> TIME_STOP_CAST = SOUND_EVENTS.register("time_stop_cast",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "time_stop_cast")));

    public static final RegistrySupplier<SoundEvent> TIME_RESUME = SOUND_EVENTS.register("timeresume",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "timeresume")));

    public static final RegistrySupplier<SoundEvent> TIMESTOP_BEGINNING = SOUND_EVENTS.register("timestop_beginning",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "timestop_beginning")));

    public static final RegistrySupplier<SoundEvent> HAMON = SOUND_EVENTS.register("hamon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hamon")));

    public static final RegistrySupplier<SoundEvent> HAMON_DISCHARGE = SOUND_EVENTS.register("hamon_discharge",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hamon_discharge")));
    public static final RegistrySupplier<SoundEvent> HAMON_STRIKE = SOUND_EVENTS.register("hamon_strike",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hamon_strike")));

    public static final RegistrySupplier<SoundEvent> TIME_SKIP = SOUND_EVENTS.register("time_skip",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "time_skip")));

    public static final RegistrySupplier<SoundEvent> TW_BARRAGE = SOUND_EVENTS.register("tw_barrage",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_barrage")));

    public static final RegistrySupplier<SoundEvent> SP_BARRAGE = SOUND_EVENTS.register("sp_barrage",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sp_barrage")));

    public static final RegistrySupplier<SoundEvent> STAND_HIT = SOUND_EVENTS.register("stand_hit",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_hit")));

    public static final RegistrySupplier<SoundEvent> LIMB_SEVERED = SOUND_EVENTS.register("limb_severed",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "limb_severed")));

    public static final RegistrySupplier<SoundEvent> LIMB_REGEN = SOUND_EVENTS.register("limb_regen",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "limb_regen")));

    public static final RegistrySupplier<SoundEvent> HIT_TAP = SOUND_EVENTS.register("hit_tap",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hit_tap")));

    public static final RegistrySupplier<SoundEvent> IMPACT_PUNCH = SOUND_EVENTS.register("impact_punch",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "impact_punch")));

    public static final RegistrySupplier<SoundEvent> MOVE_CAST = SOUND_EVENTS.register("move_cast",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "move_cast")));

    public static final RegistrySupplier<SoundEvent> STAND_JUMP = SOUND_EVENTS.register("stand_jump",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_jump")));

    public static final RegistrySupplier<SoundEvent> PARRY = SOUND_EVENTS.register("parry",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "parry")));

    public static final RegistrySupplier<SoundEvent> HEAVY_PARRY = SOUND_EVENTS.register("heavy_parry",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "heavy_parry")));

    public static final RegistrySupplier<SoundEvent> GUARD_BREAK = SOUND_EVENTS.register("guard_break",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "guard_break")));

    public static final RegistrySupplier<SoundEvent> STING_DEFEAT = SOUND_EVENTS.register("sting.defeat",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sting.defeat")));

    public static final RegistrySupplier<SoundEvent> CLOCK_TICK = SOUND_EVENTS.register("clock_tick",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "clock_tick")));

    public static final RegistrySupplier<SoundEvent> CLOCK_SINGLE = SOUND_EVENTS.register("clock_single",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "clock_single")));

    public static final RegistrySupplier<SoundEvent> PHANTOM_WIND = SOUND_EVENTS.register("phantom_wind",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "phantom_wind")));

    public static final RegistrySupplier<SoundEvent> PHANTOM_AMBIENCE = SOUND_EVENTS.register("phantom_ambience",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "phantom_ambience")));

    public static final RegistrySupplier<SoundEvent> STONEMASK_STAB = SOUND_EVENTS.register("stonemask_stab",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stonemask_stab")));

    public static final RegistrySupplier<SoundEvent> SP_SUMMON = SOUND_EVENTS.register("sp_summon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sp_summon")));

    public static final RegistrySupplier<SoundEvent> SP_INHALE = SOUND_EVENTS.register("sp_inhale",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sp_inhale")));

    public static final RegistrySupplier<SoundEvent> SP_STAR_FINGER = SOUND_EVENTS.register("sp_star_finger",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sp_star_finger")));

    public static final RegistrySupplier<SoundEvent> SP_STAR_BREAKER = SOUND_EVENTS.register("sp_star_breaker",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sp_star_breaker")));

    public static final RegistrySupplier<SoundEvent> TW_SUMMON = SOUND_EVENTS.register("tw_summon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_summon")));

    public static final RegistrySupplier<SoundEvent> TW_SUMMON_CALL = SOUND_EVENTS.register("tw_summon_call",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_summon_call")));

    public static final RegistrySupplier<SoundEvent> TW_TIME_STOP_IMPACT = SOUND_EVENTS.register("tw_time_stop_impact",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_time_stop_impact")));

    public static final RegistrySupplier<SoundEvent> TW_DESUMMON = SOUND_EVENTS.register("tw_desummon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_desummon")));

    public static final RegistrySupplier<SoundEvent> TW_TIME_STOP_CALL = SOUND_EVENTS.register("tw_time_stop_call",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_time_stop_call")));

    public static final RegistrySupplier<SoundEvent> SECRET_TW_BARRAGE = SOUND_EVENTS.register("secret_tw_barrage",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "secret_tw_barrage")));

    public static final RegistrySupplier<SoundEvent> SECRET_TW_SUMMON = SOUND_EVENTS.register("secret_tw_summon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "secret_tw_summon")));

    public static final RegistrySupplier<SoundEvent> SECRET_TW_TIME_STOP = SOUND_EVENTS.register("secret_tw_time_stop",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "secret_tw_time_stop")));

    public static final RegistrySupplier<SoundEvent> SECRET_TW_TIME_RESUME = SOUND_EVENTS.register("secret_tw_time_resume",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "secret_tw_time_resume")));

    public static final RegistrySupplier<SoundEvent> TW_KICK = SOUND_EVENTS.register("tw_kick",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_kick")));

    public static final RegistrySupplier<SoundEvent> TW_CHARGE = SOUND_EVENTS.register("tw_charge",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_charge")));

    public static final RegistrySupplier<SoundEvent> TW_CHARGE_HIT = SOUND_EVENTS.register("tw_charge_hit",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_charge_hit")));

    public static final RegistrySupplier<SoundEvent> TW_MUDADA = SOUND_EVENTS.register("tw_mudada",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_mudada")));

    public static final RegistrySupplier<SoundEvent> TW_DONUT_HIT = SOUND_EVENTS.register("tw_donut_hit",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_donut_hit")));

    public static final RegistrySupplier<SoundEvent> TW_SHADOW_WARBLE = SOUND_EVENTS.register("tw_shadow_warble",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_shadow_warble")));

    public static final RegistrySupplier<SoundEvent> TW_SHADOW_TIME_STOP = SOUND_EVENTS.register("tw_shadow_time_stop",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_shadow_time_stop")));

    public static final RegistrySupplier<SoundEvent> TW_SHADOW_LAUGH = SOUND_EVENTS.register("tw_shadow_laugh",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_shadow_laugh")));

    public static final RegistrySupplier<SoundEvent> TW_SHADOW_ZAP = SOUND_EVENTS.register("tw_shadow_zap",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tw_shadow_zap")));

    public static final RegistrySupplier<SoundEvent> HG_SUMMON = SOUND_EVENTS.register("hg_summon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hg_summon")));

    public static final RegistrySupplier<SoundEvent> HG_BARRAGE = SOUND_EVENTS.register("hg_barrage",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hg_barrage")));

    public static final RegistrySupplier<SoundEvent> HG_EXTEND = SOUND_EVENTS.register("hg_extend",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hg_extend")));

    public static final RegistrySupplier<SoundEvent> HG_NET_SET = SOUND_EVENTS.register("hg_net_set",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hg_net_set")));

    public static final RegistrySupplier<SoundEvent> HG_SPLASH = SOUND_EVENTS.register("hg_splash",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hg_splash")));

    public static final RegistrySupplier<SoundEvent> SC_SUMMON = SOUND_EVENTS.register("sc_summon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sc_summon")));

    public static final RegistrySupplier<SoundEvent> MR_SUMMON = SOUND_EVENTS.register("mr_summon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "mr_summon")));

    public static final RegistrySupplier<SoundEvent> MR_BARRAGE = SOUND_EVENTS.register("mr_barrage",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "mr_barrage")));

    public static final RegistrySupplier<SoundEvent> MR_HEAVY = SOUND_EVENTS.register("mr_heavy",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "mr_heavy")));

    public static final RegistrySupplier<SoundEvent> MR_CROSSFIRE = SOUND_EVENTS.register("mr_crossfire",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "mr_crossfire")));

    public static final RegistrySupplier<SoundEvent> MR_RED_BIND = SOUND_EVENTS.register("mr_red_bind",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "mr_red_bind")));

    public static final RegistrySupplier<SoundEvent> MR_DETECTOR = SOUND_EVENTS.register("mr_detector",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "mr_detector")));

    public static final RegistrySupplier<SoundEvent> MR_REDIRECT = SOUND_EVENTS.register("mr_redirect",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "mr_redirect")));

    public static final RegistrySupplier<SoundEvent> MR_ULT = SOUND_EVENTS.register("mr_ult",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "mr_ult")));

    public static final RegistrySupplier<SoundEvent> SC_ARMOR_OFF = SOUND_EVENTS.register("sc_armor_off",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sc_armor_off")));

    public static final RegistrySupplier<SoundEvent> SC_BARRAGE = SOUND_EVENTS.register("sc_barrage",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sc_barrage")));

    public static final RegistrySupplier<SoundEvent> SC_CHARGE = SOUND_EVENTS.register("sc_charge",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sc_charge")));

    public static final RegistrySupplier<SoundEvent> SC_CLEAVE = SOUND_EVENTS.register("sc_cleave",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sc_cleave")));

    public static final RegistrySupplier<SoundEvent> SC_HEAVY = SOUND_EVENTS.register("sc_heavy",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sc_heavy")));

    public static final RegistrySupplier<SoundEvent> SC_POKE = SOUND_EVENTS.register("sc_poke",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sc_poke")));

    public static final RegistrySupplier<SoundEvent> SC_SPIN = SOUND_EVENTS.register("sc_spin",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sc_spin")));

    public static final RegistrySupplier<SoundEvent> SC_THRUST = SOUND_EVENTS.register("sc_thrust",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sc_thrust")));

    public static final RegistrySupplier<SoundEvent> HAMON_BREATHE = SOUND_EVENTS.register("hamon_breathe",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hamon_breathe")));

    public static final RegistrySupplier<SoundEvent> HAMON_CRASH = SOUND_EVENTS.register("hamon_crash",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hamon_crash")));

    public static final RegistrySupplier<SoundEvent> HAMON_RING = SOUND_EVENTS.register("hamon_ring",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hamon_ring")));

    public static final RegistrySupplier<SoundEvent> HAMON_ECHO = SOUND_EVENTS.register("hamon_echo",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hamon_echo")));

    public static final RegistrySupplier<SoundEvent> HAMON_EXHALE = SOUND_EVENTS.register("hamon_exhale",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hamon_exhale")));

    public static final RegistrySupplier<SoundEvent> HAMON_SWOOSH = SOUND_EVENTS.register("hamon_swoosh",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hamon_swoosh")));

    public static final RegistrySupplier<SoundEvent> VAMPIRE_SUCK = SOUND_EVENTS.register("vampire_suck",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "vampire_suck")));

    public static final RegistrySupplier<SoundEvent> VAMPIRE_LASER = SOUND_EVENTS.register("vampire_laser",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "vampire_laser")));

    public static final RegistrySupplier<SoundEvent> VAMPIRE_LASER_FIRE = SOUND_EVENTS.register("vampire_laser_fire",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "vampire_laser_fire")));

    public static final RegistrySupplier<SoundEvent> VAMPIRE_REANIMATE = SOUND_EVENTS.register("vampire_reanimate",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "vampire_reanimate")));

    public static final RegistrySupplier<SoundEvent> VAMPIRE_SPEC_CHANGE = SOUND_EVENTS.register("vampire_spec_change",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "vampire_spec_change")));

    public static final RegistrySupplier<SoundEvent> ANUBIS_UNSHEATHE = SOUND_EVENTS.register("anubis_unsheathe",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "anubis_unsheathe")));

    public static final RegistrySupplier<SoundEvent> ANUBIS_SHEATHE = SOUND_EVENTS.register("anubis_sheathe",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "anubis_sheathe")));

    public static final RegistrySupplier<SoundEvent> ANUBIS_SLASH = SOUND_EVENTS.register("anubis_slash",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "anubis_slash")));

    public static final RegistrySupplier<SoundEvent> ANUBIS_REKKA = SOUND_EVENTS.register("anubis_rekka",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "anubis_rekka")));

    public static final RegistrySupplier<SoundEvent> ANUBIS_POMMEL = SOUND_EVENTS.register("anubis_pommel",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "anubis_pommel")));

    public static final RegistrySupplier<SoundEvent> ANUBIS_SPEC_CHANGE = SOUND_EVENTS.register("anubis_spec_change",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "anubis_spec_change")));

    public static final RegistrySupplier<SoundEvent> NPC_VOICE_MASCULINE = SOUND_EVENTS.register("npc_voice_masculine",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "npc_voice_masculine")));

    public static final RegistrySupplier<SoundEvent> NPC_VOICE_FEMININE = SOUND_EVENTS.register("npc_voice_feminine",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "npc_voice_feminine")));

    public static final RegistrySupplier<SoundEvent> NPC_VOICE_JOTARO = SOUND_EVENTS.register("npc_voice_jotaro",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "npc_voice_jotaro")));

    public static final RegistrySupplier<SoundEvent> NPC_VOICE_JOSEPH = SOUND_EVENTS.register("npc_voice_joseph",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "npc_voice_joseph")));

    public static final RegistrySupplier<SoundEvent> NPC_VOICE_AVDOL = SOUND_EVENTS.register("npc_voice_avdol",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "npc_voice_avdol")));

    public static final RegistrySupplier<SoundEvent> NPC_VOICE_KAKYOIN = SOUND_EVENTS.register("npc_voice_kakyoin",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "npc_voice_kakyoin")));

    public static final RegistrySupplier<SoundEvent> NPC_VOICE_POLNAREFF = SOUND_EVENTS.register("npc_voice_polnareff",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "npc_voice_polnareff")));

    public static final RegistrySupplier<SoundEvent> NPC_VOICE_IGGY = SOUND_EVENTS.register("npc_voice_iggy",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "npc_voice_iggy")));

    public static final RegistrySupplier<SoundEvent> NPC_VOICE_DIO = SOUND_EVENTS.register("npc_voice_dio",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "npc_voice_dio")));

    private ModSounds() {
    }

    public static void bootstrap() {
    }
}
