package org.gumel.jojoha.stand;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModSounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class StandClash {
    public static final int WINDOW_TICKS = 3;

    private static final double SHOVE = 0.7;
    private static final double LIFT = 0.2;

    private record Pending(LivingEntity attacker, LivingEntity target, long landsAt, Runnable land) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    private StandClash() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    public static boolean contested(LivingEntity target) {
        return target instanceof ServerPlayer defender && PlayerDataAccess.get(defender).standSummoned;
    }

    public static void swing(LivingEntity attacker, LivingEntity target, Runnable land) {
        if (target instanceof ServerPlayer) {
            LivingEntity defender = target;
            for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
                Pending theirs = it.next();
                if (theirs.attacker() == defender && theirs.target() == attacker) {
                    it.remove();
                    clash(attacker, defender);
                    return;
                }
            }
        }
        PENDING.add(new Pending(attacker, target, attacker.level().getGameTime() + WINDOW_TICKS, land));
    }

    private static void tick() {
        if (PENDING.isEmpty()) {
            return;
        }
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending pending = it.next();
            if (!pending.attacker().isAlive() || !pending.target().isAlive()
                    || pending.attacker().level() != pending.target().level()) {
                it.remove();
                continue;
            }
            if (pending.attacker().level().getGameTime() >= pending.landsAt()) {
                it.remove();
                pending.land().run();
            }
        }
    }

    private static void clash(LivingEntity one, LivingEntity other) {
        ServerLevel level = (ServerLevel) one.level();
        Vec3 a = one.position().add(0, one.getBbHeight() * 0.6, 0);
        Vec3 b = other.position().add(0, other.getBbHeight() * 0.6, 0);
        Vec3 at = a.lerp(b, 0.5);
        Vec3 across = b.subtract(a);

        StandEntity mine = one instanceof ServerPlayer p ? StandEntity.findFor(p) : null;
        StandEntity theirs = other instanceof ServerPlayer p ? StandEntity.findFor(p) : null;
        if (mine != null) {
            mine.triggerPunchAt(other, false);
        }
        if (theirs != null) {
            theirs.triggerPunchAt(one, true);
        }

        Impacts.landed(level, at, across, ImpactSpec.CLASH,
                Impacts.PLAYER);
        level.playSound(null, at.x, at.y, at.z, ModSounds.STAND_HIT.get(),
                SoundSource.PLAYERS, 1.0F, 0.7F);

        shoveApart(one, other);
        shoveApart(other, one);
    }

    public static void shoveApart(LivingEntity who, LivingEntity from) {
        Vec3 away = who.position().subtract(from.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        if (flat.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 shove = flat.normalize().scale(SHOVE);
        who.setDeltaMovement(who.getDeltaMovement().add(shove.x, LIFT, shove.z));
        who.hurtMarked = true;
    }
}
