package org.gumel.jojoha.mixin.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.ArmorOutfits;
import org.gumel.jojoha.client.UltimateWingsLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class ArmorSkinLayerMixin {

    private static final TagKey<Item> JOJOHA$HIDES_SKIN = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hides_skin_layer"));

    private static boolean hides(ItemStack stack) {
        return stack.is(JOJOHA$HIDES_SKIN) || ArmorOutfits.dressed(stack);
    }

    @Inject(method = "setModelProperties", at = @At("TAIL"))
    private void jojoha$hideCoveredSkin(AbstractClientPlayer player, CallbackInfo ci) {
        PlayerModel<AbstractClientPlayer> model = ((PlayerRenderer) (Object) this).getModel();

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (hides(helmet) && !ArmorOutfits.keepsHat(helmet)) {
            model.hat.visible = false;
        }
        if (hides(player.getItemBySlot(EquipmentSlot.CHEST))) {
            model.jacket.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
        }
        if (hides(player.getItemBySlot(EquipmentSlot.LEGS))) {
            model.leftPants.visible = false;
            model.rightPants.visible = false;
        }
        UltimateWingsLayer.shapeArms(model, player);
    }
}
