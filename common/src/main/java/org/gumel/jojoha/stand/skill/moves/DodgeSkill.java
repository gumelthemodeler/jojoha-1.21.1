package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.StatPerkEffects;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.fortune.Trickster;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.Heading;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class DodgeSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "dodge");
    public static final DodgeSkill INSTANCE = new DodgeSkill();

    private static final double DISTANCE = 0.95;
    private static final double LIFT = 0.32;
    private static final int COOLDOWN_TICKS = 45;

    private static final int WAKE_TICKS = 6;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private DodgeSkill() {
    }

    @Override
    public boolean isAnswerToBeingHit() {
        return true;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "move.jojoha.dodge";
    }

    @Override
    public boolean requiresStand() {
        return false;
    }

    @Override
    public TrustTier minimumTrust() {
        return TrustTier.DORMANT;
    }

    @Override
    public boolean movesTheUsersBody() {
        return true;
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
        ServerLevel level = player.serverLevel();

        Vec3 heading = Heading.of(player, data);
        if (heading.lengthSqr() < 1.0E-8) {
            return false;
        }

        player.setDeltaMovement(heading.scale(DISTANCE).add(0, LIFT, 0));
        player.hurtMarked = true;
        StatPerkEffects.dodged(player, data);
        Trickster.tricked(player);

        String pose = MovementFx.poseFor(player, heading, true);
        MovementFx.pose(player, pose);
        MovementFx.play(player,
                pose.endsWith("roll") ? MovementFx.Kind.ROLL
                        : MovementFx.Kind.DASH,
                heading, Impacts.PLAYER, WAKE_TICKS);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.4F, 1.8F);
        return true;
    }
}
