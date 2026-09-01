package org.gumel.jojoha.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandLimbFlow;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.List;

/** Resolves geo/texture/animation per-instance from the entity's own {@code StandType}. */
public final class StandModel extends GeoModel<StandEntity> {
    // Bone names as authored in star_platinum.geo.json. For a PARTIAL manifestation only the
    // arms (and their forearm children) are visible; everything else is hidden. Body/upperTorso
    // are a special case - their own cubes must vanish while their children keep rendering,
    // since the arms hang off upperTorso and would disappear with a plain setHidden.
    private static final List<String> ARM_BONES =
            List.of("RightArm", "bone2", "finger", "LeftArm", "bone3");
    private static final List<String> TORSO_SPINE_BONES = List.of("Body", "upperTorso");
    private static final List<String> NON_ARM_BONES = List.of("Head", "hair", "RightLeg", "bone", "LeftLeg", "bone4");

    /** How far the head will turn off the body before it gives up and faces front - vanilla's own limits. */
    private static final float MAX_HEAD_YAW_DEGREES = 75F;
    private static final float MAX_HEAD_PITCH_DEGREES = 60F;

    @Override
    public ResourceLocation getModelResource(StandEntity animatable) {
        return animatable.getStandType().model();
    }

    @Override
    public ResourceLocation getTextureResource(StandEntity animatable) {
        return animatable.getStandType().textureFor(animatable.getSkin());
    }

    @Override
    public ResourceLocation getAnimationResource(StandEntity animatable) {
        return animatable.getStandType().animation();
    }

    /**
     * The default GeckoLib render type is entityCutoutNoCull, an alpha-test cutout that ignores
     * a uniform tint alpha (it only uses alpha per-pixel, as a discard threshold, not for
     * blending) - so StandRenderer's fade tint would have no visible effect under it. Switch to
     * a translucent type whenever the model isn't fully opaque, which covers both the spawn
     * fade-in and an EMERGING Stand's constant flicker; cutout the rest of the time is the
     * correct/cheaper choice for solid rendering.
     */
    @Override
    public RenderType getRenderType(StandEntity animatable, ResourceLocation texture) {
        // Must use the same alpha the renderer will tint with, or a Stand thinned only by the
        // first-person fade would still be drawn through the opaque cutout type and stay solid.
        return StandViewAlpha.of(animatable) < 1F
                ? RenderType.entityTranslucent(texture)
                : RenderType.entityCutoutNoCull(texture);
    }

    /**
     * Applies the Trust Tier's manifestation shape. Bone visibility is set explicitly every
     * frame rather than only when partial, because {@code BakedGeoModel}'s {@link GeoBone}
     * instances are cached per model file and shared by every Stand using it - leaving a bone
     * hidden would silently strip the body off an unrelated fully-manifested Stand (or the HUD
     * portrait) the moment a PARTIAL one rendered.
     */
    @Override
    public void setCustomAnimations(StandEntity animatable, long instanceId, AnimationState<StandEntity> animationState) {
        boolean armsOnly = animatable.getTrustTier().isPartialManifestation();

        for (String boneName : ARM_BONES) {
            setBoneHidden(boneName, false);
        }
        for (String boneName : NON_ARM_BONES) {
            setBoneHidden(boneName, armsOnly);
        }
        for (String boneName : TORSO_SPINE_BONES) {
            GeoBone bone = getAnimationProcessor().getBone(boneName);
            if (bone != null) {
                bone.setHidden(armsOnly);
                // Must follow setHidden, which force-hides children as a side effect.
                bone.setChildrenHidden(false);
            }
        }

        applyLimbFlow(animatable, animationState.getPartialTick(), armsOnly);
        applyBoundArms(animatable, animationState.getPartialTick());
        applyHeadTracking(animationState, armsOnly);
    }

