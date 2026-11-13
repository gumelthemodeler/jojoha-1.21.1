package org.gumel.jojoha.stand;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.anubis.Anubis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Spirit {
    public static final float LEAN = 4F;

    public static final float SPREAD = 0.5F;

    public static final float SETTLED_AT = 120F;

    public static final int PLAYER_KILL_WEIGHT = 3;

    public static final float HUNTING_SHARE = 0.5F;

    public enum Way {
        CRUELTY(1F),
        HUNTING(1F),
        NURTURE(1F),
        BLADE(3F),
        AIM(2F);

        private final float per;

        Way(float per) {
            this.per = per;
        }

        public float per() {
            return per;
        }
    }

    private static final Map<ResourceLocation, Way> CALLED_BY = Map.of(
            StandTypes.THE_WORLD_ID, Way.CRUELTY,
            StandTypes.SILVER_CHARIOT_ID, Way.BLADE,
            StandTypes.HIEROPHANT_GREEN_ID, Way.AIM,
            StandTypes.HERMIT_PURPLE_ID, Way.NURTURE);

    private static final Set<EntityType<?>> DEFENCELESS = Set.of(EntityType.VILLAGER, EntityType.WANDERING_TRADER,
            EntityType.IRON_GOLEM, EntityType.SNOW_GOLEM);

    private static final List<Item> AIMED = List.of(Items.BOW, Items.CROSSBOW, Items.TRIDENT);

    private Spirit() {
    }

    public static List<ResourceLocation> pool() {
        List<ResourceLocation> pool = new ArrayList<>(StandTypes.all().keySet());
        pool.remove(Anubis.ID);
        pool.removeIf(StandTypes::secret);
        pool.sort(Comparator.comparing(ResourceLocation::toString));
        return pool;
    }

    public static float[] deeds(ServerPlayer player) {
        StatsCounter stats = player.getStats();
        float[] raw = new float[Way.values().length];
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            int killed = stats.getValue(Stats.ENTITY_KILLED.get(type));
            if (killed <= 0) {
                continue;
            }
            MobCategory category = type.getCategory();
            if (category == MobCategory.MONSTER) {
                raw[Way.HUNTING.ordinal()] += killed;
            } else if (category != MobCategory.MISC || DEFENCELESS.contains(type)) {
                raw[Way.CRUELTY.ordinal()] += killed;
            }
        }
        raw[Way.CRUELTY.ordinal()] += PLAYER_KILL_WEIGHT * stats.getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));
        raw[Way.NURTURE.ordinal()] += stats.getValue(Stats.CUSTOM.get(Stats.ANIMALS_BRED))
                + stats.getValue(Stats.CUSTOM.get(Stats.TRADED_WITH_VILLAGER));
        for (Holder<Item> sword : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.SWORDS)) {
            raw[Way.BLADE.ordinal()] += stats.getValue(Stats.ITEM_USED.get(sword.value()));
        }
        for (Item aimed : AIMED) {
            raw[Way.AIM.ordinal()] += stats.getValue(Stats.ITEM_USED.get(aimed));
        }
        for (Way way : Way.values()) {
            raw[way.ordinal()] /= way.per();
        }
        return raw;
    }

    public static float affinity(ResourceLocation standId, float[] deeds) {
        float total = 0F;
        float most = 0F;
        for (float deed : deeds) {
            total += deed;
            most = Math.max(most, deed);
        }
        if (total <= 0F) {
            return 0F;
        }
        Way way = CALLED_BY.get(standId);
        if (way != null) {
            return deeds[way.ordinal()] / total;
        }
        if (!StandTypes.STAR_PLATINUM_ID.equals(standId)) {
            return 0F;
        }
        float ways = deeds.length;
        float evenness = Math.max(0F, Math.min(1F, (1F - most / total) / (1F - 1F / ways)));
        return HUNTING_SHARE * deeds[Way.HUNTING.ordinal()] / total + (1F - HUNTING_SHARE) * evenness;
    }

    public static Map<ResourceLocation, Float> weights(ServerPlayer player, List<ResourceLocation> pool) {
        float[] deeds = deeds(player);
        float total = 0F;
        for (float deed : deeds) {
            total += deed;
        }
        float settled = Math.min(1F, total / SETTLED_AT);
        float lean = JojohaServerConfig.SPIRIT_LEAN.value();
        float spread = JojohaServerConfig.STAND_SPREAD.value();
        StandSpread held = StandSpread.of(player.server);
        int least = Integer.MAX_VALUE;
        for (ResourceLocation standId : pool) {
            least = Math.min(least, held.held(standId));
        }

        Map<ResourceLocation, Float> weights = new LinkedHashMap<>();
        for (ResourceLocation standId : pool) {
            float called = 1F + lean * settled * affinity(standId, deeds);
            weights.put(standId, called / (1F + spread * (held.held(standId) - least)));
        }
        return weights;
    }

    public static ResourceLocation choose(ServerPlayer player, JojohaPlayerData data, List<ResourceLocation> pool) {
        RandomSource random = player.getRandom();
        if (data.alignedStand != null && pool.contains(data.alignedStand) && random.nextFloat() < data.alignOdds) {
            return data.alignedStand;
        }
        Map<ResourceLocation, Float> weights = weights(player, pool);
        float sum = 0F;
        for (float weight : weights.values()) {
            sum += weight;
        }
        float roll = random.nextFloat() * sum;
        for (Map.Entry<ResourceLocation, Float> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0F) {
                return entry.getKey();
            }
        }
        return pool.get(pool.size() - 1);
    }

    public static void arrived(ServerPlayer player, JojohaPlayerData data, ResourceLocation standId) {
        StandSpread.of(player.server).gained(standId);
        data.alignedStand = null;
        data.alignOdds = 0F;
    }

    public static void released(ServerPlayer player, ResourceLocation standId) {
        StandSpread.of(player.server).lost(standId);
    }
}
