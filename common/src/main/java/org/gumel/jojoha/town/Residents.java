package org.gumel.jojoha.town;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.core.component.DataComponents;
import org.gumel.jojoha.entity.VampireEntity;
import org.gumel.jojoha.entity.npc.HamonStudent;
import org.gumel.jojoha.entity.npc.JojoNpc;
import org.gumel.jojoha.entity.npc.Quests;
import org.gumel.jojoha.mixin.VillagerTradeInvoker;
import org.gumel.jojoha.network.packet.NpcDialoguePacket;
import org.gumel.jojoha.registry.ModEffects;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Residents {
    public static final String TRADE = "town.jojoha.reply.trade";
    public static final String WORK = "town.jojoha.reply.work";
    public static final String REPAIR = "town.jojoha.reply.repair";
    public static final String HEAL = "town.jojoha.reply.heal";
    public static final String RUMOUR = "town.jojoha.reply.rumour";
    public static final String DELIVER = "town.jojoha.reply.deliver";
    public static final String PAY_REPAIR = "town.jojoha.reply.pay_repair";
    public static final String PAY_HEAL = "town.jojoha.reply.pay_heal";
    public static final String TAKE = "town.jojoha.task.take";
    public static final String ANOTHER = "town.jojoha.task.another";
    public static final String DECLINE = "town.jojoha.task.decline";
    public static final String CLAIM = "town.jojoha.task.claim";
    public static final String KEEP = "town.jojoha.task.keep";
    public static final String ASK_TOWN = "town.jojoha.ask.town";
    public static final String ASK_PEOPLE = "town.jojoha.ask.people";
    public static final String ASK_NEWS = "town.jojoha.ask.news";
    public static final String BYE = "npc.jojoha.bye";

    private static final int HOLD_TICKS = 2400;
    private static final int COLD_TRUST = -40;
    private static final int GIFT_FAVOURITE = 6;
    private static final int GIFT_GOOD = 3;
    private static final int GIFT_FOOD = 2;
    private static final int GIFT_BAD = -4;
    private static final int MOST_REPAIR = 6;
    private static final float HEAL_AMOUNT = 8F;
    private static final int HURT_TRUST = -10;
    private static final int HURT_REP = -3;
    private static final int KILL_REP = -15;
    private static final int THEFT_REP = -3;
    private static final int FED_REP = -8;
    private static final int FED_ON_RESIDENT_REP = -15;
    private static final int FED_TRUST = -25;
    private static final double WITNESS_RANGE = 16.0;
    private static final double THEFT_WATCH = 12.0;
    private static final long THEFT_GAP = 1200L;
    private static final long SPEAK_GAP = 60L;
    private static final double SPEAK_RANGE = 24.0;
    private static final int THEFT_LINES = 3;
    private static final int HURT_LINES = 3;
    private static final int WITNESS_LINES = 3;
    private static final int RIVAL_ODDS = 5;
    private static final int THRIVING = 8;

    private record Talk(UUID player, long until) {
    }

    private static final Map<UUID, Talk> TALKING = new HashMap<>();
    private static final Map<UUID, Long> SPOKE = new HashMap<>();
    private static final Map<UUID, Long> CAUGHT = new HashMap<>();

    private Residents() {
    }

    public static void init() {
        InteractionEvent.INTERACT_ENTITY.register(Residents::interact);
        TickEvent.SERVER_POST.register(Residents::hold);
        EntityEvent.LIVING_HURT.register(Residents::hurt);
        EntityEvent.LIVING_DEATH.register(Residents::died);
        PlayerEvent.OPEN_MENU.register(Residents::opened);
    }

    private static EventResult interact(Player player, Entity target, InteractionHand hand) {
        if (!(target instanceof Villager villager) || hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) {
            return EventResult.pass();
        }
        ItemStack held = player.getMainHandItem();
        if (villager.isBaby() || villager.isSleeping() || !villager.isAlive() || held.is(Items.NAME_TAG)
                || held.getItem() instanceof SpawnEggItem) {
            return EventResult.pass();
        }
        if (!(player instanceof ServerPlayer served)) {
            return EventResult.interruptTrue();
        }
        if (villager.isTrading()) {
            return EventResult.pass();
        }
        send(served, villager, greet(served, villager));
        return EventResult.interruptTrue();
    }

    private static void hold(MinecraftServer server) {
        if (TALKING.isEmpty()) {
            return;
        }
        ServerLevel level = server.overworld();
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, Talk>> it = TALKING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Talk> entry = it.next();
            Entity body = level.getEntity(entry.getKey());
            Player player = level.getPlayerByUUID(entry.getValue().player());
            if (!(body instanceof Villager villager) || player == null || now > entry.getValue().until()
                    || villager.distanceToSqr(player) > JojoNpc.TALK_RANGE * JojoNpc.TALK_RANGE) {
                it.remove();
                continue;
            }
            villager.getNavigation().stop();
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true));
        }
    }

    public static JojoNpc.Trait trait(Entity npc) {
        JojoNpc.Trait[] all = JojoNpc.Trait.values();
        return all[Math.floorMod(npc.getUUID().getLeastSignificantBits(), all.length)];
    }

    public static int trust(ServerPlayer player, Villager villager) {
        Settlement town = town(villager);
        int rep = town == null ? 0 : town.reputation(player);
        return Mth.clamp(NpcMemory.trust(villager, player) + rep / 2, JojoNpc.TRUST_MIN, JojoNpc.TRUST_MAX);
    }

    private static JojoNpc.Tier tier(ServerPlayer player, Villager villager) {
        return JojoNpc.Tier.of(trust(player, villager));
    }

    @Nullable
    private static Settlement town(Entity entity) {
        return entity.level() instanceof ServerLevel level ? Settlements.at(level, entity.position()) : null;
    }

    private static void send(ServerPlayer player, Villager villager, @Nullable JojoNpc.Line line) {
        if (line == null) {
            return;
        }
        TALKING.put(villager.getUUID(), new Talk(player.getUUID(), villager.level().getGameTime() + HOLD_TICKS));
        NetworkManager.sendToPlayer(player, new NpcDialoguePacket(villager.getId(), line.text(), line.replies(),
                trust(player, villager), NpcMemory.canGift(villager, player), trait(villager).id()));
    }

    private static boolean trades(Villager villager) {
        VillagerProfession job = villager.getVillagerData().getProfession();
        return job != VillagerProfession.NONE && job != VillagerProfession.NITWIT && !villager.getOffers().isEmpty();
    }

    @Nullable
    private static String service(Villager villager) {
        VillagerProfession job = villager.getVillagerData().getProfession();
        if (job == VillagerProfession.ARMORER || job == VillagerProfession.WEAPONSMITH || job == VillagerProfession.TOOLSMITH) {
            return REPAIR;
        }
        if (job == VillagerProfession.CLERIC) {
            return HEAL;
        }
        if (job == VillagerProfession.LIBRARIAN || job == VillagerProfession.CARTOGRAPHER) {
            return RUMOUR;
        }
        return null;
    }

    private static List<String> mainReplies(Villager villager) {
        List<String> replies = new ArrayList<>();
        replies.add(JojoNpc.TALK);
        if (trades(villager)) {
            replies.add(TRADE);
        }
        String service = service(villager);
        if (service != null) {
            replies.add(service);
        }
        replies.add(WORK);
        replies.add(BYE);
        return replies;
    }

    private static List<String> talkReplies(Villager villager) {
        List<String> replies = new ArrayList<>();
        replies.add(JojoNpc.Small.CHAT.reply());
        replies.add(JojoNpc.Small.JOKE.reply());
        replies.add(JojoNpc.Small.COMPLIMENT.reply());
        replies.add(JojoNpc.Small.ASK.reply());
        replies.add(trait(villager).signature().reply());
        replies.add(JojoNpc.TALK_BACK);
        replies.add(JojoNpc.MORE);
        replies.add(ASK_TOWN);
        replies.add(ASK_PEOPLE);
        replies.add(ASK_NEWS);
        return replies;
    }

    private static JojoNpc.Line greet(ServerPlayer player, Villager villager) {
        Settlement town = town(villager);
        if (NpcMemory.trust(villager, player) <= COLD_TRUST || town != null && town.reputation(player) <= Settlement.REP_DISLIKED * 2) {
            return new JojoNpc.Line("town.jojoha.greet.cold", List.of(BYE));
        }
        if (town != null) {
            Quests.Quest delivered = Quests.delivered(player, town);
            if (delivered != null) {
                return new JojoNpc.Line(delivered.key("done"), List.of(CLAIM));
            }
        }
        for (Quests.Quest quest : Quests.heldFrom(player, Quests.RESIDENT)) {
            if (quest.objective() != Quests.Objective.DELIVER && Quests.ready(player, quest)) {
                return new JojoNpc.Line(quest.key("done"), quest.choice() ? List.of(CLAIM, KEEP) : List.of(CLAIM));
            }
        }
        String key;
        if (town != null && town.ravaged()) {
            key = "town.jojoha.greet.ravaged";
        } else if (tier(player, villager).ordinal() >= JojoNpc.Tier.FRIEND.ordinal()) {
            key = "town.jojoha.greet.friend";
        } else {
            key = "town.jojoha.greet." + trait(villager).id();
        }
        return new JojoNpc.Line(key, mainReplies(villager));
    }

    public static void replied(ServerPlayer player, Villager villager, String text, String reply) {
        if (!villager.isAlive() || villager.distanceToSqr(player) > JojoNpc.TALK_RANGE * JojoNpc.TALK_RANGE) {
            return;
        }
        if (JojoNpc.CLOSE.equals(reply)) {
            TALKING.remove(villager.getUUID());
            return;
        }
        if (JojoNpc.TALK.equals(reply)) {
            send(player, villager, new JojoNpc.Line("npc.jojoha.talk.open." + trait(villager).id(), talkReplies(villager)));
            return;
        }
        if (JojoNpc.TALK_BACK.equals(reply)) {
            send(player, villager, greet(player, villager));
            return;
        }
        JojoNpc.Trait mood = trait(villager);
        for (JojoNpc.Small small : JojoNpc.Small.values()) {
            if (small.reply().equals(reply) && (small.basic() || small == mood.signature())) {
                int times = NpcMemory.talkedToday(villager, player, small);
                send(player, villager, JojoNpc.smallTalk(mood, small, trust(player, villager), times, villager.getRandom(),
                        amount -> befriend(player, villager, amount), mood.id(),
                        (topic, signature) -> signature ? topic : mood.id() + "." + topic, talkReplies(villager)));
                return;
            }
        }
        send(player, villager, answer(player, villager, text, reply));
    }

    @Nullable
    private static JojoNpc.Line answer(ServerPlayer player, Villager villager, String text, String reply) {
        switch (reply) {
            case JojoNpc.GIFT -> {
                return gift(player, villager);
            }
            case TRADE -> {
                TALKING.remove(villager.getUUID());
                if (trades(villager) && !villager.isTrading()) {
                    ((VillagerTradeInvoker) villager).jojoha$startTrading(player);
                }
                return null;
            }
            case REPAIR -> {
                return repairOffer(player, villager);
            }
            case PAY_REPAIR -> {
                return repair(player, villager, text);
            }
            case HEAL -> {
                return healOffer(player, villager);
            }
            case PAY_HEAL -> {
                return heal(player, villager, text);
            }
            case RUMOUR -> {
                return work(player, villager, Quests.RUMOUR);
            }
            case WORK -> {
                return work(player, villager, Quests.RESIDENT);
            }
            case TAKE -> {
                Quests.Quest quest = Quests.fromLine(text, "offer");
                if (quest == null || !Quests.take(player, quest, villager)) {
                    return new JojoNpc.Line("town.jojoha.task.cannot", List.of(BYE));
                }
                return new JojoNpc.Line(quest.key("taken"), List.of());
            }
            case ANOTHER -> {
                Quests.Quest current = Quests.fromLine(text, "offer");
                if (current == null) {
                    return work(player, villager, Quests.RESIDENT);
                }
                Quests.Quest next = Quests.nextOffer(player, current.giver(), tier(player, villager), current);
                return next == null ? work(player, villager, current.giver()) : offerLine(player, villager, next);
            }
            case DECLINE -> {
                return new JojoNpc.Line("town.jojoha.task.declined", List.of());
            }
            case CLAIM -> {
                Quests.Quest quest = Quests.fromLine(text, "done");
                if (quest == null || !Quests.claim(player, quest, villager)) {
                    return null;
                }
                return new JojoNpc.Line(quest.key("thanks"), List.of());
            }
            case KEEP -> {
                Quests.Quest quest = Quests.fromLine(text, "done");
                if (quest == null || !Quests.keep(player, quest)) {
                    return null;
                }
                befriend(player, villager, -5);
                return new JojoNpc.Line(quest.key("kept"), List.of());
            }
            case ASK_TOWN -> {
                return new JojoNpc.Line(townLine(villager), talkReplies(villager));
            }
            case ASK_PEOPLE -> {
                return new JojoNpc.Line(peopleLine(villager), talkReplies(villager));
            }
            case ASK_NEWS -> {
                List<String> news = player.getServer() == null ? List.of() : Settlements.news(player.getServer());
                String line = news.isEmpty() ? Settlements.NEWS_NONE
                        : news.get(Math.floorMod(villager.getUUID().getMostSignificantBits(), news.size()));
                return new JojoNpc.Line(line, talkReplies(villager));
            }
            case BYE -> {
                TALKING.remove(villager.getUUID());
                return null;
            }
            default -> {
                return null;
            }
        }
    }

    private static void befriend(ServerPlayer player, Villager villager, int amount) {
        NpcMemory.addTrust(villager, player, amount);
        Settlement town = town(villager);
        if (town != null && Math.abs(amount) >= 3) {
            Settlements.addReputation(player, town, amount / 3);
        }
    }

    private static JojoNpc.Line gift(ServerPlayer player, Villager villager) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !NpcMemory.canGift(villager, player)) {
            return new JojoNpc.Line("npc.jojoha.gift.later", List.of());
        }
        int value;
        if (held.is(Items.EMERALD) || held.is(Items.CAKE)) {
            value = GIFT_FAVOURITE;
        } else if (held.is(ItemTags.FLOWERS)) {
            value = GIFT_GOOD;
        } else if (held.is(Items.ROTTEN_FLESH) || held.is(Items.SPIDER_EYE) || held.is(Items.POISONOUS_POTATO)) {
            value = GIFT_BAD;
        } else {
            value = held.has(DataComponents.FOOD) ? GIFT_FOOD : 1;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        NpcMemory.gifted(villager, player);
        befriend(player, villager, value);
        String reaction = value > 2 ? "liked" : value < 0 ? "disliked" : "neutral";
        return new JojoNpc.Line("npc.jojoha.gift." + reaction, List.of());
    }

    private static JojoNpc.Line offerLine(ServerPlayer player, Villager villager, Quests.Quest offer) {
        if (Quests.poolSize(player, offer.giver(), tier(player, villager)) > 1) {
            return new JojoNpc.Line(offer.key("offer"), List.of(TAKE, ANOTHER, DECLINE));
        }
        return new JojoNpc.Line(offer.key("offer"), List.of(TAKE, DECLINE));
    }

    private static JojoNpc.Line work(ServerPlayer player, Villager villager, String giver) {
        Settlement town = town(villager);
        long seed = town != null ? town.id() * 7919L : villager.getUUID().getLeastSignificantBits();
        if (Quests.RUMOUR.equals(giver)) {
            seed = villager.getUUID().getLeastSignificantBits();
        }
        Quests.Quest offer = Quests.offer(player, seed, giver, tier(player, villager));
        if (offer != null) {
            return offerLine(player, villager, offer);
        }
        List<Quests.Quest> held = Quests.heldFrom(player, Quests.RESIDENT);
        if (!held.isEmpty()) {
            return new JojoNpc.Line(held.get(0).key("progress"), List.of(BYE));
        }
        return new JojoNpc.Line(Quests.RUMOUR.equals(giver) ? "town.jojoha.rumour.none" : "town.jojoha.task.none", List.of(BYE));
    }

    private static int repairCost(ServerPlayer player, Villager villager, ItemStack held) {
        int cost = Mth.clamp((int) Math.ceil(held.getDamageValue() / (double) held.getMaxDamage() * MOST_REPAIR), 1, MOST_REPAIR);
        if (tier(player, villager).ordinal() >= JojoNpc.Tier.FRIEND.ordinal()) {
            cost = Math.max(1, cost - 1);
        }
        return cost;
    }

    private static JojoNpc.Line repairOffer(ServerPlayer player, Villager villager) {
        ItemStack held = player.getMainHandItem();
        if (!held.isDamageableItem() || held.getDamageValue() <= 0) {
            return new JojoNpc.Line("town.jojoha.repair.nothing", mainReplies(villager));
        }
        return new JojoNpc.Line("town.jojoha.repair.offer." + repairCost(player, villager, held), List.of(PAY_REPAIR, BYE));
    }

    private static JojoNpc.Line repair(ServerPlayer player, Villager villager, String text) {
        ItemStack held = player.getMainHandItem();
        if (!text.startsWith("town.jojoha.repair.offer.") || !held.isDamageableItem() || held.getDamageValue() <= 0) {
            return new JojoNpc.Line("town.jojoha.repair.nothing", mainReplies(villager));
        }
        int cost = repairCost(player, villager, held);
        if (!pay(player, cost)) {
            return new JojoNpc.Line("town.jojoha.repair.broke", mainReplies(villager));
        }
        held.setDamageValue(0);
        villager.level().playSound(null, villager.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.6F, 1.2F);
        befriend(player, villager, 1);
        return new JojoNpc.Line("town.jojoha.repair.done", mainReplies(villager));
    }

    private static boolean wounded(ServerPlayer player) {
        return player.hasEffect(ModEffects.bleed()) || player.getHealth() < player.getMaxHealth();
    }

    private static boolean healFree(ServerPlayer player, Villager villager) {
        return tier(player, villager).ordinal() >= JojoNpc.Tier.FRIEND.ordinal();
    }

    private static JojoNpc.Line healOffer(ServerPlayer player, Villager villager) {
        if (!wounded(player)) {
            return new JojoNpc.Line("town.jojoha.heal.nothing", mainReplies(villager));
        }
        return new JojoNpc.Line("town.jojoha.heal.offer." + (healFree(player, villager) ? "free" : "paid"), List.of(PAY_HEAL, BYE));
    }

    private static JojoNpc.Line heal(ServerPlayer player, Villager villager, String text) {
        if (!text.startsWith("town.jojoha.heal.offer.") || !wounded(player)) {
            return new JojoNpc.Line("town.jojoha.heal.nothing", mainReplies(villager));
        }
        if (!healFree(player, villager) && !pay(player, 1)) {
            return new JojoNpc.Line("town.jojoha.repair.broke", mainReplies(villager));
        }
        player.removeEffect(ModEffects.bleed());
        player.heal(HEAL_AMOUNT);
        villager.level().playSound(null, villager.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.NEUTRAL, 0.5F, 1.4F);
        return new JojoNpc.Line("town.jojoha.heal.done", mainReplies(villager));
    }

    private static boolean pay(ServerPlayer player, int emeralds) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        int have = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Items.EMERALD)) {
                have += stack.getCount();
            }
        }
        if (have < emeralds) {
            return false;
        }
        int owed = emeralds;
        for (ItemStack stack : player.getInventory().items) {
            if (owed > 0 && stack.is(Items.EMERALD)) {
                int take = Math.min(owed, stack.getCount());
                stack.shrink(take);
                owed -= take;
            }
        }
        return true;
    }

    private static String townLine(Villager villager) {
        Settlement town = town(villager);
        if (town == null) {
            return "town.jojoha.town.quiet";
        }
        if (town.ravaged()) {
            return "town.jojoha.town.ravaged";
        }
        if (town.population() < town.peak()) {
            return "town.jojoha.town.growing";
        }
        return town.population() >= THRIVING ? "town.jojoha.town.thriving" : "town.jojoha.town.quiet";
    }

    private static String peopleLine(Villager villager) {
        if (!(villager.level() instanceof ServerLevel level)) {
            return "town.jojoha.people.none";
        }
        Settlement town = town(villager);
        if (town == null) {
            return "town.jojoha.people.none";
        }
        boolean rival = Settlements.residents(level, town).stream().anyMatch(other -> other != villager && rivals(villager, other));
        return rival ? "town.jojoha.people.rival" : "town.jojoha.people.friend";
    }

    public static boolean rivals(Entity first, Entity second) {
        long mix = first.getUUID().getLeastSignificantBits() ^ second.getUUID().getLeastSignificantBits();
        return Math.floorMod(mix, RIVAL_ODDS) == 0;
    }

    public static void speak(LivingEntity speaker, String key) {
        if (!(speaker.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        Long last = SPOKE.get(speaker.getUUID());
        if (last != null && now - last < SPEAK_GAP) {
            return;
        }
        SPOKE.put(speaker.getUUID(), now);
        if (SPOKE.size() > 512) {
            SPOKE.clear();
        }
        Component line = Component.translatable("chat.type.text", speaker.getDisplayName(),
                Component.translatable(key).withStyle(ChatFormatting.WHITE));
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(speaker) <= SPEAK_RANGE * SPEAK_RANGE) {
                player.sendSystemMessage(line);
            }
        }
    }

    private static EventResult hurt(LivingEntity victim, DamageSource source, float amount) {
        if (victim.level().isClientSide() || !(victim instanceof Villager villager)
                || !(source.getEntity() instanceof ServerPlayer player) || player.isCreative()) {
            return EventResult.pass();
        }
        NpcMemory.addTrust(villager, player, HURT_TRUST);
        Settlements.addReputation(player, town(villager), HURT_REP);
        speak(villager, "town.jojoha.hurt." + villager.getRandom().nextInt(HURT_LINES));
        return EventResult.pass();
    }

    private static EventResult died(LivingEntity dead, DamageSource source) {
        if (dead.level().isClientSide()) {
            return EventResult.pass();
        }
        if (dead instanceof Villager villager) {
            if (source.getEntity() instanceof ServerPlayer player && !player.isCreative()) {
                Settlements.addReputation(player, town(villager), KILL_REP);
            }
            NpcMemory.forget(villager);
            TALKING.remove(villager.getUUID());
        } else if (dead instanceof VampireEntity && source.getEntity() instanceof ServerPlayer player) {
            Settlements.vampireSlain(player, dead);
        }
        return EventResult.pass();
    }

    private static void opened(Player player, AbstractContainerMenu menu) {
        if (!(player instanceof ServerPlayer served) || served.isCreative()
                || !(menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu)
                || !(served.level() instanceof ServerLevel level)) {
            return;
        }
        Settlement town = Settlements.at(level, served.position());
        if (town == null || town.reputation(served) >= Settlement.REP_LIKED) {
            return;
        }
        Long last = CAUGHT.get(served.getUUID());
        long now = level.getGameTime();
        if (last != null && now - last < THEFT_GAP) {
            return;
        }
        Villager witness = level.getEntitiesOfClass(Villager.class, served.getBoundingBox().inflate(THEFT_WATCH),
                villager -> villager.isAlive() && !villager.isSleeping() && !villager.isBaby() && villager.hasLineOfSight(served))
                .stream().findFirst().orElse(null);
        if (witness == null) {
            return;
        }
        CAUGHT.put(served.getUUID(), now);
        Settlements.addReputation(served, town, THEFT_REP);
        NpcMemory.addTrust(witness, served, THEFT_REP * 2);
        speak(witness, "town.jojoha.theft." + level.random.nextInt(THEFT_LINES));
    }

    public static void fed(ServerPlayer vampire, LivingEntity prey) {
        if (!(vampire.level() instanceof ServerLevel level)) {
            return;
        }
        Quests.fed(vampire, prey);
        Settlement town = Settlements.at(level, vampire.position());
        List<LivingEntity> witnesses = level.getEntitiesOfClass(LivingEntity.class, vampire.getBoundingBox().inflate(WITNESS_RANGE),
                body -> body != prey && body.isAlive() && (body instanceof Villager || body instanceof HamonStudent)
                        && body.hasLineOfSight(vampire));
        if (witnesses.isEmpty()) {
            return;
        }
        if (prey instanceof Villager) {
            NpcMemory.addTrust(prey, vampire, FED_TRUST);
        }
        Settlements.addReputation(vampire, town, prey instanceof Villager ? FED_ON_RESIDENT_REP : FED_REP);
        for (LivingEntity witness : witnesses) {
            if (witness instanceof HamonStudent student) {
                student.setTarget(vampire);
            } else if (witness instanceof Villager villager) {
                NpcMemory.addTrust(villager, vampire, FED_REP);
                villager.getBrain().setMemoryWithExpiry(MemoryModuleType.HURT_BY_ENTITY, vampire, 100L);
                villager.getBrain().setMemoryWithExpiry(MemoryModuleType.HURT_BY, vampire.damageSources().playerAttack(vampire), 100L);
            }
        }
        speak(witnesses.get(0), "town.jojoha.witness." + level.random.nextInt(WITNESS_LINES));
    }
}
