package org.gumel.jojoha.stand;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.skill.TimeStopSystem;

final class StandGrip {
    private final LivingEntity held;

    private final boolean hadNoAi;
    private final boolean hadNoGravity;

    private StandGrip(LivingEntity held, boolean hadNoAi, boolean hadNoGravity) {
        this.held = held;
        this.hadNoAi = hadNoAi;
        this.hadNoGravity = hadNoGravity;
    }

    static StandGrip take(StandEntity stand, LivingEntity target) {
        if (target == stand || !target.isAlive()) {
            return null;
        }
        if (target instanceof Player player && !holdablePlayer(stand, player)) {
            return null;
        }
        if (target.isPassenger() || !stand.getPassengers().isEmpty()) {
            return null;
        }

        Boolean beforeTheStop = target instanceof Mob mob ? TimeStopSystem.noAiBeforeStop(mob) : null;
        boolean hadNoAi = beforeTheStop != null ? beforeTheStop : target instanceof Mob mob && mob.isNoAi();
        boolean hadNoGravity = target.isNoGravity();

        if (!target.startRiding(stand, true)) {
            return null;
        }

        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setNoAi(true);
        }
        if (!(target instanceof Player)) {
            target.setNoGravity(true);
        }
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0F;

        return new StandGrip(target, hadNoAi, hadNoGravity);
    }

    private static boolean holdablePlayer(StandEntity stand, Player player) {
        return player instanceof ServerPlayer caught
                && player != stand.getOwner()
                && !caught.isSpectator()
                && PlayerDataAccess.get(caught).isTimeStopFrozen();
    }

    LivingEntity held() {
        return held;
    }

    boolean stillHeld(StandEntity stand) {
        return held.isAlive() && !held.isRemoved() && held.getVehicle() == stand;
    }

    boolean frozenInStoppedTime() {
        if (held instanceof ServerPlayer player) {
            return PlayerDataAccess.get(player).isTimeStopFrozen();
        }
        return TimeStopSystem.freezes(held);
    }

    boolean outlivedTheStop() {
        return held instanceof Player && !frozenInStoppedTime();
    }

    void tick(StandEntity stand) {
        if (!(held instanceof Player)) {
            float yaw = stand.getYRot();
            held.setYRot(yaw);
            held.setYBodyRot(yaw);
            held.setYHeadRot(yaw);
            held.yRotO = yaw;
        }
        held.setDeltaMovement(Vec3.ZERO);
        held.fallDistance = 0F;
    }

    void release(Vec3 velocity) {
        held.stopRiding();

        if (held instanceof Mob mob) {
            mob.setNoAi(hadNoAi);
        }
        held.setNoGravity(hadNoGravity);

        held.setDeltaMovement(velocity);
        held.hurtMarked = true;
        held.fallDistance = 0F;
    }
}
