package org.gumel.jojoha.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public interface FloatingKickAccessor {

    @Accessor("aboveGroundTickCount")
    void jojoha$setAboveGroundTickCount(int ticks);
}
