package org.gumel.jojoha.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.gumel.jojoha.JojohaAdvancements;
import org.gumel.jojoha.item.Curios;
import org.gumel.jojoha.registry.ModItems;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

public class WillowEntity extends AbstractVillager implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");

    private static final float WALKING_THRESHOLD = 0.01F;

    private static final String CONTROLLER = "willow";
    private static final int TRANSITION_TICKS = 5;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int tradeXp;
    private int tradeLevel;

    private static final int SHARD_COST_MIN = 30;
    private static final int SHARD_COST_MAX = 40;
    private static final int MASK_COST_MIN = 10;
    private static final int MASK_COST_MAX = 14;
    private static final int SOUL_CARD_COST = 8;

    private static final int MAP_COST_MIN = 45;
    private static final int MAP_COST_MAX = 60;

    private static final int ARROW_VALUE = 12;
    private static final int HEAVENS_ARROW_VALUE = 64;
    private static final int SHARD_VALUE = 6;
    private static final int MASK_VALUE = 6;

    private static final int RAW_METEORITE_PER_SALE = 2;
    private static final int RAW_METEORITE_VALUE = 5;

    private static final int SCRAP_PER_SALE = 4;
    private static final int SCRAP_VALUE = 4;
    private static final int INGOT_VALUE = 8;
    private static final int EMERALDS_PER_SALE = 1;
    private static final int EMERALD_VALUE = 3;

    private static final int SHARDS_PER_ARROW = 6;
    private static final int INGOTS_PER_ARROW = 3;

    private static final int[] LEVEL_AT_XP = {0, 30, 80, 160};

    private static final int TIER_SHARD = 1;
    private static final int TIER_MASK = 2;
    private static final int TIER_ARROW = 3;

    private static final int CARDS_STOCKED = 2;

    private static final float STOCK_BASE = 0.45F;
    private static final float STOCK_PER_LEVEL = 0.12F;

    private static final int MIN_DEALS = 4;

    private static final int ARROWHEAD_VALUE = 7;
    private static final int ARROWHEAD_COST_MIN = 14;
    private static final int ARROWHEAD_COST_MAX = 20;

    private static final int RAW_PER_PURCHASE = 2;
    private static final int RAW_COST_MIN = 9;
    private static final int RAW_COST_MAX = 13;
    private static final int INGOT_COST_MIN = 16;
    private static final int INGOT_COST_MAX = 22;

    private static final int SKIN_ARROW_COST_MIN = 55;
    private static final int SKIN_ARROW_COST_MAX = 75;
    private static final int PLUCK_COST_MIN = 70;
    private static final int PLUCK_COST_MAX = 95;

    private static final int TOTEM_KEY_COST_MIN = 10;
    private static final int TOTEM_KEY_COST_MAX = 14;
    private static final int GLOVES_COST_MIN = 40;
    private static final int GLOVES_COST_MAX = 55;
    private static final int SCARF_COST_MIN = 55;
    private static final int SCARF_COST_MAX = 75;

    private static final int TRADE_USES = 6;
    private static final int RARE_TRADE_USES = 2;
    private static final int EARNING_TRADE_USES = 16;
    private static final int TRADE_XP = 3;
    private static final float PRICE_MULTIPLIER = 0.05F;

    public WillowEntity(EntityType<? extends WillowEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 0.6));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.4));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.35));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player,
                                                             InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.VILLAGER_SPAWN_EGG)
                || !this.isAlive() || this.isTrading() || this.isBaby()
                || hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }

        if (!this.level().isClientSide()) {
            if (this.getOffers().isEmpty()) {
                return InteractionResult.CONSUME;
            }
            this.setTradingPlayer(player);
            this.openTradingScreen(player, this.getDisplayName(), 1);

            if (player instanceof ServerPlayer served) {
                JojohaAdvancements.award(served, "part3/a_woman_in_the_wilds");
            }
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType reason,
            SpawnGroupData data) {
        SpawnGroupData result =
                super.finalizeSpawn(level, difficulty, reason, data);
        this.setAge(0);
        return result;
    }

    private static final List<Supplier<Item>> SOUL_CARDS =
            List.of(ModItems.SOUL_CARD_JOTARO, ModItems.SOUL_CARD_KAKYOIN,
                    ModItems.SOUL_CARD_POLNAREFF, ModItems.SOUL_CARD_AVDOL,
                    ModItems.SOUL_CARD_JOSEPH, ModItems.SOUL_CARD_DIO,
                    ModItems.SOUL_CARD_ZEPPELI, ModItems.SOUL_CARD_JONATHAN, ModItems.SOUL_CARD_DIO_P1,
                    ModItems.SOUL_CARD_JOSEPH_P2, ModItems.SOUL_CARD_CAESAR);

    @Override
    protected void updateTrades() {
        MerchantOffers offers = this.getOffers();
        offers.clear();

        String[] earners = {"buy_ingot", "buy_raw", "buy_scrap", "buy_emerald"};
        String kept = earners[0];
        for (String id : earners) {
            if (stockScore(id) < stockScore(kept)) {
                kept = id;
            }
        }

        List<Deal> catalogue = new ArrayList<>();
        deal(catalogue, "buy_ingot", 0, () -> offer(
                new ItemCost(ModItems.METEORITE_INGOT.get()),
                coinStack(INGOT_VALUE), EARNING_TRADE_USES));
        deal(catalogue, "buy_raw", 0, () -> offer(
                new ItemCost(ModItems.RAW_METEORITE.get(), RAW_METEORITE_PER_SALE),
                coinStack(RAW_METEORITE_VALUE), EARNING_TRADE_USES));
        deal(catalogue, "buy_scrap", 0, () -> offer(
                new ItemCost(ModItems.RAW_METEORITE_SCRAP.get(), SCRAP_PER_SALE),
                coinStack(SCRAP_VALUE), EARNING_TRADE_USES));
        deal(catalogue, "buy_emerald", 0, () -> offer(
                new ItemCost(Items.EMERALD, EMERALDS_PER_SALE),
                coinStack(EMERALD_VALUE), EARNING_TRADE_USES));

        deal(catalogue, "buy_arrow", 0, () -> offer(
                new ItemCost(ModItems.STAND_ARROW.get()), coinStack(ARROW_VALUE), TRADE_USES));
        deal(catalogue, "buy_shard", 0, () -> offer(
                new ItemCost(ModItems.STAND_ARROW_SHARD.get()), coinStack(SHARD_VALUE), TRADE_USES));
        deal(catalogue, "buy_mask", 0, () -> offer(
                new ItemCost(ModItems.STONE_MASK.get()), coinStack(MASK_VALUE), TRADE_USES));
        deal(catalogue, "buy_arrowhead", 0, () -> offer(
                new ItemCost(ModItems.ANCIENT_ARROWHEAD.get()), coinStack(ARROWHEAD_VALUE), TRADE_USES));

        deal(catalogue, "sell_raw", 0, () -> offer(
                coins(price("sell_raw", RAW_COST_MIN, RAW_COST_MAX)),
                new ItemStack(ModItems.RAW_METEORITE.get(), RAW_PER_PURCHASE), TRADE_USES));
        deal(catalogue, "sell_arrowhead", 0, () -> offer(
                coins(price("sell_arrowhead", ARROWHEAD_COST_MIN, ARROWHEAD_COST_MAX)),
                new ItemStack(ModItems.ANCIENT_ARROWHEAD.get()), TRADE_USES));
        deal(catalogue, "sell_ingot", TIER_SHARD, () -> offer(
                coins(price("sell_ingot", INGOT_COST_MIN, INGOT_COST_MAX)),
                new ItemStack(ModItems.METEORITE_INGOT.get()), TRADE_USES));
        deal(catalogue, "sell_shard", TIER_SHARD, () -> offer(
                coins(price("sell_shard", SHARD_COST_MIN, SHARD_COST_MAX)),
                new ItemStack(ModItems.STAND_ARROW_SHARD.get()), TRADE_USES));
        deal(catalogue, "sell_totem_key", TIER_SHARD, () -> offer(
                coins(price("sell_totem_key", TOTEM_KEY_COST_MIN, TOTEM_KEY_COST_MAX)),
                new ItemStack(ModItems.TOTEM_KEY.get()), TRADE_USES));
        deal(catalogue, "sell_bubble_gloves", TIER_MASK, () -> offer(
                coins(price("sell_bubble_gloves", GLOVES_COST_MIN, GLOVES_COST_MAX)),
                new ItemStack(ModItems.BUBBLE_GLOVES.get()), RARE_TRADE_USES));
        deal(catalogue, "sell_mask", TIER_MASK, () -> offer(
                coins(price("sell_mask", MASK_COST_MIN, MASK_COST_MAX)),
                new ItemStack(ModItems.STONE_MASK.get()), RARE_TRADE_USES));
        deal(catalogue, "sell_skin_arrow", TIER_MASK, () -> offer(
                coins(price("sell_skin_arrow", SKIN_ARROW_COST_MIN, SKIN_ARROW_COST_MAX)),
                new ItemStack(ModItems.FRACTURED_SKIN_ARROW.get()), RARE_TRADE_USES));
        deal(catalogue, "sell_pluck", TIER_ARROW, () -> offer(
                coins(price("sell_pluck", PLUCK_COST_MIN, PLUCK_COST_MAX)),
                new ItemStack(ModItems.PLUCK.get()), RARE_TRADE_USES));
        deal(catalogue, "sell_lisa_scarf", TIER_ARROW, () -> offer(
                coins(price("sell_lisa_scarf", SCARF_COST_MIN, SCARF_COST_MAX)),
                new ItemStack(ModItems.LISA_SCARF.get()), RARE_TRADE_USES));

        catalogue.sort(Comparator.comparingDouble(d -> stockScore(d.id())));

        int floorTaken = 0;
        for (Deal d : catalogue) {
            boolean forced = d.id().equals(kept);
            if (d.tier() == 0 && floorTaken < MIN_DEALS) {
                forced = true;
                floorTaken++;
            }
            if (forced || stockScore(d.id()) < stockFraction()) {
                offers.add(d.build().get());
            }
        }

        List<Supplier<Item>> stock =
                new ArrayList<>(SOUL_CARDS);
        Collections.shuffle(stock, new Random(this.getUUID().getLeastSignificantBits()));
        for (int i = 0; i < Math.min(CARDS_STOCKED + this.tradeLevel, stock.size()); i++) {
            offers.add(offer(coins(SOUL_CARD_COST), new ItemStack(stock.get(i).get()), RARE_TRADE_USES));
        }

        if (this.tradeLevel >= TIER_MASK) {
            offers.add(offer(coins(price("sell_map", MAP_COST_MIN, MAP_COST_MAX)),
                    new ItemStack(ModItems.METEOR_MAP.get()), RARE_TRADE_USES));
        }

        offers.add(offer(new ItemCost(ModItems.HEAVENS_ARROW.get()), coinStack(HEAVENS_ARROW_VALUE), TRADE_USES));

        offers.add(offer(new ItemCost(ModItems.ROAD_ROLLER_KEY.get(), 1),
                coinStack(Curios.ROAD_ROLLER_KEY_COINS), TRADE_USES));

        if (this.tradeLevel >= TIER_ARROW) {
            offers.add(new MerchantOffer(
                    new ItemCost(ModItems.STAND_ARROW_SHARD.get(), SHARDS_PER_ARROW),
                    Optional.of(new ItemCost(ModItems.METEORITE_INGOT.get(), INGOTS_PER_ARROW)),
                    new ItemStack(ModItems.STAND_ARROW.get()),
                    RARE_TRADE_USES, TRADE_XP, PRICE_MULTIPLIER));
        }
    }

    private record Deal(String id, int tier, Supplier<MerchantOffer> build) {
    }

    private void deal(List<Deal> into, String id, int tier,
                      Supplier<MerchantOffer> build) {
        if (this.tradeLevel >= tier) {
            into.add(new Deal(id, tier, build));
        }
    }

    private float stockFraction() {
        return Math.min(1.0F, STOCK_BASE + STOCK_PER_LEVEL * this.tradeLevel);
    }

    private float stockScore(String id) {
        return seededFor(id + "#stock").nextFloat();
    }

    private int price(String id, int min, int max) {
        return min + seededFor(id + "#price").nextInt(max - min + 1);
    }

    private Random seededFor(String id) {
        return new Random(this.getUUID().getLeastSignificantBits() * 31L + id.hashCode());
    }

    public int tradeLevel() {
        return this.tradeLevel;
    }

    private static MerchantOffer offer(ItemCost cost, ItemStack result, int uses) {
        return new MerchantOffer(cost, result, uses, TRADE_XP, PRICE_MULTIPLIER);
    }

    private static ItemCost coins(int amount) {
        return new ItemCost(ModItems.JOJO_COIN.get(), amount);
    }

    private static ItemStack coinStack(int amount) {
        return new ItemStack(ModItems.JOJO_COIN.get(), amount);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (this.getTradingPlayer() instanceof ServerPlayer buyer) {
            JojohaAdvancements.award(buyer, "part3/a_fair_trade");
        }

        if (offer.shouldRewardExp()) {
            int amount = 3 + this.random.nextInt(4);
            this.level().addFreshEntity(new ExperienceOrb(
                    this.level(), this.getX(), this.getY() + 0.5, this.getZ(), amount));
        }

        this.tradeXp += offer.getXp();
        int earned = this.tradeLevel;
        while (earned + 1 < LEVEL_AT_XP.length && this.tradeXp >= LEVEL_AT_XP[earned + 1]) {
            earned++;
        }

        if (earned > this.tradeLevel) {
            this.tradeLevel = earned;
            this.updateTrades();
            if (this.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        this.getX(), this.getY() + 1.0, this.getZ(), 12, 0.4, 0.6, 0.4, 0.0);
            }
        }
    }

    public void moveIn(BlockPos home, int roam) {
        this.restrictTo(home, roam);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("WillowTradeXp", this.tradeXp);
        tag.putInt("WillowLevel", this.tradeLevel);
        if (this.hasRestriction()) {
            tag.putLong("WillowHome", this.getRestrictCenter().asLong());
            tag.putInt("WillowRoam", (int) this.getRestrictRadius());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.tradeXp = tag.getInt("WillowTradeXp");
        this.tradeLevel = tag.getInt("WillowLevel");
        if (tag.contains("WillowHome")) {
            moveIn(BlockPos.of(tag.getLong("WillowHome")), tag.getInt("WillowRoam"));
        }
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, TRANSITION_TICKS, this::animate));
    }

    private PlayState animate(AnimationState<WillowEntity> state) {
        return state.setAndContinue(
                state.getLimbSwingAmount() > WALKING_THRESHOLD ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean success) {
        return success ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.AMETHYST_BLOCK_BREAK;
    }

    @Override
    public void playCelebrateSound() {
    }
}
