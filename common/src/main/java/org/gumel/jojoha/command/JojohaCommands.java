package org.gumel.jojoha.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StatPoints;
import org.gumel.jojoha.stand.StandRange;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.stand.skill.moves.TimeStopSkill;
import org.gumel.jojoha.data.PlayerSpec;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.data.StandData;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.registry.ModTraits;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.TrustTier;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Operator-only debug commands used to exercise the player-data framework without a GUI:
 * {@code /jojoha stats get|set}, {@code /jojoha spec set}, {@code /jojoha trait set}.
 */
public final class JojohaCommands {

    /** Sentinel accepted by {@code /jojoha stand set} to strip a Stand rather than grant one. */
    private static final String NONE_ARGUMENT = "NONE";

    private JojohaCommands() {
    }

    public static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> register(dispatcher));
    }

    /**
     * Builds the tree.
     *
     * <p>Split out from {@link #init} so the shape can be inspected without a running server - the
     * registration event only fires when a world loads, which is a slow way to find out that two
     * literals collided or that a branch has no {@code executes} on it.
     *
     * <h2>How it is arranged</h2>
     *
     * <p>Three groups, by what they act on rather than by what they are called: {@code player} for
     * the person, {@code stand} for the thing they carry, {@code debug} for the switches that exist
     * only to make something testable and would never ship as gameplay.
     *
     * <p>It was previously flat - {@code stats}, {@code spec}, {@code trait}, {@code energy} and
     * {@code stand} all as siblings - which read fine with five branches and stopped reading at all
     * once there were more. Grouping also puts the answer to "what can I do to a Stand" in one
     * place instead of spread across the top level.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jojoha")
                .requires(source -> source.hasPermission(2))

                // ---- the person -------------------------------------------------------------
                .then(Commands.literal("player")
                        .then(Commands.literal("list").executes(JojohaCommands::getStats))
                        .then(Commands.literal("stat")
                                .then(Commands.literal("set")
                                        // "all" sits beside the stat names rather than being one of
                                        // them, so tab completion still only offers real stats when
                                        // you are naming one.
                                        .then(Commands.literal("all")
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(JojohaCommands::setAllStats)))
                                        .then(Commands.argument("stat", StringArgumentType.word())
                                                .suggests(JojohaCommands::suggestPlayerStats)
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(JojohaCommands::setStat))))
                                // Points live under stat because that is the only thing they buy.
                                .then(Commands.literal("points")
                                        .then(Commands.literal("give")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(JojohaCommands::givePoints)))
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(JojohaCommands::setPoints)))))
                        .then(Commands.literal("spec")
                                .then(Commands.argument("spec", StringArgumentType.word())
                                        .suggests(JojohaCommands::suggestSpecs)
                                        .executes(JojohaCommands::setSpec)))
                        .then(Commands.literal("trait")
                                .then(Commands.argument("trait", StringArgumentType.word())
                                        .suggests(JojohaCommands::suggestTraits)
                                        .executes(JojohaCommands::setTrait)))
                        .then(Commands.literal("energy")
                                .then(Commands.argument("bar", StringArgumentType.word())
                                        .suggests(JojohaCommands::suggestEnergyBars)
                                        // "max" is a literal rather than a magic number, because the
                                        // two bars do not share a maximum - the Stand's depends on
                                        // its trust - so there is no one number to type.
                                        .then(Commands.literal("max")
                                                .executes(ctx -> setEnergy(ctx, null)))
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                                .executes(ctx -> setEnergy(ctx,
                                                        (float) IntegerArgumentType.getInteger(ctx, "value")))))))

                // ---- the Stand --------------------------------------------------------------
                .then(Commands.literal("stand")
                        .then(Commands.literal("set")
                                .then(Commands.argument("stand", StringArgumentType.word())
                                        .suggests(JojohaCommands::suggestStandTypes)
                                        .executes(JojohaCommands::setStand)))
                        .then(Commands.literal("trust")
                                .then(Commands.argument("tier", IntegerArgumentType.integer(0, 3))
                                        .executes(JojohaCommands::setTrust)))
                        .then(Commands.literal("skin")
                                .then(Commands.argument("skin", IntegerArgumentType.integer(0))
                                        .executes(JojohaCommands::setSkin)))
                        .then(Commands.literal("stat")
                                .then(Commands.literal("set")
                                        .then(Commands.literal("all")
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(JojohaCommands::setAllStandStats)))
                                        .then(Commands.argument("stat", StringArgumentType.word())
                                                .suggests(JojohaCommands::suggestStandStats)
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(JojohaCommands::setStandStat))))
                                // Under stat, the same as the player's, and for the same reason:
                                // stats are the only thing these buy. The Stand's own pool, which
                                // the player's five cannot touch and vice versa.
                                .then(Commands.literal("points")
                                        .then(Commands.literal("give")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(JojohaCommands::giveStandPoints)))
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(JojohaCommands::setStandPoints)))))
                        .then(Commands.literal("exposures")
                                .then(Commands.argument("exposures", IntegerArgumentType.integer(0, 99))
                                        .executes(JojohaCommands::setTimeStopExposures))))

                // ---- switches that only exist to make something testable ---------------------
                .then(Commands.literal("debug")
                        .then(Commands.literal("range")
                                .then(Commands.argument("range", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                new String[] {"close", "long", "reset"}, b))
                                        .executes(JojohaCommands::setStandRange)))
                        .then(Commands.literal("energy")
                                .then(Commands.argument("state", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                new String[] {"freeze", "normal"}, b))
                                        .executes(JojohaCommands::setEnergyFrozen)))));
    }

    /**
     * Sets how many time stops the player has been caught in, which is what unlocks their own.
     *
     * <p>Exists because the intended route - surviving somebody else's stop three times - needs a
     * second Stand user to exist, and there is currently no way to arrange that in single player.
     */
    private static int setTimeStopExposures(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int exposures = IntegerArgumentType.getInteger(ctx, "exposures");

        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.timeStopExposures = exposures;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        ctx.getSource().sendSuccess(() -> Component.literal(
                "Time stop exposures: " + exposures + "/" + TimeStopSkill.REQUIRED_EXPOSURES
                        + (exposures >= TimeStopSkill.REQUIRED_EXPOSURES ? " (unlocked)" : "")), false);
        return 1;
    }

    /**
     * Debug: forces the range class every Stand is treated as, so long-range behaviour can be
     * exercised before a long-range Stand exists. "reset" hands control back to the Stand's own
     * declared range. See {@link StandSkills#setRangeOverride}.
     */
    private static int setStandRange(CommandContext<CommandSourceStack> ctx) {
        String choice = StringArgumentType.getString(ctx, "range").toLowerCase(java.util.Locale.ROOT);
        StandRange range = switch (choice) {
            case "close" -> StandRange.CLOSE;
            case "long" -> StandRange.LONG;
            default -> null;
        };

        StandSkills.setRangeOverride(range);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Stand range override: " + (range == null ? "none (using each Stand's own)" : range)), false);
        return 1;
    }

    /** Hands over points to spend in the menu. The whole reason the plus buttons have anything to do. */
    private static int givePoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        StatPoints.grant(player, amount);

        int total = PlayerDataAccess.get(player).availableStatPoints;
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Gave " + amount + " stat point" + (amount == 1 ? "" : "s") + " (now " + total + ")"), true);
        return 1;
    }

    private static int setPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        StatPoints.set(player, amount);

        ctx.getSource().sendSuccess(() -> Component.literal("Stat points set to " + amount), true);
        return 1;
    }

    /** Sets one of the Stand's five. Its own stats, not its user's - see StatPoints. */
    private static int setStandStat(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String stat = StringArgumentType.getString(ctx, "stat").toLowerCase(Locale.ROOT);
        int value = IntegerArgumentType.getInteger(ctx, "value");

        int index = indexOf(StatPoints.STAND_NAMES, stat);
        if (index < 0) {
            ctx.getSource().sendFailure(Component.literal("Unknown Stand stat: " + stat));
            return 0;
        }

        if (!StatPoints.setStandStat(player, index, value)) {
            ctx.getSource().sendFailure(Component.literal("You don't have a Stand yet."));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("Stand " + stat + " set to " + value), true);
        return 1;
    }

    /** Puts a different look on the Stand, which nothing else exposes outside the fractured arrow. */
    private static int setSkin(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int skin = IntegerArgumentType.getInteger(ctx, "skin");

        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent()) {
            ctx.getSource().sendFailure(Component.literal("You don't have a Stand yet."));
            return 0;
        }

        StandType type = StandTypes.byIdOrDefault(data.stand.standId());
        if (skin >= type.skins().size()) {
            ctx.getSource().sendFailure(Component.literal(
                    "That Stand has " + type.skins().size() + " skins (0-" + (type.skins().size() - 1) + ")."));
            return 0;
        }

        data.stand = data.stand.withSkin(skin);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        ctx.getSource().sendSuccess(() -> Component.literal(
                "Skin set to " + skin + " (" + type.skinNameKey(skin) + ")"), true);
        return 1;
    }

    private static int indexOf(String[] names, String name) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private static int getStats(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        JojohaPlayerData data = PlayerDataAccess.get(player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "STR=%d VIT=%d AGL=%d END=%d WOR=%d | statPoints=%d standPoints=%d | spec=%s | trait=%s | specEnergy=%.0f | standEnergy=%.0f | stand=%s trust=%s".formatted(
                        data.strength, data.vitality, data.agility, data.endurance, data.worthiness,
                        data.availableStatPoints, data.availableStandPoints,
                        data.spec, data.trait, data.specEnergy, data.standEnergy,
                        data.stand.isPresent() ? data.stand.standId() : "none",
                        data.stand.isPresent() ? data.stand.trust() : "-")), false);
        return 1;
    }

    private static int setStat(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String stat = StringArgumentType.getString(ctx, "stat").toLowerCase(Locale.ROOT);
        int value = Math.min(IntegerArgumentType.getInteger(ctx, "value"), StatPoints.MAX_STAT);
        JojohaPlayerData data = PlayerDataAccess.get(player);

        switch (stat) {
            case "strength", "str" -> data.strength = value;
            case "vitality", "vit" -> data.vitality = value;
            case "agility", "agl" -> data.agility = value;
            case "endurance", "end" -> data.endurance = value;
            case "worthiness", "wor" -> data.worthiness = value;
            default -> {
                ctx.getSource().sendFailure(Component.literal("Unknown stat: " + stat));
                return 0;
            }
        }

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        ctx.getSource().sendSuccess(() -> Component.literal("Set " + stat + " to " + value), true);
        return 1;
    }

    private static int setSpec(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String specName = StringArgumentType.getString(ctx, "spec");
        PlayerSpec spec;
        try {
            spec = PlayerSpec.valueOf(specName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("Unknown spec: " + specName));
            return 0;
        }

        JojohaPlayerData data = PlayerDataAccess.get(player);

        // Everything the old spec left behind goes with it. Setting the field alone was only half a
        // change: a player switched away from vampirism stayed a vampire, because the stage, the
        // mask on their face and the Hamon paths they had learned all lived outside the field being
        // set - so the command could grant a spec but never take one away, and there was no way to
        // test becoming anything a second time.
        clearSpecState(data);
        data.spec = spec;

        if (spec == PlayerSpec.HAMON) {
            data.grantHamonPath(HamonPaths.HERMIT.id());
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "You feel your body harmonizing with Hamon. Hermit Path learned - Ripple Pulse unlocked."), true);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("Spec set to " + spec), true);
        }

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        return 1;
    }

    /**
     * Strips everything a spec confers, so the next one starts from nothing.
     *
     * <p>Deliberately not selective about which spec is being left. A player is only ever meant to
     * have one, so anything belonging to any of them is stale the moment the spec changes - and
     * clearing the lot is both simpler and impossible to get subtly wrong when a fourth spec turns
     * up later.
     */
    private static void clearSpecState(JojohaPlayerData data) {
        data.vampireStage = VampireStage.NONE;
        data.stoneMaskWorn = false;
        data.stoneMaskRitualTicks = 0;
        data.clearHamonPaths();
    }

    private static int setTrait(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String traitPath = StringArgumentType.getString(ctx, "trait");
        ResourceLocation traitId = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, traitPath);
        ModTraits.Trait trait = ModTraits.byId(traitId);
        if (trait == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown trait: " + traitPath));
            return 0;
        }

        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.trait = traitId;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        ctx.getSource().sendSuccess(() -> Component.literal("Trait set to " + traitPath + ": " + trait.description()), true);
        return 1;
    }

    /**
     * Sets one bar or both.
     *
     * <p>A null value means "whatever this bar's maximum is", which cannot be a number typed at the
     * command line: the spec bar has a fixed ceiling and the Stand's depends on its trust tier, so
     * the two do not share one.
     */
    private static int setEnergy(CommandContext<CommandSourceStack> ctx, Float value)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String bar = StringArgumentType.getString(ctx, "bar").toLowerCase(Locale.ROOT);
        JojohaPlayerData data = PlayerDataAccess.get(player);

        boolean spec = bar.equals("spec") || bar.equals("all");
        boolean stand = bar.equals("stand") || bar.equals("all");
        if (!spec && !stand) {
            ctx.getSource().sendFailure(Component.literal("Unknown energy bar: " + bar));
            return 0;
        }

        if (spec) {
            data.specEnergy = value == null ? JojohaPlayerData.MAX_SPEC_ENERGY : value;
        }
        if (stand) {
            data.standEnergy = value == null ? data.maxStandEnergy() : value;
        }

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        String shown = value == null ? "max" : String.valueOf(value.intValue());
        ctx.getSource().sendSuccess(() -> Component.literal("Set " + bar + " energy to " + shown), true);
        return 1;
    }

    /** Every one of the player's five at once, which is most of what a test setup wants. */
    private static int setAllStats(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int value = Math.min(IntegerArgumentType.getInteger(ctx, "value"), StatPoints.MAX_STAT);
        JojohaPlayerData data = PlayerDataAccess.get(player);

        data.strength = value;
        data.vitality = value;
        data.agility = value;
        data.endurance = value;
        data.worthiness = value;

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        ctx.getSource().sendSuccess(() -> Component.literal("Set every player stat to " + value), true);
        return 1;
    }

    /** The same for the Stand's five. */
    private static int setAllStandStats(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int value = IntegerArgumentType.getInteger(ctx, "value");

        for (int stat = 0; stat < StatPoints.COUNT; stat++) {
            if (!StatPoints.setStandStat(player, stat, value)) {
                ctx.getSource().sendFailure(Component.literal("You don't have a Stand yet."));
                return 0;
            }
        }

        ctx.getSource().sendSuccess(() -> Component.literal("Set every Stand stat to " + value), true);
        return 1;
    }

    private static int giveStandPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        StatPoints.grantStand(player, amount);

        int total = PlayerDataAccess.get(player).availableStandPoints;
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Gave " + amount + " Stand point" + (amount == 1 ? "" : "s") + " (now " + total + ")"), true);
        return 1;
    }

    private static int setStandPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        StatPoints.setStand(player, amount);

        ctx.getSource().sendSuccess(() -> Component.literal("Stand points set to " + amount), true);
        return 1;
    }

    /**
     * Debug: holds both energy bars at full so a Stand can be kept out indefinitely.
     *
     * <p>It tops the bars up every tick rather than trying to stop each thing that spends them.
     * Costs are deducted all over - drain while summoned, every skill, the grapple - and chasing
     * each one would mean a flag threaded through all of them, and a new one missed every time a
     * move is added. Refilling covers all of it including moves written later.
     */
    private static int setEnergyFrozen(CommandContext<CommandSourceStack> ctx) {
        boolean frozen = StringArgumentType.getString(ctx, "state")
                .toLowerCase(Locale.ROOT).equals("freeze");
        org.gumel.jojoha.combat.EnergySystem.setFrozen(frozen);
        ctx.getSource().sendSuccess(() -> Component.literal(
                frozen ? "Energy frozen - both bars held at full."
                        : "Energy back to normal."), false);
        return 1;
    }

    /** Debug-only until real Stand obtainment (dungeons, bosses, etc.) is built. */
    private static int setStand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String argument = StringArgumentType.getString(ctx, "stand");

        // Names are shown and accepted in caps (STAR_PLATINUM) to match how Stands are written
        // everywhere else, but the underlying ids stay lowercase resource locations.
        if (NONE_ARGUMENT.equalsIgnoreCase(argument)) {
            return clearStand(ctx, player);
        }

        String standPath = argument.toLowerCase(Locale.ROOT);
        ResourceLocation standId = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, standPath);
        StandType type = StandTypes.byId(standId);
        if (type == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown Stand: " + argument));
            return 0;
        }

        JojohaPlayerData data = PlayerDataAccess.get(player);
        // Granted BONDED, unlike the Stand Arrow's canonical DORMANT start, purely so this debug
        // command still lands you somewhere testable - a tier-0 Stand can't be summoned at all.
        // Use `/jojoha stand trust <0-3>` to walk back down and exercise the lower tiers.
        // The bar is cleared with the Stand, because the moves on it belonged to the old one.
        //
        // Nothing checks that an equipped move is one this Stand actually has - the bar is a list of
        // ids - so a Star Platinum bar carried onto Hermit Purple leaves five slots that look
        // usable, are refused on every press, and cannot be told apart from a bug. Emptying it is
        // the only state that is honest about what just happened.
        boolean swapped = !standId.equals(data.stand.isPresent() ? data.stand.standId() : null);
        data.stand = new StandData(standId, TrustTier.BONDED.level(), 5, 5, 5, 5, 5, 0);
        if (swapped) {
            java.util.Arrays.fill(data.equippedSkills, null);
            java.util.Arrays.fill(data.utilityEquippedSkills, null);

            // And the tree with it. Clearing the bar alone left every move learned on the old Stand
            // still marked as learned, so the new Stand's page opened already showing them - which
            // is the same lie the bar was telling, one screen further back.
            //
            // Only the Stand nodes go. Hamon paths, the player tree and vampirism live in the same
            // set and have nothing to do with which Stand you are holding.
            data.getUnlockedNodes().removeAll(
                    org.gumel.jojoha.skilltree.SkillTrees.allStandNodeIds());
        }
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Stand granted: " + standPath.toUpperCase(Locale.ROOT) + " (Trust Tier 3 BONDED)"), true);
        return 1;
    }

    /**
     * Strips the player's Stand entirely, un-summoning first so a manifested one isn't orphaned
     * in the world with nothing backing it. Mainly here to re-test the Stand Arrow ritual, which
     * refuses to run for anyone who already has a Stand.
     */
    private static int clearStand(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent()) {
            ctx.getSource().sendFailure(Component.literal("You don't have a Stand to remove."));
            return 0;
        }

        if (data.standSummoned) {
            StandSummonHandler.dismissImmediately(player, data);
        }

        data.stand = StandData.NONE;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        ctx.getSource().sendSuccess(() -> Component.literal("Stand removed."), true);
        return 1;
    }

    /**
     * Moves a Stand between Trust Tiers, re-casting in place if one was already out so the new
     * tier's manifestation shape takes effect immediately - the entity's tier is synced once, at
     * spawn, so an existing one would otherwise keep rendering and behaving as its old tier.
     */
    private static int setTrust(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent()) {
            ctx.getSource().sendFailure(Component.literal("You don't have a Stand yet."));
            return 0;
        }

        TrustTier tier = TrustTier.fromLevel(IntegerArgumentType.getInteger(ctx, "tier"));
        data.stand = data.stand.withTrust(tier);

        // Re-summon rather than just dismissing when the manifestation shape changes, so
        // switching PARTIAL <-> full takes effect immediately instead of on the next summon
        // (the entity's synced tier is set once, at spawn).
        boolean wasSummoned = data.standSummoned;
        if (wasSummoned) {
            StandSummonHandler.dismiss(player, data);
        }

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        if (wasSummoned) {
            StandSummonHandler.handleToggleRequest(player);
        }

        ctx.getSource().sendSuccess(() -> Component.literal("Trust Tier set to %d (%s)".formatted(tier.level(), tier)), true);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestStandTypes(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                Stream.concat(
                        Stream.of(NONE_ARGUMENT),
                        StandTypes.all().keySet().stream()
                                .map(id -> id.getPath().toUpperCase(Locale.ROOT))),
                builder);
    }

    private static CompletableFuture<Suggestions> suggestPlayerStats(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(StatPoints.PLAYER_NAMES, builder);
    }

    private static CompletableFuture<Suggestions> suggestStandStats(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(StatPoints.STAND_NAMES, builder);
    }

    private static CompletableFuture<Suggestions> suggestEnergyBars(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(List.of("spec", "stand", "all"), builder);
    }

    private static CompletableFuture<Suggestions> suggestSpecs(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                Arrays.stream(PlayerSpec.values()).map(s -> s.name().toLowerCase(Locale.ROOT)), builder);
    }

    private static CompletableFuture<Suggestions> suggestTraits(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                ModTraits.all().keySet().stream().map(ResourceLocation::getPath), builder);
    }
}
