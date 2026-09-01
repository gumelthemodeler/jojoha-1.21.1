package org.gumel.jojoha.item;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.registry.ModComponents;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.Map;

/**
 * Dressing armour at the smithing table, the way a trim is applied.
 *
 * <h2>The three slots</h2>
 *
 * <p>The card goes in the template slot, because that is what it is: the thing that decides how the
 * result looks. The armour goes in the middle, where the item being upgraded always goes. The right
 * slot stays empty - a trim wants a material there, this does not, and demanding one would invent a
 * cost nobody asked for. Nothing can be put in it, because {@link #isAdditionIngredient} refuses
 * everything, so the slot is visibly not part of this rather than mysteriously ignored.
 *
 * <h2>Why the result is the input</h2>
 *
 * <p>A recipe that produced a fresh piece of armour would quietly destroy the durability and every
 * enchantment on the one handed in. Dressing a piece must not cost the player the piece, so the
 * result is that same stack copied, with one component set.
 */
public class OutfitSmithingRecipe implements SmithingRecipe {

    /**
     * The one instance there is.
     *
     * <p>The recipe holds no state, so a second instance would carry no more information than this
     * one - and having only one is what makes the serializer below work. StreamCodec.unit compares
     * the value it is asked to encode against the value it was built with, using equals; a class
     * without equals compares by identity, so a recipe decoded from JSON into a fresh object failed
     * to match the one the stream codec was holding and refused to encode. Handing both codecs the
     * same object removes the question rather than answering it with an equals method that would
     * exist only to satisfy this.
     */
    public static final OutfitSmithingRecipe INSTANCE = new OutfitSmithingRecipe();

    /** Which card dresses a piece in which outfit. */
    private static final Map<ResourceLocation, ResourceLocation> BY_CARD = Map.of(
            id("soul_card_jotaro"), id("jotaro_p1"));

    /** The armour this can be applied to. A tag, so a later set joins without touching this. */
    public static final TagKey<Item> DRESSABLE = TagKey.create(Registries.ITEM, id("dressable_armor"));

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    /** The outfit a card grants, or null if this stack is not a card we know an outfit for. */
    public static ResourceLocation outfitFor(ItemStack stack) {
        return BY_CARD.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return outfitFor(stack) != null;
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.is(DRESSABLE);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return false;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return outfitFor(input.template()) != null
                && input.base().is(DRESSABLE)
                && input.addition().isEmpty();
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        ResourceLocation outfit = outfitFor(input.template());
        if (outfit == null || !input.base().is(DRESSABLE)) return ItemStack.EMPTY;

        // copyWithCount rather than a fresh stack: this is the same piece of armour, dressed. Its
        // damage, its enchantments and anything else riding on it come through untouched.
        ItemStack result = input.base().copyWithCount(1);
        result.set(ModComponents.OUTFIT.get(), outfit);
        return result;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        // There is no one result - it depends entirely on what was put in. This is only used to show
        // a recipe in a book, and a special recipe does not appear in one.
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistries.OUTFIT_SMITHING.get();
    }

    /**
     * The recipe carries no data of its own, so both codecs are constants.
     *
     * <p>Everything about what it matches is in the code above, which is the point of a special
     * recipe: the JSON exists to say "this recipe is on", nothing more.
     */
    public static class Serializer implements RecipeSerializer<OutfitSmithingRecipe> {
        private static final MapCodec<OutfitSmithingRecipe> CODEC = MapCodec.unit(INSTANCE);
        private static final StreamCodec<RegistryFriendlyByteBuf, OutfitSmithingRecipe> STREAM_CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public MapCodec<OutfitSmithingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, OutfitSmithingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
