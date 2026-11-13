package org.gumel.jojoha.mixin;

import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.PilotSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Mixin(PlayerList.class)
public abstract class PilotEarsMixin {
    @Shadow
    @Final
    private List<ServerPlayer> players;

    @Inject(method = "broadcast(Lnet/minecraft/world/entity/player/Player;DDDDLnet/minecraft/resources/ResourceKey;"
            + "Lnet/minecraft/network/protocol/Packet;)V", at = @At("TAIL"))
    private void jojoha$carryToPilots(@Nullable Player except, double x, double y, double z, double radius,
                                      ResourceKey<Level> dimension, Packet<?> packet, CallbackInfo ci) {
        if (!PilotSystem.anyoneFlying()) {
            return;
        }

        double reach = radius * radius;
        for (int i = 0; i < this.players.size(); i++) {
            ServerPlayer player = this.players.get(i);
            if (player == except || player.level().dimension() != dimension) {
                continue;
            }
            if (player.distanceToSqr(x, y, z) < reach) {
                continue;
            }

            StandEntity stand = PilotSystem.flownBy(player);
            if (stand == null || stand.level().dimension() != dimension
                    || stand.distanceToSqr(x, y, z) >= reach) {
                continue;
            }

            player.connection.send(packet);
        }
    }
}
