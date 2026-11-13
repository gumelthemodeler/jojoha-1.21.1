package org.gumel.jojoha.entity.npc;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.VampireBlood;
import org.gumel.jojoha.entity.VampireEntity;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.mob.MobStands;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class CrusaderNpc extends JojoNpc {
    private static final double MAX_HEALTH = 40.0;
    private static final double ATTACK_DAMAGE = 6.0;
    private static final double MOVEMENT_SPEED = 0.3;
    private static final double FOLLOW_RANGE = 20.0;
    private static final double ARMOR = 4.0;
    private static final float HAMON_BONUS = 3F;
    private static final double KNOCKBACK = 0.5;
    private static final int FAVOURITE_GIFT = 12;
    private static final int GOOD_GIFT = 8;
    private static final int FOOD_GIFT = 3;
    private static final int BAD_GIFT = -5;

    private static final String BYE = "npc.jojoha.bye";
    private static final String ASK_STAND = "npc.jojoha.crusader.ask_stand";
    private static final String ASK_DIO = "npc.jojoha.crusader.ask_dio";
    private static final List<String> TOPICS = List.of(TALK, BYE);

    public enum Crusader {
        JOTARO("jotaro_sdc", "jotaro_p3", Trait.GRUMPY, StandTypes.STAR_PLATINUM_ID, false, ModItems.CIGARETTE, ModSounds.NPC_VOICE_JOTARO),
        JOSEPH("joseph_sdc", "joseph_p3", Trait.CHEERFUL, null, true, ModItems.COCA_COLA, ModSounds.NPC_VOICE_JOSEPH),
        AVDOL("avdol", "avdol_p3", Trait.SERIOUS, StandTypes.MAGICIANS_RED_ID, false, ModItems.TAROT_CARD, ModSounds.NPC_VOICE_AVDOL),
        KAKYOIN("kakyoin", "kakyoin_p3", Trait.SHY, StandTypes.HIEROPHANT_GREEN_ID, false, ModItems.CHERRIES, ModSounds.NPC_VOICE_KAKYOIN),
        POLNAREFF("polnareff", "polnareff_p3", Trait.PROUD, StandTypes.SILVER_CHARIOT_ID, false, ModItems.BRANDYWINE, ModSounds.NPC_VOICE_POLNAREFF);

        private final ResourceLocation model;
        private final ResourceLocation texture;
        private final Trait trait;
        @Nullable
        private final ResourceLocation stand;
        private final boolean hamon;
        private final Supplier<? extends Item> favourite;
        private final Supplier<SoundEvent> voice;

        Crusader(String model, String texture, Trait trait, @Nullable ResourceLocation stand, boolean hamon,
                 Supplier<? extends Item> favourite, Supplier<SoundEvent> voice) {
            this.model = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "geo/" + model + ".geo.json");
            this.texture = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/mob/" + texture + ".png");
            this.trait = trait;
            this.stand = stand;
            this.hamon = hamon;
            this.favourite = favourite;
            this.voice = voice;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public ResourceLocation model() {
            return model;
        }

        public ResourceLocation texture() {
            return texture;
        }

        @Nullable
        public ResourceLocation stand() {
            return stand;
        }

        private String line(String what) {
            return "npc.jojoha." + id() + "." + what;
        }
    }

    private final Crusader crusader;
    private boolean ready;

    public CrusaderNpc(EntityType<? extends PathfinderMob> type, Level level, Crusader crusader) {
        super(type, level);
        this.crusader = crusader;
        if (crusader.hamon) {
            goalSelector.addGoal(1, new HamonTechniques(this));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.ARMOR, ARMOR);
    }

    public Crusader crusader() {
        return crusader;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, VampireEntity.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                target -> !(target instanceof Creeper) && !(target instanceof EnderMan)));
    }

    @Override
    protected boolean friendly() {
        return true;
    }

    @Override
    public Trait trait() {
        return crusader.trait;
    }

    @Override
    @Nullable
    public Boolean feminineVoice() {
        return false;
    }

    @Override
    public SoundEvent voiceBlip() {
        return crusader.voice.get();
    }

    @Override
    protected String voice() {
        return crusader.id();
    }

    @Override
    protected String talkKey(String topic, boolean signature) {
        return crusader.id() + "." + topic;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!ready && !level().isClientSide()) {
            ready = true;
            settle();
            if (crusader.stand != null && !MobStands.has(this)) {
                MobStands.give(this, crusader.stand, 0);
            }
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (allies(this, target) || target instanceof Player player && getLastHurtByMob() != player) {
            return;
        }
        super.setTarget(target);
    }

    private static boolean undead(LivingEntity target) {
        return target instanceof VampireEntity || target.getType().is(EntityTypeTags.UNDEAD)
                || target instanceof Player player && VampireBlood.isVampire(player);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity victim) || !(level() instanceof ServerLevel level)) {
            return super.doHurtTarget(target);
        }
        boolean hamon = crusader.hamon && undead(victim);
        float amount = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) + (hamon ? HAMON_BONUS : 0F);
        boolean landed = victim.hurt(damageSources().mobAttack(this),
                DamageScale.scale(hamon ? DamageScale.Kind.HAMON : DamageScale.Kind.COMBAT, this, victim, amount));
        if (!landed) {
            return false;
        }
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        victim.knockback(KNOCKBACK, Mth.sin(yaw), -Mth.cos(yaw));
        setLastHurtMob(victim);
        if (crusader.hamon) {
            HamonTechniques.meleeFx(level, this, victim, hamon);
        }
        return true;
    }

    private static final int NEIGHBOUR_TICKS = 300;

    @Override
    protected void neighbourly(ServerPlayer player) {
        Holder<MobEffect> effect = switch (crusader) {
            case JOTARO -> MobEffects.DAMAGE_BOOST;
            case JOSEPH -> MobEffects.LUCK;
            case AVDOL -> MobEffects.FIRE_RESISTANCE;
            case KAKYOIN -> MobEffects.NIGHT_VISION;
            case POLNAREFF -> MobEffects.MOVEMENT_SPEED;
        };
        player.addEffect(new MobEffectInstance(effect, NEIGHBOUR_TICKS, 0, true, true));
    }

    @Override
    protected int giftValue(ItemStack stack) {
        if (stack.is(crusader.favourite.get())) {
            return FAVOURITE_GIFT;
        }
        if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE) || stack.is(Items.CAKE)) {
            return GOOD_GIFT;
        }
        if (stack.is(Items.ROTTEN_FLESH) || stack.is(Items.SPIDER_EYE) || stack.is(Items.POISONOUS_POTATO)) {
            return BAD_GIFT;
        }
        return stack.has(DataComponents.FOOD) ? FOOD_GIFT : 0;
    }

    @Override
    @Nullable
    protected Line greet(ServerPlayer player) {
        if (getTarget() != null) {
            say(crusader.line("busy"));
            return null;
        }
        if (VampireBlood.isVampire(player)) {
            return new Line(crusader.line("vampire"), List.of());
        }
        boolean friend = tier(player).ordinal() >= Tier.FRIEND.ordinal();
        return new Line(crusader.line(friend ? "greet_friend" : "greet"), TOPICS);
    }

    @Override
    protected List<String> talkTopics() {
        return List.of(ASK_STAND, ASK_DIO);
    }

    @Override
    @Nullable
    protected Line answer(ServerPlayer player, String text, String reply) {
        return switch (reply) {
            case ASK_STAND -> new Line(crusader.line("stand"), talkReplies());
            case ASK_DIO -> new Line(crusader.line("dio"), talkReplies());
            case BYE -> new Line(crusader.line("bye"), List.of());
            default -> null;
        };
    }
}
