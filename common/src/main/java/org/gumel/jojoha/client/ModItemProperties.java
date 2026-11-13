package org.gumel.jojoha.client;

import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.item.ArrowTooltips;
import org.gumel.jojoha.item.HeadJarItem;
import org.gumel.jojoha.item.MaskBlood;
import org.gumel.jojoha.item.PathScrollItem;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.stand.anubis.Anubis;
import org.gumel.jojoha.ultimate.WingFlight;

public final class ModItemProperties {
    public static final ResourceLocation IN_SLOT =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "in_slot");

    private ModItemProperties() {
    }

    public static final ResourceLocation ACTIVATED =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "activated");

    public static final ResourceLocation FULL =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "full");

    public static final ResourceLocation BLOODIED =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "bloodied");

    public static final ResourceLocation PULL =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "pull");

    public static final ResourceLocation PULLING =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "pulling");

    public static final ResourceLocation HAMON_PATH =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hamon_path");

    public static void init() {
        ArrowTooltips.setViewer(() -> Minecraft.getInstance().player);
        Anubis.clientNamed = AnubisLook::named;
        Anubis.clientWields = AnubisLook::wields;
        WingFlight.clientWings = UltimateWings::wingsOut;
        JojohaConfigScreen.registerCommand();

        ItemPropertiesRegistry.register(ModItems.HEAD_JAR.get(), FULL, (stack, level, entity, seed) ->
                HeadJarItem.occupant(stack) != null ? 1F : 0F);

        ItemPropertiesRegistry.registerGeneric(BLOODIED, (stack, level, entity, seed) ->
                MaskBlood.isBloodied(stack) ? 1F : 0F);

        ItemPropertiesRegistry.registerGeneric(ACTIVATED, (stack, level, entity, seed) ->
                entity instanceof Player
                        && ClientPlayerDataCache.data.vampireStage != VampireStage.NONE ? 1F : 0F);

        ItemPropertiesRegistry.registerGeneric(PULL, (stack, level, entity, seed) ->
                entity == null || entity.getUseItem() != stack ? 0F
                        : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20F);

        ItemPropertiesRegistry.registerGeneric(PULLING, (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1F : 0F);

        ItemPropertiesRegistry.registerGeneric(HAMON_PATH, (stack, level, entity, seed) ->
                PathScrollItem.bookNumber(stack));

        ItemPropertiesRegistry.registerGeneric(IN_SLOT,
                (stack, level, entity, seed) -> InventoryIconContext.inSlot() ? 1F : 0F);
    }
}
