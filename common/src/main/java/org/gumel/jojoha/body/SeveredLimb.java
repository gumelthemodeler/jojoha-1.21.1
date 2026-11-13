package org.gumel.jojoha.body;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.Optional;
import java.util.UUID;

public class SeveredLimb extends Entity {
    public static final int LIFETIME = 6000;
    public static final int SINK_TICKS = 40;
    public static final float SINK_DEPTH = 0.3F;

    private static final double GRAVITY = 0.04;
    private static final double DRAG = 0.98;
    private static final double GROUND_FRICTION = 0.6;
    private static final double BOUNCE = 0.35;
    private static final double BOUNCE_FROM = 0.12;
    private static final double LAUNCH = 0.28;
    private static final double LAUNCH_LIFT = 0.3;
    private static final double PUNCH = 0.35;
    private static final double PUNCH_LIFT = 0.25;
    private static final float SPIN_MIN = 18F, SPIN_SPREAD = 16F;
    private static final float SETTLE = 0.35F;

    private static final EntityDataAccessor<Byte> DATA_LIMB =
            SynchedEntityData.defineId(SeveredLimb.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(SeveredLimb.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> DATA_KIND =
            SynchedEntityData.defineId(SeveredLimb.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SOURCE =
            SynchedEntityData.defineId(SeveredLimb.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BONY =
            SynchedEntityData.defineId(SeveredLimb.class, EntityDataSerializers.BOOLEAN);

    public Object look;

    private float spin;
    private float spinO;
    private float spinSpeed = Float.NaN;

    public SeveredLimb(EntityType<? extends SeveredLimb> type, Level level) {
        super(type, level);
    }

    public static void fling(LivingEntity victim, int limb, Vec3 at, Vec3 away) {
        SeveredLimb severed = new SeveredLimb(ModRegistries.SEVERED_LIMB.get(), victim.level());
        severed.entityData.set(DATA_LIMB, (byte) limb);
        severed.entityData.set(DATA_KIND, BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString());
        severed.entityData.set(DATA_SOURCE, victim.getId());
        severed.entityData.set(DATA_BONY, victim.getType().is(EntityTypeTags.SKELETONS));
        if (victim instanceof Player) {
            severed.entityData.set(DATA_OWNER, Optional.of(victim.getUUID()));
        }
        RandomSource random = victim.getRandom();
        severed.moveTo(at.x, at.y, at.z, random.nextFloat() * 360F, 0F);
        Vec3 flat = new Vec3(away.x, 0, away.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5) : flat;
        severed.setDeltaMovement(flat.normalize().scale(LAUNCH * (0.7 + random.nextDouble() * 0.6))
                .add((random.nextDouble() - 0.5) * 0.1, LAUNCH_LIFT + random.nextDouble() * 0.15,
                        (random.nextDouble() - 0.5) * 0.1));
        victim.level().addFreshEntity(severed);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIMB, (byte) Limbs.RIGHT_ARM);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_KIND, "minecraft:player");
        builder.define(DATA_SOURCE, -1);
        builder.define(DATA_BONY, false);
    }

    public int limb() {
        return this.entityData.get(DATA_LIMB);
    }

    public Optional<UUID> owner() {
        return this.entityData.get(DATA_OWNER);
    }

    public String kind() {
        return this.entityData.get(DATA_KIND);
    }

    public int source() {
        return this.entityData.get(DATA_SOURCE);
    }

    public boolean bony() {
        return this.entityData.get(DATA_BONY);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > LIFETIME) {
            this.discard();
            return;
        }
        if (Float.isNaN(spinSpeed)) {
            RandomSource random = RandomSource.create(this.getUUID().getLeastSignificantBits());
            spinSpeed = (SPIN_MIN + random.nextFloat() * SPIN_SPREAD) * (random.nextBoolean() ? 1F : -1F);
        }
        Vec3 motion = this.getDeltaMovement();
        double falling = motion.y - GRAVITY;
        this.setDeltaMovement(motion.x * DRAG, falling * DRAG, motion.z * DRAG);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            Vec3 now = this.getDeltaMovement();
            double up = falling < -BOUNCE_FROM ? -falling * BOUNCE : now.y;
            this.setDeltaMovement(now.x * GROUND_FRICTION, up, now.z * GROUND_FRICTION);
        }
        spinO = spin;
        if (!this.onGround() || this.getDeltaMovement().horizontalDistanceSqr() > 0.004) {
            spin += spinSpeed;
        } else {
            float rest = Math.round((spin - 90F) / 180F) * 180F + 90F;
            spin += (rest - spin) * SETTLE;
        }
    }

    public float spin(float partialTick) {
        return Mth.lerp(partialTick, spinO, spin);
    }

    public float sink(float partialTick) {
        float into = this.tickCount + partialTick - (LIFETIME - SINK_TICKS);
        return into <= 0F ? 0F : Mth.clamp(into / SINK_TICKS, 0F, 1F) * SINK_DEPTH;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isRemoved() || this.isInvulnerableTo(source)) {
            return false;
        }
        Entity by = source.getEntity();
        if (by == null || this.level().isClientSide) {
            return by != null;
        }
        Vec3 away = this.position().subtract(by.position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1.0E-4 ? by.getLookAngle().multiply(1, 0, 1) : away;
        this.setDeltaMovement(away.normalize().scale(PUNCH).add(0, PUNCH_LIFT, 0));
        this.hurtMarked = true;
        return true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putByte("Limb", (byte) limb());
        tag.putString("Kind", kind());
        tag.putBoolean("Bony", bony());
        tag.putInt("Age", this.tickCount);
        owner().ifPresent(uuid -> tag.putUUID("Owner", uuid));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_LIMB, tag.getByte("Limb"));
        this.entityData.set(DATA_KIND, tag.getString("Kind"));
        this.entityData.set(DATA_BONY, tag.getBoolean("Bony"));
        this.entityData.set(DATA_OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        this.tickCount = tag.getInt("Age");
    }
}
