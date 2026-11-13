package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.packet.HeavenFxPacket;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.heaven.HeavenShow;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.Comparator;
import java.util.List;

public final class JudgementFistSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "judgement_fist");
    public static final JudgementFistSkill INSTANCE = new JudgementFistSkill();

    public static final int MAX_CHARGE_TICKS = 40;
    public static final float BASE_POWER = 0.35F;
    public static final float MIN_DAMAGE = 8F;
    public static final float MAX_DAMAGE = 20F;
    public static final double MIN_LENGTH = 6.0;
    public static final double MAX_LENGTH = 16.0;
    public static final double LINE_WIDTH = 1.2;
    public static final double MIN_KNOCKBACK = 1.0;
    public static final double MAX_KNOCKBACK = 3.0;
    public static final double LIFT = 0.3;
    public static final int CONTACT_TICKS = StandEntity.STRIKE_CONTACT_TICKS;

    private static final int MIN_COOLDOWN_TICKS = 80;
    private static final int MAX_COOLDOWN_TICKS = 140;

    private JudgementFistSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.judgement_fist";
    }

    @Override
    public int chargeMaxTicks() {
        return MAX_CHARGE_TICKS;
    }

    @Override
    public int cooldownTicks() {
        return MIN_COOLDOWN_TICKS;
    }

    @Override
    public int cooldownTicks(int chargeTicks) {
        return Math.round(Mth.lerp(power(chargeTicks), MIN_COOLDOWN_TICKS, MAX_COOLDOWN_TICKS));
    }

    @Override
    public float energyCost() {
        return EnergyWeight.STANDARD.cost();
    }

    @Override
    public float energyCost(int chargeTicks) {
        return Mth.lerp(power(chargeTicks), EnergyWeight.STANDARD.cost(), EnergyWeight.HEAVY.cost());
    }

    public static float power(int chargeTicks) {
        return BASE_POWER + (1F - BASE_POWER) * Mth.clamp(chargeTicks / (float) MAX_CHARGE_TICKS, 0F, 1F);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        return activate(player, data, stand, 0);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand, int chargeTicks) {
        if (stand == null) {
            return false;
        }
        float power = power(chargeTicks);
        Heaven.telegraph(player);
        stand.triggerStrikeForward("punch");
        player.serverLevel().playSound(null, stand.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 1.0F, 0.6F + power);
        StandBeat.at(CONTACT_TICKS, player, () -> fire(player, power));
        return true;
    }

    private static Vec3 aimedAt(ServerPlayer player, double length) {
        Vec3 eye = player.getEyePosition();
        Vec3 far = eye.add(player.getLookAngle().scale(length));
        HitResult block = player.level().clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 reach = block.getType() == HitResult.Type.MISS ? far : block.getLocation();
        EntityHitResult entity = ProjectileUtil
                .getEntityHitResult(player.level(), player, eye, reach, new AABB(eye, reach).inflate(1.0),
                        hit -> hit instanceof LivingEntity living && living.isAlive() && !(hit instanceof StandEntity)
                                && hit != player);
        return entity != null ? entity.getEntity().getBoundingBox().getCenter() : reach;
    }

    private static void fire(ServerPlayer player, float power) {
        ServerLevel level = player.serverLevel();
        StandEntity stand = StandSummonHandler.findStand(player, PlayerDataAccess.get(player));
        Vec3 from = stand != null ? stand.position().add(0.0, stand.getBbHeight() * 0.65, 0.0)
                : player.getEyePosition();
        double length = Heaven.range(Mth.lerp(power, MIN_LENGTH, MAX_LENGTH));
        Vec3 aimed = aimedAt(player, length);
        Vec3 toward = aimed.subtract(from);
        Vec3 look = toward.lengthSqr() < 1.0E-4 ? player.getLookAngle() : toward.normalize();
        Vec3 end = from.add(look.scale(length));
        HitResult wall = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                player));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }
        Vec3 stop = end;
        AABB around = new AABB(from, stop).inflate(LINE_WIDTH);
        List<LivingEntity> struck = level.getEntitiesOfClass(LivingEntity.class, around,
                entity -> entity != player && entity.isAlive() && !(entity instanceof StandEntity)
                        && StandHits.hittable(player, entity)
                        && entity.getBoundingBox().inflate(LINE_WIDTH * 0.5).clip(from, stop).isPresent());
        struck.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(from)));
        float damage = Mth.lerp(power, MIN_DAMAGE, MAX_DAMAGE);
        double knockback = Mth.lerp(power, MIN_KNOCKBACK, MAX_KNOCKBACK);
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        Vec3 push = (flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(knockback)).add(0.0, LIFT, 0.0);
        for (LivingEntity target : struck) {
            if (HeavenStrikes.strike(player, target, "judgement_fist", damage, push,
                    power >= 0.9F ? ImpactSpec.FINISHER : ImpactSpec.HEAVY, power >= 0.9F)) {
                Stun.stagger(target);
            }
        }
        if (stand != null) {
            HeavenShow.full(level, HeavenFxPacket.JUDGE, stand.getId(), -1, stop, 0.2F + 0.5F * power, 0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS,
                0.4F + 0.5F * power, 1.6F - 0.5F * power);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS,
                1.2F, 0.7F);
    }
}
