package org.gumel.jojoha.entity.npc;

import org.gumel.jojoha.network.packet.PlayerMovePosePacket;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.StandEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.GeoEntity;
import dev.architectury.networking.NetworkManager;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.util.RandomSource;
import net.minecraft.server.MinecraftServer;
import org.gumel.jojoha.town.NpcMemory;
import org.gumel.jojoha.network.packet.NpcDialoguePacket;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.IntConsumer;

public abstract class JojoNpc extends PathfinderMob implements GeoEntity {
    public static final double TALK_RANGE = 8.0;
    public static final double SPEECH_RANGE = 32.0;
    public static final int TRUST_MIN = -100;
    public static final int TRUST_MAX = 100;
    private static final int TALK_HOLD = 2400;
    private static final long GIFT_GAP = 24000L;

    public static final String GIFT = "npc.jojoha.gift";
    public static final String CLOSE = "npc.jojoha.close";
    public static final String TALK = "npc.jojoha.talk";
    public static final String TALK_BACK = "npc.jojoha.talk.back";
    public static final String MORE = "npc.jojoha.more";
    public static final String FOLLOW = "npc.jojoha.bond.follow";
    public static final String WAIT = "npc.jojoha.bond.wait";
    public static final String COME = "npc.jojoha.bond.come";
    public static final String DISMISS = "npc.jojoha.bond.dismiss";
    public static final String SETTLE = "npc.jojoha.bond.settle";
    private static final double FOLLOW_CLOSE = 4.0;
    private static final double FOLLOW_STOP = 2.5;
    private static final double FOLLOW_TELEPORT = 24.0;
    private static final int RECENT_FIGHT = 100;
    private static final double HOME_ROAM = 24.0;
    private static final int BED_SEARCH = 6;
    private static final int DOOR_SEARCH = 8;
    private static final int ROOM_LIGHT = 7;
    private static final double NEIGHBOUR_RANGE = 16.0;
    private static final int NEIGHBOUR_EVERY = 200;
    private static final int TALK_LINES = 2;
    private static final int TIRED_LINES = 2;
    private static final int SIGNATURE_LIMIT = 2;

    public enum Small {
        CHAT, JOKE, COMPLIMENT, ASK, GAME, ADVICE, COMPANY, SHOW_OFF, TRAVELS, VENT;

        public String reply() {
            return TALK + "." + name().toLowerCase(Locale.ROOT);
        }

        public String topic() {
            return reply() + ".topic";
        }

        public boolean basic() {
            return ordinal() <= ASK.ordinal();
        }
    }

    public enum Trait {
        CHEERFUL(Small.JOKE, Small.ASK, Small.GAME),
        SERIOUS(Small.ASK, Small.JOKE, Small.ADVICE),
        SHY(Small.CHAT, Small.COMPLIMENT, Small.COMPANY),
        PROUD(Small.COMPLIMENT, Small.CHAT, Small.SHOW_OFF),
        CURIOUS(Small.CHAT, Small.ASK, Small.TRAVELS),
        GRUMPY(Small.JOKE, Small.COMPLIMENT, Small.VENT);

        private final Small likes;
        private final Small dislikes;
        private final Small signature;

        Trait(Small likes, Small dislikes, Small signature) {
            this.likes = likes;
            this.dislikes = dislikes;
            this.signature = signature;
        }

        public Small likes() {
            return likes;
        }

        public Small dislikes() {
            return dislikes;
        }

        public Small signature() {
            return signature;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String key() {
            return "npc.jojoha.trait." + id();
        }

        @Nullable
        public static Trait byId(String id) {
            for (Trait trait : values()) {
                if (trait.id().equals(id)) {
                    return trait;
                }
            }
            return null;
        }
    }

    private static final List<String> SMALL_REPLIES = List.of(Small.CHAT.reply(), Small.JOKE.reply(),
            Small.COMPLIMENT.reply(), Small.ASK.reply());

    public record Line(String text, List<String> replies) {
    }

    public enum Tier {
        STRANGER(TRUST_MIN),
        ACQUAINTED(15),
        FRIEND(40),
        TRUSTED(75);

