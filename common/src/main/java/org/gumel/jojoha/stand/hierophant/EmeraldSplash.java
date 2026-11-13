package org.gumel.jojoha.stand.hierophant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.combat.BarrageDemolition;
import org.gumel.jojoha.stand.passive.ReboundPassive;
import org.gumel.jojoha.stand.passive.SplashFragmentsPassive;
import org.gumel.jojoha.stand.passive.StandPassives;

import java.util.UUID;

public class EmeraldSplash extends ThrowableItemProjectile {
    private static final int LIFETIME = 45;
    private static final int FRAGMENT_LIFETIME = 9;

    private static final double FRAGMENT_SLOWEST = 1.2;

    private static final int BREAK_RADIUS = 1;
    private static final int MOST_BROKEN = 4;

    private static final EntityDataAccessor<Integer> DATA_TINT =
            SynchedEntityData.defineId(EmeraldSplash.class,
                    EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> DATA_FRAGMENT =
            SynchedEntityData.defineId(EmeraldSplash.class,
                    EntityDataSerializers.BOOLEAN);

    private float damage = 2F;
    private EmeraldSparkle trail;
    private boolean breaksBlocks;

    private Vec3 origin;

    private boolean bounced;

    private boolean wounds;

    private UUID spared;

    public EmeraldSplash(EntityType<? extends EmeraldSplash> type, Level level) {
        super(type, level);
    }

    public EmeraldSplash(Level level, LivingEntity owner, float damage, boolean breaksBlocks) {
        super(ModRegistries.EMERALD_SPLASH.get(), owner, level);
        this.damage = damage;
        this.breaksBlocks = breaksBlocks;
    }

    public EmeraldSplash wounding() {
        this.wounds = true;
        return this;
    }

    public static EmeraldSplash fire(ServerLevel level, LivingEntity owner, Vec3 from, Vec3 heading, float speed,
                                     float inaccuracy, float damage, boolean breaksBlocks) {
        EmeraldSplash splash = new EmeraldSplash(level, owner, damage, breaksBlocks);
        if (owner instanceof Player player) {
            splash.entityData.set(DATA_TINT, Tendrils.tintOf(player));
        } else if (owner instanceof Mob mob) {
            splash.entityData.set(DATA_TINT, Tendrils.tintOf(mob));
        }
        splash.origin = from;
        splash.setPos(from.x, from.y, from.z);
        splash.shoot(heading.x, heading.y, heading.z, speed, inaccuracy);
        level.addFreshEntity(splash);
        return splash;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TINT, 0);
        builder.define(DATA_FRAGMENT, false);
    }

    public int tint() {
        return this.entityData.get(DATA_TINT);
    }

    public boolean isFragment() {
        return this.entityData.get(DATA_FRAGMENT);
    }

