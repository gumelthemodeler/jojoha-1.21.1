package org.gumel.jojoha.hamon.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonCombat;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.skill.StandBeat;

import java.util.List;

public final class RipplePulseMove implements HamonMove {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ripple_pulse");
    public static final RipplePulseMove INSTANCE = new RipplePulseMove();

    private static final double RANGE = 3.5;
    private static final float DAMAGE = 4.5F;
    private static final double KNOCKBACK = 1.2;
    private static final int COOLDOWN_TICKS = 40;
    private static final float ENERGY_COST = 20F;

    private RipplePulseMove() {
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
        MovementFx.pose(player, MovementFx.RIPPLE_PULSE);
        StandBeat.at(MovementFx.RIPPLE_PULSE_CONTACT_TICKS - MovementFx.PALM_CONTACT_TICKS, player,
                () -> NetworkHandler.sendFirstPersonPose(player, "palm"));
        StandBeat.at(MovementFx.RIPPLE_PULSE_CONTACT_TICKS, player, () -> strike(player));
        return true;
    }

    private static void strike(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        AABB area = player.getBoundingBox().inflate(RANGE);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive() && player.distanceTo(entity) <= RANGE);

        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().playerAttack(player),
                    DamageScale.hamon(player, target, HamonCombat.scaled(player, DAMAGE)));
            Vec3 push = target.position().subtract(player.position()).normalize().scale(KNOCKBACK);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.35, push.z));
            target.hurtMarked = true;
            Impacts.landed(level, target, push,
                    ImpactSpec.MEDIUM, Impacts.HAMON);
        }

        double faceX = player.getX() + look.x * 1.5;
        double faceY = player.getY() + player.getEyeHeight() * 0.5;
        double faceZ = player.getZ() + look.z * 1.5;

        HamonFx.ring(level, faceX, faceY, faceZ, 2.0, 2);
        HamonFx.ripple(level, faceX, faceY, faceZ, 5);
        HamonFx.spark(level, faceX, faceY, faceZ, 10, 0.45, 0.12);
        level.playSound(null, player.blockPosition(), ModSounds.HAMON_ECHO.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        HamonFx.sound(level, player, 0.9F, 1.0F);
    }
}
