package org.gumel.jojoha.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.item.ThrownDagger;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.FlairCuePacket;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandEntity;

import java.util.HashMap;
import java.util.Map;

public class StandKnife extends ThrownDagger {
    private static final EntityDataAccessor<Boolean> DATA_GILDED =
            SynchedEntityData.defineId(StandKnife.class,
                    EntityDataSerializers.BOOLEAN);

    private static final int LINGER_TICKS = 60;
    private static final int RUPTURE_AT_LEAST = 3;

    private int stuck;

    private boolean wounds;

    private boolean pierces;

    private static final byte PIERCE_LEVEL = 4;

    private Volley volley;

    private boolean sparesOwner;

    private static final int HOVER_LIMIT_TICKS = 200;

    public static final class Volley {
        private final int knives;
        private final Map<Integer, Integer> landed = new HashMap<>();

        public Volley(int knives) {
            this.knives = knives;
        }

        private boolean enough(int targetId) {
            return landed.merge(targetId, 1, Integer::sum) == Math.max(RUPTURE_AT_LEAST, (knives + 1) / 2);
        }
    }

    public void partOf(Volley volley) {
        this.volley = volley;
    }

    public StandKnife(EntityType<? extends StandKnife> type, Level level) {
        super(type, level);
    }

    public StandKnife(Level level, LivingEntity thrower, float damage) {
        this(level, thrower, damage, new ItemStack(ModItems.DAGGER_IRON.get()));
    }

    public StandKnife(Level level, LivingEntity thrower, float damage, ItemStack knife) {
        super(ModRegistries.STAND_KNIFE.get(), level, thrower, knife.copyWithCount(1), damage);
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    public void tick() {
        super.tick();
        if (sparesOwner && isNoGravity() && tickCount > HOVER_LIMIT_TICKS) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GILDED, false);
    }

    public void gild() {
        this.entityData.set(DATA_GILDED, true);
    }

    public boolean gilded() {
        return this.entityData.get(DATA_GILDED);
    }

    public void sparesOwner() {
        this.sparesOwner = true;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (sparesOwner && (target == getOwner()
                || target instanceof StandEntity stand && stand.getOwner() == getOwner())) {
            return false;
        }
        return super.canHitEntity(target);
    }

    public void wounding() {
        this.wounds = true;
    }

    public void piercing() {
        this.pierces = true;
    }

    @Override
    public byte getPierceLevel() {
        return pierces ? PIERCE_LEVEL : super.getPierceLevel();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        float healthBefore = 0F;
        if (result.getEntity() instanceof LivingEntity hit) {
            HitWindow.open(hit);
            healthBefore = hit.getHealth() + hit.getAbsorptionAmount();
        }
        super.onHitEntity(result);
        if (this.level() instanceof ServerLevel level
                && result.getEntity() instanceof LivingEntity struck) {
            Impacts.landed(level, struck, getDeltaMovement(), ImpactSpec.LIGHT.silent(), Impacts.STEEL);
            if (volley != null && struck.getHealth() + struck.getAbsorptionAmount() < healthBefore
                    && volley.enough(struck.getId())) {
                NetworkHandler.broadcastFlair(struck,
                        FlairCuePacket.RUPTURE, volley.knives);
            }
            if (wounds && struck.isAlive() && struck.hurtTime == struck.hurtDuration) {
                Bleed.nick(struck);
            }
        }
    }

    @Override
    protected void tickDespawn() {
        if (++stuck >= LINGER_TICKS) {
            discard();
        }
    }
}
