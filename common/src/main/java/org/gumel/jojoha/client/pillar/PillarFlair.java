package org.gumel.jojoha.client.pillar;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.network.packet.PillarFxPacket;
import org.gumel.jojoha.pillar.PillarFxKind;

public final class PillarFlair {
    private PillarFlair() {
    }

    public static void fx(ClientLevel level, PillarFxKind kind, PillarFxPacket packet, Vec3 at, Vec3 dir) {
        switch (kind) {
            case CHARGE -> FlairFx.charge(packet.a(), packet.b(), packet.ticks());
            case FLARE -> flare(level, packet.a(), packet.b(), packet.ticks() > 0);
            case VENT -> vent(level, packet.a());
            case GALE -> gale(level, at, at.add(dir));
            case SANDSTORM -> sandstorm(level, at, dir);
            case TORNADO -> tornado(level, at);
            case RIFT -> rift(level, at, dir, packet.b(), packet.ticks());
            case NEEDLE -> needle(level, packet.a(), packet.b());
            case VESSEL_GRAB -> vesselGrab(level, packet.a(), packet.b());
            case BOIL -> FlairFx.sparkle(packet.b(), FlairFx.HEAT, packet.ticks());
            case SLASH -> slash(level, at, dir);
            case SAW -> FlairFx.sparkle(packet.b(), FlairFx.LIGHT, packet.ticks());
            case FLASH -> flash(at);
            case RIB_BLADES -> ribs(level, packet.a());
            case BURST -> burstOut(level, packet.a());
            case REACH_IN -> FlairFx.charge(packet.a(), FlairFx.FLESH, packet.ticks());
            case CONTORT_CRACK -> {
                FlairFx.burst(at.add(0, 0.6, 0), 10, 0.18, FlairFx.BLOOD_COLOURS, 1, 10, 0.03, false);
                FlairFx.burst(at.add(0, 0.6, 0), 6, 0.2, FlairFx.BONE_COLOURS, 1, 8, 0.03, false);
            }
            default -> {
            }
        }
    }

