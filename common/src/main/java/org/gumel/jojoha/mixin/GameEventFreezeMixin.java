package org.gumel.jojoha.mixin;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.skill.TimeStopSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class GameEventFreezeMixin {
    @Inject(method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
            at = @At("HEAD"), cancellable = true)
    private void jojoha$swallowEventsInStoppedTime(Holder<GameEvent> event, Vec3 position,
                                                   GameEvent.Context context, CallbackInfo ci) {
        if (TimeStopSystem.freezesPosition((ServerLevel) (Object) this, position)) {
            ci.cancel();
        }
    }
}
