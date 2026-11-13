package org.gumel.jojoha.ultimate;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.pillar.PillarPoses;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.vampire.VampireMove;

public final class UltimateStrengthMove implements VampireMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ultimate_strength");
    public static final UltimateStrengthMove INSTANCE = new UltimateStrengthMove();

    public static final int LEVEL = 2;

    public static final int SECONDS = 10;

    public static final int BASE_LEVEL = 1;
    public static final int GROWN_LEVEL = 2;
    public static final int HALFWAY_RANK = 3;

    private static final int COOLDOWN_TICKS = 900;
    private static final float ENERGY_COST = 30F;

    private UltimateStrengthMove() {
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
    public VampireStage minimumStage() {
        return VampireStage.ULTIMATE_LIFEFORM;
    }

    public static int levelFor(JojohaPlayerData data) {
        int most = Math.max(1, JojohaServerConfig.ULTIMATE_STRENGTH_LEVEL.whole());
        int rank = AdaptationLibrary.rank(data);
        int grown = rank >= AdaptationLibrary.TOP_RANK ? most : rank >= HALFWAY_RANK ? GROWN_LEVEL : BASE_LEVEL;
        return Math.min(most, grown);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        MovementFx.pose(player, PillarPoses.ULTIMATE_STRENGTH);
        StandBeat.at(PillarPoses.ULTIMATE_STRENGTH_FLEX_TICKS, player, () -> flex(player, data));
    }

    private static void flex(ServerPlayer player, JojohaPlayerData data) {
        int level = levelFor(data);
        int ticks = Math.max(1, JojohaServerConfig.ULTIMATE_STRENGTH_SECONDS.whole()) * 20;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, level - 1, false, true, true));

        ServerLevel world = player.serverLevel();
        world.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(),
                30, 0.4, 0.6, 0.4, 0.3);
        world.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(),
                SoundSource.PLAYERS, 1.0F, 0.6F);
        world.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 0.8F, 0.5F);
    }
}
