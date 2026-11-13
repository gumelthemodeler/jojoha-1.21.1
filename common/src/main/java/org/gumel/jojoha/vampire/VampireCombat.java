package org.gumel.jojoha.vampire;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.combat.BonusHit;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.vampire.moves.VampiricDriveMove;

public final class VampireCombat {
    private static final double DRIVE_KNOCKBACK = 1.6;

    private VampireCombat() {
    }

    public static void init() {
        EntityEvent.LIVING_HURT.register(VampireCombat::onHurt);
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            VampireState.clear(entity);
            return EventResult.pass();
        });
    }

    private static EventResult onHurt(LivingEntity victim, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) {
            return EventResult.pass();
        }
        if (!VampireState.hasDrive(attacker)) {
            return EventResult.pass();
        }

        JojohaPlayerData data = PlayerDataAccess.get(attacker);
        if (!Fate.undead(data)) {
            return EventResult.pass();
        }

        VampireState.consumeDrive(attacker);

        ServerLevel level = attacker.serverLevel();
        BonusHit.land(victim, level.damageSources().playerAttack(attacker),
                DamageScale.vampire(attacker, victim, VampiricDriveMove.BONUS_DAMAGE));

        Bleed.apply(victim);

        Vec3 push = victim.position().subtract(attacker.position()).normalize().scale(DRIVE_KNOCKBACK);
        victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, 0.45, push.z));
        victim.hurtMarked = true;
        Impacts.landed(level, victim, push, ImpactSpec.HEAVY, Impacts.BLOOD);

        level.sendParticles(ModRegistries.BLOOD_MOTE.get(),
                victim.getX(), victim.getY() + 1.0, victim.getZ(), 25, 0.4, 0.5, 0.4, 0.1);

        return EventResult.pass();
    }
}