    private static Vec3 chest(Entity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.6, 0);
    }

    private static void flare(ClientLevel level, int entityId, int style, boolean on) {
        Entity entity = level.getEntity(entityId);
        if (entity == null) {
            return;
        }
        Vec3 chest = chest(entity);
        switch (style) {
            case FlairFx.WIND -> {
                FlairFx.groundRing(entity.position(), 0.4, on ? 2.6 : 1.4, 5, FlairFx.WIND_COLOURS[0], 2, true);
                FlairFx.glow(chest, on ? 2.4 : 1.4, 4, 0xFFAFC4C8);
                if (on) {
                    for (int i = 0; i < 14; i++) {
                        double angle = Math.PI * 2 * i / 14;
                        Vec3 head = entity.position().add(Math.cos(angle) * 0.9, 0.2 + i * 0.12, Math.sin(angle) * 0.9);
                        FlairFx.line(head, new Vec3(-Math.sin(angle), 0.05, Math.cos(angle)).scale(0.3), 0.6,
                                FlairFx.WIND_COLOURS[i % 3], 10, chest, new Vec3(0, 1, 0), 0.02, 0.05, 1);
                    }
                    FlairFx.crumbs(level, entity.position(), 10, 1.4, 0.2);
                }
            }
            case FlairFx.HEAT -> {
                FlairFx.burst(chest, on ? 24 : 10, 0.22, FlairFx.EMBER_COLOURS, 1, 10, -0.004, true);
                FlairFx.glow(chest, on ? 3.2 : 1.8, 5, 0xFFFF7A22);
                FlairFx.burst(chest, on ? 10 : 4, 0.08, FlairFx.STEAM_COLOURS, 2, 14, -0.004, false);
                if (on) {
                    FlairFx.groundRing(entity.position(), 0.3, 2.0, 5, FlairFx.EMBER_COLOURS[1], 2, true);
                }
            }
            case FlairFx.LIGHT -> {
                Vec3 right = PillarClient.hand(entity, 1F);
                Vec3 left = mirrorHand(entity, right);
                FlairFx.star(right, on ? 0.7 : 0.35, 3, FlairFx.GLINT_COLOURS[0]);
                FlairFx.star(left, on ? 0.7 : 0.35, 3, FlairFx.GLINT_COLOURS[0]);
                FlairFx.glow(right, on ? 1.8 : 1.0, 4, 0xFFFFF0B0);
                FlairFx.glow(left, on ? 1.8 : 1.0, 4, 0xFFFFF0B0);
                FlairFx.burst(right, 8, 0.2, FlairFx.GLINT_COLOURS, 1, 6, 0.01, true);
                FlairFx.burst(left, 8, 0.2, FlairFx.GLINT_COLOURS, 1, 6, 0.01, true);
            }
            default -> {
            }
        }
    }

    private static Vec3 mirrorHand(Entity entity, Vec3 right) {
        float yaw = entity instanceof LivingEntity living ? living.yBodyRot : entity.getYRot();
        double r = Math.toRadians(yaw);
        Vec3 side = new Vec3(-Math.cos(r), 0, -Math.sin(r));
        return right.subtract(side.scale(0.66));
    }

    private static void vent(ClientLevel level, int entityId) {
        Entity entity = level.getEntity(entityId);
        if (entity == null) {
            return;
        }
        Vec3 top = entity.position().add(0, entity.getBbHeight() + 0.1, 0);
        FlairFx.ring(top, new Vec3(0, 1, 0), 0.3, 2.4, 6, FlairFx.STEAM_COLOURS[0], 3, false);
        FlairFx.groundRing(entity.position(), 0.5, 3.0, 6, FlairFx.STEAM_COLOURS[1], 2, false);
        FlairFx.spray(top, new Vec3(0, 1, 0), 0.5, 26, 0.45, FlairFx.STEAM_COLOURS, 2, 14, -0.006, false);
        FlairFx.spray(top, new Vec3(0, 1, 0), 0.8, 18, 0.35, FlairFx.EMBER_COLOURS, 1, 12, 0.01, true);
        FlairFx.glow(top, 3.0, 6, 0xFFFF8A2A);
        FlairFx.glow(entity.position().add(0, entity.getBbHeight() * 0.5, 0), 2.4, 4, 0xFF7A2A10);
        FlairFx.shake(entity.position(), 0.5F);
    }

    private static void gale(ClientLevel level, Vec3 from, Vec3 to) {
        Vec3 span = to.subtract(from);
        if (span.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 heading = span.normalize();
        FlairFx.gust(from, to, 22, FlairFx.WIND_COLOURS);
        FlairFx.crumbTrail(level, from, to, 1.5, 2);
        Vec3 flat = new Vec3(heading.x, 0, heading.z);
        Vec3 side = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : new Vec3(-flat.z, 0, flat.x).normalize();
        Vec3 up = side.cross(heading).normalize();
        ArcFx.arc(ArcFx.WIND, false, to.subtract(heading.scale(0.4)), side, up, 1.1, 0.4, -1.3, 2.6, 2, 2);
        FlairFx.ring(to, heading, 0.3, 2.2, 5, FlairFx.WIND_COLOURS[0], 2, true);
        FlairFx.glow(to, 2.2, 4, 0xFFC8D8DC);
        FlairFx.glow(from, 1.2, 3, 0xFF8EA0A4);
        FlairFx.burst(to, 16, 0.3, FlairFx.WIND_COLOURS, 1, 8, 0.0, false);
        FlairFx.crumbs(level, to, 8, 0.8, 0.25);
        FlairFx.shake(to, 0.45F);
    }

    private static void sandstorm(ClientLevel level, Vec3 from, Vec3 path) {
        Vec3 to = from.add(path);
        FlairFx.gust(from, to, 28, FlairFx.SAND_COLOURS);
        FlairFx.gust(from, to, 12, FlairFx.WIND_COLOURS);
        FlairFx.spray(from, path, 0.25, 30, 0.8, FlairFx.SAND_COLOURS, 1, 10, 0.004, false);
        FlairFx.crumbTrail(level, from, to, 1.2, 2);
        FlairFx.ring(from, path, 0.2, 1.4, 4, FlairFx.SAND_COLOURS[0], 2, false);
        FlairFx.ring(to, path, 0.5, 2.0, 5, FlairFx.WIND_COLOURS[0], 2, true);
        FlairFx.glow(from, 2.0, 4, 0xFFC0A878);
        FlairFx.glow(to, 1.8, 4, 0xFFAEBCC0);
        FlairFx.shake(from, 0.5F);
    }

    private static void tornado(ClientLevel level, Vec3 base) {
        Vec3 axis = new Vec3(0, 1, 0);
        for (int i = 0; i < 3; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double h = level.random.nextDouble() * 0.8;
            double r = 0.5 + h * 0.4;
            Vec3 head = base.add(Math.cos(angle) * r, h, Math.sin(angle) * r);
            Vec3 tangent = new Vec3(-Math.sin(angle), 0, Math.cos(angle));
            FlairFx.line(head, tangent.scale(0.35).add(0, 0.28, 0), 0.5 + level.random.nextDouble() * 0.4,
                    FlairFx.WIND_COLOURS[i], 12, base, axis, -0.01, 0.12, 1);
        }
        if (level.random.nextInt(2) == 0) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            FlairFx.bit(base.add(Math.cos(angle) * 0.8, 0.1, Math.sin(angle) * 0.8),
                    new Vec3(-Math.sin(angle) * 0.25, 0.3, Math.cos(angle) * 0.25), FlairFx.SAND_COLOURS[1], 2, 14,
                    0.0, false);
        }
        FlairFx.crumbs(level, base, 3, 1.0, 0.35);
    }

    private static void rift(ClientLevel level, Vec3 from, Vec3 dir, int blades, int fanDegrees) {
        double range = dir.length();
        if (range < 1.0E-3) {
            return;
        }
        Vec3 look = dir.normalize();
        int rays = Math.max(5, blades * 2);
        for (int i = 0; i < rays; i++) {
            double turn = Math.toRadians(-fanDegrees / 2.0 + fanDegrees * i / (double) (rays - 1));
            double cos = Math.cos(turn);
            double sin = Math.sin(turn);
            Vec3 ray = new Vec3(look.x * cos - look.z * sin, look.y, look.x * sin + look.z * cos).normalize();
            FlairFx.gust(from.add(ray.scale(0.6)), from.add(ray.scale(range)), 4, FlairFx.WIND_COLOURS);
            FlairFx.crumbTrail(level, from.add(ray.scale(1.5)), from.add(ray.scale(range)), 2.5, 1);
        }
        FlairFx.star(from.add(look.scale(0.6)), 0.8, 3, FlairFx.WIND_COLOURS[0]);
        FlairFx.ring(from, look, 0.4, 3.0, 5, FlairFx.WIND_COLOURS[0], 3, true);
        FlairFx.glow(from.add(look.scale(0.6)), 3.4, 5, 0xFFDCE8EC);
        FlairFx.burst(from, 14, 0.25, FlairFx.BLOOD_COLOURS, 1, 10, 0.03, false);
        FlairFx.shake(from, 0.9F);
    }

    private static void needle(ClientLevel level, int playerId, int targetId) {
        Entity target = level.getEntity(targetId);
        Entity player = level.getEntity(playerId);
        if (target == null) {
            return;
        }
        Vec3 centre = target.getBoundingBox().getCenter();
        FlairFx.star(centre, 0.45, 3, FlairFx.EMBER_COLOURS[1]);
        FlairFx.glow(centre, 1.8, 4, 0xFFFF7A22);
        FlairFx.burst(centre, 14, 0.22, FlairFx.EMBER_COLOURS, 1, 9, 0.01, true);
        FlairFx.burst(centre, 8, 0.15, FlairFx.BLOOD_COLOURS, 1, 10, 0.03, false);
        if (player != null) {
            FlairFx.star(PillarClient.hand(player, 1F), 0.3, 2, FlairFx.EMBER_COLOURS[2]);
        }
        FlairFx.shake(centre, 0.25F);
    }

    private static void vesselGrab(ClientLevel level, int playerId, int targetId) {
        Entity target = level.getEntity(targetId);
        Entity player = level.getEntity(playerId);
        if (target == null) {
            return;
        }
        Vec3 centre = target.getBoundingBox().getCenter();
        FlairFx.burst(centre, 18, 0.2, FlairFx.EMBER_COLOURS, 1, 10, 0.01, true);
        FlairFx.ring(centre, new Vec3(0, 1, 0), 0.3, 1.4, 4, FlairFx.EMBER_COLOURS[2], 2, true);
        FlairFx.glow(centre, 2.4, 5, 0xFFFF6A1A);
        if (player != null) {
            Vec3 hand = PillarClient.hand(player, 1F);
            FlairFx.star(hand, 0.35, 3, FlairFx.EMBER_COLOURS[1]);
            Vec3 span = centre.subtract(hand);
            for (int i = 1; i <= 6; i++) {
                FlairFx.bit(hand.add(span.scale(i / 7.0)), Vec3.ZERO, FlairFx.EMBER_COLOURS[i % 4], 2, 3 + i, 0, true);
                FlairFx.glow(hand.add(span.scale(i / 7.0)), 0.7, 2 + i / 2, 0xFFB04A14);
            }
        }
        FlairFx.shake(centre, 0.3F);
    }

    private static void slash(ClientLevel level, Vec3 from, Vec3 path) {
        Vec3 to = from.add(path);
        FlairFx.star(from, 0.7, 3, FlairFx.GLINT_COLOURS[0]);
        FlairFx.star(to, 1.0, 4, FlairFx.GLINT_COLOURS[0]);
        FlairFx.glow(from, 1.8, 3, 0xFFFFF2C0);
        FlairFx.glow(to, 2.8, 5, 0xFFFFF2C0);
        double length = path.length();
        Vec3 heading = path.normalize();
        for (int i = 0; i < (int) (length * 4); i++) {
            Vec3 at = from.add(heading.scale(level.random.nextDouble() * length));
            FlairFx.bit(at, new Vec3(0, 0.02, 0).add(heading.scale(0.05)), FlairFx.GLINT_COLOURS[i % 3], 1,
                    4 + level.random.nextInt(6), 0.0, true);
        }
        FlairFx.spray(to, heading, 0.7, 18, 0.35, FlairFx.GLINT_COLOURS, 1, 8, 0.01, true);
        Vec3 ground = FlairFx.floor(level, to);
        if (ground != null) {
            FlairFx.groundRing(ground, 0.4, 2.2, 5, FlairFx.GLINT_COLOURS[1], 2, true);
        }
        FlairFx.shake(to, 0.45F);
        level.playLocalSound(to.x, to.y, to.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F,
                1.6F + level.random.nextFloat() * 0.3F, false);
    }

    private static void flash(Vec3 at) {
        FlairFx.star(at, 1.8, 4, FlairFx.GLINT_COLOURS[0]);
        FlairFx.star(at, 1.1, 3, FlairFx.GLINT_COLOURS[2]);
        FlairFx.glow(at, 7.0, 6, 0xFFFFF6D0);
        FlairFx.glow(at, 3.0, 3, 0xFFFFFFFF);
        FlairFx.ring(at, new Vec3(0, 1, 0), 0.4, 4.0, 5, FlairFx.GLINT_COLOURS[0], 3, true);
        FlairFx.burst(at, 34, 0.4, FlairFx.GLINT_COLOURS, 1, 9, 0.005, true);
        FlairFx.shake(at, 0.3F);
    }

    private static void ribs(ClientLevel level, int entityId) {
        Entity entity = level.getEntity(entityId);
        if (entity == null) {
            return;
        }
        Vec3 chest = chest(entity);
        FlairFx.burst(chest, 26, 0.32, FlairFx.BLOOD_COLOURS, 2, 12, 0.035, false);
        FlairFx.burst(chest, 14, 0.28, FlairFx.BONE_COLOURS, 1, 10, 0.03, false);
        FlairFx.ring(chest, new Vec3(0, 1, 0), 0.4, 3.0, 5, FlairFx.BONE_COLOURS[0], 3, false);
        FlairFx.glow(chest, 2.0, 3, 0xFF6A0C0A);
        FlairFx.groundRing(entity.position(), 0.5, 2.6, 5, FlairFx.BLOOD_COLOURS[0], 2, false);
        FlairFx.shake(chest, 0.6F);
    }

    private static void burstOut(ClientLevel level, int entityId) {
        Entity entity = level.getEntity(entityId);
        if (entity == null) {
            return;
        }
        Vec3 chest = chest(entity);
        FlairFx.burst(chest, 34, 0.38, FlairFx.BLOOD_COLOURS, 2, 14, 0.04, false);
        FlairFx.burst(chest, 12, 0.3, FlairFx.BONE_COLOURS, 1, 10, 0.03, false);
        FlairFx.ring(chest, new Vec3(0, 1, 0), 0.3, 2.4, 4, FlairFx.BLOOD_COLOURS[2], 3, false);
        FlairFx.glow(chest, 2.2, 3, 0xFF6A0C0A);
        FlairFx.shake(chest, 0.7F);
    }

    public static void ambient(ClientLevel level, Entity entity, boolean cloak, boolean heat, boolean blades) {
        long time = level.getGameTime();
        Vec3 chest = chest(entity);
        if (cloak) {
            for (int i = 0; i < 2; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double h = 0.1 + level.random.nextDouble() * entity.getBbHeight();
                Vec3 head = entity.position().add(Math.cos(angle) * 0.85, h, Math.sin(angle) * 0.85);
                Vec3 tangent = new Vec3(-Math.sin(angle), 0.02, Math.cos(angle));
                FlairFx.line(head, tangent.scale(0.32), 0.45 + level.random.nextDouble() * 0.35,
                        FlairFx.WIND_COLOURS[i], 6, chest, new Vec3(0, 1, 0), 0.03, 0.06, 1);
            }
            if (time % 2 == 0) {
                FlairFx.peripheralWind(level, entity);
            }
            Vec3 motion = entity.getDeltaMovement();
            if (entity.onGround() && motion.horizontalDistanceSqr() > 0.004 && time % 3 == 0) {
                FlairFx.crumbs(level, entity.position(), 2, 0.5, 0.12);
            }
        }
        if (heat) {
            if (time % 4 == 0) {
                FlairFx.glow(chest, entity.getBbHeight() + 0.6 + (time / 4 % 3) * 0.2, 4, 0xFF4A1806);
            }
            if (time % 2 == 0) {
                FlairFx.bit(entity.position().add((level.random.nextDouble() - 0.5) * 0.7,
                                level.random.nextDouble() * entity.getBbHeight(), (level.random.nextDouble() - 0.5) * 0.7),
                        new Vec3(0, 0.05 + level.random.nextDouble() * 0.03, 0),
                        FlairFx.EMBER_COLOURS[level.random.nextInt(FlairFx.EMBER_COLOURS.length)], 1, 10, -0.002, true);
            }
            if (time % 4 == 0) {
                double sway = Mth.sin(time * 0.6F) * 0.06;
                Vec3 above = entity.position().add((level.random.nextDouble() - 0.5) * 0.5, entity.getBbHeight() + 0.1,
                        (level.random.nextDouble() - 0.5) * 0.5);
                FlairFx.line(above, new Vec3(sway, 0.08, -sway), 0.3, 0xFFF6C49A, 8, null, null, 0, 0, 1);
            }
        }
    }
}
