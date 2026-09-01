package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.gumel.jojoha.stand.StandMode;

/**
 * The saved state of how a player is currently using their Stand.
 *
 * <p>Grouped into its own record purely to get under a codec limit: {@code RecordCodecBuilder.group}
 * accepts at most sixteen fields, and the player record had outgrown it. Nesting related fields is
 * the standard way out, and these four belong together anyway - they are all "what is the Stand
 * doing right now", as opposed to the stats and progression around them.
 *
 * <p>{@link JojohaPlayerData} keeps them as plain flat fields and only assembles this on the way in
 * and out of serialisation, so no call site has to know the grouping exists.
 */
public record StandSession(boolean summoned, int modeOrdinal, boolean piloting, int timeStopExposures,
                           int timeStopFrozenTicks, int timeStopHeldTicks, int timeStopCastTicks,
                           int timeStopCasts, int buildModeOrdinal, int timeStopUsesThisFight,
                           int killProgress, java.util.List<String> equipped,
                           java.util.List<String> utilityEquipped, int standPoints) {
    public static final StandSession DEFAULT =
            new StandSession(false, StandMode.ANALOG.ordinal(), false, 0, 0, 0, 0, 0,
                    org.gumel.jojoha.stand.BuildMode.SINGLE.ordinal(), 0, 0,
                    java.util.List.of(), java.util.List.of(), 0);

    public static final Codec<StandSession> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("summoned", false).forGetter(StandSession::summoned),
            Codec.INT.optionalFieldOf("mode", StandMode.ANALOG.ordinal()).forGetter(StandSession::modeOrdinal),
            Codec.BOOL.optionalFieldOf("piloting", false).forGetter(StandSession::piloting),
            Codec.INT.optionalFieldOf("time_stop_exposures", 0).forGetter(StandSession::timeStopExposures),
            // Carried here because the client has to know about it to stop reading the keyboard.
            // It also gets written to disk as a side effect, which is harmless: the count is only
            // ever a second or two and runs itself down immediately on load.
            Codec.INT.optionalFieldOf("time_stop_frozen", 0).forGetter(StandSession::timeStopFrozenTicks),
            // The caster's own counter. They are not frozen by their own stop, but their client
            // still has to know it is running in order to draw it.
            Codec.INT.optionalFieldOf("time_stop_held", 0).forGetter(StandSession::timeStopHeldTicks),
            // The wind-up. Synced because the world starts folding while it charges, so the client
            // has to know a cast is underway before the freeze itself lands.
            Codec.INT.optionalFieldOf("time_stop_cast", 0).forGetter(StandSession::timeStopCastTicks),
            // How many stops this player has thrown. Practice, which lifts the ceiling on how long
            // they can hold one - see TimeStopSkill. Kept here rather than with the stats because it
            // is not a stat: it cannot be spent, reset or reallocated, only accumulated.
            Codec.INT.optionalFieldOf("time_stop_casts", 0).forGetter(StandSession::timeStopCasts),
            // The building shape. Synced rather than kept server-side because the preview has to
            // draw the run before the click exists, and it cannot draw a shape it does not know.
            Codec.INT.optionalFieldOf("build_mode", org.gumel.jojoha.stand.BuildMode.SINGLE.ordinal())
                    .forGetter(StandSession::buildModeOrdinal),
            // How many stops have been thrown in the fight currently under way. Saved, because it
            // is the only thing standing between a player and an unlimited supply of them: it is
            // spent per fight rather than per cooldown, so a player who quit and came back used to
            // return with the count at zero and every use restored.
            Codec.INT.optionalFieldOf("time_stop_uses_this_fight", 0)
                    .forGetter(StandSession::timeStopUsesThisFight),
            // Progress toward the next stat point. Saved for the obvious reason: a total that reset
            // on every log-out would make short sessions worth nothing at all.
            Codec.INT.optionalFieldOf("kill_progress", 0).forGetter(StandSession::killProgress),
            // What is on the bar, by slot. Ids rather than indices into a moveset, because a
            // moveset is derived from the Stand and would silently re-point every slot the moment
            // anything about the Stand changed - a different skin order, a new signature move, a
            // range override. An id means the same move tomorrow.
            //
            // An empty string is an empty slot. A list rather than a map because the slots are a
            // fixed run of positions and their order is the whole meaning of the thing.
            Codec.STRING.listOf().optionalFieldOf("equipped", java.util.List.of())
                    .forGetter(StandSession::equipped),
            // The Utility stance keeps its own bar. Optional and empty by default, so a save from
            // before it existed reads back as "never touched" and falls through to the stock tools.
            //
            // LAST, and it has to be: RecordCodecBuilder hands the group's values to the canonical
            // constructor in the order they are listed, and this record takes "equipped" before
            // "utilityEquipped". Listed the other way round - as it was - both are List<String>, so
            // nothing complains at compile time and the two bars simply swap places at runtime. Any
            // field added here goes after this one, matching the record.
            Codec.STRING.listOf().optionalFieldOf("utility_equipped", java.util.List.of())
                    .forGetter(StandSession::utilityEquipped),
            // Optional so saves made before the Stand had its own points still decode - a required
            // field missing from older NBT fails the whole decode and silently wipes the player.
            Codec.INT.optionalFieldOf("stand_points", 0).forGetter(StandSession::standPoints)
    ).apply(instance, StandSession::new));
}
