package org.gumel.jojoha.fortune;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.ChatEvent;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.LightningEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.accessory.Charms;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.StandCombatHandler;

import java.util.ArrayList;
import java.util.List;

public final class Fortunes {
    private static final int HURT_WINDOW_OPEN = 10;

    private static final String[] DIRECTIONS = {"south", "south_west", "west", "north_west",
            "north", "north_east", "east", "south_east"};

    private Fortunes() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(Fortunes::tick);
        EntityEvent.LIVING_DEATH.register(Fortunes::onDeath);
        InteractionEvent.RIGHT_CLICK_ITEM.register(Fortunes::onUse);
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) ->
                on() ? Gambler.chest(player, hand, pos) : EventResult.pass());
        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) ->
                on() ? Gambler.wager(player, entity, hand) : EventResult.pass());
        ChatEvent.RECEIVED.register((player, message) ->
                on() ? Gambler.spoke(player, message) : EventResult.pass());
        LifecycleEvent.SERVER_STOPPING.register(Gambler::stopping);
        PlayerEvent.PICKUP_ITEM_POST.register((player, item, stack) -> {
            if (on() && player instanceof ServerPlayer picker) {
                Honor.picked(picker, item, stack);
            }
        });
        LightningEvent.STRIKE.register((bolt, level, pos, struck) -> {
            if (on()) {
                Lucky.lightning(level, pos);
            }
        });
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> commands(dispatcher));
        PlayerEvent.PICKUP_ITEM_PRE.register((player, item, stack) ->
                on() && Cursed.pickupRefused(player) ? EventResult.interruptFalse() : EventResult.pass());
        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (on()) {
                Cursed.offer(player, PlayerDataAccess.get(player));
            }
        });
        PlayerEvent.PLAYER_QUIT.register(player -> {
            Cursed.forget(player);
            Idealogue.forget(player);
            Wicked.forget(player);
            Lucky.forget(player);
            Bizarre.forget(player);
            Honor.forget(player);
            Trickster.forget(player);
            Kindred.forget(player);
            Devout.forget(player);
            Gambler.quit(player);
        });
    }

    private static void commands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fortune")
                .then(Commands.literal("curse")
                        .then(Commands.argument("curse", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (int curse : Cursed.ALL) {
                                        builder.suggest(Cursed.name(curse));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> Cursed.take(ctx.getSource().getPlayerOrException(),
                                        Cursed.byName(StringArgumentType.getString(ctx, "curse"))) ? 1 : 0)))
                .then(Commands.literal("sense")
                        .then(Commands.argument("sense", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (int sense : Cursed.SENSES) {
                                        builder.suggest(Cursed.senseName(sense));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> Cursed.lean(ctx.getSource().getPlayerOrException(),
                                        Cursed.senseByName(StringArgumentType.getString(ctx, "sense"))) ? 1 : 0)))
                .then(Commands.literal("pass").executes(ctx -> {
                    Cursed.passOn(ctx.getSource().getPlayerOrException());
                    return 1;
                })));
    }

    public static boolean on() {
        return JojohaServerConfig.FORTUNES.on();
    }

    public static boolean rollIfUnset(ServerPlayer player, JojohaPlayerData data) {
        if (!data.fortune.isEmpty() || !on()) {
            return false;
        }
        Fortune rolled = draw(player, data, null);
        player.sendSystemMessage(Component.translatable("message.jojoha.fortune.rolled",
                Component.translatable(rolled.nameKey()).withStyle(ChatFormatting.GOLD),
                Component.translatable(rolled.descriptionKey())));
        if (rolled == Fortune.LUCKY) {
            player.sendSystemMessage(Component.translatable("message.jojoha.fortune.phobia",
                    Component.translatable(Fortune.phobiaKey(data.phobia)).withStyle(ChatFormatting.RED),
                    Component.translatable(Fortune.phobiaKey(data.phobia) + ".desc")));
        }
        if (rolled == Fortune.CURSED) {
            cursedWith(player, data);
        }
        return true;
    }

    public static Fortune redraw(ServerPlayer player, JojohaPlayerData data) {
        return draw(player, data, Fortune.of(data));
    }

    private static Fortune draw(ServerPlayer player, JojohaPlayerData data, Fortune excluded) {
        List<Fortune> pool = new ArrayList<>(List.of(Fortune.values()));
        pool.remove(excluded);
        Fortune rolled = pool.get(player.getRandom().nextInt(pool.size()));
        data.fortune = rolled.id();
        data.phobia = player.getRandom().nextInt(Fortune.PHOBIAS);
        data.honor = Honor.START;
        data.curses = rolled == Fortune.CURSED ? Cursed.roll(player.getRandom()) : 0;
        data.lastConflict = 0L;
        data.fortuneTally = 0;
        data.fortuneUntil = 0L;
        return rolled;
    }

    public static void cursedWith(ServerPlayer player, JojohaPlayerData data) {
        for (int curse : Cursed.ALL) {
            if ((data.curses & curse) != 0) {
                player.sendSystemMessage(Component.translatable("message.jojoha.curse.rolled",
                        Component.translatable(Cursed.nameKey(curse)).withStyle(ChatFormatting.DARK_RED),
                        Component.translatable(Cursed.nameKey(curse) + ".desc")));
            }
        }
        Cursed.offer(player, data);
    }

    private static void tick(Player player) {
        if (!(player instanceof ServerPlayer server) || !server.isAlive() || !on()) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(server);
        Fortune fortune = Fortune.of(data);
        Lucky.tick(server, data, fortune == Fortune.LUCKY);
        Bizarre.tick(server, data, fortune == Fortune.BIZARRE);
        Honor.tick(server, data, fortune == Fortune.HONORBOUND);
        if (fortune == Fortune.WICKED) {
            Wicked.tick(server);
            Wicked.thirst(server, data);
        }
        Cursed.tick(server, data, fortune == Fortune.CURSED);
        Idealogue.tick(server, data, fortune == Fortune.IDEALOGUE);
        Kindred.tick(server, data, fortune == Fortune.KINDRED);
        Gambler.tick(server, data, fortune == Fortune.GAMBLER);
        Devout.tick(server, data, fortune == Fortune.DEVOUT);
    }

    public static float incoming(LivingEntity victim, DamageSource source, float amount) {
        if (!on() || victim.level().isClientSide() || victim.invulnerableTime > HURT_WINDOW_OPEN) {
            return amount;
        }
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
            JojohaPlayerData data = PlayerDataAccess.get(attacker);
            Fortune fortune = Fortune.of(data);
            if (fortune == Fortune.LUCKY && source.getDirectEntity() == attacker
                    && !StandCombatHandler.standBlow(source)) {
                Lucky.struck(attacker, data);
            } else if (fortune == Fortune.HONORBOUND) {
                Honor.struck(attacker, data, victim, source, amount);
            } else if (fortune == Fortune.KINDRED) {
                Kindred.struck(attacker, victim);
            } else if (fortune == Fortune.DEVOUT) {
                Devout.struck(attacker, data, victim);
            }
            amount = Trickster.dealt(attacker, data, source, amount);
            amount = Idealogue.dealt(attacker, data, victim, amount);
        }
        if (victim instanceof ServerPlayer hurt) {
            JojohaPlayerData data = PlayerDataAccess.get(hurt);
            if (Fortune.has(data, Fortune.LUCKY)) {
                Lucky.wounded(hurt, data, source);
            }
            if (source.getEntity() instanceof ServerPlayer by && by != hurt) {
                Honor.opposed(hurt, by);
            }
            amount = Cursed.incoming(hurt, data, amount);
            if (source.getEntity() instanceof LivingEntity by) {
                amount = Idealogue.taken(hurt, data, by, amount);
            }
        }
        return amount;
    }

    private static EventResult onDeath(LivingEntity entity, DamageSource source) {
        if (on() && source.getEntity() instanceof ServerPlayer killer && killer != entity) {
            Idealogue.killed(killer, entity);
            JojohaPlayerData killerData = PlayerDataAccess.get(killer);
            if (Fortune.has(killerData, Fortune.WICKED)) {
                Wicked.killed(killer, killerData);
            } else if (Fortune.has(killerData, Fortune.KINDRED)) {
                Kindred.killed(killer, killerData, entity);
            } else if (Fortune.has(killerData, Fortune.DEVOUT)) {
                Devout.killed(killer, killerData, entity);
            }
        }
        if (entity instanceof ServerPlayer dying) {
            EventResult miracle = Devout.miracle(dying);
            if (miracle.interruptsFurtherEvaluation()) {
                return miracle;
            }
            Gambler.fell(dying);
            EventResult linger = Cursed.died(dying, source);
            if (linger.interruptsFurtherEvaluation()) {
                return linger;
            }
        }
        if (on() && entity instanceof ServerPlayer player
                && Fortune.has(PlayerDataAccess.get(player), Fortune.HONORBOUND)) {
            Honor.fell(player, source);
        }
        return EventResult.pass();
    }

    private static CompoundEventResult<ItemStack> onUse(Player player, InteractionHand hand) {
        if (on() && player instanceof ServerPlayer server
                && Fortune.has(PlayerDataAccess.get(server), Fortune.HONORBOUND)) {
            Honor.used(server, player.getItemInHand(hand));
        }
        return CompoundEventResult.pass();
    }

    public static float arrowOdds(Player player, JojohaPlayerData data) {
        return Fortune.has(data, Fortune.LUCKY) && Lucky.intact(player, data)
                ? 1F + (Lucky.ARROW_ODDS - 1F) * Charms.fortune(player) : 1F;
    }

    public static void whisper(ServerPlayer player, Component words) {
        player.sendSystemMessage(words.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    public static Component direction(Vec3 from, Vec3 to) {
        double angle = Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG - 90.0;
        int sector = Math.floorMod((int) Math.round(angle / 45.0), DIRECTIONS.length);
        return Component.translatable("fortune.jojoha.dir." + DIRECTIONS[sector]);
    }

    static void modifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount,
                         AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && existing.amount() == amount) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }
}
