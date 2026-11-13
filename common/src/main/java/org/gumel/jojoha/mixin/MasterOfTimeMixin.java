package org.gumel.jojoha.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.combat.PlayerStun;
import org.gumel.jojoha.combat.StatusResistance;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.passive.MasterOfTimePassive;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class MasterOfTimeMixin {
    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance jojoha$masterOfTime(MobEffectInstance incoming) {
        if ((Object) this instanceof ServerPlayer player) {
            MobEffectInstance capped = PlayerStun.cap(incoming);
            return Heaven.steady(player, MasterOfTimePassive.shorten(player, capped));
        }
        return StatusResistance.adjust((LivingEntity) (Object) this, incoming);
    }
}
