package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

/**
 * A hard burst of speed in the direction of travel - and, once the user understands stopped time,
 * not a burst at all but a step straight to the destination.
 *
 * <p>The evolution into Time Shift is tied to having learned Time Stop, rather than to a counter of
 * its own. Learning to move through a halted world is exactly what would turn a dash into a
 * teleport, so the same understanding buying both keeps the progression legible: the dash changes
 * the moment the user has a reason for it to.
 *
 * <p>The teleport traces its path and stops at the first obstruction, so it can cross a gap but
 * never a wall. A blink that ignored geometry would be a phase door, which is a different and much
 * stronger ability than the one being described.
 */
public final class StandDashSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_dash");
    public static final StandDashSkill INSTANCE = new StandDashSkill();

    private static final int COOLDOWN_TICKS = 40;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private static final double DASH_POWER = 1.7;
    private static final double SHIFT_DISTANCE = 12.0;
    /** Backed off the hit point so the arrival never leaves the user inside the surface they stopped at. */
    private static final double SHIFT_WALL_MARGIN = 0.6;

    private StandDashSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.stand_dash";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    /**
     * A dash, always.
     *
     * <p>Learning Time Stop used to turn this into a teleport behind the player's back - same slot,
     * same key, different move. That behaviour now lives in {@code TimeSkipSkill}, which is bought
     * separately and cannot share a bar with this one.
     */
    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        boolean fired = dash(player, data);
        if (fired) {
            trail(player, data);
        }
        return fired;
    }

    private boolean dash(ServerPlayer player, JojohaPlayerData data) {
        Vec3 heading = dashHeading(player, data);
        if (heading.lengthSqr() < 1.0E-4) {
            return false;
        }

        Vec3 launch = heading.add(0, 0.15, 0);
        player.setDeltaMovement(launch);
        player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), launch));
        player.fallDistance = 0F;

        player.serverLevel().playSound(null, player.blockPosition(), ModSounds.STAND_JUMP.get(),
                SoundSource.PLAYERS, 0.9F, 1.2F);
        return true;
    }

    /**
     * Which way a dash goes: where the player is travelling, or where they are looking if still.
     *
     * <p>Taken from the ground they actually covered rather than from their keys, because a walking
     * player's raw WASD never reaches the server - only the resulting movement does. That travel is
     * sampled on the player tick and remembered (see JojohaPlayerData.recentMoveX): measuring it
     * here would always read zero, since this runs from a packet and the level has already rolled
     * the player's previous position forward by then.
     *
     * <p>The result is that a dash follows a strafe or a backpedal instead of always firing along
     * the crosshair, which is what makes it usable for repositioning rather than only for closing.
     */
    private static Vec3 dashHeading(ServerPlayer player, JojohaPlayerData data) {
        if (data.recentMoveTicks > 0) {
            Vec3 travelled = new Vec3(data.recentMoveX, 0, data.recentMoveZ);
            if (travelled.lengthSqr() > 1.0E-8) {
                return travelled.normalize().scale(DASH_POWER);
            }
        }

        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z);
        return forward.lengthSqr() < 1.0E-4 ? Vec3.ZERO : forward.normalize().scale(DASH_POWER);
    }

    private boolean timeShift(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(SHIFT_DISTANCE));

        HitResult hit = level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        Vec3 arrival = hit.getType() == HitResult.Type.MISS
                ? to
                : hit.getLocation().subtract(player.getLookAngle().scale(SHIFT_WALL_MARGIN));

        // Eye height is removed so the player's feet, not their head, land at the traced point.
        double y = arrival.y - player.getEyeHeight();

        spark(level, player.position());
        player.teleportTo(arrival.x, y, arrival.z);
        player.fallDistance = 0F;
        spark(level, player.position());

        // Its own cue rather than a repurposed teleport: Time Shift is a step through stopped time,
        // not a translocation, and it should not sound like an enderman.
        level.playSound(null, player.blockPosition(), ModSounds.TIME_SKIP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static void spark(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1.0, at.z, 20, 0.3, 0.6, 0.3, 0.05);
    }
    /** How long the trail keeps laying down ghosts after the move fires. */
    private static final int AFTERIMAGE_TICKS = 14;

    private static void trail(ServerPlayer player, JojohaPlayerData data) {
        NetworkHandler.broadcastAfterimages(player,
                StandTypes.byIdOrDefault(data.stand.standId()).auraColorFor(data.stand.skin()),
                AFTERIMAGE_TICKS);
    }
}
