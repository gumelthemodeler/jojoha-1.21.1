package org.gumel.jojoha.stand;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.EnergySystem;
import org.gumel.jojoha.combat.fx.Palette;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.dream.DreamRealm;
import org.gumel.jojoha.fortune.Cursed;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.FlairCuePacket;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.speech.Speech;
import org.gumel.jojoha.stand.grapple.HermitGrappleHook;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.workshop.SummonStyle;
import org.gumel.jojoha.vampire.Headless;

import java.util.function.Supplier;

public final class StandSummonHandler {

    private StandSummonHandler() {
    }

    public static void handleHalfSummonRequest(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent() || !StandTypes.hasBody(data)) {
            return;
        }
        if (data.standPiloting && !data.halfSummon) {
            player.displayClientMessage(Component.translatable("message.jojoha.half_summon.piloting"), true);
            return;
        }

        data.halfSummon = !data.halfSummon;
        PlayerDataAccess.set(player, data);

        StandEntity stand = findStand(player, data);
        if (stand != null) {
            stand.setTrustTier(data.manifestTier());
        }

        player.displayClientMessage(data.halfSummon
                ? Component.translatable("message.jojoha.half_summon.on",
                        Component.keybind("key.jojoha.skill_modifier"))
                : Component.translatable("message.jojoha.half_summon.off"), true);

