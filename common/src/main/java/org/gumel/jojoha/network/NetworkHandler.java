package org.gumel.jojoha.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonMoves;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.gumel.jojoha.network.packet.PilotPosePacket;
import org.gumel.jojoha.network.packet.SkullFlashPacket;
import org.gumel.jojoha.network.packet.UnlockNodePacket;
import org.gumel.jojoha.network.packet.StandAfterimagePacket;
import org.gumel.jojoha.network.packet.RequestStandPunchPacket;
import org.gumel.jojoha.network.packet.UseStandSkillPacket;
import org.gumel.jojoha.stand.skill.PilotSystem;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.client.StandAfterimages;
import org.gumel.jojoha.client.TerrainCurve;
import org.gumel.jojoha.client.StandRitualEffects;
import org.gumel.jojoha.network.packet.CycleStandModePacket;
import org.gumel.jojoha.network.packet.EquipSkillPacket;
import org.gumel.jojoha.network.packet.SpendStatPointPacket;
import org.gumel.jojoha.network.packet.EngageStandTargetPacket;
import org.gumel.jojoha.network.packet.SetStandGuardPacket;
import org.gumel.jojoha.network.packet.StandRitualEffectPacket;
import org.gumel.jojoha.network.packet.StandUseItemPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.network.packet.SyncPlayerDataPacket;
import org.gumel.jojoha.network.packet.TimeStopStatePacket;
import org.gumel.jojoha.network.packet.ToggleStandSummonPacket;
import org.gumel.jojoha.network.packet.UseHamonMovePacket;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.StandHands;
import org.gumel.jojoha.stand.StandSummonHandler;

/** Registers the mod's packets and offers small helpers to send them. */
public final class NetworkHandler {

    private NetworkHandler() {
    }

