package org.gumel.jojoha.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.gumel.jojoha.item.Provisions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LootTable.class)
public abstract class NapkinLootMixin {
    @Unique
    private static boolean jojoha$again;

    @Inject(method = "fill", at = @At("TAIL"))
    private void jojoha$blessedFind(Container container, LootParams params, long seed, CallbackInfo ci) {
        if (jojoha$again) {
            return;
        }
        Entity looter = params.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(looter instanceof Player player) || !Provisions.blessed(player)) {
            return;
        }
        jojoha$again = true;
        try {
            ((LootTable) (Object) this).fill(container, params, seed + 1L);
        } finally {
            jojoha$again = false;
        }
    }
}
