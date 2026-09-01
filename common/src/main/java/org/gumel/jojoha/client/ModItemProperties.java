package org.gumel.jojoha.client;

import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.VampireStage;

/**
 * The model predicate that lets an item look different in a slot than it does in the world.
 *
 * <p>Several of the mod's items are authored as real geometry rather than as flat sprites, because
 * that is what they should be when held or thrown. The same geometry in an inventory slot is a
 * small object seen from an odd angle, and reads worse than a drawn icon would - so those items
 * ship an icon as well, and their model carries an override that swaps to it when this reports 1.
 *
 * <p>Registered generically rather than per item. The predicate costs nothing for an item whose
 * model never mentions it, and naming every item here would mean this file had to be edited every
 * time a new one wanted an icon - whereas as it stands, the model asking for it is the only change
 * required.
 */
public final class ModItemProperties {
    /** Named for what it answers rather than where it is used: is this being drawn into a slot. */
    public static final ResourceLocation IN_SLOT =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "in_slot");

    private ModItemProperties() {
    }

    /**
     * Whether the mask in this hand has woken.
     *
     * <p>Read off the holder rather than off the stack. The mask does not carry a switched-on flag -
     * what changes is the person holding it, and a mask in the hands of something the mask has
     * already turned is not the dormant rock it was in the hands of a human.
     */
    public static final ResourceLocation ACTIVATED =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "activated");

    /** Whether this particular mask has been fed - read off the stack, where the mark lives. */
    public static final ResourceLocation BLOODIED =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "bloodied");

    public static void init() {
        ItemPropertiesRegistry.registerGeneric(BLOODIED, (stack, level, entity, seed) ->
                org.gumel.jojoha.item.MaskBlood.isBloodied(stack) ? 1F : 0F);

        ItemPropertiesRegistry.registerGeneric(ACTIVATED, (stack, level, entity, seed) ->
                entity instanceof net.minecraft.world.entity.player.Player
                        && ClientPlayerDataCache.data.vampireStage != VampireStage.NONE ? 1F : 0F);

        ItemPropertiesRegistry.registerGeneric(IN_SLOT,
                (stack, level, entity, seed) -> InventoryIconContext.inSlot() ? 1F : 0F);
    }
}
