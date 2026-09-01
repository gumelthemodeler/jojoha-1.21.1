package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;
import java.util.List;

/**
 * A vine round something living, and then it comes with you.
 *
 * <h2>Vanilla's leash, on purpose</h2>
 *
 * <p>The obvious build is a rope entity of our own with a spring pulling the mob along it, and it
 * would be a great deal of work to arrive somewhere worse. Being led is not one behaviour, it is a
 * pile of them: the mob has to follow at a distance, resist at the end of its tether, break free if
 * dragged through a wall, stop pathing where it was going, survive the chunk unloading, and be let
 * go of when the holder dies. Vanilla has all of that, tested, in {@code Leashable}.
 *
 * <p>So this leashes. What the move actually adds is who it may be used on.
 *
 * <h2>It does not ask permission</h2>
 *
 * <p>{@code canBeLeashed} refuses for anything hostile, which is right for a lead in the hand of a
 * farmer and wrong for a thorned vine thrown by a Stand. That check lives in the mob's own
 * interaction handling rather than in {@code setLeashedTo}, so calling the latter directly gets the
 * whole leash mechanic with the livestock rule left out - which is exactly the seam this move wants.
 * A zombie on the end of a vine is the point of it.
 *
 * <p>Pressed again, or pointed at something already on the vine, it lets go.
 */
public final class LassoOfThornsSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "lasso_of_thorns");

    public static final LassoOfThornsSkill INSTANCE = new LassoOfThornsSkill();

    private static final int COOLDOWN_TICKS = 20;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    /**
     * Marks a mob as being held by a vine rather than by a lead.
     *
     * <p>Read by LassoNoLeadMixin, which is what stops the release handing out a free lead item.
     * A tag rather than anything synced, because the only side that has to know is the server.
     */
    public static final String LASSO_TAG = "jojoha_lasso";

    /** How far the loop is thrown, and how wide a miss it forgives. */
    private static final double REACH = 12.0;
    private static final double FORGIVENESS = 2.0;

    private LassoOfThornsSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.lasso_of_thorns";
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

        // Anything already on the end of a vine is released first, wherever it is. Otherwise the
        // only way to let a mob go would be to find it again and aim at it, which is a poor deal
        // when the reason you want rid of it is usually that it is dragging behind you in a fight.
        List<Entity> held = level.getEntities((Entity) null,
                player.getBoundingBox().inflate(Leashable.LEASH_TOO_FAR_DIST),
                entity -> entity instanceof Leashable leashed && leashed.getLeashHolder() == player);

        if (!held.isEmpty()) {
            for (Entity entity : held) {
                entity.removeTag(LASSO_TAG);
                ((Leashable) entity).dropLeash(true, false);
            }
            level.playSound(null, player.blockPosition(), ModSounds.STAND_HIT.get(),
                    SoundSource.PLAYERS, 0.5F, 1.7F);
            return true;
        }

        LivingEntity caught = look(player);
        if (caught == null) {
            return false;
        }

        caught.addTag(LASSO_TAG);
        ((Leashable) caught).setLeashedTo(player, true);

        level.playSound(null, caught.blockPosition(), ModSounds.STAND_HIT.get(),
                SoundSource.PLAYERS, 0.8F, 1.1F);

        if (stand != null) {
            stand.triggerGrabAt(caught);
        }
        return true;
    }

    /**
     * The nearest leashable thing along the line of sight.
     *
     * <p>Scored by how far it sits off the line rather than by distance alone, so aiming at the mob
     * behind a closer one still takes the one being aimed at. The forgiveness is what makes it
     * throwable at a moving target without leading it perfectly.
     */
    private static LivingEntity look(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(REACH));

        AABB search = new AABB(eye, end).inflate(FORGIVENESS);
        LivingEntity best = null;
        double bestOffset = Double.MAX_VALUE;

        for (Entity entity : player.serverLevel().getEntities(player, search)) {
            if (!(entity instanceof LivingEntity living) || !(entity instanceof Leashable leashable)
                    || entity == player || leashable.isLeashed()) {
                continue;
            }

            Vec3 toward = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(eye);
            double along = toward.dot(look);
            if (along <= 0 || along > REACH) {
                continue;
            }

            // Distance from the aim line: the part of the offset that is not along it.
            double offset = toward.subtract(look.scale(along)).length();
            if (offset < bestOffset && offset <= FORGIVENESS) {
                bestOffset = offset;
                best = living;
            }
        }
        return best;
    }
}
