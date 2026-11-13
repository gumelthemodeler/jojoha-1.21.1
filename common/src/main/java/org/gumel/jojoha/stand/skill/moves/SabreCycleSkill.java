package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.chariot.Chariot;
import org.gumel.jojoha.stand.passive.PiercingThrustPassive;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.joml.Vector3f;

public final class SabreCycleSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sabre_cycle");
    public static final SabreCycleSkill INSTANCE = new SabreCycleSkill();

    public static final int CONTACT_TICKS = 5;
    public static final double LEAST_RADIUS = 2.5;
    public static final double MOST_RADIUS = 5.0;
    public static final float LEAST_DAMAGE = 4.0F;
    public static final float MOST_DAMAGE = 6.0F;
    private static final double REACH_UP = 2.0;
    private static final double SHOVE = 0.4;
    private static final double SHOVE_PER_CHARGE = 0.4;
    public static final int SWEEP_TICKS = 4;
    private static final double SWEEP_MARK_SPACING = 1.0;
    private static final int SWEEP_DUST = 2;
    private static final int CHARGE_GLINTS = 2;
    private static final double GLINT_HEIGHT = 1.0;
    private static final double GLINT_SPEED = 1.1;
    private static final int FULL_RING_MARKS = 28;
    private static final int CHARGE_SOUND_LEAD = 11;
    private static final DustParticleOptions SILVER =
            new DustParticleOptions(new Vector3f(0.82F, 0.86F, 0.92F), 0.7F);

    private static final int COOLDOWN_TICKS = 200;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private SabreCycleSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.sabre_cycle";
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
    public boolean isSustained() {
        return true;
    }

    @Override
    public boolean isSustainActive(Player player) {
        if (player instanceof ServerPlayer server) {
            return Chariot.sabreCharging(server);
        }
        StandEntity stand = Chariot.standFor(player);
        return stand != null && stand.holdPose() == StandEntity.HOLD_SABRE;
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        return isSustainActive(player) ? null : Chariot.rapierless(player);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (Chariot.sabreCharging(player)) {
            release(player, Chariot.stand(player));
            return true;
        }
        if (!Chariot.is(stand)) {
            return false;
        }
        Chariot.beginSabre(player, stand);
        return true;
    }

    public static void release(ServerPlayer player, StandEntity stand) {
        float charge = Chariot.endSabre(player, stand);
        Chariot.cool(player, INSTANCE);
        if (stand == null) {
            return;
        }
        stand.triggerStrikeForward("sabre_cycle");
        player.serverLevel().playSound(null, player.blockPosition(), ModSounds.SC_SPIN.get(),
                SoundSource.PLAYERS, 1.0F, 0.95F + 0.1F * (1F - charge));
        StandBeat.at(CONTACT_TICKS, player, () -> slash(player, charge));
    }

    public static double radius(float charge) {
        return Mth.lerp(charge, LEAST_RADIUS, MOST_RADIUS);
    }

    public static float damage(float charge) {
        return Mth.lerp(charge, LEAST_DAMAGE, MOST_DAMAGE);
    }

    public static void charging(ServerPlayer player, long held) {
        ServerLevel level = player.serverLevel();
        float charge = (float) Math.min(1.0, held / (double) Chariot.SABRE_FULL_TICKS);
        double radius = radius(charge);
        Vec3 centre = player.position();
        RandomSource random = level.getRandom();

        for (int i = 0; i < CHARGE_GLINTS; i++) {
            double around = random.nextDouble() * Math.PI * 2.0;
            double cos = Math.cos(around), sin = Math.sin(around);
            double x = centre.x + cos * radius, z = centre.z + sin * radius;
            level.sendParticles(SILVER, x, centre.y + 0.1, z, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.CRIT, x, centre.y + GLINT_HEIGHT, z, 0, -cos, 0.05, -sin, GLINT_SPEED);
        }

        if (held == Chariot.SABRE_FULL_TICKS - CHARGE_SOUND_LEAD) {
            level.playSound(null, player.blockPosition(), ModSounds.SC_CHARGE.get(), SoundSource.PLAYERS,
                    0.9F, 1.0F);
        }
        if (held == Chariot.SABRE_FULL_TICKS) {
            for (int i = 0; i < FULL_RING_MARKS; i++) {
                double around = i * Math.PI * 2.0 / FULL_RING_MARKS;
                level.sendParticles(SILVER, centre.x + Math.cos(around) * radius, centre.y + 0.1,
                        centre.z + Math.sin(around) * radius, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private static void sweep(ServerLevel level, Vec3 centre, double radius, double start, int marks, int from, int to) {
        for (int i = from; i < to; i++) {
            double around = start + i * Math.PI * 2.0 / marks;
            double x = centre.x + Math.cos(around) * radius;
            double z = centre.z + Math.sin(around) * radius;
            BlockState ground =
                    level.getBlockState(BlockPos.containing(x, centre.y - 0.2, z));
            if (!ground.isAir() && ground.getRenderShape() != RenderShape.INVISIBLE) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                        x, centre.y + 0.1, z, SWEEP_DUST, 0.15, 0.05, 0.15, 0.1);
            }
        }
    }

    private static void slash(ServerPlayer player, float charge) {
        ServerLevel level = player.serverLevel();
        Vec3 centre = player.position();
        double radius = radius(charge);

        double start = Math.toRadians(player.getYRot() + 90.0);
        NetworkHandler.broadcastSabreArc(level, centre, (float) radius, (float) start, charge);
        int marks = Math.max(8, (int) Math.ceil(Math.PI * 2.0 * radius / SWEEP_MARK_SPACING));
        sweep(level, centre, radius, start, marks, 0, marks / SWEEP_TICKS);
        StandBeat.during(1, SWEEP_TICKS - 1, player, tick -> sweep(level, centre, radius, start, marks,
                marks * tick / SWEEP_TICKS, marks * (tick + 1) / SWEEP_TICKS));

        AABB reach = new AABB(centre.subtract(radius, 1.0, radius), centre.add(radius, REACH_UP + 1.0, radius));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, reach,
                entity -> entity != player && entity.isAlive() && !(entity instanceof StandEntity)
                        && StandHits.hittable(player, entity))) {
            Vec3 out = new Vec3(target.getX() - centre.x, 0.0, target.getZ() - centre.z);
            if (out.length() > radius) {
                continue;
            }
            float blow = Chariot.blow(player, target, "sabre_cycle", damage(charge));
            if (charge >= 1F) {
                PiercingThrustPassive.charged(() -> {
                    if (Chariot.strike(player, target, blow)) {
                        Bleed.apply(target);
                    }
                });
            } else {
                Chariot.strike(player, target, blow);
            }
            Vec3 shove = out.lengthSqr() < 1.0E-6 ? Vec3.ZERO : out.normalize().scale(SHOVE + SHOVE_PER_CHARGE * charge);
            target.setDeltaMovement(target.getDeltaMovement().add(shove.x, 0.25, shove.z));
            target.hurtMarked = true;
            Impacts.landed(level, target, out,
                    ImpactSpec.MEDIUM.silent(), Impacts.STEEL);
        }
    }
}
