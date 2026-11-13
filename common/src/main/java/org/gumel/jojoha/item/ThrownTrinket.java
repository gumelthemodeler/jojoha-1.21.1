package org.gumel.jojoha.item;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModRegistries;

public class ThrownTrinket extends ThrowableItemProjectile {
    public static final float CAN_DAMAGE = 1F;

    public ThrownTrinket(EntityType<? extends ThrownTrinket> type, Level level) {
        super(type, level);
    }

    public ThrownTrinket(Level level, LivingEntity thrower) {
        super(ModRegistries.THROWN_TRINKET.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.COCA_COLA.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(result.getEntity() instanceof LivingEntity target) || !(level() instanceof ServerLevel level)) {
            return;
        }
        target.hurt(level.damageSources().thrown(this, getOwner()),
                DamageScale.combat(getOwner(), target, CAN_DAMAGE));
        Stun.stagger(target);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, getItem()), getX(), getY(), getZ(),
                    8, 0.1, 0.1, 0.1, 0.08);
            level.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS, 0.7F, 1.2F);
            discard();
        }
    }
}
