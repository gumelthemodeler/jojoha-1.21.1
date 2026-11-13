package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.BarrageBank;
import org.gumel.jojoha.combat.MovementCharges;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.combat.fx.StandFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.Heading;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class StandDashSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_dash");
    public static final StandDashSkill INSTANCE = new StandDashSkill();

    private static final int COOLDOWN_TICKS = 40;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private static final double DASH_POWER = 1.7;

    public static final int ESCAPE_LOCK_TICKS = 60;

    private StandDashSkill() {
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
        return "skill.jojoha.stand_dash";
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
        boolean escaping = BarrageBank.pinned(player);
        boolean fired = dash(player, data);
        if (fired) {
            trail(player, data);
            if (escaping) {
                escape(player, data);
            }
        }
        return fired;
    }

    private static void escape(ServerPlayer player, JojohaPlayerData data) {
        BarrageBank.slip(player);
        MovementCharges.lock(data, MovementCharges.Kind.DASH, player.level().getGameTime(), ESCAPE_LOCK_TICKS);
        ServerLevel level = player.serverLevel();
        StandFx.burst(level, player.position().add(0.0, 1.0, 0.0), Impacts.colourOf(player), 16, 0.45);
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(),
                SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    private boolean dash(ServerPlayer player, JojohaPlayerData data) {
        Vec3 heading = dashHeading(player, data);
        if (heading.lengthSqr() < 1.0E-4) {
            return false;
        }

        Vec3 launch = heading.add(0, 0.15, 0);
        player.setDeltaMovement(launch);
        player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), launch));
        player.fallDistance = 0F;

        player.serverLevel().playSound(null, player.blockPosition(), ModSounds.STAND_JUMP.get(),
                SoundSource.PLAYERS, 0.9F, 1.2F);

        takeOff(player, heading);
        return true;
    }

    private static void takeOff(ServerPlayer player, Vec3 heading) {
        ServerLevel level = player.serverLevel();
        int colour = Impacts.colourOf(player);
        MovementFx.pose(player, MovementFx.poseFor(player, heading, false));
        MovementFx.play(player, MovementFx.Kind.DASH,
                heading, colour, WAKE_TICKS);
        level.playSound(null, player.blockPosition(),
                SoundEvents.WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 0.5F, 1.4F);
    }

    private static final int WAKE_TICKS = 8;

    private static Vec3 dashHeading(ServerPlayer player, JojohaPlayerData data) {
        return Heading.of(player, data).scale(DASH_POWER);
    }

    private static final int AFTERIMAGE_TICKS = 14;

    private static void trail(ServerPlayer player, JojohaPlayerData data) {
        NetworkHandler.broadcastAfterimages(player,
                StandShadowForm.auraColourFor(data.stand, player.getUUID()),
                AFTERIMAGE_TICKS);
    }
}
