package org.gumel.jojoha.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.BarragePressure;
import org.gumel.jojoha.combat.Hitbox;
import org.gumel.jojoha.combat.WeaponClasses;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.skill.moves.TimeStopThrow;

public final class StandCombatInput {
    public static boolean standTakesSwing(Minecraft mc) {
        JojohaPlayerData data = ClientPlayerDataCache.data;
        if (mc.player != null && data.standSummoned && data.standMode.handlesItems()) {
            StandEntity stand = StandEntity.findFor(mc.player);
            return stand != null && stand.isCradling();
        }
        if (mc.player == null || !data.standSummoned || data.halfSummon || data.standMode.handlesItems()
                || !data.stand.isPresent() || !data.stand.trust().canStandPunch()
                || !StandTypes.hasBody(data)) {
            return false;
        }
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK
                && enemyInLane(mc) == null) {
            return false;
        }
        return !WeaponClasses.swingsItself(mc.player.getMainHandItem());
    }

    public static boolean throwsWeapon(Minecraft mc) {
        JojohaPlayerData data = ClientPlayerDataCache.data;
        return mc.player != null && mc.screen == null && TimeStopThrow.throwing(data)
                && data.standSummoned && !data.halfSummon && !data.standMode.handlesItems()
                && data.stand.isPresent() && data.stand.trust().canStandPunch() && StandTypes.hasBody(data)
                && WeaponClasses.swingsItself(mc.player.getMainHandItem());
    }

    private static int aimedId(Minecraft mc) {
        if (mc.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity) {
            return hit.getEntity().getId();
        }
        LivingEntity lane = enemyInLane(mc);
        return lane == null ? -1 : lane.getId();
    }

    private static LivingEntity enemyInLane(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getViewVector(1.0F);
        double wall = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK
                ? mc.hitResult.getLocation().distanceTo(eye) : Double.MAX_VALUE;
        double reach = StandCombatHandler.PUNCH_RANGE;
        LivingEntity nearest = null;
        double nearestAt = Double.MAX_VALUE;
        for (LivingEntity body : mc.level.getEntitiesOfClass(
                LivingEntity.class, mc.player.getBoundingBox().inflate(reach + 1.0),
                entity -> entity instanceof Enemy && entity.isAlive()
                        && !(entity instanceof StandEntity))) {
            double at = Hitbox.distanceAlong(eye, look, reach,
                    StandCombatHandler.PUNCH_LANE_HALF_WIDTH, body);
            if (at >= 0 && at < wall && at < nearestAt) {
                nearest = body;
                nearestAt = at;
            }
        }
        return nearest;
    }

    private static boolean wasAttackDown = false;
    private static boolean wasGuardDown = false;

    private static boolean guardClaimed = false;

    private static int summonHeld;
    private static boolean summonSpent;

    private static final int MODE_HOLD_TICKS = 6;

    private StandCombatInput() {
    }

    public static void tick(KeyMapping summonKey) {

        Minecraft mc = Minecraft.getInstance();
        MoveInputReport.tick(mc);
        PossessionClient.tick(mc);
        if (PossessionClient.possessing() || PossessionClient.possessed()) {
            wasAttackDown = mc.options.keyAttack.isDown();
            return;
        }
        ParryInput.tick(mc);
        DoubleJumpInput.tick(mc);
        if (mc.player == null) {
            wasAttackDown = false;
            wasGuardDown = false;
            guardClaimed = false;
            summonHeld = 0;
            summonSpent = false;
            return;
        }

        if (RawKey.isDown(summonKey)) {
            summonHeld++;
            if (!summonSpent && summonHeld >= MODE_HOLD_TICKS) {
                NetworkHandler.sendCycleStandMode();
                summonSpent = true;
            }
        } else {
            if (summonHeld > 0 && !summonSpent) {
                if (RawKey.modifierDown()) {
                    NetworkHandler.sendToggleHalfSummon();
                } else {
                    NetworkHandler.sendToggleStandSummon();
                }
            }
            summonHeld = 0;
            summonSpent = false;
        }

        boolean attackDown = mc.options.keyAttack.isDown();
        if (attackDown && !wasAttackDown && ClientPlayerDataCache.data.standSummoned
                && StandTypes.hasBody(ClientPlayerDataCache.data)
                && (standTakesSwing(mc) || BarrageStruggleHud.active() || StandSkillInput.isPiloting())) {
            NetworkHandler.sendRequestStandPunch(aimedId(mc));
        }
        wasAttackDown = attackDown;

        boolean guardDown = guardClaimed && canGuard(mc) && mc.options.keyUse.isDown();
        if (!guardDown) {
            guardClaimed = false;
        }
        if (guardDown != wasGuardDown) {
            wasGuardDown = guardDown;
            NetworkHandler.sendSetStandGuard(guardDown);
        }
    }

    private static boolean canGuard(Minecraft mc) {
        return mc.player != null && mc.screen == null
                && ClientPlayerDataCache.data.standSummoned
                && ClientPlayerDataCache.data.stand.trust().canGuard()
                && !ClientPlayerDataCache.data.standMode.handlesItems()
                && StandTypes.hasBody(ClientPlayerDataCache.data)
                && (mc.player.getMainHandItem().isEmpty()
                        || BarragePressure.on(mc.player));
    }

    public static boolean claimsUse() {
        Minecraft mc = Minecraft.getInstance();
        if (!canGuard(mc)) {
            return false;
        }
        if (guardClaimed) {
            return true;
        }
        if (aimedAtInteraction(mc)) {
            return false;
        }
        guardClaimed = true;
        return true;
    }

    private static boolean aimedAtInteraction(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return false;
        }
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            return !(target instanceof Enemy
                    || target instanceof Player
                    || target instanceof StandEntity);
        }
        return true;
    }
}
