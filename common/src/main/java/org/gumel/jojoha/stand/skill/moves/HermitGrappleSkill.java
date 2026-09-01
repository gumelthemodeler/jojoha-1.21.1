package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.grapple.HermitGrappleHook;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

/**
 * Hermit Purple's vine, thrown.
 *
 * <p>A toggle rather than a cast. Pressing it with nothing out throws the hook; pressing it again
 * lets go, wherever you are and whatever the rope is doing. That is the only shape that works for
 * something you travel on - a grapple on a fixed duration drops you at a time of its choosing, and
 * the whole point of a swing is choosing when to let go of it.
 *
 * <p>The cooldown is therefore on the release rather than the throw, so holding a swing costs
 * nothing and re-throwing has a beat in it.
 */
public final class HermitGrappleSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hermit_grapple");

    public static final HermitGrappleSkill INSTANCE = new HermitGrappleSkill();

    /**
     * None, and it has to be none.
     *
     * <p>A sustained move ends by being used a second time - the key coming up sends the same packet
     * the key going down did, and activate reads the first as a throw and the second as a release.
     * Any cooldown at all therefore eats the release: the vine goes out, the gate closes behind it,
     * and letting go of the key does nothing until the cooldown expires. What is meant to be a hold
     * behaves like a toggle that ignores you.
     *
     * <p>Nothing is lost by dropping it. You cannot throw again without releasing first, because the
     * input latch will not send a second throw while the key is still down, and releasing is the
     * thing that would have been on cooldown anyway.
     */
    private static final int COOLDOWN_TICKS = 0;
    /**
     * Nothing to throw, and nothing to let go of.
     *
     * <p>A sustained move is used twice - once to start and once to stop - and the gate that checks
     * energy cannot tell the two apart, because all it is given is the move and the charge. So a
     * charged release is a release that can be refused, and refusing a release is the worst thing
     * this move can do: the vine stays attached, letting go does nothing, and a hold behaves like a
     * toggle that has stopped listening. Exactly the failure it is easiest to blame on the input.
     *
     * <p>The cost moved onto the vine instead - see HermitGrappleHook, which drains while it holds
     * you. That is the truer model anyway. The effort in a grapple is hanging from it, not throwing
     * it, and paying by the tick means a short swing is cheap and a long haul is not.
     */
    private static final float ENERGY_COST = 0F;

    private HermitGrappleSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.hermit_grapple";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    /**
     * Held, not tapped.
     *
     * <p>You are on the vine until you let go of the key. The toggle in activate is what both ends
     * of that resolve to - see StandSkill.isSustained.
     */
    @Override
    public boolean isSustained() {
        return true;
    }

    /** Running exactly when there is a vine out - see StandSkill.isSustainActive. */
    @Override
    public boolean isSustainActive(net.minecraft.world.entity.player.Player player) {
        return HermitGrappleHook.findSwing(player) != null;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();

        // Already out: this press is the release. Returning true so the press is still consumed -
        // letting go is a use of the move, and the short cooldown is what stops it being spammed.
        // Only ever the vine this press is actually holding. Any other hook belonging to this
        // player is cleared out at the same time rather than left to be found by the next press -
        // one stale anchor used to be enough to swallow every throw that followed it.
        HermitGrappleHook existing = HermitGrappleHook.findSwing(player);
        if (existing != null) {
            // Grapple vines only. Sweeping every hook the player owns would take a zip in flight
            // down with it, which is precisely the bug this pair of calls used to cause.
            for (Entity entity : player.serverLevel().getAllEntities()) {
                if (entity instanceof HermitGrappleHook other
                        && other.holder() == player && !other.isZip()) {
                    other.discard();
                }
            }
            level.playSound(null, player.blockPosition(), ModSounds.STAND_HIT.get(),
                    SoundSource.PLAYERS, 0.4F, 1.6F);
            return true;
        }

        // Chosen before anything is thrown, by the same code that drew the mark - see GrappleAim.
        net.minecraft.world.phys.BlockHitResult anchor =
                org.gumel.jojoha.stand.grapple.GrappleAim.find(player);
        if (anchor == null) {
            return false;
        }

        HermitGrappleHook hook = new HermitGrappleHook(level, player, anchor.getLocation());
        level.addFreshEntity(hook);

        level.playSound(null, player.blockPosition(), ModSounds.STAND_HIT.get(),
                SoundSource.PLAYERS, 0.55F, 1.25F);

        if (stand != null) {
            // The arms are the throw. Hermit Purple is the vine coming off them, so the Stand
            // reaching is the animation and the hook is the thing that leaves.
            stand.triggerPunchAt(player, false);
        }
        return true;
    }
}
