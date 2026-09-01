package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.gumel.jojoha.stand.StandEntity;
import net.minecraft.client.renderer.texture.OverlayTexture;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public final class StandRenderer extends GeoEntityRenderer<StandEntity> {
    /**
     * A real emissive pass, drawn from a mask rather than invented.
     *
     * <p>The alternative considered first was re-drawing the whole model on an emissive render type
     * at low alpha, and it is worse in the way that matters: it glows <em>uniformly</em>. Every pixel
     * of the Stand lifts by the same amount, so the thorns glow exactly as much as the skin between
     * them and the result reads as a flat wash rather than as anything being lit.
     *
     * <p>This draws only where the artwork says to. GeckoLib looks for a second texture beside the
     * first with {@code _glowmask} on the end, and lights whatever is opaque in it - so which parts
     * glow, and how brightly, are decisions made in the image rather than in a constant here.
     *
     * <p>A Stand with no mask file simply does not glow. The layer is added for every Stand because
     * the absence of the texture is the switch, which means the next Stand that wants a glow needs
     * to ship a mask and nothing else.
     */
    private void addGlowLayer() {
        // StandGlowLayer rather than GeckoLib's own, which throws outright on a Stand with no mask
        // and takes the whole portrait down with it - see that class.
        //
        // Added more than once on purpose. Each layer is a translucent draw of the mask, so laying
        // several down builds the lit parts up to something that reads at noon rather than only
        // after dark - the same reason the vine draws its own glow three times.
        for (int i = 0; i < ThornRope.GLOW_PASSES; i++) {
            addRenderLayer(new StandGlowLayer(this));
        }
    }

    /** Degrees of lean per block-per-tick of travel, and the ceiling on it. */
    private static final float BANK_PER_SPEED = 260F;
    private static final float MAX_BANK_DEGREES = 26F;
    /** How quickly the lean catches up, so it eases in and out instead of snapping. */
    private static final float BANK_SMOOTHING = 0.25F;
    /** Movement below this (blocks/tick) counts as standing still and produces no lean. */
    private static final double BANK_DEADZONE = 0.012;

    private float bankRoll;
    private float bankPitch;

    public StandRenderer(EntityRendererProvider.Context context) {
        super(context, new StandModel());
        addRenderLayer(new StandBarrageArmsLayer(this));
        addRenderLayer(new StandHeldItemLayer(this));
    }

    /**
     * Draws the guard pane after the Stand itself.
     *
     * <p>Placed here rather than in a render layer because the pane is built in world axes. A layer
     * is handed the pose with the entity's rotation and GeckoLib's model transform already on it,
     * so anything positioned by hand inside one depends on which way that space happens to point.
     * On the way out of {@code super.render} the stack is balanced back to the entity's origin with
     * no rotation applied, which is the frame the pane wants - see {@link StandGuardPlate}.
     */
    @Override
    public void render(StandEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        // Nothing to draw at all, rather than everything drawn at zero alpha. A fully transparent
        // model still walks its bones, fills a buffer and takes a draw call, and on the frames a
        // bound Stand is hidden that is the whole of its cost for no pixels.
        if (StandViewAlpha.of(entity) <= 0.001F) {
            return;
        }

        // No light floor here any more. The glow layer draws the lit parts from the Stand's own
        // mask, and brightening the whole model underneath it as well was the same glow applied
        // twice - once where the artwork asked for it and once everywhere.
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        StandGuardPlate.render(entity, partialTick, poseStack, bufferSource);
    }

    {
        addGlowLayer();
    }

    /**
     * Puts a bound Stand exactly on its user, every frame.
     *
     * <p>Pinning the entity to the owner's position on the server was not enough and could not have
     * been. The Stand is still an entity, so the client sees its position arrive twenty times a
     * second and interpolates between those samples, while the player it is supposed to be growing
     * out of is drawn from their own interpolation at the full frame rate. Two different smoothings
     * of the same path do not agree, and the gap between them is the lag - it opens the moment the
     * player accelerates and closes when they stop, which is exactly the tell.
     *
     * <p>So this does not try to make the entity keep up. It cancels the entity's own position
     * outright - subtracting where the renderer was about to put it and adding where the owner
     * actually is this frame - and both halves of that come from the same interpolation. There is
     * nothing left to drift, because the Stand is no longer being positioned by its own state at
     * all.
     *
     * <p>Free-standing Stands are untouched. Their lag is the point: a figure trailing its user on a
     * spring is what makes it read as following rather than as being carried.
     */
    @Override
    public net.minecraft.world.phys.Vec3 getRenderOffset(StandEntity entity, float partialTick) {
        net.minecraft.world.entity.player.Player owner = entity.getOwner();
        if (owner == null || !entity.getStandType().form().isBound()) {
            return super.getRenderOffset(entity, partialTick);
        }

        return new net.minecraft.world.phys.Vec3(
                net.minecraft.util.Mth.lerp(partialTick, owner.xo, owner.getX())
                        - net.minecraft.util.Mth.lerp(partialTick, entity.xo, entity.getX()),
                net.minecraft.util.Mth.lerp(partialTick, owner.yo, owner.getY())
                        - net.minecraft.util.Mth.lerp(partialTick, entity.yo, entity.getY()),
                net.minecraft.util.Mth.lerp(partialTick, owner.zo, owner.getZ())
                        - net.minecraft.util.Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }

    /**
     * Leans the Stand into its own movement.
     *
     * <p>Mobs stay bolt upright because they walk; something that flies banks into a turn and
     * tips forward as it accelerates. Adding that on top of the pose is a large part of what
     * makes this read as gliding through the world rather than being dragged along it.
     *
     * <p>The tilt is derived from where the entity actually moved this tick rather than from its
     * velocity field, because the Stand is positioned with setPos every tick and never builds up
     * a delta-movement vector to read.
     */
    @Override
    protected void applyRotations(StandEntity animatable, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick, float nativeScale) {
        // A bound Stand turns with the body it is attached to, taken from the owner's own
        // interpolation for the same reason the position is - and then stops, because the lean below
        // is a flying body banking into its own movement and these arms are not flying anywhere.
        net.minecraft.world.entity.player.Player boundTo =
                animatable.getStandType().form().isBound() ? animatable.getOwner() : null;
        if (boundTo != null) {
            super.applyRotations(animatable, poseStack, ageInTicks,
                    net.minecraft.util.Mth.rotLerp(partialTick, boundTo.yBodyRotO, boundTo.yBodyRot),
                    partialTick, nativeScale);
            return;
        }

        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);

        // A Stand that has locked onto something, or planted itself in a block, squares up: no lean,
        // no roll, upright on its own plane. The tilt is there to sell drifting along beside its
        // user, and it works against both of those - reading a fight is harder when the thing
        // fighting is pitching about, and a guard that rolls with its user's every sidestep does not
        // look like it is holding anything. Zeroed as a target rather than applied instantly, so the
        // existing smoothing eases it upright and eases it back when the Stand goes back to
        // following.
        // A partial manifestation is a pair of arms hanging off their user's shoulders, and the
        // lean is a whole body banking into its own travel. Applied to arms it reads as exactly what
        // was reported: they tip away from the player as he moves and stop looking attached.
        //
        // Zeroed as a target rather than skipped outright, so the existing smoothing eases them
        // upright instead of snapping when the manifestation changes.
        // Everything that counts as fighting or working, not just the two cases this originally
        // named. A barrage was the one that gave it away: the flurry displaces the Stand about a
        // fifth of a block a tick, and at 260 degrees per block-per-tick that asks for 52 degrees of
        // pitch against a 26 degree clamp - so the bank sat pinned at its limit and swung rail to
        // rail several times a second. Read as flicker, and no wonder.
        //
        // The reasoning was already written here for the guard and the pursuit: a body that is
        // fighting should be squared up, because a fight is harder to read when the thing throwing
        // the punches is pitching about. A strike and a flurry are the same case, and the lunge
        // out to the attack position is fast enough to pin the clamp on its own.
        org.gumel.jojoha.stand.StandMovementState state = animatable.movementState();
        boolean squaredUp = state.isCombatStance()
                || state == org.gumel.jojoha.stand.StandMovementState.PURSUING
                || state == org.gumel.jojoha.stand.StandMovementState.WORKING
                || animatable.getTrustTier().isPartialManifestation();

        double dx = squaredUp ? 0.0 : animatable.getX() - animatable.xo;
        double dz = squaredUp ? 0.0 : animatable.getZ() - animatable.zo;

        // Split travel into "along my facing" and "across it" so forward motion tips the body
        // forward while sideways motion rolls it, the way a banking turn actually works.
        // Positive forward is the way it is looking; positive lateral is to its left.
        float yawRadians = (float) Math.toRadians(rotationYaw);
        double forward = -dx * Math.sin(yawRadians) + dz * Math.cos(yawRadians);
        double lateral = dx * Math.cos(yawRadians) + dz * Math.sin(yawRadians);

        // Below the deadzone the Stand is effectively holding station, and the tiny residual
        // spring jitter there would otherwise be magnified into a visible twitch.
        if (Math.abs(lateral) < BANK_DEADZONE) {
            lateral = 0.0;
        }
        if (Math.abs(forward) < BANK_DEADZONE) {
            forward = 0.0;
        }

        // Both signs are negated against the intuitive reading, because a positive rotation about
        // these axes tips the model the opposite way to what the names suggest. Vanilla pins it
        // down: elytra flight is applied as XP.rotationDegrees(-90 - xRot), and that lays a
        // standing player out face-first along their flight path - so negative pitches forward.
        // The same holds for roll, where positive lifts the model's right side and leans it left.
        float targetRoll = (float) Mth.clamp(lateral * BANK_PER_SPEED, -MAX_BANK_DEGREES, MAX_BANK_DEGREES);
        float targetPitch = (float) Mth.clamp(-forward * BANK_PER_SPEED, -MAX_BANK_DEGREES, MAX_BANK_DEGREES);

        bankRoll = Mth.lerp(BANK_SMOOTHING, bankRoll, targetRoll);
        bankPitch = Mth.lerp(BANK_SMOOTHING, bankPitch, targetPitch);

        poseStack.mulPose(Axis.ZP.rotationDegrees(bankRoll));
        poseStack.mulPose(Axis.XP.rotationDegrees(bankPitch));
    }

    /**
     * Drives the spawn fade-in, an EMERGING Stand's ongoing flicker, and the withdrawal fade-out
     * - see StandEntity.getRenderAlpha() for the curve and StandModel.getRenderType() for why the
     * render type has to follow it.
     */
    /**
     * Turns the Stand white as a time stop is wound up.
     *
     * <p>The overlay is vanilla's own white channel, so this needs no extra pass, no second render
     * type and no shader - the model is already being drawn with an overlay coordinate and this
     * simply says what it should be. Taken as the greater of whatever was already asked for and the
     * wind-up, so a Stand being hurt mid-charge still flashes.
     */
    @Override
    public int getPackedOverlay(StandEntity animatable, float u, float partialTick) {
        // The greater of the two whites. A time stop wind-up only ever burns the caster's own
        // Stand, so it is asked per-viewer; a skin swap is a spectacle and rides the entity's own
        // synced clock, so everyone watching sees it. Nothing stops both being true at once.
        float white = Math.max(StandCastGlow.whiteFor(animatable), animatable.skinSwapWhite(partialTick));
        if (white <= 0.001F) {
            return super.getPackedOverlay(animatable, u, partialTick);
        }

        return OverlayTexture.pack(OverlayTexture.u(Math.max(u, white)), OverlayTexture.v(false));
    }

    @Override
    public Color getRenderColor(StandEntity animatable, float partialTick, int packedLight) {
        return Color.ofRGBA(1F, 1F, 1F, StandViewAlpha.of(animatable));
    }

    /**
     * No floating "Stand" label. This has to be overridden on the <em>renderer</em>, not the
     * entity: GeoEntityRenderer replaces vanilla's nameplate check with its own team/distance
     * logic and never consults {@code Entity.shouldShowName()}, so the entity-side override alone
     * was silently ignored and the tag rendered anyway.
     */
    @Override
    public boolean shouldShowName(StandEntity animatable) {
        return false;
    }

}