    /**
     * Swings a bound Stand's arms with the arms it is growing out of.
     *
     * <p>Computed from the player's own animation state rather than read off their posed model. The
     * tempting version is to reach for the {@code PlayerModel} and copy the rotations across, and it
     * does not work here: that model is one shared instance posed during the player's own render,
     * and nothing orders a Stand's render against its owner's - so on any frame drawn in the wrong
     * order the arms would carry whichever player was drawn last. The same trap that had a skeleton
     * wearing the previous mob's pose.
     *
     * <p>These are vanilla's own numbers instead, out of {@code HumanoidModel.setupAnim} and
     * {@code AnimationUtils.bobArms}: the walk swings the arms in opposite phase, the idle bob keeps
     * them alive while standing still, and an attack throws the swinging arm forward. Deterministic,
     * order-independent, and correct on the frame it is asked for.
     *
     * <h2>The sign</h2>
     *
     * <p>Vanilla's, unnegated. The limb flow above is written against a convention where a positive
     * X rotation swings a limb backward, and these were negated to match it - but that convention
     * was worked out for bones posed by an animation, and it does not carry to bones being set
     * outright from rest. In the model as authored, positive X on an arm swings it the same way
     * vanilla's {@code xRot} does, so vanilla's own numbers go in as they come out.
     */
    /**
     * Which bone actually draws on which side of the player.
     *
     * <p>Backwards from their names, and this is the measurement rather than a hunch. Vanilla puts
     * the right arm at model x -5 and then renders the whole model through {@code scale(-1, -1, 1)},
     * so it lands a fifth of a block to the <em>positive</em> side of the entity. GeckoLib applies
     * no such flip; it negates X on its way into every bone instead, so the bone at geo x +5 - the
     * one the file calls RightArm - lands on the negative side. The two are opposite, and the file's
     * arm names describe the model rather than the body it grows out of.
     *
     * <p>It went unnoticed because vines are symmetric: at rest, and through a walk cycle where both
     * arms swing, a swap is invisible. The punch is what shows it - one arm moves, and it is the
     * wrong one.
     *
     * <p>Stated here once so the two places that care agree - see StandFirstPersonArms.boneFor,
     * which reaches the same conclusion from the same numbers.
     */
    public static final String BONE_ON_RIGHT = "LeftArm";
    public static final String BONE_ON_LEFT = "RightArm";

    private void applyBoundArms(StandEntity animatable, float partialTick) {
        if (!animatable.getStandType().form().isBound()) {
            return;
        }

        net.minecraft.world.entity.player.Player owner = animatable.getOwner();
        if (owner == null) {
            return;
        }

        float swing = owner.walkAnimation.position(partialTick);
        float amount = owner.walkAnimation.speed(partialTick);
        float age = owner.tickCount + partialTick;

        // Opposite phase, which is what makes a walk read as a walk rather than as a shrug.
        float right = Mth.cos(swing * 0.6662F + Mth.PI) * 2.0F * amount * 0.5F;
        float left = Mth.cos(swing * 0.6662F) * 2.0F * amount * 0.5F;

        // The idle bob. Small, and the whole reason a standing player does not look frozen.
        float bobX = Mth.sin(age * 0.067F) * 0.05F;
        float bobZ = Mth.cos(age * 0.09F) * 0.05F + 0.05F;

        // And the punch. Eased so it snaps out and returns slowly, like vanilla's.
        float attack = owner.getAttackAnim(partialTick);
        float thrust = Mth.sin(Mth.sqrt(attack) * Mth.PI) * 1.2F;
        boolean rightHanded = owner.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;

        // Set, not added, and that is the entire difference between arms that swing and arms that
        // spin. Adding is right when there is an animation underneath to layer onto - which is what
        // the limb flow above does - but a bound Stand has a stub animation with no keyframes in it,
        // so nothing ever puts these bones back. Every frame added its swing to the last frame's and
        // the arms wound up like a clock spring.
        // By side, not by name. The value worked out for the player's right arm goes to whichever
        // bone is drawn on their right, which is the one the file calls LeftArm - see above.
        setRotation(BONE_ON_RIGHT, right + bobX + (rightHanded ? thrust : 0F), 0F, bobZ);
        setRotation(BONE_ON_LEFT, left + bobX + (rightHanded ? 0F : thrust), 0F, -bobZ);
    }

