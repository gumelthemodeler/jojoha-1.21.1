package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class GrabSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "grab");
    public static final GrabSkill INSTANCE = new GrabSkill();

    private static final int COOLDOWN_TICKS = 50;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private GrabSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.grab";
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
    public boolean requiresStand() {
        return true;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (stand == null || stand.isHolding()) {
            return false;
        }

        LivingEntity target = SkillTargeting.lookTarget(player);
        if (target == null) {
            Jojoha.LOGGER.info("Grab by {} found nothing to take hold of", player.getGameProfile().getName());
            stand.triggerStrikeForward("grab");
            Whiff.stand(player);
            return true;
        }

        stand.triggerGrabAt(target);
        StandBeat.after(StandEntity.STRIKE_CONTACT_TICKS, player, target, GrabSkill::seize);
        return true;
    }

    private static void seize(ServerPlayer player, LivingEntity target) {
        StandEntity stand = StandEntity.findFor(player);
        if (stand == null || stand.isHolding() || !SkillTargeting.stillInReach(player, target, SkillTargeting.REACH)) {
            Whiff.stand(player);
            return;
        }
        if (!stand.seize(target)) {
            Jojoha.LOGGER.info("Grab by {} was refused by {} (riding {}, alive {}, Stand carrying {})",
                    player.getGameProfile().getName(), target, target.getVehicle(), target.isAlive(),
                    stand.getPassengers());
            Whiff.stand(player);
            return;
        }

        Impacts.landed(player.serverLevel(), target,
                player.position().subtract(target.position()),
                ImpactSpec.MEDIUM,
                Impacts.colourOf(player));
    }
}
