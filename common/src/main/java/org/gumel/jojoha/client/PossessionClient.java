package org.gumel.jojoha.client;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.network.packet.PossessionAttackPacket;
import org.gumel.jojoha.network.packet.PossessionSteerPacket;
import org.gumel.jojoha.stand.hierophant.PossessionDrive;

public final class PossessionClient {
    private static int bodyId = -1;
    private static boolean possessed;
    private static boolean merged;

    private static Vec3 velocity = Vec3.ZERO;

    private static Vec3 placed;

    private PossessionClient() {
    }

    public static boolean possessing() {
        return bodyId >= 0;
    }

    public static boolean possessed() {
        return possessed;
    }

    public static boolean merged() {
        return possessing() && merged;
    }

    public static boolean drives(Entity entity) {
        return possessing() && entity.level().isClientSide() && entity.getId() == bodyId;
    }

    public static void state(int entityId, boolean asVictim, boolean on, boolean merged) {
        Minecraft minecraft = Minecraft.getInstance();
        if (asVictim) {
            possessed = on;
            placed = null;
            return;
        }
        bodyId = on ? entityId : -1;
        PossessionClient.merged = on && merged;
        velocity = Vec3.ZERO;
        if (!on && minecraft.player != null) {
            minecraft.setCameraEntity(minecraft.player);
        }
    }

    public static void relayed(float yaw, float pitch, double x, double y, double z) {
        PossessionClient.placed = new Vec3(x, y, z);
        Minecraft minecraft = Minecraft.getInstance();
        if (possessed && minecraft.player != null) {
            minecraft.player.setYRot(yaw);
            minecraft.player.setXRot(pitch);
            minecraft.player.setYHeadRot(yaw);
        }
    }

    public static void tickBody(LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (merged && entity == minecraft.player) {
            Entity host = minecraft.level == null ? null : minecraft.level.getEntity(bodyId);
            if (host != null) {
                entity.setPos(host.getX(), host.getY(), host.getZ());
                entity.setDeltaMovement(Vec3.ZERO);
            }
        } else if (!merged && entity != minecraft.player && drives(entity)) {
            walk(minecraft, entity);
        } else if (possessed && entity == minecraft.player && placed != null) {
            entity.setPos(placed.x, placed.y, placed.z);
            entity.setDeltaMovement(Vec3.ZERO);
            placed = null;
        }
    }

    private static void walk(Minecraft minecraft, LivingEntity body) {
        var options = minecraft.options;
        float ahead = (options.keyUp.isDown() ? 1F : 0F) - (options.keyDown.isDown() ? 1F : 0F);
        float aside = (options.keyLeft.isDown() ? 1F : 0F) - (options.keyRight.isDown() ? 1F : 0F);
        float facing = minecraft.player.getYRot();
        velocity = PossessionDrive.step(body, velocity, ahead, aside, options.keyJump.isDown(),
                options.keyShift.isDown(), options.keySprint.isDown(), facing);
        body.setYRot(facing);
        body.setYHeadRot(facing);
        body.setYBodyRot(facing);
        body.setXRot(minecraft.player.getXRot());
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            bodyId = -1;
            possessed = false;
            merged = false;
            placed = null;
            return;
        }
        if (!possessing()) {
            return;
        }
        Entity body = minecraft.level.getEntity(bodyId);
        if (body == null) {
            return;
        }
        if (merged) {
            if (minecraft.getCameraEntity() != minecraft.player) {
                minecraft.setCameraEntity(minecraft.player);
            }
            return;
        }
        if (minecraft.getCameraEntity() != body) {
            minecraft.setCameraEntity(body);
        }
        NetworkManager.sendToServer(new PossessionSteerPacket(minecraft.player.getYRot(), minecraft.player.getXRot(),
                body.getX(), body.getY(), body.getZ()));
    }

    public static void attack() {
        NetworkManager.sendToServer(PossessionAttackPacket.INSTANCE);
    }
}
