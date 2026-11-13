package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.gumel.jojoha.accessory.Accessories;
import org.gumel.jojoha.accessory.AccessoryItem;
import org.gumel.jojoha.accessory.AccessoryMenuSlot;
import org.gumel.jojoha.accessory.AccessorySlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuAccessoriesMixin extends RecipeBookMenu<CraftingInput, CraftingRecipe> {
    @Unique
    private static final int jojoha$LAST_INVENTORY_SLOT = 45;

    @Unique
    private int jojoha$firstAccessory = -1;

    private InventoryMenuAccessoriesMixin(MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void jojoha$addAccessorySlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        jojoha$firstAccessory = this.slots.size();
        for (AccessorySlot kind : AccessorySlot.values()) {
            this.addSlot(new AccessoryMenuSlot(Accessories.of(owner), kind, owner));
        }
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void jojoha$wearAccessory(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (jojoha$firstAccessory < 0 || index < 9 || index > jojoha$LAST_INVENTORY_SLOT) {
            return;
        }
        Slot from = this.slots.get(index);
        ItemStack stack = from.getItem();
        if (!(stack.getItem() instanceof AccessoryItem accessory)) {
            return;
        }
        int target = jojoha$firstAccessory + accessory.slot().ordinal();
        if (this.slots.get(target).hasItem()) {
            return;
        }
        ItemStack before = stack.copy();
        if (!this.moveItemStackTo(stack, target, target + 1, false)) {
            return;
        }
        if (stack.isEmpty()) {
            from.setByPlayer(ItemStack.EMPTY, before);
        } else {
            from.setChanged();
        }
        cir.setReturnValue(ItemStack.EMPTY);
    }
}
