package org.gumel.jojoha.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import org.gumel.jojoha.registry.ModItems;

public final class MeteoriteTier implements Tier {

    public static final MeteoriteTier INSTANCE = new MeteoriteTier();

    private MeteoriteTier() {
    }

    @Override
    public int getUses() {
        return 3200;
    }

    @Override
    public float getSpeed() {
        return 11.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 5.0F;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return 18;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.METEORITE_INGOT.get());
    }
}
