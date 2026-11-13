package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.anubis.Anubis;

public final class AnubisReach {
    private AnubisReach() {
    }

    public static Entity behindWall(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null || player.isSpectator()
                || !(minecraft.hitResult instanceof BlockHitResult block) || block.getType() != HitResult.Type.BLOCK
                || !Anubis.isBlade(player.getMainHandItem()) || !Anubis.cutsThroughWalls(ClientPlayerDataCache.data)) {
            return null;
        }
        double reach = player.entityInteractionRange();
        Vec3 eye = player.getEyePosition(1F);
        Vec3 look = player.getViewVector(1F);
        Vec3 end = eye.add(look.scale(reach));
        AABB swept = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        EntityHitResult found = ProjectileUtil.getEntityHitResult(player, eye, end, swept,
                entity -> entity instanceof LivingEntity && entity != player && entity.isPickable()
                        && !entity.isSpectator() && !player.isPassengerOfSameVehicle(entity),
                reach * reach);
        return found == null ? null : found.getEntity();
    }

    public static boolean strike(Minecraft minecraft) {
        Entity target = behindWall(minecraft);
        if (target == null || minecraft.player.isHandsBusy()) {
            return false;
        }
        minecraft.gameMode.attack(minecraft.player, target);
        minecraft.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }
}
