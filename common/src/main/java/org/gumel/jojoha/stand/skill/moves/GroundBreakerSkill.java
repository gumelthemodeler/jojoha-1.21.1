package org.gumel.jojoha.stand.skill.moves;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.StandFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.GroundBreakerFxPacket;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class GroundBreakerSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ground_breaker");
    public static final GroundBreakerSkill INSTANCE = new GroundBreakerSkill();

    private static final double STRIKE_AHEAD = 8.0;

    private static final double PAST_SHOULDER = 1.1;

    private static final double SHOULDER_HEIGHT = 0.85;

    private static final double CRUISE_HEIGHT = 2.5;

    private static final int OUT_TICKS = 15;
    private static final double OUT_SPEED = 0.6;

    private static final double RISE_HEIGHT = 9.0;

    private static final int RISE_TICKS = 12;
    private static final double RISE_SPEED = 0.6;

    private static final int HOVER_TICKS = 4;

    private static final int ANIM_AT_TICKS = OUT_TICKS + RISE_TICKS + HOVER_TICKS;

    private static final int ANIM_SLAM_TICKS = 17;

    private static final int ANIM_STROKE_TICKS = 12;

    private static final int SLAM_TICKS = ANIM_AT_TICKS + ANIM_SLAM_TICKS;

    private static final int DIVE_BEFORE_SLAM = ANIM_SLAM_TICKS - ANIM_STROKE_TICKS;

    private static final double DIVE_SPEED = 2.0;

    private static final double RADIUS = 7.5;

    private static final int WAVE_TICKS = 13;

    private static final float DAMAGE = 6.0F;
    private static final double LAUNCH = 1.05;
    private static final double SHOVE = 0.35;

    private static final int COOLDOWN_TICKS = 160;
    private static final float ENERGY_COST = EnergyWeight.HEAVY.cost();

    private GroundBreakerSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.ground_breaker";
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
        if (stand == null) {
            return false;
        }

        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3 strike = groundAt(player.serverLevel(), player.position().add(flat.scale(STRIKE_AHEAD)));

        NetworkHandler.broadcastGroundBreaker(player.serverLevel(), player, strike, (float) RADIUS,
                GroundBreakerFxPacket.TELEGRAPH, SLAM_TICKS);

        Vec3 side = new Vec3(-flat.z, 0.0, flat.x).scale(PAST_SHOULDER);
        Vec3 shoulder = player.position()
                .add(0.0, player.getBbHeight() * SHOULDER_HEIGHT, 0.0)
                .add(side);
        stand.moveTo(shoulder.x, shoulder.y, shoulder.z, player.getYRot(), 0F);

        Vec3 overCentre = strike.add(0.0, CRUISE_HEIGHT, 0.0);
        stand.sendToWork(overCentre, OUT_SPEED);

        Vec3 apex = strike.add(0.0, RISE_HEIGHT, 0.0);
        StandBeat.after(OUT_TICKS, player, player,
                (thrower, self) -> {
                    StandEntity rising = StandEntity.findFor(thrower);
                    if (rising != null) {
                        rising.sendToWork(apex, RISE_SPEED);
                    }
                });

        StandBeat.after(ANIM_AT_TICKS, player, player,
                (thrower, self) -> {
                    StandEntity winding = StandEntity.findFor(thrower);
                    if (winding != null) {
                        winding.triggerGroundBreakerPose();
                    }
                });

        StandBeat.after(SLAM_TICKS - DIVE_BEFORE_SLAM, player, player,
                (thrower, self) -> {
                    StandEntity diving = StandEntity.findFor(thrower);
                    if (diving != null) {
                        diving.sendToWork(strike.add(0.0, 0.6, 0.0), DIVE_SPEED);
                    }
                });

        StandBeat.after(SLAM_TICKS, player, player,
                (thrower, self) -> quake(thrower, strike));
        return true;
    }

    private static Vec3 groundAt(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        for (int down = 0; down <= 3; down++) {
            BlockPos below = pos.below(down + 1);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                return new Vec3(at.x, below.getY() + 1.0, at.z);
            }
        }
        return at;
    }

    private static void quake(ServerPlayer player, Vec3 at) {
        ServerLevel level = player.serverLevel();
        JojohaPlayerData data = PlayerDataAccess.get(player);

        data.combatTicks = StandCombatHandler.COMBAT_TIMER_TICKS;
        PlayerDataAccess.set(player, data);

        int colour = Impacts.colourOf(player);
        StandFx.kick(level, at.add(0.0, 0.2, 0.0), new Vec3(0.0, 1.0, 0.0),
                colour, 24, 0.5);
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, BlockPos.containing(at), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                0.6F, 0.5F);

        NetworkHandler.broadcastGroundBreaker(level, player, at, (float) RADIUS,
                GroundBreakerFxPacket.SLAM, WAVE_TICKS);

        float base = StandTuning.damage("ground_breaker", DAMAGE) * data.stand.powerScale();
        WAVES.add(new Wave(player, at, base));

        StandEntity stand = StandEntity.findFor(player);
        if (stand != null) {
            stand.clearWork();
        }
    }

    private static final List<Wave> WAVES = new ArrayList<>();

    public static void init() {
        TickEvent.SERVER_POST.register(
                server -> WAVES.removeIf(Wave::advance));
    }

    private static final class Wave {
        private final ServerPlayer thrower;
        private final Vec3 centre;
        private final float base;

        private final Set<UUID> caught = new HashSet<>();

        private int elapsed;

        private Wave(ServerPlayer thrower, Vec3 centre, float base) {
            this.thrower = thrower;
            this.centre = centre;
            this.base = base;
        }

        private boolean advance() {
            if (!thrower.isAlive() || !(thrower.level() instanceof ServerLevel level)) {
                return true;
            }

            double was = RADIUS * (elapsed / (double) WAVE_TICKS);
            elapsed++;
            double now = RADIUS * (elapsed / (double) WAVE_TICKS);

            sweep(level, was, now);

            return elapsed >= WAVE_TICKS;
        }

        private void sweep(ServerLevel level, double was, double now) {
            JojohaPlayerData data = PlayerDataAccess.get(thrower);

            AABB probe = new AABB(centre.subtract(now, 1.5, now), centre.add(now, 2.5, now));
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, probe,
                    entity -> entity != thrower && entity.isAlive()
                            && !(entity instanceof StandEntity))) {

                if (!caught.add(target.getUUID())) {
                    continue;
                }

                double away = Math.sqrt(target.distanceToSqr(centre.x, target.getY(), centre.z));
                if (away > now || away < was) {
                    caught.remove(target.getUUID());
                    continue;
                }

                float damage = StandPassives.scaleOutgoing(
                        thrower, data, target, base);
                if (target.hurt(level.damageSources().playerAttack(thrower), DamageScale.stand(thrower, target, damage))) {
                    Stun.stagger(target);
                }

                Vec3 out = new Vec3(target.getX() - centre.x, 0.0, target.getZ() - centre.z);
                out = out.lengthSqr() < 1.0E-6 ? Vec3.ZERO : out.normalize().scale(SHOVE);
                target.setDeltaMovement(out.x, LAUNCH, out.z);
                target.hurtMarked = true;
                Impacts.landed(level, target, new Vec3(out.x, 1.0, out.z), ImpactSpec.MEDIUM.silent(),
                        Impacts.colourOf(thrower));
            }
        }
    }
}
