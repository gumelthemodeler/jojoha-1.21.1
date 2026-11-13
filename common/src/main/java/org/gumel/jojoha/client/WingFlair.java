package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.client.pillar.FlairFx;

public final class WingFlair {
    private static final int[] FEATHER_COLOURS = {0xFFF4F1EA, 0xFFE2DCCF, 0xFFCFC6B4, 0xFFB9AE98};
    private static final int FEATHER_GLOW = 0xFFE8E2D2;

    private static final double SHOULDER_HEIGHT = 1.35;
    private static final double SHOULDER_WIDTH = 0.4;
    private static final double WING_SPAN = 1.9;
    private static final double TRAIL_SPEED = 0.9;
    private static final double GROUND_REACH = 4.0;

    private WingFlair() {
    }

    private static Vec3 right(AbstractClientPlayer player) {
        float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
    }

    private static Vec3 shoulder(AbstractClientPlayer player, double side) {
        return player.position().add(0.0, SHOULDER_HEIGHT, 0.0).add(right(player).scale(SHOULDER_WIDTH * side));
    }

    private static Vec3 middle(AbstractClientPlayer player) {
        return player.getBoundingBox().getCenter();
    }

    private static void sound(AbstractClientPlayer player, SoundEvent sound, float volume, float pitch) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            level.playLocalSound(player.getX(), player.getY() + 1.0, player.getZ(), sound, SoundSource.PLAYERS,
                    volume, pitch, false);
        }
    }

    public static void swell(AbstractClientPlayer player) {
        Vec3 right = right(player);
        for (double side : new double[]{-1.0, 1.0}) {
            Gore.tear(shoulder(player, side), right.scale(side * 0.3), 6, 0, 0.08);
        }
        sound(player, SoundEvents.SLIME_SQUISH, 0.7F, 0.6F);
        sound(player, SoundEvents.BONE_BLOCK_BREAK, 0.6F, 0.7F);
    }

    public static void ooze(AbstractClientPlayer player) {
        Vec3 right = right(player);
        for (double side : new double[]{-1.0, 1.0}) {
            Gore.tear(shoulder(player, side), right.scale(side * 0.2).add(0.0, -0.3, 0.0), 2, 0, 0.05);
        }
    }

    public static void burst(AbstractClientPlayer player) {
        Vec3 right = right(player);
        for (double side : new double[]{-1.0, 1.0}) {
            Vec3 at = shoulder(player, side);
            Vec3 out = right.scale(side).add(0.0, 0.3, 0.0);
            Gore.tear(at, out, 26, 10, 0.32);
            Gore.tear(at, right.scale(side * 0.4).add(0.0, -0.6, 0.0), 10, 3, 0.12);
            FlairFx.spray(at, out, 0.7, 14, 0.35, FEATHER_COLOURS, 1, 22, 0.006, false);
        }
        Vec3 chest = player.position().add(0.0, SHOULDER_HEIGHT - 0.2, 0.0);
        FlairFx.ring(chest, new Vec3(0, 1, 0), 0.4, 3.0, 7, FEATHER_COLOURS[0], 2, true);
        FlairFx.shake(chest, 0.55F);
        sound(player, SoundEvents.BONE_BLOCK_BREAK, 1.0F, 0.5F);
        sound(player, SoundEvents.SLIME_SQUISH, 0.9F, 0.5F);
        sound(player, SoundEvents.ENDER_DRAGON_FLAP, 0.8F, 1.1F);
    }

    public static void fold(AbstractClientPlayer player) {
        Vec3 right = right(player);
        for (double side : new double[]{-1.0, 1.0}) {
            Vec3 tip = shoulder(player, side).add(right.scale(side * WING_SPAN * 0.6));
            FlairFx.gust(tip, shoulder(player, side), 5, FEATHER_COLOURS);
            FlairFx.burst(shoulder(player, side), 10, 0.18, FEATHER_COLOURS, 1, 16, 0.008, false);
            Gore.tear(shoulder(player, side), right.scale(side * 0.3), 8, 2, 0.12);
        }
        FlairFx.glow(player.position().add(0.0, SHOULDER_HEIGHT, 0.0), 1.6, 5, FEATHER_GLOW);
        sound(player, SoundEvents.PHANTOM_FLAP, 0.8F, 1.3F);
        sound(player, SoundEvents.BONE_BLOCK_BREAK, 0.6F, 1.1F);
    }

    public static void takeoff(AbstractClientPlayer player) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Vec3 feet = player.position();
        Vec3 middle = middle(player);
        Vec3 ground = null;
        for (int down = 0; down < GROUND_REACH && ground == null; down++) {
            ground = FlairFx.floor(level, feet.subtract(0.0, down, 0.0));
        }
        if (ground != null) {
            FlairFx.groundRing(ground, 0.5, 4.2, 8, FlairFx.WIND_COLOURS[0], 2, true);
            FlairFx.groundRing(ground, 0.3, 2.4, 6, FlairFx.WIND_COLOURS[1], 1, false);
            FlairFx.crumbs(level, ground, 18, 1.6, 0.3);
            FlairFx.gust(middle, ground.add(0.0, 0.2, 0.0), 12, FlairFx.WIND_COLOURS);
        }
        FlairFx.ring(middle, new Vec3(0, 1, 0), 0.6, 3.6, 7, FEATHER_COLOURS[0], 3, true);
        FlairFx.spray(feet, new Vec3(0, -1, 0), 1.2, 30, 0.45, FlairFx.WIND_COLOURS, 1, 10, 0.0, false);
        Vec3 right = right(player);
        for (double side : new double[]{-1.0, 1.0}) {
            Vec3 tip = shoulder(player, side).add(right.scale(side * WING_SPAN));
            FlairFx.burst(tip, 12, 0.22, FEATHER_COLOURS, 1, 24, 0.006, false);
            FlairFx.star(tip, 0.6, 4, FEATHER_COLOURS[0]);
        }
        FlairFx.glow(middle, 3.2, 6, FEATHER_GLOW);
        FlairFx.shake(middle, 0.7F);
        sound(player, SoundEvents.ENDER_DRAGON_FLAP, 1.0F, 0.8F);
        sound(player, SoundEvents.PHANTOM_SWOOP, 0.8F, 0.7F);
    }

    public static void boost(AbstractClientPlayer player) {
        Vec3 middle = middle(player);
        Vec3 heading = player.getDeltaMovement().lengthSqr() > 0.01 ? player.getDeltaMovement().normalize()
                : player.getLookAngle();
        Vec3 behind = middle.subtract(heading.scale(1.2));
        FlairFx.ring(behind, heading, 0.5, 2.8, 6, FEATHER_COLOURS[0], 2, true);
        FlairFx.ring(behind.subtract(heading.scale(1.4)), heading, 0.3, 1.8, 8, FlairFx.WIND_COLOURS[1], 1, false);
        FlairFx.gust(middle.add(heading.scale(1.5)), middle.subtract(heading.scale(5.0)), 14, FlairFx.WIND_COLOURS);
        Vec3 right = right(player);
        for (double side : new double[]{-1.0, 1.0}) {
            Vec3 tip = middle.add(right.scale(side * WING_SPAN));
            FlairFx.spray(tip, heading.scale(-1.0), 0.6, 10, 0.35, FEATHER_COLOURS, 1, 18, 0.006, false);
        }
        FlairFx.glow(behind, 2.4, 5, FEATHER_GLOW);
        FlairFx.shake(middle, 0.35F);
        sound(player, SoundEvents.ENDER_DRAGON_FLAP, 0.9F, 1.3F);
        sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 0.6F);
    }

    public static void trail(AbstractClientPlayer player) {
        Vec3 motion = player.getDeltaMovement();
        double speed = motion.length();
        if (speed < TRAIL_SPEED) {
            return;
        }
        Vec3 middle = middle(player);
        Vec3 right = right(player);
        for (double side : new double[]{-1.0, 1.0}) {
            Vec3 tip = middle.add(right.scale(side * WING_SPAN));
            FlairFx.line(tip, motion.scale(-0.15), 0.6 + speed * 0.5, FlairFx.WIND_COLOURS[0], 5, null, null, 0, 0, 1);
        }
    }
}
