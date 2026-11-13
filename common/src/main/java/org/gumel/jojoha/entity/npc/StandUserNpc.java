package org.gumel.jojoha.entity.npc;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.stand.mob.MobStands;
import org.gumel.jojoha.town.NpcMemory;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class StandUserNpc extends JojoNpc {
    public enum Personality { HONOURABLE, HOSTILE, TRICKSTER }

    private enum State { IDLE, APPROACH, OFFER, DUEL, AMBUSH, LEAVING }

    private static final double MAX_HEALTH = 30.0;
    private static final double ATTACK_DAMAGE = 3.0;
    private static final double MOVEMENT_SPEED = 0.3;
    private static final double FOLLOW_RANGE = 20.0;

    private static final double OFFER_RANGE = 5.0;
    private static final double WALK_OFF_RANGE = 24.0;
    private static final double LOST_RANGE = 48.0;
    private static final int APPROACH_LIMIT = 1200;
    private static final int OFFER_LIMIT = 600;
    private static final int LEAVE_LIMIT = 600;
    private static final int AMBUSH_FEWEST = 160;
    private static final int AMBUSH_SPREAD = 120;
    private static final int CALM_BEFORE_ASKING = 100;
    private static final float YIELD_AT = 0.3F;
    private static final float SPARE_AT = 0.2F;
    private static final int FEWEST_COINS = 4;
    private static final int EXTRA_COINS = 5;
    private static final float CRYSTAL_CHANCE = 0.2F;

    private static final String ACCEPT = "npc.jojoha.stand_user.accept";
    private static final String DECLINE = "npc.jojoha.stand_user.decline";

    private Personality personality = Personality.HONOURABLE;
    private State state = State.IDLE;
    @Nullable
    private UUID challenger;
    private boolean lethal;
    private int stateTicks;
    private int ambushIn;
    private boolean bounty;
    private boolean ally;
    private static final double BOUNTY_SPOT = 12.0;
    private static final float BOUNTY_GROWTH = 1.2F;
    private static final double BOUNTY_MOST_HEALTH = 120.0;

    public StandUserNpc(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    public static void init() {
        EntityEvent.LIVING_HURT.register((victim, source, amount) -> {
            if (!(victim instanceof ServerPlayer player) || amount < player.getHealth()) {
                return EventResult.pass();
            }
            StandUserNpc spared = sparing(player, source);
            if (spared == null) {
                return EventResult.pass();
            }
            spared.won();
            return EventResult.interruptFalse();
        });
    }

    @Nullable
    private static StandUserNpc sparing(ServerPlayer player, DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker == null) {
            return null;
        }
        for (StandUserNpc npc : player.level().getEntitiesOfClass(StandUserNpc.class,
                player.getBoundingBox().inflate(LOST_RANGE), npc -> npc.friendlyDuelWith(player))) {
            if (attacker == npc || attacker == MobStands.standOf(npc)) {
                return npc;
            }
        }
        return null;
    }

    private boolean friendlyDuelWith(Player player) {
        return state == State.DUEL && !lethal && player.getUUID().equals(challenger);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return state == State.IDLE && super.canUse();
            }
        });
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        personality = Personality.values()[random.nextInt(Personality.values().length)];
        MobStands.give(this, MobStands.roll(random), 0);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData);
    }

    public void challenge(ServerPlayer player) {
        challenger = player.getUUID();
        enter(State.APPROACH);
    }

    public Personality personality() {
        return personality;
    }

    public void honourable() {
        personality = Personality.HONOURABLE;
    }

    public void bounty(Component name) {
        bounty = true;
        personality = Personality.HOSTILE;
        setCustomName(name);
        setCustomNameVisible(true);
        settle();
        enter(State.IDLE);
    }

    public boolean isBounty() {
        return bounty;
    }

    public void awaken(boolean friendly, BlockPos home) {
        ally = friendly;
        setCustomName(Component.translatable(friendly ? "town.jojoha.awakened.friendly" : "town.jojoha.awakened.rogue"));
        setCustomNameVisible(true);
        settle();
        if (friendly) {
            personality = Personality.HONOURABLE;
            setHome(home);
        } else {
            bounty = true;
            personality = Personality.HOSTILE;
        }
    }

    @Override
    protected boolean friendly() {
        return ally;
    }

    private void enter(State next) {
        state = next;
        stateTicks = 0;
        if (next == State.LEAVING || next == State.AMBUSH) {
            setTarget(null);
            setLastHurtByMob(null);
        }
    }

    @Nullable
    private ServerPlayer challenger() {
        if (challenger == null || !(level() instanceof ServerLevel level)) {
            return null;
        }
        Player player = level.getPlayerByUUID(challenger);
        return player instanceof ServerPlayer served && served.isAlive() && !served.isSpectator() ? served : null;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel)) {
            return;
        }
        stateTicks++;
        ServerPlayer player = challenger();
        switch (state) {
            case IDLE -> {
                if (bounty && stateTicks % 20 == 0 && level() instanceof ServerLevel level) {
                    Player spotted = level.getNearestPlayer(this, BOUNTY_SPOT);
                    if (spotted instanceof ServerPlayer target && !target.isCreative() && !target.isSpectator()) {
                        challenger = target.getUUID();
                        lethal = true;
                        say(NpcMemory.remembers(this, target) ? "npc.jojoha.stand_user.bounty.again" : "npc.jojoha.stand_user.bounty.spotted",
                                target.getDisplayName());
                        enter(State.DUEL);
                    }
                }
            }
            case APPROACH -> approach(player);
            case OFFER -> {
                if (player == null || (stateTicks > OFFER_LIMIT && !talking()) || distanceTo(player) > WALK_OFF_RANGE) {
                    declined();
                } else {
                    getNavigation().stop();
                    getLookControl().setLookAt(player);
                }
            }
            case DUEL -> duel(player);
            case AMBUSH -> {
                walkAway(player);
                if (--ambushIn <= 0 && player != null) {
                    say("npc.jojoha.stand_user.trickster.ambush");
                    lethal = true;
                    enter(State.DUEL);
                }
            }
            case LEAVING -> {
                walkAway(player);
                setTarget(null);
                if (stateTicks > LEAVE_LIMIT || player == null || distanceTo(player) > LOST_RANGE) {
                    if (bounty || ally) {
                        challenger = null;
                        enter(State.IDLE);
                    } else {
                        discard();
                    }
                }
            }
        }
    }

    private void approach(@Nullable ServerPlayer player) {
        if (player == null || stateTicks > APPROACH_LIMIT) {
            enter(State.LEAVING);
            return;
        }
        if (distanceTo(player) > OFFER_RANGE) {
            if (stateTicks % 20 == 1) {
                getNavigation().moveTo(player, 1.0);
            }
            return;
        }
        getNavigation().stop();
        enter(State.OFFER);
        say("npc.jojoha.stand_user." + key() + ".call", player.getDisplayName());
        if (player.tickCount - player.getLastHurtByMobTimestamp() > CALM_BEFORE_ASKING && player.hurtTime == 0) {
            talkTo(player, offer());
        }
    }

    private void duel(@Nullable ServerPlayer player) {
        if (player == null || distanceTo(player) > LOST_RANGE) {
            if (lethal && challenger != null && level() instanceof ServerLevel level
                    && level.getPlayerByUUID(challenger) instanceof Player fallen && !fallen.isAlive()) {
                say("npc.jojoha.stand_user.gloat");
                if (bounty) {
                    NpcMemory.remember(this, fallen);
                    double grown = Math.min(BOUNTY_MOST_HEALTH, getMaxHealth() * BOUNTY_GROWTH);
                    getAttribute(Attributes.MAX_HEALTH).setBaseValue(grown);
                    setHealth(getMaxHealth());
                }
            }
            enter(State.LEAVING);
            return;
        }
        if (getTarget() != player) {
            setTarget(player);
        }
        if (lethal) {
            return;
        }
        if (getHealth() < getMaxHealth() * YIELD_AT) {
            yielded(player);
        } else if (player.getHealth() < player.getMaxHealth() * SPARE_AT) {
            won();
        }
    }

    private void yielded(ServerPlayer player) {
        say("npc.jojoha.stand_user.yield", player.getDisplayName());
        Quests.duelWon(player, this);
        spawnAtLocation(new ItemStack(ModItems.JOJO_COIN.get(), FEWEST_COINS + random.nextInt(EXTRA_COINS)));
        if (random.nextFloat() < CRYSTAL_CHANCE) {
            spawnAtLocation(new ItemStack(ModItems.STAT_CRYSTAL.get()));
        }
        enter(State.LEAVING);
    }

    private void won() {
        say("npc.jojoha.stand_user.victory");
        if (challenger() != null) {
            Quests.duelLost(challenger(), this);
        }
        enter(State.LEAVING);
    }

    private void declined() {
        switch (personality) {
            case HONOURABLE -> {
                say("npc.jojoha.stand_user.honourable.declined");
                enter(State.LEAVING);
            }
            case HOSTILE -> {
                say("npc.jojoha.stand_user.hostile.declined");
                lethal = true;
                enter(State.DUEL);
            }
            case TRICKSTER -> {
                say("npc.jojoha.stand_user.trickster.declined");
                ambushIn = AMBUSH_FEWEST + random.nextInt(AMBUSH_SPREAD);
                enter(State.AMBUSH);
            }
        }
    }

    private void walkAway(@Nullable ServerPlayer player) {
        if (player == null || stateTicks % 40 != 1) {
            return;
        }
        Vec3 away = DefaultRandomPos.getPosAway(this, 16, 7, player.position());
        if (away != null) {
            getNavigation().moveTo(away.x, away.y, away.z, 1.0);
        }
    }

    private String key() {
        return personality.name().toLowerCase(Locale.ROOT);
    }

    private Line offer() {
        return new Line("npc.jojoha.stand_user." + key() + ".offer", List.of(ACCEPT, DECLINE));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && !ally && (state == State.LEAVING || state == State.AMBUSH || state == State.IDLE
                || state == State.APPROACH || state == State.OFFER)) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide() && source.getEntity() instanceof ServerPlayer player && state != State.DUEL) {
            if (ally) {
                ally = false;
                bounty = true;
                personality = Personality.HOSTILE;
            }
            challenger = player.getUUID();
            lethal = true;
            say("npc.jojoha.stand_user.provoked");
            enter(State.DUEL);
        }
        return hurt;
    }

    @Override
    @Nullable
    protected Line greet(ServerPlayer player) {
        if (ally && state == State.IDLE) {
            return new Line("npc.jojoha.stand_user.awakened.greet", talkReplies());
        }
        if (bounty && state == State.IDLE) {
            challenger = player.getUUID();
            lethal = true;
            say("npc.jojoha.stand_user.bounty.spotted", player.getDisplayName());
            enter(State.DUEL);
            return null;
        }
        switch (state) {
            case IDLE, APPROACH, OFFER -> {
                challenger = player.getUUID();
                enter(State.OFFER);
                return offer();
            }
            case DUEL -> say("npc.jojoha.stand_user.busy");
            case AMBUSH, LEAVING -> say("npc.jojoha.stand_user.farewell");
        }
        return null;
    }

    @Override
    @Nullable
    protected Line answer(ServerPlayer player, String text, String reply) {
        if (state != State.OFFER || !text.equals(offer().text()) || !player.getUUID().equals(challenger)) {
            return null;
        }
        if (ACCEPT.equals(reply)) {
            say(ACCEPT + "ed", player.getDisplayName());
            lethal = false;
            enter(State.DUEL);
        } else if (DECLINE.equals(reply)) {
            declined();
        }
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Personality", personality.name());
        tag.putBoolean("Bounty", bounty);
        tag.putBoolean("Ally", ally);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try {
            personality = Personality.valueOf(tag.getString("Personality"));
        } catch (IllegalArgumentException missing) {
            personality = Personality.HONOURABLE;
        }
        state = State.IDLE;
        bounty = tag.getBoolean("Bounty");
        ally = tag.getBoolean("Ally");
    }
}
