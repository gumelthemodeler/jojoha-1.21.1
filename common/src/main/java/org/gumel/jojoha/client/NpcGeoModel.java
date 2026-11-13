package org.gumel.jojoha.client;

import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.combat.StandFist;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.gumel.jojoha.client.anim.BedrockAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.entity.npc.CrusaderNpc;
import org.gumel.jojoha.entity.npc.HamonStudent;
import org.gumel.jojoha.entity.npc.JojoNpc;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public final class NpcGeoModel extends GeoModel<JojoNpc> {

    private static final String HEAD = "Head";
    private static final String BODY = "Body";
    private static final String RIGHT_ARM = "RightArm";
    private static final String LEFT_ARM = "LeftArm";
    private static final String RIGHT_LEG = "RightLeg";
    private static final String LEFT_LEG = "LeftLeg";
    private static final String[] BONES = {HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG};
    private static final String[][] CLIP_BONES = {{"torso", "torso"}, {"head", HEAD}, {"right_arm", RIGHT_ARM},
            {"left_arm", LEFT_ARM}, {"right_leg", RIGHT_LEG}, {"left_leg", LEFT_LEG}};

    private static final ResourceLocation STUDENT = asset("geo/hamon_student.geo.json");
    private static final ResourceLocation STUDENT_FEMALE = asset("geo/hamon_student_female.geo.json");
    private static final int STUDENT_SKINS = 6;
    private static final ResourceLocation[] STUDENT_TEXTURES = studentTextures("");
    private static final ResourceLocation[] STUDENT_FEMALE_TEXTURES = studentTextures("_female");
    private static final ResourceLocation ANIMATION = asset("animations/npc.animation.json");

    private static final float STRIDE = 0.6662F;
    private static final float LEG_SWING = 1.4F;
    private static final float ARM_SWING = 1.0F;
    private static final float STRIKE = 1.6F;
    private static final float BREATH = 0.04F;
    private static final float MAX_HEAD_YAW = 75F;
    private static final float MAX_HEAD_PITCH = 50F;
    private static final float ARM_PIXELS = 12F;

    private static ResourceLocation asset(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    private static ResourceLocation[] studentTextures(String suffix) {
        ResourceLocation[] textures = new ResourceLocation[STUDENT_SKINS];
        for (int i = 0; i < STUDENT_SKINS; i++) {
            textures[i] = asset("textures/entity/mob/hamon_student_" + (i + 1) + suffix + ".png");
        }
        return textures;
    }

    @Override
    public ResourceLocation getModelResource(JojoNpc npc) {
        if (npc instanceof CrusaderNpc crusader) {
            return crusader.crusader().model();
        }
        return female(npc) ? STUDENT_FEMALE : STUDENT;
    }

    @Override
    public ResourceLocation getTextureResource(JojoNpc npc) {
        if (npc instanceof CrusaderNpc crusader) {
            return crusader.crusader().texture();
        }
        ResourceLocation[] skins = female(npc) ? STUDENT_FEMALE_TEXTURES : STUDENT_TEXTURES;
        return skins[Math.floorMod(npc.getUUID().getLeastSignificantBits(), STUDENT_SKINS)];
    }

    private static boolean female(JojoNpc npc) {
        return npc instanceof HamonStudent student && student.female();
    }

    @Override
    public ResourceLocation getAnimationResource(JojoNpc npc) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(JojoNpc npc, long instanceId, AnimationState<JojoNpc> state) {
        float partialTick = state.getPartialTick();
        float swing = state.getLimbSwing();
        float amount = Math.min(1F, state.getLimbSwingAmount());
        float age = npc.tickCount + partialTick;
        for (String bone : BONES) {
            pose(bone, 0F, 0F, 0F);
        }
        EntityModelData look = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (look != null) {
            pose(HEAD, Mth.clamp(look.headPitch(), -MAX_HEAD_PITCH, MAX_HEAD_PITCH) * Mth.DEG_TO_RAD,
                    Mth.clamp(look.netHeadYaw(), -MAX_HEAD_YAW, MAX_HEAD_YAW) * Mth.DEG_TO_RAD, 0F);
        }
        pose(RIGHT_LEG, Mth.cos(swing * STRIDE) * LEG_SWING * amount, 0F, 0F);
        pose(LEFT_LEG, Mth.cos(swing * STRIDE + Mth.PI) * LEG_SWING * amount, 0F, 0F);
        float breath = Mth.sin(age * 0.067F) * BREATH;
        float drift = Mth.cos(age * 0.09F) * BREATH + BREATH;
        pose(RIGHT_ARM, Mth.cos(swing * STRIDE + Mth.PI) * ARM_SWING * amount + breath + chop(npc, partialTick) * STRIKE, 0F, drift);
        pose(LEFT_ARM, Mth.cos(swing * STRIDE) * ARM_SWING * amount + breath, 0F, -drift);
        clip(npc, partialTick);
        punch(npc, partialTick);
    }

    private void punch(JojoNpc npc, float partialTick) {
        GeoBone arm = getAnimationProcessor().getBone(RIGHT_ARM);
        if (arm == null) {
            return;
        }
        arm.setScaleY(1F);
        ZoomPunchArm.Reach reach = ZoomPunchArm.reach(npc, partialTick);
        if (reach == null || reach.out() <= 0F) {
            return;
        }
        float bodyYaw = Mth.rotLerp(partialTick, npc.yBodyRotO, npc.yBodyRot);
        Vec3 target = StandFist.toModel(npc.getPosition(partialTick), bodyYaw, reach.at());
        Vec3 shoulder = new Vec3(arm.getPivotX(), arm.getPivotY(), arm.getPivotZ());
        Quaternionf twist = new Quaternionf();
        GeoBone torso = getAnimationProcessor().getBone("torso");
        if (torso != null) {
            Vec3 waist = new Vec3(torso.getPivotX(), torso.getPivotY(), torso.getPivotZ());
            twist.rotationZYX(torso.getRotZ(), torso.getRotY(), torso.getRotX());
            Vector3f turned = new Vector3f((float) (shoulder.x - waist.x), (float) (shoulder.y - waist.y),
                    (float) (shoulder.z - waist.z)).rotate(twist);
            shoulder = waist.add(turned.x, turned.y, turned.z);
        }
        Vector3f aim = new Vector3f((float) (target.x - shoulder.x), (float) (target.y - shoulder.y),
                (float) (target.z - shoulder.z)).rotate(new Quaternionf(twist).conjugate());
        float length = aim.length();
        if (length < 1.0E-4F) {
            return;
        }
        aim.div(length);
        float w = reach.out();
        float xRot = (float) Math.asin(Mth.clamp(-aim.z, -1F, 1F));
        float zRot = (float) Math.atan2(aim.x, -aim.y);
        arm.setRotX(Mth.lerp(w, arm.getRotX(), xRot));
        arm.setRotY(Mth.lerp(w, arm.getRotY(), 0F));
        arm.setRotZ(Mth.lerp(w, arm.getRotZ(), zRot));
        arm.setScaleY(1F + Math.max(0F, length / ARM_PIXELS - 1F) * w);
    }

    private void clip(JojoNpc npc, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        PlayerMoveAnimation.Pose clip = PlayerMoveAnimation.pose(npc.getId(), mc.level.getGameTime() + partialTick);
        GeoBone torso = getAnimationProcessor().getBone("torso");
        if (torso != null) {
            torso.setRotX(0F);
            torso.setRotY(0F);
            torso.setRotZ(0F);
        }
        if (clip == null) {
            return;
        }
        float w = clip.weight();
        for (String[] pair : CLIP_BONES) {
            GeoBone bone = getAnimationProcessor().getBone(pair[1]);
            if (bone == null || !clip.anim().drives(pair[0])) {
                continue;
            }
            Vector3f rotation = clip.anim().sample(pair[0], BedrockAnimation.Channel.ROTATION, clip.seconds());
            if (rotation != null) {
                bone.setRotX(Mth.lerp(w, bone.getRotX(), -rotation.x));
                bone.setRotY(Mth.lerp(w, bone.getRotY(), -rotation.y));
                bone.setRotZ(Mth.lerp(w, bone.getRotZ(), rotation.z));
            }
        }
    }

    private static float chop(JojoNpc npc, float partialTick) {
        float eased = 1F - npc.getAttackAnim(partialTick);
        eased *= eased;
        eased *= eased;
        return Mth.sin((1F - eased) * Mth.PI);
    }

    private void pose(String name, float x, float y, float z) {
        GeoBone bone = getAnimationProcessor().getBone(name);
        if (bone != null) {
            bone.setRotX(x);
            bone.setRotY(y);
            bone.setRotZ(z);
        }
    }
}
