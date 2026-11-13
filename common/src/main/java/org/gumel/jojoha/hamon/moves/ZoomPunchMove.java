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

public final class ZoomPunchMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "zoom_punch");
    public static final ZoomPunchMove INSTANCE = new ZoomPunchMove();

    private static final double REACH = 10.0;

    private static final float DAMAGE = 3.5F;
    private static final double PULL = 0.45;
    private static final int COOLDOWN_TICKS = 30;
    private static final float ENERGY_COST = 12F;

    private static final double STEP = 0.5;
    private static final double HIT_RADIUS = 0.6;

    private static final int ARM_TICKS = 8;

    private ZoomPunchMove() {
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
        MovementFx.pose(player, MovementFx.ZOOM_PUNCH);
        NetworkHandler.sendFirstPersonPose(player, MovementFx.ZOOM_PUNCH);
        StandBeat.at(MovementFx.ZOOM_PUNCH_CONTACT_TICKS, player, () -> strike(player));
        return true;
    }

    private static void strike(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        LivingEntity struck = null;
        Vec3 at = eye;

        for (double travelled = 1.0; travelled <= REACH; travelled += STEP) {
            at = eye.add(look.scale(travelled));
            AABB probe = new AABB(at.subtract(HIT_RADIUS, HIT_RADIUS, HIT_RADIUS),
                    at.add(HIT_RADIUS, HIT_RADIUS, HIT_RADIUS));

            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, probe,
                    entity -> entity != player && entity.isAlive())) {
                struck = candidate;
                break;
            }
            if (struck != null) {
                break;
            }
        }

        for (double travelled = 1.0; travelled <= eye.distanceTo(at); travelled += STEP) {
            Vec3 point = eye.add(look.scale(travelled));
            HamonFx.spark(level, point.x, point.y, point.z, 1, 0.04, 0.0);
        }

        HamonFx.ring(level, at.x, at.y, at.z, 1.5, struck == null ? 0 : 2);
        HamonFx.ripple(level, at.x, at.y, at.z, struck == null ? 1 : 4);

        NetworkHandler.sendZoomPunch(level, player, at, ARM_TICKS);

        if (struck == null) {
            level.playSound(null, player.blockPosition(), ModSounds.HAMON_SWOOSH.get(),
                    SoundSource.PLAYERS, 0.5F, 1.5F);
            return;
        }

        struck.hurt(level.damageSources().playerAttack(player),
                DamageScale.hamon(player, struck, HamonCombat.scaled(player, DAMAGE)));

        Vec3 pull = player.position().subtract(struck.position()).normalize().scale(PULL);
        struck.setDeltaMovement(struck.getDeltaMovement().add(pull.x, 0.1, pull.z));
        struck.hurtMarked = true;

        Impacts.landed(level, struck, look, ImpactSpec.HEAVY, Impacts.HAMON);

        HamonFx.sound(level, player, 0.8F, 1.15F);
    }
}
