package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * The Stand hurls its own user where they are looking.
 *
 * <p>A close-range Stand's answer to distance: it cannot go and fetch a target the way a long-range
 * one can, so instead it closes the gap bodily. That is the trade the range classes exist to
 * express, which is why this is the close-range generic and piloting is the long-range one.
 *
 * <p>The throw is flattened and floored before it is applied. Aiming straight down would otherwise
 * drive the player into the ground, and looking near the horizon would produce a fast, flat skid
 * rather than a leap - so the horizontal aim is taken from the look vector while the lift is
 * largely supplied regardless of it.
 */
public final class StandLeapSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_leap");
    public static final StandLeapSkill INSTANCE = new StandLeapSkill();

    private static final int COOLDOWN_TICKS = 60;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private static final double HORIZONTAL_POWER = 1.35;
    private static final double MINIMUM_LIFT = 0.62;
    private static final double AIM_LIFT_SHARE = 0.45;

    /**
     * How long after a leap an attack becomes a grab.
     *
     * <p>About a second and a half - long enough to line up a target while airborne, short enough
     * that it reads as a follow-up to the leap rather than a state the player is left sitting in.
     */
    private static final int GRAB_WINDOW_TICKS = 30;

    private StandLeapSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.stand_leap";
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
        Vec3 look = player.getLookAngle();
        Vec3 heading = new Vec3(look.x, 0, look.z);
        if (heading.lengthSqr() < 1.0E-4) {
            // Looking dead up or down - no horizontal intent to work with, so it becomes a hop.
            heading = Vec3.ZERO;
        } else {
            heading = heading.normalize().scale(HORIZONTAL_POWER);
        }

        double lift = Math.max(MINIMUM_LIFT, look.y * AIM_LIFT_SHARE + MINIMUM_LIFT);
        Vec3 launch = new Vec3(heading.x, lift, heading.z);

        player.setDeltaMovement(launch);
        // Player movement is client-authoritative, so the server changing deltaMovement alone would
        // be corrected away on the next position packet. The explicit motion packet is what makes
        // the client apply it too.
        player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), launch));

        // And the server's own bookkeeping is told what it just did.
        //
        // Movement is validated against what the server believes the player is capable of this
        // tick, and a launch it does not know about reads as somebody moving further than they
        // should be able to. The answer to that is a teleport back to the last position it trusted -
        // which mid-leap is behind you, and which arrives or does not depending on where the next
        // movement packet lands. That is the shape of the inconsistent backwards throw testers hit
        // while sprint-jumping, when there is already momentum for the check to be surprised by.
        //
        // Correct whether or not it turns out to be that: the velocity is deliberate and the server
        // should not be surprised by its own doing.
        player.setKnownMovement(launch);
        // Cleared so the throw itself never costs health on landing; the fall afterwards still does.
        player.fallDistance = 0F;

        player.serverLevel().playSound(null, player.blockPosition(), ModSounds.STAND_JUMP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        stand.triggerLeapPose();
        trail(player, data);

        // Opens the follow-up: an attack thrown during the leap reaches out and hauls a distant
        // target in rather than swinging at whatever happens to be next to you.
        data.standLeapGrabTicks = GRAB_WINDOW_TICKS;
        return true;
    }
    /** How long the trail keeps laying down ghosts after the move fires. */
    private static final int AFTERIMAGE_TICKS = 16;

    private static void trail(ServerPlayer player, JojohaPlayerData data) {
        NetworkHandler.broadcastAfterimages(player,
                StandTypes.byIdOrDefault(data.stand.standId()).auraColorFor(data.stand.skin()),
                AFTERIMAGE_TICKS);
    }
}
