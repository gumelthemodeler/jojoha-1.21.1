package org.gumel.jojoha.neoforge.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.BarrageAir;
import org.gumel.jojoha.client.BarrageFx;
import org.gumel.jojoha.client.BarrageStruggleHud;
import org.gumel.jojoha.client.ComboCounter;
import org.gumel.jojoha.client.CentralBarOverlay;
import org.gumel.jojoha.client.ClackerClient;
import org.gumel.jojoha.client.Clairvoyance;
import org.gumel.jojoha.client.CombatBarOverlay;
import org.gumel.jojoha.client.DreamAmbience;
import org.gumel.jojoha.client.DreamSpirit;
import org.gumel.jojoha.client.DreamWater;
import org.gumel.jojoha.client.FactionBanner;
import org.gumel.jojoha.client.FamilySenseHud;
import org.gumel.jojoha.client.FortuneSense;
import org.gumel.jojoha.client.GrappleController;
import org.gumel.jojoha.client.GrappleTarget;
import org.gumel.jojoha.client.GroundBreakerFx;
import org.gumel.jojoha.client.HeavenHud;
import org.gumel.jojoha.client.HeavenWorldFx;
import org.gumel.jojoha.client.LocalStandAuraEffect;
import org.gumel.jojoha.client.LockOn;
import org.gumel.jojoha.client.LootSense;
import org.gumel.jojoha.client.PhantomAmbience;
import org.gumel.jojoha.client.PilotBody;
import org.gumel.jojoha.client.PilotPose;
import org.gumel.jojoha.client.PlayerMenuScreen;
import org.gumel.jojoha.client.QuestBanner;
import org.gumel.jojoha.client.QuestTracker;
import org.gumel.jojoha.client.RawKey;
import org.gumel.jojoha.client.SabreArcFx;
import org.gumel.jojoha.client.ScarfClient;
import org.gumel.jojoha.client.ScreenLanguage;
import org.gumel.jojoha.client.SkullCrushOverlay;
import org.gumel.jojoha.client.SkullFlashFx;
import org.gumel.jojoha.client.SpaceRipperBeam;
import org.gumel.jojoha.client.StandBarrageAudio;
import org.gumel.jojoha.client.StandCombatInput;
import org.gumel.jojoha.client.StandGlowTracker;
import org.gumel.jojoha.client.StandIdles;
import org.gumel.jojoha.client.StandRipples;
import org.gumel.jojoha.client.StandRitualEffects;
import org.gumel.jojoha.client.StandRitualText;
import org.gumel.jojoha.client.StandSkillInput;
import org.gumel.jojoha.client.StandStretch;
import org.gumel.jojoha.client.StandSummonSound;
import org.gumel.jojoha.client.StandTurn;
import org.gumel.jojoha.client.StingFx;
import org.gumel.jojoha.client.SunScorch;
import org.gumel.jojoha.client.SunScorchOverlay;
import org.gumel.jojoha.client.TendrilStrands;
import org.gumel.jojoha.client.ThornLashFx;
import org.gumel.jojoha.client.TimeStopChargeBar;
import org.gumel.jojoha.client.TimeStopShader;
import org.gumel.jojoha.client.UltimateWings;
import org.gumel.jojoha.client.VampireColourOverlay;
import org.gumel.jojoha.client.WaterWalk;
import org.gumel.jojoha.client.WorldVfx;
import org.gumel.jojoha.client.ZoomPunchArm;
import org.gumel.jojoha.client.pillar.PillarClient;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Jojoha.MOD_ID, value = Dist.CLIENT)
public final class JojohaNeoForgeClient {
    public static final KeyMapping OPEN_STATS_KEY = new KeyMapping(
            "key.jojoha.open_stats", GLFW.GLFW_KEY_G, "key.categories.jojoha");
    public static final KeyMapping TOGGLE_COMBAT_PAGE_KEY = new KeyMapping(
            "key.jojoha.toggle_combat_page", GLFW.GLFW_KEY_I, "key.categories.jojoha");
    public static final KeyMapping TOGGLE_COMBAT_BAR_KEY = new KeyMapping(
            "key.jojoha.toggle_combat_bar", GLFW.GLFW_KEY_O, "key.categories.jojoha");
    public static final KeyMapping SUMMON_STAND_KEY = new KeyMapping(
            "key.jojoha.summon_stand", GLFW.GLFW_KEY_X, "key.categories.jojoha");

