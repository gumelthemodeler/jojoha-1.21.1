package org.gumel.jojoha.hamon.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonCombat;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.hamon.RippleSurface;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;

public final class TurquoiseBlueOverdriveMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "turquoise_blue_overdrive");
    public static final TurquoiseBlueOverdriveMove INSTANCE = new TurquoiseBlueOverdriveMove();

    private static final float DAMAGE = 9.0F;

    private static final float UNDEAD_DAMAGE = 18.0F;

    private static final double RANGE = 6.0;

    private static final int WET_RADIUS = 2;

    private static final int COOLDOWN_TICKS = 140;
    private static final float ENERGY_COST = 25F;

    private TurquoiseBlueOverdriveMove() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data) {
        ServerLevel level = player.serverLevel();

        if (!conducting(level, player)) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.hamon.no_water"), true);
            return false;
        }

        AABB probe = player.getBoundingBox().inflate(RANGE);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, probe,
                entity -> entity != player && entity.isAlive())) {
            if (!target.isInWaterOrRain()) {
                continue;
            }

            float damage = HamonCombat.isUndead(target) ? UNDEAD_DAMAGE : DAMAGE;
            target.hurt(level.damageSources().playerAttack(player),
                    DamageScale.hamon(player, target, HamonCombat.scaled(player, damage)));
            Impacts.landed(level, target, target.position().subtract(player.position()),
                    ImpactSpec.MEDIUM, Impacts.HAMON);

            HamonFx.ripple(level, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(), 8);
        }

        int spokes = 20;
        for (double radius = 0.8; radius <= RANGE; radius += 0.9) {
            for (int i = 0; i < spokes; i++) {
                double angle = i * (Math.PI * 2) / spokes;
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;

                double surface = RippleSurface.surfaceAt(
                        level, x, player.getY() + 0.6, z);
                if (Double.isNaN(surface)) {
                    continue;
                }

                if ((i + (int) (radius * 2)) % 3 == 0) {
                    HamonFx.ring(level, x, surface, z, 1.0, radius > RANGE * 0.6 ? 2 : 1);
                }
                HamonFx.fineSpark(level, x, surface, z, 1, 0.05, 0.0);
            }
        }

        double under = RippleSurface.surfaceAt(
                level, player.getX(), player.getY() + 0.6, player.getZ());
        HamonFx.spark(level, player.getX(),
                Double.isNaN(under) ? player.getY() + 0.3 : under, player.getZ(), 30, 0.9, 0.05);

        NetworkHandler.sendFirstPersonPose(player, "palm");

        level.playSound(null, player.blockPosition(), ModSounds.HAMON_CRASH.get(),
                SoundSource.PLAYERS, 0.7F, 1.5F);
        HamonFx.sound(level, player, 0.9F, 1.1F);
        return true;
    }

    private static boolean conducting(ServerLevel level, ServerPlayer player) {
        if (player.isInWaterOrRain()) {
            return true;
        }

        BlockPos at = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-WET_RADIUS, -1, -WET_RADIUS),
                at.offset(WET_RADIUS, 1, WET_RADIUS))) {
            if (!level.getFluidState(pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
