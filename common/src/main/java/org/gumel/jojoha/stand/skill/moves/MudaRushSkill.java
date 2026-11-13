package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.GuardBreak;
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.network.packet.HeavenFxPacket;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.heaven.HeavenShow;
import org.gumel.jojoha.stand.passive.CrushingForcePassive;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.HashSet;
import java.util.Set;

public final class MudaRushSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "muda_rush");
    public static final MudaRushSkill INSTANCE = new MudaRushSkill();

    public enum Pattern {
        HEXAGRAM,
        ZIGZAG,
        SPIRAL
    }

    public static final int LEGS = 6;
    public static final int MARK_EVERY = 3;
    public static final int SETUP_TICKS = LEGS * MARK_EVERY + 4;
    public static final int LEG_TICKS = 3;
    public static final double LEG_LENGTH = 2.0;
    public static final double SWAY = 1.4;
    public static final double HEX_RADIUS = 3.5;
    public static final double SPIRAL_REACH = 6.0;
    public static final double SPIRAL_OUTER = 5.0;
    public static final double SPIRAL_STEP = 0.8;
    public static final double SPIRAL_TURN_DEGREES = 100.0;
    public static final double TARGET_REACH = 18.0;
    public static final double CATCH = 1.6;
    public static final double CATCH_HIGH = 3.0;
    public static final float DAMAGE = 6F;
    public static final double LAUNCH = 0.85;
    public static final double JUGGLE = 0.12;
    public static final double FINAL_LAUNCH = 1.35;
    public static final int GUARD_BREAK_TICKS = 40;
    public static final double TREMOR_RADIUS = 3.5;
    public static final int TREMOR_TICKS = 30;
    private static final double RUSH_SPEED = 1.6;
    private static final double SPEED_MARGIN = 1.15;
    private static final int RETURN_TICKS = 8;
    private static final int MARK_LINGER_TICKS = 10;
    private static final int[] HEX_ORDER = {0, 3, 1, 4, 2, 5};

    private static final int COOLDOWN_TICKS = 200;
    private static final float ENERGY_COST = EnergyWeight.HEAVY.cost();

    private MudaRushSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.muda_rush";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    public static Pattern pattern(boolean sneaking, boolean aimed) {
        if (sneaking) {
            return Pattern.SPIRAL;
        }
        return aimed ? Pattern.HEXAGRAM : Pattern.ZIGZAG;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (stand == null) {
            return false;
        }
        Heaven.telegraph(player);
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        Vec3 heading = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        LivingEntity target = SkillTargeting.lookTarget(player, TARGET_REACH);
        Pattern pattern = pattern(player.isShiftKeyDown(), target != null);
        Vec3[] path = path(player, stand, heading, target, pattern);

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.6F, 1.6F);
        for (int i = 0; i < LEGS; i++) {
            int index = i;
            StandBeat.at(1 + i * MARK_EVERY, player, () -> lay(player, path, index));
        }

        Set<LivingEntity> struck = new HashSet<>();
        int rush = LEGS * LEG_TICKS;
        StandBeat.at(SETUP_TICKS, player, () -> {
            StandEntity rushing = StandSummonHandler.findStand(player, PlayerDataAccess.get(player));
            if (rushing != null) {
                HeavenShow.on(rushing, HeavenFxPacket.RUSH, 0F, rush + RETURN_TICKS);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8F, 1.2F);
        });
        StandBeat.during(SETUP_TICKS + 1, SETUP_TICKS + rush, player, tick -> {
            StandEntity rushing = StandSummonHandler.findStand(player, PlayerDataAccess.get(player));
            if (rushing == null) {
                return;
            }
            int step = tick - SETUP_TICKS - 1;
            int leg = Math.min(LEGS - 1, step / LEG_TICKS);
            if (step % LEG_TICKS == 0) {
                double reach = rushing.position().distanceTo(path[leg]);
                rushing.sendToWork(path[leg], Math.max(RUSH_SPEED, reach / (LEG_TICKS - 1) * SPEED_MARGIN));
                rushing.triggerStrikeForward(leg % 2 == 0 ? "punch" : "punch2");
                if (leg > 0) {
                    tremor(player, rushing, path[leg - 1], leg - 1, false);
                    struck.clear();
                }
            }
            Vec3 along = path[leg].subtract(leg == 0 ? player.position() : path[leg - 1]);
            Vec3 legHeading = new Vec3(along.x, 0.0, along.z).lengthSqr() < 1.0E-4 ? heading
                    : new Vec3(along.x, 0.0, along.z).normalize();
            sweep(player, rushing, legHeading, leg, struck);
        });
        StandBeat.at(SETUP_TICKS + rush + 1, player, () -> {
            StandEntity rushing = StandSummonHandler.findStand(player, PlayerDataAccess.get(player));
            if (rushing != null) {
                tremor(player, rushing, path[LEGS - 1], LEGS - 1, true);
            }
        });
        StandBeat.at(SETUP_TICKS + rush + RETURN_TICKS, player, () -> {
            StandEntity back = StandSummonHandler.findStand(player, PlayerDataAccess.get(player));
            if (back != null) {
                back.clearWork();
            }
        });
        return true;
    }

    private static Vec3[] path(ServerPlayer player, StandEntity stand, Vec3 heading, LivingEntity target,
                               Pattern pattern) {
        Vec3[] path = new Vec3[LEGS];
        double y = target != null ? target.getY() : stand.getY();
        switch (pattern) {
            case HEXAGRAM -> {
                Vec3 centre = new Vec3(target.getX(), y, target.getZ());
                Vec3 toUser = player.position().subtract(centre);
                double base = Math.atan2(toUser.z, toUser.x);
                double radius = Heaven.range(HEX_RADIUS);
                for (int i = 0; i < LEGS; i++) {
                    double angle = base + HEX_ORDER[i] * Math.PI / 3.0;
                    path[i] = centre.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
                }
            }
            case SPIRAL -> {
                Vec3 centre = target != null ? new Vec3(target.getX(), y, target.getZ())
                        : new Vec3(player.getX(), y, player.getZ()).add(heading.scale(Heaven.range(SPIRAL_REACH)));
                Vec3 toUser = player.position().subtract(centre);
                double base = Math.atan2(toUser.z, toUser.x);
                for (int i = 0; i < LEGS; i++) {
                    double angle = base + Math.toRadians(SPIRAL_TURN_DEGREES * i);
                    double radius = Heaven.range(SPIRAL_OUTER - SPIRAL_STEP * i);
                    path[i] = centre.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
                }
            }
            default -> {
                Vec3 side = new Vec3(-heading.z, 0.0, heading.x);
                Vec3 start = new Vec3(player.getX(), y, player.getZ()).add(heading);
                double leg = Heaven.range(LEG_LENGTH);
                for (int i = 0; i < LEGS; i++) {
                    double sway = (i % 2 == 0 ? 1 : -1) * SWAY;
                    path[i] = start.add(heading.scale(leg * (i + 1))).add(side.scale(sway));
                }
            }
        }
        return path;
    }

    private static float yawToward(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    private static void lay(ServerPlayer player, Vec3[] path, int index) {
        StandEntity stand = StandSummonHandler.findStand(player, PlayerDataAccess.get(player));
        if (stand == null) {
            return;
        }
        Vec3 next = index + 1 < LEGS ? path[index + 1] : path[0];
        Vec3 from = index == 0 ? player.position() : path[index - 1];
        float yaw = index + 1 < LEGS ? yawToward(path[index], next) : yawToward(from, path[index]);
        int linger = SETUP_TICKS - (1 + index * MARK_EVERY) + (index + 1) * LEG_TICKS + MARK_LINGER_TICKS;
        stand.triggerStrikeForward(index % 2 == 0 ? "punch" : "punch2");
        HeavenShow.full(player.serverLevel(), HeavenFxPacket.RUSH_MARK, stand.getId(), index, path[index], yaw,
                linger);
        player.serverLevel().playSound(null, BlockPos.containing(path[index]), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.0F, 0.8F + index * 0.12F);
    }

    private static void sweep(ServerPlayer player, StandEntity stand, Vec3 heading, int leg, Set<LivingEntity> struck) {
        ServerLevel level = player.serverLevel();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                stand.getBoundingBox().inflate(CATCH, CATCH_HIGH, CATCH),
                entity -> entity != player && entity.isAlive() && !(entity instanceof StandEntity)
                        && StandHits.hittable(player, entity))) {
            if (!struck.add(target)) {
                continue;
            }
            hit(player, target, heading, leg == LEGS - 1 ? FINAL_LAUNCH : LAUNCH + leg * JUGGLE);
        }
    }

    private static void hit(ServerPlayer player, LivingEntity target, Vec3 heading, double launch) {
        ServerLevel level = player.serverLevel();
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (CrushingForcePassive.isGuarding(target)) {
            GuardBreak.apply(target, GUARD_BREAK_TICKS);
        }
        if (target instanceof Player victim && victim.isBlocking()) {
            victim.disableShield();
        }
        float raw = StandTuning.damage("muda_rush", DAMAGE) * data.stand.powerScale();
        float dealt = StandPassives.scaleOutgoing(player, data,
                StandHits.bodyOf(target), raw);
        HitWindow.open(target);
        target.hurt(player.damageSources().playerAttack(player),
                DamageScale.stand(player, target, dealt));
        target.setDeltaMovement(heading.x * 0.25, launch, heading.z * 0.25);
        target.hurtMarked = true;
        target.fallDistance = 0F;
        data.combatTicks = StandCombatHandler.COMBAT_TIMER_TICKS;
        Impacts.landed(level, target, heading,
                ImpactSpec.HEAVY.silent(), Impacts.FLESH);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS,
                1.0F, 0.8F);
    }

    private static void tremor(ServerPlayer player, StandEntity stand, Vec3 at, int leg, boolean last) {
        ServerLevel level = player.serverLevel();
        BlockPos ground = BlockPos.containing(at.x, at.y - 0.5, at.z);
        HeavenShow.full(level, HeavenFxPacket.RUSH_LEG, stand.getId(), last ? 1 : 0,
                new Vec3(at.x, ground.getY() + 1.0, at.z), leg, 0);
        if (last) {
            level.playSound(null, ground, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.6F);
            level.playSound(null, ground, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 0.5F);
        }
        level.playSound(null, ground, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
        BlockState floor = level.getBlockState(ground);
        if (!floor.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), at.x, at.y + 0.1, at.z,
                    30, 1.2, 0.1, 1.2, 0.15);
        }
        level.playSound(null, ground, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.45F, 1.5F);
        double radius = Heaven.range(TREMOR_RADIUS);
        for (LivingEntity shaken : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(at, at).inflate(radius, 1.5, radius),
                entity -> entity != player && entity.onGround() && !(entity instanceof StandEntity))) {
            shaken.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TREMOR_TICKS, 1, false, true, true));
            shaken.setDeltaMovement(shaken.getDeltaMovement().add(0.0, 0.25, 0.0));
            shaken.hurtMarked = true;
        }
    }
}