        PlayerDataAccess.sync(player);
    }

    public static void handleToggleRequest(ServerPlayer player) {
        if (Headless.is(player)) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent()) {
            Jojoha.LOGGER.info("{} pressed summon but has no Stand - ignoring", player.getGameProfile().getName());
            return;
        }

        if (data.standArrowRitualTicks > 0) {
            return;
        }

        if (!StandTypes.hasBody(data)) {
            player.displayClientMessage(Component.translatable(
                    "message.jojoha.anubis.summon"), true);
            return;
        }

        TrustTier tier = data.stand.trust();

        if (!data.standSummoned && DreamRealm.dreaming(player)) {
            return;
        }

        if (!data.standSummoned && Heaven.summonRefused(player)) {
            return;
        }

        if (Cursed.summonRefused(player, data, !data.standSummoned)) {
            return;
        }

        if (data.standSummoned) {
            Jojoha.LOGGER.info("{} ending Stand cast", player.getGameProfile().getName());
            dismiss(player, data);
        } else {
            if (tier.manifestsEntity()) {
                if (EnergySystem.recallWaitTicks(player) > 0) {
                    player.displayClientMessage(Component.translatable(
                            "message.jojoha.stand.recovering"), true);
                    return;
                }
                if (!EnergySystem.canAffordSummon(data)) {
                    player.displayClientMessage(Component.translatable(
                            "message.jojoha.stand.too_tired",
                            Math.round(EnergySystem.SUMMON_MINIMUM_FRACTION * 100F)), true);
                    return;
                }
                data.standEnergy -= EnergySystem.summonCost(data);
            }
            Jojoha.LOGGER.info("{} casting Stand {} at trust tier {}",
                    player.getGameProfile().getName(), data.stand.standId(), tier);
            summon(player, data);
        }

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    private static void summon(ServerPlayer player, JojohaPlayerData data) {
        data.standSummoned = true;
        data.summonedStandEntityUuid = null;

        StandType type = StandTypes.byIdOrDefault(data.stand.standId());
        boolean shadow = StandVoices.shadowed(data.stand);
        player.serverLevel().playSound(player, player.blockPosition(), StandVoices.summon(type, shadow),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        SoundEvent call = StandVoices.summonCall(type, shadow);
        if (call != null) {
            player.serverLevel().playSound(player, player.blockPosition(), call, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        Speech.summoned(player);

        if (!data.stand.trust().manifestsEntity() || !StandTypes.hasBody(data)) {
            return;
        }

        StandEntity stand = new StandEntity(ModRegistries.STAND.get(), player.serverLevel());
        stand.setOwner(player);
        stand.setStandType(data.stand.standId());
        stand.setTrustTier(data.manifestTier());
        stand.setSkin(data.stand.skin());
        stand.setPlacement(data.standPlacement);
        stand.setIdleClip(StandCustomization.idleOf(player));
        stand.setLook(data.restPose, data.barrageShout);
        stand.setMode(data.standMode);
        stand.moveTo(player.getX(), player.getY() + 0.5, player.getZ(), player.getYRot(), 0F);
        player.serverLevel().addFreshEntity(stand);

        data.summonedStandEntityUuid = stand.getUUID();
        NetworkHandler.broadcastFlair(player,
                FlairCuePacket.SUMMON,
                SummonStyle.of(data.summonStyle).ordinal() << SummonStyle.CUE_SHIFT
                        | StandShadowForm.auraColourFor(data.stand, player.getUUID()) & 0xFFFFFF);
        Heaven.summoned(player, data);
        Standoff.summoned(player);

        Supplier<SoundEvent> voice =
                StandTypes.byIdOrDefault(data.stand.standId()).voice();
        if (voice != null && voice.get() != null) {
            player.serverLevel().playSound(null, player.blockPosition(), voice.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static void forceSummon(ServerPlayer player, JojohaPlayerData data) {
        if (!data.stand.isPresent()) {
            return;
        }

        if (data.standSummoned && findStand(player, data) != null) {
            return;
        }

        data.standSummoned = false;
        data.summonedStandEntityUuid = null;
        summon(player, data);
    }

    public static StandEntity findStand(ServerPlayer player, JojohaPlayerData data) {
        if (data.summonedStandEntityUuid == null) {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(data.summonedStandEntityUuid);
        return entity instanceof StandEntity stand ? stand : null;
    }

    public static void dismiss(ServerPlayer player, JojohaPlayerData data) {
        HermitGrappleHook.releaseAll(player);

        StandEntity stand = findStand(player, data);
        if (stand != null) {
            stand.beginDismissal();
            NetworkHandler.broadcastFlair(player,
                    FlairCuePacket.UNSUMMON,
                    StandShadowForm.auraColourFor(data.stand, player.getUUID()) & 0xFFFFFF);
        }

        if (data.standSummoned) {
            player.serverLevel().playSound(null, player.blockPosition(), StandVoices.dismiss(
                            StandTypes.byIdOrDefault(data.stand.standId())),
                    SoundSource.PLAYERS, 1.0F, 0.8F);
        }

        if (data.standSummoned && data.stand.trust().manifestsEntity()) {
            EnergySystem.standWentAway(player);
        }
        data.standSummoned = false;
        data.summonedStandEntityUuid = null;
    }

    public static void collapse(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        collapse(player, data);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static void collapse(ServerPlayer player, JojohaPlayerData data) {
        if (!data.standSummoned) {
            return;
        }

        player.serverLevel().playSound(null, player.blockPosition(), ModSounds.STAND_DISMISS.get(),
                SoundSource.PLAYERS, 1.0F, COLLAPSE_PITCH);

        data.standEnergy = 0F;
        NetworkHandler.sendPalette(player, Palette.DREAD, COLLAPSE_PALETTE_TICKS);
        dismiss(player, data);
    }

    private static final float COLLAPSE_PITCH = 0.45F;
    private static final int COLLAPSE_PALETTE_TICKS = 30;

    public static void dismissImmediately(ServerPlayer player, JojohaPlayerData data) {
        HermitGrappleHook.releaseAll(player);

        if (data.summonedStandEntityUuid != null) {
            Entity entity = player.serverLevel().getEntity(data.summonedStandEntityUuid);
            if (entity != null) {
                entity.discard();
            }
        }
        if (data.standSummoned && data.stand.trust().manifestsEntity()) {
            EnergySystem.standWentAway(player);
        }
        data.standSummoned = false;
        data.summonedStandEntityUuid = null;
    }
}
