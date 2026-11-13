package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.packet.HeavenFxPacket;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.heaven.FallenMobs;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.heaven.HeavenShow;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.ArrayList;
import java.util.List;

public final class RestorationSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "restoration");
    public static final RestorationSkill INSTANCE = new RestorationSkill();

    public static final double REACH = 24.0;
    public static final double REVIVE_RADIUS = 6.0;
    private static final float PILLAR_HEIGHT = 14F;
    private static final int PILLAR_TICKS = 30;
    private static final int BEAM_TICKS = 20;

    private static final int COOLDOWN_TICKS = 700;
    public static final int SELF_COOLDOWN_TICKS = 3600;

    private static boolean selfHealing;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private RestorationSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.restoration";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public int cooldownTicks(int chargeTicks) {
        return selfHealing ? SELF_COOLDOWN_TICKS : COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();
        selfHealing = player.isShiftKeyDown();
        if (selfHealing) {
            heal(player);
            int caster = stand != null ? stand.getId() : player.getId();
            HeavenShow.full(level, HeavenFxPacket.PILLAR,
                    -1, player.getId(), player.position(), PILLAR_HEIGHT, PILLAR_TICKS);
            HeavenShow.full(level, HeavenFxPacket.BEAM,
                    caster, player.getId(), player.position(), 0F, BEAM_TICKS);
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                    30, 0.5, 0.8, 0.5, 0.05);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,
                    1.2F, 1.5F);
            level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6F, 1.6F);
            return true;
        }
        double reach = Heaven.range(REACH);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        HitResult block = level.clip(new ClipContext(eye, eye.add(look.scale(reach)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 end = block.getType() == HitResult.Type.MISS ? eye.add(look.scale(reach)) : block.getLocation();
        EntityHitResult aimed = ProjectileUtil.getEntityHitResult(level, player, eye, end,
                new AABB(eye, end).inflate(1.0), entity -> entity instanceof LivingEntity living && living.isAlive()
                        && !(entity instanceof StandEntity) && entity != player);

        Entity restored;
        boolean revived = false;
        if (aimed != null && aimed.getEntity() instanceof LivingEntity living) {
            heal(living);
            restored = living;
        } else {
            restored = FallenMobs.revivePlayer(level, end, Heaven.range(REVIVE_RADIUS));
            if (restored == null) {
                restored = FallenMobs.revive(level, end, Heaven.range(REVIVE_RADIUS));
            }
            revived = true;
        }
        if (restored == null) {
            return false;
        }
        int caster = stand != null ? stand.getId() : player.getId();
        if (revived) {
            HeavenShow.full(level, HeavenFxPacket.PILLAR,
                    -1, restored.getId(), restored.position(), PILLAR_HEIGHT, PILLAR_TICKS);
            HeavenShow.full(level, HeavenFxPacket.BEAM,
                    caster, -1, restored.position().add(0.0, 1.0, 0.0), 0F, BEAM_TICKS);
            level.playSound(null, restored.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,
                    1.0F, 1.3F);
        } else {
            HeavenShow.full(level, HeavenFxPacket.BEAM,
                    caster, restored.getId(), restored.position(), 0F, BEAM_TICKS);
        }
        if (stand != null) {
            stand.triggerStrikeAt(restored instanceof LivingEntity living ? living : player, "star_finger");
        }
        level.sendParticles(ParticleTypes.HEART, restored.getX(), restored.getY() + restored.getBbHeight() + 0.3,
                restored.getZ(), 6, 0.4, 0.2, 0.4, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, restored.getX(), restored.getY() + restored.getBbHeight() * 0.5,
                restored.getZ(), 20, 0.4, 0.6, 0.4, 0.04);
        level.playSound(null, restored.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS,
                0.7F, 1.4F);
        return true;
    }

    private static void heal(LivingEntity living) {
        if (living instanceof Player player) {
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5F);
            for (ItemStack worn : player.getArmorSlots()) {
                if (worn.isDamageableItem()) {
                    worn.setDamageValue(0);
                }
            }
        }
        List<Holder<MobEffect>> harmful = new ArrayList<>();
        for (MobEffectInstance effect : living.getActiveEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                harmful.add(effect.getEffect());
            }
        }
        harmful.forEach(living::removeEffect);
        living.clearFire();
        living.setHealth(living.getMaxHealth());
    }
}