        private final int from;

        Tier(int from) {
            this.from = from;
        }

        public static Tier of(int trust) {
            Tier reached = STRANGER;
            for (Tier tier : values()) {
                if (trust >= tier.from) {
                    reached = tier;
                }
            }
            return reached;
        }

        public String key() {
            return "npc.jojoha.tier." + name().toLowerCase(Locale.ROOT);
        }
    }

    @Nullable
    private UUID companionOf;
    private boolean waiting;
    private boolean pilgrim;
    @Nullable
    private Trait trait;
    @Nullable
    private BlockPos home;
    private boolean transientVisitor = true;
    private int talkHold;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID talkingTo;

    protected JojoNpc(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        goalSelector.addGoal(1, new TalkGoal());
        goalSelector.addGoal(3, new CompanionGoal());
        goalSelector.addGoal(6, new HomeGoal());
    }

    private final class CompanionGoal extends Goal {
        CompanionGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player leader = leader();
            return leader != null && !waiting && getTarget() == null && distanceToSqr(leader) > FOLLOW_CLOSE * FOLLOW_CLOSE;
        }

        @Override
        public boolean canContinueToUse() {
            Player leader = leader();
            return leader != null && !waiting && getTarget() == null && distanceToSqr(leader) > FOLLOW_STOP * FOLLOW_STOP;
        }

        @Override
        public void tick() {
            Player leader = leader();
            if (leader == null) {
                return;
            }
            if (distanceToSqr(leader) > FOLLOW_TELEPORT * FOLLOW_TELEPORT && leader.onGround()) {
                teleportTo(leader.getX(), leader.getY(), leader.getZ());
                getNavigation().stop();
            } else if (tickCount % 10 == 0) {
                getNavigation().moveTo(leader, 1.15);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    private final class HomeGoal extends Goal {
        HomeGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (home == null || leader() != null || getTarget() != null || tickCount % 40 != 0) {
                return false;
            }
            double distance = distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            return level().isNight() ? distance > 9.0 : distance > HOME_ROAM * HOME_ROAM;
        }

        @Override
        public boolean canContinueToUse() {
            return home != null && getTarget() == null && !getNavigation().isDone();
        }

        @Override
        public void start() {
            if (home != null) {
                getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.9);
            }
        }
    }

    @Nullable
    public Player leader() {
        if (companionOf == null) {
            return null;
        }
        Player player = level().getPlayerByUUID(companionOf);
        return player != null && player.isAlive() && !player.isSpectator() ? player : null;
    }

    public boolean companion() {
        return companionOf != null;
    }

    public void makePilgrim() {
        pilgrim = true;
    }

    public boolean pilgrim() {
        return pilgrim;
    }

    public void escort(ServerPlayer player) {
        companionOf = player.getUUID();
        waiting = false;
        settle();
    }

    public void release() {
        companionOf = null;
        waiting = false;
    }

    protected void neighbourly(ServerPlayer player) {
    }

    private final class TalkGoal extends Goal {
        TalkGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return talker() != null && getTarget() == null;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            Player player = talker();
            if (player == null) {
                return;
            }
            getNavigation().stop();
            getLookControl().setLookAt(player, 30F, 30F);
            double dx = player.getX() - getX();
            double dz = player.getZ() - getZ();
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
            setYRot(Mth.approachDegrees(getYRot(), yaw, 20F));
            yBodyRot = getYRot();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    @Nullable
    private Player talker() {
        if (talkHold <= 0 || talkingTo == null) {
            return null;
        }
        Player player = level().getPlayerByUUID(talkingTo);
        if (player == null || !player.isAlive() || player.distanceToSqr(this) > TALK_RANGE * TALK_RANGE) {
            return null;
        }
        return player;
    }

    public void stopTalking() {
        talkHold = 0;
        talkingTo = null;
    }

    public int trust(Player player) {
        return NpcMemory.trust(this, player);
    }

    public void addTrust(Player player, int amount) {
        NpcMemory.addTrust(this, player, amount);
    }

