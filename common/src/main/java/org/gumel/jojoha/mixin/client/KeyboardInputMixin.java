package org.gumel.jojoha.mixin.client;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.gumel.jojoha.client.StandSkillInput;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Holds the player still while they are flying their Stand.
 *
 * <p>Two things want that. Piloting sends the movement keys to the Stand, so the body they belong
 * to has to stop answering them - otherwise the user walks off a ledge while looking through
 * something else's eyes. Being caught in someone's stopped time wants the same thing for a
 * different reason: a player cannot be frozen with NoAi the way a mob can, because their movement
 * is decided on their own client, so stopping them means telling that client to stop listening.
 *
 * <p>Zeroing the input struct is how vanilla itself immobilises a player (it is the same field set
 * the sleeping and riding checks clear), which keeps this consistent with the rest of the game
 * rather than fighting the movement code from outside.
 *
 * <p>Injected at TAIL so it runs after vanilla has read the keyboard: the point is to discard what
 * was read, and doing it beforehand would simply be overwritten.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {

    /**
     * Whether a stun currently has hold of the local player.
     *
     * <p>Taking a mob's speed away stops it dead, but a player's movement does not come from an
     * attribute - it comes from their keyboard. So the same effect has to be read here as well, and
     * the input dropped, or a stunned player would walk about at normal speed.
     */
    private static boolean stunned() {
        net.minecraft.client.player.LocalPlayer player =
                net.minecraft.client.Minecraft.getInstance().player;
        return player != null
                && player.hasEffect(org.gumel.jojoha.registry.ModEffects.stun());
    }

    @Inject(method = "tick(ZF)V", at = @At("TAIL"))
    private void jojoha$holdStillWhilePiloting(boolean isSneaking, float sneakSpeedMultiplier, CallbackInfo ci) {
        if (!StandSkillInput.isPiloting()
                && !ClientPlayerDataCache.data.isTimeStopFrozen()
                && !stunned()) {
            return;
        }

        this.up = false;
        this.down = false;
        this.left = false;
        this.right = false;
        this.forwardImpulse = 0F;
        this.leftImpulse = 0F;
        this.jumping = false;
    }
}
