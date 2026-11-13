package org.gumel.jojoha.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.JojohaAdvancements;
import org.gumel.jojoha.combat.EnergySystem;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.DebugCooldowns;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandData;
import org.gumel.jojoha.data.StatPoints;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.dream.DreamRealm;
import org.gumel.jojoha.fortune.Cursed;
import org.gumel.jojoha.fortune.Fortune;
import org.gumel.jojoha.fortune.Fortunes;
import org.gumel.jojoha.fortune.Honor;
import org.gumel.jojoha.hamon.HamonPath;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.network.packet.StingPacket;
import org.gumel.jojoha.registry.ModTraits;
import org.gumel.jojoha.skilltree.SkillNode;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.FightStings;
import org.gumel.jojoha.stand.StandRange;
import org.gumel.jojoha.stand.StandRelease;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.anubis.Anubis;
import org.gumel.jojoha.stand.anubis.Possession;
import org.gumel.jojoha.stand.mob.MobStands;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.stand.skill.moves.TimeStopSkill;
import org.gumel.jojoha.stand.workshop.Unlocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.gumel.jojoha.worldevent.MinorEvents;
import org.gumel.jojoha.worldevent.WorldEvent;
import org.gumel.jojoha.worldevent.WorldEvents;

public final class JojohaCommands {
    private static final String PLAYERS = "players";

    private static final String ALL = "all";

    private static final String NONE = "none";

    private static final String[] BARS = {"spec", "stand", ALL};

    private static final int TOP_TRUST = 3;

    private static final int MAX_EXPOSURES = 99;

    @FunctionalInterface
    private interface Action {
        int run(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) throws CommandSyntaxException;
    }

    private JojohaCommands() {
    }

