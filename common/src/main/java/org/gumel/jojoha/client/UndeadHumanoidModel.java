package org.gumel.jojoha.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import org.gumel.jojoha.Jojoha;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public abstract class UndeadHumanoidModel<T extends Mob & GeoAnimatable> extends GeoModel<T> {

    protected static final String BODY = "body";
    protected static final String TORSO = "torso";
    protected static final String HEAD = "head";
    protected static final String RIGHT_ARM = "right_arm";
    protected static final String LEFT_ARM = "left_arm";
    protected static final String RIGHT_LEG = "right_leg";
    protected static final String LEFT_LEG = "left_leg";

    private static final String[] BONES = {BODY, TORSO, HEAD, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG};

    private static final float STRIDE = 0.6662F;
    private static final float LEG_SWING = 1.4F;

    private static final float BOB = 0.05F;

    private static final float MAX_HEAD_YAW = 75F;
    private static final float MAX_HEAD_PITCH = 50F;

    protected static ResourceLocation asset(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> state) {
        float partialTick = state.getPartialTick();
        float swing = state.getLimbSwing();
        float amount = Math.min(1F, state.getLimbSwingAmount());
        float age = animatable.tickCount + partialTick;

        for (String bone : BONES) {
            pose(bone, 0F, 0F, 0F);
            place(bone, 0F, 0F, 0F);
        }

        EntityModelData look = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (look != null) {
            pose(HEAD,
                    Mth.clamp(look.headPitch(), -MAX_HEAD_PITCH, MAX_HEAD_PITCH) * Mth.DEG_TO_RAD,
                    Mth.clamp(look.netHeadYaw(), -MAX_HEAD_YAW, MAX_HEAD_YAW) * Mth.DEG_TO_RAD, 0F);
        }

        pose(RIGHT_LEG, Mth.cos(swing * STRIDE) * LEG_SWING * amount, 0F, 0F);
        pose(LEFT_LEG, Mth.cos(swing * STRIDE + Mth.PI) * LEG_SWING * amount, 0F, 0F);

        poseArms(animatable, swing, amount, age, partialTick);
        poseExtra(animatable, age, partialTick);
    }

    protected abstract void poseArms(T animatable, float swing, float amount, float age,
                                     float partialTick);

    protected void poseExtra(T animatable, float age, float partialTick) {
    }

    protected void bob(float age) {
        float sway = Mth.sin(age * 0.067F) * BOB;
        float drift = Mth.cos(age * 0.09F) * BOB + BOB;
        add(RIGHT_ARM, sway, drift);
        add(LEFT_ARM, sway, -drift);
    }

    protected float chop(T animatable, float partialTick) {
        float attack = animatable.getAttackAnim(partialTick);
        float eased = 1F - attack;
        eased *= eased;
        eased *= eased;
        return Mth.sin((1F - eased) * Mth.PI);
    }

    protected void pose(String name, float x, float y, float z) {
        GeoBone bone = getAnimationProcessor().getBone(name);
        if (bone != null) {
            bone.setRotX(x);
            bone.setRotY(y);
            bone.setRotZ(z);
        }
    }

    protected void place(String name, float x, float y, float z) {
        GeoBone bone = getAnimationProcessor().getBone(name);
        if (bone != null) {
            bone.setPosX(x);
            bone.setPosY(y);
            bone.setPosZ(z);
        }
    }

    protected void add(String name, float x, float z) {
        GeoBone bone = getAnimationProcessor().getBone(name);
        if (bone != null) {
            bone.setRotX(bone.getRotX() + x);
            bone.setRotZ(bone.getRotZ() + z);
        }
    }

    protected float rotX(String name) {
        GeoBone bone = getAnimationProcessor().getBone(name);
        return bone == null ? 0F : bone.getRotX();
    }
}
