package org.gumel.jojoha.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandAuraEffect;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.StandTypes;

public final class LocalStandAuraEffect {
    private LocalStandAuraEffect() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || !ClientPlayerDataCache.data.standSummoned) {
            return;
        }

        if (ClientPlayerDataCache.data.stand.trust().manifestsEntity()
                && StandTypes.hasBody(ClientPlayerDataCache.data)
                && !StandGlowTracker.ownStandOut()) {
            return;
        }

        if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            return;
        }

        if (StandAuraEffect.asleep(player)) {
            return;
        }

        if (player.tickCount % StandAuraEffect.spawnIntervalTicks() != 0) {
            return;
        }

        boolean bound = StandTypes
                .byIdOrDefault(ClientPlayerDataCache.data.stand.standId()).form().isBound();

        int colour = StandShadowForm.auraColourFor(ClientPlayerDataCache.data.stand);
        double red = ((colour >> 16) & 0xFF) / 255.0;
        double green = ((colour >> 8) & 0xFF) / 255.0;
        double blue = (colour & 0xFF) / 255.0;

        for (int i = 0; i < StandAuraEffect.motesPerSpawn(); i++) {
            int index = player.tickCount * StandAuraEffect.motesPerSpawn() + i;
            Vec3 offset = bound
                    ? StandAuraEffect.handOffset(index, player.level().getRandom(), player.yBodyRot)
                    : StandAuraEffect.auraOffset(index, player.level().getRandom());

            player.level().addParticle(ModRegistries.STAND_AURA.get(),
                    player.getX() + offset.x, player.getY() + offset.y, player.getZ() + offset.z,
                    red, green, blue);
        }
    }
}
