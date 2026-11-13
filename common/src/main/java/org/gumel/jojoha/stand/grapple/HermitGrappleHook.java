package org.gumel.jojoha.stand.grapple;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.hierophant.Tendrils;

import java.util.List;

public class HermitGrappleHook extends Entity {
    public static final double SPEED = 2.6;
    public static final double MAX_RANGE = GrappleAim.RANGE;

    private static final EntityDataAccessor<Boolean> DATA_ATTACHED =
            SynchedEntityData.defineId(HermitGrappleHook.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_OWNER =
            SynchedEntityData.defineId(HermitGrappleHook.class, EntityDataSerializers.INT);

    private static final float HOLD_COST = 0.16F;

    private static final EntityDataAccessor<Boolean> DATA_ZIP =
            SynchedEntityData.defineId(HermitGrappleHook.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LEFT =
            SynchedEntityData.defineId(HermitGrappleHook.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> DATA_TENDRIL =
            SynchedEntityData.defineId(HermitGrappleHook.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_TINT =
            SynchedEntityData.defineId(HermitGrappleHook.class, EntityDataSerializers.INT);

    private static final int ZIP_TIMEOUT = 60;

    private int zipTicks;

    private double zipClosest = Double.MAX_VALUE;
    private int zipStallTicks;

    private static final double ZIP_PROGRESS = 0.05;

    private static final int ZIP_STALL_TICKS = 5;

    private Vec3 origin = Vec3.ZERO;

    private Vec3 target;

    public HermitGrappleHook(EntityType<? extends HermitGrappleHook> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public HermitGrappleHook(Level level, Player thrower, Vec3 anchor) {
        this(ModRegistries.GRAPPLE_HOOK.get(), level);

        Vec3 from = thrower.getEyePosition().subtract(0, 0.28, 0);
        this.setPos(from.x, from.y, from.z);
        this.origin = from;
        this.target = anchor;
        this.entityData.set(DATA_OWNER, thrower.getId());

        Vec3 toward = anchor.subtract(from);
        double distance = toward.length();
        this.setDeltaMovement(distance < 1.0E-4
                ? thrower.getLookAngle().scale(SPEED)
                : toward.scale(SPEED / distance));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ATTACHED, false);
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_ZIP, false);
        builder.define(DATA_LEFT, false);
        builder.define(DATA_TENDRIL, false);
        builder.define(DATA_TINT, 0);
    }

    public static HermitGrappleHook tendril(Level level, Player thrower, Vec3 anchor) {
        HermitGrappleHook hook = new HermitGrappleHook(level, thrower, anchor);
        hook.entityData.set(DATA_TENDRIL, true);
        hook.entityData.set(DATA_TINT, Tendrils.tintOf(thrower));
        return hook;
    }

    public int tint() {
        return this.entityData.get(DATA_TINT);
    }

    public boolean isTendril() {
        return this.entityData.get(DATA_TENDRIL);
    }

    public static HermitGrappleHook zip(Level level, Player thrower, Vec3 anchor, boolean leftArm) {
        HermitGrappleHook hook = new HermitGrappleHook(level, thrower, anchor);
        hook.entityData.set(DATA_ZIP, true);
        hook.entityData.set(DATA_LEFT, leftArm);
        return hook;
    }

    public boolean isZip() {
        return this.entityData.get(DATA_ZIP);
    }

    public boolean isLeftArm() {
        return this.entityData.get(DATA_LEFT);
    }

    public boolean isAttached() {
        return this.entityData.get(DATA_ATTACHED);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Player owner = holder();
        return owner == null
                ? super.getBoundingBoxForCulling()
                : super.getBoundingBoxForCulling().minmax(owner.getBoundingBox());
    }

    public static void releaseAll(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        for (HermitGrappleHook hook : player.level().getEntitiesOfClass(
                HermitGrappleHook.class,
                player.getBoundingBox().inflate(MAX_RANGE * 2.5),
                hook -> hook.holder() == player)) {
            hook.discard();
        }
    }

    public static HermitGrappleHook findFor(Player player) {
        return find(player, false);
    }

    public static HermitGrappleHook findSwing(Player player) {
        return find(player, true);
    }

    private static HermitGrappleHook find(Player player, boolean swingOnly) {
        List<HermitGrappleHook> found = player.level().getEntitiesOfClass(
                HermitGrappleHook.class,
                player.getBoundingBox().inflate(MAX_RANGE * 2.5),
                hook -> hook.holder() == player && (!swingOnly || !hook.isZip()));

        return found.isEmpty() ? null : found.get(0);
    }

    public Player holder() {
        int id = this.entityData.get(DATA_OWNER);
        return id >= 0 && this.level().getEntity(id) instanceof Player player ? player : null;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        Player owner = holder();
        if (owner == null || !owner.isAlive() || owner.level() != this.level()) {
            this.discard();
            return;
        }

        if (this.position().distanceTo(owner.position()) > MAX_RANGE * 2.5) {
            this.discard();
            return;
        }

        if (owner instanceof ServerPlayer held) {
            VineFlight.heldUp(held);
        }

        if (this.isAttached()) {
            this.setDeltaMovement(Vec3.ZERO);

            if (isZip()) {
                double gap = GrappleZip.gap(owner, this.position());
                boolean arrived = gap <= GrappleZip.ARRIVE;

                if (gap < zipClosest - ZIP_PROGRESS) {
                    zipClosest = gap;
                    zipStallTicks = 0;
                } else {
                    zipStallTicks++;
                }
                boolean stalled = zipStallTicks >= ZIP_STALL_TICKS;

                if (arrived || stalled || ++zipTicks > ZIP_TIMEOUT) {
                    GrappleZip.forget(owner);
                    this.discard();
                }
                return;
            }

            drain(owner);
            return;
        }

        if (this.origin.equals(Vec3.ZERO)) {
            this.origin = this.position();
        }

        if (this.target == null) {
            this.discard();
            return;
        }

        Vec3 remaining = this.target.subtract(this.position());
        double left = remaining.length();

        if (left <= SPEED) {
            this.setPos(this.target.x, this.target.y, this.target.z);
            attach();
            return;
        }

        Vec3 step = remaining.scale(SPEED / left);
        this.setDeltaMovement(step);
        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
    }

    private void drain(Player owner) {
        if (!(this.level() instanceof ServerLevel) || !(owner instanceof ServerPlayer holder)) {
            return;
        }

        JojohaPlayerData data =
                PlayerDataAccess.get(holder);
        if (data == null) {
            return;
        }

        if (data.standEnergy < HOLD_COST) {
            this.discard();
            return;
        }

        data.standEnergy -= HOLD_COST;

        PlayerDataAccess.set(holder, data);
        if (this.tickCount % 5 == 0) {
            PlayerDataAccess.sync(holder);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        Player owner = holder();
        if (owner != null && !this.level().isClientSide()) {
            GrappleGrace.grant(owner);

            if (owner instanceof ServerPlayer holder && speaksForTheThrow()) {
                MovementFx.pose(holder, isZip()
                        ? MovementFx.WEB_ZIP_END
                        : MovementFx.THORN_ZIP_END);
            }
        }
        super.remove(reason);
    }

    private void attach() {
        this.setDeltaMovement(Vec3.ZERO);
        this.entityData.set(DATA_ATTACHED, true);

        if (this.level() instanceof ServerLevel server) {
            server.playSound(null, this.blockPosition(), SoundEvents.WEEPING_VINES_PLACE,
                    SoundSource.PLAYERS, 0.8F, 0.75F);
        }

        if (!isZip() && holder() instanceof ServerPlayer owner) {
            MovementFx.hold(owner,
                    MovementFx.THORN_ZIP_SWING);
        }
    }

    private boolean speaksForTheThrow() {
        return !isZip() || !isLeftArm();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }
}
