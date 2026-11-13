package org.gumel.jojoha.hamon;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.gumel.jojoha.Jojoha;

public final class HamonBacklash {
    public static final ResourceKey<DamageType> HAMON_BACKLASH = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hamon_backlash"));

    private static final int SPRAY = 40;

    private HamonBacklash() {
    }

    public static void burst(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double x = player.getX();
        double y = player.getY(0.55);
        double z = player.getZ();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        HamonFx.spark(level, x, y, z, SPRAY, 0.5, 0.4);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, SPRAY / 2, 0.4, 0.6, 0.4, 0.3);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(HAMON_BACKLASH));
        player.hurt(source, Float.MAX_VALUE);
        if (player.isAlive()) {
            player.setHealth(0F);
            player.die(source);
        }
    }
}
