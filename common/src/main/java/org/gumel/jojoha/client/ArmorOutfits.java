package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.registry.ModComponents;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * What a dressed piece of armour is drawn as.
 *
 * <h2>Why a lookup rather than a branch</h2>
 *
 * <p>Outfits are a set that will grow - one per character, eventually. The armour layer should not
 * learn a new name each time; it should ask what this stack looks like and draw that. So each outfit
 * is one entry here, and adding the next is a line rather than an edit to the renderer.
 *
 * <p>The armour's own appearance is the entry under a null key, which keeps "undressed" from being a
 * special case anywhere else.
 *
 * <h2>Baking</h2>
 *
 * <p>Models are baked on first use and kept. They cannot be built at class-init because the model set
 * does not exist yet, and rebaking one per frame would be wasteful for something that never changes.
 */
public final class ArmorOutfits {

    /** One outfit: the layer its geometry comes from, and the sheet painted on it. */
    public record Outfit(ModelLayerLocation layer, ResourceLocation texture,
                         Function<net.minecraft.client.model.geom.ModelPart,
                                 HumanoidModel<LivingEntity>> factory) {
    }

    private static final Map<ResourceLocation, Outfit> OUTFITS = new HashMap<>();
    private static final Map<ResourceLocation, HumanoidModel<LivingEntity>> BAKED = new HashMap<>();

    /** The armour as it comes: meteorite plate. */
    private static final Outfit PLAIN = new Outfit(
            MeteoriteArmorModel.LAYER, MeteoriteArmorModel.TEXTURE, MeteoriteArmorModel::new);

    static {
        OUTFITS.put(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "jotaro_p1"),
                new Outfit(JotaroOutfitModel.LAYER, JotaroOutfitModel.TEXTURE,
                        JotaroOutfitModel::new));
    }

    private ArmorOutfits() {
    }

    /**
     * The outfit this stack should be drawn in.
     *
     * <p>An outfit named on a stack but not known here - an older world, a datapack ahead of the
     * code - falls back to the plain armour rather than failing to draw. A piece you cannot see is
     * a worse outcome than a piece in the wrong coat.
     */
    public static Outfit forStack(ItemStack stack) {
        ResourceLocation named = stack.get(ModComponents.OUTFIT.get());
        if (named == null) return PLAIN;
        return OUTFITS.getOrDefault(named, PLAIN);
    }

    /** Every layer that needs baking, so the client can register them all in one place. */
    public static Iterable<Outfit> all() {
        java.util.List<Outfit> list = new java.util.ArrayList<>(OUTFITS.values());
        list.add(PLAIN);
        return list;
    }

    public static HumanoidModel<LivingEntity> model(Outfit outfit) {
        return BAKED.computeIfAbsent(outfit.texture(), key ->
                outfit.factory().apply(Minecraft.getInstance().getEntityModels()
                        .bakeLayer(outfit.layer())));
    }

    /**
     * Ask the model which parts this slot covers.
     *
     * <p>Every outfit that has nothing unusual about it answers with the standard rule below. Jotaro's
     * does not, because its boots and its trousers are different geometry on the same leg part, so
     * the model gets to decide rather than this.
     */
    public static void showOnly(HumanoidModel<LivingEntity> model, EquipmentSlot slot) {
        if (model instanceof OutfitModel outfit) {
            outfit.showOnly(slot);
        } else {
            showStandardParts(model, slot);
        }
    }

    /**
     * The ordinary answer: a helmet is a head, a chestplate is a body and two arms, and so on.
     *
     * <p>Legs bring the body along, which looks odd written down and is what vanilla does - a pair of
     * leggings covers the waist, and the waist is the bottom of the body part.
     */
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
