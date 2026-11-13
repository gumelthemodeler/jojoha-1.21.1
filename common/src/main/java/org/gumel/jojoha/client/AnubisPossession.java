package org.gumel.jojoha.client;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.anubis.Anubis;
import org.gumel.jojoha.stand.anubis.Possession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AnubisPossession {
    private static final double SEEK_RANGE = 16.0;
    private static final double LOSE_RANGE = 24.0;
    private static final int PICK_FROM = 3;
    private static final int RETARGET_TICKS = 100;
    private static final int WANDER_TICKS = 40;
    private static final float TURN_PER_TICK = 25F;
    private static final double CLOSE_ENOUGH = 1.8;
    private static final float SWING_READY = 0.9F;

    private static boolean active;
    private static int lockedSlot;
    private static LivingEntity target;
    private static int retarget;
    private static float wanderYaw;
    private static int wander;

    private AnubisPossession() {
    }

    public static void init() {
        Anubis.clientPossessed = AnubisPossession::locks;
        ClientTickEvent.CLIENT_PRE.register(AnubisPossession::tick);
    }

    private static boolean locks(Entity entity) {
        return active && entity == Minecraft.getInstance().player;
    }

    private static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        boolean possessed = player != null && mc.gameMode != null && player.isAlive()
                && ClientPlayerDataCache.data != null && Possession.possessed(ClientPlayerDataCache.data);
        if (!possessed) {
            if (active) {
                active = false;
                target = null;
                KeyMapping.releaseAll();
                KeyMapping.setAll();
            }
            return;
        }
        if (!active) {
            active = true;
            lockedSlot = player.getInventory().selected;
            target = null;
            retarget = 0;
            wander = 0;
            wanderYaw = player.getYRot();
            if (mc.screen != null && !(mc.screen instanceof ChatScreen)) {
                mc.setScreen(null);
            }
        }

        silence(mc);
        player.getInventory().selected = lockedSlot;

        if (target == null || !target.isAlive() || target.isRemoved() || target.distanceTo(player) > LOSE_RANGE
                || --retarget <= 0) {
            target = pick(player);
            retarget = RETARGET_TICKS;
        }

        boolean forward;
        if (target != null) {
            Vec3 aim = target.getBoundingBox().getCenter();
            face(player, aim);
            double distance = player.distanceTo(target);
            forward = distance > CLOSE_ENOUGH;
            double reach = player.entityInteractionRange() + target.getBbWidth() * 0.5;
            if (distance <= reach && player.getAttackStrengthScale(0.5F) >= SWING_READY) {
                mc.gameMode.attack(player, target);
                player.swing(InteractionHand.MAIN_HAND);
            }
        } else {
            if (--wander <= 0) {
                wander = WANDER_TICKS;
                wanderYaw = player.getYRot() + (player.getRandom().nextFloat() - 0.5F) * 180F;
            }
            player.setYRot(Mth.approachDegrees(player.getYRot(), wanderYaw, TURN_PER_TICK));
            player.setXRot(Mth.approach(player.getXRot(), 0F, TURN_PER_TICK));
            forward = true;
        }

        mc.options.keyUp.setDown(forward);
        mc.options.keySprint.setDown(forward);
        mc.options.keyJump.setDown(forward && player.horizontalCollision);
    }

    private static void silence(Minecraft mc) {
        for (KeyMapping key : List.of(mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight,
                mc.options.keyShift, mc.options.keyAttack, mc.options.keyUse, mc.options.keyPickItem,
                mc.options.keyDrop, mc.options.keySwapOffhand, mc.options.keyInventory)) {
            drain(key);
        }
        for (KeyMapping key : mc.options.keyHotbarSlots) {
            drain(key);
        }
    }

    private static void drain(KeyMapping key) {
        key.setDown(false);
        boolean clicked = key.consumeClick();
        while (clicked) {
            clicked = key.consumeClick();
        }
    }

    private static LivingEntity pick(LocalPlayer player) {
        List<LivingEntity> near = new ArrayList<>(player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(SEEK_RANGE), AnubisPossession::prey));
        near.remove(player);
        if (near.isEmpty()) {
            return null;
        }
        near.sort(Comparator.comparingDouble(player::distanceToSqr));
        return near.get(player.getRandom().nextInt(Math.min(PICK_FROM, near.size())));
    }

    private static boolean prey(LivingEntity entity) {
        if (!entity.isAlive() || entity.isSpectator() || entity instanceof ArmorStand
                || entity instanceof StandEntity) {
            return false;
        }
        return !(entity instanceof Player other) || !other.isCreative();
    }

    private static void face(LocalPlayer player, Vec3 at) {
        Vec3 from = player.getEyePosition();
        double dx = at.x - from.x, dy = at.y - from.y, dz = at.z - from.z;
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
        player.setYRot(Mth.approachDegrees(player.getYRot(), yaw, TURN_PER_TICK));
        player.setXRot(Mth.clamp(Mth.approachDegrees(player.getXRot(), pitch, TURN_PER_TICK), -90F, 90F));
        player.setYHeadRot(player.getYRot());
    }
}
