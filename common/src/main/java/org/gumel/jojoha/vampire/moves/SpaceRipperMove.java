package org.gumel.jojoha.vampire.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.vampire.RipperPath;
import org.gumel.jojoha.vampire.VampireFx;
import org.gumel.jojoha.vampire.VampireMove;
import org.gumel.jojoha.vampire.VampirePoses;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class SpaceRipperMove implements VampireMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "space_ripper_stingy_eyes");
    public static final SpaceRipperMove INSTANCE = new SpaceRipperMove();

    private static final double REACH = RipperPath.REACH;

    private static final float DAMAGE = 9.0F;

    private static final int FIRE_FROM = 20;
    private static final int FIRE_TO = 35;
    private static final int VARIANT_FIRE_FROM = 22;
    private static final int VARIANT_FIRE_TO = 32;

    private static final int TRAIL_TICKS = 8;

    private static final int MIST_EVERY = 5;

    private static final int MIST_TICKS = 3;

    private static final int COOLDOWN_TICKS = 300;
    private static final float ENERGY_COST = 70F;

    private static final Set<UUID> FIRED_VARIANT = new HashSet<>();

    private SpaceRipperMove() {
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
        boolean variant = !FIRED_VARIANT.remove(player.getUUID());
        if (variant) {
            FIRED_VARIANT.add(player.getUUID());
        }
        MovementFx.pose(player, variant
                ? VampirePoses.SPACE_RIPPER_VARIANT
                : VampirePoses.SPACE_RIPPER);

        int from = variant ? VARIANT_FIRE_FROM : FIRE_FROM;
        int to = variant ? VARIANT_FIRE_TO : FIRE_TO;
        Set<LivingEntity> cut = new HashSet<>();
        StandBeat.during(from, to, player, tick -> {
            ServerLevel level = player.serverLevel();
            boolean first = tick == from;

            for (double side : new double[]{-1.0, 1.0}) {
                fire(player, level, VampireFx.eye(player, side), side > 0 ? 1 : 0, cut,
                        to - from + 1, (tick - from) % MIST_TICKS == 0, first);
            }

            if (first) {
                level.playSound(null, player.blockPosition(), ModSounds.VAMPIRE_LASER.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, player.blockPosition(), ModSounds.VAMPIRE_LASER_FIRE.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            } else if ((tick - from) % 4 == 0) {
                level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.PLAYERS, 0.35F, 1.8F);
            }
        });
    }

    private void fire(ServerPlayer player, ServerLevel level, Vec3 from, int eye, Set<LivingEntity> cut,
                      int fireTicks, boolean mist, boolean first) {
        RipperPath.Result beam = RipperPath.walk(player, from);

        for (LivingEntity target : beam.struck()) {
            if (!cut.add(target)) {
                continue;
            }
            if (target.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, target, DAMAGE))) {
                Bleed.apply(target);
            }
            Impacts.landed(level, target, target.getEyePosition().subtract(from), ImpactSpec.MEDIUM.silent(), Impacts.RIPPER);
            VampireFx.ripperRing(level, target.getX(), target.getEyeY(), target.getZ(), 1.4, 1);
            VampireFx.blood(level, target.getX(), target.getEyeY(), target.getZ(), 8, 0.2, 0.12);
        }

        NetworkHandler.sendSpaceRipper(level, player, beam.points(), eye,
                fireTicks, fireTicks + TRAIL_TICKS);

        if (mist) {
            List<Vec3> points = beam.points();
            for (int i = 0; i < points.size(); i += MIST_EVERY) {
                Vec3 at = points.get(i);
                VampireFx.ripperMist(level, at.x, at.y, at.z, 1, 0.06);
            }
        }

        if (first) {
            VampireFx.ripperRing(level, from.x, from.y, from.z, 0.7, 0);
        }
    }
}
