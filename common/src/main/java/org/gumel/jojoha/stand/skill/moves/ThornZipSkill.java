package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.grapple.GrappleAim;
import org.gumel.jojoha.stand.grapple.HermitGrappleHook;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

/**
 * Both arms, both vines, and no hanging about.
 *
 * <p>Where the grapple gives you a rope and leaves the rest to you, this is a decision already made:
 * it fastens and it pulls, and it is over in under a second. The two exist side by side because they
 * answer different questions - the grapple is for getting height and keeping it, and the zip is for
 * closing distance.
 *
 * <h2>Two hooks, one anchor</h2>
 *
 * <p>The pair is the whole look of the move and it costs almost nothing: two vines from two arms
 * meeting at one point reads as being hauled, where a single line reads as swinging. They share an
 * anchor rather than taking one each, deliberately - two anchors means two directions to be pulled
 * in, and the average of them is a direction neither vine is pointing.
 *
 * <p>They are told which arm they left, which is the only thing the renderer needs to draw them
 * apart. Everything else about them is identical.
 *
 * <p>Not a hold. There is nothing to sustain: the vines let go on arrival, and the momentum you
 * carry out of it is the point.
 */
public final class ThornZipSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "thorn_zip");

    public static final ThornZipSkill INSTANCE = new ThornZipSkill();

    private static final int COOLDOWN_TICKS = 26;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private ThornZipSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.thorn_zip";
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
        ServerLevel level = player.serverLevel();

        // One vine already out means the arms are busy. Refused rather than silently replacing it,
        // because zipping out of a swing you were in the middle of is a good way to lose the height
        // you had just earned.
        if (HermitGrappleHook.findFor(player) != null) {
            return false;
        }

        BlockHitResult anchor = GrappleAim.find(player);
        if (anchor == null) {
            return false;
        }

        // The top of what was aimed at, not the face of it - see GrappleAim.perch.
        Vec3 at = GrappleAim.perch(level, anchor);

        level.addFreshEntity(HermitGrappleHook.zip(level, player, at, false));
        level.addFreshEntity(HermitGrappleHook.zip(level, player, at, true));

        level.playSound(null, player.blockPosition(), ModSounds.STAND_HIT.get(),
                SoundSource.PLAYERS, 0.7F, 1.5F);

        if (stand != null) {
            stand.triggerPunchAt(player, false);
        }
        return true;
    }
}
