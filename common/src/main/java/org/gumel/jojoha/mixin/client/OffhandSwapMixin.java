package org.gumel.jojoha.mixin.client;

import net.minecraft.client.Minecraft;
import org.gumel.jojoha.client.StandCombatInput;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops the guard key from also emptying the player's off hand.
 *
 * <p>Guard sits on F, which is vanilla's Swap Item to Off Hand. Two bindings on one physical key
 * both fire - the controls screen flags the clash but nothing prevents it - and the guard is read
 * with {@code isDown()} rather than {@code consumeClick()}, deliberately, because it is a hold
 * rather than a press. So without this, every attempt to block would swap whatever was in the off
 * hand, which is not a subtle bug: it is the player's shield or totem going away at the exact
 * moment they reached for it.
 *
 * <p>Drained at the head of {@code handleKeybinds}, which is where vanilla's own
 * {@code keySwapOffhand.consumeClick()} loop lives. Emptying the queue before that loop reaches it
 * is what makes the press silent; doing it from the mod's own input tick would be too late, since
 * that runs at the end of the client tick and vanilla has already acted by then.
 *
 * <p>Only while a Stand is actually out. With nothing summoned there is no guard to hold and F is
 * simply the vanilla key doing its vanilla job - which is most of the time, and is why this is
 * narrower than "take F away".
 *
 * <p>The cost, stated plainly: while your Stand is summoned you cannot swap your off hand. That is
 * not a bug to be fixed later, it is what putting two functions on one key buys.
 */
@Mixin(Minecraft.class)
public abstract class OffhandSwapMixin {
    /** Far more presses than a frame can produce; see the loop for why there is a ceiling at all. */
    private static final int MAX_DRAIN = 16;


    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void jojoha$holdOffhandWhileGuarding(CallbackInfo ci) {
        if (!ClientPlayerDataCache.data.standSummoned || !StandCombatInput.guardKeyDown()) {
            return;
        }

        Minecraft minecraft = (Minecraft) (Object) this;

        // Drained rather than flagged: the queue can hold more than one click from a single frame,
        // and leaving any of them would let the swap through a tick later.
        //
        // Bounded, and not out of caution about the number. consumeClick stops at clickCount == 0
        // rather than <= 0 - checked in the bytecode - so an unbounded drain is an infinite loop
        // the moment anything drives that counter negative. Nothing in vanilla does, but a frozen
        // client is a bad way to find out that something else did.
        for (int i = 0; i < MAX_DRAIN && minecraft.options.keySwapOffhand.consumeClick(); i++) {
            // Discarded on purpose - see the class note.
        }
    }
}
