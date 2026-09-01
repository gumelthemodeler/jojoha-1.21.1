package org.gumel.jojoha.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.gumel.jojoha.client.CentralBarOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lifts vanilla's hearts, armour, hunger and air clear of the combat bar.
 *
 * <p>Done by translating the matrix around the whole status draw rather than by moving each row.
 * Vanilla lays those rows out relative to the bottom of the screen in a single private method, and
 * shifting the one thing they are all measured from moves them together and keeps their spacing -
 * whereas patching each row's y would mean re-deriving a layout that is not ours, and re-deriving it
 * again every time it changes.
 *
 * <p>Mounted health gets the same treatment: it replaces the hunger row when riding, in the same
 * place, and would otherwise be the one row left underneath the bar.
 */
@Mixin(Gui.class)
public abstract class GuiStatusBarMixin {
    /**
     * Whether the push actually happened.
     *
     * <p>The lift is read once, on the way in, and this remembers the answer for the way out. Asking
     * again at the end would risk a pop without a push if the bar were toggled off mid-draw, which
     * unbalances the matrix stack for everything drawn afterwards.
     */
    private boolean jojoha$lifted;

    /** Whether this frame's item name and chat were drawn lifted, tracked separately - see push. */
    @Unique
    private boolean jojoha$itemNameLifted;

    @Unique
    private boolean jojoha$chatLifted;

    /**
     * Hides the experience bar while the combat bar is up.
     *
     * <p>They occupy the same strip of screen and the move wells are the thing you need to read, so
     * the two cannot both have it. Answered at vanilla's own question rather than by cancelling the
     * draw: it is the one place that already decides whether the bar exists this frame, and the level
     * number is laid out from the same answer.
     */
    @Inject(method = "isExperienceBarVisible", at = @At("HEAD"), cancellable = true)
    private void jojoha$hideExperienceBar(CallbackInfoReturnable<Boolean> cir) {
        if (CentralBarOverlay.showing()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void jojoha$hideExperienceLevel(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (CentralBarOverlay.showing()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPlayerHealth", at = @At("HEAD"))
    private void jojoha$liftStatus(GuiGraphics graphics, CallbackInfo ci) {
        jojoha$lift(graphics);
    }

    @Inject(method = "renderPlayerHealth", at = @At("RETURN"))
    private void jojoha$dropStatus(GuiGraphics graphics, CallbackInfo ci) {
        jojoha$drop(graphics);
    }

    @Inject(method = "renderVehicleHealth", at = @At("HEAD"))
    private void jojoha$liftVehicle(GuiGraphics graphics, CallbackInfo ci) {
        jojoha$lift(graphics);
    }

    @Inject(method = "renderVehicleHealth", at = @At("RETURN"))
    private void jojoha$dropVehicle(GuiGraphics graphics, CallbackInfo ci) {
        jojoha$drop(graphics);
    }

    /**
     * The name of the held item, moved out from behind the bar.
     *
     * <p>Vanilla puts it 59 pixels from the bottom and the bar owns the bottom 61, so it was drawn
     * inside the frame - not near it, behind it. Lifted by the same amount the hearts are, which
     * clears the bar with room to spare and, more to the point, means there is one number deciding
     * where everything at the bottom of the screen goes rather than three that agree until somebody
     * changes the frame height.
     *
     * <p>Targets the private one-argument form on purpose. NeoForge adds a wrapper and a public
     * overload beside it; vanilla has neither, so either of those would have loaded on one loader
     * and crashed the other. This is the method both of them actually have, and the descriptor is
     * spelled out so the selector cannot also catch NeoForge's overload and lift by double.
     *
     * <p>It is still the whole draw rather than an inner part of it, so the fade the name does on
     * its way out is lifted with it instead of sliding back down as it goes.
     */
    @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At("HEAD"))
    private void jojoha$liftItemName(GuiGraphics graphics, CallbackInfo ci) {
        jojoha$itemNameLifted = jojoha$push(graphics);
    }

    @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At("RETURN"))
    private void jojoha$dropItemName(GuiGraphics graphics, CallbackInfo ci) {
        if (jojoha$itemNameLifted) {
            graphics.pose().popPose();
            jojoha$itemNameLifted = false;
        }
    }

    /**
     * And the chat log, for the same reason.
     *
     * <p>Anchored 40 from the bottom, so its lower lines sat well inside the frame. The lift takes
     * the whole component rather than reflowing it, which is what keeps a wrapped message together:
     * moving the anchor moves every line it already laid out, and nothing has to know how tall the
     * log happens to be this frame.
     *
     * <p>Applied while the chat screen is open too. The bar is still drawn then, so the overlap is
     * still there, and a log that jumped down the moment you pressed T would be a worse answer than
     * one that stays where you were reading it.
     */
    @Inject(method = "renderChat(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"))
    private void jojoha$liftChat(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        jojoha$chatLifted = jojoha$push(graphics);
    }

    @Inject(method = "renderChat(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("RETURN"))
    private void jojoha$dropChat(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (jojoha$chatLifted) {
            graphics.pose().popPose();
            jojoha$chatLifted = false;
        }
    }

    /**
     * Pushes a lifted pose if there is anything to lift by, and says whether it did.
     *
     * <p>Each caller keeps its own flag rather than sharing one. These draws do not nest today, but
     * a shared boolean is only correct for as long as that stays true, and the failure mode - a pose
     * popped by somebody who never pushed one - unbalances the stack for the whole frame.
     */
    private boolean jojoha$push(GuiGraphics graphics) {
        int lift = CentralBarOverlay.statusLift();
        if (lift <= 0) {
            return false;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0F, -lift, 0F);
        return true;
    }

    private void jojoha$lift(GuiGraphics graphics) {
        int lift = CentralBarOverlay.statusLift();
        jojoha$lifted = lift > 0;

        if (jojoha$lifted) {
            graphics.pose().pushPose();
            graphics.pose().translate(0F, -lift, 0F);
        }
    }

    private void jojoha$drop(GuiGraphics graphics) {
        if (jojoha$lifted) {
            graphics.pose().popPose();
            jojoha$lifted = false;
        }
    }
}
