package org.gumel.jojoha.stand.skill;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.FlairCuePacket;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.speech.Speech;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.skill.moves.TimeStopSkill;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class TimeStopCharging {
    private static final ResourceLocation DRAW_SPEED =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "time_stop_charge");

    private static final double DRAW_SLOW = -0.8;

    private static final int LATE_TICKS = 40;

    private static final int SLACK_TICKS = 3;

    private static final float CYCLE_PITCH = 0.04F;

    private static final Map<UUID, Charge> CHARGING = new HashMap<>();

    private static final Map<UUID, Charge> RELEASED = new HashMap<>();

    private static final class Charge {
        private final ServerPlayer player;
        private final long began;
        private final int max;
        private final int voiceAt;
        private int poseAt = -1;
        private boolean spoke;
        private boolean posed;
        private boolean looping;
        private int cycles;
        private int userPoseAt = -1;
        private String userClip;
        private boolean userPosed;
        private boolean userHeld;

        private Charge(ServerPlayer player, long began, int max, int voiceAt) {
            this.player = player;
            this.began = began;
            this.max = max;
            this.voiceAt = voiceAt;
        }
    }

    private TimeStopCharging() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(TimeStopCharging::tick);
    }

    public static boolean charging(ServerPlayer player) {
        return CHARGING.containsKey(player.getUUID());
    }

    public static void heard(ServerPlayer player, JojohaPlayerData data, StandSkill skill, int signal) {
        long now = player.level().getGameTime();
        if (signal == StandSkills.CHARGE_BEGIN) {
            if (!skill.isUnlocked(data) || data.isMoveOnCooldown(skill.id(), now) || skill.isRunning(data)) {
                return;
            }
            begin(player, data, now);
            return;
        }
        Charge charge = end(player);
        if (charge == null) {
            return;
        }
        hush(charge, data);
        if (signal == StandSkills.CHARGE_BROKEN) {
            data.setMoveCooldown(skill.id(), now, TimeStopSkill.BROKEN_COOLDOWN_TICKS);
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            Stun.stagger(player);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS, 1.0F, 0.6F);
            Speech.timeStopBroken(player);
        }
    }

    public static int settle(ServerPlayer player, int held) {
        Charge charge = CHARGING.get(player.getUUID());
        if (charge == null) {
            return 0;
        }
        end(player);
        long elapsed = player.level().getGameTime() - charge.began;
        int settled = (int) Math.min(held, elapsed + SLACK_TICKS);
        RELEASED.put(player.getUUID(), charge);
        return settled;
    }

    private static void begin(ServerPlayer player, JojohaPlayerData data, long now) {
        int max = TimeStopLine.chargeTicks(data);
        int lead = TimeStopLine.leadTicks(data);
        int freezeAt = max - 1;
        Charge charge = new Charge(player, now, max, lead > 0 ? Math.max(0, freezeAt - lead) : -1);
        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand != null) {
            charge.poseAt = Math.max(0, freezeAt - stand.chooseTimeStopPose());
            charge.looping = stand.startChargeLoop();
        }
        StandType type = data.stand.isPresent() ? StandTypes.byIdOrDefault(data.stand.standId()) : null;
        charge.userClip = StandTypes.userTimeStopAnimation(type);
        if (charge.userClip != null) {
            charge.userPoseAt = Math.max(0, freezeAt - StandTypes.USER_TIME_STOP_KEY_TICKS);
        }
        String hold = StandTypes.userChargeAnimation(type);
        if (hold != null) {
            MovementFx.hold(player, hold);
            charge.userHeld = true;
        }
        CHARGING.put(player.getUUID(), charge);
        advance(charge, data, 0);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(DRAW_SPEED, DRAW_SLOW,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        NetworkHandler.broadcastFlair(player, FlairCuePacket.TIME_CHARGE, max);
    }

    private static void advance(Charge charge, JojohaPlayerData data, long elapsed) {
        if (charge.looping && !charge.posed && elapsed >= Math.round(charge.cycles * TimeStopLine.CHARGE_CYCLE_TICKS)
                && (charge.poseAt < 0 || elapsed < charge.poseAt)) {
            StandEntity stand = StandSummonHandler.findStand(charge.player, data);
            if (stand != null) {
                charge.player.serverLevel().playSound(null, stand.getX(), stand.getY(), stand.getZ(),
                        ModSounds.TW_CHARGE.get(), SoundSource.PLAYERS, 1.0F, 1.0F + charge.cycles * CYCLE_PITCH);
            }
            charge.cycles++;
        }
        if (!charge.spoke && charge.voiceAt >= 0 && elapsed >= charge.voiceAt) {
            charge.spoke = true;
            TimeStopLine.speak(charge.player, data);
        }
        if (!charge.userPosed && charge.userClip != null && elapsed >= charge.userPoseAt) {
            charge.userPosed = true;
            MovementFx.pose(charge.player, charge.userClip);
        }
        if (!charge.posed && charge.poseAt >= 0 && elapsed >= charge.poseAt) {
            StandEntity stand = StandSummonHandler.findStand(charge.player, data);
            if (stand != null) {
                charge.posed = true;
                stand.triggerTimeStopPose();
            }
        }
    }

    public static void struck(ServerPlayer player, JojohaPlayerData data) {
        Charge charge = RELEASED.get(player.getUUID());
        if (charge != null && (charge.userPosed || charge.userClip == null)) {
            return;
        }
        String clip = charge != null ? charge.userClip
                : StandTypes.userTimeStopAnimation(data.stand.isPresent() ? StandTypes.byIdOrDefault(data.stand.standId()) : null);
        if (clip == null) {
            return;
        }
        if (charge != null) {
            charge.userPosed = true;
        }
        MovementFx.pose(player, clip);
    }

    private static void hush(Charge charge, JojohaPlayerData data) {
        if (charge.userHeld || charge.userPosed) {
            MovementFx.drop(charge.player);
        }
        if (charge.spoke) {
            TimeStopLine.silence(charge.player, data);
        }
        if (charge.posed || charge.looping) {
            StandEntity stand = StandSummonHandler.findStand(charge.player, data);
            if (stand != null) {
                if (charge.posed) {
                    stand.stopTimeStopPose();
                } else {
                    stand.stopChargeLoop();
                }
            }
        }
    }

    private static Charge end(ServerPlayer player) {
        Charge charge = CHARGING.remove(player.getUUID());
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(DRAW_SPEED);
        }
        NetworkHandler.broadcastFlair(player, FlairCuePacket.TIME_CHARGE, 0);
        return charge;
    }

    private static void tick(MinecraftServer server) {
        if (!RELEASED.isEmpty()) {
            for (Charge charge : RELEASED.values()) {
                JojohaPlayerData data = PlayerDataAccess.get(charge.player);
                if (!TimeStopSkill.INSTANCE.isRunning(data)) {
                    hush(charge, data);
                }
            }
            RELEASED.clear();
        }
        if (CHARGING.isEmpty()) {
            return;
        }
        Iterator<Charge> each = CHARGING.values().iterator();
        while (each.hasNext()) {
            Charge charge = each.next();
            ServerPlayer player = charge.player;
            JojohaPlayerData data = PlayerDataAccess.get(player);
            long elapsed = player.level().getGameTime() - charge.began;
            if (!player.isAlive() || player.isRemoved() || elapsed > charge.max + LATE_TICKS) {
                each.remove();
                hush(charge, data);
                AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.removeModifier(DRAW_SPEED);
                }
                NetworkHandler.broadcastFlair(player, FlairCuePacket.TIME_CHARGE, 0);
                continue;
            }
            advance(charge, data, elapsed);
        }
    }
}
