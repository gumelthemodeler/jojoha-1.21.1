package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.ArmorOutfits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the meteorite armour as a model instead of as a painted-on skin.
 *
 * <h2>Why a mixin</h2>
 *
 * <p>Vanilla armour is two flat sheets wrapped round the player's own box model, and that is all the
 * armour layer knows how to do. Fabric and NeoForge each offer a hook for custom armour models and
 * they are different hooks; Architectury 9.2.14 wraps neither. Since this project keeps common code
 * free of loader-specific calls, the layer itself is intercepted instead - one method, the same on
 * both loaders.
 *
 * <h2>Why this injects at RETURN rather than HEAD</h2>
 *
 * <p>The model parameter is not posed when the method starts; vanilla copies the wearer's pose onto
 * it partway through. Reading it at HEAD gets whatever the last thing rendered left behind, and that
 * is worse than it sounds: the copy carries the {@code young} flag, and a humanoid model built with
 * {@code young} true renders at half scale translated a block and a half downwards. Inheriting a
 * stale one from any baby mob drawn earlier in the frame put the armour tiny and buried inside the
 * player.
 *
 * <p>By RETURN the pose is done and correct, so the copy is simply right. Two things make injecting
 * there safe rather than merely convenient. Vanilla has already finished drawing this piece, and it
 * drew nothing, because the material's layer list is deliberately empty - so there is no doubling
 * and nothing to cancel. And RETURN needs no mid-method target to match, which is what the earlier
 * attempt at this got wrong: it tried to shadow {@code getParentModel}, which is declared on
 * {@code RenderLayer} rather than on the target, and a shadow only looks at the target class itself.
 *
 * <p>Which items are drawn this way is a tag, so the next armour set is a data change.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class MeteoriteArmorLayerMixin {

    private static final TagKey<Item> JOJOHA$METEORITE_ARMOR = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "meteorite_armor"));

    @Inject(method = "renderArmorPiece", at = @At("RETURN"))
    private void jojoha$renderMeteorite(PoseStack poseStack, MultiBufferSource buffers,
                                        LivingEntity entity, EquipmentSlot slot, int light,
                                        HumanoidModel<LivingEntity> posed, CallbackInfo ci) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (!stack.is(JOJOHA$METEORITE_ARMOR)) return;

        // RETURN catches every exit, including the early one vanilla takes when the piece does not
        // belong in this slot - and on that path the model was never posed. Repeating vanilla's own
        // condition means we only draw where it would have.
        if (!(stack.getItem() instanceof net.minecraft.world.item.ArmorItem piece)
                || piece.getEquipmentSlot() != slot) {
            return;
        }

        // Which coat this piece wears. Undressed armour answers with its own look, so there is no
        // branch here between "plain" and "dressed" - both are just an outfit.
        ArmorOutfits.Outfit outfit = ArmorOutfits.forStack(stack);
        HumanoidModel<LivingEntity> model = ArmorOutfits.model(outfit);

        // Posed by now, so this carries the wearer's limbs, their crouch, and the correct young flag.
        posed.copyPropertiesTo(model);
        ArmorOutfits.showOnly(model, slot);

        // armorCutoutNoCull is what vanilla armour uses: an alpha test, and both faces drawn, which
        // a shaped piece of plate needs for the same reason the Stone Mask did - you can see into it.
        VertexConsumer buffer = ItemRenderer.getArmorFoilBuffer(buffers,
                RenderType.armorCutoutNoCull(outfit.texture()), stack.hasFoil());
        model.renderToBuffer(poseStack, buffer, light, OverlayTexture.NO_OVERLAY, -1);
    }
}
