package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.gumel.jojoha.client.StandAfterimages;
import org.gumel.jojoha.client.PilotPose;
import org.gumel.jojoha.client.StandFirstPersonArms;
import org.gumel.jojoha.client.StandAwakeningRays;
import org.gumel.jojoha.client.StandAwakeningGlowLayer;
import org.gumel.jojoha.client.StoneMaskLayer;
import org.gumel.jojoha.client.StoneMaskModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the Stand-awakening rays around a player mid-ritual (see {@link StandAwakeningRays}) and
 * attaches the layer that lights their body up while it happens.
 *
 * <p>The layer is added from the constructor rather than through each loader's own
 * add-layers event, so one hook covers both platforms.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin
        extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** The pilot's real rotation, borrowed for the length of one draw. */
    @Unique
    private float[] jojoha$heldPose;

    private PlayerRendererMixin(EntityRendererProvider.Context context,
                                PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void jojoha$addAwakeningGlowLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        this.addLayer(new StandAwakeningGlowLayer(this));

        // A bound Stand is drawn here rather than as an entity of its own - see
        // StandBoundArmsLayer for why an entity could only ever imitate being attached.
        this.addLayer(new org.gumel.jojoha.client.StandBoundArmsLayer(this));
        // Baked here because this is where a context exists to bake from. The mask is a model
        // rather than a texture on the skin, so it needs real geometry handed to it.
        this.addLayer(new StoneMaskLayer(this, context.bakeLayer(StoneMaskModel.LAYER)));
    }
    /**
     * The body yaw a pilot is drawn at, which is the one they had when they took off.
     *
     * <p>Swapped at the argument rather than on the entity, because the dispatcher worked this value
     * out before the render was ever called - writing the field here would be too late for it. See
     * PilotPose for why the rotation itself cannot simply be frozen.
     */
    @ModifyVariable(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float jojoha$holdPilotBody(float entityYaw, AbstractClientPlayer player) {
        float[] held = PilotPose.of(player);
        return held == null ? entityYaw : held[0];
    }

    /**
     * And the head with it, for the half of the pose the argument does not carry.
     *
     * <p>Freezing the body alone would be worse than freezing nothing: the head is drawn as an
     * offset from the body, so a still body under a head that still tracks the mouse gives a neck
     * doing full rotations. Both halves or neither.
     *
     * <p>Written onto the entity and put back at the end of the draw, because these are read from
     * fields rather than passed in. The previous-tick copies go too - the renderer interpolates
     * between them, and leaving those would leave the spin in the interpolation.
     */
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void jojoha$holdPilotHead(AbstractClientPlayer player, float entityYaw, float partialTick,
                                      PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, CallbackInfo ci) {
        float[] held = PilotPose.of(player);
        if (held == null) {
            return;
        }

        jojoha$heldPose = new float[]{player.yHeadRot, player.yHeadRotO,
                player.getXRot(), player.xRotO, player.yBodyRot, player.yBodyRotO};

        player.yHeadRot = held[1];
        player.yHeadRotO = held[1];
        player.setXRot(held[2]);
        player.xRotO = held[2];
        player.yBodyRot = held[0];
        player.yBodyRotO = held[0];
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void jojoha$releasePilotHead(AbstractClientPlayer player, float entityYaw, float partialTick,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, CallbackInfo ci) {
        if (jojoha$heldPose == null) {
            return;
        }

        // Put back on the way out. These are the live entity's own fields, and everything else that
        // reads them this tick - the steering included - must see the real ones.
        player.yHeadRot = jojoha$heldPose[0];
        player.yHeadRotO = jojoha$heldPose[1];
        player.setXRot(jojoha$heldPose[2]);
        player.xRotO = jojoha$heldPose[3];
        player.yBodyRot = jojoha$heldPose[4];
        player.yBodyRotO = jojoha$heldPose[5];
        jojoha$heldPose = null;
    }

    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL"))
    private void jojoha$renderAwakeningRays(AbstractClientPlayer player, float entityYaw, float partialTick,
                                            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                            CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float clientTime = (float) minecraft.level.getGameTime() + partialTick;
        StandAwakeningRays.render(player, poseStack, bufferSource, clientTime);
        StandAfterimages.renderTrail(player, getModel(), poseStack, bufferSource, partialTick,
                minecraft.level.getGameTime());
    }

    /**
     * Grows a bound Stand out of the arm vanilla just drew.
     *
     * <p>At TAIL rather than HEAD so the vines sit over the skin instead of under it, and on these
     * two methods rather than on the frame because they are exactly the moments an arm is visible -
     * the empty hand and the two-handed map. Vanilla draws no arm while an item is held, and neither
     * does this.
     *
     * <p>The pose stack here is vanilla model space, untouched by the {@code ModelPart} render that
     * came before it - {@code ModelPart.render} balances its own push and pop - which is the frame
     * StandFirstPersonArms is written against.
     */
    @Inject(method = "renderRightHand", at = @At("TAIL"))
    private void jojoha$standOnRightArm(PoseStack poseStack, MultiBufferSource buffers, int light,
                                        AbstractClientPlayer player, CallbackInfo ci) {
        StandFirstPersonArms.render(poseStack, buffers, light, player, HumanoidArm.RIGHT);
    }

    @Inject(method = "renderLeftHand", at = @At("TAIL"))
    private void jojoha$standOnLeftArm(PoseStack poseStack, MultiBufferSource buffers, int light,
                                       AbstractClientPlayer player, CallbackInfo ci) {
        StandFirstPersonArms.render(poseStack, buffers, light, player, HumanoidArm.LEFT);
    }
}
