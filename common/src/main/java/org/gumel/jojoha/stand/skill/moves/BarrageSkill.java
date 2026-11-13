package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.data.StatEffects;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.chariot.Chariot;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class BarrageSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "barrage");
    public static final BarrageSkill INSTANCE = new BarrageSkill();

    private static final int COOLDOWN_TICKS = 120;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private static final double BODY_REACH = 3.5;
    private static final int BODY_HITS = 5;
    private static final int BODY_HIT_INTERVAL = 3;
    private static final float BODY_HIT_DAMAGE = 1.0F;

    private BarrageSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.barrage";
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
        return false;
    }

    @Override
    public boolean isSustained() {
        return true;
    }

    @Override
    public boolean isSustainActive(Player player) {
        StandEntity stand = StandEntity.findFor(player);
        return stand != null && stand.isHeldBarrageRunning();
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        StandEntity stand = Chariot.stand(player);
        return stand == null || stand.isHeldBarrageRunning() ? null
                : Chariot.rapierless(player);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (stand != null) {
            if (stand.isHeldBarrageRunning()) {
                stand.endHeldBarrage();
            } else {
                stand.beginHeldBarrage();
            }
            return true;
        }

        NetworkHandler.sendFirstPersonPose(player, "barrage",
                BODY_HITS * BODY_HIT_INTERVAL);
        MovementFx.pose(player, MovementFx.BARRAGE);

        float damage = StandTuning.damage("barrage_body_hit", BODY_HIT_DAMAGE)
                * StatEffects.bodyDamageScale(data.strength);
        PlayerFlurry.begin(player, BODY_HITS, BODY_HIT_INTERVAL, damage, BODY_REACH, 1F, null,
                Impacts.PLAYER, false, DamageScale.Kind.STAND);
        return true;
    }
}
