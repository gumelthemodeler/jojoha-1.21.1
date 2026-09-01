package org.gumel.jojoha.mixin.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import org.gumel.jojoha.Jojoha;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the player's outer skin layer wherever armour from {@code #jojoha:hides_skin_layer} covers it.
 *
 * <h2>Why</h2>
 *
 * <p>The second skin layer - the hat, jacket, sleeves and trouser overlays - sits a fraction of a
 * pixel outside the body. Vanilla armour is drawn further out again, so the two never meet. Armour
 * drawn as a shaped model does not have that luxury: wherever the model passes close to the body,
 * the skin layer pokes through it and flickers as the camera moves.
 *
 * <h2>Where</h2>
 *
 * <p>{@code setModelProperties} is where vanilla decides which of those overlays are shown, reading
 * the player's own skin customisation settings. Appending to it means this runs after that decision
 * and can only ever turn things off - so a player who has already hidden their jacket stays hidden,
 * and nothing here has to know about those settings.
 *
 * <p>Boots are deliberately absent. The trouser overlay covers the whole leg, and hiding all of it
 * for a pair of boots would strip the skin from the thigh to hide an ankle.
 */
@Mixin(PlayerRenderer.class)
public abstract class ArmorSkinLayerMixin {

    private static final TagKey<Item> JOJOHA$HIDES_SKIN = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hides_skin_layer"));

    @Inject(method = "setModelProperties", at = @At("TAIL"))
    private void jojoha$hideCoveredSkin(AbstractClientPlayer player, CallbackInfo ci) {
        PlayerModel<AbstractClientPlayer> model = ((PlayerRenderer) (Object) this).getModel();

        if (player.getItemBySlot(EquipmentSlot.HEAD).is(JOJOHA$HIDES_SKIN)) {
            model.hat.visible = false;
        }
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(JOJOHA$HIDES_SKIN)) {
            model.jacket.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
        }
        if (player.getItemBySlot(EquipmentSlot.LEGS).is(JOJOHA$HIDES_SKIN)) {
            model.leftPants.visible = false;
            model.rightPants.visible = false;
        }
    }
}
