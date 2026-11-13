package org.gumel.jojoha.town;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import org.gumel.jojoha.combat.VampireBlood;
import org.gumel.jojoha.entity.VampireEntity;
import org.gumel.jojoha.entity.npc.JojoNpc;
import org.gumel.jojoha.entity.npc.NpcStanding;
import org.gumel.jojoha.entity.npc.Quests;
import org.gumel.jojoha.network.packet.NpcDialoguePacket;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class VampireTalk {
    public static final String WORK = "vampire_npc.jojoha.reply.work";
    public static final String COVEN = "vampire_npc.jojoha.reply.coven";
    public static final String HUNGER = "vampire_npc.jojoha.reply.hunger";
    public static final String TAKE = "vampire_npc.jojoha.task.take";
    public static final String ANOTHER = "vampire_npc.jojoha.task.another";
    public static final String DECLINE = "vampire_npc.jojoha.task.decline";
    public static final String CLAIM = "vampire_npc.jojoha.task.claim";
    public static final String BYE = "npc.jojoha.bye";

    private static final int HOLD_TICKS = 2400;
    private static final int GIFT_FAVOURITE = 8;
    private static final int GIFT_GOOD = 3;
    private static final int GIFT_BAD = -5;

    private record Talk(UUID player, long until) {
    }

    private static final Map<UUID, Talk> TALKING = new HashMap<>();

    private VampireTalk() {
    }

    public static void init() {
        InteractionEvent.INTERACT_ENTITY.register(VampireTalk::interact);
        TickEvent.SERVER_POST.register(VampireTalk::hold);
    }

    private static EventResult interact(Player player, Entity target, InteractionHand hand) {
        if (!(target instanceof VampireEntity vampire) || hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) {
            return EventResult.pass();
        }
        if (!(player instanceof ServerPlayer served)) {
            return EventResult.pass();
        }
        if (!VampireBlood.isVampire(served) || !vampire.isAlive() || !vampire.spared(served) || vampire.getTarget() != null) {
            return EventResult.pass();
        }
        send(served, vampire, greet(served, vampire));
        return EventResult.interruptTrue();
    }

    private static void hold(MinecraftServer server) {
        if (TALKING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Talk>> it = TALKING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Talk> entry = it.next();
            Entity body = null;
            for (ServerLevel level : server.getAllLevels()) {
                body = level.getEntity(entry.getKey());
                if (body != null) {
                    break;
                }
            }
            Player player = body == null ? null : body.level().getPlayerByUUID(entry.getValue().player());
            if (!(body instanceof VampireEntity vampire) || player == null || body.level().getGameTime() > entry.getValue().until()
                    || vampire.getTarget() != null || vampire.distanceToSqr(player) > JojoNpc.TALK_RANGE * JojoNpc.TALK_RANGE) {
                it.remove();
                continue;
            }
            vampire.getNavigation().stop();
            vampire.getLookControl().setLookAt(player);
        }
    }

    private static int trust(ServerPlayer player, VampireEntity vampire) {
        return Mth.clamp(NpcMemory.trust(vampire, player) + NpcStanding.vampire(player), JojoNpc.TRUST_MIN, JojoNpc.TRUST_MAX);
    }

    private static JojoNpc.Tier tier(ServerPlayer player, VampireEntity vampire) {
        JojoNpc.Tier tier = JojoNpc.Tier.of(trust(player, vampire));
        if (NpcStanding.vampire(player) >= NpcStanding.FRIENDLY && tier.ordinal() < JojoNpc.Tier.FRIEND.ordinal()) {
            return JojoNpc.Tier.FRIEND;
        }
        return tier;
    }

    private static String kind(VampireEntity vampire) {
        return vampire.kind().name().toLowerCase(Locale.ROOT);
    }

    private static boolean sire(VampireEntity vampire) {
        return vampire.kind() == VampireEntity.Kind.SIRE;
    }

    private static void send(ServerPlayer player, VampireEntity vampire, @Nullable JojoNpc.Line line) {
        if (line == null) {
            return;
        }
        TALKING.put(vampire.getUUID(), new Talk(player.getUUID(), vampire.level().getGameTime() + HOLD_TICKS));
        NetworkManager.sendToPlayer(player, new NpcDialoguePacket(vampire.getId(), line.text(), line.replies(),
                trust(player, vampire), NpcMemory.canGift(vampire, player), Residents.trait(vampire).id()));
    }

    private static List<String> mainReplies(VampireEntity vampire) {
        List<String> replies = new ArrayList<>();
        replies.add(JojoNpc.TALK);
        replies.add(sire(vampire) ? WORK : HUNGER);
        replies.add(COVEN);
        replies.add(BYE);
        return replies;
    }

    private static List<String> talkReplies(VampireEntity vampire) {
        List<String> replies = new ArrayList<>();
        replies.add(JojoNpc.Small.CHAT.reply());
        replies.add(JojoNpc.Small.JOKE.reply());
        replies.add(JojoNpc.Small.COMPLIMENT.reply());
        replies.add(JojoNpc.Small.ASK.reply());
        replies.add(Residents.trait(vampire).signature().reply());
        replies.add(JojoNpc.TALK_BACK);
        return replies;
    }

    private static JojoNpc.Line greet(ServerPlayer player, VampireEntity vampire) {
        for (Quests.Quest quest : Quests.heldFrom(player, Quests.VAMPIRE)) {
            if (sire(vampire) && Quests.ready(player, quest)) {
                return new JojoNpc.Line(quest.key("done"), List.of(CLAIM));
            }
        }
        boolean close = tier(player, vampire).ordinal() >= JojoNpc.Tier.FRIEND.ordinal();
        return new JojoNpc.Line("vampire_npc.jojoha.greet." + kind(vampire) + (close ? ".close" : ""), mainReplies(vampire));
    }

    public static void replied(ServerPlayer player, VampireEntity vampire, String text, String reply) {
        if (!vampire.isAlive() || vampire.distanceToSqr(player) > JojoNpc.TALK_RANGE * JojoNpc.TALK_RANGE) {
            return;
        }
        if (JojoNpc.CLOSE.equals(reply) || BYE.equals(reply)) {
            TALKING.remove(vampire.getUUID());
            return;
        }
        if (JojoNpc.TALK.equals(reply)) {
            send(player, vampire, new JojoNpc.Line("vampire_npc.jojoha.talk.open", talkReplies(vampire)));
            return;
        }
        if (JojoNpc.TALK_BACK.equals(reply)) {
            send(player, vampire, greet(player, vampire));
            return;
        }
        JojoNpc.Trait mood = Residents.trait(vampire);
        for (JojoNpc.Small small : JojoNpc.Small.values()) {
            if (small.reply().equals(reply) && (small.basic() || small == mood.signature())) {
                int times = NpcMemory.talkedToday(vampire, player, small);
                send(player, vampire, JojoNpc.smallTalk(mood, small, trust(player, vampire), times, vampire.getRandom(),
                        amount -> NpcMemory.addTrust(vampire, player, amount), mood.id(),
                        (topic, signature) -> signature ? topic : mood.id() + "." + topic, talkReplies(vampire)));
                return;
            }
        }
        send(player, vampire, answer(player, vampire, text, reply));
    }

    @Nullable
    private static JojoNpc.Line answer(ServerPlayer player, VampireEntity vampire, String text, String reply) {
        switch (reply) {
            case JojoNpc.GIFT -> {
                return gift(player, vampire);
            }
            case HUNGER -> {
                return new JojoNpc.Line("vampire_npc.jojoha.hunger." + kind(vampire), mainReplies(vampire));
            }
            case COVEN -> {
                NpcStanding.announceVampire(player);
                NpcStanding.Level level = NpcStanding.Level.of(NpcStanding.vampire(player));
                return new JojoNpc.Line("vampire_npc.jojoha.coven." + level.name().toLowerCase(Locale.ROOT), mainReplies(vampire));
            }
            case WORK -> {
                return work(player, vampire);
            }
            case TAKE -> {
                Quests.Quest quest = Quests.fromLine(text, "offer");
                if (quest == null || !Quests.take(player, quest, vampire)) {
                    return new JojoNpc.Line("vampire_npc.jojoha.task.cannot", List.of(BYE));
                }
                return new JojoNpc.Line(quest.key("taken"), List.of());
            }
            case ANOTHER -> {
                Quests.Quest current = Quests.fromLine(text, "offer");
                Quests.Quest next = current == null ? null : Quests.nextOffer(player, Quests.VAMPIRE, tier(player, vampire), current);
                return next == null ? work(player, vampire) : offerLine(player, vampire, next);
            }
            case DECLINE -> {
                return new JojoNpc.Line("vampire_npc.jojoha.task.declined", List.of());
            }
            case CLAIM -> {
                Quests.Quest quest = Quests.fromLine(text, "done");
                if (quest == null || !Quests.claim(player, quest, vampire)) {
                    return null;
                }
                return new JojoNpc.Line(quest.key("thanks"), List.of());
            }
            default -> {
                return null;
            }
        }
    }

    private static JojoNpc.Line offerLine(ServerPlayer player, VampireEntity vampire, Quests.Quest offer) {
        if (Quests.poolSize(player, Quests.VAMPIRE, tier(player, vampire)) > 1) {
            return new JojoNpc.Line(offer.key("offer"), List.of(TAKE, ANOTHER, DECLINE));
        }
        return new JojoNpc.Line(offer.key("offer"), List.of(TAKE, DECLINE));
    }

    private static JojoNpc.Line work(ServerPlayer player, VampireEntity vampire) {
        if (!sire(vampire)) {
            return new JojoNpc.Line("vampire_npc.jojoha.task.ask_sire", mainReplies(vampire));
        }
        Quests.Quest offer = Quests.offer(player, vampire.getUUID().getLeastSignificantBits(), Quests.VAMPIRE, tier(player, vampire));
        if (offer != null) {
            return offerLine(player, vampire, offer);
        }
        List<Quests.Quest> held = Quests.heldFrom(player, Quests.VAMPIRE);
        if (!held.isEmpty()) {
            return new JojoNpc.Line(held.get(0).key("progress"), List.of(BYE));
        }
        return new JojoNpc.Line("vampire_npc.jojoha.task.none", List.of(BYE));
    }

    private static JojoNpc.Line gift(ServerPlayer player, VampireEntity vampire) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !NpcMemory.canGift(vampire, player)) {
            return new JojoNpc.Line("npc.jojoha.gift.later", List.of());
        }
        int value;
        if (held.is(Items.BEEF) || held.is(Items.PORKCHOP) || held.is(Items.MUTTON) || held.is(Items.RABBIT) || held.is(Items.CHICKEN)) {
            value = GIFT_GOOD;
        } else if (held.is(Items.WITHER_ROSE) || held.is(Items.GOLD_INGOT)) {
            value = GIFT_FAVOURITE;
        } else if (held.has(DataComponents.FOOD) && !held.is(Items.ROTTEN_FLESH)) {
            value = GIFT_BAD;
        } else {
            value = 1;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        NpcMemory.gifted(vampire, player);
        NpcMemory.addTrust(vampire, player, value);
        String reaction = value > 2 ? "liked" : value < 0 ? "disliked" : "neutral";
        return new JojoNpc.Line("npc.jojoha.gift." + reaction, List.of());
    }
}
