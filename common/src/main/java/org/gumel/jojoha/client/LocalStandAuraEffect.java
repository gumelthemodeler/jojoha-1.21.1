package org.gumel.jojoha.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandAuraEffect;

/**
 * The owning player's own copy of the Stand aura, spawned purely client-side (never networked -
 * the server deliberately excludes the owner from its broadcast, see {@link StandAuraEffect}).
 * Skipped entirely in first person, since a player doesn't want their own aura cluttering their
 * own view - third person and every other player watching them still see it normally.
 *
 * <p>Runs the same emission the server does, from the shared helpers, so the owner sees the
 * identical aura everyone else does rather than a differently-shaped local approximation.
 */
public final class LocalStandAuraEffect {
    private LocalStandAuraEffect() {
    }

    /** Call once per client tick. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        // Mirrors the server's gate in EnergySystem: the aura answers the cast at every Trust
        // Tier, including a DORMANT Stand that never actually takes form.
        if (player == null || !ClientPlayerDataCache.data.standSummoned) {
            return;
        }

        if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            return;
        }

        if (player.tickCount % StandAuraEffect.spawnIntervalTicks() != 0) {
            return;
        }

        // The same question the server asks, asked here too - and this is why the change to the
        // hands appeared to do nothing. There are two aura paths: the server broadcasts motes to
        // everybody except the owner, and this draws the owner's own. Fixing one leaves the person
        // actually using the Stand looking at the old behaviour, which is precisely who was looking.
        boolean bound = org.gumel.jojoha.stand.StandTypes
                .byIdOrDefault(ClientPlayerDataCache.data.stand.standId()).form().isBound();

        for (int i = 0; i < StandAuraEffect.motesPerSpawn(); i++) {
            int index = player.tickCount * StandAuraEffect.motesPerSpawn() + i;
            Vec3 offset = bound
                    ? StandAuraEffect.handOffset(index, player.level().getRandom(), player.yBodyRot)
                    : StandAuraEffect.auraOffset(index, player.level().getRandom());

            // Zero velocity: the particle tracks its owner rather than coasting - see
            // StandAuraParticle for why that's what keeps the aura stuck to a moving player.
            player.level().addParticle(ModRegistries.STAND_AURA.get(),
                    player.getX() + offset.x, player.getY() + offset.y, player.getZ() + offset.z,
                    0.0, 0.0, 0.0);
        }
    }
}
