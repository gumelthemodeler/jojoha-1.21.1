package org.gumel.jojoha.hamon.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonCombat;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.skill.moves.PlayerFlurry;

public final class SunlightYellowOverdriveMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sunlight_yellow_overdrive");
    public static final SunlightYellowOverdriveMove INSTANCE = new SunlightYellowOverdriveMove();

    private static final int HITS = 7;
    private static final int HIT_INTERVAL = 3;

    private static final float HIT_DAMAGE = 1.8F;

    private static final float UNDEAD_MULTIPLIER = 4.0F;

    private static final double REACH = 4.5;

    private static final double FIST_RING = 0.75;

    private static final double FIST_SHORT = 0.55;

    private static final double FIST_ASIDE = 0.3;

    private static final double CHEST = 0.62;

    public static final int LOCK_TICKS = 30;

    private static final int COOLDOWN_TICKS = 500;
    private static final float ENERGY_COST = 60F;

    private SunlightYellowOverdriveMove() {
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
        ServerLevel level = player.serverLevel();

        NetworkHandler.sendFirstPersonPose(player, "barrage",
                HITS * HIT_INTERVAL);
        MovementFx.pose(player, MovementFx.BARRAGE);

        float perHit = HamonCombat.scaled(player, HIT_DAMAGE);

        int[] blow = {0};

        PlayerFlurry.begin(player, HITS, HIT_INTERVAL, perHit, REACH, UNDEAD_MULTIPLIER,
                new PlayerFlurry.HitEffect() {
                    @Override
                    public void at(ServerLevel hitLevel, LivingEntity victim) {
                        Vec3 fist = fistAt(player, victim, blow[0]++ % 2 == 0);

                        HamonFx.ring(hitLevel, fist.x, fist.y, fist.z, FIST_RING, 1);
                        HamonFx.fineSpark(hitLevel, fist.x, fist.y, fist.z, 3, 0.14, 0.03);

                        HamonFx.ripple(hitLevel, victim.getX(), victim.getY() + 1.0, victim.getZ(), 2);
                        HamonFx.spark(hitLevel, victim.getX(), victim.getY() + 1.0, victim.getZ(),
                                4, 0.3, 0.05);
                    }

                    @Override
                    public void last(ServerLevel hitLevel, LivingEntity victim) {
                        Stun.lock(victim,
                                JojohaServerConfig.SYO_LOCK_TICKS.whole());
                    }
                },
                Impacts.HAMON, true, DamageScale.Kind.HAMON);

        Vec3 eye = player.getEyePosition();
        HamonFx.ring(level, eye.x, eye.y, eye.z, 2.6, 2);
        HamonFx.ripple(level, eye.x, eye.y, eye.z, 6);
        HamonFx.mote(level, eye.x, eye.y, eye.z, 14, 0.7);

        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, player.blockPosition(), ModSounds.HAMON_CRASH.get(),
                SoundSource.PLAYERS, 0.6F, 1.6F);
        HamonFx.sound(level, player, 1.0F, 0.8F);

        return true;
    }

    private static Vec3 fistAt(ServerPlayer player, LivingEntity victim,
                               boolean rightFist) {
        Vec3 chest = victim.position().add(0.0, victim.getBbHeight() * CHEST, 0.0);
        Vec3 from = player.position().add(0.0, player.getBbHeight() * CHEST, 0.0);

        Vec3 along = chest.subtract(from);
        if (along.lengthSqr() < 1.0E-6) {
            return chest;
        }
        along = along.normalize();

        Vec3 across = new Vec3(-along.z, 0.0, along.x);
        if (across.lengthSqr() < 1.0E-6) {
            across = Vec3.ZERO;
        } else {
            across = across.normalize().scale(rightFist ? FIST_ASIDE : -FIST_ASIDE);
        }

        return chest.subtract(along.scale(FIST_SHORT)).add(across);
    }
}
