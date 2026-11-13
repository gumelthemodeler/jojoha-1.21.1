package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.chariot.Chariot;
import org.gumel.jojoha.stand.combat.StandFist;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class BalestraSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "balestra");
    public static final BalestraSkill INSTANCE = new BalestraSkill();

    public static final int LUNGE_TICKS = 18;
    public static final int LUNGE_WINDOW_TICKS = 8;
    public static final int THRUST_TICKS = 23;
    public static final int TWIST_TICKS = 35;
    public static final int RELEASE_TICKS = 36;
    public static final int RETURN_TICKS = 40;
    public static final double REACH = 6.0;
    private static final double LUNGE_SPEED = 1.5;
    private static final double SET_SPEED = 0.8;
    private static final double SET_AHEAD = 0.9;
    private static final double CATCH = 0.7;
    public static final float DAMAGE = 6.0F;
    public static final float TWIST_DAMAGE = 3.0F;
    private static final double TIP_AHEAD = 2.9;
    private static final double TIP_UP = 1.3;
    private static final double TIP_RIGHT = 0.2;
    private static final double THROUGH = 0.25;
    private static final double NECK_SHARE = 0.85;
    public static final int STUN_TICKS = 25;
    private static final double DRIVE = 0.6;
    private static final double DRIVE_LIFT = 0.2;

    private static final int COOLDOWN_TICKS = 160;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private BalestraSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.balestra";
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
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        return Chariot.rapierless(player);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (!Chariot.is(stand)) {
            return false;
        }
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        Vec3 heading = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3 set = new Vec3(player.getX() + heading.x * SET_AHEAD, stand.getY(), player.getZ() + heading.z * SET_AHEAD);

        stand.sendToWork(set, SET_SPEED);
        stand.triggerStrikeForward("balestra");

        Skewer skewer = new Skewer();
        StandBeat.during(LUNGE_TICKS, RELEASE_TICKS, player, tick -> {
            StandEntity lunging = Chariot.stand(player);
            if (skewer.body != null) {
                if (lunging == null || !skewer.body.isAlive() || skewer.body.isRemoved()) {
                    skewer.body = null;
                    return;
                }
                hold(lunging, skewer, tick);
                if (tick == TWIST_TICKS) {
                    twist(player, skewer.body, heading);
                }
                if (tick == RELEASE_TICKS) {
                    release(skewer.body, heading);
                }
                return;
            }
            if (lunging == null || skewer.spent || tick > LUNGE_TICKS + LUNGE_WINDOW_TICKS) {
                return;
            }
            if (tick == LUNGE_TICKS) {
                lunging.sendToWork(lungeEnd(player, set, heading), LUNGE_SPEED);
            }
            LivingEntity caught = catchFirst(player, lunging, heading);
            if (caught != null) {
                skewer.body = caught;
                skewer.spent = true;
                skewer.caughtAt = tick;
                skewer.from = caught.position();
            }
        });
        StandBeat.at(RETURN_TICKS, player, () -> {
            StandEntity back = Chariot.stand(player);
            if (back != null) {
                back.clearWork();
            }
        });
        return true;
    }

    private static Vec3 lungeEnd(ServerPlayer player, Vec3 from, Vec3 heading) {
        Vec3 to = from.add(heading.scale(REACH));
        BlockHitResult wall = player.level().clip(new ClipContext(from.add(0.0, 0.5, 0.0), to.add(0.0, 0.5, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() == HitResult.Type.MISS) {
            return to;
        }
        double room = Math.max(0.0, wall.getLocation().distanceTo(from.add(0.0, 0.5, 0.0)) - 0.5);
        return from.add(heading.scale(room));
    }

    private static LivingEntity catchFirst(ServerPlayer player, StandEntity stand, Vec3 heading) {
        ServerLevel level = player.serverLevel();
        LivingEntity caught = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, stand.getBoundingBox().inflate(CATCH),
                entity -> entity != player && entity.isAlive() && !(entity instanceof StandEntity)
                        && StandHits.hittable(player, entity))) {
            double distance = candidate.distanceToSqr(stand);
            if (distance < nearest) {
                nearest = distance;
                caught = candidate;
            }
        }
        if (caught == null) {
            return null;
        }
        stand.sendToWork(stand.position(), LUNGE_SPEED);
        caught.stopRiding();
        Chariot.strike(player, caught, Chariot.blow(player, caught, "balestra", DAMAGE));
        Chariot.stun(caught, STUN_TICKS, Stun.TIER_TWO);
        Impacts.landed(level, caught, heading,
                ImpactSpec.HEAVY.silent(), Impacts.STEEL);
        level.playSound(null, caught.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.9F);
        level.playSound(null, caught.blockPosition(), ModSounds.SC_THRUST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return caught;
    }

    private static void hold(StandEntity stand, Skewer skewer, int tick) {
        LivingEntity body = skewer.body;
        float yaw = stand.getYRot();
        Vec3 tip = StandFist.toWorld(stand.position(), yaw, new Vec3(TIP_RIGHT * 16.0, TIP_UP * 16.0, -TIP_AHEAD * 16.0));
        Vec3 back = StandFist.toWorld(Vec3.ZERO, yaw, new Vec3(0.0, 0.0, 16.0));
        Vec3 neck = tip.add(back.scale(body.getBbWidth() * 0.5 + THROUGH))
                .subtract(0.0, body.getEyeHeight() * NECK_SHARE, 0.0);
        double drawn = Math.min(1.0, (tick - skewer.caughtAt) / (double) Math.max(1, THRUST_TICKS - skewer.caughtAt));
        Vec3 goal = skewer.from.lerp(neck, drawn);

        Vec3 to = fits(body, goal) ? goal
                : fits(body, new Vec3(goal.x, body.getY(), goal.z)) ? new Vec3(goal.x, body.getY(), goal.z)
                : body.position();
        body.teleportTo(to.x, to.y, to.z);
        body.setDeltaMovement(Vec3.ZERO);
        body.fallDistance = 0.0F;
        body.hurtMarked = true;
    }

    private static boolean fits(LivingEntity body, Vec3 at) {
        return body.level().noCollision(body, body.getBoundingBox().move(at.subtract(body.position())));
    }

    private static void twist(ServerPlayer player, LivingEntity body, Vec3 heading) {
        ServerLevel level = player.serverLevel();
        Chariot.strike(player, body, Chariot.blow(player, body, "balestra_twist", TWIST_DAMAGE));
        Impacts.landed(level, body, heading,
                ImpactSpec.LIGHT.silent(), Impacts.STEEL);
        level.playSound(null, body.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.9F, 0.6F);
    }

    private static void release(LivingEntity body, Vec3 heading) {
        body.setDeltaMovement(heading.x * DRIVE, DRIVE_LIFT, heading.z * DRIVE);
        body.hurtMarked = true;
    }

    private static final class Skewer {
        private LivingEntity body;
        private boolean spent;
        private int caughtAt;
        private Vec3 from;
    }
}
