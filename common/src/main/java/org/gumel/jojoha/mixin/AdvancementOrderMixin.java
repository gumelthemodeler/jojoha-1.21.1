package org.gumel.jojoha.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Mixin(PlayerAdvancements.class)
public abstract class AdvancementOrderMixin {

    @ModifyArg(
            method = "flushDirty",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ClientboundUpdateAdvancementsPacket;"
                            + "<init>(ZLjava/util/Collection;Ljava/util/Set;Ljava/util/Map;)V"),
            index = 1)
    private Collection<AdvancementHolder> jojoha$orderTabs(Collection<AdvancementHolder> added) {
        List<AdvancementHolder> ordered = new ArrayList<>(added);
        ordered.sort(Comparator.comparing(holder -> holder.id().toString()));
        return ordered;
    }
}
