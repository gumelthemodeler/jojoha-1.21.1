package org.gumel.jojoha.combat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.vampire.VampireState;

public final class JojohaUndead {
    public static final TagKey<EntityType<?>> UNDEAD = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "undead"));

    private JojohaUndead() {
    }

    public static boolean is(LivingEntity entity) {
        if (entity.getType().is(UNDEAD)) {
            return true;
        }
        if (!(entity instanceof Player player) || !isVampiric(player)) {
            return false;
        }
        return !VampireState.isFrozen(player);
    }

    public static boolean isVampiric(Player player) {
        return PlayerDataAccess.get(player).vampireStage != VampireStage.NONE;
    }
}
