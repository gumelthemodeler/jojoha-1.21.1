package org.gumel.jojoha.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.gumel.jojoha.fortune.Bizarre;
import org.gumel.jojoha.registry.ModRegistries;

public record BizarreFind() implements LootItemCondition {
    public static final MapCodec<BizarreFind> CODEC = MapCodec.unit(new BizarreFind());

    @Override
    public boolean test(LootContext context) {
        Entity looter = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        return looter instanceof LivingEntity living && Bizarre.finds(living);
    }

    @Override
    public LootItemConditionType getType() {
        return ModRegistries.BIZARRE_FIND.get();
    }
}
