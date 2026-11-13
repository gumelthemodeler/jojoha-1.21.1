package org.gumel.jojoha.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.anim.BedrockAnimation;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandLimbFlow;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.chariot.Chariot;
import org.gumel.jojoha.stand.combat.StandFist;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.model.GeoModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

public final class StandModel extends GeoModel<StandEntity> {
    private static final List<String> ARM_BONES =
            List.of("RightArm", "bone2", "finger", "LeftArm", "bone3");
    private static final List<String> TORSO_SPINE_BONES = List.of("Body", "upperTorso");
    private static final List<String> NON_ARM_BONES = List.of("Head", "hair", "RightLeg", "bone", "LeftLeg", "bone4");

    private static final List<String> BARRAGE_BONES = List.of(
            "barrage_rig",
            "barrage_arm_right", "barrage_arm_right2", "barrage_arm_right3",
            "barrage_arm_left", "barrage_arm_left2", "barrage_arm_left3",
            "barrage_arm_right4", "barrage_arm_right5", "barrage_arm_right6",
            "barrage_arm_left4", "barrage_arm_left5", "barrage_arm_left6");

    private static final String[][] ADDED_ARMS = {
            {"barrage_arm_right4", "barrage_arm_right"}, {"barrage_arm_left4", "barrage_arm_left"},
            {"barrage_arm_right5", "barrage_arm_right2"}, {"barrage_arm_left5", "barrage_arm_left2"},
            {"barrage_arm_right6", "barrage_arm_right3"}, {"barrage_arm_left6", "barrage_arm_left3"}};

    public static final String TRAIL = "_trail";

    public static final int TRAIL_COPIES_MAX = 3;

    public static final int TRAIL_COPIES = 2;

    private static final double TRAIL_LAG_TICKS = 0.6;

    private static final double FLASH_WIDTH = 3.0;

    private static final List<String> ARM_NAMES = List.of(
            "barrage_arm_right", "barrage_arm_right2", "barrage_arm_right3",
            "barrage_arm_left", "barrage_arm_left2", "barrage_arm_left3",
            "barrage_arm_right4", "barrage_arm_right5", "barrage_arm_right6",
            "barrage_arm_left4", "barrage_arm_left5", "barrage_arm_left6");

    private static final List<String> TRAIL_BONES = trailNames();

    private static final Map<GeoBone, Float> FLASH = new IdentityHashMap<>();

    private static final Set<BakedGeoModel> PREPARED =
            Collections.newSetFromMap(new WeakHashMap<>());

    private static List<String> trailNames() {
        List<String> names = new ArrayList<>();
        for (String arm : ARM_NAMES) {
            for (int copy = 1; copy <= TRAIL_COPIES_MAX; copy++) {
                names.add(arm + TRAIL + copy);
            }
        }
        return List.copyOf(names);
    }

    private static int trailCopies() {
        if (JojohaClientConfig.BARRAGE_ECHO.whole() > 0) {
            return 0;
        }
        return Math.max(0, Math.min(TRAIL_COPIES_MAX, JojohaClientConfig.BARRAGE_TRAILS.whole()));
    }

    public static float flash(GeoBone bone) {
        for (GeoBone at = bone; at != null; at = at.getParent()) {
            Float lit = FLASH.get(at);
            if (lit != null) {
                return lit;
            }
        }
        return 0F;
    }

    private static float peak(double share) {
        return (float) Math.max(0.0, 1.0 - Math.abs(share - 1.0) * FLASH_WIDTH);
    }

    private GeoBone trail(String arm, int copy) {
        return getAnimationProcessor().getBone(arm + TRAIL + copy);
    }

