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
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.List;

public final class LassoOfThornsSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "lasso_of_thorns");

    public static final LassoOfThornsSkill INSTANCE = new LassoOfThornsSkill();

    private static final int COOLDOWN_TICKS = 20;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    public static final String LASSO_TAG = "jojoha_lasso";

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

        MovementFx.pose(player, MovementFx.THROW);
        StandBeat.at(MovementFx.THROW_RELEASE_TICKS, player, () -> rope(player));
        return true;
    }

    private static void rope(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        LivingEntity caught = look(player);
        if (caught == null) {
            level.playSound(null, player.blockPosition(), ModSounds.STAND_HIT.get(),
                    SoundSource.PLAYERS, 0.5F, 1.9F);
            return;
        }

        caught.addTag(LASSO_TAG);
        ((Leashable) caught).setLeashedTo(player, true);

        level.playSound(null, caught.blockPosition(), ModSounds.STAND_HIT.get(),
                SoundSource.PLAYERS, 0.8F, 1.1F);
    }

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

            double offset = toward.subtract(look.scale(along)).length();
            if (offset < bestOffset && offset <= FORGIVENESS) {
                bestOffset = offset;
                best = living;
            }
        }
        return best;
    }
}
