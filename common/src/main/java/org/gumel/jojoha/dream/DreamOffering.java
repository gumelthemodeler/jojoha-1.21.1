package org.gumel.jojoha.dream;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StatGrowth;
import org.gumel.jojoha.data.StatPerk;
import org.gumel.jojoha.data.StatPoints;
import org.gumel.jojoha.fortune.Devout;
import org.gumel.jojoha.fortune.Fortune;
import org.gumel.jojoha.fortune.Fortunes;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModTraits;
import org.gumel.jojoha.skilltree.SkillNode;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.BondRank;
import org.gumel.jojoha.stand.Spirit;
import org.gumel.jojoha.stand.StandRelease;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.anubis.Anubis;
import org.gumel.jojoha.trait.Traits;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DreamOffering {
    private static final ResourceLocation GIFT =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "dream_gift");

    private static final int FALLBACK_DIAMONDS = 2;

    private static final int WORTH_PER_DRAW = 24;
    private static final int WORTH_PER_TRY = 30;
    private static final int MOST_TRIES = 4;
    private static final int MOST_TOP_UPS = 6;

    private static final float MOST_RARE_ODDS = 7F;

    public static final float ALIGN_LEAST_ODDS = 0.5F;

    public static final float ALIGN_MOST_ODDS = 0.9F;

    public enum Wish {
        TRINKET("trinket", 12, 120, false),

        FATE("fate", 35, 140, false),

        SELF("self", 4, 32, false),

        STAND("stand", 4, 32, true),

        HOLLOW("hollow", 100, 100, true),

        ALIGN("align", 120, 240, false, true),

        FORTUNE("fortune", 40, 40, false),

        FORGET("forget", 40, 40, false),

        NIGHT("night", 24, 24, false);

        private final String id;
        private final int price;
        private final int most;
        private final boolean needsStand;
        private final boolean standless;

        Wish(String id, int price, int most, boolean needsStand) {
            this(id, price, most, needsStand, false);
        }

        Wish(String id, int price, int most, boolean needsStand, boolean standless) {
            this.id = id;
            this.price = price;
            this.most = most;
            this.needsStand = needsStand;
            this.standless = standless;
        }

        public String id() {
            return id;
        }

        public int price() {
            return price;
        }

        public int most() {
            return most;
        }

        public boolean scales() {
            return most > price;
        }

        public boolean needsStand() {
            return needsStand;
        }

        public boolean standless() {
            return standless;
        }

        public String nameKey() {
            return "dream.jojoha.wish." + id;
        }

        public String descriptionKey() {
            return "dream.jojoha.wish." + id + ".desc";
        }

        public boolean picksStat() {
            return this == SELF || this == STAND;
        }
    }

    public record Result(boolean granted, String message, Raised raised, ItemStack taken, int paid) {
        static Result no(String message) {
            return new Result(false, message, Raised.NOTHING_TO_SAY, ItemStack.EMPTY, 0);
        }
    }

    record Raised(int stat, int levels, boolean stand, List<StatPerk> perks, String trust, String rank,
                  ModTraits.Trait trait, List<ItemStack> gifts, ResourceLocation aligned, float odds,
                  String fortune, String forgot) {
        static final Raised NOTHING_TO_SAY = new Raised(-1, 0, false);

        Raised(int stat, int levels, boolean stand) {
            this(stat, levels, stand, List.of(), null, null, null, List.of(), null, 0F, null, null);
        }

        static Raised settled(ModTraits.Trait trait) {
            return new Raised(-1, 0, false, List.of(), null, null, trait, List.of(), null, 0F, null, null);
        }

        static Raised bright(List<ItemStack> gifts) {
            return new Raised(-1, 0, false, List.of(), null, null, null, List.copyOf(gifts), null, 0F, null, null);
        }

        static Raised leaning(ResourceLocation standId, float odds) {
            return new Raised(-1, 0, false, List.of(), null, null, null, List.of(), standId, odds, null, null);
        }

        static Raised forgot(String discipline) {
            return new Raised(-1, 0, false, List.of(), null, null, null, List.of(), null, 0F, null, discipline);
        }
    }

    private DreamOffering() {
    }

    public static Wish byIndex(int index) {
        Wish[] all = Wish.values();
        return index >= 0 && index < all.length ? all[index] : null;
    }

    public static boolean available(JojohaPlayerData data, Wish wish) {
        return wish != null && (!wish.needsStand() || data.stand.isPresent())
                && (!wish.standless() || !data.stand.isPresent())
                && (wish != Wish.FORTUNE || !data.fortune.isEmpty())
                && (wish != Wish.FORGET || data.discipline != Discipline.NONE || data.anubis);
    }

    public static int worth(List<ItemStack> held) {
        int total = 0;
        for (ItemStack stack : held) {
            total += Worth.of(stack);
        }
        return total;
    }

    public static Result settle(ServerPlayer player, List<ItemStack> held, Wish wish) {
        return settle(player, held, wish, null, -1, 0);
    }

    public static Result settle(ServerPlayer player, List<ItemStack> held, Wish wish, ResourceLocation aligning,
                                int stat, int memories) {
        if (wish == null || (wish == Wish.ALIGN && !Spirit.pool().contains(aligning))) {
            return Result.no("message.jojoha.dream.refused");
        }

        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!available(data, wish)) {
            return Result.no("message.jojoha.dream.no_stand");
        }

        int offered = worth(held) + memories;
        if (offered < price(player, wish)) {
            return Result.no("message.jojoha.dream.not_enough");
        }

        List<ItemStack> left = new ArrayList<>();
        for (ItemStack stack : held) {
            left.add(stack.copy());
        }
        List<ItemStack> charged = charge(left, Math.max(0, Math.min(offered, most(player, wish)) - memories));
        int paid = worth(charged) + memories;
        int earned = Math.max(wish.price(), Math.round(paid / Devout.priceScale(data)));

        Raised raised = wish == Wish.ALIGN ? align(data, aligning, earned) : grant(player, data, wish, earned, stat);
        if (raised == null) {
            return Result.no("message.jojoha.dream.refused");
        }

        held.clear();
        held.addAll(left);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        return new Result(true, "message.jojoha.dream.granted." + wish.id(), raised,
                charged.isEmpty() ? ItemStack.EMPTY : charged.get(0), paid);
    }

    static List<ItemStack> charge(List<ItemStack> held, int price) {
        List<List<ItemStack>> kinds = new ArrayList<>();
        for (ItemStack stack : held) {
            if (stack.isEmpty()) {
                continue;
            }
            List<ItemStack> kind = null;
            for (List<ItemStack> existing : kinds) {
                if (ItemStack.isSameItemSameComponents(existing.get(0), stack)) {
                    kind = existing;
                    break;
                }
            }
            if (kind == null) {
                kind = new ArrayList<>();
                kinds.add(kind);
            }
            kind.add(stack);
        }

        List<ItemStack> change = new ArrayList<>();
        List<List<ItemStack>> worth = new ArrayList<>();
        for (List<ItemStack> kind : kinds) {
            if (Worth.perItem(kind.get(0)) > 0) {
                worth.add(kind);
            } else {
                change.addAll(kind);
            }
        }

        int[] each = new int[worth.size()];
        int[] count = new int[worth.size()];
        for (int k = 0; k < worth.size(); k++) {
            each[k] = Worth.perItem(worth.get(k).get(0));
            for (ItemStack stack : worth.get(k)) {
                count[k] += stack.getCount();
            }
        }
        int[] take = Change.charged(each, count, price);

        List<ItemStack> charged = new ArrayList<>();
        for (int k = 0; k < worth.size(); k++) {
            int left = take[k];
            for (ItemStack stack : worth.get(k)) {
                int from = Math.min(left, stack.getCount());
                left -= from;
                if (from > 0) {
                    charged.add(stack.copyWithCount(from));
                }
                if (stack.getCount() > from) {
                    change.add(stack.copyWithCount(stack.getCount() - from));
                }
            }
        }
        charged.sort(Comparator.comparingInt(Worth::perItem).reversed());
        held.clear();
        held.addAll(change);
        return charged;
    }

    public static int price(ServerPlayer player, Wish wish) {
        return Math.max(1, Math.round(wish.price() * Devout.priceScale(PlayerDataAccess.get(player))));
    }

    public static int most(ServerPlayer player, Wish wish) {
        return Math.max(price(player, wish), Math.round(wish.most() * Devout.priceScale(PlayerDataAccess.get(player))));
    }

    public static int howMany(Wish wish, int eachWorth) {
        return eachWorth <= 0 ? 0 : (wish.price() + eachWorth - 1) / eachWorth;
    }

    private static Raised grant(ServerPlayer player, JojohaPlayerData data, Wish wish, int paid, int stat) {
        return switch (wish) {
            case TRINKET -> Raised.bright(trinket(player, paid));
            case FATE -> settled(Traits.reroll(player, data, settleFate(paid)));
            case SELF -> boost(player, data, false, paid, stat);
            case STAND -> boost(player, data, true, paid, stat);
            case HOLLOW -> done(StandRelease.release(player, data));
            case ALIGN -> null;
            case FORTUNE -> fortuned(player, data);
            case FORGET -> forgotten(player, data);
            case NIGHT -> Raised.bright(night(player));
        };
    }

    private static Raised forgotten(ServerPlayer player, JojohaPlayerData data) {
        Discipline was = data.discipline;
        if (was == Discipline.NONE && !data.anubis) {
            return null;
        }
        Set<ResourceLocation> nodes = new HashSet<>();
        if (was == Discipline.HAMON) {
            for (SkillNode node : SkillTrees.of(SkillTrees.Tree.HAMON)) {
                nodes.add(node.id());
            }
            data.clearHamonPaths();
            data.hamonSpent = 0F;
        }
        if (data.anubis) {
            if (!Anubis.inStandSlot(data)) {
                for (SkillNode node : SkillTrees.forStand(StandTypes.ANUBIS_ID)) {
                    nodes.add(node.id());
                }
            }
            Anubis.unbond(player, data);
        }
        data.getUnlockedNodes().removeAll(nodes);
        data.discipline = Discipline.NONE;
        data.dropUnequippable();
        Discipline named = was == Discipline.NONE ? Discipline.ANUBIS : was;
        return Raised.forgot("discipline.jojoha." + named.name().toLowerCase(Locale.ROOT));
    }

    private static List<ItemStack> night(ServerPlayer player) {
        ItemStack lily = new ItemStack(ModItems.LUCID_LILY.get());
        player.getInventory().placeItemBackInInventory(lily.copy());
        return List.of(lily);
    }

    private static Raised align(JojohaPlayerData data, ResourceLocation standId, int paid) {
        float through = Math.max(0F, Math.min(1F,
                (paid - Wish.ALIGN.price()) / (float) (Wish.ALIGN.most() - Wish.ALIGN.price())));
        data.alignedStand = standId;
        data.alignOdds = ALIGN_LEAST_ODDS + (ALIGN_MOST_ODDS - ALIGN_LEAST_ODDS) * through;
        return Raised.leaning(standId, data.alignOdds);
    }

    private static Raised fortuned(ServerPlayer player, JojohaPlayerData data) {
        if (!Fortunes.on()) {
            return null;
        }
        Fortune drawn = Fortunes.redraw(player, data);
        return new Raised(-1, 0, false, List.of(), null, null, null, List.of(), null, 0F, drawn.id(), null);
    }

    private static Raised done(boolean happened) {
        return happened ? Raised.NOTHING_TO_SAY : null;
    }

    private static Raised settled(ModTraits.Trait trait) {
        return trait == null ? null : Raised.settled(trait);
    }

    static float settleFate(int paid) {
        float through = Math.max(0F, Math.min(1F,
                (paid - Wish.FATE.price()) / (float) (Wish.FATE.most() - Wish.FATE.price())));
        return 1F + (MOST_RARE_ODDS - 1F) * through;
    }

    static final int WORTH_OF_A_POINT = 8;

    public static final int MOST_POINTS = 4;

    static int points(int paid) {
        return Math.min(MOST_POINTS, paid / WORTH_OF_A_POINT);
    }

    public static boolean hasRoom(JojohaPlayerData data, boolean stand, int stat) {
        if (stat < 0 || stat >= StatPoints.COUNT || (stand && !data.stand.isPresent())) {
            return false;
        }
        int level = stand && stat != StatPoints.STAND_ENDURANCE
                ? data.stand.stats()[stat]
                : StatPoints.playerStat(data, stand ? StatPoints.ENDURANCE : stat);
        return level < StatPoints.MAX_STAT;
    }

    private static Raised boost(ServerPlayer player, JojohaPlayerData data, boolean stand, int paid, int stat) {
        if (!hasRoom(data, stand, stat)) {
            return null;
        }
        int levels = points(paid);

        boolean ownStand = stand && stat != StatPoints.STAND_ENDURANCE;
        int own = ownStand ? stat : (stand ? StatPoints.ENDURANCE : stat);
        int before = ownStand ? data.stand.stats()[own] : StatPoints.playerStat(data, own);
        int trustBefore = data.stand.isPresent() ? data.stand.trust().level() : -1;
        BondRank rankBefore = data.stand.isPresent() ? BondRank.of(data.stand) : null;

        int rose = levels > 0
                ? StatGrowth.raise(player, stand, stat, levels, true)
                : StatGrowth.feed(player, stand, stat, paid / (float) WORTH_OF_A_POINT, true);
        if (levels > 0 && rose <= 0) {
            return null;
        }

        List<StatPerk> perks = new ArrayList<>(StatPerk.crossed(ownStand, own, before, before + rose));
        if (!ownStand && own == StatPoints.ENDURANCE) {
            perks.addAll(StatPerk.crossed(true, StatPoints.STAND_ENDURANCE, before, before + rose));
        }
        JojohaPlayerData after = PlayerDataAccess.get(player);
        String trust = null, rank = null;
        if (after.stand.isPresent() && trustBefore >= 0 && after.stand.trust().level() > trustBefore) {
            trust = "trust_tier.jojoha." + after.stand.trust().key();
        }
        if (after.stand.isPresent() && rankBefore != null && BondRank.of(after.stand).ordinal() > rankBefore.ordinal()) {
            rank = BondRank.of(after.stand).translationKey();
        }
        return new Raised(stat, rose, stand, List.copyOf(perks), trust, rank, null, List.of(), null, 0F, null, null);
    }

    private static List<ItemStack> trinket(ServerPlayer player, int paid) {
        ServerLevel level = player.serverLevel();
        LootTable table = level.getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, GIFT));
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.CHEST);

        int draws = 1 + paid / WORTH_PER_DRAW;
        int tries = Math.min(MOST_TRIES, 1 + paid / WORTH_PER_TRY);
        List<ItemStack> rolled = new ArrayList<>();
        for (int draw = 0; draw < draws; draw++) {
            List<ItemStack> best = List.of();
            for (int attempt = 0; attempt < tries; attempt++) {
                List<ItemStack> tried = table.getRandomItems(params);
                if (best.isEmpty() || worth(tried) > worth(best)) {
                    best = tried;
                }
            }
            rolled.addAll(best);
        }
        for (int extra = 0; extra < MOST_TOP_UPS && worth(rolled) < paid; extra++) {
            rolled.addAll(table.getRandomItems(params));
        }
        int shortBy = paid - worth(rolled);
        if (shortBy > 0) {
            int emerald = Worth.perItem(new ItemStack(Items.EMERALD));
            rolled.add(new ItemStack(Items.EMERALD, (shortBy + emerald - 1) / emerald));
        }
        if (rolled.isEmpty()) {
            rolled = List.of(new ItemStack(Items.DIAMOND, FALLBACK_DIAMONDS));
        }
        List<ItemStack> given = new ArrayList<>();
        for (ItemStack stack : rolled) {
            given.add(stack.copy());
            player.getInventory().placeItemBackInInventory(stack.copy());
        }
        return given;
    }
}
