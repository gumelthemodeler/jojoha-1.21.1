package org.gumel.jojoha.stand.anubis;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.dream.DreamRealm;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandRelease;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.mob.MobStands;
import org.gumel.jojoha.stand.passive.SwordsmanshipPassive;
import org.gumel.jojoha.stand.skill.moves.DrawSkill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class Anubis {
    public static final ResourceLocation ID = StandTypes.ANUBIS_ID;

    private static final int BROADCAST_EVERY = 10;

    private static final double BROADCAST_RANGE = 96.0;

    public static final float BLADE_BONUS = 0.10F;

    public static final float CEILING = 10.0F;

    private record Held(ItemStack stack, int seenAt) {
    }

    private static final Map<UUID, Held> HELD = new ConcurrentHashMap<>();

    private static final int HELD_FOR = 40;

    public static volatile Predicate<ItemStack> clientNamed = stack -> false;

    public static volatile Predicate<LivingEntity> clientWields = holder -> false;

    public static volatile Predicate<Entity> clientPossessed = entity -> false;

    private static final Set<Mob> MOBS = Collections.newSetFromMap(new WeakHashMap<>());

    private Anubis() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer server) {
                manifest(server);
                Adaptation.tick(server);
            }
        });
        TickEvent.SERVER_LEVEL_POST.register(Anubis::broadcast);
        Passing.init();
        Bearer.init();
        PlayerEvent.ATTACK_ENTITY.register((player, level, target, hand, hit) -> {
            if (player instanceof ServerPlayer server && target instanceof LivingEntity victim
                    && cutsThroughWalls(server) && !server.hasLineOfSight(victim)) {
                slice(server.serverLevel(), victim);
            }
            return EventResult.pass();
        });
        PlayerEvent.PLAYER_QUIT.register(player -> {
            forgetHolder(player);
            Passing.forget(player);
        });
    }

    public static void forgetHolder(Player player) {
        HELD.remove(player.getUUID());
        DRAWN.remove(player.getUUID());
        Adaptation.forget(player);
        DrawSkill.forget(player);
        SwordsmanshipPassive.forget(player);
    }

    public static boolean isBlade(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemTags.SWORDS);
    }

    public static boolean is(JojohaPlayerData data) {
        return data.anubis;
    }

    public static void bond(JojohaPlayerData data) {
        data.anubis = true;
        data.discipline = Discipline.ANUBIS;
    }

    public static void unbond(ServerPlayer player, JojohaPlayerData data) {
        data.anubis = false;
        if (data.discipline == Discipline.ANUBIS) {
            data.discipline = Discipline.NONE;
        }
        forgetHolder(player);
        SwordsmanshipPassive.clear(player);
    }

    private static boolean settle(ServerPlayer player, JojohaPlayerData data) {
        if (data.anubis && data.discipline == Discipline.HAMON) {
            unbond(player, data);
            Passing.handBack(player);
            player.displayClientMessage(Component.translatable("message.jojoha.anubis.hamon_parts"), false);
            return true;
        }
        if (data.anubis && data.discipline != Discipline.ANUBIS) {
            data.discipline = Discipline.ANUBIS;
            return true;
        }
        if (!data.anubis && data.discipline == Discipline.ANUBIS) {
            data.discipline = Discipline.NONE;
            return true;
        }
        return false;
    }

    public static boolean inStandSlot(JojohaPlayerData data) {
        return data.stand.isPresent() && ID.equals(data.stand.standId());
    }

    public static boolean has(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return is(PlayerDataAccess.get(player));
        }
        return entity instanceof Mob mob && ID.equals(MobStands.standId(mob));
    }

    public static boolean out(ServerPlayer player) {
        return is(PlayerDataAccess.get(player)) && drawn(player);
    }

    private static boolean drawn(ServerPlayer player) {
        return isBlade(player.getMainHandItem()) && player.isAlive() && !player.isSpectator()
                && !DreamRealm.dreaming(player);
    }

    private static final Set<UUID> DRAWN = ConcurrentHashMap.newKeySet();

    public static boolean out(Mob mob) {
        return isBlade(mob.getMainHandItem());
    }

    public static boolean wields(LivingEntity holder) {
        if (holder == null || !isBlade(holder.getMainHandItem())) {
            return false;
        }
        if (holder.level().isClientSide()) {
            return clientWields.test(holder);
        }
        if (holder instanceof ServerPlayer player) {
            return out(player);
        }
        return holder instanceof Mob mob && ID.equals(MobStands.standId(mob));
    }

    private static void manifest(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        boolean changed = false;
        if (inStandSlot(data)) {
            StandRelease.release(player, data);
            data.anubis = true;
            changed = true;
        }
        if (settle(player, data) || changed) {
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
        }
        boolean wanted = is(data) && drawn(player);
        AnubisBlade.tick(player, wanted);
        Possession.tick(player, data, wanted);
        if (!is(data)) {
            if (DRAWN.remove(player.getUUID())) {
                SwordsmanshipPassive.clear(player);
                hold(player, null, player.getServer().getTickCount());
            }
            return;
        }
        hold(player, wanted ? player.getMainHandItem() : null, player.getServer().getTickCount());
        if (wanted) {
            SwordsmanshipPassive.INSTANCE.tick(player, data);
        }
        if (wanted == DRAWN.contains(player.getUUID())) {
            return;
        }
        if (wanted) {
            DRAWN.add(player.getUUID());
            player.serverLevel().playSound(null, player.blockPosition(), ModSounds.ANUBIS_UNSHEATHE.get(),
                    SoundSource.PLAYERS, 0.8F, 1.0F);
        } else {
            DRAWN.remove(player.getUUID());
            SwordsmanshipPassive.clear(player);
            player.serverLevel().playSound(null, player.blockPosition(), ModSounds.ANUBIS_SHEATHE.get(),
                    SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    public static void arm(Mob mob) {
        if (isBlade(mob.getMainHandItem())) {
            return;
        }
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        mob.setDropChance(EquipmentSlot.MAINHAND, 0F);
    }

    public static void noteMob(Mob mob) {
        MOBS.add(mob);
        if (mob.getServer() != null) {
            hold(mob, out(mob) ? mob.getMainHandItem() : null, mob.getServer().getTickCount());
        }
        Bearer.fight(mob);
    }

    private static void hold(LivingEntity holder, ItemStack stack, int now) {
        if (stack == null || stack.isEmpty()) {
            HELD.remove(holder.getUUID());
        } else {
            HELD.put(holder.getUUID(), new Held(stack, now));
        }
    }

    public static boolean named(ItemStack stack) {
        if (stack.isEmpty() || !isBlade(stack)) {
            return false;
        }
        for (Held held : HELD.values()) {
            if (held.stack() == stack) {
                return true;
            }
        }
        return clientNamed.test(stack);
    }

    private static void broadcast(ServerLevel level) {
        if (level.getGameTime() % BROADCAST_EVERY != 0) {
            return;
        }
        List<LivingEntity> wielders = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (is(PlayerDataAccess.get(player))) {
                wielders.add(player);
            }
        }
        for (Mob mob : List.copyOf(MOBS)) {
            if (mob.level() == level && mob.isAlive() && !mob.isRemoved()) {
                wielders.add(mob);
            }
        }
        MOBS.removeIf(mob -> mob.level() == level);
        int now = level.getServer().getTickCount();
        HELD.values().removeIf(held -> now - held.seenAt() > HELD_FOR || held.seenAt() > now);
        if (wielders.isEmpty()) {
            return;
        }
        for (ServerPlayer viewer : level.players()) {
            List<Integer> ids = new ArrayList<>();
            for (LivingEntity wielder : wielders) {
                if (wielder != viewer && wielder.distanceToSqr(viewer) < BROADCAST_RANGE * BROADCAST_RANGE) {
                    ids.add(wielder.getId());
                }
            }
            if (!ids.isEmpty()) {
                NetworkHandler.sendAnubisWielders(viewer, ids);
            }
        }
    }

    public static boolean cutsThroughWalls(JojohaPlayerData data) {
        return is(data);
    }

    public static boolean cutsThroughWalls(ServerPlayer player) {
        return isBlade(player.getMainHandItem()) && cutsThroughWalls(PlayerDataAccess.get(player));
    }

    public static void slice(ServerLevel level, LivingEntity target) {
        double x = target.getX(), y = target.getY() + target.getBbHeight() * 0.6, z = target.getZ();
        double spread = target.getBbWidth() * 0.25;
        for (int i = 0; i < SLICES; i++) {
            level.sendParticles(ModRegistries.ANUBIS_SLICE.get(),
                    x + (level.getRandom().nextDouble() - 0.5) * spread,
                    y + (level.getRandom().nextDouble() - 0.5) * spread,
                    z + (level.getRandom().nextDouble() - 0.5) * spread, 1, 0.0, 0.0, 0.0, 0.0);
        }
        level.playSound(null, target.blockPosition(), ModSounds.ANUBIS_SLASH.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    private static final int SLICES = 2;

    public static float outgoing(DamageSource source, LivingEntity victim, float amount) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker
                || !isBlade(attacker.getMainHandItem())) {
            return amount;
        }
        float bonus = 0F;
        if (attacker instanceof ServerPlayer player) {
            if (!out(player)) {
                return amount;
            }
            bonus += BLADE_BONUS + SwordsmanshipPassive.markBonus(player, victim)
                    + Adaptation.damageBonus(player, victim);
        } else if (attacker instanceof Mob mob && ID.equals(MobStands.standId(mob))) {
            bonus = BLADE_BONUS;
        }
        if (bonus <= 0F) {
            return amount;
        }
        return Math.min(amount * (1F + bonus), Math.max(amount, CEILING));
    }

    public static float incoming(ServerPlayer victim, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == victim || !out(victim)) {
            return amount;
        }
        return Adaptation.struck(victim, attacker, source, amount);
    }

    public static boolean foresees(ServerPlayer victim, DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == victim || !out(victim)
                || !Defence.lastStand(victim, attacker)) {
            return false;
        }
        return Adaptation.foresees(victim, attacker, source);
    }

    public static void parried(ServerPlayer defender, LivingEntity attacker, DamageSource source) {
        if (!out(defender)) {
            return;
        }
        SwordsmanshipPassive.mark(defender, attacker);
        Adaptation.experience(defender, attacker, Adaptation.kindOf(source));
        DrawSkill.parried(defender, attacker);
    }

    public static int windowBonus(ServerPlayer player) {
        if (!out(player)) {
            return 0;
        }
        return Adaptation.windowBonus(player);
    }
}
