package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

/**
 * Takes hold of something and keeps hold of it.
 *
 * <p>Until now a grab could only happen by accident - it was the thing a punch turned into if you
 * happened to be mid-leap and the target happened to be out of arm's reach. That made the whole
 * holding system unreachable on purpose, which is no way to have a move.
 *
 * <p>The hold itself is {@code StandGrip}, hanging off {@link StandEntity#grab}: the target becomes
 * a passenger of the Stand, positioned at its hand. Everything about how that behaves is described
 * there. This is only the way in.
 *
 * <p>Letting go is not here either, and deliberately has no binding of its own - with something in
 * its hand the Stand's ordinary attack throws it. One move to pick up, the button you were already
 * pressing to put it down again.
 */
public final class GrabSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "grab");
    public static final GrabSkill INSTANCE = new GrabSkill();

    /**
     * Short, because the cost of a grab is already paid in what it stops you doing.
     *
     * <p>A Stand with its hands full cannot punch - its attack throws instead - so holding something
     * is its own restraint. A long cooldown on top would be charging twice for the same thing.
     */
    private static final int COOLDOWN_TICKS = 50;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private GrabSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.grab";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    /** There is no bare-handed version: the whole move is the Stand's hand holding something. */
    @Override
    public boolean requiresStand() {
        return true;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        // Every one of these returns false rather than failing quietly, so nothing is charged and
        // nothing goes on cooldown for a grab that never happened.
        if (stand == null || stand.isHolding()) {
            return false;
        }

        LivingEntity target = SkillTargeting.lookTarget(player);
        if (target == null) {
            return false;
        }

        // Refuses on its own account for things that cannot be held - a player, or something already
        // riding. See StandGrip.take.
        if (!stand.grab(target)) {
            return false;
        }

        player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 0.8F, 1.2F);
        return true;
    }
}
