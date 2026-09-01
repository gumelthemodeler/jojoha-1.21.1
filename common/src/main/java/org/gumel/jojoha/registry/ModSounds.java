package org.gumel.jojoha.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.gumel.jojoha.Jojoha;

import static org.gumel.jojoha.registry.ModRegistries.SOUND_EVENTS;

public final class ModSounds {
    public static final RegistrySupplier<SoundEvent> STAND_SUMMON = SOUND_EVENTS.register("stand_summon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_summon")));

    /**
     * Withdrawal cue. Registered as its own event even though {@code sounds.json} currently
     * points it at the summon clip (played pitched down) - no dedicated recording exists yet, and
     * having the event separate means dropping in a real {@code stand_dismiss.ogg} is a one-line
     * resource change rather than a code change.
     */
    public static final RegistrySupplier<SoundEvent> STAND_DISMISS = SOUND_EVENTS.register("stand_dismiss",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_dismiss")));

    /** Plays under the Stand Arrow's eruption, while the body is glowing - see StandArrowRitual. */
    public static final RegistrySupplier<SoundEvent> STAND_AWAKEN = SOUND_EVENTS.register("stand_awaken",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_awaken")));

    /**
     * The arrow's own voice, laid under the stab. Started and faded by the client rather than
     * fired with {@code playSound}, because the engine has no volume envelope - see
     * {@code FadingRitualSound}.
     */
    public static final RegistrySupplier<SoundEvent> STAND_ARROW = SOUND_EVENTS.register("stand_arrow",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_arrow")));

    /** The Stand naming itself, played as it is cast. */
    public static final RegistrySupplier<SoundEvent> SP_STAR_PLATINUM = SOUND_EVENTS.register("sp_starplatinum",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sp_starplatinum")));

    /** The call that starts a time stop - it runs under the whole wind-up. */
    public static final RegistrySupplier<SoundEvent> SP_TIMESTOP = SOUND_EVENTS.register("sp_timestop",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sp_timestop")));

    /** Time starting again. */
    public static final RegistrySupplier<SoundEvent> TIME_RESUME = SOUND_EVENTS.register("timeresume",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "timeresume")));

    /** The blink Stand Dash becomes once stopped time is understood. */
    public static final RegistrySupplier<SoundEvent> TIME_SKIP = SOUND_EVENTS.register("time_skip",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "time_skip")));

    /** The cry that carries a flurry. */
    public static final RegistrySupplier<SoundEvent> SP_BARRAGE = SOUND_EVENTS.register("sp_barrage",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sp_barrage")));

    /** One blow landing. */
    public static final RegistrySupplier<SoundEvent> STAND_HIT = SOUND_EVENTS.register("stand_hit",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_hit")));

    /** The Stand throwing its user, or itself, somewhere. */
    public static final RegistrySupplier<SoundEvent> STAND_JUMP = SOUND_EVENTS.register("stand_jump",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_jump")));

    /** A block being broken through. */
    public static final RegistrySupplier<SoundEvent> GUARD_BREAK = SOUND_EVENTS.register("guard_break",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "guard_break")));

    /** The ticking heard while time is held - a continuous track, not a single tick. */
    public static final RegistrySupplier<SoundEvent> CLOCK_TICK = SOUND_EVENTS.register("clock_tick",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "clock_tick")));

    /**
     * One tick of the clock, cut out of the track above.
     *
     * <p>The track runs at a fixed one beat a second and cannot be made to slow down: Minecraft's
     * pitch control doubles as playback rate but is clamped at half speed, which is nowhere near a
     * stop, and it drags the pitch down with it. Playing a single beat on an interval this side
     * decides is the only way to actually wind the clock down and start it again.
     */
    public static final RegistrySupplier<SoundEvent> CLOCK_SINGLE = SOUND_EVENTS.register("clock_single",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "clock_single")));

    /**
     * The Highlands' constant bed of wind.
     *
     * <p>Named as the biome's rather than as "wind" because it is the biome's: it is set as the
     * biome's ambient_sound, which the game loops for as long as a player stands in it.
     */
    public static final RegistrySupplier<SoundEvent> PHANTOM_WIND = SOUND_EVENTS.register("phantom_wind",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "phantom_wind")));

    /**
     * The occasional noises over that bed - a howl, or something breathing out.
     *
     * <p>One event holding both clips rather than two events. A biome may name exactly one
     * additions_sound, and a sound event may list any number of clips and pick between them at
     * random, so the variety belongs in {@code sounds.json} rather than in a second field the biome
     * format does not have.
     */
    public static final RegistrySupplier<SoundEvent> PHANTOM_AMBIENCE = SOUND_EVENTS.register("phantom_ambience",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "phantom_ambience")));

    /** The spines going in, when the mask seats itself on a face. */
    public static final RegistrySupplier<SoundEvent> STONEMASK_STAB = SOUND_EVENTS.register("stonemask_stab",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stonemask_stab")));

    private ModSounds() {
    }

    /** No-op call site to force this class's static initializers to run before {@link ModRegistries#SOUND_EVENTS} registers. */
    public static void bootstrap() {
    }
}
