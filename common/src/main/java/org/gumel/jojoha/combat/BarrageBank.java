package org.gumel.jojoha.combat;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.FlairCuePacket;
import org.gumel.jojoha.pillar.PillarAim;
import org.gumel.jojoha.stand.StandEntity;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class BarrageBank {
    private static final int RELEASE_GAP_TICKS = 10;
    private static final int LONGEST_HOLD_TICKS = 200;
    private static final float SLIVER = 0.01F;
    private static final int HOLD_GAP_TICKS = 5;
    private static final double HOLD_REACH = 3.0;
    private static final double HOLD_ROOM = 0.5;
    private static final double HOLD_PULL = 0.35;
    private static final double HOLD_FASTEST = 0.6;
    private static final double HOLD_BREAK = 8.0;
    private static final float HOLD_WIDEST = 2.0F;
    private static final float TINT_FULL_SHARE = 0.5F;
    private static final double MOB_PULL = 0.8;

    private static final Map<LivingEntity, Owed> OWED = new IdentityHashMap<>();
    private static final Map<LivingEntity, Before> BEFORE = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> SLIPPED = new WeakHashMap<>();

    public static final int SLIP_TICKS = 12;

    private static Entity flurrying;
    private static boolean releasing;

    private BarrageBank() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> {
            for (Iterator<Map.Entry<LivingEntity, Owed>> it = OWED.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<LivingEntity, Owed> entry = it.next();
                LivingEntity victim = entry.getKey();
                Owed owed = entry.getValue();
                if (!victim.isAlive() || victim.isRemoved()) {
                    it.remove();
                    continue;
                }
                long now = victim.level().getGameTime();
                if (now - owed.last >= RELEASE_GAP_TICKS || now - owed.first >= LONGEST_HOLD_TICKS) {
                    it.remove();
                    pay(victim, owed);
                    continue;
                }
                if (owed.holds && now - owed.last <= HOLD_GAP_TICKS) {
                    pin(victim, owed);
                }
            }
        });
    }

    public static void during(Entity attacker, Runnable blow) {
        Entity previous = flurrying;
        flurrying = attacker;
        try {
            blow.run();
        } finally {
            flurrying = previous;
        }
    }

    private static boolean banked(DamageSource source) {
        if (releasing || source.getEntity() == null) {
            return false;
        }
        return BarrageBlow.is(source) || source.getEntity() == flurrying;
    }

    public static void beforeHurt(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide()) {
            return;
        }
        if (!banked(source)) {
            BEFORE.remove(victim);
            Owed owed = OWED.get(victim);
            if (owed != null && !releasing && source.getEntity() == owed.attacker) {
                OWED.remove(victim);
                pay(victim, owed);
            }
            return;
        }
        BEFORE.put(victim, new Before(victim.getHealth(), victim.getAbsorptionAmount(), source));
    }

    public static boolean spares(LivingEntity victim, DamageSource source) {
        Before before = BEFORE.get(victim);
        return before != null && before.source == source;
    }

    public static void afterHurt(LivingEntity victim, DamageSource source) {
        Before before = BEFORE.get(victim);
        if (before == null || before.source != source) {
            return;
        }
        BEFORE.remove(victim);
        float lost = before.health + before.absorption - victim.getHealth() - victim.getAbsorptionAmount();
        if (lost <= 0F) {
            return;
        }
        victim.setHealth(before.health);
        victim.setAbsorptionAmount(before.absorption);
        long now = victim.level().getGameTime();
        Owed owed = OWED.computeIfAbsent(victim, v -> new Owed(source.getEntity(), now, victim));
        owed.amount += lost;
        owed.source = source;
        owed.last = now;
        show(victim, owed.amount);
    }

    public static boolean pinned(LivingEntity victim) {
        Owed owed = OWED.get(victim);
        return owed != null && victim.level().getGameTime() - owed.last <= HOLD_GAP_TICKS;
    }

    public static void keep(LivingEntity victim, Entity attacker) {
        Owed owed = OWED.get(victim);
        if (owed != null && owed.attacker == attacker) {
            owed.last = victim.level().getGameTime();
        }
    }

    public static void slip(LivingEntity victim) {
        Owed owed = OWED.remove(victim);
        if (owed != null) {
            pay(victim, owed);
        }
        SLIPPED.put(victim, victim.level().getGameTime() + SLIP_TICKS);
    }

    public static boolean slipping(LivingEntity victim) {
        Long until = SLIPPED.get(victim);
        if (until == null) {
            return false;
        }
        if (victim.level().getGameTime() >= until) {
            SLIPPED.remove(victim);
            return false;
        }
        return true;
    }

    public static boolean absorbing(LivingEntity victim) {
        return BEFORE.containsKey(victim);
    }

    public static void letGo(LivingEntity victim) {
        Owed owed = OWED.get(victim);
        if (owed != null) {
            owed.holds = false;
        }
    }

    private static boolean holdable(LivingEntity victim, Entity attacker) {
        if (victim instanceof StandEntity || victim.isPassenger() || victim.isVehicle()
                || victim.getBbWidth() > HOLD_WIDEST || PillarAim.boss(victim)) {
            return false;
        }
        if (victim instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return attacker != null && attacker.level() == victim.level();
    }

    private static void pin(LivingEntity victim, Owed owed) {
        Entity attacker = owed.attacker;
        if (attacker == null || attacker.isRemoved() || attacker.level() != victim.level()
                || attacker.distanceToSqr(victim) > HOLD_BREAK * HOLD_BREAK) {
            owed.holds = false;
            return;
        }
        Vec3 look = attacker.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(victim.getX() - attacker.getX(), 0.0, victim.getZ() - attacker.getZ());
        }
        if (flat.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 anchor = attacker.position().add(flat.normalize().scale(owed.distance));
        double pull = victim instanceof Player ? HOLD_PULL : MOB_PULL;
        double dx = (anchor.x - victim.getX()) * pull;
        double dz = (anchor.z - victim.getZ()) * pull;
        double flatSpeed = Math.sqrt(dx * dx + dz * dz);
        if (flatSpeed > HOLD_FASTEST) {
            dx *= HOLD_FASTEST / flatSpeed;
            dz *= HOLD_FASTEST / flatSpeed;
        }
        double dy = Mth.clamp((owed.height - victim.getY()) * pull, -HOLD_FASTEST, HOLD_FASTEST);
        victim.fallDistance = 0F;
        if (victim instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setZza(0F);
            mob.setXxa(0F);
            mob.setYya(0F);
            mob.setDeltaMovement(Vec3.ZERO);
            mob.move(MoverType.SELF, new Vec3(dx, dy, dz));
            return;
        }
        victim.setDeltaMovement(dx, dy, dz);
        victim.hurtMarked = true;
    }

    private static void show(LivingEntity victim, float owed) {
        int percent = Math.round(Mth.clamp(owed / Math.max(1F, victim.getMaxHealth() * TINT_FULL_SHARE), 0F, 1F) * 100F);
        NetworkHandler.broadcastFlair(victim, FlairCuePacket.BANKED, percent);
    }

    private static void pay(LivingEntity victim, Owed owed) {
        show(victim, 0F);
        if (!victim.isAlive() || owed.amount <= 0F) {
            return;
        }
        float left = owed.amount;
        float shield = Math.min(victim.getAbsorptionAmount(), left);
        victim.setAbsorptionAmount(victim.getAbsorptionAmount() - shield);
        left -= shield;
        if (left < victim.getHealth()) {
            victim.setHealth(victim.getHealth() - left);
            victim.level().broadcastDamageEvent(victim, owed.source);
            return;
        }
        victim.setHealth(Math.min(victim.getHealth(), SLIVER));
        victim.invulnerableTime = 0;
        releasing = true;
        try {
            victim.hurt(owed.source, DamageScale.combat(null, victim, Math.max(1F, left)));
        } finally {
            releasing = false;
        }
    }

    private record Before(float health, float absorption, DamageSource source) {
    }

    private static final class Owed {
        private final Entity attacker;
        private final long first;
        private final double distance;
        private final double height;
        private DamageSource source;
        private float amount;
        private long last;
        private boolean holds;

        private Owed(Entity attacker, long first, LivingEntity victim) {
            this.attacker = attacker;
            this.first = first;
            this.last = first;
            this.height = victim.getY();
            this.holds = holdable(victim, attacker);
            double gap = attacker == null ? HOLD_REACH
                    : Math.sqrt(Mth.square(victim.getX() - attacker.getX()) + Mth.square(victim.getZ() - attacker.getZ()));
            double closest = attacker == null ? HOLD_REACH
                    : (attacker.getBbWidth() + victim.getBbWidth()) * 0.5 + HOLD_ROOM;
            this.distance = Mth.clamp(gap, closest, Math.max(closest, HOLD_REACH));
        }
    }
}
