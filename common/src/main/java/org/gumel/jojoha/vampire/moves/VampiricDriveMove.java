package org.gumel.jojoha.vampire.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.vampire.VampireFx;
import org.gumel.jojoha.vampire.VampireMove;
import org.gumel.jojoha.vampire.VampirePoses;
import org.gumel.jojoha.vampire.VampireState;

public final class VampiricDriveMove implements VampireMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "vampiric_drive");
    public static final VampiricDriveMove INSTANCE = new VampiricDriveMove();

    private static final int CHARGE_TICKS = 120;

    public static final float BONUS_DAMAGE = 8.0F;

    private static final int COOLDOWN_TICKS = 160;
    private static final float ENERGY_COST = 25F;

    private VampiricDriveMove() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
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
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        MovementFx.pose(player, VampirePoses.VAMPIRIC_DRIVE);
        StandBeat.at(VampirePoses.VAMPIRIC_DRIVE_CHARGE_TICKS, player, () -> charge(player));
    }

    private static void charge(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        VampireState.chargeDrive(player, CHARGE_TICKS);

        for (int i = 0; i < 3; i++) {
            VampireFx.ring(level, player.getX(), player.getY() + 0.9 + i * 0.35, player.getZ(),
                    2.0 - i * 0.4, 2);
        }
        VampireFx.blood(level, player.getX(), player.getY() + 1.1, player.getZ(), 30, 0.5, 0.14);

        level.sendParticles(ModRegistries.BLOOD_MOTE.get(),
                player.getX(), player.getY() + 1.2, player.getZ(), 30, 0.4, 0.5, 0.4, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR,
                SoundSource.PLAYERS, 0.5F, 1.6F);
    }
}
