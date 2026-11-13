package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.anim.BedrockAnimation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerStabAnimation {
    public static final ResourceLocation ANIMATION_FILE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "animations/player_stab.animation.json");

    public static final String STAB_ANIMATION = "stab2";
    public static final String LAND_ANIMATION = "land";
    public static final String AWAKEN_ANIMATION = "awaken";

    public static final String EQUIP_MASK_ANIMATION = "equip_mask";

    private record BoneBinding(String bone, ModelPart part) {
    }

    private record ActiveStab(float startTick, String animationName) {
    }

    private static final Map<String, Optional<BedrockAnimation>> LOADED = new HashMap<>();

    private static final Map<UUID, ActiveStab> ACTIVE = new HashMap<>();

    private PlayerStabAnimation() {
    }

    public static void begin(UUID playerId, String animationName, float clientTimeTicks) {
        ACTIVE.put(playerId, new ActiveStab(clientTimeTicks, animationName));
    }

    public static void tick(float clientTimeTicks) {
        ACTIVE.entrySet().removeIf(entry -> {
            BedrockAnimation anim = animation(entry.getValue().animationName());
            return anim == null || clientTimeTicks - entry.getValue().startTick() > anim.lengthTicks();
        });
    }

    public static void stop(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static boolean apply(PlayerModel<?> model, Player player, float clientTimeTicks) {
        ActiveStab active = ACTIVE.get(player.getUUID());
        if (active == null) {
            return false;
        }

        BedrockAnimation anim = animation(active.animationName());
        if (anim == null) {
            return false;
        }

        float elapsedTicks = clientTimeTicks - active.startTick();
        if (elapsedTicks < 0F || elapsedTicks > anim.lengthTicks()) {
            return false;
        }

        pose(model, anim, elapsedTicks / 20F);
        return true;
    }

    private static final String HELD_ARM = "right_arm";
    private static final float HELD_TIP_X = -1F / 16F;
    private static final float HELD_TIP_Y = 10F / 16F;
    private static final float HELD_TIP_Z = -1F / 16F;
    private static final float PLAYER_SCALE = 0.9375F;
    private static final float MODEL_DROP = 1.501F;

    public static Vec3 heldTipOffset(Player player, float clientTimeTicks) {
        PartPose rest = PartPose.offset(-5F, 2F, 0F);
        if (Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof LivingEntityRenderer<?, ?> renderer
                && renderer.getModel() instanceof HumanoidModel<?> model) {
            rest = model.rightArm.getInitialPose();
        }

        Vector3f rotation = new Vector3f();
        Vector3f position = new Vector3f();
        ActiveStab active = ACTIVE.get(player.getUUID());
        BedrockAnimation anim = active == null ? null : animation(active.animationName());
        if (anim != null) {
            float seconds = Mth.clamp((clientTimeTicks - active.startTick()) / 20F, 0F, anim.lengthSeconds());
            Vector3f sampled = anim.sample(HELD_ARM, BedrockAnimation.Channel.ROTATION, seconds);
            if (sampled != null) {
                rotation.set(sampled);
            }
            sampled = anim.sample(HELD_ARM, BedrockAnimation.Channel.POSITION, seconds);
            if (sampled != null) {
                position.set(sampled);
            }
        }

        Matrix4f pose = new Matrix4f()
                .rotateY((180F - player.yBodyRot) * Mth.DEG_TO_RAD)
                .scale(-PLAYER_SCALE, -PLAYER_SCALE, PLAYER_SCALE)
                .translate(0F, -MODEL_DROP, 0F)
                .translate((rest.x + position.x) / 16F, (rest.y + position.y) / 16F, (rest.z + position.z) / 16F)
                .rotate(new Quaternionf().rotationZYX(rest.zRot + rotation.z, rest.yRot + rotation.y,
                        rest.xRot + rotation.x));
        Vector3f tip = pose.transformPosition(new Vector3f(HELD_TIP_X, HELD_TIP_Y, HELD_TIP_Z));
        return new Vec3(tip.x, tip.y, tip.z);
    }

    static void pose(HumanoidModel<?> model, BedrockAnimation anim, float seconds) {
        pose(model, anim, seconds, 1F);
    }

    static void pose(HumanoidModel<?> model, BedrockAnimation anim, float seconds, float weight) {
        for (BoneBinding binding : bindings(model)) {
            if (!anim.drives(binding.bone())) {
                continue;
            }

            binding.part().resetPose();
            unscale(binding.part());
            if (model.crouching) {
                crouch(binding);
            }

            Vector3f rotation = anim.sample(binding.bone(), BedrockAnimation.Channel.ROTATION, seconds);
            if (rotation != null) {
                if (weight < 1F) {
                    rotation.set(Mth.wrapDegrees(rotation.x * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD,
                            Mth.wrapDegrees(rotation.y * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD,
                            Mth.wrapDegrees(rotation.z * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD);
                }
                binding.part().offsetRotation(rotation.mul(weight));
            }

            Vector3f position = anim.sample(binding.bone(), BedrockAnimation.Channel.POSITION, seconds);
            if (position != null) {
                binding.part().offsetPos(position.mul(weight));
            }

            Vector3f scale = handPass ? null : anim.sample(binding.bone(), BedrockAnimation.Channel.SCALE, seconds);
            if (scale != null) {
                binding.part().xScale = 1F + (scale.x - 1F) * weight;
                binding.part().yScale = 1F + (scale.y - 1F) * weight;
                binding.part().zScale = 1F + (scale.z - 1F) * weight;
            }
        }

        Vector3f neck = anim.sample(TORSO, BedrockAnimation.Channel.POSITION, seconds);
        if (neck != null && anim.sample(HEAD, BedrockAnimation.Channel.POSITION, seconds) == null) {
            model.head.offsetPos(neck.mul(weight));
        }

        copyOverlays(model);
    }

    private static final String TORSO = "torso";
    private static final String HEAD = "head";

    public static void resetParts(HumanoidModel<?> model) {
        for (BoneBinding binding : bindings(model)) {
            binding.part().resetPose();
            unscale(binding.part());
        }
        copyOverlays(model);
    }

    private static final float CROUCH_LEAN = 0.5F;
    private static final float CROUCH_ARM_TILT = 0.4F;
    private static final float CROUCH_BODY_DROP = 3.2F;
    private static final float CROUCH_HEAD_DROP = 4.2F;
    private static final float CROUCH_LEG_DROP = 0.2F;
    private static final float CROUCH_LEG_BACK = 4.0F;

    private static void crouch(BoneBinding binding) {
        ModelPart part = binding.part();
        switch (binding.bone()) {
            case "torso" -> {
                part.xRot += CROUCH_LEAN;
                part.y += CROUCH_BODY_DROP;
            }
            case "head" -> part.y += CROUCH_HEAD_DROP;
            case "right_arm", "left_arm" -> {
                part.xRot += CROUCH_ARM_TILT;
                part.y += CROUCH_BODY_DROP;
            }
            case "right_leg", "left_leg" -> {
                part.y += CROUCH_LEG_DROP;
                part.z += CROUCH_LEG_BACK;
            }
            default -> {
            }
        }
    }

    private static boolean handPass;

    public static void handPass(boolean drawing) {
        handPass = drawing;
    }

    public static boolean inHandPass() {
        return handPass;
    }

    private static void unscale(ModelPart part) {
        part.xScale = ModelPart.DEFAULT_SCALE;
        part.yScale = ModelPart.DEFAULT_SCALE;
        part.zScale = ModelPart.DEFAULT_SCALE;
    }

    private static BoneBinding[] bindings(HumanoidModel<?> model) {
        return new BoneBinding[] {
                new BoneBinding("torso", model.body),
                new BoneBinding("head", model.head),
                new BoneBinding("right_arm", model.rightArm),
                new BoneBinding("left_arm", model.leftArm),
                new BoneBinding("right_leg", model.rightLeg),
                new BoneBinding("left_leg", model.leftLeg),
        };
    }

    static void copyOverlays(HumanoidModel<?> model) {
        model.hat.copyFrom(model.head);
        if (model instanceof PlayerModel<?> player) {
            player.jacket.copyFrom(player.body);
            player.rightSleeve.copyFrom(player.rightArm);
            player.leftSleeve.copyFrom(player.leftArm);
            player.rightPants.copyFrom(player.rightLeg);
            player.leftPants.copyFrom(player.leftLeg);
        }
    }

    private static BedrockAnimation animation(String name) {
        return LOADED.computeIfAbsent(name, n -> BedrockAnimation.load(fileOf(n), n)).orElse(null);
    }

    static ResourceLocation fileOf(String name) {
        return name.equals(AWAKEN_ANIMATION) || name.equals(LAND_ANIMATION) ? PlayerMoveAnimation.FILE : ANIMATION_FILE;
    }
}
