package org.gumel.jojoha.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import org.gumel.jojoha.client.BarrageStruggleHud;
import org.gumel.jojoha.client.PossessionClient;
import org.gumel.jojoha.client.StandSkillInput;
import org.gumel.jojoha.client.TimeStopView;
import org.gumel.jojoha.client.pillar.PillarClient;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    private static boolean frozenByStun() {
        LocalPlayer player =
                Minecraft.getInstance().player;
        return player != null && (Stun.freezesMovement(player) || PillarClient.petrified(player));
    }

    @Inject(method = "tick(ZF)V", at = @At("TAIL"))
    private void jojoha$holdStillWhilePiloting(boolean isSneaking, float sneakSpeedMultiplier, CallbackInfo ci) {
        if (!PossessionClient.possessed()
                && !StandSkillInput.isPiloting()
                && !PossessionClient.possessing()
                && !ClientPlayerDataCache.data.isTimeStopFrozen()
                && !frozenByStun()
                && !BarrageStruggleHud.active()
                && !TimeStopView.settlingHere()) {
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