    public static void init() {
        // These two are mutually exclusive per the physical side: a physical client (which also
        // hosts the integrated server in singleplayer) registers the receiver, which already gives
        // it everything needed to both decode and encode. A dedicated server never receives its own
        // S2C packets, so it needs the payload type registered explicitly to be able to encode them.
        if (Platform.getEnvironment() == Env.CLIENT) {
            NetworkManager.registerReceiver(NetworkManager.s2c(), SyncPlayerDataPacket.TYPE, SyncPlayerDataPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> {
                        JojohaPlayerData data = payload.data();
                        ClientPlayerDataCache.data = data;
                    }));
            NetworkManager.registerReceiver(NetworkManager.s2c(), StandRitualEffectPacket.TYPE, StandRitualEffectPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> StandRitualEffects.handle(payload)));
            NetworkManager.registerReceiver(NetworkManager.s2c(), StandAfterimagePacket.TYPE, StandAfterimagePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> StandAfterimages.begin(payload)));
            NetworkManager.registerReceiver(NetworkManager.s2c(), TimeStopStatePacket.TYPE, TimeStopStatePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> org.gumel.jojoha.client.TimeStopView.set(
                            payload.active(), payload.x(), payload.y(), payload.z(),
                            payload.remainingTicks())));
        } else {
            NetworkManager.registerS2CPayloadType(SyncPlayerDataPacket.TYPE, SyncPlayerDataPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(StandRitualEffectPacket.TYPE, StandRitualEffectPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(StandAfterimagePacket.TYPE, StandAfterimagePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(TimeStopStatePacket.TYPE, TimeStopStatePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(SkullFlashPacket.TYPE, SkullFlashPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    org.gumel.jojoha.network.packet.ThornLashPacket.TYPE,
                    org.gumel.jojoha.network.packet.ThornLashPacket.STREAM_CODEC);
        }

        NetworkManager.registerReceiver(NetworkManager.c2s(), UseHamonMovePacket.TYPE, UseHamonMovePacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        HamonMoves.handleUseRequest(serverPlayer, payload.moveId());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), ToggleStandSummonPacket.TYPE, ToggleStandSummonPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandSummonHandler.handleToggleRequest(serverPlayer);
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), RequestStandPunchPacket.TYPE, RequestStandPunchPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandCombatHandler.handlePunchRequest(serverPlayer);
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), StandUseItemPacket.TYPE, StandUseItemPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandHands.handleUseRequest(serverPlayer, payload.stretchAnchor());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), UseStandSkillPacket.TYPE, UseStandSkillPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandSkills.handleUseRequest(serverPlayer, payload.slot(), payload.chargeTicks());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.s2c(), SkullFlashPacket.TYPE,
                SkullFlashPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() ->
                        org.gumel.jojoha.client.SkullFlashFx.begin(payload.victimId(),
                                payload.attackerId(), payload.phase(), payload.ticks())));

        NetworkManager.registerReceiver(NetworkManager.s2c(),
                org.gumel.jojoha.network.packet.ThornLashPacket.TYPE,
                org.gumel.jojoha.network.packet.ThornLashPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() ->
                        org.gumel.jojoha.client.ThornLashFx.begin(payload.fromId(),
                                payload.toId(), payload.ticks())));

        NetworkManager.registerReceiver(NetworkManager.c2s(), UnlockNodePacket.TYPE, UnlockNodePacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
                        return;
                    }

                    org.gumel.jojoha.skilltree.SkillNode node = org.gumel.jojoha.skilltree.SkillTrees
                            .byId(net.minecraft.resources.ResourceLocation.tryParse(payload.nodeId()));

                    org.gumel.jojoha.data.JojohaPlayerData data =
                            org.gumel.jojoha.data.PlayerDataAccess.get(serverPlayer);

                    // Asked again here, of the player rather than of what the client claimed. The
                    // client's own copy of this test is only ever for drawing a node lit or dark.
                    if (!org.gumel.jojoha.skilltree.SkillTrees.canUnlock(serverPlayer, data, node)) {
                        return;
                    }

                    // Taken only once the whole thing is known to be affordable, so a node that
                    // fails on its second requirement cannot charge for its first.
                    for (org.gumel.jojoha.skilltree.SkillNode.ItemRequirement cost : node.items()) {
                        cost.consume(serverPlayer);
                    }

                    data.grantNode(node.id());
                    org.gumel.jojoha.data.PlayerDataAccess.set(serverPlayer, data);
                    org.gumel.jojoha.data.PlayerDataAccess.sync(serverPlayer);
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), PilotPosePacket.TYPE, PilotPosePacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        PilotSystem.applyClientPose(serverPlayer, payload.x(), payload.y(),
                                payload.z(), payload.yRot(), payload.xRot());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), EngageStandTargetPacket.TYPE, EngageStandTargetPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandCombatHandler.handleEngageTarget(serverPlayer, payload.targetId());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), EquipSkillPacket.TYPE, EquipSkillPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
                        return;
                    }

                    if (payload.skillId().isEmpty()) {
                        org.gumel.jojoha.stand.skill.SkillLoadout.clear(serverPlayer, payload.slot(),
                                payload.utility());
                        return;
                    }

                    net.minecraft.resources.ResourceLocation id =
                            net.minecraft.resources.ResourceLocation.tryParse(payload.skillId());
                    if (id == null) {
                        return;
                    }

                    // Three requests share this packet, told apart by the slot. Spelled out rather
                    // than inferred, because the first version leaned on a negative number meaning
                    // one thing here and another there, and the unequip sentinel fell through every
                    // branch without matching any of them.
                    if (payload.slot() == EquipSkillPacket.UNEQUIP) {
                        org.gumel.jojoha.stand.skill.SkillLoadout.unequip(serverPlayer, id,
                                payload.utility());
                    } else {
                        org.gumel.jojoha.stand.skill.SkillLoadout.equip(serverPlayer, id,
                                payload.slot(), payload.utility());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), SpendStatPointPacket.TYPE, SpendStatPointPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        org.gumel.jojoha.data.StatPoints.spend(serverPlayer, payload.stand(),
                                payload.stat(), payload.amount());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), CycleStandModePacket.TYPE, CycleStandModePacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandCombatHandler.handleCycleMode(serverPlayer);
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), SetStandGuardPacket.TYPE, SetStandGuardPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandCombatHandler.handleSetGuard(serverPlayer, payload.guarding());
                    }
                }));
    }

    /** Nobody past this is going to make out a stab animation anyway, and it comfortably covers render distance for the rays. */
    private static final double RITUAL_EFFECT_RADIUS = 96.0;

    /**
     * Fires a ritual effect on {@code subject} for everyone nearby who can see them - including
     * the subject themselves, since the whole point is that it's a spectacle.
     *
     * <p>Filters on level and distance rather than the platform's entity-tracking list, which
     * Fabric and NeoForge expose through different (non-Architectury) APIs.
     */
    public static void broadcastRitualEffect(ServerPlayer subject, StandRitualEffectPacket.Effect effect) {
        broadcastNearby(subject, new StandRitualEffectPacket(subject.getUUID(), effect));
    }

    /** Tells everyone who can see this player to start trailing after-images behind them. */
    public static void broadcastAfterimages(ServerPlayer subject, int color, int durationTicks) {
        broadcastNearby(subject, new StandAfterimagePacket(subject.getUUID(), color, durationTicks));
    }

    /** The subject plus anyone close enough to be looking at them. */
    private static void broadcastNearby(ServerPlayer subject, CustomPacketPayload packet) {
        double radiusSqr = RITUAL_EFFECT_RADIUS * RITUAL_EFFECT_RADIUS;

        for (ServerPlayer viewer : subject.serverLevel().players()) {
            if (viewer == subject || viewer.distanceToSqr(subject) <= radiusSqr) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    public static void sendPlayerData(ServerPlayer player, JojohaPlayerData data) {
        NetworkManager.sendToPlayer(player, new SyncPlayerDataPacket(data));
    }

    /** Client-side only: fire a move-use request at the server. */
    public static void sendUseMove(ResourceLocation moveId) {
        NetworkManager.sendToServer(new UseHamonMovePacket(moveId));
    }

    /** Client-side only: request the server toggle this player's Stand summon state. */
    public static void sendToggleStandSummon() {
        NetworkManager.sendToServer(new ToggleStandSummonPacket());
    }

    /** Client-side only: request the server play the next punch in the Stand's M1 chain. */
    public static void sendRequestStandPunch() {
        NetworkManager.sendToServer(new RequestStandPunchPacket());
    }

    /** Client-side only: hand this right-click to the Stand instead of using it ourselves. */
    public static void sendStandUseItem(java.util.Optional<net.minecraft.core.BlockPos> stretchAnchor) {
        NetworkManager.sendToServer(new StandUseItemPacket(stretchAnchor));
    }

    /** Client-side only: tell the server the guard key was pressed or released. */
    public static void sendSetStandGuard(boolean guarding) {
        NetworkManager.sendToServer(new SetStandGuardPacket(guarding));
    }

    /** Client-side only: press one of the Stand's skill slots. */
    public static void sendUseStandSkill(int slot, int chargeTicks) {
        NetworkManager.sendToServer(new UseStandSkillPacket(slot, chargeTicks));
    }

    /**
     * Tells everyone near a time stop that it has started or ended.
     *
     * <p>Sent to every player within range of the stop rather than to its caster, which is what makes
     * it visible in multiplayer at all. The range is generous relative to the sphere: somebody just
     * outside it should still see it standing there, and the alternative to a margin is people
     * watching the effect wink in and out as they walk along its edge.
     */
    public static void broadcastTimeStop(ServerLevel level, Vec3 centre, int remainingTicks,
                                         boolean active) {
        TimeStopStatePacket packet = active
                ? new TimeStopStatePacket(true, centre.x, centre.y, centre.z, remainingTicks)
                : TimeStopStatePacket.ended();

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(centre) <= TIME_STOP_VIEW_RANGE * TIME_STOP_VIEW_RANGE) {
                NetworkManager.sendToPlayer(player, packet);
            }
        }
    }

    /** How far away a time stop is still worth drawing, in blocks. */
    private static final double TIME_STOP_VIEW_RANGE = 96.0;

    /** Client-side only: one tick of steering for a piloted Stand. */
    /**
     * Tells everyone who can see this victim to flash a skull inside their head.
     *
     * <p>Sent to the players tracking the entity rather than to the whole server: a picture drawn on
     * somebody nobody can see is a packet for nothing.
     */
    /**
     * Tells everyone who can see it that a vine has been thrown.
     *
     * <p>Same shape as the skull flash below, and the same reasoning: a short visual that every
     * nearby client can draw for itself given two ids, rather than an entity that exists for a tick.
     */
    public static void sendThornLash(net.minecraft.server.level.ServerLevel level,
                                     net.minecraft.world.entity.Entity from,
                                     net.minecraft.world.entity.Entity to, int ticks) {
        org.gumel.jojoha.network.packet.ThornLashPacket packet =
                new org.gumel.jojoha.network.packet.ThornLashPacket(from.getId(), to.getId(), ticks);

        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(from) < 64 * 64) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    public static void sendSkullFlash(net.minecraft.server.level.ServerLevel level,
                                      net.minecraft.world.entity.LivingEntity victim,
                                      ServerPlayer attacker, int phase, int ticks) {
        SkullFlashPacket packet =
                new SkullFlashPacket(victim.getId(), attacker.getId(), phase, ticks);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(victim) < 64 * 64) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    /** Client-side only: ask for a skill-tree node. */
    public static void sendUnlockNode(net.minecraft.resources.ResourceLocation nodeId) {
        NetworkManager.sendToServer(new UnlockNodePacket(nodeId.toString()));
    }

    public static void sendPilotPose(double x, double y, double z, float yRot, float xRot) {
        NetworkManager.sendToServer(new PilotPosePacket(x, y, z, yRot, xRot));
    }

    /** Client-side only: send the Stand at the entity the player has lined up. */
    public static void sendEngageStandTarget(int targetId) {
        NetworkManager.sendToServer(new EngageStandTargetPacket(targetId));
    }

    /** Client-side only: ask the server to switch the Stand's stance. */
    /** Asks for a move to be put on the bar, in a given slot or wherever there is room. */
    public static void sendEquipSkill(net.minecraft.resources.ResourceLocation skillId, int slot) {
        sendEquipSkill(skillId, slot, false);
    }

    public static void sendEquipSkill(net.minecraft.resources.ResourceLocation skillId, int slot,
                                      boolean utility) {
        NetworkManager.sendToServer(new EquipSkillPacket(skillId.toString(), slot, utility));
    }

    /** Empties one slot, whatever is in it. The server reads an empty id as "clear this". */
    public static void sendClearSlot(int slot, boolean utility) {
        NetworkManager.sendToServer(new EquipSkillPacket(EquipSkillPacket.CLEAR, slot, utility));
    }

    /** Asks for a move to be taken off the bar, wherever it is. */
    public static void sendUnequipSkill(net.minecraft.resources.ResourceLocation skillId) {
        sendUnequipSkill(skillId, false);
    }

    public static void sendUnequipSkill(net.minecraft.resources.ResourceLocation skillId,
                                        boolean utility) {
        NetworkManager.sendToServer(
                new EquipSkillPacket(skillId.toString(), EquipSkillPacket.UNEQUIP, utility));
    }

    public static void sendSpendStatPoint(boolean stand, int stat, int amount) {
        NetworkManager.sendToServer(new SpendStatPointPacket(stand, stat, amount));
    }

    public static void sendCycleStandMode() {
        NetworkManager.sendToServer(new CycleStandModePacket());
    }
}
