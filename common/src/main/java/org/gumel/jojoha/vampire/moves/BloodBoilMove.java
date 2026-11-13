package org.gumel.jojoha.vampire.moves;

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
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.pillar.PillarFx;
import org.gumel.jojoha.pillar.PillarPoses;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.vampire.VampireFx;
import org.gumel.jojoha.vampire.VampireMove;

public final class BloodBoilMove implements VampireMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "blood_boil");
    public static final BloodBoilMove INSTANCE = new BloodBoilMove();

    private static final float DAMAGE = 9.0F;

    private static final float HAMON_BONUS = 7.0F;

    private static final double RANGE = 7.0;

    private static final double SPREAD = 0.55;

    private static final int BURN_SECONDS = 6;

    private static final int COOLDOWN_TICKS = 200;
    private static final float ENERGY_COST = 35F;

    private BloodBoilMove() {
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
    public boolean pillar() {
        return true;
    }

    @Override
    public VampireStage minimumStage() {
        return VampireStage.PILLAR_MAN;
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        MovementFx.pose(player, PillarPoses.BLOOD_BOIL);
        StandBeat.at(PillarPoses.BLOOD_BOIL_RELEASE_TICKS, player,
                () -> boil(player));
    }

    private static void boil(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 from = player.getEyePosition();

        AABB probe = player.getBoundingBox().inflate(RANGE);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, probe,
                entity -> entity != player && entity.isAlive())) {

            Vec3 toward = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(from);
            if (toward.lengthSqr() < 1.0E-6 || toward.normalize().dot(look) < SPREAD) {
                continue;
            }

            float damage = DAMAGE;
            if (target instanceof ServerPlayer struck
                    && Fate.breathesHamon(PlayerDataAccess.get(struck))) {
                damage += HAMON_BONUS;
            }

            target.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, target, damage));
            target.igniteForSeconds(BURN_SECONDS);
            Impacts.landed(level, target, toward, ImpactSpec.MEDIUM.silent(), Impacts.BLOOD);
        }

        for (double along = 1.0; along <= RANGE; along += 1.0) {
            Vec3 at = from.add(look.scale(along));
            double spread = 0.25 + 0.45 * (along / RANGE);
            VampireFx.blood(level, at.x, at.y, at.z, 8, spread, 0.05);
            VampireFx.ring(level, at.x, at.y, at.z, 0.5 + along * 0.18, along > RANGE * 0.6 ? 2 : 1);
            PillarFx.spray(level, PillarFx.bubble(), at, 4, spread, 0.03);
            PillarFx.spray(level, PillarFx.steam(), at, 2, spread, 0.02);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 1.0F, 0.5F);
    }
}
