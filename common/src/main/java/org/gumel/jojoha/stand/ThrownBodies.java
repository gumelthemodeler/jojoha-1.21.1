package org.gumel.jojoha.stand;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.network.NetworkHandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ThrownBodies {
    public static final float SPIN_PER_SPEED = 16F;
    public static final float MIN_SPIN = 10F;
    public static final float MAX_SPIN = 28F;
    private static final int LIFT_OFF_TICKS = 3;
    private static final int MAX_FLIGHT_TICKS = 80;
    private static final int DUST_PER_SPEED = 30;
    private static final int MAX_DUST = 40;
    private static final double WALL_SPEED = 0.3;
    private static final int WALL_CHIPS = 18;

    private static final Map<Integer, Flight> FLYING = new HashMap<>();

    private ThrownBodies() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    public static void launch(LivingEntity body, Vec3 velocity) {
        if (!(body.level() instanceof ServerLevel level) || velocity.lengthSqr() < 1.0E-4) {
            return;
        }
        float heading = (float) Math.toDegrees(Math.atan2(velocity.z, velocity.x));
        float spin = Mth.clamp((float) velocity.length() * SPIN_PER_SPEED, MIN_SPIN, MAX_SPIN);
        Flight flight = new Flight(body);
        flight.heading = velocity;
        FLYING.put(body.getId(), flight);
        NetworkHandler.broadcastThrown(level, body, heading, spin);
    }

    private static void tick() {
        if (FLYING.isEmpty()) {
            return;
        }
        Iterator<Flight> flights = FLYING.values().iterator();
        while (flights.hasNext()) {
            Flight flight = flights.next();
            LivingEntity body = flight.body;
            if (body.isRemoved() || !body.isAlive()) {
                flights.remove();
                continue;
            }
            flight.ticks++;
            double speed = body.getDeltaMovement().length();
            flight.fastest = Math.max(flight.fastest, speed);
            if (body.horizontalCollision && !flight.walled && flight.heading.horizontalDistance() >= WALL_SPEED) {
                flight.walled = true;
                wall(body, flight.heading);
            }
            flight.heading = body.getDeltaMovement();
            boolean down = flight.ticks > LIFT_OFF_TICKS && (body.onGround() || body.horizontalCollision
                    || body.isInWater() || body.isInLava());
            if (down || flight.ticks > MAX_FLIGHT_TICKS) {
                land(body, flight.fastest);
                flights.remove();
            }
        }
    }

    private static void land(LivingEntity body, double speed) {
        if (!(body.level() instanceof ServerLevel level) || body.isInWater() || body.isInLava()) {
            return;
        }
        BlockPos under = body.getOnPos();
        BlockState ground = level.getBlockState(under);
        if (ground.isAir()) {
            ground = level.getBlockState(under.below());
        }
        int dust = Mth.clamp((int) (speed * DUST_PER_SPEED), 8, MAX_DUST);
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), body.getX(), body.getY() + 0.1,
                    body.getZ(), dust, body.getBbWidth() * 0.4, 0.1, body.getBbWidth() * 0.4, 0.08);
        }
        level.playSound(null, body.blockPosition(), SoundEvents.GENERIC_BIG_FALL, SoundSource.HOSTILE,
                0.9F, 0.8F + level.getRandom().nextFloat() * 0.2F);
        if (!ground.isAir()) {
            level.playSound(null, body.blockPosition(), ground.getSoundType().getBreakSound(), SoundSource.BLOCKS,
                    0.7F, 0.7F);
        }
    }

    private static void wall(LivingEntity body, Vec3 heading) {
        if (!(body.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 flat = new Vec3(heading.x, 0.0, heading.z).normalize();
        Vec3 face = body.position().add(0.0, body.getBbHeight() * 0.5, 0.0)
                .add(flat.scale(body.getBbWidth() * 0.5 + 0.3));
        BlockState wall = level.getBlockState(BlockPos.containing(face));
        Impacts.landed(level, body, face, flat, ImpactSpec.HEAVY, Impacts.PLAYER);
        if (!wall.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, wall), face.x, face.y, face.z,
                    WALL_CHIPS, 0.1, body.getBbHeight() * 0.3, 0.1, 0.15);
            level.playSound(null, BlockPos.containing(face), wall.getSoundType().getBreakSound(), SoundSource.BLOCKS,
                    1.0F, 0.6F);
        }
    }

    private static final class Flight {
        private final LivingEntity body;
        private int ticks;
        private double fastest;
        private Vec3 heading = Vec3.ZERO;
        private boolean walled;

        private Flight(LivingEntity body) {
            this.body = body;
        }
    }
}
