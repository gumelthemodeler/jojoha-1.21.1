package org.gumel.jojoha.vampire.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.vampire.VampireFx;
import org.gumel.jojoha.vampire.VampireMove;
import org.gumel.jojoha.vampire.VampirePoses;

import java.util.HashSet;
import java.util.Set;

public final class BlitzMove implements VampireMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "blitz");
    public static final BlitzMove INSTANCE = new BlitzMove();

    private static final double DISTANCE = 9.0;
    private static final double LAUNCH = 1.9;

    private static final double LIFT = 0.25;

    private static final float DAMAGE = 5.0F;
    private static final double HIT_RADIUS = 1.1;
    private static final double STEP = 0.75;

    private static final int COOLDOWN_TICKS = 100;
    private static final float ENERGY_COST = 18F;

    private static final double RING_EVERY = 2.25;

    private static final int AFTERIMAGE_TICKS = 14;

    public static final int LAUNCH_TICKS = 18;

    private BlitzMove() {
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
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        MovementFx.pose(player, VampirePoses.BLITZ);
        StandBeat.at(LAUNCH_TICKS, player, () -> rush(player));
    }

    private void rush(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        Vec3 look = player.getLookAngle();
        Vec3 heading = new Vec3(look.x, Math.min(look.y, 0.25), look.z).normalize();

        Vec3 from = player.position();

        player.setDeltaMovement(heading.scale(LAUNCH).add(0, LIFT, 0));
        player.hurtMarked = true;

        NetworkHandler.broadcastAfterimages(player,
                VampireFx.BLOOD_ARGB, AFTERIMAGE_TICKS);

        VampireFx.ring(level, from.x, from.y + 0.9, from.z, 2.0, 2);
        VampireFx.blood(level, from.x, from.y + 1.0, from.z, 18, 0.35, 0.22);

        Set<LivingEntity> struck = new HashSet<>();

        for (double travelled = 0; travelled <= DISTANCE; travelled += STEP) {
            Vec3 at = from.add(heading.scale(travelled));
            AABB probe = new AABB(at.subtract(HIT_RADIUS, HIT_RADIUS, HIT_RADIUS),
                    at.add(HIT_RADIUS, HIT_RADIUS + 1.0, HIT_RADIUS));

            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, probe,
                    entity -> entity != player && entity.isAlive())) {
                if (struck.add(target)) {
                    target.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, target, DAMAGE));
                    Impacts.landed(level, target, heading, ImpactSpec.MEDIUM, Impacts.BLOOD);
                }
            }

            VampireFx.blood(level, at.x, at.y + 1.0, at.z, 5, 0.22, 0.09);

            if (travelled % RING_EVERY < STEP) {
                VampireFx.ring(level, at.x, at.y + 1.0, at.z, 1.3, 1);
            }
        }

        level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP,
                SoundSource.PLAYERS, 0.8F, 0.6F);
    }
}
