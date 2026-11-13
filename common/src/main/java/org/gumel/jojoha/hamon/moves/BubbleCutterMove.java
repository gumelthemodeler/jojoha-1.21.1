package org.gumel.jojoha.hamon.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.accessory.Accessories;
import org.gumel.jojoha.accessory.Relics;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.hamon.BubbleFlight;
import org.gumel.jojoha.hamon.BubbleVolley;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMastery;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.hamon.HamonState;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModItems;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class BubbleCutterMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "bubble_cutter");
    public static final BubbleCutterMove INSTANCE = new BubbleCutterMove();

    private static final float DAMAGE_PER_BUBBLE = 0.8F;

    public static final int FIRE_EVERY = 2;

    public static final float SHOT_COST = 2.0F;

    private static final double SPEED = 1.1;
    private static final float CONE_DEGREES = 4F;

    private static final int MUZZLE = 2;

    private static final int SYNC_EVERY = 5;

    private static final int COOLDOWN_TICKS = 20;
    private static final float ENERGY_COST = 8F;

    private BubbleCutterMove() {
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
    public boolean isSustained() {
        return true;
    }

    @Override
    public boolean isSustainActive(ServerPlayer player) {
        return HamonState.isFiring(player);
    }

    @Override
    public void release(ServerPlayer player, JojohaPlayerData data) {
        if (HamonState.isFiring(player)) {
            LET_GO.add(player.getUUID());
        }
    }

    private static final Set<UUID> LET_GO = new HashSet<>();

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data) {
        if (!BubbleVolley.gloved(player)) {
            return false;
        }
        HamonState.startFiring(player);
        LET_GO.remove(player.getUUID());
        NetworkHandler.sendFirstPersonPose(player, MovementFx.BUBBLE_CUTTER);
        MovementFx.hold(player, MovementFx.BUBBLE_CUTTER);
        HamonFx.sound(player.serverLevel(), player, 0.5F, 1.4F);
        return true;
    }

    public static void tick(ServerPlayer player, JojohaPlayerData data) {
        int held = HamonState.advanceFiring(player);
        if (held < 0) {
            return;
        }
        if (!Accessories.wearing(player,
                ModItems.BUBBLE_GLOVES.get())) {
            stop(player);
            PlayerDataAccess.sync(player);
            return;
        }

        if (held >= MovementFx.BUBBLE_CUTTER_HOLD_TICKS && LET_GO.contains(player.getUUID())) {
            stop(player);
            PlayerDataAccess.sync(player);
            return;
        }

        int firing = held - MovementFx.BUBBLE_CUTTER_CONTACT_TICKS;
        if (firing >= 0 && firing % FIRE_EVERY == 0) {
            if (data.specEnergy < SHOT_COST) {
                stop(player);
                PlayerDataAccess.sync(player);
                return;
            }
            data.specEnergy -= SHOT_COST;
            HamonMastery.spent(player, data, SHOT_COST);
            PlayerDataAccess.set(player, data);
            shoot(player);
        }

        if (held % SYNC_EVERY == 0) {
            PlayerDataAccess.sync(player);
        }
    }

    private static void shoot(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BubbleVolley.launch(player, BubbleFlight.Kind.DRIFT, 1 + Relics.extraBubbles(player),
                SPEED * Relics.bubbleSpeed(player), CONE_DEGREES,
                DAMAGE_PER_BUBBLE, true, MUZZLE, true);
        level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                SoundSource.PLAYERS, 0.45F, 1.4F + level.getRandom().nextFloat() * 0.4F);
    }

    private static void stop(ServerPlayer player) {
        HamonState.stopFiring(player);
        LET_GO.remove(player.getUUID());
        NetworkHandler.releaseFirstPersonPose(player, MovementFx.BUBBLE_CUTTER);
        MovementFx.pose(player, MovementFx.BUBBLE_CUTTER);
    }
}
