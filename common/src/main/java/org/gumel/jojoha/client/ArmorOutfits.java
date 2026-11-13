package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.registry.ModComponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ArmorOutfits {
    public record Outfit(ModelLayerLocation layer, Supplier<LayerDefinition> mesh,
                         ResourceLocation texture,
                         Function<ModelPart,
                                 HumanoidModel<LivingEntity>> factory) {
    }

    private static final Map<ResourceLocation, Outfit> OUTFITS = new HashMap<>();
    private static final Map<ResourceLocation, HumanoidModel<LivingEntity>> BAKED = new HashMap<>();

    private static final Outfit PLAIN = new Outfit(
            MeteoriteArmorModel.LAYER, MeteoriteArmorModel::createBodyLayer,
            MeteoriteArmorModel.TEXTURE, MeteoriteArmorModel::new);

    static {
        put("jotaro_p1", JotaroOutfitModel.LAYER, JotaroOutfitModel::createBodyLayer,
                JotaroOutfitModel.TEXTURE, JotaroOutfitModel::new);
        put("kakyoin", KakyoinOutfitModel.LAYER, KakyoinOutfitModel::createBodyLayer,
                KakyoinOutfitModel.TEXTURE, KakyoinOutfitModel::new);
        put("polnareff", PolnareffOutfitModel.LAYER, PolnareffOutfitModel::createBodyLayer,
                PolnareffOutfitModel.TEXTURE, PolnareffOutfitModel::new);
        put("avdol", AvdolOutfitModel.LAYER, AvdolOutfitModel::createBodyLayer,
                AvdolOutfitModel.TEXTURE, AvdolOutfitModel::new);
        put("joseph", JosephOutfitModel.LAYER, JosephOutfitModel::createBodyLayer,
                JosephOutfitModel.TEXTURE, JosephOutfitModel::new);
        put("dio", DioOutfitModel.LAYER, DioOutfitModel::createBodyLayer,
                DioOutfitModel.TEXTURE, DioOutfitModel::new);
        put("zeppeli", ZeppeliOutfitModel.LAYER, ZeppeliOutfitModel::createBodyLayer,
                ZeppeliOutfitModel.TEXTURE, ZeppeliOutfitModel::new);
        put("jonathan", JonathanOutfitModel.LAYER, JonathanOutfitModel::createBodyLayer,
                JonathanOutfitModel.TEXTURE, JonathanOutfitModel::new);
        put("dio_p1", DioPart1OutfitModel.LAYER, DioPart1OutfitModel::createBodyLayer,
                DioPart1OutfitModel.TEXTURE, DioPart1OutfitModel::new);
        put("joseph_p2", JosephPart2OutfitModel.LAYER, JosephPart2OutfitModel::createBodyLayer,
                JosephPart2OutfitModel.TEXTURE, JosephPart2OutfitModel::new);
        put("caesar", CaesarOutfitModel.LAYER, CaesarOutfitModel::createBodyLayer,
                CaesarOutfitModel.TEXTURE, CaesarOutfitModel::new);
    }

    private static void put(String name, ModelLayerLocation layer, Supplier<LayerDefinition> mesh,
                            ResourceLocation texture,
                            Function<ModelPart,
                                    HumanoidModel<LivingEntity>> factory) {
        OUTFITS.put(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, name),
                new Outfit(layer, mesh, texture, factory));
    }

    private static final Set<ResourceLocation> KEEPS_HAT = Set.of(
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "polnareff"),
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "dio"),
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "avdol"),
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "kakyoin"),
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "caesar"),
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "jonathan"),
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "dio_p1"),
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "joseph_p2"));

    private ArmorOutfits() {
    }

    public static boolean dressed(ItemStack stack) {
        return stack.get(ModComponents.OUTFIT.get()) != null;
    }

    public static boolean keepsHat(ItemStack stack) {
        ResourceLocation named = stack.get(ModComponents.OUTFIT.get());
        return named != null && KEEPS_HAT.contains(named);
    }

    public static Outfit forStack(ItemStack stack) {
        ResourceLocation named = stack.get(ModComponents.OUTFIT.get());
        if (named == null) return PLAIN;
        return OUTFITS.getOrDefault(named, PLAIN);
    }

    public static Iterable<Outfit> all() {
        List<Outfit> list = new ArrayList<>(OUTFITS.values());
        list.add(PLAIN);
        return list;
    }

    public static HumanoidModel<LivingEntity> model(Outfit outfit) {
        return BAKED.computeIfAbsent(outfit.texture(), key ->
                outfit.factory().apply(Minecraft.getInstance().getEntityModels()
                        .bakeLayer(outfit.layer())));
    }

    public static void showOnly(HumanoidModel<LivingEntity> model, EquipmentSlot slot) {
        if (model instanceof OutfitModel outfit) {
            outfit.showOnly(slot);
        } else {
            showStandardParts(model, slot);
        }
    }

    public static void showStandardParts(HumanoidModel<LivingEntity> model, EquipmentSlot slot) {
        model.setAllVisible(false);
        switch (slot) {
            case HEAD -> model.head.visible = true;
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS -> {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default -> {
            }
        }
    }
}
