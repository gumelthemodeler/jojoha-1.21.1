package org.gumel.jojoha.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.gumel.jojoha.client.AnubisReach;
import org.gumel.jojoha.client.BarrageStruggleHud;
import org.gumel.jojoha.client.ClackerClient;
import org.gumel.jojoha.client.PossessionClient;
import org.gumel.jojoha.client.StandCombatInput;
import org.gumel.jojoha.client.StandSkillInput;
import org.gumel.jojoha.client.TimeStopView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {
    @Shadow
    public HitResult hitResult;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void jojoha$noSwingWhileTimeSettles(CallbackInfoReturnable<Boolean> cir) {
        if (TimeStopView.settlingHere()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void jojoha$noMiningWhileTimeSettles(boolean leftClick, CallbackInfo ci) {
        if (TimeStopView.settlingHere()) {
            ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void jojoha$noUseWhileTimeSettles(CallbackInfo ci) {
        if (TimeStopView.settlingHere()) {
            ci.cancel();
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void jojoha$noSwingInStruggle(CallbackInfoReturnable<Boolean> cir) {
        if (BarrageStruggleHud.active()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void jojoha$neverAttackYourself(CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = (Minecraft) (Object) this;
        HitResult aim = this.hitResult;
        if (aim instanceof EntityHitResult hit && hit.getEntity() == minecraft.player) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void jojoha$noMiningInStruggle(boolean leftClick, CallbackInfo ci) {
        if (AnubisReach.behindWall((Minecraft) (Object) this) != null) {
            ci.cancel();
            return;
        }
        if (BarrageStruggleHud.active() || PossessionClient.possessing()
                || PossessionClient.possessed()
                || StandSkillInput.isPiloting()) {
            ci.cancel();
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void jojoha$attackWithTheBody(CallbackInfoReturnable<Boolean> cir) {
        if (PossessionClient.possessing()) {
            PossessionClient.attack();
            cir.setReturnValue(false);
        } else if (PossessionClient.possessed()) {
            cir.setReturnValue(false);
        } else if (StandSkillInput.isPiloting()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void jojoha$cutThroughWalls(CallbackInfoReturnable<Boolean> cir) {
        if (AnubisReach.strike((Minecraft) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void jojoha$throwClackers(CallbackInfoReturnable<Boolean> cir) {
        if (ClackerClient.attack()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void jojoha$standTakesTheSwing(CallbackInfoReturnable<Boolean> cir) {
        if (StandCombatInput.standTakesSwing((Minecraft) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void jojoha$noMiningWithClackers(boolean leftClick, CallbackInfo ci) {
        if (ClackerClient.holdsBack()) {
            ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void jojoha$noUseWhilePossessing(CallbackInfo ci) {
        if (PossessionClient.possessing()
                || PossessionClient.possessed()
                || StandSkillInput.isPiloting()) {
            ci.cancel();
        }
    }
}
