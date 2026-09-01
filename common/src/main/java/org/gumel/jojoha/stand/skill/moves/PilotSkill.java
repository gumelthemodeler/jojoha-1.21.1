package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.PilotSystem;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

/**
 * The long-range generic: take the Stand's view and fly it yourself.
 *
 * <p>A toggle rather than a timed state, because piloting is a mode of play rather than a burst -
 * scouting a structure or working a target at distance both take as long as they take. The cost of
 * holding it is paid in energy drain while it runs, not up front.
 *
 * <p>Restricted to long-range Stands by {@code StandSkills}, which never puts it in a close-range
 * Stand's moveset in the first place. That is the classification doing its job: a Stand that cannot
 * leave its user's side has nothing to pilot.
 */
public final class PilotSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "pilot");
    public static final PilotSkill INSTANCE = new PilotSkill();

    /** Short, since it gates a toggle - long enough only to stop the view flickering on a mashed key. */
    private static final int COOLDOWN_TICKS = 20;
    private static final float ENERGY_COST = EnergyWeight.UPKEEP.cost();

    private PilotSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.pilot";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    /** How far in front the Stand is placed when control is taken. */
    private static final double LAUNCH_DISTANCE = 3.0;

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (data.standPiloting) {
            PilotSystem.stop(player, data);
            return true;
        }

        data.standPiloting = true;
        PlayerDataAccess.set(player, data);
        stand.beginPiloting();

        // Pushed out in front before the camera arrives. The Stand follows at its user's shoulder,
        // so taking control without moving it put the camera inside the body it had just left - and
        // a model seen from within is culled, which reads as the player having vanished rather than
        // as the pilot having stayed put. A few blocks of clearance is the whole fix: the body is
        // out in front where it can be seen being left behind.
        Vec3 ahead = player.getLookAngle();
        stand.setPos(player.getX() + ahead.x * LAUNCH_DISTANCE,
                player.getEyeY() + ahead.y * LAUNCH_DISTANCE,
                player.getZ() + ahead.z * LAUNCH_DISTANCE);

        // And from rest, so the follow spring's momentum is not inherited as a lurch on the first
        // tick of flight.
        stand.setDeltaMovement(Vec3.ZERO);

        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.5F, 1.8F);
        return true;
    }
}
