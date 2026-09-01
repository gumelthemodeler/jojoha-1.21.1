package org.gumel.jojoha.client;

import net.minecraft.world.entity.EquipmentSlot;

/**
 * An armour model that knows which of its parts a given slot covers.
 *
 * <p>Most outfits answer this the same way and can use {@link ArmorOutfits#showStandardParts}. One
 * that cannot is the reason this is an interface rather than a shared method: Jotaro's has separate
 * boots, so its legs and its feet are different geometry on the same part, and only the model itself
 * knows that.
 */
public interface OutfitModel {
    void showOnly(EquipmentSlot slot);
}
