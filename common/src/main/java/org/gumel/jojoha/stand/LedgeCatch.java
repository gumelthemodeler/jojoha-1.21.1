package org.gumel.jojoha.stand;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.skill.EnergyWeight;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LedgeCatch {
    private static final float FALL = 4F;
    private static final double SINKING = -0.4;
    private static final double GROUND = 3.0;
    private static final double KEEP_DRIFT = 0.4;
    private static final double EASED_FALL = -0.2;
    private static final int REST_TICKS = 40;

    private static final Map<UUID, Long> RESTED = new HashMap<>();

    private LedgeCatch() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                tick(serverPlayer);
            }
        });
    }

    private static void tick(ServerPlayer player) {
        if (player.fallDistance < FALL || player.getDeltaMovement().y > SINKING || player.isFallFlying()
                || player.getAbilities().flying || player.isSpectator() || player.isInWater()) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent() || !data.inUtilityStance()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Long rest = RESTED.get(player.getUUID());
        if (rest != null && now < rest) {
            return;
        }
        BlockHitResult below = level.clip(new ClipContext(player.position(), player.position().add(0, -GROUND, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        if (below.getType() == HitResult.Type.MISS) {
            return;
        }
        float price = EnergyWeight.LIGHT.cost();
        if (data.standEnergy < price) {
            return;
        }
        data.standEnergy = Math.max(0F, data.standEnergy - price);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        RESTED.put(player.getUUID(), now + REST_TICKS);

        Vec3 drift = player.getDeltaMovement();
        player.setDeltaMovement(drift.x * KEEP_DRIFT, EASED_FALL, drift.z * KEEP_DRIFT);
        player.hurtMarked = true;
        player.fallDistance = 0F;

        StandEntity stand = StandEntity.findFor(player);
        if (stand != null) {
            stand.reachTowards(below.getLocation());
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1F, 0.6F);
    }
}