    private EmeraldSparkle trail() {
        if (trail == null) {
            trail = Tendrils.sparkle(tint());
        }
        return trail;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.EMERALD;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level) {
            level.sendParticles(trail(), getX(), getY(), getZ(), 1, 0.02, 0.02, 0.02, 0.0);
            if (tickCount >= (isFragment() ? FRAGMENT_LIFETIME : LIFETIME)) {
                discard();
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (spared != null && spared.equals(target.getUUID())) {
            return false;
        }
        Entity owner = getOwner();
        if (target instanceof StandEntity other && owner != null) {
            return StandHits.hittable(owner, other);
        }
        if (owner != null && (target == owner
                || target instanceof StandEntity stand && stand.getOwner() == owner)) {
            return false;
        }
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel level) || !(result.getEntity() instanceof LivingEntity hit)) {
            return;
        }
        HitWindow.open(hit);
        float dealt = damage;
        ServerPlayer owner = getOwner() instanceof ServerPlayer player ? player : null;
        if (owner != null) {
            dealt = StandPassives.scaleRanged(owner, hit,
                    origin != null ? origin : owner.position(), damage);
        }
        if (hit.hurt(damageSources().thrown(this, getOwner()), DamageScale.stand(getOwner(), hit, dealt)) && wounds) {
            Bleed.nick(hit);
        }
        burst(level, position(), 6, tint());
        if (owner != null && !isFragment() && grants(owner, SplashFragmentsPassive.ID)
                && level.random.nextFloat() < SplashFragmentsPassive.CHANCE) {
            fragment(level, owner, hit);
        }
        Impacts.landed(level, hit, getDeltaMovement(),
                ImpactSpec.LIGHT.silent(), Impacts.STEEL);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (!breaksBlocks && !bounced && getOwner() instanceof ServerPlayer owner
                && grants(owner, ReboundPassive.ID)) {
            rebound(level, result);
            return;
        }
        burst(level, result.getLocation(), breaksBlocks ? 14 : 4, tint());
        if (breaksBlocks && getOwner() instanceof ServerPlayer owner) {
            smash(level, owner, result.getBlockPos());
        }
        discard();
    }

    private static boolean grants(ServerPlayer owner, ResourceLocation passive) {
        return StandPassives.grants(
                PlayerDataAccess.get(owner), passive);
    }

    private void rebound(ServerLevel level, BlockHitResult result) {
        Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
        Vec3 velocity = getDeltaMovement();
        Vec3 away = velocity.subtract(normal.scale(2.0 * velocity.dot(normal)))
                .scale(ReboundPassive.SPEED_KEPT);
        Vec3 at = result.getLocation().add(normal.scale(0.05));
        setPos(at.x, at.y, at.z);
        setDeltaMovement(away);
        bounced = true;
        damage *= ReboundPassive.DAMAGE_KEPT;
        spared = null;
        hasImpulse = true;
        level.sendParticles(trail(), at.x, at.y, at.z, 6, 0.08, 0.08, 0.08, 0.08);
        level.sendParticles(Tendrils.chips(tint()), at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.1);
        level.playSound(null, result.getBlockPos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                0.5F, 1.8F + level.random.nextFloat() * 0.2F);
    }

    private void fragment(ServerLevel level, ServerPlayer owner, LivingEntity hit) {
        Vec3 velocity = getDeltaMovement();
        double speed = velocity.length();
        if (speed < 1.0E-4) {
            return;
        }
        Vec3 line = velocity.scale(1.0 / speed);
        double across = hit.getBbWidth() + hit.getBbHeight() + 2.0;
        Vec3 exit = hit.getBoundingBox().inflate(0.25).clip(position().add(line.scale(across)), position())
                .orElse(hit.getBoundingBox().getCenter().add(line.scale(hit.getBbWidth() * 0.5 + 0.3)));

        Vec3 up = Math.abs(line.y) < 0.95 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 side = line.cross(up).normalize();
        Vec3 lift = line.cross(side).normalize();
        int count = SplashFragmentsPassive.FRAGMENTS;
        float share = damage * SplashFragmentsPassive.SHARE;
        double out = Math.max(FRAGMENT_SLOWEST, speed * 0.8);
        double start = level.random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < count; i++) {
            double around = start + i * Math.PI * 2.0 / count;
            double open = Math.toRadians(SplashFragmentsPassive.SPREAD
                    * (0.55 + 0.45 * level.random.nextDouble()));
            Vec3 heading = line.scale(Math.cos(open))
                    .add(side.scale(Math.cos(around)).add(lift.scale(Math.sin(around))).scale(Math.sin(open)));
            EmeraldSplash piece = new EmeraldSplash(level, owner, share, false);
            piece.entityData.set(DATA_TINT, tint());
            piece.entityData.set(DATA_FRAGMENT, true);
            piece.origin = origin;
            piece.spared = hit.getUUID();
            piece.setPos(exit.x, exit.y, exit.z);
            piece.shoot(heading.x, heading.y, heading.z, (float) out, 0F);
            level.addFreshEntity(piece);
        }
        level.sendParticles(trail(), exit.x, exit.y, exit.z, 10, 0.1, 0.1, 0.1, 0.18);
        level.sendParticles(Tendrils.chips(tint()), exit.x, exit.y, exit.z, 6, 0.05, 0.05, 0.05, 0.2);
        level.playSound(null, hit.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.7F, 1.4F);
    }

    private static void smash(ServerLevel level, ServerPlayer owner, BlockPos at) {
        if (!owner.mayBuild() || owner.isSpectator()) {
            return;
        }
        int broken = 0;
        for (BlockPos pos : BlockPos.withinManhattan(at, BREAK_RADIUS, BREAK_RADIUS, BREAK_RADIUS)) {
            if (broken >= MOST_BROKEN) {
                break;
            }
            if (BarrageDemolition.breakable(level, owner, pos)
                    && level.destroyBlock(pos, false, owner)) {
                broken++;
            }
        }
        level.playSound(null, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5F, 1.6F);
    }

    private static void burst(ServerLevel level, Vec3 at, int count, int tint) {
        level.sendParticles(Tendrils.sparkle(tint), at.x, at.y, at.z, count * 2, 0.15, 0.15, 0.15, 0.12);
        ParticleOptions chips = Tendrils.chips(tint);
        level.sendParticles(chips, at.x, at.y, at.z, Math.max(2, count / 2), 0.05, 0.05, 0.05, 0.12);
        level.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS,
                0.5F, 1.5F);
    }

    public static void muzzle(ServerLevel level, Player owner, Vec3 at, Vec3 heading,
                              int shards) {
        int tint = Tendrils.tintOf(owner);
        EmeraldSparkle sparkle = Tendrils.sparkle(tint);
        ParticleOptions chips = Tendrils.chips(tint);
        level.sendParticles(sparkle, at.x, at.y, at.z, 6 + shards * 2, 0.2, 0.2, 0.2, 0.1);
        Vec3 ahead = at.add(heading.scale(0.4));
        for (int i = 0; i < 3 + shards; i++) {
            Vec3 spray = heading.add((level.random.nextDouble() - 0.5) * 0.5, (level.random.nextDouble() - 0.5) * 0.5,
                    (level.random.nextDouble() - 0.5) * 0.5);
            level.sendParticles(chips, ahead.x, ahead.y, ahead.z, 0, spray.x, spray.y, spray.z, 0.45);
        }
        level.sendParticles(sparkle, ahead.x, ahead.y, ahead.z, 2 + shards / 2, 0.1, 0.1, 0.1, 0.06);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putBoolean("BreaksBlocks", breaksBlocks);
        tag.putBoolean("Bounced", bounced);
        tag.putBoolean("Fragment", isFragment());
        if (origin != null) {
            tag.putDouble("OriginX", origin.x);
            tag.putDouble("OriginY", origin.y);
            tag.putDouble("OriginZ", origin.z);
        }
        if (spared != null) {
            tag.putUUID("Spared", spared);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        breaksBlocks = tag.getBoolean("BreaksBlocks");
        bounced = tag.getBoolean("Bounced");
        entityData.set(DATA_FRAGMENT, tag.getBoolean("Fragment"));
        origin = tag.contains("OriginX") ? new Vec3(tag.getDouble("OriginX"), tag.getDouble("OriginY"),
                tag.getDouble("OriginZ")) : null;
        spared = tag.hasUUID("Spared") ? tag.getUUID("Spared") : null;
    }
}
