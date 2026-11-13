package org.gumel.jojoha.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.item.PathScrollItem;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.List;

public class StampPathFunction extends LootItemConditionalFunction {

    public static final MapCodec<StampPathFunction> CODEC = RecordCodecBuilder.mapCodec(
            instance -> commonFields(instance).apply(instance, StampPathFunction::new));

    public StampPathFunction() {
        this(List.of());
    }

    protected StampPathFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        return PathScrollItem.of(stack, HamonPaths.random(context.getRandom()));
    }

    @Override
    public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
        return ModRegistries.STAMP_PATH.get();
    }
}
