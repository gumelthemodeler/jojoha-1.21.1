package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.combat.WeaponClasses;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.item.WeaponClass;

public final class RapierBrace {
    private static final float HAND_X = 0.56F;
    private static final float HAND_Y = -0.52F;
    private static final float HAND_Y_EQUIPPED = -0.6F;
    private static final float HAND_Z = -0.72F;

    private static final float BRACE_X = -0.26F;
    private static final float BRACE_Y = -0.08F;
    private static final float BRACE_Z = -0.17F;

    private static final float BRACE_REACH = -0.24F;

    private static final float POINT_FORWARD = -87F;

    private static final float POINT_YAW = 9F;
    private static final float POINT_ROLL = 5F;

    private static final float BRACE_SCALE = 1.5F;

    private RapierBrace() {
    }

    public static boolean isBracing(LivingEntity holder) {
        if (!(holder instanceof Player player)
                || !PlayerMoveAnimation.playing(player, MovementFx.RAPIER_LUNGE)) {
            return false;
        }
        WeaponClass weapon = WeaponClasses.held(holder);
        return weapon != null && weapon.thrusts();
    }

    public static float charge(LivingEntity holder) {
        return holder instanceof Player player
                ? PlayerMoveAnimation.progress(player, MovementFx.RAPIER_LUNGE)
                : 0F;
    }

    private static HumanoidArm bracingArm(LivingEntity holder) {
        return holder instanceof Player player ? player.getMainArm() : HumanoidArm.RIGHT;
    }

    public static boolean renderFirstPerson(ItemInHandRenderer items, AbstractClientPlayer player,
                                            InteractionHand hand, ItemStack stack,
                                            float equippedProgress, PoseStack poseStack,
                                            MultiBufferSource buffer, int light) {
        if (!isBracing(player) || hand != InteractionHand.MAIN_HAND || stack.isEmpty()) {
            return false;
        }

        HumanoidArm arm = bracingArm(player);
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float charge = charge(player);

        poseStack.pushPose();

        poseStack.translate(side * HAND_X, HAND_Y + equippedProgress * HAND_Y_EQUIPPED, HAND_Z);
        poseStack.translate(side * BRACE_X, BRACE_Y, BRACE_Z + BRACE_REACH * charge);

        poseStack.mulPose(Axis.XP.rotationDegrees(POINT_FORWARD));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * POINT_YAW));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * POINT_ROLL));

        poseStack.scale(BRACE_SCALE, BRACE_SCALE, BRACE_SCALE);

        items.renderItem(player, stack, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                arm == HumanoidArm.LEFT, poseStack, buffer, light);

        poseStack.popPose();
        return true;
    }
}
