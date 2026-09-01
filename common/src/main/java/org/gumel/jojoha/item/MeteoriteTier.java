package org.gumel.jojoha.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import org.gumel.jojoha.registry.ModItems;

/**
 * One step above netherite.
 *
 * <h2>The numbers, and what they are measured against</h2>
 *
 * <p>Netherite is 2031 uses, speed 9.0, +4.0 damage, enchantability 15. Each of these is a clear
 * step past that without being a different order of magnitude - a tier that trivialises the game is
 * not a reward, it is the end of one. Durability is the largest jump because durability is the least
 * disruptive thing to give: it changes how often you visit an anvil, not how a fight goes.
 *
 * <h2>Why the incorrect-blocks tag is netherite's own</h2>
 *
 * <p>Since 1.20.5 a tier does not carry a mining level; it carries the set of blocks it is <em>not</em>
 * good enough to drop. Netherite's set is already empty of anything meaningful - there is nothing in
 * the game a netherite pickaxe cannot harvest - so a stricter tag above it would have nothing to say.
 * Reusing netherite's is the accurate statement: this mines everything netherite mines, faster.
 */
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
