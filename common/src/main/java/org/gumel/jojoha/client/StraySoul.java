package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.item.StandArrowRitual;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandManifestation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class StraySoul {
    public static final int FLIGHT_TICKS = StandArrowRitual.SOUL_FLIGHT_TICKS;
    public static final int HOLD_TICKS = 24;

    private static final double SPIRAL_RADIUS = 1.3;
    private static final double MIN_RADIUS = 0.7;
    private static final double SPIRAL_RISE = 2.2;
    private static final double MIN_RISE = 0.6;
    private static final double WALL_MARGIN = 0.4;
    private static final int PROBES = 8;
    private static final float TURNS = 2.5F;
    private static final float FLARE = 0.2F;
    private static final float RETURN_FROM = 0.68F;
    private static final double CHEST = 0.6;
    private static final float HEADING_STEP = 0.01F;
    private static final float TIP_SETTLE_TICKS = 2F;

    private static final double STRAND_RADIUS = 0.2;
    private static final float STRAND_SPIN = 0.8F;
    private static final int STRANDS = 2;
    private static final int SUBSTEPS = 3;

    private static final double EXIT_LIFT = 0.6;
    private static final double EXIT_WIND_RADIUS = 0.25;
    private static final float EXIT_WINDS = 1F;

    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    public record Flight(Vec3 tip, double swing, float startTick, double radius, double rise) {
    }

    private static final Map<UUID, Flight> FLIGHTS = new HashMap<>();

    private StraySoul() {
    }

    public static void begin(Player user, float now) {
        Vec3 tip = PlayerStabAnimation.heldTipOffset(user, now + TIP_SETTLE_TICKS);
        Vec3 chest = chest(user, 1F);
        double reach = SPIRAL_RADIUS + WALL_MARGIN;
        double radius = reach;
        for (int i = 0; i < PROBES; i++) {
            radius = Math.min(radius, room(user, chest, Vec3.directionFromRotation(0F, i * 360F / PROBES), reach));
        }
        Vec3 head = user.getPosition(1F).add(0.0, user.getBbHeight(), 0.0);
        double rise = room(user, head, UP, SPIRAL_RISE + WALL_MARGIN);

        RandomSource random = RandomSource.create(user.getUUID().getLeastSignificantBits() ^ (long) now);
        FLIGHTS.put(user.getUUID(), new Flight(tip, random.nextBoolean() ? 1.0 : -1.0, now,
                Mth.clamp(radius - WALL_MARGIN, MIN_RADIUS, SPIRAL_RADIUS),
                Mth.clamp(rise - WALL_MARGIN, MIN_RISE, SPIRAL_RISE)));
    }

    private static double room(Player user, Vec3 from, Vec3 direction, double reach) {
        HitResult hit = user.level().clip(new ClipContext(from, from.add(direction.scale(reach)),
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, user));
        return hit.getType() == HitResult.Type.MISS ? reach : from.distanceTo(hit.getLocation());
    }

    public static Flight flightOf(UUID playerId) {
        return FLIGHTS.get(playerId);
    }

    public static void clear() {
        FLIGHTS.clear();
    }

    public static float travel(float k) {
        return Mth.clamp(k, 0F, 1F);
    }

    public static Vec3 chest(Player user, float partialTick) {
        return user.getPosition(partialTick).add(0.0, user.getBbHeight() * CHEST, 0.0);
    }

    public static Vec3 path(Flight flight, Player user, float partialTick, float u) {
        double angle = angle(flight, u);
        double flared = Mth.lerp(smooth(u / FLARE), flight.tip().horizontalDistance(), flight.radius());
        double radius = flared * (1.0 - returned(u));
        return user.getPosition(partialTick).add(Math.cos(angle) * radius, height(flight, user, u),
                Math.sin(angle) * radius);
    }

    public static double angle(Flight flight, float u) {
        return Math.atan2(flight.tip().z, flight.tip().x) + flight.swing() * TURNS * Mth.TWO_PI * u;
    }

    public static Vec3 heading(Flight flight, Player user, float u) {
        float a = Math.max(u - HEADING_STEP, 0F);
        float b = Math.min(u + HEADING_STEP, 1F);
        Vec3 d = path(flight, user, 1F, b).subtract(path(flight, user, 1F, a));
        return d.lengthSqr() < 1.0E-8 ? UP : d.normalize();
    }

    private static double height(Flight flight, Player user, float u) {
        double top = user.getBbHeight() + flight.rise();
        if (u < RETURN_FROM) {
            return Mth.lerp(smooth(u / RETURN_FROM), flight.tip().y, top);
        }
        return Mth.lerp(returned(u), top, user.getBbHeight() * CHEST);
    }

    private static float returned(float u) {
        float k = Mth.clamp((u - RETURN_FROM) / (1F - RETURN_FROM), 0F, 1F);
        return k * k;
    }

    private static float smooth(float k) {
        k = Mth.clamp(k, 0F, 1F);
        return k * k * (3F - 2F * k);
    }

    private static Vec3 across(Vec3 heading) {
        Vec3 across = heading.cross(UP);
        return across.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : across.normalize();
    }

    private static Vec3 wind(Vec3 heading, double radius, float angle) {
        Vec3 across = across(heading);
        Vec3 over = across.cross(heading).normalize();
        return across.scale(Math.cos(angle) * radius).add(over.scale(Math.sin(angle) * radius));
    }

    public static void tick(Minecraft minecraft, float now) {
        Iterator<Map.Entry<UUID, Flight>> flights = FLIGHTS.entrySet().iterator();
        while (flights.hasNext()) {
            Map.Entry<UUID, Flight> entry = flights.next();
            Flight flight = entry.getValue();
            Player user = minecraft.level.getPlayerByUUID(entry.getKey());
            float age = now - flight.startTick();
            if (user == null || age > FLIGHT_TICKS + HOLD_TICKS) {
                flights.remove();
                continue;
            }
            if (age > FLIGHT_TICKS) {
                continue;
            }

            for (int step = 1; step <= SUBSTEPS; step++) {
                float u = travel((age - 1F + step / (float) SUBSTEPS) / FLIGHT_TICKS);
                trail(minecraft, path(flight, user, 1F, u), heading(flight, user, u), now + step / (float) SUBSTEPS);
            }
        }

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof StandEntity vision && vision.isManifesting()
                    && vision.tickCount < StandManifestation.EXIT_TICKS) {
                Player user = vision.manifestUser();
                if (user != null) {
                    exit(minecraft, vision, user, now);
                }
            }
        }
    }

    private static void exit(Minecraft minecraft, StandEntity vision, Player user, float now) {
        Vec3 from = chest(user, 1F);
        Vec3 to = vision.position().add(0.0, ManifestFigure.FIGURE_HEIGHT * 0.5, 0.0);
        Vec3 heading = to.subtract(from);
        heading = heading.lengthSqr() < 1.0E-6 ? UP : heading.normalize();

        for (int step = 1; step <= SUBSTEPS; step++) {
            float k = Mth.clamp((vision.tickCount - 1F + step / (float) SUBSTEPS) / StandManifestation.EXIT_TICKS, 0F, 1F);
            float u = k * k * (3F - 2F * k);
            double arc = Math.sin(Math.PI * u);
            Vec3 at = from.lerp(to, u).add(0.0, EXIT_LIFT * arc, 0.0)
                    .add(wind(heading, EXIT_WIND_RADIUS * arc, EXIT_WINDS * Mth.TWO_PI * u));
            trail(minecraft, at, heading, now + step / (float) SUBSTEPS);
        }
    }

    private static void trail(Minecraft minecraft, Vec3 at, Vec3 heading, float time) {
        minecraft.level.addParticle(ModRegistries.STAND_AWAKEN.get(), at.x, at.y, at.z, 0.0, 0.0, 0.0);
        for (int i = 0; i < STRANDS; i++) {
            Vec3 strand = at.add(wind(heading, STRAND_RADIUS, time * STRAND_SPIN + i * Mth.TWO_PI / STRANDS));
            minecraft.level.addParticle(strandMote(i), strand.x, strand.y, strand.z, 0.0, 0.0, 0.0);
        }
    }

    private static SimpleParticleType strandMote(int strand) {
        return strand % 2 == 0 ? ModRegistries.STAND_AWAKEN_BLUE.get() : ModRegistries.STAND_AWAKEN_PINK.get();
    }
}
