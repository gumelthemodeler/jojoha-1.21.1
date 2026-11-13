package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandAim;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.hierophant.HierophantAim;
import org.gumel.jojoha.stand.hierophant.HierophantClips;
import org.gumel.jojoha.stand.hierophant.Tendrils;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TendrilSpikeSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tendril_spike");
    public static final TendrilSpikeSkill INSTANCE = new TendrilSpikeSkill();

    private static final double REACH = 20.0;
    private static final double SIDE = 1.5;
    private static final double LEG_GAP = 0.3;
    private static final double LEG_AHEAD = 1.1;
    private static final double STEP = 1.2;
    private static final double HIT_RADIUS = 0.9;
    private static final float DAMAGE = 6F;
    private static final double THROW = 0.5;
    private static final double SPIKE_HEIGHT = 1.7;
    private static final double SPIKE_BASE = 0.3;
    private static final double SPIKE_LEAN = 0.5;

    private static final int COOLDOWN_TICKS = 120;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private TendrilSpikeSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.tendril_spike";
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
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();
        float damage = Math.min(10F, StandTuning.damage("tendril_spike", DAMAGE) * data.stand.powerScale());
        Vec3 aim = HierophantAim.point(player, REACH);
        if (stand != null) {
            stand.triggerStrikeForward(HierophantClips.SPIKE);
        }
        Vec3 origin = HierophantAim.planted(player, stand, LEG_AHEAD);
        Vec3 flat = new Vec3(aim.x - origin.x, 0.0, aim.z - origin.z);
        if (flat.lengthSqr() < 1.0) {
            flat = new Vec3(player.getLookAngle().x, 0.0, player.getLookAngle().z);
        }
        flat = flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0.0, flat.x);
        Vec3 goal = new Vec3(aim.x, origin.y, aim.z);
        double distance = Math.min(REACH, goal.subtract(origin).horizontalDistance());
        int steps = Math.max(2, (int) Math.ceil(distance / STEP));
        int lead = stand != null ? HierophantClips.SPIKE_CONTACT_TICKS - 1 : 0;

        Set<UUID> struck = new HashSet<>();
        int tint = Tendrils.tintOf(player);
        final Vec3 heading = flat;
        for (double s : new double[]{-1.0, 1.0}) {
            Vec3 start = origin.add(side.scale(s * LEG_GAP));
            StandBeat.during(lead + 1, lead + steps, player, tick -> {
                double t = (tick - lead) / (double) steps;
                Vec3 along = start.lerp(goal, t).add(side.scale(s * SIDE * Math.sin(Math.PI * t)));
                Vec3 ground = ground(level, along);
                if (ground != null) {
                    spike(level, player, ground, heading, damage, struck, tint, 1.0);
                }
            });
        }
        StandBeat.at(lead + steps + 1, player, () -> {
            Vec3 ground = ground(level, goal);
            if (ground != null) {
                spike(level, player, ground, heading, damage, struck, tint, BIG);
                level.playSound(null, BlockPos.containing(ground), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS,
                        1.0F, 0.7F);
            }
        });
        level.playSound(null, BlockPos.containing(origin), ModSounds.HG_EXTEND.get(), SoundSource.PLAYERS,
                1.0F, 0.6F);
        return true;
    }

    private static final double BIG = 1.6;

    private static final int STRANDS = 5;
    private static final double TWIST = 0.65;
    private static final int ROOTS = 4;
    private static final int STANDS = 16;

    private static void spike(ServerLevel level, ServerPlayer player, Vec3 at, Vec3 heading, float damage,
                              Set<UUID> struck, int tint, double scale) {
        var random = level.getRandom();
        double height = (SPIKE_HEIGHT + random.nextDouble() * 0.9) * scale;
        Vec3 tip = at.add(heading.scale(SPIKE_LEAN * scale)).add((random.nextDouble() - 0.5) * 0.2, height,
                (random.nextDouble() - 0.5) * 0.2);
        int strands = scale > 1.0 ? STRANDS + 2 : STRANDS;
        int lasts = STANDS + (scale > 1.0 ? 6 : 0);
        double base = SPIKE_BASE * 1.2 * scale;
        double wind = (random.nextBoolean() ? 1.0 : -1.0) * TWIST * Math.PI * 2.0;
        double turn = random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < strands; i++) {
            double start = turn + i * Math.PI * 2.0 / strands;
            List<Vec3> points = new ArrayList<>();
            for (int j = 0; j <= 5; j++) {
                double t = j / 5.0;
                if (j == 5) {
                    points.add(tip);
                    break;
                }
                Vec3 centre = at.lerp(tip, t);
                double radius = base * Math.pow(1.0 - t, 0.9);
                double angle = start + wind * t;
                points.add(centre.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius));
            }
            Tendrils.show(level, Tendrils.newKey(), points, i < 2 ? Tendrils.THICK : Tendrils.THIN, lasts, tint);
        }
        for (int i = 0; i < ROOTS; i++) {
            double angle = turn + (i + random.nextDouble() * 0.6) * Math.PI * 2.0 / ROOTS;
            double length = (0.5 + random.nextDouble() * 0.6) * scale;
            Vec3 out = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            Tendrils.show(level, Tendrils.newKey(), List.of(at.add(0.0, 0.03, 0.0),
                    at.add(out.scale(length * 0.55)).add(0.0, 0.12, 0.0), at.add(out.scale(length)).add(0.0, 0.03, 0.0)),
                    Tendrils.THIN, lasts, tint);
        }

        BlockPos under = BlockPos.containing(at.x, at.y - 0.5, at.z);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(under)),
                at.x, at.y + 0.1, at.z, (int) (10 * scale), 0.3 * scale, 0.05, 0.3 * scale, 0.15);
        level.sendParticles(Tendrils.sparkle(tint), at.x, at.y + 0.15, at.z, (int) (8 * scale),
                0.35 * scale, 0.05, 0.35 * scale, 0.08);
        level.sendParticles(Tendrils.sparkle(tint), tip.x, tip.y, tip.z, (int) (4 * scale), 0.15, 0.15, 0.15, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, tip.x, tip.y, tip.z, scale > 1.0 ? 5 : 1, 0.1, 0.1, 0.1, 0.04);
        level.playSound(null, BlockPos.containing(at), SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS,
                0.45F, 1.2F + random.nextFloat() * 0.5F);

        double hitRadius = HIT_RADIUS * scale;
        AABB reach = new AABB(at, at).inflate(hitRadius, 0.0, hitRadius).expandTowards(0.0, height, 0.0);
        for (LivingEntity body : level.getEntitiesOfClass(LivingEntity.class, reach,
                e -> e != player && e.isAlive() && !(e instanceof StandEntity) && !e.isSpectator())) {
            if (!struck.add(body.getUUID())) {
                continue;
            }
            HitWindow.open(body);
            if (body.hurt(level.damageSources().playerAttack(player), DamageScale.stand(player, body, StandPassives
                    .scaleRanged(player, body, StandAim.feet(player), damage)))) {
                Stun.stagger(body);
            }
            Impacts.landed(level, body, new Vec3(0.0, 1.0, 0.0),
                    ImpactSpec.LIGHT, Impacts.STEEL);
            body.setDeltaMovement(body.getDeltaMovement().add(0.0, THROW, 0.0));
            body.hurtMarked = true;
        }
    }

    private static Vec3 ground(ServerLevel level, Vec3 at) {
        BlockPos base = BlockPos.containing(at.x, at.y + 2.0, at.z);
        for (int down = 0; down <= 5; down++) {
            BlockPos pos = base.below(down);
            if (level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP)
                    && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                return new Vec3(at.x, pos.getY() + 1.0, at.z);
            }
        }
        return null;
    }
}
