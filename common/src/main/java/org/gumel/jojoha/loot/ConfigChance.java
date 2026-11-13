package org.gumel.jojoha.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.config.NumberSetting;
import org.gumel.jojoha.registry.ModRegistries;

public record ConfigChance(String loot, float chance) implements LootItemCondition {
    public static final MapCodec<ConfigChance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("loot").forGetter(ConfigChance::loot),
            Codec.FLOAT.fieldOf("chance").forGetter(ConfigChance::chance)
    ).apply(instance, ConfigChance::new));

    public static LootItemCondition.Builder of(String loot, float chance) {
        return () -> new ConfigChance(loot, chance);
    }

    public float scaled() {
        NumberSetting setting = JojohaServerConfig.loot(loot);
        return chance * (setting == null ? 1F : setting.value());
    }

    @Override
    public boolean test(LootContext context) {
        return context.getRandom().nextFloat() < scaled();
    }

    @Override
    public LootItemConditionType getType() {
        return ModRegistries.CONFIG_CHANCE.get();
    }
}