    public Tier tier(Player player) {
        Tier reached = Tier.STRANGER;
        for (Tier tier : Tier.values()) {
            if (trust(player) >= tier.from) {
                reached = tier;
            }
        }
        return reached;
    }

    protected int giftValue(ItemStack stack) {
        return 0;
    }

    private Line gift(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !canGift(player)) {
            return new Line("npc.jojoha.gift.later", List.of());
        }
        int value = giftValue(held);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        gifted(player);
        addTrust(player, value == 0 ? 1 : value);
        String reaction = value > 2 ? "liked" : value < 0 ? "disliked" : "neutral";
        return new Line("npc.jojoha.gift." + reaction, List.of());
    }

    public boolean canGift(Player player) {
        return NpcMemory.canGift(this, player);
    }

    public void gifted(Player player) {
        NpcMemory.gifted(this, player);
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    public void setHome(@Nullable BlockPos home) {
        this.home = home;
    }

    public void settle() {
        transientVisitor = false;
        setPersistenceRequired();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return transientVisitor && companionOf == null && super.removeWhenFarAway(distance);
    }

    @Nullable
    protected abstract Line greet(ServerPlayer player);

    @Nullable
    protected abstract Line answer(ServerPlayer player, String text, String reply);

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !isAlive() || held.is(Items.NAME_TAG) || held.getItem() instanceof SpawnEggItem) {
            return super.mobInteract(player, hand);
        }
        if (player instanceof ServerPlayer served) {
            talkTo(served, greet(served));
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    public void talkTo(ServerPlayer player, @Nullable Line line) {
        if (line == null) {
            return;
        }
        talkHold = TALK_HOLD;
        talkingTo = player.getUUID();
        getNavigation().stop();
        getLookControl().setLookAt(player);
        NetworkManager.sendToPlayer(player, new NpcDialoguePacket(getId(), line.text(), line.replies(), trust(player), canGift(player), trait().id()));
    }

    protected List<String> talkTopics() {
        return List.of();
    }

    protected final List<String> talkReplies() {
        List<String> replies = new ArrayList<>(SMALL_REPLIES);
        replies.add(trait().signature().reply());
        replies.add(TALK_BACK);
        return withQuestions(replies);
    }

    protected final List<String> withQuestions(List<String> main) {
        List<String> questions = new ArrayList<>(talkTopics());
        if (friendly()) {
            if (companionOf == null) {
                questions.add(FOLLOW);
            } else {
                questions.add(waiting ? COME : WAIT);
                questions.add(DISMISS);
            }
            if (home == null && !pilgrim) {
                questions.add(SETTLE);
            }
        }
        if (questions.isEmpty()) {
            return main;
        }
        List<String> replies = new ArrayList<>(main);
        replies.add(MORE);
        replies.addAll(questions);
        return replies;
    }

    public void replied(ServerPlayer player, String text, String reply) {
        if (!isAlive() || distanceToSqr(player) > TALK_RANGE * TALK_RANGE) {
            return;
        }
        if (TALK.equals(reply)) {
            talkTo(player, new Line("npc.jojoha.talk.open." + voice(), talkReplies()));
            return;
        }
        if (TALK_BACK.equals(reply)) {
            talkTo(player, greet(player));
            return;
        }
        for (Small small : Small.values()) {
            if (small.reply().equals(reply) && (small.basic() || small == trait().signature())) {
                talkTo(player, smallTalk(player, small));
                return;
            }
        }
        if (CLOSE.equals(reply)) {
            if (player.getUUID().equals(talkingTo)) {
                stopTalking();
            }
            return;
        }
        if (friendly() && (FOLLOW.equals(reply) || WAIT.equals(reply) || COME.equals(reply) || DISMISS.equals(reply)
                || SETTLE.equals(reply))) {
            talkTo(player, bond(player, reply));
            return;
        }
        talkTo(player, GIFT.equals(reply) ? gift(player) : answer(player, text, reply));
    }

    public Trait trait() {
        if (trait == null) {
            trait = Trait.values()[random.nextInt(Trait.values().length)];
        }
        return trait;
    }

    private Line bond(ServerPlayer player, String reply) {
        boolean close = tier(player).ordinal() >= Tier.FRIEND.ordinal();
        MinecraftServer server = player.getServer();
        if (!close || server == null) {
            return new Line("npc.jojoha.bond.not_yet", talkReplies());
        }
        switch (reply) {
            case FOLLOW -> {
                UUID previous = NpcMemory.companion(server, player);
                if (previous != null && !previous.equals(getUUID()) && level() instanceof ServerLevel level
                        && level.getEntity(previous) instanceof JojoNpc old) {
                    old.release();
                }
                NpcMemory.setCompanion(server, player, getUUID());
                companionOf = player.getUUID();
                waiting = false;
                return new Line("npc.jojoha.bond.following", List.of());
            }
            case WAIT -> {
                waiting = true;
                getNavigation().stop();
                return new Line("npc.jojoha.bond.waiting", List.of());
            }
            case COME -> {
                waiting = false;
                return new Line("npc.jojoha.bond.coming", List.of());
            }
            case DISMISS -> {
                release();
                NpcMemory.setCompanion(server, player, null);
                return new Line("npc.jojoha.bond.dismissed", List.of());
            }
            default -> {
                BlockPos bed = freeBed(player);
                if (bed == null) {
                    return new Line("npc.jojoha.bond.no_room", talkReplies());
                }
                setHome(bed);
                settle();
                return new Line("npc.jojoha.bond.settled", List.of());
            }
        }
    }

    @Nullable
    private BlockPos freeBed(ServerPlayer player) {
        Level level = level();
        BlockPos centre = player.blockPosition();
        for (BlockPos at : BlockPos.betweenClosed(centre.offset(-BED_SEARCH, -2, -BED_SEARCH), centre.offset(BED_SEARCH, 2, BED_SEARCH))) {
            BlockState state = level.getBlockState(at);
            if (!(state.getBlock() instanceof BedBlock) || state.getValue(BedBlock.PART) != BedPart.HEAD) {
                continue;
            }
            BlockPos bed = at.immutable();
            boolean taken = !level.getEntitiesOfClass(JojoNpc.class, getBoundingBox().inflate(64.0),
                    other -> other != this && other.home() != null && other.home().distManhattan(bed) <= 2).isEmpty();
            if (taken || level.getBrightness(LightLayer.BLOCK, bed.above()) < ROOM_LIGHT) {
                continue;
            }
            for (BlockPos near : BlockPos.betweenClosed(bed.offset(-DOOR_SEARCH, -2, -DOOR_SEARCH), bed.offset(DOOR_SEARCH, 2, DOOR_SEARCH))) {
                if (level.getBlockState(near).getBlock() instanceof DoorBlock) {
                    return bed;
                }
            }
        }
        return null;
    }

    private Line smallTalk(ServerPlayer player, Small small) {
        int times = NpcMemory.talkedToday(this, player, small);
        return smallTalk(trait(), small, trust(player), times, random, amount -> addTrust(player, amount), voice(),
                this::talkKey, talkReplies());
    }

    public static Line smallTalk(Trait mood, Small small, int trust, int times, RandomSource random, IntConsumer change,
                                 String voice, BiFunction<String, Boolean, String> talkKey, List<String> replies) {
        boolean signature = mood.signature == small;
        boolean liked = signature || mood.likes == small;
        boolean disliked = mood.dislikes == small;
        if (times >= (signature ? SIGNATURE_LIMIT : 3)) {
            return new Line("npc.jojoha.talk.tired." + voice + "." + random.nextInt(TIRED_LINES), replies);
        }
        float chance = (signature ? 0.75F : 0.6F) + trust / 300F + (liked ? 0.25F : 0F) - (disliked ? 0.35F : 0F)
                - times * 0.12F;
        boolean good = random.nextFloat() < Mth.clamp(chance, 0.05F, 0.95F);
        if (good) {
            change.accept(Math.max(1, (signature ? 5 : liked ? 4 : 2) - times));
        } else {
            change.accept(signature ? -1 : disliked ? -3 : -2);
        }
        String topic = small.name().toLowerCase(Locale.ROOT);
        String key = "npc.jojoha.talk." + talkKey.apply(topic, signature) + "." + (good ? "good" : "bad") + "." + random.nextInt(TALK_LINES);
        return new Line(key, replies);
    }

    protected boolean friendly() {
        return false;
    }

    public static boolean allies(Entity a, @Nullable Entity b) {
        Entity left = a instanceof StandEntity stand && stand.getMobOwner() != null ? stand.getMobOwner() : a;
        Entity right = b instanceof StandEntity stand && stand.getMobOwner() != null ? stand.getMobOwner() : b;
        return left != right && left instanceof JojoNpc one && right instanceof JojoNpc two && one.friendly() && two.friendly();
    }

    public void playClip(String clip) {
        NetworkHandler.broadcastEntityPose(this, clip, PlayerMovePosePacket.ONCE);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (allies(this, source.getEntity()) || allies(this, source.getDirectEntity())) {
            return false;
        }
        return super.hurt(source, amount);
    }

    protected String voice() {
        return trait().id();
    }

    protected String talkKey(String topic, boolean signature) {
        return signature ? topic : trait().id() + "." + topic;
    }

    @Nullable
    public Boolean feminineVoice() {
        return null;
    }

    @Nullable
    public SoundEvent voiceBlip() {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    protected boolean talking() {
        return talker() != null;
    }

    public void say(String key, Object... args) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Component line = Component.translatable("chat.type.text", getDisplayName(),
                Component.translatable(key, args).withStyle(ChatFormatting.WHITE));
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(this) <= SPEECH_RANGE * SPEECH_RANGE) {
                player.sendSystemMessage(line);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && tickCount % 10 == 0) {
            guardLeader();
        }
        if (home != null && tickCount % NEIGHBOUR_EVERY == 0 && level() instanceof ServerLevel level) {
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) <= NEIGHBOUR_RANGE * NEIGHBOUR_RANGE
                        && tier(player).ordinal() >= Tier.FRIEND.ordinal()) {
                    neighbourly(player);
                }
            }
        }
        if (talkHold > 0) {
            talkHold--;
            if (talker() == null || getTarget() != null) {
                stopTalking();
            }
        }
    }

    private void guardLeader() {
        Player leader = leader();
        if (leader == null || getTarget() != null && getTarget().isAlive()) {
            return;
        }
        LivingEntity threat = leader.getLastHurtByMob();
        if (threat != null && threat != this && threat.isAlive() && !allies(this, threat)
                && leader.tickCount - leader.getLastHurtByMobTimestamp() < RECENT_FIGHT) {
            setTarget(threat);
            return;
        }
        LivingEntity prey = leader.getLastHurtMob();
        if (prey != null && prey != this && prey.isAlive() && !allies(this, prey) && !(prey instanceof AbstractVillager)
                && leader.tickCount - leader.getLastHurtMobTimestamp() < RECENT_FIGHT) {
            setTarget(prey);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (companionOf != null && getServer() != null && level().getPlayerByUUID(companionOf) instanceof Player leader) {
            NpcMemory.setCompanion(getServer(), leader, null);
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (companionOf != null) {
            tag.putUUID("Companion", companionOf);
        }
        tag.putBoolean("Waiting", waiting);
        tag.putBoolean("Pilgrim", pilgrim);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
        tag.putBoolean("Visitor", transientVisitor);
        tag.putString("Trait", trait().name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        for (Trait each : Trait.values()) {
            if (each.name().equals(tag.getString("Trait"))) {
                trait = each;
            }
        }
        CompoundTag held = tag.getCompound("Trust");
        for (String id : held.getAllKeys()) {
            NpcMemory.seedTrust(this, UUID.fromString(id), held.getInt(id));
        }
        companionOf = tag.hasUUID("Companion") ? tag.getUUID("Companion") : null;
        waiting = tag.getBoolean("Waiting");
        pilgrim = tag.getBoolean("Pilgrim");
        home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
        transientVisitor = !tag.contains("Visitor") || tag.getBoolean("Visitor");
    }
}