    public static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> register(dispatcher));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jojoha")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("player")
                        .then(Commands.literal("stat")
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .suggests(names(() -> Stream.concat(Arrays.stream(StatPoints.PLAYER_NAMES), Stream.of(ALL))))
                                        .then(forPlayers(Commands.argument("value", IntegerArgumentType.integer(0, StatPoints.MAX_STAT)),
                                                JojohaCommands::setStat))))
                        .then(Commands.literal("fate")
                                .then(forPlayers(Commands.argument("fate", StringArgumentType.word())
                                        .suggests(names(Fate.values())), JojohaCommands::setFate)))
                        .then(Commands.literal("discipline")
                                .then(forPlayers(Commands.literal("none"),
                                        (ctx, players) -> setDiscipline(ctx, players, Discipline.NONE, null)))
                                .then(forPlayers(Commands.literal("anubis"),
                                        (ctx, players) -> setDiscipline(ctx, players, Discipline.ANUBIS, null)))
                                .then(Commands.literal("hamon")
                                        .executes(ctx -> setDiscipline(ctx, List.of(ctx.getSource().getPlayerOrException()),
                                                Discipline.HAMON, null))
                                        .then(Commands.argument(PLAYERS, EntityArgument.players())
                                                .executes(ctx -> setDiscipline(ctx, EntityArgument.getPlayers(ctx, PLAYERS),
                                                        Discipline.HAMON, null))
                                                .then(Commands.argument("path", StringArgumentType.word())
                                                        .suggests(names(() -> Stream.concat(Stream.of(ALL),
                                                                HamonPaths.all().stream().map(path -> path.id().getPath()))))
                                                        .executes(ctx -> setDiscipline(ctx, EntityArgument.getPlayers(ctx, PLAYERS),
                                                                Discipline.HAMON, StringArgumentType.getString(ctx, "path")))))))
                        .then(Commands.literal("possess")
                                .then(forPlayers(Commands.argument("seconds", IntegerArgumentType.integer(0, 600)),
                                        JojohaCommands::setPossessed)))
                        .then(Commands.literal("trait")
                                .then(forPlayers(Commands.argument("trait", StringArgumentType.word())
                                        .suggests(names(() -> ModTraits.all().keySet().stream().map(ResourceLocation::getPath))),
                                        JojohaCommands::setTrait)))
                        .then(Commands.literal("fortune")
                                .then(forPlayers(Commands.argument("fortune", StringArgumentType.word())
                                        .suggests(names(Fortune.values())), JojohaCommands::setFortune))))

                .then(Commands.literal("stand")
                        .then(Commands.literal("set")
                                .then(forPlayers(Commands.argument("stand", StringArgumentType.word())
                                        .suggests(names(() -> Stream.concat(Stream.of(NONE),
                                                StandTypes.all().keySet().stream().filter(id -> !StandTypes.secret(id))
                                                        .map(ResourceLocation::getPath)))),
                                        JojohaCommands::setStand)))
                        .then(Commands.literal("trust")
                                .then(forPlayers(Commands.argument("tier", IntegerArgumentType.integer(0, TOP_TRUST)),
                                        JojohaCommands::setTrust)))
                        .then(Commands.literal("skin")
                                .then(forPlayers(Commands.argument("skin", IntegerArgumentType.integer(0)),
                                        JojohaCommands::setSkin)))
                        .then(Commands.literal("stat")
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .suggests(names(() -> Stream.concat(Arrays.stream(StatPoints.STAND_NAMES), Stream.of(ALL))))
                                        .then(forPlayers(Commands.argument("value", IntegerArgumentType.integer(0, StatPoints.MAX_STAT)),
                                                JojohaCommands::setStandStat))))
                        .then(Commands.literal("exposures")
                                .then(forPlayers(Commands.argument("exposures", IntegerArgumentType.integer(0, MAX_EXPOSURES)),
                                        JojohaCommands::setTimeStopExposures))))

                .then(Commands.literal("event")
                        .then(Commands.literal("stop").executes(JojohaCommands::stopEvent))
                        .then(Commands.literal("status").executes(JojohaCommands::eventStatus))
                        .then(Commands.literal("start")
                                .then(Commands.argument("event", StringArgumentType.word())
                                        .suggests(names(() -> Stream.concat(Stream.of(WorldEvent.values()).map(WorldEvent::id),
                                                Stream.of(MinorEvents.SKIRMISH, MinorEvents.STAND_USER))))
                                        .executes(JojohaCommands::startEvent))))

                .then(Commands.literal("debug")
                        .then(Commands.literal("mobstand")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(ctx -> giveMobStand(ctx, null))
                                        .then(Commands.argument("stand", StringArgumentType.word())
                                                .suggests(names(() -> MobStands.pool().stream().map(ResourceLocation::getPath)))
                                                .executes(ctx -> giveMobStand(ctx, StringArgumentType.getString(ctx, "stand"))))))
                        .then(forPlayers(Commands.literal("dream"), JojohaCommands::dreamNow)
                                .then(forPlayers(Commands.literal("wake"), JojohaCommands::dreamWake)))
                        .then(Commands.literal("range")
                                .then(Commands.literal("close").executes(ctx -> setStandRange(ctx, StandRange.CLOSE)))
                                .then(Commands.literal("long").executes(ctx -> setStandRange(ctx, StandRange.LONG)))
                                .then(Commands.literal("reset").executes(ctx -> setStandRange(ctx, null))))
                        .then(energy(Commands.literal("energy")
                                .then(Commands.literal("freeze").executes(ctx -> setEnergyFrozen(ctx, true)))
                                .then(Commands.literal("normal").executes(ctx -> setEnergyFrozen(ctx, false)))))
                        .then(Commands.literal("cooldown")
                                .then(forPlayers(Commands.literal("reset"), JojohaCommands::resetCooldowns))
                                .then(Commands.literal("none").executes(ctx -> setNoCooldowns(ctx, true)))
                                .then(Commands.literal("normal").executes(ctx -> setNoCooldowns(ctx, false))))
                        .then(Commands.literal("sting").executes(ctx -> sting(ctx,
                                FightStings.DEFEAT_PREVIEW)))
                        .then(Commands.literal("reload").executes(JojohaCommands::reloadConfig))
                        .then(forPlayers(Commands.literal("max"), JojohaCommands::maxAll))
                        .then(Commands.literal("workshop")
                                .then(forPlayers(Commands.literal("unlock"), (ctx, players) -> setWorkshopUnlocked(ctx, players, true)))
                                .then(forPlayers(Commands.literal("lock"), (ctx, players) -> setWorkshopUnlocked(ctx, players, false))))));
    }

    private static int sting(CommandContext<CommandSourceStack> ctx, int kind)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        NetworkManager.sendToPlayer(player,
                new StingPacket(kind));
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> energy(LiteralArgumentBuilder<CommandSourceStack> energy) {
        for (String bar : BARS) {
            energy.then(Commands.literal(bar)
                    .then(forPlayers(Commands.literal("max"), (ctx, players) -> setEnergy(ctx, players, bar, null)))
                    .then(forPlayers(Commands.argument("value", IntegerArgumentType.integer(0)),
                            (ctx, players) -> setEnergy(ctx, players, bar, IntegerArgumentType.getInteger(ctx, "value")))));
        }
        return energy;
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T forPlayers(T node, Action action) {
        return node.executes(ctx -> action.run(ctx, List.of(ctx.getSource().getPlayerOrException())))
                .then(Commands.argument(PLAYERS, EntityArgument.players())
                        .executes(ctx -> action.run(ctx, EntityArgument.getPlayers(ctx, PLAYERS))));
    }

    private static int setStat(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        String stat = lower(StringArgumentType.getString(ctx, "stat"));
        int value = IntegerArgumentType.getInteger(ctx, "value");
        int index = statIndex(StatPoints.PLAYER_NAMES, StatPoints.PLAYER_ABBREVIATIONS, stat);
        if (index < 0 && !ALL.equals(stat)) {
            return fail(ctx, "commands.jojoha.unknown.stat", stat);
        }

        for (ServerPlayer player : players) {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            for (int i = 0; i < StatPoints.COUNT; i++) {
                if (index < 0 || i == index) {
                    setPlayerStat(data, i, value);
                }
            }
            save(player, data);
        }
        return report(ctx, players, "commands.jojoha.stat.set", statName(StatPoints.PLAYER_NAMES, index,
                "commands.jojoha.stat.all"), value);
    }

    private static void setPlayerStat(JojohaPlayerData data, int stat, int value) {
        switch (stat) {
            case StatPoints.STRENGTH -> data.strength = value;
            case StatPoints.VITALITY -> data.vitality = value;
            case StatPoints.AGILITY -> data.agility = value;
            case StatPoints.ENDURANCE -> data.endurance = value;
            case StatPoints.WORTHINESS -> data.worthiness = value;
            default -> {
            }
        }
    }

    private static int setEnergy(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players,
                                 String bar, Integer value) {
        boolean spec = !bar.equals("stand");
        boolean stand = !bar.equals("spec");
        if (bar.equals("stand") && standUsers(players).isEmpty()) {
            return fail(ctx, "commands.jojoha.no_stand", who(players));
        }

        for (ServerPlayer player : players) {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            if (spec) {
                data.specEnergy = value == null ? JojohaPlayerData.MAX_SPEC_ENERGY
                        : Math.min(value, JojohaPlayerData.MAX_SPEC_ENERGY);
            }
            if (stand && data.stand.isPresent()) {
                data.standEnergy = value == null ? data.maxStandEnergy() : Math.min(value, data.maxStandEnergy());
            }
            save(player, data);
        }
        return report(ctx, players, "commands.jojoha.energy.set",
                Component.translatable("commands.jojoha.energy." + bar),
                value == null ? Component.translatable("commands.jojoha.energy.full") : Component.literal(String.valueOf(value)));
    }

    private static int setFate(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        String name = StringArgumentType.getString(ctx, "fate");
        Fate fate = parse(Fate.values(), name);
        if (fate == null) {
            return fail(ctx, "commands.jojoha.unknown.fate", name);
        }

        for (ServerPlayer player : players) {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            data.vampireStage = fate == Fate.HUMAN ? VampireStage.NONE : VampireStage.valueOf(fate.name());
            data.stoneMaskRitualTicks = 0;
            if (fate == Fate.HUMAN) {
                data.stoneMaskWorn = false;
            }
            if (fate == Fate.VAMPIRE) {
                JojohaAdvancements.award(player, "part1/the_change");
            }
            save(player, data);
        }
        return report(ctx, players, "commands.jojoha.fate.set", Component.translatable(fate.translationKey()));
    }

    private static int setDiscipline(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players,
                                     Discipline discipline, String pathName) {
        List<HamonPath> paths = List.of();
        if (pathName != null) {
            HamonPath path = HamonPaths.byId(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, lower(pathName)));
            if (ALL.equals(lower(pathName))) {
                paths = List.copyOf(HamonPaths.all());
            } else if (path != null) {
                paths = List.of(path);
            } else {
                return fail(ctx, "commands.jojoha.unknown.path", pathName);
            }
        }

        for (ServerPlayer player : players) {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            if (discipline == Discipline.ANUBIS) {
                Anubis.bond(data);
            } else if (data.anubis) {
                Anubis.unbond(player, data);
            }
            data.discipline = discipline;
            if (discipline == Discipline.HAMON) {
                data.grantHamonPath(HamonPaths.HERMIT.id());
                for (HamonPath path : paths) {
                    data.grantHamonPath(path.id());
                }
                JojohaAdvancements.award(player, "part2/first_breath");
            } else {
                data.clearHamonPaths();
            }
            save(player, data);
        }
        if (discipline != Discipline.HAMON) {
            return report(ctx, players, "commands.jojoha.discipline.set", Component.translatable(disciplineKey(discipline)));
        }
        if (paths.size() > 1) {
            return report(ctx, players, "commands.jojoha.discipline.hamon.all");
        }
        if (paths.size() == 1 && paths.get(0) != HamonPaths.HERMIT) {
            return report(ctx, players, "commands.jojoha.discipline.hamon.path", paths.get(0).displayName());
        }
        return report(ctx, players, "commands.jojoha.discipline.hamon", Component.translatable(disciplineKey(discipline)));
    }

    private static int setTrait(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        String name = lower(StringArgumentType.getString(ctx, "trait"));
        ModTraits.Trait trait = ModTraits.byId(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, name));
        if (trait == null) {
            return fail(ctx, "commands.jojoha.unknown.trait", name);
        }

        for (ServerPlayer player : players) {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            data.trait = trait.id();
            save(player, data);
        }
        return report(ctx, players, "commands.jojoha.trait.set", Component.translatable(trait.nameKey()));
    }

    private static int setFortune(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        String name = StringArgumentType.getString(ctx, "fortune");
        Fortune fortune = parse(Fortune.values(), name);
        if (fortune == null) {
            return fail(ctx, "commands.jojoha.unknown.fortune", name);
        }
        if (!Fortunes.on()) {
            return fail(ctx, "commands.jojoha.fortune.off");
        }

        for (ServerPlayer player : players) {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            data.fortune = fortune.id();
            data.honor = Honor.START;
            data.curses = fortune == Fortune.CURSED ? Cursed.roll(player.getRandom()) : 0;
            data.lastConflict = 0L;
            data.fortuneTally = 0;
            data.fortuneUntil = 0L;
            save(player, data);
            if (fortune == Fortune.CURSED) {
                Fortunes.cursedWith(player, data);
            }
        }
        return report(ctx, players, "commands.jojoha.fortune.set", Component.translatable(fortune.nameKey()));
    }

    private static int setStand(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        String name = lower(StringArgumentType.getString(ctx, "stand"));
        if (NONE.equals(name)) {
            return clearStand(ctx, players);
        }

        ResourceLocation standId = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, name);
        if (StandTypes.byId(standId) == null) {
            return fail(ctx, "commands.jojoha.unknown.stand", name);
        }

        for (ServerPlayer player : players) {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            boolean swapped = !standId.equals(data.stand.isPresent() ? data.stand.standId() : null);
            if (swapped) {
                StandRelease.release(player, data);
            }
            data.stand = new StandData(standId, TrustTier.BONDED.level(), 5, 5, 5, 5, 5, 0);
            save(player, data);
            if (StandTypes.secret(standId)) {
                ascend(player);
            }
        }
        return report(ctx, players, "commands.jojoha.stand.set", standName(standId));
    }

    private static void ascend(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        for (SkillNode node : SkillTrees.forStand(data.stand.standId())) {
            if (!node.innate()) {
                data.grantNode(node.id());
            }
        }
        data.timeStopExposures = MAX_EXPOSURES;
        PlayerDataAccess.set(player, data);
        for (int stat = 0; stat < StatPoints.COUNT; stat++) {
            StatPoints.setStandStat(player, stat, StatPoints.MAX_STAT);
        }
        applyTrust(player, PlayerDataAccess.get(player), TrustTier.fromLevel(TOP_TRUST));
        JojohaPlayerData after = PlayerDataAccess.get(player);
        after.standEnergy = after.maxStandEnergy();
        save(player, after);
    }

    private static int clearStand(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        List<ServerPlayer> users = standUsers(players);
        if (users.isEmpty()) {
            return fail(ctx, "commands.jojoha.no_stand", who(players));
        }

        for (ServerPlayer player : users) {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            StandRelease.release(player, data);
            save(player, data);
        }
        return report(ctx, users, "commands.jojoha.stand.removed");
    }

    private static int setTrust(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        List<ServerPlayer> users = standUsers(players);
        if (users.isEmpty()) {
            return fail(ctx, "commands.jojoha.no_stand", who(players));
        }

        TrustTier tier = TrustTier.fromLevel(IntegerArgumentType.getInteger(ctx, "tier"));
        for (ServerPlayer player : users) {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            applyTrust(player, data, tier);
        }
        return report(ctx, users, "commands.jojoha.stand.trust", tier.level(),
                Component.translatable("commands.jojoha.trust." + lower(tier.name())));
    }

    private static void applyTrust(ServerPlayer player, JojohaPlayerData data, TrustTier tier) {
        data.stand = data.stand.withTrust(tier);

        boolean wasSummoned = data.standSummoned;
        if (wasSummoned) {
            StandSummonHandler.dismiss(player, data);
        }

        save(player, data);

        if (wasSummoned) {
            StandSummonHandler.handleToggleRequest(player);
        }
    }

    private static int setSkin(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        int skin = IntegerArgumentType.getInteger(ctx, "skin");
        List<ServerPlayer> users = standUsers(players);
        if (users.isEmpty()) {
            return fail(ctx, "commands.jojoha.no_stand", who(players));
        }

        List<ServerPlayer> changed = new ArrayList<>();
        for (ServerPlayer player : users) {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            if (skin < StandTypes.byIdOrDefault(data.stand.standId()).skins().size()) {
                data.stand = data.stand.withSkin(skin);
                save(player, data);
                changed.add(player);
            }
        }
        if (changed.isEmpty()) {
            return fail(ctx, "commands.jojoha.stand.no_skin", who(users), skin);
        }
        return report(ctx, changed, "commands.jojoha.stand.skin", skin);
    }

    private static int setStandStat(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        String stat = lower(StringArgumentType.getString(ctx, "stat"));
        int value = IntegerArgumentType.getInteger(ctx, "value");
        int index = statIndex(StatPoints.STAND_NAMES, StatPoints.STAND_ABBREVIATIONS, stat);
        if (index < 0 && !ALL.equals(stat)) {
            return fail(ctx, "commands.jojoha.unknown.stat", stat);
        }
        List<ServerPlayer> users = standUsers(players);
        if (users.isEmpty()) {
            return fail(ctx, "commands.jojoha.no_stand", who(players));
        }

        for (ServerPlayer player : users) {
            for (int i = 0; i < StatPoints.COUNT; i++) {
                if (index < 0 || i == index) {
                    StatPoints.setStandStat(player, i, value);
                }
            }
        }
        return report(ctx, users, "commands.jojoha.stat.set", statName(StatPoints.STAND_NAMES, index,
                "commands.jojoha.stand.stat.all"), value);
    }

    private static int setTimeStopExposures(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        int exposures = IntegerArgumentType.getInteger(ctx, "exposures");
        for (ServerPlayer player : players) {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            data.timeStopExposures = exposures;
            save(player, data);
        }
        return report(ctx, players, "commands.jojoha.stand.exposures", exposures, TimeStopSkill.REQUIRED_EXPOSURES);
    }

    private static int giveMobStand(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ResourceLocation chosen = null;
        if (name != null) {
            chosen = name.contains(":") ? ResourceLocation.tryParse(name)
                    : ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, lower(name));
            if (chosen == null || !MobStands.pool().contains(chosen)) {
                return fail(ctx, "commands.jojoha.mobstand.unknown", name, String.join(", ",
                        MobStands.pool().stream().map(ResourceLocation::getPath).toList()));
            }
        }

        List<Entity> given = new ArrayList<>();
        for (Entity entity : EntityArgument.getEntities(ctx, "targets")) {
            if (entity instanceof Mob mob && MobStands.canHold(mob)) {
                MobStands.give(mob, chosen != null ? chosen : MobStands.roll(mob.getRandom()), 0);
                given.add(entity);
            }
        }
        if (given.isEmpty()) {
            return fail(ctx, "commands.jojoha.mobstand.none");
        }
        Component mobs = given.size() == 1 ? given.get(0).getDisplayName()
                : Component.translatable("commands.jojoha.mobs", given.size());
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.jojoha.mobstand.given", mobs), true);
        return given.size();
    }

    private static int startEvent(CommandContext<CommandSourceStack> ctx) {
        String id = lower(StringArgumentType.getString(ctx, "event"));
        WorldEvent event = WorldEvent.byId(id);
        MinecraftServer server = ctx.getSource().getServer();
        if (event == null) {
            ServerPlayer player = ctx.getSource().getPlayer();
            if (player == null || !MinorEvents.force(id, player)) {
                ctx.getSource().sendFailure(Component.translatable("commands.jojoha.event.failed"));
                return 0;
            }
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.jojoha.event.started", id), true);
            return 1;
        }
        if (!WorldEvents.get(server).start(server, event, true)) {
            ctx.getSource().sendFailure(Component.translatable("commands.jojoha.event.failed"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.jojoha.event.started", event.id()), true);
        return 1;
    }

    private static int eventStatus(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        WorldEvents state = WorldEvents.get(server);
        long day = server.overworld().getDayTime() / WorldEvents.DAY;
        int first = JojohaServerConfig.FIRST_EVENT_DAY.whole();
        if (state.active() == null) {
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.jojoha.event.status.none", day, first), false);
            return 0;
        }
        long left = Math.max(0L, state.endsAt() - server.overworld().getGameTime()) / 20L;
        String id = state.active().id();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.jojoha.event.status.active", id, left / 60L,
                left % 60L), false);
        return 1;
    }

    private static int stopEvent(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        WorldEvents state = WorldEvents.get(server);
        if (state.active() == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.jojoha.event.none"));
            return 0;
        }
        state.stop(server);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.jojoha.event.stopped"), true);
        return 1;
    }

    private static int dreamNow(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        List<ServerPlayer> taken = new ArrayList<>();
        for (ServerPlayer player : players) {
            if (DreamRealm.dreaming(player)) {
                continue;
            }
            DreamRealm.take(player, PlayerDataAccess.get(player), player.level().getGameTime());
            if (DreamRealm.dreaming(player)) {
                DreamRealm.allowAgain(player);
                taken.add(player);
            } else {
                return fail(ctx, "commands.jojoha.dream.missing");
            }
        }
        if (taken.isEmpty()) {
            return fail(ctx, "commands.jojoha.dream.already", who(players));
        }
        return report(ctx, taken, "commands.jojoha.dream.taken");
    }

    private static int dreamWake(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        List<ServerPlayer> dreamers = players.stream().filter(DreamRealm::dreaming).toList();
        if (dreamers.isEmpty()) {
            return fail(ctx, "commands.jojoha.dream.not_dreaming", who(players));
        }
        dreamers.forEach(DreamRealm::wake);
        return report(ctx, dreamers, "commands.jojoha.dream.woken");
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> ctx) {
        JojohaServerConfig.FILE.reload();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.jojoha.config.reloaded",
                JojohaServerConfig.FILE.name()), true);
        return 1;
    }

    private static int setStandRange(CommandContext<CommandSourceStack> ctx, StandRange range) {
        StandSkills.setRangeOverride(range);
        ctx.getSource().sendSuccess(() -> range == null
                ? Component.translatable("commands.jojoha.stand_range.reset")
                : Component.translatable("commands.jojoha.stand_range", lower(range.name())), false);
        return 1;
    }

    private static int setEnergyFrozen(CommandContext<CommandSourceStack> ctx, boolean frozen) {
        EnergySystem.setFrozen(frozen);
        ctx.getSource().sendSuccess(() -> Component.translatable(frozen
                ? "commands.jojoha.energy.frozen" : "commands.jojoha.energy.normal"), false);
        return 1;
    }

    private static int resetCooldowns(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        int cleared = 0;
        for (ServerPlayer player : players) {
            cleared += DebugCooldowns.reset(player);
        }
        return report(ctx, players, "commands.jojoha.cooldown.reset", cleared);
    }

    private static int setNoCooldowns(CommandContext<CommandSourceStack> ctx, boolean none) {
        DebugCooldowns.setNone(none);
        if (none) {
            for (ServerPlayer online : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                DebugCooldowns.reset(online);
            }
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(none
                ? "commands.jojoha.cooldown.none" : "commands.jojoha.cooldown.normal"), false);
        return 1;
    }

    private static int setWorkshopUnlocked(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players,
                                           boolean unlocked) {
        for (ServerPlayer player : players) {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            if (unlocked) {
                data.workshopUnlocks.add(Unlocks.ALL);
            } else {
                data.workshopUnlocks.remove(Unlocks.ALL);
            }
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
        }
        return report(ctx, players, unlocked ? "commands.jojoha.workshop.unlocked" : "commands.jojoha.workshop.locked");
    }

    private static int maxAll(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        int unlocked = 0;
        for (ServerPlayer player : players) {
            unlocked += maxOne(player);
        }
        return report(ctx, players, "commands.jojoha.max", StatPoints.MAX_STAT, unlocked);
    }

    private static int maxOne(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);

        data.strength = StatPoints.MAX_STAT;
        data.vitality = StatPoints.MAX_STAT;
        data.agility = StatPoints.MAX_STAT;
        data.endurance = StatPoints.MAX_STAT;
        data.worthiness = StatPoints.MAX_STAT;

        List<SkillNode> nodes = new ArrayList<>(SkillTrees.of(SkillTrees.Tree.PLAYER));
        if (data.discipline == Discipline.HAMON) {
            nodes.addAll(SkillTrees.of(SkillTrees.Tree.HAMON));
        }
        if (Fate.ultimate(data)) {
            nodes.addAll(SkillTrees.of(SkillTrees.Tree.ULTIMATE));
        } else if (Fate.undead(data)) {
            nodes.addAll(SkillTrees.of(SkillTrees.Tree.VAMPIRE));
        }
        if (Fate.pillar(data)) {
            nodes.addAll(SkillTrees.of(SkillTrees.Tree.PILLAR));
        }
        boolean hasStand = data.stand.isPresent();
        if (hasStand) {
            nodes.addAll(SkillTrees.forStand(data.stand.standId()));
            data.timeStopExposures = MAX_EXPOSURES;
        }
        int unlocked = 0;
        for (SkillNode node : nodes) {
            if (node.innate() || data.hasNode(node.id())) {
                continue;
            }
            data.grantNode(node.id());
            HamonPath path = SkillTrees.pathOf(node);
            if (path != null) {
                data.grantHamonPath(path.id());
            }
            unlocked++;
        }
        data.specEnergy = JojohaPlayerData.MAX_SPEC_ENERGY;
        PlayerDataAccess.set(player, data);

        if (hasStand) {
            for (int stat = 0; stat < StatPoints.COUNT; stat++) {
                StatPoints.setStandStat(player, stat, StatPoints.MAX_STAT);
            }
            applyTrust(player, PlayerDataAccess.get(player), TrustTier.fromLevel(TOP_TRUST));
            JojohaPlayerData after = PlayerDataAccess.get(player);
            after.standEnergy = after.maxStandEnergy();
            PlayerDataAccess.set(player, after);
        }
        PlayerDataAccess.sync(player);
        return unlocked;
    }

    private static void save(ServerPlayer player, JojohaPlayerData data) {
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    private static List<ServerPlayer> standUsers(Collection<ServerPlayer> players) {
        return players.stream().filter(player -> PlayerDataAccess.get(player).stand.isPresent()).toList();
    }

    private static Component who(Collection<ServerPlayer> players) {
        return players.size() == 1 ? players.iterator().next().getDisplayName()
                : Component.translatable("commands.jojoha.players", players.size());
    }

    private static int setPossessed(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
        for (ServerPlayer player : players) {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            if (seconds > 0) {
                Possession.begin(player, data,
                        seconds * Possession.SECOND);
            } else if (data.anubisPossessed > 0) {
                data.anubisPossessed = 1;
            }
        }
        return report(ctx, players, "commands.jojoha.possess.set", seconds);
    }

    private static int report(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players,
                              String key, Object... args) {
        Object[] full = new Object[args.length + 1];
        full[0] = who(players);
        System.arraycopy(args, 0, full, 1, args.length);
        ctx.getSource().sendSuccess(() -> Component.translatable(key, full), true);
        return players.size();
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().sendFailure(Component.translatable(key, args));
        return 0;
    }

    private static Component standName(ResourceLocation standId) {
        return Component.translatable("stand." + standId.getNamespace() + "." + standId.getPath());
    }

    private static String disciplineKey(Discipline discipline) {
        return "discipline.jojoha." + lower(discipline.name());
    }

    private static int statIndex(String[] names, String[] abbreviations, String stat) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(stat) || lower(abbreviations[i]).equals(stat)) {
                return i;
            }
        }
        return -1;
    }

    private static Component statName(String[] names, int index, String allKey) {
        return index < 0 ? Component.translatable(allKey) : Component.literal(names[index]);
    }

    private static <E extends Enum<E>> E parse(E[] values, String name) {
        for (E value : values) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    private static SuggestionProvider<CommandSourceStack> names(Enum<?>[] values) {
        return names(() -> Arrays.stream(values).map(Enum::name));
    }

    private static SuggestionProvider<CommandSourceStack> names(Supplier<Stream<String>> names) {
        return (ctx, builder) -> SharedSuggestionProvider.suggest(
                names.get().map(name -> name.toUpperCase(Locale.ROOT)), builder);
    }

    private static String lower(String text) {
        return text.toLowerCase(Locale.ROOT);
    }
}