    public static final KeyMapping SKILL_MODIFIER_KEY = new KeyMapping(
            RawKey.MODIFIER_KEY, GLFW.GLFW_KEY_LEFT_ALT, "key.categories.jojoha");

    public static final KeyMapping[] STAND_SKILL_KEYS = {
            new KeyMapping("key.jojoha.stand_skill_1", GLFW.GLFW_KEY_Z, "key.categories.jojoha"),
            new KeyMapping("key.jojoha.stand_skill_2", GLFW.GLFW_KEY_Y, "key.categories.jojoha"),
            new KeyMapping("key.jojoha.stand_skill_3", GLFW.GLFW_KEY_C, "key.categories.jojoha"),
            new KeyMapping("key.jojoha.stand_skill_4", GLFW.GLFW_KEY_V, "key.categories.jojoha"),
    };

    private JojohaNeoForgeClient() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            WorldVfx.solid(event.getPoseStack(), event.getCamera(),
                    event.getPartialTick().getGameTimeDeltaPartialTick(false));
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        WorldVfx.render(event.getPoseStack(), event.getCamera(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));

        PilotBody.render(event.getPoseStack(),
                Minecraft.getInstance().renderBuffers().bufferSource(),
                event.getCamera(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));

        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        TimeStopShader.render(event.getPartialTick().getGameTimeDeltaPartialTick(false));

        SkullCrushOverlay.render(event.getGuiGraphics(),
                event.getGuiGraphics().guiWidth(), event.getGuiGraphics().guiHeight(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
        CentralBarOverlay.render(event.getGuiGraphics());
        BarrageStruggleHud.render(event.getGuiGraphics());
        ComboCounter.render(event.getGuiGraphics());
        TimeStopChargeBar.render(event.getGuiGraphics());
            VampireColourOverlay.render(event.getGuiGraphics());
            SunScorchOverlay.render(event.getGuiGraphics());
            FortuneSense.render(event.getGuiGraphics(),
                    event.getPartialTick().getGameTimeDeltaPartialTick(false));
            FamilySenseHud.render(event.getGuiGraphics(),
                    event.getPartialTick().getGameTimeDeltaPartialTick(false));
        StandRitualText.render(event.getGuiGraphics());
        DreamSpirit.renderHud(event.getGuiGraphics());
        HeavenHud.render(event.getGuiGraphics());
        QuestTracker.render(event.getGuiGraphics());
        QuestBanner.render(event.getGuiGraphics());
        FactionBanner.render(event.getGuiGraphics());
        ScreenLanguage.render(event.getGuiGraphics());
        StingFx.renderHud(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        GrappleController.tick();
        UltimateWings.tick();

        Minecraft client = Minecraft.getInstance();

        if (RawKey.pressed(OPEN_STATS_KEY)) {
            if (client.player != null && client.screen == null) {
                client.setScreen(new PlayerMenuScreen());
            }
        }

        if (RawKey.pressed(TOGGLE_COMBAT_PAGE_KEY)) {
            if (client.player != null) {
                CombatBarOverlay.togglePage();
            }
        }

        if (RawKey.pressed(TOGGLE_COMBAT_BAR_KEY)) {
            if (client.player != null) {
                CombatBarOverlay.toggleVisibility();
            }
        }

        LocalStandAuraEffect.tick();
        StandSummonSound.tickClient();
        StandRitualEffects.tick();
        StandGlowTracker.tick();
        LootSense.tick();
        Clairvoyance.tick();
        SunScorch.tick();
        WaterWalk.tick();
        GrappleTarget.tick();
        ThornLashFx.tick();
        GroundBreakerFx.tick();
        SabreArcFx.tick();
        PillarClient.tick();
        ZoomPunchArm.tick();
        SpaceRipperBeam.tick();
        HeavenWorldFx.tick();
        ClackerClient.tick();
        ScarfClient.tick();
        TendrilStrands.tick();
        StandRipples.tick();
        StandBarrageAudio.tick();
        BarrageAir.tick();
        BarrageFx.tick();
        StandIdles.tick();
        StandCombatInput.tick(SUMMON_STAND_KEY);
            StandTurn.tick();
        LockOn.tick();
            StandStretch.tick();
        StandSkillInput.tick(STAND_SKILL_KEYS);
            PilotPose.tick(Minecraft.getInstance());
        SkullFlashFx.tick();
        PhantomAmbience.tick();
        DreamAmbience.tick();
        DreamSpirit.tick();
        DreamWater.tick();
        CombatBarOverlay.tickClientState();
    }
}
