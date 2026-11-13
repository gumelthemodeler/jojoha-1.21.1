package org.gumel.jojoha.hamon.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.accessory.Relics;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.BubbleFlight;
import org.gumel.jojoha.hamon.BubbleVolley;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.skill.StandBeat;

public final class BubbleLauncherMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "bubble_launcher");
    public static final BubbleLauncherMove INSTANCE = new BubbleLauncherMove();

    private static final float DAMAGE_PER_BUBBLE = 0.85F;

    public static final int BUBBLES = 40;
    private static final double SPEED = 1.0;

    public static final float ARC_DEGREES = 160F;
    private static final float THICKNESS_DEGREES = 8F;
    private static final float TILT_DEGREES = 6F;

    private static final int SEED_RANGE = 1 << 22;

    private static final int COOLDOWN_TICKS = 160;
    private static final float ENERGY_COST = 30F;

    private BubbleLauncherMove() {
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
    public boolean activate(ServerPlayer player, JojohaPlayerData data) {
        if (!BubbleVolley.gloved(player)) {
            return false;
        }
        NetworkHandler.sendFirstPersonPose(player,
                MovementFx.BUBBLE_LAUNCHER);
        MovementFx.pose(player, MovementFx.BUBBLE_LAUNCHER);
        ServerLevel level = player.serverLevel();
        int seed = level.getRandom().nextInt(SEED_RANGE);
        Vec3 eye = player.getEyePosition();
        level.sendParticles(ModRegistries.BUBBLE_SHEET.get(), eye.x, eye.y, eye.z, 0,
                player.getId(), seed, 0.0, 1.0);
        level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                SoundSource.PLAYERS, 1.0F, 0.7F);
        StandBeat.at(MovementFx.BUBBLE_LAUNCH_TICKS, player, () -> launch(player, seed));
        return true;
    }

    private static void launch(ServerPlayer player, int seed) {
        ServerLevel level = player.serverLevel();

        BubbleVolley.launchArc(player, BubbleFlight.Kind.SHOT, BUBBLES,
                SPEED * Relics.bubbleSpeed(player), ARC_DEGREES,
                THICKNESS_DEGREES, TILT_DEGREES, DAMAGE_PER_BUBBLE, true, seed);

        level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE,
                SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE,
                SoundSource.PLAYERS, 0.8F, 1.3F);
        HamonFx.sound(level, player, 0.6F, 1.2F);
    }
}
