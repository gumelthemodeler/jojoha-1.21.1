package org.gumel.jojoha.fortune;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.packet.FlairCuePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Kindred {
    public static final double PET_RANGE = 32.0;

    public static final int PET_STRENGTH_TICKS = 40;

    public static final double TAME_REACH = 2.5;

    public static final int TAME_TICKS = 60;

    public static final int CRUEL_WEAKNESS_TICKS = 1200;

    public static final int SHUDDER_TICKS = 24000;

    private static final int PET_EVERY = 20;

    private static final int HEART_EVERY = 20;

    private static final double STILL_DRIFT_SQR = 0.0025;

    private record Taming(UUID animal, int ticks) {
    }

    private static final Map<UUID, Taming> TAMING = new HashMap<>();

    private static final Map<UUID, Vec3> LAST_SPOT = new HashMap<>();

    private static final Map<UUID, Long> NEXT_SHUDDER = new HashMap<>();

    private Kindred() {
    }

    static void tick(ServerPlayer player, JojohaPlayerData data, boolean kindred) {
        if (!kindred) {
            TAMING.remove(player.getUUID());
            LAST_SPOT.remove(player.getUUID());
            return;
        }
        if (player.tickCount % PET_EVERY == 0 && data.standSummoned) {
            strengthen(player);
        }
        befriend(player);
        shudder(player, data);
    }

    public static boolean welcome(Entity mob, LivingEntity avoided) {
        return mob instanceof Animal && !(mob instanceof Enemy) && avoided instanceof ServerPlayer player
                && Fortunes.on() && Fortune.has(PlayerDataAccess.get(player), Fortune.KINDRED);
    }

    public static boolean standPet(Entity attacker) {
        if (!(attacker instanceof OwnableEntity pet) || !(pet.getOwner() instanceof ServerPlayer owner)
                || !Fortunes.on()) {
            return false;
        }
        JojohaPlayerData data = PlayerDataAccess.get(owner);
        return data.standSummoned && Fortune.has(data, Fortune.KINDRED);
    }

    private static void strengthen(ServerPlayer player) {
        for (LivingEntity pet : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(PET_RANGE),
                pet -> pet instanceof OwnableEntity owned && player.getUUID().equals(owned.getOwnerUUID()))) {
            pet.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, PET_STRENGTH_TICKS, 0, true, false, true));
        }
    }

    private static void befriend(ServerPlayer player) {
        UUID id = player.getUUID();
        Vec3 last = LAST_SPOT.put(id, player.position());
        boolean still = last != null && last.distanceToSqr(player.position()) < STILL_DRIFT_SQR
                && player.isShiftKeyDown();
        Animal near = still ? wildNear(player) : null;
        if (near == null) {
            TAMING.remove(id);
            return;
        }
        Taming taming = TAMING.get(id);
        int ticks = taming != null && taming.animal().equals(near.getUUID()) ? taming.ticks() + 1 : 1;
        TAMING.put(id, new Taming(near.getUUID(), ticks));
        ServerLevel level = player.serverLevel();
        if (ticks % HEART_EVERY == 0 && ticks < TAME_TICKS) {
            level.sendParticles(ParticleTypes.HEART, near.getX(), near.getY(1.0), near.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
        }
        if (ticks < TAME_TICKS) {
            return;
        }
        TAMING.remove(id);
        if (near instanceof TamableAnimal tamable) {
            tamable.tame(player);
            tamable.getNavigation().stop();
            tamable.setTarget(null);
            tamable.setOrderedToSit(true);
        } else if (near instanceof AbstractHorse horse) {
            horse.tameWithName(player);
        }
        level.broadcastEntityEvent(near, (byte) 7);
        Fortunes.whisper(player, Component.translatable("message.jojoha.fortune.kindred.tamed", near.getDisplayName()));
    }

    private static Animal wildNear(ServerPlayer player) {
        Animal best = null;
        double bestDistance = TAME_REACH * TAME_REACH;
        for (Animal animal : player.serverLevel().getEntitiesOfClass(Animal.class,
                player.getBoundingBox().inflate(TAME_REACH), Kindred::wild)) {
            double distance = animal.distanceToSqr(player);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = animal;
            }
        }
        return best;
    }

    private static boolean wild(Animal animal) {
        if (!animal.isAlive()) {
            return false;
        }
        if (animal instanceof TamableAnimal tamable) {
            return !tamable.isTame();
        }
        return animal instanceof AbstractHorse horse && !horse.isTamed();
    }

    static void struck(ServerPlayer attacker, LivingEntity victim) {
        if (victim instanceof Animal && !(victim instanceof Enemy)) {
            attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, CRUEL_WEAKNESS_TICKS, 0));
        }
    }

    static void killed(ServerPlayer killer, JojohaPlayerData data, LivingEntity victim) {
        if (!(victim instanceof Animal) || victim instanceof Enemy) {
            return;
        }
        boolean fresh = !shuddering(killer, data);
        data.lastConflict = killer.level().getGameTime();
        PlayerDataAccess.set(killer, data);
        if (fresh) {
            Fortunes.whisper(killer, Component.translatable("message.jojoha.fortune.kindred.killed"));
        }
    }

    private static boolean shuddering(ServerPlayer player, JojohaPlayerData data) {
        long since = player.level().getGameTime() - data.lastConflict;
        return data.lastConflict > 0L && since >= 0L && since < SHUDDER_TICKS;
    }

    private static void shudder(ServerPlayer player, JojohaPlayerData data) {
        if (!shuddering(player, data)) {
            NEXT_SHUDDER.remove(player.getUUID());
            return;
        }
        long now = player.level().getGameTime();
        Long next = NEXT_SHUDDER.get(player.getUUID());
        if (next != null && now < next) {
            return;
        }
        NEXT_SHUDDER.put(player.getUUID(), now + Wicked.ITCH_FEWEST_TICKS
                + player.getRandom().nextInt(Wicked.ITCH_SPREAD_TICKS));
        if (next == null) {
            return;
        }
        NetworkManager.sendToPlayer(player, new FlairCuePacket(player.getId(), FlairCuePacket.ITCH,
                Math.round(Wicked.ITCH_STRENGTH * Wicked.ITCH_STEPS)));
    }

    static void forget(Player player) {
        TAMING.remove(player.getUUID());
        LAST_SPOT.remove(player.getUUID());
        NEXT_SHUDDER.remove(player.getUUID());
    }
}
