package org.gumel.jojoha.item;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.registry.ModComponents;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.Map;

public class OutfitSmithingRecipe implements SmithingRecipe {
    public static final OutfitSmithingRecipe INSTANCE = new OutfitSmithingRecipe();

    private static final Map<ResourceLocation, ResourceLocation> BY_CARD = Map.ofEntries(
            Map.entry(id("soul_card_jotaro"), id("jotaro_p1")),
            Map.entry(id("soul_card_kakyoin"), id("kakyoin")),
            Map.entry(id("soul_card_polnareff"), id("polnareff")),
            Map.entry(id("soul_card_avdol"), id("avdol")),
            Map.entry(id("soul_card_joseph"), id("joseph")),
            Map.entry(id("soul_card_dio"), id("dio")),
            Map.entry(id("soul_card_zeppeli"), id("zeppeli")),
            Map.entry(id("soul_card_jonathan"), id("jonathan")),
            Map.entry(id("soul_card_dio_p1"), id("dio_p1")),
            Map.entry(id("soul_card_joseph_p2"), id("joseph_p2")),
            Map.entry(id("soul_card_caesar"), id("caesar")));

    public static final TagKey<Item> DRESSABLE = TagKey.create(Registries.ITEM, id("dressable_armor"));

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    public static boolean dressable(ItemStack stack) {
        return stack.is(DRESSABLE) || JojohaServerConfig.CHIPS_ANY_ARMOR.on()
                && stack.getItem() instanceof ArmorItem;
    }

    public static ResourceLocation outfitFor(ItemStack stack) {
        return BY_CARD.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return outfitFor(stack) != null;
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return dressable(stack);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return false;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return outfitFor(input.template()) != null
                && dressable(input.base())
                && input.addition().isEmpty();
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        ResourceLocation outfit = outfitFor(input.template());
        if (outfit == null || !dressable(input.base())) return ItemStack.EMPTY;

        ItemStack result = input.base().copyWithCount(1);
        result.set(ModComponents.OUTFIT.get(), outfit);
        return result;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistries.OUTFIT_SMITHING.get();
    }

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
