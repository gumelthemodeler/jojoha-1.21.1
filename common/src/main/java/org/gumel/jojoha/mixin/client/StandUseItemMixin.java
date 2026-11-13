package org.gumel.jojoha.mixin.client;

import net.minecraft.client.Minecraft;
import org.gumel.jojoha.client.StandCombatInput;
import org.gumel.jojoha.client.StandHandsInput;
import org.gumel.jojoha.client.StandStretch;
import org.gumel.jojoha.client.StandTurn;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.skill.moves.TimeStopThrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Minecraft.class)
public abstract class StandUseItemMixin {
    @Shadow
    private int rightClickDelay;

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void jojoha$throwWeaponInTimeStop(CallbackInfo ci) {
        if (StandCombatInput.throwsWeapon((Minecraft) (Object) this)) {
            this.rightClickDelay = TimeStopThrow.COOLDOWN_TICKS;
            NetworkHandler.sendTimeStopThrow();
            ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void jojoha$delegateUseToStand(CallbackInfo ci) {
        if (!StandHandsInput.shouldDelegate()) {
            return;
        }

        this.rightClickDelay = StandHandsInput.REPEAT_DELAY_TICKS;

        if (!StandStretch.gestureOwnsClick()) {
            NetworkHandler.sendStandUseItem(Optional.empty(),
                    StandTurn.turns());
        }
        ci.cancel();
    }
}
