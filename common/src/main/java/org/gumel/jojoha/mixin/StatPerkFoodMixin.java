package org.gumel.jojoha.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.combat.StatPerkEffects;
import org.gumel.jojoha.combat.VampireTraits;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StatPerk;
import org.gumel.jojoha.data.VampireStage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class StatPerkFoodMixin {

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float jojoha$exhaustionScale(float exhaustion) {
        if (!((Object) this instanceof ServerPlayer player)) {
            return exhaustion;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        float scaled = exhaustion;
        if (StatPerk.IRON_STOMACH.has(data)) {
            scaled *= StatPerkEffects.IRON_STOMACH_SCALE;
        }
        if (data.vampireStage == VampireStage.ULTIMATE_LIFEFORM) {
            scaled *= VampireTraits.ULTIMATE_THIRST_SCALE;
        }
        return scaled;
    }
}
