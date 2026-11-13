package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandAim;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.hierophant.Possession;
import org.gumel.jojoha.stand.hierophant.PossessionMotion;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.PilotSystem;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class BodyPossessionSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "body_possession");
    public static final BodyPossessionSkill INSTANCE = new BodyPossessionSkill();

    private static final double REACH = 16.0;

    public static final int COOLDOWN_TICKS = 600;
    private static final float ENERGY_COST = EnergyWeight.HEAVY.cost();

    private BodyPossessionSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.body_possession";
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public float energyCost() {
        return 0F;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (Possession.possessing(player)) {
            Possession.end(player, "message.jojoha.possession.released");
            return true;
        }
        if (stand == null || PossessionMotion.diving(player)) {
            return false;
        }
        LivingEntity body = StandAim.lookTarget(player, REACH);
        if (body == null) {
            return false;
        }
        String refused = Possession.canTake(player, body);
        if (refused != null) {
            player.displayClientMessage(Component.translatable(refused), true);
            return true;
        }
        if (data.standEnergy < ENERGY_COST) {
            player.displayClientMessage(Component.translatable("message.jojoha.skill.no_energy"), true);
            return true;
        }
        data.standEnergy -= ENERGY_COST;
        if (data.standPiloting) {
            PilotSystem.stop(player, data);
        }
        PossessionMotion.enter(player, body, stand);
        return true;
    }
}
