package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.MoveInput;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class BurstSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "burst");
    public static final BurstSkill INSTANCE = new BurstSkill();

    private static final double POWER = 1.2;

    private static final double LIFT_CAP = 0.62;

    private static final double FLOOR_LIFT = 0.12;

    private static final int COOLDOWN_TICKS = 60;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private static final int WAKE_TICKS = 8;

    private BurstSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "move.jojoha.burst";
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
    public boolean isAnswerToBeingHit() {
        return true;
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
        Vec3 look = player.getLookAngle();
        Vec3 aim = look.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : look.normalize();
        Vec3 held = MoveInput.heading(player);
        if (held != null && held.lengthSqr() > 1.0E-8) {
            double flat = Math.sqrt(Math.max(0.0, 1.0 - aim.y * aim.y));
            aim = new Vec3(held.x * flat, aim.y, held.z * flat);
        }

        double lift = aim.y * POWER;
        lift = lift > 0 ? Math.min(lift, LIFT_CAP) + FLOOR_LIFT : lift;
        Vec3 launch = new Vec3(aim.x * POWER, lift, aim.z * POWER);

        player.setDeltaMovement(launch);
        player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), launch));
        player.setKnownMovement(launch);
        player.fallDistance = 0F;

        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 0.7F, 1.25F);

        MovementFx.pose(player, MovementFx.LEAP);
        MovementFx.play(player, MovementFx.Kind.LEAP,
                launch, Impacts.PLAYER, WAKE_TICKS);
        return true;
    }
}
