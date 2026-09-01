package org.gumel.jojoha.mixin.client;

import net.minecraft.client.Minecraft;
import org.gumel.jojoha.client.StandHandsInput;
import org.gumel.jojoha.network.NetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Takes the right-click away from the player and gives it to their Stand.
 *
 * <p>{@code startUseItem} is the single funnel every use goes through - block, entity and air alike
 * - so intercepting it at the head is enough to cover all of them without knowing which one this
 * click would have been. Nothing about vanilla's own path is touched: it is cancelled outright, and
 * the server is asked to run the same act from the Stand's position instead. See {@code
 * StandHands} for what happens on the other end.
 *
 * <p>{@code rightClickDelay} is set here because cancelling at HEAD skips the assignment that would
 * have set it. Without that, the method is re-entered on every tick the button is held and the
 * server is asked twenty times a second for something it is rate-limited to doing five times.
 */
@Mixin(Minecraft.class)
public abstract class StandUseItemMixin {
    @Shadow
    private int rightClickDelay;

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void jojoha$delegateUseToStand(CallbackInfo ci) {
        if (!StandHandsInput.shouldDelegate()) {
            return;
        }

        this.rightClickDelay = StandHandsInput.REPEAT_DELAY_TICKS;

        // A drag places once, when the button comes up - see StandStretch. This runs on the way
        // down and would otherwise lay a block every four ticks underneath the run being sized, so
        // in any shape that stretches it cancels vanilla's use and then says nothing.
        //
        // Asked of the shape rather than of whether a drag is currently under way, and the
        // difference is a real bug rather than a nicety. handleKeybinds runs at offset 363 of
        // Minecraft.tick, and the client tick that plants the anchor is injected at its tail - so
        // on the very tick the button goes down there is no drag yet, and a check for one would say
        // no and place a block. That was the stray block appearing on top of the one you aimed at,
        // every single time a run was started. The shape is known before the press and cannot lag.
        if (!org.gumel.jojoha.client.StandStretch.gestureOwnsClick()) {
            NetworkHandler.sendStandUseItem(java.util.Optional.empty());
        }
        ci.cancel();
    }
}
