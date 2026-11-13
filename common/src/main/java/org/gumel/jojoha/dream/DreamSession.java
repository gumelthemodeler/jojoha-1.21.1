package org.gumel.jojoha.dream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.DreamMemory;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StatPoints;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.DreamSpeechPacket;
import org.gumel.jojoha.stand.Spirit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class DreamSession {
    static final int GREET_AT = 130;

    private static final int REMIND_EVERY = 900;

    private static final int PATIENCE_TICKS = 20 * 60 * 3;

    public static final int PARTING_TICKS = 40;

    private static final int SWEEP_EVERY = 5;

    static final String LEFT_TAG = "jojoha.dream_left";

    private static final int DUST_QUIET_TICKS = 100;

    private static final int SHORT_PAUSE_TICKS = 30;

    private static final int SETTLE_PAUSE_TICKS = 100;

    static final String OFFERED_TAG = "jojoha.dream_offered";
    private static final double RISE_SPEED = 0.09;

    private enum Stage { ARRIVING, ASKING, PRICING, LEAVING }

    private enum Kind { WISH, PICK, STAT, WHAT, OFFER, MEMORY, ELSE, WAKE }

    static final int XP_PER_WORTH = 20;

    private record Answer(Kind kind, DreamOffering.Wish wish, Component words, ResourceLocation stand, int stat) {
        Answer(Kind kind, DreamOffering.Wish wish, Component words) {
            this(kind, wish, words, null, -1);
        }

        Answer(Kind kind, DreamOffering.Wish wish, Component words, ResourceLocation stand) {
            this(kind, wish, words, stand, -1);
        }

        int tone() {
            return switch (kind) {
                case WISH, PICK, STAT, OFFER, MEMORY -> DreamSpeechPacket.GOLD;
                case WHAT, ELSE -> DreamSpeechPacket.PALE;
                case WAKE -> DreamSpeechPacket.WHITE;
            };
        }
    }

    private final ResourceKey<Level> from;
    private final BlockPos bed;

    private final DreamMemory before;
    private final long today;

    private int ticks;
    private Stage stage = Stage.ARRIVING;

    private DreamOffering.Wish wanted;

    private ResourceLocation aligning;

    private int stat = -1;

    private int memories;

    private int memoryPoints;

    private int turn;
    private List<Answer> answers = List.of();

    private int speakingUntil;

    private int lastActed;
    private int remindedAt;

    private final Set<UUID> ignored = new HashSet<>();
    private int dustSaidAt = -DUST_QUIET_TICKS;

    private int leaveAt = -1;

    private boolean askedWhat;

    private final List<ItemStack> held = new ArrayList<>();

    private int heldAt;
    private boolean shortSaid = true;

    private boolean enoughSaid;

    private String granted = "";
    private String paidWith = "";

    DreamSession(ResourceKey<Level> from, BlockPos bed, DreamMemory before, long today) {
        this.from = from;
        this.bed = bed;
        this.before = before;
        this.today = today;
    }

    public ResourceKey<Level> from() {
        return from;
    }

    public BlockPos bed() {
        return bed;
    }

    void tick(ServerPlayer player) {
        ticks++;

        if (stage == Stage.ARRIVING && ticks >= GREET_AT) {
            lastActed = ticks;
            ask(player, DreamLines.greeting(player, before, today), DreamSpeechPacket.CALM, wishes(player));
        }

        if (ticks % SWEEP_EVERY == 0) {
            catchFalling(player);
        }
        if (stage == Stage.PRICING && ticks % SWEEP_EVERY == 0) {
            sweep(player);
        }
        if (ticks % SWEEP_EVERY == 0) {
            tally(player);
        }

        if (stage == Stage.PRICING && !shortSaid && ticks - heldAt >= SHORT_PAUSE_TICKS) {
            shortSaid = true;
            if (offered() < DreamOffering.price(player, wanted)) {
                say(player, List.of(DreamLines.notEnough(player, wanted, offered())), DreamSpeechPacket.CALM);
            } else if (!enoughSaid) {
                enoughSaid = true;
                say(player, List.of(DreamLines.line("enough")), DreamSpeechPacket.CALM);
            }
        }

        if (stage == Stage.PRICING && wanted != null && offered() > 0 && ticks >= speakingUntil
                && ticks - heldAt >= SETTLE_PAUSE_TICKS && offered() >= DreamOffering.price(player, wanted)) {
            pay(player);
            return;
        }

        if (stage == Stage.PRICING && ticks >= speakingUntil
                && ticks - Math.max(remindedAt, lastActed) >= REMIND_EVERY) {
            remindedAt = ticks;
            say(player, List.of(DreamLines.line("remind")), DreamSpeechPacket.CALM);
        }

        if ((stage == Stage.ASKING || stage == Stage.PRICING) && ticks - lastActed >= PATIENCE_TICKS) {
            part(player, DreamMemory.IGNORED, new ArrayList<>(List.of(DreamLines.line("part.impatient"))),
                    DreamSpeechPacket.COLD);
        }

        if (leaveAt >= 0 && ticks >= leaveAt) {
            DreamRealm.sendHome(player, this);
        }
    }

    private List<Answer> wishes(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        List<Answer> said = new ArrayList<>();
        for (DreamOffering.Wish wish : DreamOffering.Wish.values()) {
            if (DreamOffering.available(data, wish)) {
                said.add(new Answer(Kind.WISH, wish, DreamLines.replyFor(wish, data)));
            }
        }
        if (!askedWhat) {
            said.add(new Answer(Kind.WHAT, null, DreamLines.line("reply.what")));
        }
        said.add(new Answer(Kind.WAKE, null, DreamLines.line("reply.wake")));
        return said;
    }

    private List<Answer> waiting(ServerPlayer player) {
        List<Answer> said = new ArrayList<>();
        if (wanted != null && wanted.scales()) {
            said.add(new Answer(Kind.OFFER, null, DreamLines.line("reply.offer")));
        }
        if (wanted != null && xpTotal(player) >= XP_PER_WORTH) {
            said.add(new Answer(Kind.MEMORY, null, DreamLines.line("reply.memory")));
        }
        said.add(new Answer(Kind.ELSE, null, DreamLines.line("reply.else")));
        said.add(new Answer(Kind.WAKE, null, DreamLines.line("reply.wake")));
        return said;
    }

    private List<Answer> statsToName(JojohaPlayerData data, DreamOffering.Wish wish) {
        boolean stand = wish == DreamOffering.Wish.STAND;
        List<Answer> said = new ArrayList<>();
        for (int each = 0; each < StatPoints.COUNT; each++) {
            if (DreamOffering.hasRoom(data, stand, each)) {
                said.add(new Answer(Kind.STAT, wish,
                        DreamLines.line("reply.stat." + (stand ? "stand." : "player.") + each), null, each));
            }
        }
        said.add(new Answer(Kind.ELSE, null, DreamLines.line("reply.else")));
        return said;
    }

    private int offered() {
        return DreamOffering.worth(held) + memories;
    }

    private boolean full(ServerPlayer player) {
        return wanted != null && offered() > 0
                && offered() >= (wanted.scales() ? DreamOffering.most(player, wanted) : DreamOffering.price(player, wanted));
    }

    private void tally(ServerPlayer player) {
        if (stage == Stage.PRICING && wanted != null) {
            int worth = offered();
            float beyond = wanted.scales() && worth > DreamOffering.price(player, wanted)
                    ? (worth - DreamOffering.price(player, wanted)) / (float) (DreamOffering.most(player, wanted) - DreamOffering.price(player, wanted)) : 0F;
            DreamRealm.tally(player, worth / (float) Math.max(1, DreamOffering.price(player, wanted)), beyond);
        } else {
            DreamRealm.tally(player, 1F, 0F);
        }
    }

    static int xpTotal(ServerPlayer player) {
        int total = 0;
        for (int level = 0; level < player.experienceLevel; level++) {
            total += level >= 30 ? 112 + (level - 30) * 9 : level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
        }
        return total + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    private void takeMemories(ServerPlayer player) {
        int worth = offered();
        int goal = worth < DreamOffering.price(player, wanted) || !wanted.scales() ? DreamOffering.price(player, wanted) : DreamOffering.most(player, wanted);
        int need = goal - worth;
        if (need <= 0) {
            pay(player);
            return;
        }
        int add = Math.min(xpTotal(player), need * XP_PER_WORTH) / XP_PER_WORTH;
        if (add <= 0) {
            ask(player, List.of(DreamLines.line("memory.thin")), DreamSpeechPacket.COLD, waiting(player));
            return;
        }
        player.giveExperiencePoints(-add * XP_PER_WORTH);
        memoryPoints += add * XP_PER_WORTH;
        memories += add;
        heldAt = ticks;
        shortSaid = false;
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.AMBIENT, 0.6F, 0.6F);
        tally(player);
        if (full(player)) {
            pay(player);
            return;
        }
        ask(player, List.of(DreamLines.line("memory.taken")), DreamSpeechPacket.CALM, waiting(player));
    }

    void answer(ServerPlayer player, int answeredTurn, int index) {
        if (answeredTurn != turn || (stage != Stage.ASKING && stage != Stage.PRICING)
                || index < 0 || index >= answers.size()) {
            return;
        }
        Answer chosen = answers.get(index);
        lastActed = ticks;

        JojohaPlayerData data = PlayerDataAccess.get(player);
        switch (chosen.kind()) {
            case WISH -> {
                if (chosen.wish() == DreamOffering.Wish.ALIGN) {
                    ask(player, List.of(DreamLines.line("ask.align")), DreamSpeechPacket.CALM, standsToName());
                    return;
                }
                if (chosen.wish().picksStat()) {
                    ask(player, List.of(DreamLines.line("ask.stat")), DreamSpeechPacket.CALM,
                            statsToName(data, chosen.wish()));
                    return;
                }
                wanted = chosen.wish();
                pricing(player, data);
            }
            case STAT -> {
                stat = chosen.stat();
                wanted = chosen.wish();
                pricing(player, data);
            }
            case MEMORY -> {
                if (wanted != null) {
                    takeMemories(player);
                }
            }
            case PICK -> {
                aligning = chosen.stand();
                wanted = DreamOffering.Wish.ALIGN;
                pricing(player, data);
            }
            case OFFER -> {
                if (wanted != null && offered() >= DreamOffering.price(player, wanted)) {
                    pay(player);
                    return;
                }
                ask(player, List.of(offered() <= 0 ? DreamLines.line("remind")
                                : DreamLines.notEnough(player, wanted, offered())),
                        DreamSpeechPacket.COLD, waiting(player));
            }
            case WHAT -> {
                askedWhat = true;
                Component told = DreamLines.lore(data.dreamMemory.lore());
                data.dreamMemory = data.dreamMemory.toldMore();
                PlayerDataAccess.set(player, data);
                ask(player, List.of(told, DreamLines.line("ask.after_lore")), DreamSpeechPacket.CALM,
                        wishes(player));
            }
            case ELSE -> {
                wanted = null;
                stat = -1;
                stage = Stage.ASKING;
                ask(player, List.of(DreamLines.line("ask.again")), DreamSpeechPacket.CALM, wishes(player));
            }
            case WAKE -> part(player, DreamMemory.WOKE,
                    new ArrayList<>(List.of(DreamLines.line("part.woke"))), DreamSpeechPacket.CALM, true);
        }
    }

    private void pricing(ServerPlayer player, JojohaPlayerData data) {
        stage = Stage.PRICING;
        remindedAt = ticks;
        enoughSaid = false;
        heldAt = ticks;
        if (full(player)) {
            pay(player);
            return;
        }
        Component want = wanted == DreamOffering.Wish.ALIGN ? DreamLines.wantsAligned(aligning)
                : wanted.picksStat() ? DreamLines.wantsStat(wanted, data, stat) : DreamLines.wants(wanted, data);
        List<Component> said = new ArrayList<>(List.of(want));
        if (offered() <= 0) {
            said.add(DreamLines.price(player, wanted));
            if (xpTotal(player) >= XP_PER_WORTH) {
                said.add(DreamLines.line("price.memory"));
            }
        } else if (offered() < DreamOffering.price(player, wanted)) {
            said.add(DreamLines.notEnough(player, wanted, offered()));
        } else {
            enoughSaid = true;
            said.add(DreamLines.line("enough"));
        }
        tally(player);
        ask(player, said, DreamSpeechPacket.CALM, waiting(player));
    }

    private static List<Answer> standsToName() {
        List<Answer> said = new ArrayList<>();
        for (ResourceLocation standId : Spirit.pool()) {
            said.add(new Answer(Kind.PICK, null, DreamLines.line("reply.pick", DreamLines.standName(standId)), standId));
        }
        said.add(new Answer(Kind.ELSE, null, DreamLines.line("reply.else")));
        return said;
    }

    private void sweep(ServerPlayer player) {
        ServerLevel dream = player.serverLevel();
        List<ItemEntity> lying = dream.getEntitiesOfClass(ItemEntity.class, DreamRealm.area(player));
        for (ItemEntity dropped : lying) {
            if (dropped.isRemoved() || dropped.getItem().isEmpty()
                    || dropped.getTags().contains(OFFERED_TAG) || ignored.contains(dropped.getUUID())) {
                continue;
            }

            if (dropped.getTags().contains(LEFT_TAG) || dropped.getAge() > ticks) {
                give(player, dropped.getItem().copy());
                dropped.discard();
                continue;
            }

            if (Worth.perItem(dropped.getItem()) <= 0) {
                leave(dropped);
                if (ticks - dustSaidAt >= DUST_QUIET_TICKS) {
                    dustSaidAt = ticks;
                    lastActed = ticks;
                    say(player, List.of(DreamLines.line("dust")), DreamSpeechPacket.COLD);
                }
                continue;
            }

            take(player, dropped);
        }

        if (full(player)) {
            pay(player);
        }
    }

    private void take(ServerPlayer player, ItemEntity dropped) {
        lastActed = ticks;
        heldAt = ticks;
        shortSaid = false;
        ItemStack given = dropped.getItem().copy();
        dropped.discard();
        held.add(given);
        rise(player.serverLevel(), dropped, given.copyWithCount(1));
    }

    private void pay(ServerPlayer player) {
        lastActed = ticks;
        shortSaid = true;

        DreamOffering.Result result = DreamOffering.settle(player, held, wanted, aligning, stat, memories);
        if (!result.granted()) {
            handBack(player);
            wanted = null;
            stat = -1;
            stage = Stage.ASKING;
            ask(player, List.of(Component.translatable(result.message()), DreamLines.line("ask.again")),
                    DreamSpeechPacket.COLD, wishes(player));
            return;
        }

        memories = 0;
        memoryPoints = 0;
        handBack(player);

        ItemStack paid = result.taken();
        granted = wanted.id();
        paidWith = paid.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(paid.getItem()).toString();

        List<Component> lines = new ArrayList<>();
        lines.add(paid.isEmpty() ? DreamLines.line("taken.memories") : DreamLines.taken(paid, before));
        lines.add(Component.translatable(result.message()));
        lines.addAll(DreamLines.told(result));
        lines.add(DreamLines.line("part.granted"));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.AMBIENT, 0.5F, 1.4F);
        part(player, DreamMemory.GRANTED, lines, DreamSpeechPacket.WARM);
    }

    private static void rise(ServerLevel dream, ItemEntity where, ItemStack paid) {
        ItemEntity rising = new ItemEntity(dream, where.getX(), where.getY() + 0.1, where.getZ(), paid);
        rising.makeFakeItem();
        rising.setNoGravity(true);
        rising.setDeltaMovement(0.0, RISE_SPEED, 0.0);
        rising.addTag(OFFERED_TAG);
        dream.addFreshEntity(rising);
        dream.sendParticles(ParticleTypes.END_ROD, where.getX(), where.getY() + 0.2, where.getZ(),
                24, 0.2, 0.2, 0.2, 0.04);
        dream.playSound(null, where.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.AMBIENT, 0.6F, 0.8F);
    }

    private void handBack(ServerPlayer player) {
        for (ItemStack stack : held) {
            give(player, stack);
        }
        held.clear();
        giveMemoriesBack(player);
    }

    private void giveMemoriesBack(ServerPlayer player) {
        if (memoryPoints > 0) {
            player.giveExperiencePoints(memoryPoints);
        }
        memoryPoints = 0;
        memories = 0;
    }

    private void give(ServerPlayer player, ItemStack giving) {
        if (giving.isEmpty() || (player.getInventory().add(giving) && giving.isEmpty())) {
            return;
        }
        ItemEntity left = player.drop(giving, false);
        if (left != null) {
            leave(left);
        }
    }

    private void leave(ItemEntity lying) {
        lying.addTag(LEFT_TAG);
        lying.setUnlimitedLifetime();
        ignored.add(lying.getUUID());
    }

    private void catchFalling(ServerPlayer player) {
        for (ItemEntity falling : player.serverLevel().getEntitiesOfClass(ItemEntity.class, DreamRealm.area(player),
                item -> item.getY() < DreamRealm.FLOOR_Y - 1 && !item.getTags().contains(OFFERED_TAG))) {
            falling.teleportTo(player.getX(), player.getY() + 0.5, player.getZ());
            falling.setDeltaMovement(Vec3.ZERO);
        }
    }

    private void ask(ServerPlayer player, List<Component> lines, int mood, List<Answer> newAnswers) {
        turn++;
        answers = newAnswers;
        stage = stage == Stage.ARRIVING ? Stage.ASKING : stage;
        send(player, lines, mood, DreamSpeechPacket.GOING_ON);
    }

    private void say(ServerPlayer player, List<Component> lines, int mood) {
        send(player, lines, mood, DreamSpeechPacket.GOING_ON);
    }

    private void part(ServerPlayer player, String outcome, List<Component> lines, int mood) {
        part(player, outcome, lines, mood, false);
    }

    private void part(ServerPlayer player, String outcome, List<Component> lines, int mood, boolean waking) {
        handBack(player);
        stage = Stage.LEAVING;
        turn++;
        answers = List.of();
        remember(player, outcome);
        send(player, lines, mood, waking ? DreamSpeechPacket.WAKING : DreamSpeechPacket.PARTING);
        leaveAt = ticks + DreamPacing.ticks(lines) + PARTING_TICKS;
    }

    private void send(ServerPlayer player, List<Component> lines, int mood, int ending) {
        speakingUntil = ticks + DreamPacing.ticks(lines);
        List<Component> words = new ArrayList<>();
        List<Integer> tones = new ArrayList<>();
        for (Answer answer : answers) {
            words.add(answer.words());
            tones.add(answer.tone());
        }
        NetworkHandler.sendDreamSpeech(player, new DreamSpeechPacket(lines, mood, turn, words, tones, ending));
    }

    private void remember(ServerPlayer player, String outcome) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        String stand = data.stand.isPresent() ? data.stand.standId().toString() : "";
        data.dreamMemory = data.dreamMemory.ended(outcome, granted, paidWith, stand);
        PlayerDataAccess.set(player, data);
    }

    List<ItemStack> close(ServerPlayer player) {
        List<ItemStack> left = new ArrayList<>(held);
        held.clear();
        giveMemoriesBack(player);
        for (ItemEntity lying : player.serverLevel().getEntitiesOfClass(ItemEntity.class, DreamRealm.area(player))) {
            if (lying.isRemoved() || lying.getTags().contains(OFFERED_TAG) || lying.getItem().isEmpty()) {
                continue;
            }
            left.add(lying.getItem().copy());
            lying.discard();
        }
        return left;
    }

    void release(ServerPlayer player) {
        handBack(player);
    }
}
