package org.gumel.jojoha.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.client.PlayerStabAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the Stand Arrow ritual pose the player.
 *
 * <p>Two injections, and both are load-bearing:
 *
 * <ul>
 *   <li><b>HEAD</b> wipes any pose left over from a previous stab before vanilla runs. The stab
 *       writes part <em>positions</em>, which vanilla's {@code setupAnim} doesn't reliably reset
 *       each frame the way it does rotations - so without this the model stays warped once the
 *       animation ends. And since a single {@code PlayerModel} renders every player in turn, the
 *       leftovers would otherwise smear onto everyone else on screen too.</li>
 *   <li><b>TAIL</b> applies the stab itself. Injected last rather than cancelling vanilla, because
 *       {@code setupAnim} also handles held items, riding and swimming - cancelling it would leave
 *       all of that stale. Running afterwards means the stab simply wins on the parts it drives.</li>
 * </ul>
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    /** Whether this model still carries a stab pose that needs clearing before vanilla poses it. */
    @Unique
    private boolean jojoha$posedForStab;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void jojoha$clearStabPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                      float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (jojoha$posedForStab) {
            jojoha$posedForStab = false;
            PlayerStabAnimation.resetParts((PlayerModel<?>) (Object) this);
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void jojoha$applyStabAnimation(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                           float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        // Includes the partial tick so the pose advances smoothly between ticks rather than
        // stepping 20 times a second.
        float clientTime = (float) minecraft.level.getGameTime()
                + minecraft.getTimer().getGameTimeDeltaPartialTick(false);

        if (PlayerStabAnimation.apply((PlayerModel<?>) (Object) this, player, clientTime)) {
            jojoha$posedForStab = true;
        }
    }
}