    // How far each joint swings, in degrees per block travelled in a tick. Outboard joints get
    // larger numbers than the ones they hang off: the same motion has further to travel by the
    // time it reaches them, and a limb whose end moves less than its root reads as stiff.
    //
    // Sign convention, which is easy to get backwards: GeckoLib feeds bone rotations straight into
    // Axis.XP with no handedness flip between them and the body's own bank, so a POSITIVE X
    // rotation swings a limb BACKWARD. Trailing behind the direction of travel therefore means
    // adding a positive multiple of the forward speed, not subtracting one.
    private static final float SHOULDER_SWING = 62F;
    private static final float ELBOW_SWING = 78F;
    private static final float HIP_SWING = 74F;
    private static final float KNEE_SWING = 58F;
    private static final float HEAD_COUNTER = 26F;
    private static final float HAIR_TRAIL = 88F;
    /** Extra kick from how hard the limb is being dragged right now - see StandLimbFlow.Axis.whip. */
    private static final float WHIP_GAIN = 46F;
    /** Nothing swings past this, however fast the Stand is thrown around. */
    private static final float MAX_SWING_DEGREES = 42F;

    /**
     * Streams the limbs behind the body's motion.
     *
     * <p>Runs after the animation has been applied - GeckoLib calls {@code tickAnimation} and only
     * then {@code setCustomAnimations} - so these are added to the posed rotation rather than
     * replacing it. That is what keeps an idle or a barrage playing normally underneath while the
     * flight still shows in the limbs.
     *
     * <p>Skipped for a partial manifestation, whose arms are pinned to the player's body and are
     * meant to look welded there, and for the HUD portrait, which never moves and would otherwise
     * inherit whatever the last real Stand on screen was doing.
     */
    private void applyLimbFlow(StandEntity animatable, float partialTick, boolean armsOnly) {
        // A bound Stand has no gait. Its arms are vines, not limbs, and swinging them to the walk
        // cycle makes them row through the air as their user strolls about. Everything below this
        // line describes how a body carries itself, which is a question that does not apply.
        if (!animatable.getStandType().form().isFreeStanding()) {
            return;
        }

        if (armsOnly || animatable.isPreviewMode()) {
            return;
        }

        // Squared up on a target, the limbs hold the pose the attack animation put them in. Trailing
        // them behind the Stand's travel is right for a drift and wrong for a stance - it would drag
        // the arms out of a punch that is meant to be aimed.
        //
        // Inhaling is the same claim for a different reason. The Stand is planted on an anchor for
        // the whole of that move rather than following its user - see StandEntity.beginInhale - so
        // it is not travelling anywhere and there is no drift for the limbs to trail behind. What
        // the flow found instead was the jump onto the anchor, read as one enormous tick of travel,
        // and passed it down the chain: the hair is the last and loosest joint on it, which is why
        // that is where testers saw it. The move has its own animation and it should have the model
        // to itself.
        if (animatable.isEngaged() || animatable.isInhaling()) {
            return;
        }

        StandLimbFlow flow = animatable.getLimbFlow();
        StandLimbFlow.Axis forward = flow.forward();
        StandLimbFlow.Axis lateral = flow.lateral();
        StandLimbFlow.Axis vertical = flow.vertical();

        float whip = forward.whip(partialTick) * WHIP_GAIN;

        // Arms sweep back as the Stand drives forward and rise as it climbs. The lateral term is
        // mirrored between the two so a sideways slide fans them both the same way through the
        // world, instead of pinching them together or splaying them apart.
        float armSwing = swing(forward.near(partialTick) * SHOULDER_SWING + vertical.near(partialTick) * SHOULDER_SWING);
        float armSplay = swing(lateral.mid(partialTick) * SHOULDER_SWING);
        addRotation("RightArm", armSwing, 0F, armSplay);
        addRotation("LeftArm", armSwing, 0F, armSplay);

        // Forearms run on the slower stage plus the whip, so they arrive after the shoulders and
        // snap through on a direction change rather than tracking them rigidly.
        float forearm = swing(forward.mid(partialTick) * ELBOW_SWING + whip);
        addRotation("bone2", forearm, 0F, 0F);
        addRotation("bone3", forearm, 0F, 0F);

        // Legs hang, so they trail hardest and sway across the direction of travel.
        float legSwing = swing(forward.mid(partialTick) * HIP_SWING);
        float legSway = swing(lateral.far(partialTick) * HIP_SWING);
        addRotation("RightLeg", legSwing, 0F, legSway);
        addRotation("LeftLeg", legSwing, 0F, legSway);

        float shin = swing(forward.far(partialTick) * KNEE_SWING + whip * 0.6F);
        addRotation("bone", shin, 0F, 0F);
        addRotation("bone4", shin, 0F, 0F);

        // The head holds its line while the body pitches under it, which is what stops the Stand
        // reading as a thrown object - it looks like it is going somewhere on purpose.
        addRotation("Head", swing(forward.near(partialTick) * HEAD_COUNTER), 0F,
                swing(-lateral.near(partialTick) * HEAD_COUNTER));

        // Hair is the last thing to hear about any of it.
        addRotation("hair", swing(forward.far(partialTick) * HAIR_TRAIL), 0F,
                swing(lateral.far(partialTick) * HAIR_TRAIL));
    }

