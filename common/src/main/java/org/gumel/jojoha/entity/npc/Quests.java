package org.gumel.jojoha.entity.npc;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.entity.VampireEntity;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.item.PathScrollItem;
import org.gumel.jojoha.level.StructureSeek;
import org.gumel.jojoha.network.packet.QuestBannerPacket;
import org.gumel.jojoha.network.packet.QuestLogPacket;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.anubis.Anubis;
import org.gumel.jojoha.town.NpcMemory;
import org.gumel.jojoha.town.Settlement;
import org.gumel.jojoha.town.Settlements;
import org.gumel.jojoha.worldevent.VampireNight;
import org.gumel.jojoha.worldevent.WorldEvent;
import org.gumel.jojoha.worldevent.WorldEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class Quests extends SavedData {
    public enum Objective {
        SLAY, GATHER, SURVIVE, HUNT, DELIVER, ESCORT, INVESTIGATE, DEFEND, DUEL, RESCUE, FEED, RAID, BREATH, BOUNTY;

        public Objective group() {
            return switch (this) {
                case SLAY, DEFEND, DUEL, FEED, RAID -> SLAY;
                case GATHER, DELIVER, ESCORT -> GATHER;
                case SURVIVE, BREATH -> SURVIVE;
                case HUNT, INVESTIGATE, RESCUE, BOUNTY -> HUNT;
            };
        }

        public static final Objective[] GROUPS = {SLAY, GATHER, SURVIVE, HUNT};
    }

    public enum Quarry { VAMPIRE, HAMON, STAND_USER }

    public static final String HAMON = "hamon_student";
    public static final String RESIDENT = "resident";
    public static final String RUMOUR = "rumour";
    public static final String VAMPIRE = "vampire";
    public static final int MAX_HELD = 3;
    public static final long NO_MARK = Long.MIN_VALUE;
    public static final String PARCEL = "JojohaParcel";
    public static final String PARCEL_TO = "To";
    private static final String QUEST_TAG = "jojoha_quest_";
    private static final int HUNT_NAMES = 6;
    private static final int BOUNTY_NAMES = 6;
    private static final int HUNT_NEAREST = 48;
    private static final int HUNT_FARTHEST = 96;
    private static final double HUNT_HEALTH = 1.5;
    private static final int GATHER_EVERY = 40;
    private static final int HUNT_EVERY = 100;
    private static final int BREATH_EVERY = 20;
    private static final int MARK_MOVE = 8;
    private static final int AFTERMATH_DAYS = 2;
    private static final int OFFER_EVERY = 200;
    private static final double ARRIVED = 24.0;
    private static final double DEFEND_RANGE = 48.0;
    private static final double RESCUE_RANGE = 40.0;
    private static final double RESCUE_CLEAR = 64.0;
    private static final int SEEK_CHUNKS = 64;
    private static final int WANDER_NEAREST = 120;
    private static final int WANDER_FARTHEST = 220;
    private static final double OTHER_TOWN = 120.0;
    private static final double RAID_RANGE = 160.0;
    private static final int RESCUE_GUARDS = 2;
    private static final String ANCHOR_TO = "to";
    private static final String ANCHOR_SPAWNED = "spawned";

    public static final class Quest {
        private final String id;
        private String giver = HAMON;
        private Objective objective = Objective.SLAY;
        private Predicate<LivingEntity> slays = dead -> false;
        private Predicate<ItemStack> wants = stack -> false;
        private int count = 1;
        private JojoNpc.Tier tier = JojoNpc.Tier.STRANGER;
        @Nullable
        private String requires;
        @Nullable
        private WorldEvent during;
        @Nullable
        private WorldEvent after;
        private VampireEntity.Kind huntKind = VampireEntity.Kind.SIRE;
        private Quarry quarry = Quarry.VAMPIRE;
        @Nullable
        private TagKey<Structure> site;
        private boolean child;
        private Predicate<ServerPlayer> when = player -> true;
        @Nullable
        private Supplier<ItemStack> bonus;
        private BiConsumer<ServerPlayer, Entity> onClaim = (player, giver) -> {
        };
        private boolean choice;
        private int coins;
        private int trust;
        private int standing;
        private float crystal;
        private int cooldownDays = 1;

        private Quest(String id) {
            this.id = id;
        }

        Quest from(String giver) {
            this.giver = giver;
            return this;
        }

        Quest slay(Predicate<LivingEntity> slays, int count) {
            this.objective = Objective.SLAY;
            this.slays = slays;
            this.count = count;
            return this;
        }

        Quest gather(Supplier<Item> item, int count) {
            return gatherAny(stack -> stack.is(item.get()), count);
        }

        Quest gatherAny(Predicate<ItemStack> wants, int count) {
            this.objective = Objective.GATHER;
            this.wants = wants;
            this.count = count;
            return this;
        }

        Quest survive(WorldEvent event) {
            this.objective = Objective.SURVIVE;
            this.during = event;
            this.count = 1;
            return this;
        }

        Quest hunt(VampireEntity.Kind kind) {
            this.objective = Objective.HUNT;
            this.huntKind = kind;
            this.quarry = Quarry.VAMPIRE;
            this.count = 1;
            return this;
        }

        Quest huntHamon() {
            this.objective = Objective.HUNT;
            this.quarry = Quarry.HAMON;
            this.count = 1;
            return this;
        }

        Quest bounty() {
            this.objective = Objective.BOUNTY;
            this.quarry = Quarry.STAND_USER;
            this.count = 1;
            return this;
        }

        Quest objective(Objective objective, int count) {
            this.objective = objective;
            this.count = count;
            return this;
        }

        Quest site(@Nullable TagKey<Structure> site) {
            this.site = site;
            return this;
        }

        Quest child() {
            this.child = true;
            return this;
        }

        Quest when(Predicate<ServerPlayer> when) {
            this.when = when;
            return this;
        }

        Quest bonus(Supplier<ItemStack> bonus) {
            this.bonus = bonus;
            return this;
        }

        Quest onClaim(BiConsumer<ServerPlayer, Entity> onClaim) {
            this.onClaim = onClaim;
            return this;
        }

        Quest offersChoice() {
            this.choice = true;
            return this;
        }

        Quest tier(JojoNpc.Tier tier) {
            this.tier = tier;
            return this;
        }

        Quest requires(String id) {
            this.requires = id;
            return this;
        }

        Quest during(WorldEvent event) {
            this.during = event;
            return this;
        }

        Quest after(WorldEvent event) {
            this.after = event;
            return this;
        }

        Quest reward(int coins, int trust, int standing, float crystal) {
            this.coins = coins;
            this.trust = trust;
            this.standing = standing;
            this.crystal = crystal;
            return this;
        }

        Quest cooldown(int days) {
            this.cooldownDays = days;
            return this;
        }

        public String id() {
            return id;
        }

        public String giver() {
            return giver;
        }

        public Objective objective() {
            return objective;
        }

        public int count() {
            return count;
        }

        public int coins() {
            return coins;
        }

        public int trust() {
            return trust;
        }

        public int standing() {
            return standing;
        }

        public boolean choice() {
            return choice;
        }

        public boolean timely() {
            return during != null || after != null;
        }

        public boolean claimedBy(String group) {
            return giver.equals(group) || RESIDENT.equals(group) && RUMOUR.equals(giver);
        }

        public String key(String part) {
            return "quest.jojoha." + id + "." + part;
        }
    }

    private static final Map<String, Quest> ALL = new LinkedHashMap<>();

    private static TagKey<Structure> structures(String path) {
        return TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path));
    }

    private static boolean night(ServerPlayer player) {
        return player.level() instanceof ServerLevel level && WorldEvents.night(level);
    }

    @Nullable
    private static Settlement townOf(ServerPlayer player) {
        return player.level() instanceof ServerLevel level ? Settlements.at(level, player.position()) : null;
    }

    private static boolean ravaged(ServerPlayer player) {
        Settlement town = townOf(player);
        return town != null && town.ravaged();
    }

    private static boolean otherTown(ServerPlayer player) {
        Settlement town = townOf(player);
        return town != null && player.getServer() != null
                && Settlements.nearestOther(player.getServer(), town.centre(), town.id(), OTHER_TOWN) != null;
    }

    private static void rebuild(ServerPlayer player, Entity giver) {
        if (giver.level() instanceof ServerLevel level) {
            Settlements.rebuilt(level, Settlements.at(level, giver.position()));
        }
    }

    private static boolean wantsHermit(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        return !data.hasHamonPath(HamonPaths.HERMIT.id()) && !Fate.barredFromHamon(data) && !Anubis.is(data);
    }

    public static final Quest SLAY_VAMPIRES = add(new Quest("slay_vampires")
            .slay(dead -> dead instanceof VampireEntity, 3).reward(6, 10, 3, 0F));
    public static final Quest THIN_THE_DEAD = add(new Quest("thin_the_dead")
            .slay(dead -> dead.getType().is(EntityTypeTags.UNDEAD) && !(dead instanceof VampireEntity), 10)
            .reward(4, 6, 1, 0F));
    public static final Quest RATIONS = add(new Quest("rations")
            .gather(() -> Items.BREAD, 8).reward(3, 8, 1, 0F));
    public static final Quest SUN_HARVEST = add(new Quest("sun_harvest")
            .gather(() -> Items.GOLDEN_CARROT, 6).tier(JojoNpc.Tier.ACQUAINTED).reward(7, 10, 2, 0F).cooldown(2));
    public static final Quest COLD_BLOOD = add(new Quest("cold_blood")
            .slay(dead -> dead instanceof VampireEntity vampire && vampire.kind() == VampireEntity.Kind.FROST, 1)
            .tier(JojoNpc.Tier.FRIEND).reward(10, 12, 3, 0.15F).cooldown(3));
    public static final Quest HOLD_THE_LINE = add(new Quest("hold_the_line")
            .survive(WorldEvent.VAMPIRE_NIGHT).reward(8, 12, 3, 0.1F));
    public static final Quest FALLEN_STAR = add(new Quest("fallen_star")
            .gather(() -> ModItems.RAW_METEORITE_SCRAP.get(), 3).after(WorldEvent.METEOR_SHOWER)
            .tier(JojoNpc.Tier.ACQUAINTED).reward(9, 10, 2, 0.1F));
    public static final Quest NAMELESS_SIRE = add(new Quest("nameless_sire")
            .hunt(VampireEntity.Kind.SIRE).requires("slay_vampires").tier(JojoNpc.Tier.FRIEND)
            .reward(14, 15, 5, 0.3F).cooldown(5));
    public static final Quest RAID_BUTCHER = add(new Quest("raid_butcher")
            .hunt(VampireEntity.Kind.RAVAGER).after(WorldEvent.VAMPIRE_RAID).tier(JojoNpc.Tier.ACQUAINTED)
            .reward(12, 12, 4, 0.25F));
    public static final Quest PILGRIM_ESCORT = add(new Quest("pilgrim_escort")
            .objective(Objective.ESCORT, 1).when(Quests::otherTown).tier(JojoNpc.Tier.ACQUAINTED)
            .reward(8, 10, 3, 0.1F).cooldown(2));
    public static final Quest CAMP_WATCH = add(new Quest("camp_watch")
            .objective(Objective.DEFEND, 4).when(player -> night(player) && wantsHermit(player)).tier(JojoNpc.Tier.FRIEND)
            .reward(8, 12, 4, 0F).cooldown(1));
    public static final Quest HERMIT_BREATH = add(new Quest("hermit_breath")
            .objective(Objective.BREATH, 45).requires("camp_watch").when(Quests::wantsHermit).tier(JojoNpc.Tier.FRIEND)
            .bonus(() -> PathScrollItem.of(new ItemStack(ModItems.PATH_SCROLL.get()), HamonPaths.HERMIT))
            .reward(5, 20, 6, 0F).cooldown(1));

    public static final Quest TOWN_PANTRY = add(new Quest("town_pantry").from(RESIDENT)
            .gather(() -> Items.BREAD, 10).reward(4, 8, 2, 0F));
    public static final Quest PEST_CONTROL = add(new Quest("pest_control").from(RESIDENT)
            .slay(dead -> dead.getType().is(EntityTypeTags.UNDEAD) && !(dead instanceof VampireEntity), 8)
            .reward(5, 8, 2, 0F));
    public static final Quest REBUILD_TIMBER = add(new Quest("rebuild_timber").from(RESIDENT)
            .gatherAny(stack -> stack.is(ItemTags.LOGS), 32).when(Quests::ravaged).onClaim(Quests::rebuild)
            .reward(6, 12, 4, 0F).cooldown(1));
    public static final Quest REBUILD_BREAD = add(new Quest("rebuild_bread").from(RESIDENT)
            .gather(() -> Items.BREAD, 12).when(Quests::ravaged).onClaim(Quests::rebuild)
            .reward(5, 12, 4, 0F).cooldown(1));
    public static final Quest NIGHT_WATCH = add(new Quest("night_watch").from(RESIDENT)
            .objective(Objective.DEFEND, 4).when(Quests::night).tier(JojoNpc.Tier.ACQUAINTED)
            .reward(8, 10, 4, 0.1F).cooldown(2));
    public static final Quest LETTER_RUN = add(new Quest("letter_run").from(RESIDENT)
            .objective(Objective.DELIVER, 1).when(Quests::otherTown).reward(7, 10, 3, 0F).cooldown(2));
    public static final Quest STRANGER_ON_THE_ROAD = add(new Quest("stranger_on_the_road").from(RESIDENT)
            .objective(Objective.INVESTIGATE, 1).tier(JojoNpc.Tier.ACQUAINTED).reward(5, 6, 2, 0F).cooldown(3));
    public static final Quest STRANGER_WITH_A_STAND = add(new Quest("stranger_with_a_stand").from(RESIDENT)
            .objective(Objective.DUEL, 1).requires("stranger_on_the_road").tier(JojoNpc.Tier.ACQUAINTED)
            .reward(10, 12, 4, 0.25F).cooldown(3));
    public static final Quest WANTED = add(new Quest("wanted").from(RESIDENT)
            .bounty().tier(JojoNpc.Tier.ACQUAINTED).reward(14, 10, 4, 0.3F).cooldown(3));
    public static final Quest MASK_COLLECTOR = add(new Quest("mask_collector").from(RESIDENT)
            .gather(() -> ModItems.STONE_MASK.get(), 1).offersChoice().tier(JojoNpc.Tier.FRIEND)
            .reward(20, 15, 6, 0.5F).cooldown(7));
    public static final Quest STONES_FROM_THE_SKY = add(new Quest("stones_from_the_sky").from(RESIDENT)
            .gather(() -> ModItems.RAW_METEORITE_SCRAP.get(), 4).after(WorldEvent.METEOR_SHOWER)
            .bonus(() -> new ItemStack(ModItems.METEOR_MAP.get())).reward(8, 10, 3, 0F).cooldown(3));
    public static final Quest MISSING_CHILDREN = add(new Quest("missing_children").from(RESIDENT)
            .objective(Objective.INVESTIGATE, 1).site(structures("rumour_orphanage")).tier(JojoNpc.Tier.ACQUAINTED)
            .reward(6, 8, 2, 0F).cooldown(5));
    public static final Quest BRING_THEM_HOME = add(new Quest("bring_them_home").from(RESIDENT)
            .objective(Objective.RESCUE, 1).site(structures("rumour_orphanage")).child().requires("missing_children")
            .tier(JojoNpc.Tier.ACQUAINTED).reward(15, 20, 6, 0.35F).cooldown(5));
    public static final Quest TAKEN_AT_NIGHT = add(new Quest("taken_at_night").from(RESIDENT)
            .objective(Objective.RESCUE, 1).tier(JojoNpc.Tier.FRIEND).reward(12, 15, 5, 0.25F).cooldown(4));

    public static final Quest RUMOUR_CATACOMBS = add(new Quest("rumour_catacombs").from(RUMOUR)
            .objective(Objective.INVESTIGATE, 1).site(structures("rumour_catacombs")).reward(4, 4, 1, 0F).cooldown(4));
    public static final Quest RUMOUR_FARMHOUSE = add(new Quest("rumour_farmhouse").from(RUMOUR)
            .objective(Objective.INVESTIGATE, 1).site(structures("rumour_farmhouse")).reward(4, 4, 1, 0F).cooldown(4));
    public static final Quest RUMOUR_CASTLE = add(new Quest("rumour_castle").from(RUMOUR)
            .objective(Objective.INVESTIGATE, 1).site(structures("rumour_castle")).reward(4, 4, 1, 0F).cooldown(4));
    public static final Quest RUMOUR_SHACK = add(new Quest("rumour_shack").from(RUMOUR)
            .objective(Objective.INVESTIGATE, 1).site(structures("rumour_shack")).reward(4, 4, 1, 0F).cooldown(4));
    public static final Quest RUMOUR_WILLOW = add(new Quest("rumour_willow").from(RUMOUR)
            .objective(Objective.INVESTIGATE, 1).site(structures("rumour_willow")).reward(4, 4, 1, 0F).cooldown(4));
    public static final Quest RUMOUR_ORPHANAGE = add(new Quest("rumour_orphanage").from(RUMOUR)
            .objective(Objective.INVESTIGATE, 1).site(structures("rumour_orphanage")).reward(4, 4, 1, 0F).cooldown(4));

    public static final Quest FIRST_TASTE = add(new Quest("first_taste").from(VAMPIRE)
            .objective(Objective.FEED, 3).reward(5, 10, 3, 0F));
    public static final Quest HUNTERS_HUNTED = add(new Quest("hunters_hunted").from(VAMPIRE)
            .slay(dead -> dead instanceof HamonStudent, 2).requires("first_taste").reward(8, 10, 3, 0.1F).cooldown(2));
    public static final Quest SCOUT_THE_TOWN = add(new Quest("scout_the_town").from(VAMPIRE)
            .objective(Objective.INVESTIGATE, 1).site(StructureTags.VILLAGE).requires("first_taste")
            .reward(5, 8, 2, 0F).cooldown(2));
    public static final Quest LOOSE_ENDS = add(new Quest("loose_ends").from(VAMPIRE)
            .slay(dead -> dead instanceof Villager && dead.level() instanceof ServerLevel level && WorldEvents.night(level), 2)
            .tier(JojoNpc.Tier.ACQUAINTED).reward(7, 8, 3, 0F).cooldown(2));
    public static final Quest NIGHT_FEAST = add(new Quest("night_feast").from(VAMPIRE)
            .objective(Objective.FEED, 6).during(WorldEvent.VAMPIRE_NIGHT).reward(10, 12, 4, 0.15F));
    public static final Quest HAMON_MASTER = add(new Quest("hamon_master").from(VAMPIRE)
            .huntHamon().requires("hunters_hunted").tier(JojoNpc.Tier.FRIEND).reward(14, 14, 5, 0.3F).cooldown(4));
    public static final Quest SIRES_RAID = add(new Quest("sires_raid").from(VAMPIRE)
            .objective(Objective.RAID, 1).requires("scout_the_town").tier(JojoNpc.Tier.FRIEND)
            .when(player -> night(player) && player.getServer() != null && WorldEvents.get(player.getServer()).active() == null)
            .reward(20, 20, 8, 0.5F).cooldown(7));

    private static Quest add(Quest quest) {
        ALL.put(quest.id(), quest);
        return quest;
    }

    @Nullable
    public static Quest byId(String id) {
        return ALL.get(id);
    }

    @Nullable
    public static Quest fromLine(String text, String part) {
        String prefix = "quest.jojoha.";
        String suffix = "." + part;
        if (!text.startsWith(prefix) || !text.endsWith(suffix)) {
            return null;
        }
        return ALL.get(text.substring(prefix.length(), text.length() - suffix.length()));
    }

    private static final String NAME = "jojoha_quests";
    private static final Factory<Quests> FACTORY = new Factory<>(Quests::new, Quests::load, null);

    private final Map<UUID, Map<String, Integer>> progress = new HashMap<>();
    private final Map<UUID, Map<String, Long>> finished = new HashMap<>();
    private final Map<UUID, Map<String, UUID>> targets = new HashMap<>();
    private final Map<UUID, Map<String, Long>> marks = new HashMap<>();
    private final Map<UUID, Map<String, Long>> anchors = new HashMap<>();
    private final Map<String, Long> eventEnded = new HashMap<>();
    private final Map<UUID, List<String>> offering = new HashMap<>();

    private Quests() {
    }

    public static void init() {
        PlayerEvent.PLAYER_JOIN.register(Quests::sync);
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                fell(player);
            }
            ServerPlayer killer = source.getEntity() instanceof ServerPlayer player ? player : null;
            if (killer != null) {
                slain(killer, entity);
            }
            quarryDied(entity, killer);
            return EventResult.pass();
        });
        TickEvent.SERVER_POST.register(Quests::tick);
    }

    public static Quests get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static long day(MinecraftServer server) {
        return server.overworld().getDayTime() / WorldEvents.DAY;
    }

    private Map<String, Integer> held(UUID player) {
        return progress.getOrDefault(player, Map.of());
    }

    private static String tagFor(ServerPlayer player, Quest quest) {
        return QUEST_TAG + quest.id + "_" + player.getUUID();
    }

    private static boolean tagged(Entity entity, ServerPlayer player, Quest quest) {
        return entity.getTags().contains(tagFor(player, quest));
    }

    private static void slain(ServerPlayer player, LivingEntity dead) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Quests quests = get(server);
        Map<String, Integer> held = quests.progress.get(player.getUUID());
        if (held == null) {
            return;
        }
        boolean changed = false;
        for (Map.Entry<String, Integer> entry : held.entrySet()) {
            Quest quest = ALL.get(entry.getKey());
            if (quest == null || entry.getValue() >= quest.count) {
                continue;
            }
            boolean counts = quest.objective == Objective.SLAY && quest.slays.test(dead)
                    || (quest.objective == Objective.DEFEND || quest.objective == Objective.DUEL) && tagged(dead, player, quest);
            if (!counts) {
                continue;
            }
            entry.setValue(quest.objective == Objective.DUEL ? quest.count : entry.getValue() + 1);
            changed = true;
            announce(player, quest, entry.getValue());
        }
        if (changed) {
            quests.setDirty();
            sync(player);
        }
    }

    private static void quarryDied(LivingEntity dead, @Nullable ServerPlayer killer) {
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        Quests quests = get(level.getServer());
        for (Map.Entry<UUID, Map<String, UUID>> owner : quests.targets.entrySet()) {
            Iterator<Map.Entry<String, UUID>> it = owner.getValue().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, UUID> hunt = it.next();
                if (!hunt.getValue().equals(dead.getUUID())) {
                    continue;
                }
                it.remove();
                quests.setDirty();
                ServerPlayer holder = level.getServer().getPlayerList().getPlayer(owner.getKey());
                Quest quest = ALL.get(hunt.getKey());
                if (holder == null || quest == null) {
                    continue;
                }
                if (quest.objective == Objective.ESCORT || quest.objective == Objective.RESCUE) {
                    quests.fail(holder, quest);
                } else if (killer == holder) {
                    quests.progress.computeIfAbsent(holder.getUUID(), id -> new HashMap<>()).put(quest.id, 1);
                    quests.marks.getOrDefault(holder.getUUID(), new HashMap<>()).remove(quest.id);
                    announce(holder, quest, 1);
                } else {
                    holder.sendSystemMessage(Component.translatable("quest.jojoha.hunt.escaped", dead.getDisplayName())
                            .withStyle(ChatFormatting.GRAY));
                }
                sync(holder);
            }
        }
    }

    private void fail(ServerPlayer player, Quest quest) {
        progress.getOrDefault(player.getUUID(), new HashMap<>()).remove(quest.id);
        targets.getOrDefault(player.getUUID(), new HashMap<>()).remove(quest.id);
        marks.getOrDefault(player.getUUID(), new HashMap<>()).remove(quest.id);
        anchors.getOrDefault(player.getUUID(), new HashMap<>()).remove(quest.id + "/" + ANCHOR_TO);
        anchors.getOrDefault(player.getUUID(), new HashMap<>()).remove(quest.id + "/" + ANCHOR_SPAWNED);
        setDirty();
        player.sendSystemMessage(Component.translatable("quest.jojoha.failed", Component.translatable(quest.key("name")))
                .withStyle(ChatFormatting.RED));
        sync(player);
    }

    private static void fell(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Quests quests = get(server);
        Map<String, Integer> held = quests.progress.get(player.getUUID());
        if (held == null) {
            return;
        }
        Iterator<Map.Entry<String, Integer>> it = held.entrySet().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            Quest quest = ALL.get(entry.getKey());
            if (quest != null && quest.objective == Objective.SURVIVE && entry.getValue() < quest.count
                    && WorldEvents.active(server, quest.during)) {
                it.remove();
                changed = true;
                player.sendSystemMessage(Component.translatable(quest.key("failed")).withStyle(ChatFormatting.RED));
            }
        }
        if (changed) {
            quests.setDirty();
            sync(player);
        }
    }

    public static void eventEnded(MinecraftServer server, WorldEvent event) {
        Quests quests = get(server);
        quests.eventEnded.put(event.id(), day(server));
        quests.setDirty();
        if (event == WorldEvent.METEOR_SHOWER) {
            Settlements.meteorsFell(server);
        } else if (event == WorldEvent.VAMPIRE_NIGHT) {
            Settlements.report(server, "town.jojoha.news.vampire_night");
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Map<String, Integer> held = quests.progress.get(player.getUUID());
            if (held == null || !player.isAlive()) {
                continue;
            }
            for (Map.Entry<String, Integer> entry : held.entrySet()) {
                Quest quest = ALL.get(entry.getKey());
                if (quest != null && quest.objective == Objective.SURVIVE && quest.during == event
                        && entry.getValue() < quest.count) {
                    entry.setValue(quest.count);
                    announce(player, quest, quest.count);
                    sync(player);
                }
            }
        }
    }

    private static void bump(ServerPlayer player, Objective objective, int amount) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Quests quests = get(server);
        Map<String, Integer> held = quests.progress.get(player.getUUID());
        if (held == null) {
            return;
        }
        boolean changed = false;
        for (Map.Entry<String, Integer> entry : held.entrySet()) {
            Quest quest = ALL.get(entry.getKey());
            if (quest == null || quest.objective != objective || entry.getValue() >= quest.count) {
                continue;
            }
            entry.setValue(Math.min(quest.count, entry.getValue() + amount));
            changed = true;
            if (objective != Objective.BREATH || entry.getValue() >= quest.count || entry.getValue() % 15 == 0) {
                announce(player, quest, entry.getValue());
            }
        }
        if (changed) {
            quests.setDirty();
            sync(player);
        }
    }

    public static void fed(ServerPlayer player, LivingEntity prey) {
        if (prey instanceof Villager || prey instanceof Player || prey instanceof JojoNpc) {
            bump(player, Objective.FEED, 1);
        }
    }

    public static void duelWon(ServerPlayer player, StandUserNpc duelist) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        for (String id : List.copyOf(get(server).held(player.getUUID()).keySet())) {
            Quest quest = ALL.get(id);
            if (quest != null && quest.objective == Objective.DUEL && tagged(duelist, player, quest)) {
                get(server).progress.get(player.getUUID()).put(id, quest.count);
                get(server).setDirty();
                announce(player, quest, quest.count);
                sync(player);
            }
        }
    }

    public static void duelLost(ServerPlayer player, StandUserNpc duelist) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Quests quests = get(server);
        for (String id : List.copyOf(quests.held(player.getUUID()).keySet())) {
            Quest quest = ALL.get(id);
            if (quest != null && quest.objective == Objective.DUEL && tagged(duelist, player, quest)
                    && quests.held(player.getUUID()).get(id) < quest.count) {
                quests.fail(player, quest);
            }
        }
    }

    public static void raidEnded(ServerLevel level, BlockPos bell, boolean townLost) {
        Quests quests = get(level.getServer());
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(Vec3.atCenterOf(bell)) > RAID_RANGE * RAID_RANGE) {
                continue;
            }
            for (String id : List.copyOf(quests.held(player.getUUID()).keySet())) {
                Quest quest = ALL.get(id);
                if (quest == null || quest.objective != Objective.RAID || quests.held(player.getUUID()).get(id) >= quest.count) {
                    continue;
                }
                if (townLost) {
                    quests.progress.get(player.getUUID()).put(id, quest.count);
                    quests.setDirty();
                    announce(player, quest, quest.count);
                    sync(player);
                } else {
                    quests.fail(player, quest);
                }
            }
        }
    }

    @Nullable
    public static Quest delivered(ServerPlayer player, Settlement town) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }
        Quests quests = get(server);
        for (String id : List.copyOf(quests.held(player.getUUID()).keySet())) {
            Quest quest = ALL.get(id);
            Long to = quests.anchor(player, quest, ANCHOR_TO);
            if (quest == null || quest.objective != Objective.DELIVER || to == null || to != town.id()) {
                continue;
            }
            for (ItemStack stack : player.getInventory().items) {
                CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                if (data != null && id.equals(data.copyTag().getString(PARCEL)) && data.copyTag().getInt(PARCEL_TO) == town.id()) {
                    stack.shrink(1);
                    quests.progress.get(player.getUUID()).put(id, quest.count);
                    quests.setDirty();
                    sync(player);
                    return quest;
                }
            }
        }
        return null;
    }

    private static void announce(ServerPlayer player, Quest quest, int done) {
        boolean ready = done >= quest.count;
        player.displayClientMessage(Component.translatable(ready ? "quest.jojoha.ready" : "quest.jojoha.progress",
                Component.translatable(quest.key("name")), done, quest.count)
                .withStyle(ready ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
        if (ready) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS,
                    0.6F, 1.4F);
        }
    }

    @Nullable
    private Long anchor(ServerPlayer player, @Nullable Quest quest, String what) {
        return quest == null ? null : anchors.getOrDefault(player.getUUID(), Map.of()).get(quest.id + "/" + what);
    }

    private void setAnchor(ServerPlayer player, Quest quest, String what, long value) {
        anchors.computeIfAbsent(player.getUUID(), id -> new HashMap<>()).put(quest.id + "/" + what, value);
        setDirty();
    }

    private static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        if (now % OFFER_EVERY == 0) {
            Quests quests = get(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!offers(player).equals(quests.offering.get(player.getUUID()))) {
                    sync(player);
                }
            }
        }
        boolean gather = now % GATHER_EVERY == 0;
        boolean hunt = now % HUNT_EVERY == 0;
        boolean breath = now % BREATH_EVERY == 0;
        if (!gather && !hunt && !breath) {
            return;
        }
        Quests quests = get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Map<String, Integer> held = quests.progress.get(player.getUUID());
            if (held == null || held.isEmpty()) {
                continue;
            }
            if (breath && player.isUnderWater() && player.getAirSupply() < player.getMaxAirSupply()) {
                bump(player, Objective.BREATH, 1);
            }
            if (!gather && !hunt) {
                continue;
            }
            boolean changed = false;
            for (Map.Entry<String, Integer> entry : List.copyOf(held.entrySet())) {
                Quest quest = ALL.get(entry.getKey());
                if (quest == null || !held.containsKey(entry.getKey())) {
                    continue;
                }
                if (gather && quest.objective == Objective.GATHER) {
                    int have = Math.min(quest.count, countHeld(player, quest.wants));
                    if (have != entry.getValue()) {
                        if (have >= quest.count) {
                            announce(player, quest, have);
                        }
                        held.put(entry.getKey(), have);
                        changed = true;
                    }
                }
                if (gather && entry.getValue() < quest.count) {
                    changed |= quests.travel(player, quest);
                }
                if (hunt && (quest.objective == Objective.HUNT || quest.objective == Objective.BOUNTY)
                        && entry.getValue() < quest.count) {
                    changed |= quests.stalk(player, quest);
                }
            }
            if (changed) {
                quests.setDirty();
                sync(player);
            }
        }
    }

    private boolean travel(ServerPlayer player, Quest quest) {
        if (!(player.level() instanceof ServerLevel level) || level != level.getServer().overworld()) {
            return false;
        }
        Long spot = marks.getOrDefault(player.getUUID(), Map.of()).get(quest.id);
        if (spot == null) {
            return false;
        }
        BlockPos mark = BlockPos.of(spot);
        double flat = Math.hypot(player.getX() - mark.getX() - 0.5, player.getZ() - mark.getZ() - 0.5);
        switch (quest.objective) {
            case INVESTIGATE -> {
                if (flat <= ARRIVED) {
                    finish(player, quest);
                    return true;
                }
            }
            case ESCORT -> {
                UUID who = targets.getOrDefault(player.getUUID(), Map.of()).get(quest.id);
                Entity escortee = who == null ? null : level.getEntity(who);
                if (escortee instanceof JojoNpc npc && Math.hypot(npc.getX() - mark.getX(), npc.getZ() - mark.getZ()) <= ARRIVED) {
                    npc.release();
                    npc.setHome(null);
                    targets.get(player.getUUID()).remove(quest.id);
                    npc.say("quest.jojoha.pilgrim_escort.arrived");
                    finish(player, quest);
                    return true;
                }
            }
            case DEFEND -> {
                if (anchor(player, quest, ANCHOR_SPAWNED) == null && WorldEvents.night(level) && flat <= DEFEND_RANGE) {
                    for (int i = 0; i < quest.count; i++) {
                        VampireEntity vampire = VampireNight.spawnAround(level, ModRegistries.VAMPIRE.get(), mark, 18, 30, false);
                        if (vampire != null) {
                            vampire.addTag(tagFor(player, quest));
                            vampire.setPersistenceRequired();
                            vampire.setTarget(player);
                        }
                    }
                    setAnchor(player, quest, ANCHOR_SPAWNED, 1L);
                    player.sendSystemMessage(Component.translatable("quest.jojoha.defend.coming").withStyle(ChatFormatting.DARK_RED));
                    return true;
                }
            }
            case RESCUE -> {
                return rescue(level, player, quest, mark, flat);
            }
            default -> {
            }
        }
        return false;
    }

    private boolean rescue(ServerLevel level, ServerPlayer player, Quest quest, BlockPos mark, double flat) {
        if (anchor(player, quest, ANCHOR_SPAWNED) == null) {
            if (flat > RESCUE_RANGE) {
                return false;
            }
            Villager captive = VampireNight.spawnAround(level, EntityType.VILLAGER, mark, 0, 6, false);
            if (captive == null) {
                return false;
            }
            if (quest.child) {
                captive.setAge(-24000);
            }
            captive.setCustomName(Component.translatable(quest.child ? "quest.jojoha.rescue.child" : "quest.jojoha.rescue.captive"));
            captive.setPersistenceRequired();
            targets.computeIfAbsent(player.getUUID(), id -> new HashMap<>()).put(quest.id, captive.getUUID());
            VampireEntity sire = VampireNight.spawnAround(level, ModRegistries.VAMPIRE.get(), mark, 2, 8, false);
            if (sire != null) {
                sire.huntKind(VampireEntity.Kind.SIRE);
                sire.addTag(tagFor(player, quest));
                sire.setPersistenceRequired();
            }
            for (int i = 0; i < RESCUE_GUARDS; i++) {
                VampireEntity guard = VampireNight.spawnAround(level, ModRegistries.VAMPIRE.get(), mark, 3, 10, false);
                if (guard != null) {
                    guard.addTag(tagFor(player, quest));
                    guard.setPersistenceRequired();
                }
            }
            setAnchor(player, quest, ANCHOR_SPAWNED, 1L);
            player.sendSystemMessage(Component.translatable("quest.jojoha.rescue.found").withStyle(ChatFormatting.DARK_RED));
            return true;
        }
        if (flat > RESCUE_CLEAR) {
            return false;
        }
        boolean guarded = !level.getEntitiesOfClass(VampireEntity.class, new AABB(mark).inflate(RESCUE_CLEAR),
                vampire -> vampire.isAlive() && tagged(vampire, player, quest)).isEmpty();
        UUID who = targets.getOrDefault(player.getUUID(), Map.of()).get(quest.id);
        if (guarded || who == null || !(level.getEntity(who) instanceof Villager captive) || !captive.isAlive()) {
            return false;
        }
        captive.setCustomName(null);
        targets.get(player.getUUID()).remove(quest.id);
        finish(player, quest);
        return true;
    }

    private void finish(ServerPlayer player, Quest quest) {
        progress.computeIfAbsent(player.getUUID(), id -> new HashMap<>()).put(quest.id, quest.count);
        marks.getOrDefault(player.getUUID(), new HashMap<>()).remove(quest.id);
        setDirty();
        announce(player, quest, quest.count);
    }

    private boolean stalk(ServerPlayer player, Quest quest) {
        if (!(player.level() instanceof ServerLevel level) || level != player.getServer().overworld()) {
            return false;
        }
        Map<String, UUID> mine = targets.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        Map<String, Long> spots = marks.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        UUID target = mine.get(quest.id);
        if (target != null) {
            Entity body = level.getEntity(target);
            if (body == null) {
                return false;
            }
            long spot = body.blockPosition().asLong();
            Long old = spots.get(quest.id);
            if (old == null || BlockPos.of(old).distManhattan(body.blockPosition()) > MARK_MOVE) {
                spots.put(quest.id, spot);
                return true;
            }
            return false;
        }
        Mob quarry = switch (quest.quarry) {
            case VAMPIRE -> {
                VampireEntity vampire = VampireNight.spawnAround(level, ModRegistries.VAMPIRE.get(), player.blockPosition(),
                        HUNT_NEAREST, HUNT_FARTHEST, false);
                if (vampire != null) {
                    vampire.huntKind(quest.huntKind);
                    vampire.setCustomName(Component.translatable("quest.jojoha.hunt.name." + level.random.nextInt(HUNT_NAMES)));
                }
                yield vampire;
            }
            case HAMON -> {
                HamonStudent master = VampireNight.spawnAround(level, ModRegistries.HAMON_STUDENT.get(), player.blockPosition(),
                        HUNT_NEAREST, HUNT_FARTHEST, false);
                if (master != null) {
                    master.setCustomName(Component.translatable("quest.jojoha.hamon_master.title"));
                    master.settle();
                }
                yield master;
            }
            case STAND_USER -> {
                StandUserNpc outlaw = VampireNight.spawnAround(level, ModRegistries.STAND_USER.get(), player.blockPosition(),
                        HUNT_NEAREST, HUNT_FARTHEST, false);
                if (outlaw != null) {
                    outlaw.bounty(Component.translatable("quest.jojoha.wanted.name." + level.random.nextInt(BOUNTY_NAMES)));
                }
                yield outlaw;
            }
        };
        if (quarry == null) {
            return false;
        }
        quarry.getAttribute(Attributes.MAX_HEALTH).setBaseValue(quarry.getMaxHealth() * HUNT_HEALTH);
        quarry.setHealth(quarry.getMaxHealth());
        quarry.setCustomNameVisible(true);
        quarry.setPersistenceRequired();
        mine.put(quest.id, quarry.getUUID());
        spots.put(quest.id, quarry.blockPosition().asLong());
        player.sendSystemMessage(Component.translatable("quest.jojoha.hunt.near", quarry.getDisplayName())
                .withStyle(ChatFormatting.DARK_RED));
        return true;
    }

    private static int countHeld(ServerPlayer player, Predicate<ItemStack> wants) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (wants.test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static List<String> offers(ServerPlayer player) {
        List<String> givers = new ArrayList<>();
        JojoNpc.Tier hamon = NpcStanding.hamon(player) >= NpcStanding.FRIENDLY ? JojoNpc.Tier.FRIEND : JojoNpc.Tier.STRANGER;
        if (!pool(player, HAMON, hamon).isEmpty()) {
            givers.add(HAMON);
        }
        return givers;
    }

    public static void sync(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Quests quests = get(server);
        List<String> ids = new ArrayList<>();
        List<Integer> done = new ArrayList<>();
        List<Integer> needed = new ArrayList<>();
        List<Long> spots = new ArrayList<>();
        Map<String, Integer> held = quests.held(player.getUUID());
        Map<String, Long> mine = quests.marks.getOrDefault(player.getUUID(), Map.of());
        for (Quest quest : ALL.values()) {
            Integer count = held.get(quest.id);
            if (count != null) {
                ids.add(quest.id);
                done.add(Math.min(count, quest.count));
                needed.add(quest.count);
                spots.add(count < quest.count ? mine.getOrDefault(quest.id, NO_MARK) : NO_MARK);
            }
        }
        List<String> givers = offers(player);
        quests.offering.put(player.getUUID(), givers);
        NetworkManager.sendToPlayer(player, new QuestLogPacket(ids, done, needed, spots, givers));
    }

    @Nullable
    public static Integer progress(ServerPlayer player, Quest quest) {
        MinecraftServer server = player.getServer();
        return server == null ? null : get(server).held(player.getUUID()).get(quest.id);
    }

    public static boolean ready(ServerPlayer player, Quest quest) {
        Integer done = progress(player, quest);
        if (done == null) {
            return false;
        }
        if (quest.objective == Objective.GATHER) {
            return countHeld(player, quest.wants) >= quest.count;
        }
        return done >= quest.count;
    }

    public static List<Quest> heldFrom(ServerPlayer player, String giver) {
        List<Quest> list = new ArrayList<>();
        MinecraftServer server = player.getServer();
        if (server == null) {
            return list;
        }
        for (String id : get(server).held(player.getUUID()).keySet()) {
            Quest quest = ALL.get(id);
            if (quest != null && quest.claimedBy(giver)) {
                list.add(quest);
            }
        }
        return list;
    }

    @Nullable
    public static Quest offer(ServerPlayer player, JojoNpc npc, String giver, JojoNpc.Tier tier) {
        return offer(player, npc.getUUID().getLeastSignificantBits(), giver, tier);
    }

    @Nullable
    public static Quest offer(ServerPlayer player, long seed, String giver, JojoNpc.Tier tier) {
        List<Quest> pool = pool(player, giver, tier);
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get((int) Math.floorMod(seed(player, seed), (long) pool.size()));
    }

    @Nullable
    public static Quest nextOffer(ServerPlayer player, String giver, JojoNpc.Tier tier, Quest after) {
        List<Quest> pool = pool(player, giver, tier);
        if (pool.size() < 2) {
            return null;
        }
        int at = pool.indexOf(after);
        return pool.get((at + 1) % pool.size());
    }

    public static int poolSize(ServerPlayer player, String giver, JojoNpc.Tier tier) {
        return pool(player, giver, tier).size();
    }

    private static long seed(ServerPlayer player, long seed) {
        MinecraftServer server = player.getServer();
        long today = server == null ? 0L : day(server);
        return seed ^ (today * 31L) ^ player.getUUID().getMostSignificantBits();
    }

    private static List<Quest> pool(ServerPlayer player, String giver, JojoNpc.Tier tier) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return List.of();
        }
        Quests quests = get(server);
        Map<String, Integer> held = quests.held(player.getUUID());
        if (held.size() >= MAX_HELD) {
            return List.of();
        }
        long today = day(server);
        Map<String, Long> done = quests.finished.getOrDefault(player.getUUID(), Map.of());
        List<Quest> timely = new ArrayList<>();
        List<Quest> usual = new ArrayList<>();
        for (Quest quest : ALL.values()) {
            if (!quest.giver.equals(giver) || held.containsKey(quest.id) || tier.ordinal() < quest.tier.ordinal()) {
                continue;
            }
            if (quest.requires != null && !done.containsKey(quest.requires)) {
                continue;
            }
            Long last = done.get(quest.id);
            if (last != null && today - last < quest.cooldownDays) {
                continue;
            }
            if (quest.during != null && !WorldEvents.active(server, quest.during)) {
                continue;
            }
            if (quest.after != null) {
                Long ended = quests.eventEnded.get(quest.after.id());
                if (ended == null || today - ended > AFTERMATH_DAYS) {
                    continue;
                }
            }
            if (!quest.when.test(player)) {
                continue;
            }
            (quest.timely() ? timely : usual).add(quest);
        }
        List<Quest> pool = new ArrayList<>(timely);
        pool.addAll(usual);
        return pool;
    }

    public static void take(ServerPlayer player, Quest quest) {
        take(player, quest, null);
    }

    public static boolean take(ServerPlayer player, Quest quest, @Nullable Entity giver) {
        MinecraftServer server = player.getServer();
        if (server == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        Quests quests = get(server);
        Map<String, Integer> held = quests.progress.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        if (held.size() >= MAX_HELD || held.containsKey(quest.id)) {
            return false;
        }
        if (!quests.prepare(level, player, quest, giver)) {
            return false;
        }
        int start = quest.objective == Objective.GATHER ? Math.min(quest.count, countHeld(player, quest.wants)) : 0;
        held.putIfAbsent(quest.id, start);
        quests.setDirty();
        if (quest.objective == Objective.HUNT || quest.objective == Objective.BOUNTY) {
            quests.stalk(player, quest);
        }
        NetworkManager.sendToPlayer(player, new QuestBannerPacket(quest.id, false, List.of(), List.of(), 0, 0));
        sync(player);
        return true;
    }

    private boolean prepare(ServerLevel level, ServerPlayer player, Quest quest, @Nullable Entity giver) {
        BlockPos from = giver != null ? giver.blockPosition() : player.blockPosition();
        Map<String, Long> spots = marks.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        switch (quest.objective) {
            case DELIVER, ESCORT -> {
                Settlement origin = Settlements.at(level, Vec3.atCenterOf(from));
                Settlement to = origin == null ? null
                        : Settlements.nearestOther(level.getServer(), origin.centre(), origin.id(), OTHER_TOWN);
                if (to == null) {
                    return false;
                }
                spots.put(quest.id, to.centre().asLong());
                setAnchor(player, quest, ANCHOR_TO, to.id());
                if (quest.objective == Objective.DELIVER) {
                    ItemStack parcel = new ItemStack(Items.PAPER);
                    CompoundTag tag = new CompoundTag();
                    tag.putString(PARCEL, quest.id);
                    tag.putInt(PARCEL_TO, to.id());
                    parcel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    parcel.set(DataComponents.CUSTOM_NAME, Component.translatable("quest.jojoha.parcel", to.name()));
                    if (!player.getInventory().add(parcel)) {
                        player.drop(parcel, false);
                    }
                } else {
                    HamonStudent pilgrim = giver instanceof HamonStudent student && student.pilgrim() ? student
                            : VampireNight.spawnAround(level, ModRegistries.HAMON_STUDENT.get(), player.blockPosition(), 1, 3, false);
                    if (pilgrim == null) {
                        return false;
                    }
                    pilgrim.makePilgrim();
                    pilgrim.escort(player);
                    targets.computeIfAbsent(player.getUUID(), id -> new HashMap<>()).put(quest.id, pilgrim.getUUID());
                }
            }
            case INVESTIGATE, RESCUE -> {
                BlockPos site = null;
                if (quest.site != null) {
                    site = StructureSeek.nearest(level, quest.site, player.blockPosition(), SEEK_CHUNKS);
                }
                if (site == null && quest.site == null) {
                    site = wander(level, player.blockPosition());
                }
                if (site == null) {
                    player.sendSystemMessage(Component.translatable("quest.jojoha.nowhere").withStyle(ChatFormatting.GRAY));
                    return false;
                }
                spots.put(quest.id, site.asLong());
            }
            case DEFEND -> spots.put(quest.id, from.asLong());
            case DUEL -> {
                StandUserNpc duelist = VampireNight.spawnAround(level, ModRegistries.STAND_USER.get(), player.blockPosition(), 10, 18, false);
                if (duelist == null) {
                    return false;
                }
                duelist.honourable();
                duelist.addTag(tagFor(player, quest));
                duelist.setPersistenceRequired();
                duelist.challenge(player);
            }
            case RAID -> {
                if (!WorldEvents.get(level.getServer()).start(level.getServer(), WorldEvent.VAMPIRE_RAID)) {
                    return false;
                }
                BlockPos bell = WorldEvents.get(level.getServer()).focus();
                if (bell != null) {
                    spots.put(quest.id, bell.asLong());
                }
            }
            default -> {
            }
        }
        return true;
    }

    private static BlockPos wander(ServerLevel level, BlockPos from) {
        double angle = level.random.nextDouble() * Math.PI * 2.0;
        int distance = Mth.nextInt(level.random, WANDER_NEAREST, WANDER_FARTHEST);
        int x = from.getX() + (int) (Math.cos(angle) * distance);
        int z = from.getZ() + (int) (Math.sin(angle) * distance);
        return new BlockPos(x, from.getY(), z);
    }

    public static boolean claim(ServerPlayer player, Quest quest, Entity giver) {
        MinecraftServer server = player.getServer();
        if (server == null || !ready(player, quest)) {
            return false;
        }
        if (quest.objective == Objective.GATHER) {
            int owed = quest.count;
            for (ItemStack stack : player.getInventory().items) {
                if (owed > 0 && quest.wants.test(stack)) {
                    int take = Math.min(owed, stack.getCount());
                    stack.shrink(take);
                    owed -= take;
                }
            }
        }
        Quests quests = get(server);
        quests.forgetQuest(player, quest);
        quests.finished.computeIfAbsent(player.getUUID(), id -> new HashMap<>()).put(quest.id, day(server));
        quests.setDirty();
        List<ItemStack> rewards = new ArrayList<>();
        rewards.add(new ItemStack(ModItems.JOJO_COIN.get(), quest.coins));
        if (quest.crystal > 0F && player.getRandom().nextFloat() < quest.crystal) {
            rewards.add(new ItemStack(ModItems.STAT_CRYSTAL.get()));
        }
        if (quest.bonus != null) {
            rewards.add(quest.bonus.get());
        }
        List<String> items = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (ItemStack stack : rewards) {
            items.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            counts.add(stack.getCount());
            give(player, stack);
        }
        NpcMemory.addTrust(giver, player, quest.trust);
        NetworkManager.sendToPlayer(player, new QuestBannerPacket(quest.id, true, items, counts, quest.trust, quest.standing));
        if (quest.standing != 0) {
            switch (quest.giver) {
                case HAMON -> NpcStanding.addHamon(server, player, quest.standing, true);
                case VAMPIRE -> NpcStanding.addVampire(server, player, quest.standing, true);
                default -> {
                    if (giver.level() instanceof ServerLevel level) {
                        Settlements.addReputation(player, Settlements.at(level, giver.position()), quest.standing * 2);
                    }
                }
            }
        }
        quest.onClaim.accept(player, giver);
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.PLAYERS, 0.5F, 1.2F);
        sync(player);
        return true;
    }

    public static boolean keep(ServerPlayer player, Quest quest) {
        MinecraftServer server = player.getServer();
        if (server == null || !ready(player, quest)) {
            return false;
        }
        Quests quests = get(server);
        quests.forgetQuest(player, quest);
        quests.finished.computeIfAbsent(player.getUUID(), id -> new HashMap<>()).put(quest.id, day(server));
        quests.setDirty();
        sync(player);
        return true;
    }

    private void forgetQuest(ServerPlayer player, Quest quest) {
        progress.getOrDefault(player.getUUID(), new HashMap<>()).remove(quest.id);
        targets.getOrDefault(player.getUUID(), new HashMap<>()).remove(quest.id);
        marks.getOrDefault(player.getUUID(), new HashMap<>()).remove(quest.id);
        anchors.getOrDefault(player.getUUID(), new HashMap<>()).keySet().removeIf(key -> key.startsWith(quest.id + "/"));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static CompoundTag ints(Map<UUID, Map<String, Integer>> map) {
        CompoundTag players = new CompoundTag();
        map.forEach((id, held) -> {
            CompoundTag tag = new CompoundTag();
            held.forEach(tag::putInt);
            players.put(id.toString(), tag);
        });
        return players;
    }

    private static CompoundTag longs(Map<UUID, Map<String, Long>> map) {
        CompoundTag players = new CompoundTag();
        map.forEach((id, held) -> {
            CompoundTag tag = new CompoundTag();
            held.forEach(tag::putLong);
            players.put(id.toString(), tag);
        });
        return players;
    }

    private static Quests load(CompoundTag tag, HolderLookup.Provider registries) {
        Quests quests = new Quests();
        CompoundTag players = tag.getCompound("Players");
        for (String id : players.getAllKeys()) {
            CompoundTag held = players.getCompound(id);
            Map<String, Integer> map = new HashMap<>();
            for (String quest : held.getAllKeys()) {
                map.put(quest, held.getInt(quest));
            }
            quests.progress.put(UUID.fromString(id), map);
        }
        readLongs(tag.getCompound("Finished"), quests.finished);
        readLongs(tag.getCompound("Marks"), quests.marks);
        readLongs(tag.getCompound("Anchors"), quests.anchors);
        CompoundTag hunts = tag.getCompound("Targets");
        for (String id : hunts.getAllKeys()) {
            CompoundTag held = hunts.getCompound(id);
            Map<String, UUID> map = new HashMap<>();
            for (String quest : held.getAllKeys()) {
                map.put(quest, held.getUUID(quest));
            }
            quests.targets.put(UUID.fromString(id), map);
        }
        CompoundTag ended = tag.getCompound("EventEnded");
        for (String event : ended.getAllKeys()) {
            quests.eventEnded.put(event, ended.getLong(event));
        }
        return quests;
    }

    private static void readLongs(CompoundTag tag, Map<UUID, Map<String, Long>> into) {
        for (String id : tag.getAllKeys()) {
            CompoundTag held = tag.getCompound(id);
            Map<String, Long> map = new HashMap<>();
            for (String quest : held.getAllKeys()) {
                map.put(quest, held.getLong(quest));
            }
            into.put(UUID.fromString(id), map);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Players", ints(progress));
        tag.put("Finished", longs(finished));
        tag.put("Marks", longs(marks));
        tag.put("Anchors", longs(anchors));
        CompoundTag hunts = new CompoundTag();
        targets.forEach((id, held) -> {
            CompoundTag inner = new CompoundTag();
            held.forEach(inner::putUUID);
            hunts.put(id.toString(), inner);
        });
        tag.put("Targets", hunts);
        CompoundTag ended = new CompoundTag();
        eventEnded.forEach(ended::putLong);
        tag.put("EventEnded", ended);
        return tag;
    }
}
