package org.gumel.jojoha.stand.heaven;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.network.packet.HeavenFxPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Servitude {
    public static final double RADIUS = 16.0;
    public static final double HUNT_RANGE = 24.0;
    public static final int SERVICE_TICKS = 1200;
    public static final float SERVANT_DAMAGE = 4F;
    public static final int SERVANT_SWING_TICKS = 20;
    public static final double SERVANT_REACH = 2.2;
    private static final double FOLLOW_GAP = 6.0;
    private static final int TETHER_TICKS = 24;

    private static final Map<UUID, Servant> SERVANTS = new HashMap<>();

    private Servitude() {
    }

    static void init() {
        TickEvent.SERVER_POST.register(Servitude::tick);
    }

    public static int bind(ServerPlayer owner) {
        ServerLevel level = owner.serverLevel();
        long until = level.getGameTime() + SERVICE_TICKS;
        int bound = 0;
        HeavenShow.on(owner, HeavenFxPacket.PULSE, (float) Heaven.range(RADIUS), 0);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(Heaven.range(RADIUS)),
                Servitude::bindable)) {
            SERVANTS.put(mob.getUUID(), new Servant(mob, owner.getUUID(), until, 0L));
            calm(mob, owner);
            mob.setPersistenceRequired();
            level.sendParticles(ParticleTypes.ENCHANT, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(),
                    20, 0.3, 0.3, 0.3, 0.6);
            HeavenShow.between(owner, mob, HeavenFxPacket.TETHER, TETHER_TICKS);
            HeavenShow.on(mob, HeavenFxPacket.SERVANT, 0F, SERVICE_TICKS);
            bound++;
        }
        return bound;
    }

    private static boolean bindable(Mob mob) {
        return mob.isAlive() && !(mob instanceof EnderDragon)
                && !(mob instanceof WitherBoss);
    }

    public static boolean servesAgainst(Entity attacker, LivingEntity victim) {
        Servant servant = attacker == null ? null : SERVANTS.get(attacker.getUUID());
        return servant != null && servant.owner.equals(victim.getUUID());
    }

    public static boolean serves(Entity entity) {
        return entity != null && SERVANTS.containsKey(entity.getUUID());
    }

    private static void tick(MinecraftServer server) {
        if (SERVANTS.isEmpty()) {
            return;
        }
        List<UUID> released = new ArrayList<>();
        Iterator<Map.Entry<UUID, Servant>> iterator = SERVANTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Servant> entry = iterator.next();
            Servant servant = entry.getValue();
            Mob mob = servant.mob;
            ServerPlayer owner = server.getPlayerList().getPlayer(servant.owner);
            if (!mob.isAlive() || mob.isRemoved() || owner == null || owner.level() != mob.level()
                    || mob.level().getGameTime() >= servant.until) {
                released.add(entry.getKey());
                continue;
            }
            entry.setValue(serve(servant, mob, owner));
        }
        for (UUID id : released) {
            Servant servant = SERVANTS.remove(id);
            if (servant != null && servant.mob.isAlive() && servant.mob.getTarget() instanceof Player) {
                servant.mob.setTarget(null);
            }
            if (servant != null && servant.mob.isAlive()) {
                HeavenShow.on(servant.mob, HeavenFxPacket.SERVANT, 0F, 0);
            }
        }
    }

    private static void calm(Mob mob, ServerPlayer owner) {
        if (mob.getTarget() == owner) {
            mob.setTarget(null);
            mob.setAggressive(false);
        }
        if (mob.getLastHurtByMob() == owner) {
            mob.setLastHurtByMob(null);
        }
        if (mob instanceof NeutralMob neutral && owner.getUUID().equals(neutral.getPersistentAngerTarget())) {
            neutral.stopBeingAngry();
        }
        if (mob instanceof Creeper creeper && creeper.getTarget() != owner && creeper.getSwellDir() > 0
                && (creeper.getTarget() == null || !creeper.getTarget().isAlive())) {
            creeper.setSwellDir(-1);
        }
        Brain<?> brain = mob.getBrain();
        if (brain.checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)
                && brain.getMemory(MemoryModuleType.ATTACK_TARGET).filter(owner::equals).isPresent()) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
        if (brain.checkMemory(MemoryModuleType.ANGRY_AT, MemoryStatus.REGISTERED)
                && brain.getMemory(MemoryModuleType.ANGRY_AT).filter(owner.getUUID()::equals).isPresent()) {
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
        }
    }

    private static Servant serve(Servant servant, Mob mob, ServerPlayer owner) {
        calm(mob, owner);
        Player prey = prey(mob, owner);
        if (prey == null) {
            if (mob.getTarget() == owner || mob.getTarget() instanceof Player) {
                mob.setTarget(null);
            }
            if (mob.distanceToSqr(owner) > FOLLOW_GAP * FOLLOW_GAP && mob.tickCount % 10 == 0) {
                mob.getNavigation().moveTo(owner, 1.2);
            }
            return servant;
        }
        if (mob instanceof Monster) {
            if (mob.getTarget() != prey) {
                mob.setTarget(prey);
            }
            return servant;
        }
        if (mob.tickCount % 10 == 0) {
            mob.getNavigation().moveTo(prey, 1.3);
        }
        mob.getLookControl().setLookAt(prey);
        long now = mob.level().getGameTime();
        if (mob.distanceToSqr(prey) <= SERVANT_REACH * SERVANT_REACH && now - servant.lastSwing >= SERVANT_SWING_TICKS) {
            mob.swing(InteractionHand.MAIN_HAND);
            prey.hurt(mob.damageSources().mobAttack(mob), DamageScale.stand(owner, prey,
                    SERVANT_DAMAGE));
            return new Servant(mob, servant.owner, servant.until, now);
        }
        return servant;
    }

    private static Player prey(Mob mob, ServerPlayer owner) {
        double range = HUNT_RANGE;
        AABB around = mob.getBoundingBox().inflate(range);
        Player nearest = null;
        double best = range * range;
        for (Player player : mob.level().getEntitiesOfClass(Player.class, around,
                player -> player != owner && player.isAlive() && !player.isSpectator() && !player.isCreative())) {
            double distance = player.distanceToSqr(mob);
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private record Servant(Mob mob, UUID owner, long until, long lastSwing) {
    }
}
