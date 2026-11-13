package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.heaven.Servitude;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class InfiniteServitudeSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "infinite_servitude");
    public static final InfiniteServitudeSkill INSTANCE = new InfiniteServitudeSkill();

    private static final int COOLDOWN_TICKS = 1800;
    private static final float ENERGY_COST = EnergyWeight.HEAVY.cost();

    private InfiniteServitudeSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.infinite_servitude";
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
        int bound = Servitude.bind(player);
        if (bound == 0) {
            return false;
        }
        if (stand != null) {
            stand.triggerStrikeForward("grab");
        }
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON,
                SoundSource.PLAYERS, 1.0F, 0.7F);
        player.displayClientMessage(Component.translatable("message.jojoha.heaven.servitude", bound)
                .withStyle(ChatFormatting.GOLD), true);
        return true;
    }
}
