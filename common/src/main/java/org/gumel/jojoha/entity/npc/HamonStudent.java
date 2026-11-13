package org.gumel.jojoha.entity.npc;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.VampireBlood;
import org.gumel.jojoha.entity.VampireEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class HamonStudent extends JojoNpc {
    private static final double MAX_HEALTH = 26.0;
    private static final double ATTACK_DAMAGE = 4.0;
    private static final double MOVEMENT_SPEED = 0.3;
    private static final double FOLLOW_RANGE = 12.0;
    private static final double ARMOR = 2.0;
    private static final float HAMON_BONUS = 3F;
    private static final double KNOCKBACK = 0.4;
    private static final long THANKS_GAP = 6000L;
    private static final double THANKS_RANGE = 20.0;

    private static final String GREET = "npc.jojoha.hamon_student.greet";
    private static final String GREET_FRIEND = "npc.jojoha.hamon_student.greet_friend";
    private static final String ASK_HAMON = "npc.jojoha.hamon_student.ask_hamon";
    private static final String ASK_VAMPIRES = "npc.jojoha.hamon_student.ask_vampires";
    private static final String ASK_TASK = "npc.jojoha.hamon_student.ask_task";
    private static final String ASK_STANDING = "npc.jojoha.hamon_student.ask_standing";
    private static final String TAKE_TASK = "npc.jojoha.hamon_student.take_task";
    private static final String CLAIM_TASK = "npc.jojoha.hamon_student.claim_task";
    private static final String DECLINE_TASK = "npc.jojoha.hamon_student.decline_task";
    private static final String NO_TASK = "npc.jojoha.hamon_student.task_none";
    private static final String BYE = "npc.jojoha.bye";
    private static final String ANOTHER_TASK = "npc.jojoha.hamon_student.another_task";
    private static final List<String> TOPICS = List.of(TALK, ASK_TASK, BYE);

    private long lastThanks = -THANKS_GAP;

    public HamonStudent(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.ARMOR, ARMOR);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HamonTechniques(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(HamonStudent.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, VampireEntity.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true,
                target -> target instanceof Player player && hostileTo(player)));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Zombie.class, true));
    }

    public boolean female() {
        return (getUUID().getMostSignificantBits() & 1L) == 1L;
    }

    @Override
    public Boolean feminineVoice() {
        return female();
    }

    @Override
    protected boolean friendly() {
        return true;
    }

    public static boolean hostileTo(Player player) {
        return !player.isCreative() && !player.isSpectator()
                && (VampireBlood.isVampire(player) || NpcStanding.hamon(player) <= NpcStanding.HOSTILE);
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
        boolean hamon = undead(victim);
        float amount = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) + (hamon ? HAMON_BONUS : 0F);
        boolean landed = victim.hurt(damageSources().mobAttack(this),
                DamageScale.scale(DamageScale.Kind.HAMON, this, victim, amount));
        if (!landed) {
            return false;
        }
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        victim.knockback(KNOCKBACK, Mth.sin(yaw), -Mth.cos(yaw));
        setLastHurtMob(victim);
        HamonTechniques.meleeFx(level, this, victim, hamon);
        return true;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (allies(this, target)) {
            return;
        }
        if (target instanceof Player player && !hostileTo(player) && getLastHurtByMob() != player
                && !(player.getLastHurtMob() instanceof HamonStudent)) {
            return;
        }
        super.setTarget(target);
    }

    public void thank(ServerPlayer player, LivingEntity slain) {
        long now = level().getGameTime();
        if (now - lastThanks < THANKS_GAP) {
            return;
        }
        boolean fighting = !level().getEntitiesOfClass(VampireEntity.class, getBoundingBox().inflate(THANKS_RANGE),
                vampire -> vampire.isAlive() && vampire != slain).isEmpty();
        if (fighting) {
            return;
        }
        lastThanks = now;
        say("npc.jojoha.hamon_student.thanks", player.getDisplayName());
    }

    public void cry() {
        say("npc.jojoha.hamon_student.cry");
    }

    @Override
    protected int giftValue(ItemStack stack) {
        if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            return 12;
        }
        if (stack.is(Items.CAKE)) {
            return 8;
        }
        if (stack.is(Items.ROTTEN_FLESH) || stack.is(Items.SPIDER_EYE) || stack.is(Items.POISONOUS_POTATO)) {
            return -5;
        }
        return stack.has(DataComponents.FOOD) ? 3 : 0;
    }

    @Override
    @Nullable
    protected Line greet(ServerPlayer player) {
        if (hostileTo(player)) {
            say(VampireBlood.isVampire(player) ? "npc.jojoha.hamon_student.smell" : "npc.jojoha.hamon_student.traitor");
            setTarget(player);
            return null;
        }
        if (getTarget() != null) {
            say("npc.jojoha.hamon_student.busy");
            return null;
        }
        boolean friend = NpcStanding.hamon(player) >= NpcStanding.FRIENDLY || tier(player).ordinal() >= Tier.FRIEND.ordinal();
        return new Line((friend ? GREET_FRIEND : GREET) + "." + trait().id(), TOPICS);
    }

    private Tier standingTier(ServerPlayer player) {
        Tier tier = tier(player);
        if (NpcStanding.hamon(player) >= NpcStanding.FRIENDLY && tier.ordinal() < Tier.FRIEND.ordinal()) {
            return Tier.FRIEND;
        }
        return tier;
    }

    private Line offerLine(ServerPlayer player, Quests.Quest offer) {
        if (Quests.poolSize(player, Quests.HAMON, standingTier(player)) > 1) {
            return new Line(offer.key("offer"), List.of(TAKE_TASK, ANOTHER_TASK, DECLINE_TASK));
        }
        return new Line(offer.key("offer"), List.of(TAKE_TASK, DECLINE_TASK));
    }

    @Nullable
    private Line task(ServerPlayer player) {
        List<Quests.Quest> held = Quests.heldFrom(player, Quests.HAMON);
        for (Quests.Quest quest : held) {
            if (Quests.ready(player, quest)) {
                return new Line(quest.key("done"), List.of(CLAIM_TASK));
            }
        }
        Quests.Quest offer = Quests.offer(player, this, Quests.HAMON, standingTier(player));
        if (offer != null) {
            return offerLine(player, offer);
        }
        if (!held.isEmpty()) {
            return new Line(held.get(0).key("progress"), List.of(BYE));
        }
        return new Line(NO_TASK, List.of(BYE));
    }

    @Override
    protected List<String> talkTopics() {
        return List.of(ASK_STANDING, ASK_HAMON, ASK_VAMPIRES);
    }

    @Override
    @Nullable
    protected Line answer(ServerPlayer player, String text, String reply) {
        switch (reply) {
            case ASK_HAMON -> {
                return new Line("npc.jojoha.hamon_student.hamon", talkReplies());
            }
            case ASK_STANDING -> {
                NpcStanding.announce(player);
                NpcStanding.Level level = NpcStanding.Level.of(NpcStanding.hamon(player));
                return new Line("npc.jojoha.hamon_student.standing." + level.name().toLowerCase(Locale.ROOT), talkReplies());
            }
            case ASK_VAMPIRES -> {
                return new Line("npc.jojoha.hamon_student.vampires", talkReplies());
            }
            case ASK_TASK -> {
                return task(player);
            }
            case TAKE_TASK -> {
                Quests.Quest quest = Quests.fromLine(text, "offer");
                if (quest == null) {
                    return null;
                }
                Quests.take(player, quest, this);
                return new Line(quest.key("taken"), List.of());
            }
            case CLAIM_TASK -> {
                Quests.Quest quest = Quests.fromLine(text, "done");
                if (quest == null || !Quests.claim(player, quest, this)) {
                    return null;
                }
                return new Line(quest.key("thanks"), List.of());
            }
            case ANOTHER_TASK -> {
                Quests.Quest current = Quests.fromLine(text, "offer");
                Quests.Quest next = current == null ? null
                        : Quests.nextOffer(player, Quests.HAMON, standingTier(player), current);
                return next == null ? task(player) : offerLine(player, next);
            }
            case DECLINE_TASK -> {
                return new Line("npc.jojoha.hamon_student.task_declined", List.of());
            }
            default -> {
                return null;
            }
        }
    }
}