    private static final ResourceLocation UNIVERSAL_ANIMATIONS =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                    "animations/universal.animation.json");

    @Override
    public ResourceLocation getModelResource(StandEntity animatable) {
        StandShadowForm shadow = StandShadowForm.of(animatable);
        if (shadow != null) {
            return shadow.model();
        }
        if (unarmored(animatable)) {
            return UNARMORED_CHARIOT_MODEL;
        }
        ResourceLocation remote = RemoteSkins.model(animatable.getSkin());
        return remote != null ? remote : animatable.getStandType().model();
    }

    @Override
    public BakedGeoModel getBakedModel(ResourceLocation location) {
        BakedGeoModel model =
                GeckoLibCache.getBakedModels().get(location);
        if (model != null) {
            addAfterimageArms(model);
        }
        return super.getBakedModel(location);
    }

    static void addAfterimageArms(BakedGeoModel model) {
        if (PREPARED.contains(model)) {
            return;
        }
        GeoBone rig = model.getBone("barrage_rig").orElse(null);
        if (rig == null) {
            return;
        }
        PREPARED.add(model);
        for (GeoBone extra : model.topLevelBones()) {
            if (extra != rig && extra.getName().startsWith("barrage_rig")) {
                for (GeoBone arm : extra.getChildBones()) {
                    rig.getChildBones().add(copyBone(rig, arm, ""));
                }
                extra.getChildBones().clear();
                extra.setHidden(true);
            }
        }
        for (String[] added : ADDED_ARMS) {
            GeoBone template = null;
            boolean present = false;
            for (GeoBone child : rig.getChildBones()) {
                present |= child.getName().equals(added[0]);
                if (child.getName().equals(added[1])) {
                    template = child;
                }
            }
            if (present || template == null) {
                continue;
            }
            GeoBone arm = new GeoBone(rig, added[0], template.getMirror(), template.getInflate(),
                    template.shouldNeverRender(), template.getReset());
            arm.setPivotX(template.getPivotX());
            arm.setPivotY(template.getPivotY());
            arm.setPivotZ(template.getPivotZ());
            arm.setRotX(template.getRotX());
            arm.setRotY(template.getRotY());
            arm.setRotZ(template.getRotZ());
            arm.getCubes().addAll(template.getCubes());
            rig.getChildBones().add(arm);
        }

        List<GeoBone> arms = new ArrayList<>();
        for (GeoBone child : rig.getChildBones()) {
            if (ARM_NAMES.contains(child.getName())) {
                arms.add(child);
            }
        }
        Set<String> present = new HashSet<>();
        for (GeoBone child : rig.getChildBones()) {
            present.add(child.getName());
        }
        for (GeoBone arm : arms) {
            for (int copy = 1; copy <= TRAIL_COPIES_MAX; copy++) {
                if (!present.contains(arm.getName() + TRAIL + copy)) {
                    rig.getChildBones().add(copyBone(rig, arm, TRAIL + copy));
                }
            }
        }
    }

    private static GeoBone copyBone(GeoBone parent, GeoBone template, String suffix) {
        GeoBone copy = new GeoBone(parent, template.getName() + suffix, template.getMirror(), template.getInflate(),
                template.shouldNeverRender(), template.getReset());
        copy.setPivotX(template.getPivotX());
        copy.setPivotY(template.getPivotY());
        copy.setPivotZ(template.getPivotZ());
        copy.setRotX(template.getRotX());
        copy.setRotY(template.getRotY());
        copy.setRotZ(template.getRotZ());
        copy.getCubes().addAll(template.getCubes());
        for (GeoBone child : template.getChildBones()) {
            copy.getChildBones().add(copyBone(copy, child, suffix));
        }
        return copy;
    }

    @Override
    public ResourceLocation getTextureResource(StandEntity animatable) {
        StandShadowForm shadow = StandShadowForm.of(animatable);
        if (shadow != null) {
            return shadow.texture();
        }
        if (unarmored(animatable)) {
            return UNARMORED_CHARIOT_TEXTURE;
        }
        ResourceLocation remote = RemoteSkins.texture(animatable.getSkin());
        return remote != null ? remote : animatable.getStandType().textureFor(animatable.getSkin());
    }

    private static final ResourceLocation UNARMORED_CHARIOT_MODEL =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "geo/silver_chariot_non_armored.geo.json");
    private static final ResourceLocation UNARMORED_CHARIOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/stand/silver_chariot/silver_chariot_nonarmored.png");

    private static boolean unarmored(StandEntity stand) {
        return stand.armorOff() && Chariot.is(stand);
    }

    @Override
    public ResourceLocation getAnimationResource(StandEntity animatable) {
        ResourceLocation remote = RemoteSkins.animations(animatable.getSkin());
        return remote != null ? remote : animatable.getStandType().animation();
    }

    @Override
    public ResourceLocation[] getAnimationResourceFallbacks(StandEntity animatable) {
        if (RemoteSkins.animations(animatable.getSkin()) != null) {
            return new ResourceLocation[] { animatable.getStandType().animation(), UNIVERSAL_ANIMATIONS,
                    REFERENCE_HUMANOID_ANIMATIONS };
        }
        return new ResourceLocation[] { UNIVERSAL_ANIMATIONS, REFERENCE_HUMANOID_ANIMATIONS };
    }

    private static final ResourceLocation REFERENCE_HUMANOID_ANIMATIONS =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                    "animations/star_platinum.animation.json");

    @Override
    public RenderType getRenderType(StandEntity animatable, ResourceLocation texture) {
        return StandViewAlpha.of(animatable) < 1F || animatable.isManifesting()
                || StandShadowForm.of(animatable) != null
                ? RenderType.entityTranslucent(texture)
                : RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void setCustomAnimations(StandEntity animatable, long instanceId, AnimationState<StandEntity> animationState) {
        frame++;
        unstackTrails();
        boolean armsOnly = animatable.getTrustTier().isPartialManifestation();
        boolean justArms = armsOnly || PilotView.drawing(animatable);

        for (String boneName : ARM_BONES) {
            setBoneHidden(boneName, false);
        }
        for (String boneName : NON_ARM_BONES) {
            setBoneHidden(boneName, justArms);
        }
        for (String boneName : TORSO_SPINE_BONES) {
            GeoBone bone = getAnimationProcessor().getBone(boneName);
            if (bone != null) {
                bone.setHidden(justArms);
                bone.setChildrenHidden(false);
            }
        }

        boolean barraging = animatable.isBarraging();
        for (String boneName : BARRAGE_BONES) {
            GeoBone bone = getAnimationProcessor().getBone(boneName);
            if (bone != null) {
                bone.setHidden(true);
                boolean rig = boneName.equals("barrage_rig");
                bone.setChildrenHidden(!rig && !barraging);
                if (!rig) {
                    hideHeldBones(bone);
                }
            }
        }
        FLASH.clear();
        for (String boneName : TRAIL_BONES) {
            GeoBone bone = getAnimationProcessor().getBone(boneName);
            if (bone != null) {
                bone.setHidden(true);
                bone.setChildrenHidden(!barraging);
                hideHeldBones(bone);
                if (barraging) {
                    hideArm(bone);
                }
            }
        }
        applyCustomIdle(animatable, instanceId, animationState.getPartialTick());
        if (!applyFistArms(animatable, animationState.getPartialTick()) && barraging) {
            if (!applyStabArms(animatable, animationState.getPartialTick())) {
                applyBarrageArms(animatable, animationState.getPartialTick());
            }
        }
        applyFistBody(animatable, animationState.getPartialTick());
        if (barraging) {
            applyBarrageRecoil(animatable, animationState.getPartialTick());
            applyBarrageFlow(animatable, animationState.getPartialTick());
        }

        GeoBone blade = getAnimationProcessor().getBone("Blade");
        if (blade != null) {
            blade.setHidden(animatable.bladeOut());
        }

        applyHairTrail(animatable, animationState.getPartialTick(), armsOnly);
        applyBoundArms(animatable, animationState.getPartialTick());
        applyFingerReach(animatable, animationState.getPartialTick());
        applyStepOut(animatable, animationState.getPartialTick(), armsOnly);
    }

    private static final float STEP_LEG_SWING = 0.65F;
    private static final float STEP_ARM_SWING = 0.4F;

    private void applyStepOut(StandEntity animatable, float partialTick, boolean armsOnly) {
        float stride = SummonFlair.stride(animatable, partialTick);
        if (stride == 0F || armsOnly) {
            return;
        }
        addRotation("RightLeg", stride * STEP_LEG_SWING, 0F, 0F);
        addRotation("LeftLeg", -stride * STEP_LEG_SWING, 0F, 0F);
        addRotation("RightArm", -stride * STEP_ARM_SWING, 0F, 0F);
        addRotation("LeftArm", stride * STEP_ARM_SWING, 0F, 0F);
    }

    private static final float FINGER_CLIP_PEAK = 22.175F;

    private static final float FINGER_LENGTH_PX = 2F;

    private static final double FINGER_SNAP = 3.0;

    private void applyFingerReach(StandEntity stand, float partialTick) {
        GeoBone finger = getAnimationProcessor().getBone("finger");
        if (finger == null || finger.getScaleY() <= 1.001F) {
            return;
        }
        Vec3 goal = stand.fingerGoal(partialTick);
        if (goal == null) {
            return;
        }
        float facing = Mth.rotLerp(partialTick, stand.yBodyRotO, stand.yBodyRot);
        Vec3 aim = StandFist.toModel(
                stand.getPosition(partialTick), facing, goal);

        List<GeoBone> chain = new ArrayList<>();
        for (GeoBone at = finger.getParent(); at != null; at = at.getParent()) {
            chain.add(0, at);
        }
        Matrix4f frame = new Matrix4f();
        for (GeoBone bone : chain) {
            frame.translate(-bone.getPosX(), bone.getPosY(), bone.getPosZ())
                    .translate(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ())
                    .rotateZ(bone.getRotZ()).rotateY(bone.getRotY()).rotateX(bone.getRotX())
                    .scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ())
                    .translate(-bone.getPivotX(), -bone.getPivotY(), -bone.getPivotZ());
        }
        Vector3f knuckle = frame.transformPosition(new Vector3f(
                finger.getPivotX() - finger.getPosX(), finger.getPivotY() + finger.getPosY(),
                finger.getPivotZ() + finger.getPosZ()));
        Vector3f reach = new Vector3f((float) aim.x, (float) aim.y, (float) aim.z).sub(knuckle);
        float distance = reach.length();
        if (distance < FINGER_LENGTH_PX) {
            return;
        }
        Vector3f local = new Matrix4f(frame).invert().transformDirection(reach).normalize();
        finger.setRotX((float) Math.acos(Mth.clamp(-local.y, -1F, 1F)));
        finger.setRotY((float) Mth.atan2(-local.x, -local.z));
        finger.setRotZ(0F);
        float out = 1F - (float) Math.pow(1F - Mth.clamp((finger.getScaleY() - 1F) / (FINGER_CLIP_PEAK - 1F), 0F, 1F),
                FINGER_SNAP);
        finger.setScaleY(1F + out * (distance / FINGER_LENGTH_PX - 1F));
    }

    private static final float IDLE_BLEND_TICKS = 6F;
    private static final float BOB_TICKS = 64F;
    private static final float BOB_SPREAD = 7F;
    private static final float BOB_RISE = 0.8F;
    private static final float BOB_BREATH = 1.5F * Mth.DEG_TO_RAD;
    private static final float BOB_ARMS = 2F * Mth.DEG_TO_RAD;
    private static final float BOB_HEAD = 1F * Mth.DEG_TO_RAD;
    private static final float BOB_ARM_LAG = 0.6F;
    private static final float BOB_HEAD_LAG = 0.3F;

    private static final Set<String> RESTING_CLIPS = Set.of("idle", "idle_pose");

    private static final Map<Integer, Integer> IDLE_SINCE = new HashMap<>();

    private void applyCustomIdle(StandEntity stand, long instanceId, float partialTick) {
        BedrockAnimation clip = stand.isBarraging() ? null
                : stand.isPreviewMode() ? StandIdles.own() : StandIdles.clip(stand.idleClip());
        if (clip == null || clip.lengthSeconds() <= 0F || !resting(stand, instanceId)) {
            IDLE_SINCE.remove(stand.getId());
            return;
        }
        int since = IDLE_SINCE.computeIfAbsent(stand.getId(), id -> stand.tickCount);
        float w = Mth.clamp((stand.tickCount - since + partialTick) / IDLE_BLEND_TICKS, 0F, 1F);
        float seconds = (stand.tickCount + partialTick) / 20F;
        for (String boneName : clip.boneNames()) {
            if (BARRAGE_BONES.contains(boneName)) {
                continue;
            }
            GeoBone bone = getAnimationProcessor().getBone(boneName);
            if (bone == null) {
                continue;
            }
            BoneSnapshot rest = bone.getInitialSnapshot();
            Vector3f rotation = clip.sampleLooping(boneName, BedrockAnimation.Channel.ROTATION, seconds);
            if (rotation != null) {
                bone.setRotX(Mth.lerp(w, bone.getRotX(), -rotation.x + rest.getRotX()));
                bone.setRotY(Mth.lerp(w, bone.getRotY(), -rotation.y + rest.getRotY()));
                bone.setRotZ(Mth.lerp(w, bone.getRotZ(), rotation.z + rest.getRotZ()));
            }
            Vector3f position = clip.sampleLooping(boneName, BedrockAnimation.Channel.POSITION, seconds);
            if (position != null) {
                bone.setPosX(Mth.lerp(w, bone.getPosX(), position.x));
                bone.setPosY(Mth.lerp(w, bone.getPosY(), -position.y));
                bone.setPosZ(Mth.lerp(w, bone.getPosZ(), position.z));
            }
            Vector3f scale = clip.sampleLooping(boneName, BedrockAnimation.Channel.SCALE, seconds);
            if (scale != null) {
                bone.setScaleX(Mth.lerp(w, bone.getScaleX(), scale.x));
                bone.setScaleY(Mth.lerp(w, bone.getScaleY(), scale.y));
                bone.setScaleZ(Mth.lerp(w, bone.getScaleZ(), scale.z));
            }
        }
        if (clip.still()) {
            bob(stand, w, partialTick);
        }
    }

    private void bob(StandEntity stand, float weight, float partialTick) {
        float phase = (stand.tickCount + partialTick + stand.getId() * BOB_SPREAD) / BOB_TICKS * Mth.TWO_PI;
        float rise = Mth.sin(phase) * weight;
        float arms = Mth.sin(phase - BOB_ARM_LAG) * weight;
        GeoBone whole = getAnimationProcessor().getBone("root");
        if (whole == null) {
            whole = getAnimationProcessor().getBone("Body");
        }
        if (whole != null) {
            whole.setPosY(whole.getPosY() + rise * BOB_RISE);
        }
        GeoBone chest = getAnimationProcessor().getBone("upperTorso");
        if (chest != null) {
            chest.setRotX(chest.getRotX() + rise * BOB_BREATH);
        }
        GeoBone head = getAnimationProcessor().getBone("Head");
        if (head != null) {
            head.setRotX(head.getRotX() - Mth.sin(phase - BOB_HEAD_LAG) * weight * BOB_HEAD);
        }
        GeoBone right = getAnimationProcessor().getBone("RightArm");
        if (right != null) {
            right.setRotZ(right.getRotZ() + arms * BOB_ARMS);
        }
        GeoBone left = getAnimationProcessor().getBone("LeftArm");
        if (left != null) {
            left.setRotZ(left.getRotZ() - arms * BOB_ARMS);
        }
    }

    private static boolean resting(StandEntity stand, long instanceId) {
        AnimatableManager<?> manager =
                stand.getAnimatableInstanceCache().getManagerForId(instanceId);
        AnimationController<?> controller = manager == null ? null
                : manager.getAnimationControllers().get(StandEntity.CONTROLLER);
        return controller != null && controller.getCurrentAnimation() != null && !stand.inCombatFrame()
                && RESTING_CLIPS.contains(controller.getCurrentAnimation().animation().name());
    }

    private void applyBarrageArms(StandEntity animatable, float partialTick) {
        if (animatable.isKickBarrage() && applyKickLegs(animatable, partialTick)) {
            return;
        }
        BedrockAnimation cycle = barrageArms(animatable);
        if (cycle == null || cycle.lengthSeconds() <= 0F) {
            return;
        }

        float seconds = (animatable.tickCount + partialTick) / 20F;

        for (String boneName : BARRAGE_BONES) {
            GeoBone bone = getAnimationProcessor().getBone(boneName);
            if (bone == null) {
                continue;
            }
            if (!cycle.drives(boneName)) {
                if (!boneName.equals("barrage_rig")) {
                    hideArm(bone);
                }
                continue;
            }
            BoneSnapshot rest = bone.getInitialSnapshot();

            poseFromCycle(bone, rest, boneName, cycle, seconds);
            if (!boneName.equals("barrage_rig")) {
                for (int copy = 1; copy <= trailCopies(); copy++) {
                    GeoBone trail = trail(boneName, copy);
                    if (trail != null) {
                        poseFromCycle(trail, rest, boneName, cycle, (float) (seconds - copy * TRAIL_LAG_TICKS / 20.0));
                    }
                }
            }
        }
    }

    private static void poseFromCycle(GeoBone bone, BoneSnapshot rest, String boneName, BedrockAnimation cycle,
                                      float seconds) {
        bone.setRotX(rest.getRotX());
        bone.setRotY(rest.getRotY());
        bone.setRotZ(rest.getRotZ());
        bone.setPosX(rest.getOffsetX());
        bone.setPosY(rest.getOffsetY());
        bone.setPosZ(rest.getOffsetZ());
        bone.setScaleX(rest.getScaleX());
        bone.setScaleY(rest.getScaleY());
        bone.setScaleZ(rest.getScaleZ());

        Vector3f rotation = cycle.sampleLooping(boneName, BedrockAnimation.Channel.ROTATION, seconds);
        if (rotation != null) {
            bone.setRotX(-rotation.x + rest.getRotX());
            bone.setRotY(-rotation.y + rest.getRotY());
            bone.setRotZ(rotation.z + rest.getRotZ());
        }

        Vector3f position = cycle.sampleLooping(boneName, BedrockAnimation.Channel.POSITION, seconds);
        if (position != null) {
            bone.setPosX(position.x);
            bone.setPosY(-position.y);
            bone.setPosZ(position.z);
        }

        Vector3f scale = cycle.sampleLooping(boneName, BedrockAnimation.Channel.SCALE, seconds);
        if (scale != null) {
            bone.setScaleX(scale.x);
            bone.setScaleY(scale.y);
            bone.setScaleZ(scale.z);
        }
    }

    private static final String[] STAB_ARM_BONES = {
            "barrage_arm_right", "barrage_arm_right2", "barrage_arm_right3",
            "barrage_arm_left", "barrage_arm_left2", "barrage_arm_left3"};

    private static final int[] STAB_ORDER = {0, 2, 4, 1, 3, 5};

    private static final int STAB_CYCLE_TICKS = StandEntity.SWORD_HIT_INTERVAL;

    private static final double STAB_REACH_PX = 12.0;

    private static final double STAB_OUT_SHARE = 0.3;

    private static final double STAB_SPREAD_PX = 5.0;

    private static final double STAB_AIM_PX = 40.0;

    private static final double STAB_AIM_HEIGHT_PX = 20.0;

    private static final double STAB_FIND_BLOCKS = 4.0;

    private static final double STAB_FIND_FACING = 0.4;

    private static final int KICK_CYCLE_TICKS = StandEntity.KICK_HIT_INTERVAL;

    private static final double KICK_REACH_PX = 10.0;

    private static final double[] KICK_HIP_PX = {1.0, 13.0, -5.0};

    private static final double[][] KICK_HIP_SPREAD_PX = {
            {6.0, 0.0}, {3.0, 5.0}, {-3.0, 5.0}, {-6.0, 0.0}, {-3.0, -4.0}, {3.0, -4.0}};

    private static final double KICK_AIM_SPREAD = 2.5;

    private boolean applyKickLegs(StandEntity stand, float partialTick) {
        GeoBone rig = getAnimationProcessor().getBone("barrage_rig");
        if (rig == null) {
            return false;
        }

        float facing = Mth.rotLerp(partialTick, stand.yBodyRotO, stand.yBodyRot);
        Vec3 origin = new Vec3(
                Mth.lerp(partialTick, stand.xo, stand.getX()),
                Mth.lerp(partialTick, stand.yo, stand.getY()),
                Mth.lerp(partialTick, stand.zo, stand.getZ()));
        Vec3 rigPivot = new Vec3(rig.getPivotX(), rig.getPivotY(), rig.getPivotZ());
        Vec3 aim = stabAim(stand, origin, facing, partialTick);

        List<String> kicking = Arrays.asList(STAB_ARM_BONES);
        for (String boneName : BARRAGE_BONES) {
            GeoBone bone = getAnimationProcessor().getBone(boneName);
            if (bone != null && !boneName.equals("barrage_rig") && !kicking.contains(boneName)) {
                hideArm(bone);
            }
        }

        double cycles = (stand.tickCount + partialTick) / KICK_CYCLE_TICKS;
        for (int leg = 0; leg < STAB_ARM_BONES.length; leg++) {
            GeoBone bone = getAnimationProcessor().getBone(STAB_ARM_BONES[leg]);
            if (bone == null) {
                continue;
            }
            Vec3[] ends = armEnds(bone);
            if (ends == null) {
                hideArm(bone);
                continue;
            }
            double turn = cycles + STAB_ORDER[leg] / (double) STAB_ARM_BONES.length;
            long kick = (long) Math.floor(turn);
            Vec3 hipModel = new Vec3(
                    KICK_HIP_PX[0] + KICK_HIP_SPREAD_PX[leg][0], KICK_HIP_PX[1] + KICK_HIP_SPREAD_PX[leg][1],
                    KICK_HIP_PX[2]);
            Vec3 spotModel = aim.add(
                    KICK_HIP_SPREAD_PX[leg][0] * KICK_AIM_SPREAD + spread(stand.getId(), leg, kick, 0),
                    KICK_HIP_SPREAD_PX[leg][1] * KICK_AIM_SPREAD + spread(stand.getId(), leg, kick, 1), 0.0);
            Vec3 hip = toRig(hipModel, rigPivot, rig.getRotX());
            Vec3 toward = toRig(spotModel, rigPivot, rig.getRotX()).subtract(hip);
            if (toward.lengthSqr() < 1.0E-4) {
                hideArm(bone);
                continue;
            }
            Vec3 dir = toward.normalize();
            double length = ends[0].y - ends[1].y;
            Vec3 from = hip.add(dir.scale(KICK_REACH_PX * stabExtension(turn - kick)));
            reachArm(bone, ends[0], ends[1], from, from.add(dir.scale(length)));
            FLASH.put(bone, peak((turn - kick) / STAB_OUT_SHARE));
            for (int copy = 1; copy <= trailCopies(); copy++) {
                GeoBone trail = trail(STAB_ARM_BONES[leg], copy);
                double behind = turn - kick - copy * TRAIL_LAG_TICKS / KICK_CYCLE_TICKS;
                if (trail != null && behind >= 0.0) {
                    Vec3 back = hip.add(dir.scale(KICK_REACH_PX * stabExtension(behind)));
                    reachArm(trail, ends[0], ends[1], back, back.add(dir.scale(length)));
                }
            }
        }
        return true;
    }

    private static void hideHeldBones(GeoBone bone) {
        for (GeoBone held : bone.getChildBones()) {
            held.setHidden(true);
            held.setChildrenHidden(false);
            hideHeldBones(held);
        }
    }

    private boolean applyStabArms(StandEntity stand, float partialTick) {
        GeoBone rig = getAnimationProcessor().getBone("barrage_rig");
        GeoBone first = getAnimationProcessor().getBone(STAB_ARM_BONES[0]);
        if (rig == null || first == null || first.getChildBones().isEmpty()) {
            return false;
        }

        float facing = Mth.rotLerp(partialTick, stand.yBodyRotO, stand.yBodyRot);
        Vec3 origin = new Vec3(
                Mth.lerp(partialTick, stand.xo, stand.getX()),
                Mth.lerp(partialTick, stand.yo, stand.getY()),
                Mth.lerp(partialTick, stand.zo, stand.getZ()));
        Vec3 rigPivot = new Vec3(rig.getPivotX(), rig.getPivotY(), rig.getPivotZ());
        Vec3 aim = toRig(stabAim(stand, origin, facing, partialTick), rigPivot, rig.getRotX());

        List<String> stabbing = Arrays.asList(STAB_ARM_BONES);
        for (String boneName : BARRAGE_BONES) {
            GeoBone bone = getAnimationProcessor().getBone(boneName);
            if (bone != null && !boneName.equals("barrage_rig") && !stabbing.contains(boneName)) {
                hideArm(bone);
            }
        }

        Vec3[] lean = {
                shoulderLean(getAnimationProcessor().getBone("RightArm"), rigPivot, rig.getRotX()),
                shoulderLean(getAnimationProcessor().getBone("LeftArm"), rigPivot, rig.getRotX())};

        double cycles = (stand.tickCount + partialTick) / STAB_CYCLE_TICKS;
        for (int arm = 0; arm < STAB_ARM_BONES.length; arm++) {
            GeoBone bone = getAnimationProcessor().getBone(STAB_ARM_BONES[arm]);
            if (bone == null) {
                continue;
            }
            Vec3[] ends = armEnds(bone);
            Vec3 shoulder = ends == null ? null : restPoint(ends[0],
                    new Vec3(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ()),
                    bone.getInitialSnapshot()).add(lean[arm < STAB_ARM_BONES.length / 2 ? 0 : 1]);

            double turn = cycles + STAB_ORDER[arm] / (double) STAB_ARM_BONES.length;
            long stab = (long) Math.floor(turn);
            Vec3 spot = aim.add(spread(stand.getId(), arm, stab, 0), 0.0,
                    spread(stand.getId(), arm, stab, 1));
            Vec3 toward = shoulder == null ? null : spot.subtract(shoulder);
            if (toward == null || toward.lengthSqr() < 1.0E-4) {
                hideArm(bone);
            } else {
                stabArm(bone, ends, shoulder, toward.normalize(), turn - stab);
                FLASH.put(bone, peak((turn - stab) / STAB_OUT_SHARE));
                for (int copy = 1; copy <= trailCopies(); copy++) {
                    GeoBone trail = trail(STAB_ARM_BONES[arm], copy);
                    double behind = turn - stab - copy * TRAIL_LAG_TICKS / STAB_CYCLE_TICKS;
                    if (trail != null && behind >= 0.0) {
                        stabArm(trail, ends, shoulder, toward.normalize(), behind);
                    }
                }
            }
        }
        return true;
    }

    private static Vec3 shoulderLean(GeoBone arm, Vec3 rigPivot,
                                                              double rigAngle) {
        if (arm == null) {
            return Vec3.ZERO;
        }
        Vector3d posed = new Vector3d(arm.getPivotX(), arm.getPivotY(), arm.getPivotZ());
        Vector3d rest = new Vector3d(posed);
        for (GeoBone at = arm.getParent(); at != null; at = at.getParent()) {
            BoneSnapshot bind = at.getInitialSnapshot();
            posed.sub(at.getPivotX(), at.getPivotY(), at.getPivotZ())
                    .rotateX(at.getRotX()).rotateY(at.getRotY()).rotateZ(at.getRotZ())
                    .add(at.getPivotX(), at.getPivotY(), at.getPivotZ())
                    .add(-at.getPosX(), at.getPosY(), at.getPosZ());
            rest.sub(at.getPivotX(), at.getPivotY(), at.getPivotZ())
                    .rotateX(bind.getRotX()).rotateY(bind.getRotY()).rotateZ(bind.getRotZ())
                    .add(at.getPivotX(), at.getPivotY(), at.getPivotZ())
                    .add(-bind.getOffsetX(), bind.getOffsetY(), bind.getOffsetZ());
        }
        return toRig(new Vec3(posed.x, posed.y, posed.z), rigPivot, rigAngle)
                .subtract(toRig(new Vec3(rest.x, rest.y, rest.z), rigPivot, rigAngle));
    }

    private static void stabArm(GeoBone bone, Vec3[] ends, Vec3 shoulder,
                                Vec3 dir, double phase) {
        double length = ends[0].y - ends[1].y;
        Vec3 from = shoulder.add(dir.scale(STAB_REACH_PX * stabExtension(phase)));
        reachArm(bone, ends[0], ends[1], from, from.add(dir.scale(length)));
    }

    private static Vec3 restPoint(Vec3 point,
                                                           Vec3 pivot, BoneSnapshot rest) {
        Vector3d d = new Vector3d(point.x - pivot.x, point.y - pivot.y, point.z - pivot.z)
                .rotateX(rest.getRotX()).rotateY(rest.getRotY()).rotateZ(rest.getRotZ());
        return pivot.add(d.x, d.y, d.z);
    }

    private static double stabExtension(double phase) {
        if (phase < STAB_OUT_SHARE) {
            double t = 1.0 - phase / STAB_OUT_SHARE;
            return 1.0 - t * t * t;
        }
        double t = (phase - STAB_OUT_SHARE) / (1.0 - STAB_OUT_SHARE);
        return 0.5 + 0.5 * Math.cos(Math.PI * t);
    }

    private static double spread(int stand, int arm, long stab, int axis) {
        long seed = stand * 341873128712L + arm * 132897987541L + stab * 2654435761L + axis * 40503L;
        seed ^= seed >>> 29;
        seed *= 0xBF58476D1CE4E5B9L;
        seed ^= seed >>> 32;
        return ((seed & 0xFFFF) / 65535.0 * 2.0 - 1.0) * STAB_SPREAD_PX;
    }

    private static Vec3 stabAim(StandEntity stand, Vec3 origin,
                                                         float yaw, float partialTick) {
        Vec3 forward = StandFist.toWorld(
                Vec3.ZERO, yaw, new Vec3(0, 0, -16));
        Player owner = stand.getOwner();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity body : stand.level().getEntitiesOfClass(
                LivingEntity.class, stand.getBoundingBox().inflate(STAB_FIND_BLOCKS),
                entity -> entity != stand && entity != owner && entity.isAlive() && !(entity instanceof StandEntity))) {
            Vec3 at = body.getPosition(partialTick);
            Vec3 flat = new Vec3(at.x - origin.x, 0, at.z - origin.z);
            double distance = flat.length();
            if (distance < 1.0E-3 || distance > STAB_FIND_BLOCKS || distance >= bestDistance
                    || flat.scale(1.0 / distance).dot(forward) < STAB_FIND_FACING) {
                continue;
            }
            best = body;
            bestDistance = distance;
        }
        if (best == null) {
            return new Vec3(0, STAB_AIM_HEIGHT_PX, -STAB_AIM_PX);
        }
        Vec3 chest = best.getPosition(partialTick).add(0, best.getBbHeight() * 0.6, 0);
        return StandFist.toModel(origin, yaw, chest);
    }

    private static final String[] FIST_ARM_BONES = {
            "barrage_arm_right", "barrage_arm_left", "barrage_arm_right2",
            "barrage_arm_left2", "barrage_arm_right3", "barrage_arm_left3",
            "barrage_arm_right4", "barrage_arm_left4", "barrage_arm_right5",
            "barrage_arm_left5", "barrage_arm_right6", "barrage_arm_left6"};

    static final String[] FIST_ARMS = FIST_ARM_BONES;

    static float[] keepPose(GeoBone bone) {
        return new float[] {bone.getRotX(), bone.getRotY(), bone.getRotZ(), bone.getPosX(), bone.getPosY(),
                bone.getPosZ(), bone.getScaleX(), bone.getScaleY(), bone.getScaleZ()};
    }

    static void restorePose(GeoBone bone, float[] kept) {
        bone.setRotX(kept[0]);
        bone.setRotY(kept[1]);
        bone.setRotZ(kept[2]);
        bone.setPosX(kept[3]);
        bone.setPosY(kept[4]);
        bone.setPosZ(kept[5]);
        bone.setScaleX(kept[6]);
        bone.setScaleY(kept[7]);
        bone.setScaleZ(kept[8]);
    }

    static boolean reachFist(GeoBone rig, GeoBone bone, StandEntity stand, StandFist fist,
                             double age, float grow, float partialTick) {
        return reachFist(rig, bone, stand, fist, StandFist.shoulderModel(fist.slot()), age, grow, partialTick);
    }

    static boolean reachFist(GeoBone rig, GeoBone bone, StandEntity stand, StandFist fist, Vec3 from,
                             double age, float grow, float partialTick) {
        Vec3[] ends = armEnds(bone);
        if (ends == null) {
            hideArm(bone);
            return false;
        }
        float yaw = Mth.rotLerp(partialTick, stand.yBodyRotO, stand.yBodyRot);
        Vec3 origin = stand.getPosition(partialTick).add(BarrageFlow.bodyOffset(stand, partialTick));
        Vec3 right = StandFist.right(yaw);
        Vec3 rigPivot = new Vec3(rig.getPivotX(), rig.getPivotY(), rig.getPivotZ());
        Vec3 fistWorld = fist.at(StandFist.toWorld(origin, yaw, from), right, age);
        Vec3 shoulder = toRig(from, rigPivot, rig.getRotX());
        Vec3 reach = toRig(
                StandFist.toModel(origin, yaw, fistWorld), rigPivot, rig.getRotX());
        reachArm(bone, ends[0], ends[1], shoulder, reach);
        if (grow != 1F && bone.getScaleY() != 0F) {
            bone.setScaleX(grow);
            bone.setScaleZ(grow);
        }
        return true;
    }

    private boolean applyFistArms(StandEntity stand, float partialTick) {
        boolean looping = BarrageLoop.runs(stand);
        if (!looping && !StandFistView.anyLive(stand, partialTick)) {
            return false;
        }
        GeoBone rig = getAnimationProcessor().getBone("barrage_rig");
        if (rig == null) {
            return false;
        }

        float yaw = Mth.rotLerp(partialTick, stand.yBodyRotO, stand.yBodyRot);
        Vec3 origin = new Vec3(
                Mth.lerp(partialTick, stand.xo, stand.getX()),
                Mth.lerp(partialTick, stand.yo, stand.getY()),
                Mth.lerp(partialTick, stand.zo, stand.getZ())).add(BarrageFlow.bodyOffset(stand, partialTick));
        Vec3 right = StandFist.right(yaw);
        Vec3 rigPivot = new Vec3(rig.getPivotX(), rig.getPivotY(), rig.getPivotZ());
        BarrageLoop.Punch[] loop = looping ? BarrageLoop.punches(stand, partialTick) : null;

        for (int slot = 0; slot < FIST_ARM_BONES.length; slot++) {
            GeoBone bone = getAnimationProcessor().getBone(FIST_ARM_BONES[slot]);
            if (bone == null) {
                continue;
            }
            StandFist fist;
            double age;
            float grow = 1F;
            if (loop != null && slot < StandFist.BODY_SLOTS) {
                BarrageLoop.Punch punch = loop[slot];
                fist = punch == null ? null : punch.fist();
                age = punch == null ? 0.0 : punch.fistAge();
                grow = punch != null && punch.focus() ? BarrageLoop.FOCUS_SCALE : 1F;
            } else {
                fist = StandFistView.live(stand, slot, partialTick);
                age = fist == null ? 0.0 : StandFistView.age(fist, partialTick);
            }
            Vec3[] ends = armEnds(bone);
            if (fist == null || ends == null) {
                hideArm(bone);
                continue;
            }
            Vec3 fistWorld = fist.at(
                    StandFist.shoulderWorld(origin, yaw, slot), right, age);
            Vec3 shoulder = toRig(
                    StandFist.shoulderModel(slot), rigPivot, rig.getRotX());
            Vec3 reach = toRig(
                    StandFist.toModel(origin, yaw, fistWorld), rigPivot, rig.getRotX());
            reachArm(bone, ends[0], ends[1], shoulder, reach);
            if (grow != 1F && bone.getScaleY() != 0F) {
                bone.setScaleX(grow);
                bone.setScaleZ(grow);
            }
            FLASH.put(bone, peak(age / StandFist.OUT_TICKS));
            for (int copy = 1; copy <= trailCopies(); copy++) {
                GeoBone trail = trail(FIST_ARM_BONES[slot], copy);
                double behind = age - copy * TRAIL_LAG_TICKS;
                if (trail == null || behind <= 0.0) {
                    continue;
                }
                Vec3 then = fist.at(
                        StandFist.shoulderWorld(origin, yaw, slot), right, behind);
                reachArm(trail, ends[0], ends[1], shoulder, toRig(
                        StandFist.toModel(origin, yaw, then), rigPivot, rig.getRotX()));
            }
        }
        return true;
    }

    private static final double RECOIL_TICKS = 2.0;

    private static final float RECOIL_PITCH = 0.07F;

    private static final float RECOIL_TWIST = 0.06F;

    private static final float LEAN_PITCH = 0.15F;

    private static final double FOCUS_KICK = 1.4;

    private static final int CYCLE_BEAT_TICKS = 3;

    private void applyBarrageRecoil(StandEntity stand, float partialTick) {
        float strength = JojohaClientConfig.BARRAGE_RECOIL.value();
        if (strength <= 0F) {
            return;
        }
        double kick = 0.0;
        double side = 0.0;
        boolean fists = BarrageLoop.runs(stand);
        if (fists) {
            for (BarrageLoop.Punch punch : BarrageLoop.punches(stand, partialTick)) {
                if (punch == null) {
                    continue;
                }
                double fresh = punch.focus()
                        ? punch.age() < BarrageLoop.OUT_TICKS + BarrageLoop.FOCUS_HOLD_TICKS ? FOCUS_KICK : 0.0
                        : punch.age() < BarrageLoop.OUT_TICKS ? 1.0 - punch.age() / BarrageLoop.OUT_TICKS : 0.0;
                if (fresh > kick) {
                    kick = fresh;
                    side = punch.fist().fromRight() ? 1.0 : -1.0;
                }
            }
        }
        for (int slot = 0; slot < StandFist.SLOTS && !BarrageLoop.runs(stand); slot++) {
            StandFist fist = StandFistView.live(stand, slot, partialTick);
            if (fist == null) {
                continue;
            }
            fists = true;
            double age = StandFistView.age(fist, partialTick);
            double fresh = age >= 0.0 && age < RECOIL_TICKS ? 1.0 - age / RECOIL_TICKS : 0.0;
            if (fresh > kick) {
                kick = fresh;
                side = fist.fromRight() ? 1.0 : -1.0;
            }
        }
        if (!fists) {
            int beat = stand.isKickBarrage() ? StandEntity.KICK_HIT_INTERVAL
                    : Chariot.is(stand) ? StandEntity.SWORD_HIT_INTERVAL : CYCLE_BEAT_TICKS;
            double clock = stand.tickCount + partialTick;
            double into = clock % beat;
            kick = into < RECOIL_TICKS ? 1.0 - into / RECOIL_TICKS : 0.0;
            side = ((long) Math.floor(clock / beat)) % 2 == 0 ? 1.0 : -1.0;
        }

        float lean = BarrageAir.lean(stand, partialTick);
        GeoBone body = getAnimationProcessor().getBone("Body");
        if (body != null) {
            body.setRotX(body.getRotX() + strength * (RECOIL_PITCH * (float) kick - LEAN_PITCH * lean));
        }
        GeoBone torso = getAnimationProcessor().getBone("upperTorso");
        if (torso != null) {
            torso.setRotY(torso.getRotY() + strength * RECOIL_TWIST * (float) (kick * side));
        }
    }

    private static final float ROLL_TWIST = 0.1F;

    private static final float ROLL_TILT = 0.045F;

    private static final float PEAK_LEAN = 0.14F;

    private static final float CLOSING_COIL = 0.08F;

    private void applyBarrageFlow(StandEntity stand, float partialTick) {
        if (!BarrageLoop.runs(stand)) {
            return;
        }
        float strength = JojohaClientConfig.BARRAGE_RECOIL.value();
        if (strength <= 0F) {
            return;
        }
        double roll = 0.0;
        for (BarrageLoop.Punch punch : BarrageLoop.punches(stand, partialTick)) {
            if (punch == null) {
                continue;
            }
            double life = BarrageLoop.life(punch.focus());
            double out = Math.sin(Math.PI * Mth.clamp(punch.age() / life, 0.0, 1.0));
            roll += out * (punch.fist().fromRight() ? 1.0 : -1.0) * (punch.focus() ? 1.4 : 1.0);
        }
        float twist = (float) Mth.clamp(roll, -1.4, 1.4);
        float momentum = BarrageFlow.momentum(stand, partialTick);
        float closing = BarrageFlow.closing(stand, partialTick);
        addRotation("upperTorso", 0F, strength * ROLL_TWIST * twist * (1F - closing),
                strength * ROLL_TILT * twist * (1F - closing));
        addRotation("Body", -strength * (PEAK_LEAN * momentum * momentum + CLOSING_COIL * closing), 0F, 0F);
    }

    private static final Map<ResourceLocation, Optional<BedrockAnimation>> BODY_CYCLES =
            new HashMap<>();

    private void applyFistBody(StandEntity stand, float partialTick) {
        BedrockAnimation cycle = bodyCycle(stand);
        if (cycle == null || cycle.lengthSeconds() <= 0F) {
            return;
        }
        double loop = cycle.lengthSeconds();

        List<StandFist> fists = new ArrayList<>(2);
        List<Double> ages = new ArrayList<>(2);
        List<Double> shares = new ArrayList<>(2);
        double weight = 0.0;
        BarrageLoop.Punch[] punches = BarrageLoop.runs(stand) ? BarrageLoop.punches(stand, partialTick) : null;
        for (int slot = 0; slot < StandFist.BODY_SLOTS; slot++) {
            BarrageLoop.Punch punch = punches == null ? null : punches[slot];
            StandFist fist = punches == null
                    ? StandFistView.live(stand, slot, partialTick) : punch == null ? null : punch.fist();
            if (fist == null) {
                continue;
            }
            double age = punch != null ? punch.fistAge() : StandFistView.age(fist, partialTick);
            double share = StandFist.loopShare(age);
            if (share <= 0.0) {
                continue;
            }
            fists.add(fist);
            ages.add(age);
            shares.add(share);
            weight += share;
        }
        double fill = punches != null ? Math.max(0.0, 1.0 - weight) : 0.0;
        float clock = (float) (((stand.tickCount + partialTick) / 20.0) % loop);
        float w = (float) Math.min(1.0, weight + fill);
        if (w <= 0F) {
            return;
        }
        double total = weight + fill;

        for (String boneName : cycle.boneNames()) {
            if (BARRAGE_BONES.contains(boneName)) {
                continue;
            }
            GeoBone bone = getAnimationProcessor().getBone(boneName);
            if (bone == null) {
                continue;
            }
            BoneSnapshot rest = bone.getInitialSnapshot();

            Vector3f rotation = loopPose(cycle, boneName, BedrockAnimation.Channel.ROTATION, fists, ages, shares,
                    total, loop, fill, clock);
            if (rotation != null) {
                bone.setRotX(Mth.lerp(w, bone.getRotX(), -rotation.x + rest.getRotX()));
                bone.setRotY(Mth.lerp(w, bone.getRotY(), -rotation.y + rest.getRotY()));
                bone.setRotZ(Mth.lerp(w, bone.getRotZ(), rotation.z + rest.getRotZ()));
            }
            Vector3f position = loopPose(cycle, boneName, BedrockAnimation.Channel.POSITION, fists, ages, shares,
                    total, loop, fill, clock);
            if (position != null) {
                bone.setPosX(Mth.lerp(w, bone.getPosX(), position.x));
                bone.setPosY(Mth.lerp(w, bone.getPosY(), -position.y));
                bone.setPosZ(Mth.lerp(w, bone.getPosZ(), position.z));
            }
            Vector3f scale = loopPose(cycle, boneName, BedrockAnimation.Channel.SCALE, fists, ages, shares,
                    total, loop, fill, clock);
            if (scale != null && !scale.equals(1F, 1F, 1F)) {
                bone.setScaleX(Mth.lerp(w, bone.getScaleX(), scale.x));
                bone.setScaleY(Mth.lerp(w, bone.getScaleY(), scale.y));
                bone.setScaleZ(Mth.lerp(w, bone.getScaleZ(), scale.z));
            }
        }
    }

    private static Vector3f loopPose(BedrockAnimation cycle, String bone, BedrockAnimation.Channel channel,
                                     List<StandFist> fists,
                                     List<Double> ages, List<Double> shares,
                                     double total, double loop, double fill, float clock) {
        Vector3f pose = new Vector3f();
        if (fill > 0.0) {
            Vector3f held = cycle.sample(bone, channel, clock);
            if (held == null) {
                return null;
            }
            pose.add(held.mul((float) (fill / total)));
        }
        for (int i = 0; i < fists.size(); i++) {
            Vector3f at = cycle.sample(bone, channel, (float) fists.get(i).loopSeconds(ages.get(i), loop));
            if (at == null) {
                return null;
            }
            pose.add(at.mul((float) (shares.get(i) / total)));
        }
        return pose;
    }

    private static BedrockAnimation bodyCycle(StandEntity stand) {
        ResourceLocation file = stand.getStandType().animation();
        Optional<BedrockAnimation> own = BODY_CYCLES.computeIfAbsent(file,
                key -> BedrockAnimation.loadGeckoLib(key, "barrage"));
        Optional<BedrockAnimation> cycle = own.isPresent() ? own
                : BODY_CYCLES.computeIfAbsent(UNIVERSAL_ANIMATIONS, key -> BedrockAnimation.loadGeckoLib(key, "barrage"));
        return cycle.orElse(null);
    }

    private static void hideArm(GeoBone bone) {
        bone.setScaleX(0F);
        bone.setScaleY(0F);
        bone.setScaleZ(0F);
    }

    static Vec3 toRig(Vec3 model, Vec3 pivot, double angle) {
        Vec3 d = model.subtract(pivot);
        double cos = Math.cos(-angle), sin = Math.sin(-angle);
        return pivot.add(d.x, d.y * cos - d.z * sin, d.y * sin + d.z * cos);
    }

    static Vec3[] armEnds(GeoBone bone) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE,
                minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        boolean any = false;
        for (GeoCube cube : bone.getCubes()) {
            if (cube.inflate() != 0) {
                continue;
            }
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) {
                    continue;
                }
                for (GeoVertex vertex : quad.vertices()) {
                    Vector3f p = vertex.position();
                    minX = Math.min(minX, p.x() * 16); maxX = Math.max(maxX, p.x() * 16);
                    minY = Math.min(minY, p.y() * 16); maxY = Math.max(maxY, p.y() * 16);
                    minZ = Math.min(minZ, p.z() * 16); maxZ = Math.max(maxZ, p.z() * 16);
                    any = true;
                }
            }
        }
        if (!any || maxY - minY <= 0) {
            return null;
        }
        double cx = (minX + maxX) / 2, cz = (minZ + maxZ) / 2;
        return new Vec3[] {
                new Vec3(cx, maxY, cz), new Vec3(cx, minY, cz)};
    }

    static void reachArm(GeoBone bone, Vec3 shoulderEnd, Vec3 fistEnd,
                         Vec3 shoulder, Vec3 reach) {
        Vec3 d = reach.subtract(shoulder);
        double length = d.length();
        double rest = shoulderEnd.y - fistEnd.y;
        if (length < 1.0E-4 || rest <= 0) {
            hideArm(bone);
            return;
        }
        Vec3 u = d.scale(1.0 / length);
        double a = Math.asin(Mth.clamp(-u.z, -1.0, 1.0));
        double c = Math.atan2(u.x, -u.y);
        double stretch = length / rest;

        Vec3 pivot = new Vec3(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ());
        Vec3 local = shoulderEnd.subtract(pivot);
        double sy = local.y * stretch;
        double cosA = Math.cos(a), sinA = Math.sin(a);
        double ry = sy * cosA - local.z * sinA;
        double rz = sy * sinA + local.z * cosA;
        double cosC = Math.cos(c), sinC = Math.sin(c);
        double tx = local.x * cosC - ry * sinC;
        double ty = local.x * sinC + ry * cosC;
        Vec3 move = shoulder.subtract(pivot).subtract(tx, ty, rz);

        bone.setRotX((float) a);
        bone.setRotY(0F);
        bone.setRotZ((float) c);
        bone.setPosX((float) -move.x);
        bone.setPosY((float) move.y);
        bone.setPosZ((float) move.z);
        bone.setScaleX(1F);
        bone.setScaleY((float) stretch);
        bone.setScaleZ(1F);
    }

    private static final Map<ResourceLocation, Optional<BedrockAnimation>> ARM_CYCLES = new HashMap<>();

    private static BedrockAnimation barrageArms(StandEntity stand) {
        Optional<BedrockAnimation> own = ARM_CYCLES.computeIfAbsent(stand.getStandType().animation(),
                key -> BedrockAnimation.loadIfPresent(key, "barrage"));
        Optional<BedrockAnimation> cycle = own.isPresent() ? own
                : ARM_CYCLES.computeIfAbsent(UNIVERSAL_ANIMATIONS, key -> BedrockAnimation.load(key, "barrage"));
        return cycle.orElse(null);
    }

    public static final String BONE_ON_RIGHT = "LeftArm";
    public static final String BONE_ON_LEFT = "RightArm";

    private void applyBoundArms(StandEntity animatable, float partialTick) {
        if (!animatable.getStandType().form().isBound()) {
            return;
        }

        Player owner = animatable.getOwner();
        if (owner == null) {
            return;
        }

        float swing = owner.walkAnimation.position(partialTick);
        float amount = owner.walkAnimation.speed(partialTick);
        float age = owner.tickCount + partialTick;

        float right = Mth.cos(swing * 0.6662F + Mth.PI) * 2.0F * amount * 0.5F;
        float left = Mth.cos(swing * 0.6662F) * 2.0F * amount * 0.5F;

        float bobX = Mth.sin(age * 0.067F) * 0.05F;
        float bobZ = Mth.cos(age * 0.09F) * 0.05F + 0.05F;

        float attack = owner.getAttackAnim(partialTick);
        float thrust = Mth.sin(Mth.sqrt(attack) * Mth.PI) * 1.2F;
        boolean rightHanded = owner.getMainArm() == HumanoidArm.RIGHT;

        setRotation(BONE_ON_RIGHT, right + bobX + (rightHanded ? thrust : 0F), 0F, bobZ);
        setRotation(BONE_ON_LEFT, left + bobX + (rightHanded ? 0F : thrust), 0F, -bobZ);
    }

    private static final float HAIR_TRAIL = 88F;
    private static final float MAX_SWING_DEGREES = 42F;

    private void applyHairTrail(StandEntity animatable, float partialTick, boolean armsOnly) {
        if (!animatable.getStandType().form().isFreeStanding()) {
            return;
        }

        if (armsOnly || animatable.isPreviewMode()) {
            return;
        }

        if (animatable.isEngaged() || animatable.isInhaling()) {
            return;
        }

        if (animatable.isPosing() || TimeStopView.active()) {
            return;
        }

        StandLimbFlow flow = animatable.getLimbFlow();
        addRotation("hair", swing(flow.forward().far(partialTick) * HAIR_TRAIL), 0F,
                swing(flow.lateral().far(partialTick) * HAIR_TRAIL));
    }

    private static float swing(float degrees) {
        return (float) Math.toRadians(Mth.clamp(degrees, -MAX_SWING_DEGREES, MAX_SWING_DEGREES));
    }

    private void unstackTrails() {
        for (Map.Entry<GeoBone, Trail> entry : trails.entrySet()) {
            GeoBone bone = entry.getKey();
            Trail trail = entry.getValue();
            if (bone.getRotX() == trail.setX && bone.getRotY() == trail.setY && bone.getRotZ() == trail.setZ) {
                bone.setRotX(bone.getRotX() - trail.addX);
                bone.setRotY(bone.getRotY() - trail.addY);
                bone.setRotZ(bone.getRotZ() - trail.addZ);
            }
            trail.setX = Float.NaN;
            trail.setY = Float.NaN;
            trail.setZ = Float.NaN;
            trail.addX = 0F;
            trail.addY = 0F;
            trail.addZ = 0F;
        }
    }

    private void addRotation(String boneName, float x, float y, float z) {
        GeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone == null) {
            return;
        }

        float baseX = bone.getRotX();
        float baseY = bone.getRotY();
        float baseZ = bone.getRotZ();
        Trail trail = trails.computeIfAbsent(bone, key -> new Trail());
        if (trail.frame == frame) {
            trail.addX += x;
            trail.addY += y;
            trail.addZ += z;
        } else {
            if (baseX == trail.setX && baseY == trail.setY && baseZ == trail.setZ) {
                baseX -= trail.addX;
                baseY -= trail.addY;
                baseZ -= trail.addZ;
            }
            trail.frame = frame;
            trail.addX = x;
            trail.addY = y;
            trail.addZ = z;
        }
        bone.setRotX(baseX + x);
        bone.setRotY(baseY + y);
        bone.setRotZ(baseZ + z);
        trail.setX = bone.getRotX();
        trail.setY = bone.getRotY();
        trail.setZ = bone.getRotZ();
    }

    private static final class Trail {
        private long frame = -1L;
        private float setX = Float.NaN;
        private float setY = Float.NaN;
        private float setZ = Float.NaN;
        private float addX;
        private float addY;
        private float addZ;
    }

    private final Map<GeoBone, Trail> trails = new IdentityHashMap<>();

    private long frame;

    private void setRotation(String boneName, float x, float y, float z) {
        GeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone == null) {
            return;
        }

        bone.setRotX(x);
        bone.setRotY(y);
        bone.setRotZ(z);
    }

    private void setBoneHidden(String boneName, boolean hidden) {
        GeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone != null) {
            bone.setHidden(hidden);
        }
    }
}
