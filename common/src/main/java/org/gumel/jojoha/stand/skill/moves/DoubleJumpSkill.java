package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import org.gumel.jojoha.stand.grapple.HermitGrappleHook;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.Heading;
import org.gumel.jojoha.stand.skill.MoveInput;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class DoubleJumpSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "double_jump");
    public static final DoubleJumpSkill INSTANCE = new DoubleJumpSkill();

    private static final double LIFT = 0.78;

    private static final double CARRY = 1.0;

    private static final double DRIVE = 0.22;

    private static final int COOLDOWN_TICKS = 10;

    private static final double MOST_CARRIED = 1.2;

    private static Vec3 reported;

    private static final int WAKE_TICKS = 6;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private DoubleJumpSkill() {
    }

    public static void report(double x, double z) {
        reported = Double.isFinite(x) && Double.isFinite(z) ? new Vec3(x, 0.0, z) : null;
    }

    public static void forget() {
        reported = null;
    }

    static Vec3 carried(ServerPlayer player) {
        Vec3 travel = reported != null ? reported
                : new Vec3(player.getDeltaMovement().x, 0.0, player.getDeltaMovement().z);
        double speed = travel.length();
        return speed > MOST_CARRIED ? travel.scale(MOST_CARRIED / speed) : travel;
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
        return "move.jojoha.double_jump";
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
    public boolean passiveOnly() {
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
        if (player.onGround()) {
            return false;
        }
        if (HermitGrappleHook.findFor(player) != null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        Vec3 travel = carried(player);
        Vec3 along;
        Vec3 held = MoveInput.heading(player);
        if (held != null) {
            along = held;
            travel = held.scale(travel.length());
        } else {
            along = travel.length() > Heading.MOVING_THRESHOLD ? travel.normalize()
                    : Heading.of(player, data);
        }
        Vec3 push = along.scale(DRIVE);
        player.setDeltaMovement(travel.x * CARRY + push.x, LIFT, travel.z * CARRY + push.z);
        player.hurtMarked = true;

        MovementFx.pose(player, MovementFx.DOUBLE_JUMP);
        MovementFx.play(player, MovementFx.Kind.DOUBLE_JUMP,
                player.getDeltaMovement(), Impacts.PLAYER, WAKE_TICKS);
        level.playSound(null, player.blockPosition(),
                SoundEvents.WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 0.45F, 1.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP,
                SoundSource.PLAYERS, 0.5F, 1.5F);
        return true;
    }
}
