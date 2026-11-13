package org.gumel.jojoha.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.combat.fx.Palette;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.StandData;
import org.gumel.jojoha.item.StandArrowRitual;
import org.gumel.jojoha.network.packet.StandNamedPacket;
import org.gumel.jojoha.network.packet.StandRitualEffectPacket;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandManifestation;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.supporter.SupporterAuras;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class StandRitualEffects {
    private static final int MASK_TEXT_COLOR = 0xFF0000;

    private static final int MASK_CLOSING_DELAY_TICKS = 70;

    private static final float MASK_RAY_RED = 1.0F;
    private static final float MASK_RAY_GREEN = 0.13F;
    private static final float MASK_RAY_BLUE = 0.09F;

    private StandRitualEffects() {
    }

    public static void handle(StandRitualEffectPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float now = (float) minecraft.level.getGameTime();
        boolean isLocalPlayer = minecraft.player != null && minecraft.player.getUUID().equals(packet.playerId());

        switch (packet.effect()) {
            case STAB -> PlayerStabAnimation.begin(packet.playerId(), PlayerStabAnimation.STAB_ANIMATION, now);
            case PIERCED -> {
                if (isLocalPlayer) {
                    PaletteSwap.hold(Palette.PIERCE, PIERCE_PALETTE_TICKS);
                    StandRitualText.show(Component.translatable("message.jojoha.arrow.resonate"),
                            now, StandRitualText.NARRATION_COLOR);
                }
            }
            case SOUL_CALLED -> {
                Player user = minecraft.level.getPlayerByUUID(packet.playerId());
                if (user != null) {
                    StraySoul.begin(user, now);
                }
            }
            case AWAKENING_RAYS -> {
                PlayerStabAnimation.begin(packet.playerId(), PlayerStabAnimation.AWAKEN_ANIMATION, now);
                StandAwakeningRays.begin(packet.playerId(), now);
                if (isLocalPlayer) {
                    announceStandAfterAwakening(now);
                }
            }
            case AWAKENING_REFUSED -> {
                PlayerStabAnimation.stop(packet.playerId());
                namedNothing(packet.playerId());
            }
            case SKIN_RAYS -> {
                PlayerStabAnimation.begin(packet.playerId(), PlayerStabAnimation.AWAKEN_ANIMATION, now);
                StandAwakeningRays.begin(packet.playerId(), now);
                playArrowSound(minecraft, packet.playerId());
                if (isLocalPlayer) {
                    announceSkinAfterAwakening(now);
                }
            }

            case SKIN_NAMED -> {
                if (isLocalPlayer) {
                    StandRitualText.show(
                            Component.translatable("message.jojoha.arrow.skin_name", standName(), skinName()),
                            now, StandRitualText.STAND_NAME_COLOR);
                }
            }

            case LAND -> PlayerStabAnimation.begin(packet.playerId(), PlayerStabAnimation.LAND_ANIMATION, now);

            case MASK_EQUIP -> {
                PlayerStabAnimation.begin(packet.playerId(), PlayerStabAnimation.EQUIP_MASK_ANIMATION, now);
                if (isLocalPlayer) {
                    StandRitualText.show(Component.translatable("message.jojoha.mask.raise"),
                            now, MASK_TEXT_COLOR);
                }
            }

            case MASK_SEATED, AJA_SEATED -> {
                StoneMaskState.seat(packet.playerId(), now, packet.effect() == StandRitualEffectPacket.Effect.AJA_SEATED);
                if (isLocalPlayer) {
                    PaletteSwap.hold(Palette.PIERCE, PIERCE_PALETTE_TICKS);
                    StandRitualText.show(Component.translatable("message.jojoha.mask.seated"),
                            now, MASK_TEXT_COLOR);
                }
            }

            case MASK_TURNING -> StoneMaskState.activate(packet.playerId(), now);

            case MASK_AWAKENING -> {
                PlayerStabAnimation.begin(packet.playerId(), PlayerStabAnimation.AWAKEN_ANIMATION, now);
                StandAwakeningRays.beginMask(packet.playerId(), now,
                        MASK_RAY_RED, MASK_RAY_GREEN, MASK_RAY_BLUE);
                if (isLocalPlayer) {
                    VampireColour.begin(now);
                }
                if (isLocalPlayer) {
                    StandRitualText.show(Component.translatable("message.jojoha.mask.turn"),
                            now, MASK_TEXT_COLOR);
                    StandRitualText.showAfter(Component.translatable("message.jojoha.mask.become"),
                            now, MASK_CLOSING_DELAY_TICKS, MASK_TEXT_COLOR);
                }
            }

            case MASK_SPENT -> {
                StoneMaskState.remove(packet.playerId());
                if (isLocalPlayer) {
                    VampireColour.release();
                }
            }

            case MASK_CANCELLED -> {
                PlayerStabAnimation.stop(packet.playerId());
                StoneMaskState.remove(packet.playerId());
                if (isLocalPlayer) {
                    VampireColour.release();
                }
            }
        }
    }

    private static void playArrowSound(Minecraft minecraft, UUID playerId) {
        if (minecraft.level == null) {
            return;
        }

        Player user = minecraft.level.getPlayerByUUID(playerId);
        if (user != null) {
            playArrowSound(minecraft, user);
        }
    }

    private static void playArrowSound(Minecraft minecraft, Player user) {
        minecraft.getSoundManager().play(new FadingRitualSound(ModSounds.STAND_ARROW.get(), user,
                ARROW_SOUND_VOLUME, ARROW_SOUND_TICKS, ARROW_SOUND_FADE_IN_TICKS, ARROW_SOUND_FADE_OUT_TICKS));
    }

    private static final float ARROW_SOUND_VOLUME = 0.4F;
    private static final int ARROW_SOUND_TICKS = 85;
    private static final int ARROW_SOUND_FADE_IN_TICKS = 14;
    private static final int ARROW_SOUND_FADE_OUT_TICKS = 22;
    private static final int ARROW_SOUND_PEAK_TICKS = StandArrowRitual.BOOM_AFTER_AWAKEN_TICKS;

    private static final int SCORE_AT = StandManifestation.FULL_AT - ARROW_SOUND_PEAK_TICKS;
    private static final int SCORE_CATCH_TICKS = 3;
    private static final Set<Integer> SCORED = new HashSet<>();

    private static void scoreVisions(Minecraft minecraft) {
        Set<Integer> present = new HashSet<>();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof StandEntity vision) || !vision.isManifesting()) {
                continue;
            }
            present.add(vision.getId());
            int t = vision.tickCount;
            if (t >= SCORE_AT && t < SCORE_AT + SCORE_CATCH_TICKS && SCORED.add(vision.getId())) {
                Player user = vision.manifestUser();
                if (user != null) {
                    playArrowSound(minecraft, user);
                }
            }
        }
        SCORED.retainAll(present);
    }

    private static StandEntity visionOf(Minecraft minecraft, UUID playerId) {
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof StandEntity vision && vision.isManifesting()) {
                Player user = vision.manifestUser();
                if (user != null && user.getUUID().equals(playerId)) {
                    return vision;
                }
            }
        }
        return null;
    }

    private static void announceStandAfterAwakening(float now) {
        StandRitualText.showAfter(Component.translatable("message.jojoha.arrow.stand_revealed"),
                now, REVEAL_AFTER_AWAKEN_TICKS, StandRitualText.NARRATION_COLOR);
    }

    private static final int REVEAL_AFTER_AWAKEN_TICKS = 12;

    private static void announceSkinAfterAwakening(float now) {
        StandRitualText.showAfter(Component.translatable("message.jojoha.arrow.skin_revealed"),
                now, StandArrowRitual.REVEAL_PAUSE_TICKS, StandRitualText.NARRATION_COLOR);
    }

    private static Component skinName() {
        StandData stand = ClientPlayerDataCache.data.stand;
        ResourceLocation id = stand.isPresent() ? stand.standId() : StandTypes.STAR_PLATINUM_ID;
        String key = StandTypes.byIdOrDefault(id).skinNameKey(stand.skin());
        return Component.literal(Component.translatable(key).getString().toUpperCase(Locale.ROOT));
    }

    private static Component standName() {
        StandData stand = ClientPlayerDataCache.data.stand;
        return nameOf(stand.isPresent() ? stand.standId() : StandTypes.STAR_PLATINUM_ID);
    }

    private static Component nameOf(ResourceLocation id) {
        String key = "stand." + id.getNamespace() + "." + id.getPath();
        return Component.literal(Component.translatable(key).getString().toUpperCase(Locale.ROOT));
    }

    public static void named(StandNamedPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        boolean figure = visionOf(minecraft, packet.playerId()) != null;
        if (!figure) {
            playArrowSound(minecraft, packet.playerId());
        }
        if (minecraft.player == null || !minecraft.player.getUUID().equals(packet.playerId())) {
            return;
        }
        float now = (float) minecraft.level.getGameTime()
                + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        StandRitualText.showAwakened(awakensHeadline(), awakenedName(packet.standId()), trustHint(),
                figure ? now : now + ARROW_SOUND_PEAK_TICKS);
        PaletteSwap.hold(Palette.AWAKEN, AWAKEN_PALETTE_TICKS);
    }

    private static Component awakensHeadline() {
        return Component.translatable("message.jojoha.arrow.stand_awakened").withStyle(ChatFormatting.BOLD);
    }

    private static Component awakenedName(ResourceLocation id) {
        StandData stand = ClientPlayerDataCache.data.stand;
        int aura = StandTypes.byIdOrDefault(id).auraColorFor(stand.isPresent() ? stand.skin() : 0);
        return Component.translatable("stand." + id.getNamespace() + "." + id.getPath())
                .withStyle(style -> style.withColor(brighten(aura)));
    }

    private static Component trustHint() {
        StandData stand = ClientPlayerDataCache.data.stand;
        if (stand.isPresent() && stand.trust() == TrustTier.BONDED) {
            return null;
        }
        return Component.translatable("message.jojoha.arrow.trust_hint", TrustTier.BONDED.level());
    }

    private static int brighten(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return ((r + (255 - r) * NAME_LIFT / 100) << 16) | ((g + (255 - g) * NAME_LIFT / 100) << 8)
                | (b + (255 - b) * NAME_LIFT / 100);
    }

    private static final int NAME_LIFT = 35;

    private static final int AWAKEN_PALETTE_TICKS = 50;
    private static final int PIERCE_PALETTE_TICKS = 40;

    public static void namedNothing(UUID playerId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getUUID().equals(playerId)) {
            StandRitualText.clear();
        }
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            PlayerStabAnimation.clear();
            PlayerMoveAnimation.clear();
            ParryFeel.clear();
            StunPose.clear();
            StandAwakeningRays.clear();
            StandAfterimages.clear();
            TimeStopShader.clear();
            TimeStopClient.clear();
            FrozenEntityFx.clear();
            TimeStopCharge.clear();
            TimeStopChargers.clear();
            TimeStopView.clear();
            StandBarrageAudio.clear();
            BarrageAir.clear();
            BarrageFx.clear();
            Menacing.clear();
            ImpactWords.clear();
            PaletteSwap.clear();
            Gore.clear();
            LimbRegrowLayer.clear();
            SummonFlair.clear();
            BlindSense.clear();
            ScreenLanguage.clear();
            StingFx.clear();
            MovePreview.idle();
            MagicianFx.clear();
            MagicianHeat.clear();
            FlameFx.clear();
            BarrageGlow.clear();
            FortuneSense.clear();
            FamilySenseHud.clear();
            SupporterAuras.clear();
            TangledVines.clear();
            HeadlessView.clear();
            StandRitualText.clear();
            ManifestCamera.clear();
            SCORED.clear();
            StraySoul.clear();
            AwakeningSound.clear();
            PreciseEyesView.clear();
            DowsingFx.clear();
            StandGlowTracker.clear();
            Clairvoyance.clear();
            SubzeroChill.clear();
            ClientPlayerDataCache.forget();
            return;
        }

        TimeStopView.tick();
        DowsingFx.tick();
        PreciseEyesView.tick();
        UltimateWings.tick();
        UltimateWingsLayer.tick();
        TimeStopShader.setPhase(TimeStopView.active());

        float now = (float) minecraft.level.getGameTime();
        PlayerStabAnimation.tick(now);
        PlayerMoveAnimation.tick(now);
        StunPose.tick(now);
        StandAwakeningRays.tick(now);
        StandAfterimages.tick();
        TimeStopClient.tick(now);
        FrozenEntityFx.tick(TimeStopView.active());
        TimeStopCharge.tick();
        TimeStopChargers.tick();
        StandRitualText.tick(now);
        StraySoul.tick(minecraft, now);
        AwakeningSound.tick(minecraft, now);
        ManifestFigure.clinkPieces(minecraft);
        ManifestCamera.tick();
        Gore.tick();
        LimbRegrowLayer.tick();
        SummonFlair.tick();
        BlindSense.tick();
        MagicianFx.tick();
        MagicianHeat.tick();
        WorldEventSky.tick();
        FlameFx.tick();
        scoreVisions(minecraft);
    }
}
