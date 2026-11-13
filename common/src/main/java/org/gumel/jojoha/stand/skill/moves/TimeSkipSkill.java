package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.StandFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class TimeSkipSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "time_skip");
    public static final TimeSkipSkill INSTANCE = new TimeSkipSkill();

    private static final int COOLDOWN_TICKS = 50;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

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

    @Override
    public ResourceLocation replaces() {
        return StandDashSkill.ID;
    }

    @Override
    public boolean movesTheUsersBody() {
        return true;
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

        Vec3 landing = landingFor(player, from, arrival);
        if (landing == null) {
            return false;
        }

        Vec3 left = player.position();
        spark(level, left, player);
        player.teleportTo(landing.x, landing.y, landing.z);
        player.fallDistance = 0F;
        spark(level, player.position(), player);

        StandFx.trail(level, left.add(0.0, 1.0, 0.0),
                player.position().add(0.0, 1.0, 0.0),
                Impacts.colourOf(player));

        level.playSound(null, player.blockPosition(), ModSounds.TIME_SKIP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static Vec3 landingFor(ServerPlayer player, Vec3 from, Vec3 arrival) {
        ServerLevel level = player.serverLevel();
        EntityDimensions size = player.getDimensions(player.getPose());
        double eye = player.getEyeHeight();
        double rise = eye;

        Vec3 back = from.subtract(arrival);
        int steps = Math.max(1, (int) Math.ceil(back.length() / LANDING_STEP));
        for (int step = 0; step <= steps; step++) {
            Vec3 point = arrival.add(back.scale((double) step / steps));
            double feet = point.y - eye;
            for (double lift = 0; lift <= rise; lift += LANDING_LIFT_STEP) {
                AABB box = size.makeBoundingBox(point.x, feet + lift, point.z);
                if (level.noCollision(player, box)) {
                    return settle(level, player, size, new Vec3(point.x, feet + lift, point.z), lift);
                }
            }
        }
        return null;
    }

    private static Vec3 settle(ServerLevel level, ServerPlayer player,
                               EntityDimensions size, Vec3 feet, double lifted) {
        if (lifted <= 0) {
            return feet;
        }
        double y = feet.y;
        for (double drop = LANDING_SETTLE_STEP; drop <= LANDING_LIFT_STEP; drop += LANDING_SETTLE_STEP) {
            if (!level.noCollision(player, size.makeBoundingBox(feet.x, feet.y - drop, feet.z))) {
                break;
            }
            y = feet.y - drop;
        }
        return new Vec3(feet.x, y, feet.z);
    }

    private static final double LANDING_STEP = 0.25;
    private static final double LANDING_LIFT_STEP = 0.125;
    private static final double LANDING_SETTLE_STEP = 0.015625;

    private static void spark(ServerLevel level, Vec3 at, ServerPlayer player) {
        int colour = Impacts.colourOf(player);
        StandFx.ring(level, at.add(0.0, 1.0, 0.0), 1.2, 1);
        StandFx.burst(level, at.add(0.0, 1.0, 0.0), colour, 12, 0.3);
    }
}
