package org.gumel.jojoha.ultimate;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.vampire.VampireMove;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AdaptiveFlightMove implements VampireMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "adaptive_flight");
    public static final AdaptiveFlightMove INSTANCE = new AdaptiveFlightMove();

    public static final float DRAIN_PER_SECOND = 1.5F;

    public static final float FLAP_COST = 3F;

    public static final double FLAP_LIFT = 0.6;

    private static final double FLAP_PUSH = 0.25;

    private static final double FLAP_THRUST = 0.55;

    private static final double FLAP_RISE = 0.15;

    private static final double FLAP_TOP_SPEED = 1.8;

    private static final int FLAP_GAP_TICKS = 8;

    private static final int TOGGLE_TICKS = 10;
    private static final int FOLDED_COOLDOWN = 400;
    private static final float OPEN_COST = 10F;

    private static final Map<UUID, Long> LAST_FLAP = new HashMap<>();

    private AdaptiveFlightMove() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int cooldownTicks() {
        return TOGGLE_TICKS;
    }

    @Override
    public int cooldownTicks(JojohaPlayerData data) {
        return data.ultimateWings ? FOLDED_COOLDOWN : TOGGLE_TICKS;
    }

    public static void folded(ServerPlayer player, JojohaPlayerData data) {
        data.setMoveCooldown(ID, player.level().getGameTime(), FOLDED_COOLDOWN);
    }

    @Override
    public float energyCost() {
        return OPEN_COST;
    }

    @Override
    public float energyCost(JojohaPlayerData data) {
        return data.ultimateWings ? 0F : OPEN_COST;
    }

    @Override
    public VampireStage minimumStage() {
        return VampireStage.ULTIMATE_LIFEFORM;
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        data.ultimateWings = !data.ultimateWings;
        player.serverLevel().playSound(null, player.blockPosition(),
                data.ultimateWings ? SoundEvents.PHANTOM_FLAP : SoundEvents.ARMOR_EQUIP_ELYTRA.value(),
                SoundSource.PLAYERS, 0.9F, data.ultimateWings ? 0.8F : 1.2F);
    }

    public static boolean gliding(ServerPlayer player) {
        return WingFlight.canGlide(player);
    }

    public static void flap(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!Fate.ultimate(data) || !data.ultimateWings || !gliding(player)) {
            return;
        }
        long now = player.level().getGameTime();
        Long last = LAST_FLAP.get(player.getUUID());
        if (last != null && now >= last && now - last < FLAP_GAP_TICKS) {
            return;
        }
        float cost = JojohaServerConfig.WING_FLAP_COST.value();
        if (data.specEnergy < cost) {
            return;
        }
        LAST_FLAP.put(player.getUUID(), now);
        data.specEnergy -= cost;

        Vec3 motion = player.getDeltaMovement();
        Vec3 look = player.getLookAngle();
        Vec3 launch;
        if (player.isFallFlying()) {
            launch = motion.add(look.scale(FLAP_THRUST)).add(0.0, FLAP_RISE, 0.0);
            if (launch.length() > FLAP_TOP_SPEED) {
                launch = launch.normalize().scale(FLAP_TOP_SPEED);
            }
        } else {
            Vec3 heading = new Vec3(look.x, 0, look.z);
            heading = heading.lengthSqr() < 1.0E-4 ? Vec3.ZERO : heading.normalize().scale(FLAP_PUSH);
            launch = new Vec3(motion.x + heading.x, Math.max(motion.y, 0.0) * 0.3 + FLAP_LIFT,
                    motion.z + heading.z);
        }

        player.setDeltaMovement(launch);
        player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), launch));
        player.fallDistance = 0F;
        NetworkHandler.broadcastWings(player, true, true);

        ServerLevel level = player.serverLevel();
        if (!player.isFallFlying()) {
            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                    SoundSource.PLAYERS, 0.5F, 1.4F);
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(),
                    6, 0.4, 0.1, 0.4, 0.02);
        }

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static void forget(UUID playerId) {
        LAST_FLAP.remove(playerId);
    }
}
