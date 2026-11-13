package org.gumel.jojoha.combat;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.accessory.Charms;
import org.gumel.jojoha.accessory.Relics;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.DebugCooldowns;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.item.Curios;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.pillar.PillarMen;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.stand.BarrageStruggle;
import org.gumel.jojoha.stand.ReflexGuard;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.anubis.Anubis;
import org.gumel.jojoha.stand.chariot.Chariot;
import org.gumel.jojoha.stand.passive.DuelistsFocusPassive;
import org.gumel.jojoha.ultimate.AdaptiveDefence;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Defence {
    public enum State {
        IDLE, BLOCK, PARRY, STAGGER, COUNTER, GUARD_BROKEN
    }

    public enum Facing {
        FRONT, SIDE, BACK
    }

    private static final double FRONT_DOT = 0.5;

    private static final double BACK_DOT = -0.3;

    public static final int PARRY_WINDOW_TICKS = 2;

    public static final int PARRY_WHIFF_TICKS = 40;

    public static final int PARRY_LAND_TICKS = 20;

    public static final int PARRY_CHAIN_TICKS = 10;

    public static final int PARRY_CHAIN_MAX = 4;

    public static final int PARRY_CHAIN_WINDOW_TICKS = 60;

    public static final int STAGGER_TICKS = 18;

    private static final double STAGGER_SHOVE = 0.65;

    private static final double CLASH_REACH = 1.2;

    public static final int COUNTER_TICKS = 30;

    public static final float COUNTER_SCALE = 1.25F;

    public static final float COUNTER_CEILING = 10.0F;

    public static final float HEAVY_BREAK_AT = 9.0F;

    public static final int PERFECT_PARRY_TICKS = 1;

    public static final int LAG_ALLOWANCE_TICKS = 3;
    public static final float LAST_STAND_PERCENT = 40F;
    private static final double TICK_MS = 50.0;
    public static final float PERFECT_PARRY_DAMAGE = 2.0F;

    public static final ResourceLocation PARRY_CHARGE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "charge/parry");
    public static final ResourceLocation PARRY_PERFECT =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "charge/parry_perfect");

    public static final int PERFECT_SHOWN_TICKS = 20;

    private static final Map<UUID, Long> PARRY_OPENED = new HashMap<>();
    private static final Map<UUID, Long> PARRY_READY_AT = new HashMap<>();
    private static final Map<UUID, Integer> PARRY_LENGTH = new HashMap<>();
    private static final Map<UUID, Long> COUNTER_UNTIL = new HashMap<>();
    private static final Map<UUID, long[]> PARRY_CHAIN = new HashMap<>();

    private static final Map<UUID, Long> COUNTERED = new HashMap<>();

    private record LateHit(long tick, DamageSource source, float lost) {
    }

    private static final Map<UUID, LateHit> LATE_HITS = new HashMap<>();

    private Defence() {
    }

    public static void init() {
        PlayerEvent.PLAYER_QUIT.register(Defence::forget);
    }

    public static Facing facing(LivingEntity defender, Entity attacker) {
        return attacker == null ? Facing.FRONT : facing(defender, attacker.position());
    }

    public static Facing facing(LivingEntity defender, Vec3 from) {
        Vec3 look = defender.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z);
        Vec3 away = from.subtract(defender.position());
        Vec3 toward = new Vec3(away.x, 0, away.z);
        if (forward.lengthSqr() < 1.0E-6 || toward.lengthSqr() < 1.0E-6) {
            return Facing.FRONT;
        }
        double dot = forward.normalize().dot(toward.normalize());
        return dot >= FRONT_DOT ? Facing.FRONT : dot < BACK_DOT ? Facing.BACK : Facing.SIDE;
    }

    public static boolean guardable(DamageSource source) {
        return source.getDirectEntity() != null;
    }

    private static boolean parryable(DamageSource source) {
        return source.getEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity()
                && !ranged(source);
    }

    private static boolean ranged(DamageSource source) {
        return source.is(DamageTypes.SONIC_BOOM);
    }

    public static boolean openParry(ServerPlayer player, int windowTicks) {
        windowTicks = Charms.parryWindow(player, windowTicks);
        long now = player.level().getGameTime();
        Long readyAt = PARRY_READY_AT.get(player.getUUID());
        if (readyAt != null && now < readyAt && readyAt - now <= STALE_AFTER_TICKS
                && !DebugCooldowns.none()) {
            return false;
        }
        PARRY_OPENED.put(player.getUUID(), now);
        PARRY_LENGTH.put(player.getUUID(), windowTicks);
        PARRY_READY_AT.put(player.getUUID(), now + worstCase(windowTicks));
        publish(player, worstCase(windowTicks), false);
        return true;
    }

    public static void forgetParryWait(ServerPlayer player) {
        PARRY_READY_AT.remove(player.getUUID());
    }

    private static int worstCase(int windowTicks) {
        return windowTicks + PARRY_WHIFF_TICKS;
    }

    private static final int STALE_AFTER_TICKS = 200;

    private static void publish(ServerPlayer player, int readyIn, boolean perfect) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        long now = player.level().getGameTime();
        data.setMoveCooldown(PARRY_CHARGE, now, readyIn);
        if (perfect) {
            data.setMoveCooldown(PARRY_PERFECT, now, PERFECT_SHOWN_TICKS);
        }
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static boolean inParryWindow(ServerPlayer player) {
        Long opened = PARRY_OPENED.get(player.getUUID());
        if (opened == null) {
            return false;
        }
        long age = player.level().getGameTime() - opened;
        return age >= 0 && age < PARRY_LENGTH.getOrDefault(player.getUUID(), PARRY_WINDOW_TICKS);
    }

    public static boolean tryParry(ServerPlayer defender, DamageSource source, int colour) {
        if (!inParryWindow(defender)) {
            return false;
        }
        long age = defender.level().getGameTime() - PARRY_OPENED.get(defender.getUUID());
        return resolve(defender, source, colour, age < PERFECT_PARRY_TICKS);
    }

    public static void struck(ServerPlayer player, DamageSource source, float lost) {
        AdaptiveDefence.struck(player, source, lost);
        if (lost > 0F) {
            Curios.spill(player);
        }
        if (lost <= 0F || !parryable(source) || inParryWindow(player)) {
            return;
        }
        LATE_HITS.put(player.getUUID(), new LateHit(player.level().getGameTime(), source, lost));
    }

    public static int lagAllowance(ServerPlayer player) {
        int latency = (int) Math.ceil(player.connection.latency() / TICK_MS);
        return Math.max(0, Math.min(JojohaServerConfig.PARRY_LAG_ALLOWANCE.whole(), latency));
    }

    public static LivingEntity parryLate(ServerPlayer defender, int colour) {
        LateHit hit = LATE_HITS.remove(defender.getUUID());
        if (hit == null || !defender.isAlive()) {
            return null;
        }
        long behind = defender.level().getGameTime() - hit.tick();
        int allowance = lagAllowance(defender);
        if (behind < 0 || behind > allowance || !resolve(defender, hit.source(), colour, behind == allowance)) {
            return null;
        }
        defender.setHealth(Math.min(defender.getMaxHealth(), defender.getHealth() + hit.lost()));
        return (LivingEntity) hit.source().getEntity();
    }

    private static boolean resolve(ServerPlayer defender, DamageSource source, int colour, boolean onTime) {
        if (BarrageBlow.is(source)) {
            return false;
        }
        if (ranged(source)) {
            return source.getEntity() != null && parryRanged(defender, source.getEntity().position(), colour);
        }
        if (!parryable(source)) {
            return false;
        }
        LivingEntity attacker = (LivingEntity) source.getEntity();
        if (!attacker.isAlive() || facing(defender, attacker) == Facing.BACK) {
            return false;
        }

        boolean perfect = spendWindow(defender, onTime);

        if (perfect) {
            attacker.hurt(defender.damageSources().playerAttack(defender), DamageScale.combat(defender, attacker, PERFECT_PARRY_DAMAGE));
        }
        stagger(defender, attacker);
        PillarMen.parried(defender, attacker);
        COUNTER_UNTIL.put(defender.getUUID(), defender.level().getGameTime() + COUNTER_TICKS);
        Anubis.parried(defender, attacker, source);

        ServerLevel level = defender.serverLevel();
        Vec3 guard = defender.position().add(0, defender.getBbHeight() * 0.6, 0);
        Vec3 struck = attacker.position().add(0, attacker.getBbHeight() * 0.6, 0);
        Vec3 between = struck.subtract(guard);
        Vec3 clash = guard.add(between.scale(Math.min(0.5, CLASH_REACH / Math.max(1.0E-3, between.length()))));
        Impacts.landed(level, attacker, clash,
                attacker.position().subtract(defender.position()),
                ImpactSpec.parry(parriesWithWeapon(defender), perfect), colour);
        NetworkHandler.broadcastParryCaught(defender, attacker.position(), perfect,
                parriesWithWeapon(defender));
        return true;
    }

    private static boolean spendWindow(ServerPlayer defender, boolean onTime) {
        long now = defender.level().getGameTime();
        boolean perfect = SabreParry.perfect(defender, onTime);
        if (perfect) {
            Relics.perfectParry(defender);
        }

        int recovery = PARRY_LAND_TICKS + PARRY_CHAIN_TICKS * chained(defender, now);
        PARRY_LENGTH.put(defender.getUUID(), 0);
        PARRY_READY_AT.put(defender.getUUID(), now + recovery);
        publish(defender, recovery, perfect);
        return perfect;
    }

    private static int chained(ServerPlayer defender, long now) {
        long[] chain = PARRY_CHAIN.get(defender.getUUID());
        int count = chain != null && now - chain[0] <= PARRY_CHAIN_WINDOW_TICKS
                ? (int) Math.min(PARRY_CHAIN_MAX, chain[1] + 1) : 0;
        PARRY_CHAIN.put(defender.getUUID(), new long[] {now, count});
        return count;
    }

    public static boolean parryRanged(ServerPlayer defender, Vec3 from, int colour) {
        if (!inParryWindow(defender) || !parriesWithWeapon(defender) || facing(defender, from) == Facing.BACK) {
            return false;
        }
        long age = defender.level().getGameTime() - PARRY_OPENED.get(defender.getUUID());
        boolean perfect = spendWindow(defender, age < PERFECT_PARRY_TICKS);
        COUNTER_UNTIL.put(defender.getUUID(), defender.level().getGameTime() + COUNTER_TICKS);

        Vec3 guard = defender.position().add(0, defender.getBbHeight() * 0.6, 0);
        Vec3 toward = from.subtract(guard);
        Vec3 clash = toward.lengthSqr() < 1.0E-6 ? guard
                : guard.add(toward.normalize().scale(Math.min(CLASH_REACH, toward.length() * 0.5)));
        Impacts.landed(defender.serverLevel(), clash, toward.scale(-1),
                ImpactSpec.parry(true, perfect), colour);
        NetworkHandler.broadcastParryCaught(defender, from, perfect, true);
        return true;
    }

    public static ProjectileDeflection parryProjectile(
            ServerPlayer defender, Projectile projectile) {
        Vec3 velocity = projectile.getDeltaMovement();
        Vec3 from = velocity.lengthSqr() < 1.0E-6 ? projectile.position()
                : projectile.position().subtract(velocity.normalize().scale(3.0));
        if (!parryRanged(defender, from, Impacts.PLAYER)) {
            return null;
        }
        return (thrown, by, random) -> {
            if (by == null) {
                return;
            }
            double speed = Math.max(thrown.getDeltaMovement().length(), PARRIED_LEAST_SPEED);
            thrown.setDeltaMovement(by.getLookAngle().scale(speed * PARRIED_SPEED_KEPT));
            thrown.hasImpulse = true;
            thrown.setOwner(by);
        };
    }

    public static final double PARRIED_SPEED_KEPT = 0.9;
    private static final double PARRIED_LEAST_SPEED = 0.6;

    private static boolean parriesWithWeapon(ServerPlayer defender) {
        return WeaponClasses.held(defender) != null
                || Anubis.wields(defender)
                || DuelistsFocusPassive.guards(defender)
                || defender.getMainHandItem().getItem() instanceof SwordItem;
    }

    private static void stagger(ServerPlayer defender, LivingEntity attacker) {
        Vec3 away = attacker.position().subtract(defender.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        if (flat.lengthSqr() > 1.0E-6) {
            Vec3 shove = flat.normalize().scale(STAGGER_SHOVE);
            attacker.setDeltaMovement(attacker.getDeltaMovement().add(shove.x, 0.22, shove.z));
            attacker.hurtMarked = true;
        }
        attacker.addEffect(new MobEffectInstance(ModEffects.stun(), STAGGER_TICKS, Stun.TIER_ONE));
    }

    public static float counter(DamageSource source, float amount) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) {
            return amount;
        }
        Long until = COUNTER_UNTIL.get(attacker.getUUID());
        if (until == null) {
            return amount;
        }
        COUNTER_UNTIL.remove(attacker.getUUID());
        if (attacker.level().getGameTime() > until) {
            return amount;
        }
        COUNTERED.put(attacker.getUUID(), attacker.level().getGameTime());
        return Math.min(amount * COUNTER_SCALE, Math.max(amount, COUNTER_CEILING));
    }

    public static boolean lastStand(ServerPlayer player, Entity attacker) {
        return attacker instanceof Mob mob
                && !(mob instanceof OwnableEntity owned
                        && owned.getOwner() instanceof Player)
                && player.getHealth() <= player.getMaxHealth()
                        * JojohaServerConfig.LAST_STAND.value() / 100F;
    }

    public static long counteredAt(ServerPlayer player) {
        Long at = COUNTERED.get(player.getUUID());
        return at == null ? -1L : at;
    }

    public static boolean refuses(LivingEntity victim, DamageSource source, float amount) {
        if (victim.level().isClientSide() || victim.isDeadOrDying() || victim.isInvulnerableTo(source)) {
            return false;
        }

        if (source.getEntity() instanceof LivingEntity striker && source.getDirectEntity() == striker
                && BarrageStruggle.in(striker)) {
            return true;
        }

        if (Chariot.refuses(victim, source)) {
            return true;
        }

        if (AdaptiveDefence.refuses(victim, source)) {
            return true;
        }

        if (victim instanceof ServerPlayer duelist && guardable(source)
                && DuelistsFocusPassive.guards(duelist) && Parry.meets(duelist, source)) {
            return true;
        }

        EventResult result = StandCombatHandler.handleLivingHurt(victim, source, amount);
        if (result.isFalse()) {
            return true;
        }

        if (victim instanceof ServerPlayer seer && guardable(source)
                && Anubis.foresees(seer, source)) {
            return true;
        }

        if (victim instanceof ServerPlayer parrying && guardable(source) && Parry.meets(parrying, source)) {
            return true;
        }

        if (victim instanceof ServerPlayer ready && ReflexGuard.catches(ready, source)) {
            return true;
        }
        return false;
    }

    public static float scale(LivingEntity victim, DamageSource source, float amount) {
        float dealt = counter(source, amount);
        dealt = Relics.outgoing(victim, source, dealt);
        dealt = PillarMen.adjust(victim, source, dealt);
        dealt = AdaptiveDefence.incoming(victim, source, dealt);
        if (victim.level().isClientSide() || !(victim instanceof ServerPlayer player) || !guardable(source)) {
            return dealt;
        }

        JojohaPlayerData data = PlayerDataAccess.get(player);
        Facing facing = facing(player, source.getEntity());
        if (data.standGuarding && facing == Facing.SIDE) {
            dealt *= STAND_SIDE_TAKEN;
        }
        dealt *= Chariot.takenScale(player);
        return Anubis.incoming(player, source, dealt);
    }

    public static final float STAND_SIDE_TAKEN = 0.5F;

    public static State stateOf(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (GuardBreak.isBroken(player) || data.guardBreakCooldownTicks > 0) {
            return State.GUARD_BROKEN;
        }
        if (Stun.isStunned(player)) {
            return State.STAGGER;
        }
        boolean guarding = data.standGuarding;
        if (guarding && inParryWindow(player)) {
            return State.PARRY;
        }
        if (guarding) {
            return State.BLOCK;
        }
        Long until = COUNTER_UNTIL.get(player.getUUID());
        if (until != null && player.level().getGameTime() <= until) {
            return State.COUNTER;
        }
        return State.IDLE;
    }

    public static void forget(Player player) {
        PARRY_OPENED.remove(player.getUUID());
        PARRY_READY_AT.remove(player.getUUID());
        PARRY_LENGTH.remove(player.getUUID());
        COUNTER_UNTIL.remove(player.getUUID());
        PARRY_CHAIN.remove(player.getUUID());
        COUNTERED.remove(player.getUUID());
        LATE_HITS.remove(player.getUUID());
        Parry.forget(player);
        SabreParry.forget(player);
    }
}
