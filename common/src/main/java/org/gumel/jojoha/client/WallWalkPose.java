package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.WallWalk;
import org.joml.Vector3f;

public final class WallWalkPose {
    private WallWalkPose() {
    }

    public static void apply(LivingEntity entity, PoseStack poseStack) {
        if (!(entity instanceof Player player)) {
            return;
        }
        Direction side = WallWalk.renderSide(player);
        if (side == null) {
            return;
        }
        Vec3 lift = WallWalk.renderLift(side);
        poseStack.translate(lift.x, lift.y, lift.z);
        Vector3f axis = new Vector3f(side.getStepZ(), 0F, -side.getStepX());
        poseStack.mulPose(Axis.of(axis).rotationDegrees(-90F));
    }
}
