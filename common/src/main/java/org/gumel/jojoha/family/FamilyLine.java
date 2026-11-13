package org.gumel.jojoha.family;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.item.StandArrowRitual;
import org.gumel.jojoha.network.packet.FamilySensePacket;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.Spirit;
import org.gumel.jojoha.stand.StandRelease;
import org.gumel.jojoha.stand.anubis.Anubis;
import org.gumel.jojoha.vampire.VampireFx;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FamilyLine {
    public static final int MOST_HEIRS = 2;

    public static final double SENSE_RANGE = 100.0;
    private static final int SENSE_EVERY_TICKS = 10;

    public static final float STAND_CHANCE = 0.4F;
    public static final int FEVER_TICKS = 72000;
    private static final int FEVER_EVERY_TICKS = 100;
    private static final int FEVER_WEAKNESS_TICKS = 140;
    private static final float FEVER_SPELL_CHANCE = 0.12F;
    private static final int FEVER_SPELL_TICKS = 120;

    public static final float PAIN_CHANCE = 0.25F;
    private static final float PAIN_LEAST_DAMAGE = 2.0F;
    private static final int PAIN_COOLDOWN_TICKS = 600;

    public static final int TAKEOVER_TICKS = 200;
    public static final float THIEF_GAIN = 0.97F;

    private static final int INVITE_TICKS = 1200;

    private record Death(ResourceKey<Level> level, Vec3 at, long when) {
    }

    private record Invite(UUID from, long until) {
    }

    private static final Map<UUID, Death> DEATHS = new HashMap<>();
    private static final Map<UUID, Invite> INVITES = new HashMap<>();
    private static final Map<UUID, Long> PAIN_READY = new HashMap<>();
    private static final Map<UUID, Integer> SENSED = new HashMap<>();

    private FamilyLine() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(FamilyLine::tick);
        PlayerEvent.PLAYER_JOIN.register(FamilyLine::joined);
        PlayerEvent.PLAYER_QUIT.register(player -> {
            SENSED.remove(player.getUUID());
            PAIN_READY.remove(player.getUUID());
        });
        EntityEvent.LIVING_HURT.register((victim, source, amount) -> {
            if (victim instanceof ServerPlayer player && amount >= PAIN_LEAST_DAMAGE) {
                pain(player);
            }
            return EventResult.pass();
        });
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player
                    && !FamilyRecord.of(player.server).family(player.getUUID()).isEmpty()) {
                DEATHS.put(player.getUUID(), new Death(player.level().dimension(), player.position(),
                        player.server.overworld().getGameTime()));
            }
            return EventResult.pass();
        });
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> commands(dispatcher));
    }

    private static void commands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("family")
                .then(Commands.literal("accept").executes(ctx -> {
                    accept(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("decline").executes(ctx -> {
                    decline(ctx.getSource().getPlayerOrException());
                    return 1;
                })));
    }

    private static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private static void tick(MinecraftServer server) {
        long now = now(server);
        FamilyRecord record = FamilyRecord.of(server);
        if (now % SENSE_EVERY_TICKS == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sense(player, record);
            }
        }
        if (now % FEVER_EVERY_TICKS == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                fever(player, record, now);
            }
        }
        DEATHS.values().removeIf(death -> now - death.when() > TAKEOVER_TICKS);
        INVITES.values().removeIf(invite -> now > invite.until());
    }

    private static void joined(ServerPlayer player) {
        FamilyRecord record = FamilyRecord.of(player.server);
        if (record.get(player.getUUID()) != null) {
            record.member(player.getUUID(), player.getGameProfile().getName());
        }
        if (record.takeStir(player.getUUID())) {
            stir(player, record);
        }
    }

    private static void sense(ServerPlayer player, FamilyRecord record) {
        Set<UUID> family = record.family(player.getUUID());
        double nearest = -1.0;
        for (UUID id : family) {
            if (id.equals(player.getUUID())) {
                continue;
            }
            ServerPlayer relative = player.server.getPlayerList().getPlayer(id);
            if (relative == null || relative.level() != player.level() || relative.isSpectator()) {
                continue;
            }
            double away = relative.distanceTo(player);
            if (away <= SENSE_RANGE && (nearest < 0.0 || away < nearest)) {
                nearest = away;
            }
        }
        int bucket = nearest < 0.0 ? -1 : (int) Math.round(nearest);
        Integer last = SENSED.get(player.getUUID());
        if (last != null && last == bucket) {
            return;
        }
        if (last == null && bucket < 0) {
            SENSED.put(player.getUUID(), bucket);
            return;
        }
        SENSED.put(player.getUUID(), bucket);
        NetworkManager.sendToPlayer(player, new FamilySensePacket(bucket));
    }

    private static void pain(ServerPlayer victim) {
        FamilyRecord record = FamilyRecord.of(victim.server);
        Set<UUID> family = record.family(victim.getUUID());
        if (family.isEmpty()) {
            return;
        }
        long now = now(victim.server);
        for (UUID id : family) {
            if (id.equals(victim.getUUID())) {
                continue;
            }
            ServerPlayer relative = victim.server.getPlayerList().getPlayer(id);
            if (relative == null || !relative.isAlive()) {
                continue;
            }
            if (now < PAIN_READY.getOrDefault(id, 0L) || relative.getRandom().nextFloat() >= PAIN_CHANCE) {
                continue;
            }
            PAIN_READY.put(id, now + PAIN_COOLDOWN_TICKS);
            relative.connection.send(new ClientboundHurtAnimationPacket(relative));
            relative.playNotifySound(SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, 0.9F);
            relative.displayClientMessage(Component.translatable("message.jojoha.family.pain",
                    victim.getDisplayName()).withStyle(ChatFormatting.RED), true);
        }
    }

    public static void bloodlineStirs(ServerPlayer stabbed) {
        FamilyRecord record = FamilyRecord.of(stabbed.server);
        for (UUID id : record.descendants(stabbed.getUUID())) {
            ServerPlayer descendant = stabbed.server.getPlayerList().getPlayer(id);
            if (descendant == null) {
                record.markStir(id);
            } else {
                stir(descendant, record);
            }
        }
    }

    private static void stir(ServerPlayer player, FamilyRecord record) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.stand.isPresent() || record.feverUntil(player.getUUID()) > now(player.server)) {
            return;
        }
        if (player.getRandom().nextFloat() < STAND_CHANCE) {
            StandArrowRitual.grant(player, "message.jojoha.family.stand_stirs");
            return;
        }
        record.setFever(player.getUUID(), player.getGameProfile().getName(), now(player.server) + FEVER_TICKS);
        player.sendSystemMessage(Component.translatable("message.jojoha.family.fever_begins")
                .withStyle(ChatFormatting.DARK_RED));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, FEVER_SPELL_TICKS, 0, true, false, true));
    }

    private static void fever(ServerPlayer player, FamilyRecord record, long now) {
        long until = record.feverUntil(player.getUUID());
        if (until <= 0L) {
            return;
        }
        if (now >= until) {
            record.setFever(player.getUUID(), player.getGameProfile().getName(), 0L);
            player.sendSystemMessage(Component.translatable("message.jojoha.family.fever_ends")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!player.isAlive()) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, FEVER_WEAKNESS_TICKS, 0, true, false, true));
        if (player.getRandom().nextFloat() < FEVER_SPELL_CHANCE) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, FEVER_SPELL_TICKS, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, FEVER_SPELL_TICKS, 0, true, false, true));
        }
    }

    public static boolean fevered(ServerPlayer player) {
        return FamilyRecord.of(player.server).feverUntil(player.getUUID()) > now(player.server);
    }

    public static boolean takeOver(ServerPlayer caster, double reach) {
        MinecraftServer server = caster.server;
        long now = now(server);
        UUID bodyId = null;
        double best = reach * reach;
        for (Map.Entry<UUID, Death> entry : DEATHS.entrySet()) {
            Death death = entry.getValue();
            if (entry.getKey().equals(caster.getUUID()) || death.level() != caster.level().dimension()
                    || now - death.when() > TAKEOVER_TICKS) {
                continue;
            }
            double away = death.at().distanceToSqr(caster.position());
            if (away <= best) {
                best = away;
                bodyId = entry.getKey();
            }
        }
        if (bodyId == null) {
            return false;
        }
        Death death = DEATHS.remove(bodyId);
        FamilyRecord record = FamilyRecord.of(server);
        String bodyName = record.nameOf(bodyId);
        String casterName = caster.getGameProfile().getName();

        if (!record.related(caster.getUUID(), bodyId) && record.parentOf(caster.getUUID()) == null) {
            record.bindBody(caster.getUUID(), casterName, bodyId);
        } else {
            record.member(caster.getUUID(), casterName).bodyThief = true;
            record.setDirty();
        }

        boolean stole = steal(caster, server.getPlayerList().getPlayer(bodyId));

        ServerLevel level = caster.serverLevel();
        Vec3 at = death.at();
        for (double up = 0.0; up <= 2.0; up += 0.35) {
            VampireFx.blood(level, at.x, at.y + up, at.z, 8, 0.32, 0.03);
        }
        VampireFx.ring(level, at.x, at.y + 0.15, at.z, 1.8, 2);
        VampireFx.aura(level, at.x, at.y + 1.0, at.z, 24);
        level.playSound(null, caster.blockPosition(), ModSounds.VAMPIRE_REANIMATE.get(), SoundSource.PLAYERS, 1.0F, 0.5F);

        caster.sendSystemMessage(Component.translatable(stole ? "message.jojoha.family.took_body_stand"
                : "message.jojoha.family.took_body", bodyName).withStyle(ChatFormatting.DARK_RED));
        for (UUID id : record.family(bodyId)) {
            if (id.equals(caster.getUUID())) {
                continue;
            }
            ServerPlayer relative = server.getPlayerList().getPlayer(id);
            if (relative != null) {
                relative.sendSystemMessage(Component.translatable("message.jojoha.family.body_taken",
                        caster.getDisplayName(), bodyName).withStyle(ChatFormatting.DARK_RED));
                relative.playNotifySound(ModSounds.VAMPIRE_REANIMATE.get(), SoundSource.PLAYERS, 0.8F, 0.5F);
            }
        }
        return true;
    }

    private static boolean steal(ServerPlayer caster, @Nullable ServerPlayer body) {
        if (body == null) {
            return false;
        }
        JojohaPlayerData mine = PlayerDataAccess.get(caster);
        JojohaPlayerData theirs = PlayerDataAccess.get(body);
        if (mine.stand.isPresent() || !theirs.stand.isPresent()
                || Fate.barredFromStands(mine)
                || Anubis.inStandSlot(theirs)) {
            return false;
        }
        mine.stand = theirs.stand;
        mine.standStatProgress = theirs.standStatProgress.clone();
        Spirit.arrived(caster, mine, mine.stand.standId());
        PlayerDataAccess.set(caster, mine);
        PlayerDataAccess.sync(caster);

        StandRelease.release(body, theirs);
        PlayerDataAccess.set(body, theirs);
        PlayerDataAccess.sync(body);
        return true;
    }

    public static int standGain(ServerPlayer player, int amount) {
        if (amount <= 0 || !FamilyRecord.of(player.server).bodyThief(player.getUUID())) {
            return amount;
        }
        float scaled = amount * THIEF_GAIN;
        int whole = (int) scaled;
        return whole + (player.getRandom().nextFloat() < scaled - whole ? 1 : 0);
    }

    public static void invite(ServerPlayer from, ServerPlayer to) {
        FamilyRecord record = FamilyRecord.of(from.server);
        if (record.heirsOf(from.getUUID()).size() >= MOST_HEIRS) {
            from.displayClientMessage(Component.translatable("message.jojoha.family.has_heir",
                    names(record, record.heirsOf(from.getUUID()))), true);
            return;
        }
        if (record.parentOf(to.getUUID()) != null) {
            from.displayClientMessage(Component.translatable("message.jojoha.family.has_parent", to.getDisplayName()), true);
            return;
        }
        if (record.related(from.getUUID(), to.getUUID())) {
            from.displayClientMessage(Component.translatable("message.jojoha.family.already_family", to.getDisplayName()), true);
            return;
        }
        INVITES.put(to.getUUID(), new Invite(from.getUUID(), now(from.server) + INVITE_TICKS));
        MutableComponent accept = Component.translatable("message.jojoha.family.accept").withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/family accept"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("message.jojoha.family.accept_hover"))));
        MutableComponent decline = Component.translatable("message.jojoha.family.decline").withStyle(style -> style
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/family decline")));
        to.sendSystemMessage(Component.translatable("message.jojoha.family.invited", from.getDisplayName())
                .withStyle(ChatFormatting.GOLD).append(" ").append(accept).append(" ").append(decline));
        from.displayClientMessage(Component.translatable("message.jojoha.family.invite_sent", to.getDisplayName()), true);
        from.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void accept(ServerPlayer player) {
        Invite invite = INVITES.remove(player.getUUID());
        if (invite == null || now(player.server) > invite.until()) {
            player.displayClientMessage(Component.translatable("message.jojoha.family.no_invite"), true);
            return;
        }
        FamilyRecord record = FamilyRecord.of(player.server);
        ServerPlayer from = player.server.getPlayerList().getPlayer(invite.from());
        if (from == null || record.heirsOf(invite.from()).size() >= MOST_HEIRS || record.parentOf(player.getUUID()) != null
                || record.related(invite.from(), player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.jojoha.family.no_invite"), true);
            return;
        }
        record.bindHeir(from.getUUID(), from.getGameProfile().getName(), player.getUUID(), player.getGameProfile().getName());
        from.sendSystemMessage(Component.translatable("message.jojoha.family.joined_parent", player.getDisplayName())
                .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("message.jojoha.family.joined_heir", from.getDisplayName())
                .withStyle(ChatFormatting.GOLD));
        from.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    private static void decline(ServerPlayer player) {
        Invite invite = INVITES.remove(player.getUUID());
        if (invite == null) {
            player.displayClientMessage(Component.translatable("message.jojoha.family.no_invite"), true);
            return;
        }
        ServerPlayer from = player.server.getPlayerList().getPlayer(invite.from());
        if (from != null) {
            from.displayClientMessage(Component.translatable("message.jojoha.family.declined", player.getDisplayName()), true);
        }
        player.displayClientMessage(Component.translatable("message.jojoha.family.you_declined"), true);
    }

    public static boolean isHeir(ServerPlayer player, ServerPlayer other) {
        return FamilyRecord.of(player.server).heirsOf(player.getUUID()).contains(other.getUUID());
    }

    public static void releaseHeir(ServerPlayer player, @Nullable UUID which) {
        FamilyRecord record = FamilyRecord.of(player.server);
        UUID heir = record.releaseHeir(player.getUUID(), which);
        if (heir == null) {
            player.displayClientMessage(Component.translatable("message.jojoha.family.no_heir"), true);
            return;
        }
        player.sendSystemMessage(Component.translatable("message.jojoha.family.released", record.nameOf(heir))
                .withStyle(ChatFormatting.GRAY));
        ServerPlayer gone = player.server.getPlayerList().getPlayer(heir);
        if (gone != null) {
            gone.sendSystemMessage(Component.translatable("message.jojoha.family.released_you", player.getDisplayName())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static Component names(FamilyRecord record, List<UUID> ids) {
        MutableComponent names = Component.empty();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                names.append(", ");
            }
            names.append(record.nameOf(ids.get(i)));
        }
        return names;
    }

    public static void show(ServerPlayer player) {
        FamilyRecord record = FamilyRecord.of(player.server);
        UUID id = player.getUUID();
        Set<UUID> family = record.family(id);
        player.sendSystemMessage(Component.translatable("message.jojoha.family.header").withStyle(ChatFormatting.GOLD));
        if (family.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.jojoha.family.alone").withStyle(ChatFormatting.GRAY));
            return;
        }
        UUID parent = record.parentOf(id);
        List<UUID> heirs = record.heirsOf(id);
        player.sendSystemMessage(Component.translatable("message.jojoha.family.ancestor",
                parent == null ? Component.translatable("message.jojoha.family.none") : Component.literal(record.nameOf(parent)))
                .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.translatable("message.jojoha.family.descendant",
                heirs.isEmpty() ? Component.translatable("message.jojoha.family.none") : names(record, heirs))
                .withStyle(ChatFormatting.GRAY));
        MutableComponent names = Component.empty();
        boolean first = true;
        for (UUID member : family) {
            if (!first) {
                names.append(", ");
            }
            first = false;
            names.append(Component.literal(record.nameOf(member))
                    .withStyle(member.equals(id) ? ChatFormatting.WHITE : ChatFormatting.GRAY));
        }
        player.sendSystemMessage(Component.translatable("message.jojoha.family.members", names).withStyle(ChatFormatting.GRAY));
        if (record.bodyThief(id)) {
            player.sendSystemMessage(Component.translatable("message.jojoha.family.thief_note").withStyle(ChatFormatting.DARK_RED));
        }
        if (fevered(player)) {
            player.sendSystemMessage(Component.translatable("message.jojoha.family.fever_note").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
