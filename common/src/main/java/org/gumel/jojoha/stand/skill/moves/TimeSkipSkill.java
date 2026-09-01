package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

/**
 * A step through stopped time: the user vanishes and reappears a dozen blocks along.
 *
 * <p>This used to be what Stand Dash quietly turned into. Learning Time Stop replaced the dash
 * outright - same slot, same key, different move and a different name on the bar - so a player who
 * wanted the dash back had no way to ask for it, and a player who never noticed the swap had a move
 * behave differently one day for no visible reason.
 *
 * <p>It is its own move now, bought from its own node. What stops you having both at once is not
 * that one becomes the other but that the bar refuses to hold both - see {@link #replaces}.
 */
public final class TimeSkipSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "time_skip");
    public static final TimeSkipSkill INSTANCE = new TimeSkipSkill();

    private static final int COOLDOWN_TICKS = 50;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    /** How far the step carries, and how far short of a wall it stops. */
    private static final double DISTANCE = 12.0;
    private static final double WALL_MARGIN = 0.6;

    private TimeSkipSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.time_skip";
    }

    /** The dash it grew out of. Holding both would be holding the same key twice. */
    @Override
    public ResourceLocation replaces() {
        return StandDashSkill.ID;
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
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(DISTANCE));

        HitResult hit = level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        Vec3 arrival = hit.getType() == HitResult.Type.MISS
                ? to
                : hit.getLocation().subtract(player.getLookAngle().scale(WALL_MARGIN));

        // Eye height is removed so the player's feet, not their head, land at the traced point.
        double y = arrival.y - player.getEyeHeight();

        spark(level, player.position());
        player.teleportTo(arrival.x, y, arrival.z);
        player.fallDistance = 0F;
        spark(level, player.position());

        // Its own cue rather than a repurposed teleport: this is a step through stopped time, not a
        // translocation, and it should not sound like an enderman.
        level.playSound(null, player.blockPosition(), ModSounds.TIME_SKIP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static void spark(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1.0, at.z, 12, 0.3, 0.6, 0.3, 0.02);
    }
}
