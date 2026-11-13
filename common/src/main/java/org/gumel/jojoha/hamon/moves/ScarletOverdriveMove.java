package org.gumel.jojoha.hamon.moves;

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
import org.gumel.jojoha.combat.Burning;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonCombat;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.StandBeat;

public final class ScarletOverdriveMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "scarlet_overdrive");
    public static final ScarletOverdriveMove INSTANCE = new ScarletOverdriveMove();

    public static final float DAMAGE = 6.0F;
    public static final float UNDEAD_DAMAGE = 9.0F;
    public static final int BURN_SECONDS = 4;
    public static final double REACH = 3.5;
    private static final double STEP = 0.25;
    private static final double HIT_RADIUS = 0.5;

    private static final int COOLDOWN_TICKS = 120;
    private static final float ENERGY_COST = 18F;

    private ScarletOverdriveMove() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
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
    public boolean activate(ServerPlayer player, JojohaPlayerData data) {
        MovementFx.pose(player, MovementFx.GUT_PUNCH);
        NetworkHandler.sendFirstPersonPose(player, "palm");
        Vec3 hand = HamonFx.mainHand(player);
        player.serverLevel().sendParticles(ParticleTypes.FLAME, hand.x, hand.y, hand.z, 6, 0.08, 0.08, 0.08, 0.01);
        StandBeat.at(MovementFx.GUT_PUNCH_CONTACT_TICKS, player,
                () -> punch(player));
        return true;
    }

    private static void punch(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        LivingEntity struck = null;
        Vec3 at = eye.add(look.scale(REACH));
        for (double travelled = 0.5; travelled <= REACH && struck == null; travelled += STEP) {
            Vec3 point = eye.add(look.scale(travelled));
            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(point, point).inflate(HIT_RADIUS),
                    entity -> entity != player && entity.isAlive()
                            && !(entity instanceof StandEntity))) {
                struck = candidate;
                at = point;
                break;
            }
        }

        level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 14, 0.25, 0.25, 0.25, 0.04);
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 6, 0.2, 0.2, 0.2, 0.02);
        HamonFx.spark(level, at.x, at.y, at.z, 6, 0.2, 0.04);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.2F);

        if (struck == null) {
            return;
        }
        float damage = HamonCombat.isUndead(struck) ? UNDEAD_DAMAGE : DAMAGE;
        struck.hurt(level.damageSources().playerAttack(player),
                DamageScale.hamon(player, struck, HamonCombat.scaled(player, damage)));
        if (HamonCombat.isUndead(struck)) {
            Burning.apply(struck);
        } else {
            struck.igniteForSeconds(BURN_SECONDS);
        }
        struck.setDeltaMovement(struck.getDeltaMovement().add(look.x * 0.4, 0.15, look.z * 0.4));
        struck.hurtMarked = true;
        Impacts.landed(level, struck, look,
                ImpactSpec.HEAVY, Impacts.HAMON);
        HamonFx.sound(level, player, 0.8F, 0.9F);
    }
}
