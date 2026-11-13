package org.gumel.jojoha.combat.fx;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.MovementCuePacket;
import org.gumel.jojoha.network.packet.PlayerMovePosePacket;

import java.util.Map;
import java.util.Set;

public final class MovementFx {
    public enum Kind {
        DASH,
        ROLL,
        LEAP,
        DOUBLE_JUMP,
        RISE
    }

    public static final String FRONT_DASH = "frontdash";
    public static final String BACK_DASH = "backdash";
    public static final String LEFT_DASH = "leftdash";
    public static final String RIGHT_DASH = "rightdash";
    public static final String FRONT_ROLL = "frontroll";
    public static final String BACK_ROLL = "backroll";
    public static final String LEAP = "leap";
    public static final String DOUBLE_JUMP = "doublejump";
    public static final String UPPERCUT = "uppercut";

    public static final String GUT_PUNCH = "gut_punch";

    public static final String THROW = "throw";

    public static final String THORN_ZIP_START = "thorn_zip_start";

    public static final String THORN_ZIP_SWING = "thorn_zip_swing";

    public static final String THORN_ZIP_END = "thorn_zip_end";

    public static final String WEB_ZIP_START = "web_zip_start";

    public static final String WEB_ZIP_END = "web_zip_end";

    public static final String TWISTING_GUT_PUNCH_START = "twisting_gut_punch_start";

    public static final String TWISTING_GUT_PUNCH_END = "twisting_gut_punch_end";

    public static final String BARRAGE = "barrage";

    public static final String GUARD = "guard";

    public static final int GUT_PUNCH_CONTACT_TICKS = 8;

    public static final int THROW_RELEASE_TICKS = 10;

    public static final String GUARD_BREAK = "guardbreak";

    public static final String RAPIER_LUNGE = "rapier_lunge";

    public static final String SCYTHE_SWEEP = "scythe_sweep";

    public static final int RAPIER_LUNGE_CONTACT_TICKS = 4;

    public static final int SCYTHE_SWEEP_CONTACT_TICKS = 4;

    public static final String ZOOM_PUNCH = "zoom_punch";

    public static final int ZOOM_PUNCH_CONTACT_TICKS = 15;

    public static final String RIPPLE_PULSE = "ripple_pulse";

    public static final int RIPPLE_PULSE_CONTACT_TICKS = 17;

    public static final String PALM = "palm";

    public static final int PALM_CONTACT_TICKS = 8;

    public static final String GATHER = "gather";

    public static final String CHANNEL = "channel";

    public static final String HEAL = "heal";

    public static final String HEAL_HOLD = "heal_hold";

    public static final String THEWORLD_TIMESTOP_HOLD = "theworld_timestop_hold";

    public static final String THEWORLD_TIMESTOP_PLAY = "theworld_timestop_play";

    public static final Set<String> LOOPING_HOLDS = Set.of(HEAL_HOLD, THEWORLD_TIMESTOP_HOLD);

    public static final String RIPPLE_BARRIER = "ripple_barrier";

    public static final String OVERDRIVE = "overdrive";

    public static final int OVERDRIVE_RELEASE_TICKS = 25;

    public static final String BUBBLE_LAUNCHER = "bubble_launcher";

    public static final int BUBBLE_LAUNCH_TICKS = 16;

    public static final String BUBBLE_CUTTER = "bubble_cutter";

    public static final int BUBBLE_CUTTER_CONTACT_TICKS = 22;

    public static final int BUBBLE_CUTTER_HOLD_TICKS = 27;

    public static final String CLACKER_HIT = "clacker_hit";

    public static final String CLACKER_SPIN = "clacker_spin";

    public static final String THUNDER_CROSS = "thunder_cross_split_attack";

    public static final int THUNDER_CROSS_LAUNCH_TICKS = 8;

    public static final int THUNDER_CROSS_CONTACT_TICKS = 41;

    public static final Map<String, Float> HOLD_AT = Map.of(BUBBLE_CUTTER,
            BUBBLE_CUTTER_HOLD_TICKS / 20F);

    public static final Set<String> HAMON_POSES = Set.of(ZOOM_PUNCH, RIPPLE_PULSE,
            RIPPLE_BARRIER, OVERDRIVE, BUBBLE_LAUNCHER, BUBBLE_CUTTER, CLACKER_HIT, CLACKER_SPIN, PALM, GATHER, CHANNEL, HEAL, HEAL_HOLD,
            THUNDER_CROSS);

    private MovementFx() {
    }

    public static void play(ServerPlayer player, Kind kind, Vec3 heading, int colour, int wakeTicks) {
        Vec3 at = player.position();
        Vec3 dir = heading.lengthSqr() < 1.0E-8 ? new Vec3(0, 1, 0) : heading.normalize();
        NetworkHandler.broadcastMovementCue(player, new MovementCuePacket(kind.ordinal(), player.getId(),
                at.x, at.y, at.z, (float) dir.x, (float) dir.y, (float) dir.z, colour & 0xFFFFFF,
                player.onGround(), wakeTicks, player.getRandom().nextInt()));
    }

    public static String poseFor(ServerPlayer player, Vec3 heading, boolean rolls) {
        Vec3 flat = new Vec3(heading.x, 0, heading.z);
        if (flat.lengthSqr() < 1.0E-8) {
            return rolls ? FRONT_ROLL : FRONT_DASH;
        }
        flat = flat.normalize();
        float yaw = player.getYRot() * ((float) Math.PI / 180F);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        double ahead = flat.dot(forward);
        double side = flat.dot(right);
        if (Math.abs(ahead) >= Math.abs(side)) {
            if (ahead >= 0) {
                return rolls ? FRONT_ROLL : FRONT_DASH;
            }
            return rolls ? BACK_ROLL : BACK_DASH;
        }
        return side > 0 ? RIGHT_DASH : LEFT_DASH;
    }

    public static void pose(ServerPlayer player, String animation) {
        NetworkHandler.broadcastMovePose(player, animation);
    }

    public static void hold(ServerPlayer player, String animation) {
        NetworkHandler.broadcastMovePose(player, animation, PlayerMovePosePacket.HELD);
    }

    public static void drop(ServerPlayer player) {
        NetworkHandler.broadcastMovePose(player, "");
    }
}
