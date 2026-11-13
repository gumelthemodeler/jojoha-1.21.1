package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class StandLeapSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_leap");
    public static final StandLeapSkill INSTANCE = new StandLeapSkill();

    private static final int COOLDOWN_TICKS = 60;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private static final double HORIZONTAL_POWER = 1.35;
    private static final double MINIMUM_LIFT = 0.62;
    private static final double AIM_LIFT_SHARE = 0.45;

    private static final int GRAB_WINDOW_TICKS = 30;

    private static final int LEAP_WAKE_TICKS = 8;

    private StandLeapSkill() {
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
        return "skill.jojoha.stand_leap";
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
        Vec3 heading = new Vec3(look.x, 0, look.z);
        if (heading.lengthSqr() < 1.0E-4) {
            heading = Vec3.ZERO;
        } else {
            heading = heading.normalize().scale(HORIZONTAL_POWER);
        }

        double lift = Math.max(MINIMUM_LIFT, look.y * AIM_LIFT_SHARE + MINIMUM_LIFT);
        Vec3 launch = new Vec3(heading.x, lift, heading.z);

        player.setDeltaMovement(launch);
        player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), launch));

        player.setKnownMovement(launch);
        player.fallDistance = 0F;

        player.serverLevel().playSound(null, player.blockPosition(), ModSounds.STAND_JUMP.get(),
                SoundSource.PLAYERS, 1.5F, 0.95F);
        stand.triggerLeapPose();
        trail(player, data);
        MovementFx.pose(player, MovementFx.LEAP);
        MovementFx.play(player, MovementFx.Kind.LEAP,
                launch, Impacts.colourOf(player), LEAP_WAKE_TICKS);

        data.standLeapGrabTicks = GRAB_WINDOW_TICKS;
        return true;
    }
    private static final int AFTERIMAGE_TICKS = 16;

    private static void trail(ServerPlayer player, JojohaPlayerData data) {
        NetworkHandler.broadcastAfterimages(player,
                StandShadowForm.auraColourFor(data.stand, player.getUUID()),
                AFTERIMAGE_TICKS);
    }
}
