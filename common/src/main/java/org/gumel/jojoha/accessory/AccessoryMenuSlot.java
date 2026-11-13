package org.gumel.jojoha.accessory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class AccessoryMenuSlot extends Slot {
    private final AccessorySlot kind;
    private final Player owner;

    public AccessoryMenuSlot(Container worn, AccessorySlot kind, Player owner) {
        super(worn, kind.ordinal(), Accessories.slotX(kind), Accessories.slotY(kind));
        this.kind = kind;
        this.owner = owner;
    }

    public AccessorySlot kind() {
        return kind;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof AccessoryItem accessory && accessory.slot() == kind;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isActive() {
        return !owner.level().isClientSide || Accessories.shown.getAsBoolean();
    }
}
