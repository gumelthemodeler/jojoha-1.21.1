package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.passive.SecondSightPassive;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class ClairvoyanceSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "clairvoyance");

    public static final ClairvoyanceSkill INSTANCE = new ClairvoyanceSkill();

    private static final int COOLDOWN_TICKS = 600;

    private static final int DURATION_TICKS = 240;

    private static final float ENERGY_COST = EnergyWeight.HEAVY.cost();

    private ClairvoyanceSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.clairvoyance";
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
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        NetworkHandler.sendClairvoyance(player,
                SecondSightPassive.durationFor(data, DURATION_TICKS));
        aura(player.serverLevel(), player);
        return true;
    }

    private static void aura(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ModRegistries.STAND_AURA.get(), player.getX(),
                player.getY() + 1.2, player.getZ(), 40, 0.5, 0.7, 0.5, 0.03);
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.6,
                player.getZ(), 60, 0.7, 0.6, 0.7, 0.9);

        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 0.8F, 0.7F);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.35F, 1.6F);
    }
}