    /**
     * Turns the head to look where its user is looking.
     *
     * <p>The numbers are the ones GeckoLib works out for every entity it draws - the head's yaw
     * measured against its own body, and its pitch - so this is the same head tracking a vanilla mob
     * gets, taken from the same place. The signs are GeckoLib's too: its own head-turning model
     * applies both unnegated, which is worth stating because nothing else in this file rotates a bone
     * about Y and there was no other calibrated example to copy.
     *
     * <p>Set outright rather than added, which is what GeckoLib itself does and what the first
     * version of this got wrong. The punch animation turns the head 58 degrees to one side and is
     * authored {@code hold_on_last_frame}, so it keeps that pose after it finishes - and adding a
     * gaze on top of it left the head pointing somewhere neither the animation nor the player asked
     * for, and stuck there until another animation took over. That was the head sticking to the side
     * after a punch.
     *
     * <p>Aim is absolute by nature. Where the head points is not an offset from anything, so an
     * animation's own head turn has to lose to it - a head that follows your gaze cannot also be
     * doing something else with its yaw.
     *
     * <p>Roll is left alone, so the lean the limb flow puts on the head still reads.
     *
     * <p>Clamped, because the follow lets the body lag behind a fast turn - so for a few ticks the
     * gap between where the body points and where the eyes are can be most of a circle, and without a
     * limit the head would briefly face backwards.
     *
     * <p>Skipped for a partial manifestation, which is a pair of arms with no head to turn.
     */
    private void applyHeadTracking(AnimationState<StandEntity> animationState, boolean armsOnly) {
        if (armsOnly) {
            return;
        }
        EntityModelData model = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (model == null) {
            return;
        }

        float yaw = Mth.clamp(model.netHeadYaw(), -MAX_HEAD_YAW_DEGREES, MAX_HEAD_YAW_DEGREES);
        float pitch = Mth.clamp(model.headPitch(), -MAX_HEAD_PITCH_DEGREES, MAX_HEAD_PITCH_DEGREES);

        GeoBone head = getAnimationProcessor().getBone("Head");
        if (head != null) {
            head.setRotX(pitch * Mth.DEG_TO_RAD);
            head.setRotY(yaw * Mth.DEG_TO_RAD);
        }
    }

    private static float swing(float degrees) {
        return (float) Math.toRadians(Mth.clamp(degrees, -MAX_SWING_DEGREES, MAX_SWING_DEGREES));
    }

    /**
     * Adds to a bone's posed rotation instead of setting it.
     *
     * <p>{@code setRotX} and friends overwrite whatever the animation just put there, so reading
     * the current value first is the difference between layering flight on top of the pose and
     * deleting the pose.
     */
    private void addRotation(String boneName, float x, float y, float z) {
        GeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone == null) {
            return;
        }

        bone.setRotX(bone.getRotX() + x);
        bone.setRotY(bone.getRotY() + y);
        bone.setRotZ(bone.getRotZ() + z);
    }

    /**
     * Puts a bone at a rotation outright.
     *
     * <p>The counterpart to {@link #addRotation}, and the one to reach for when nothing else is
     * posing the bone. Anything that runs every frame against an unanimated bone has to be absolute
     * or it integrates itself.
     */
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
