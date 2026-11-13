package org.gumel.jojoha.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.gumel.jojoha.client.AnubisSliceParticle;
import org.gumel.jojoha.client.BarrageAir;
import org.gumel.jojoha.client.BarrageFx;
import org.gumel.jojoha.client.BarrageStruggleHud;
import org.gumel.jojoha.client.ComboCounter;
import org.gumel.jojoha.client.BloodMoteParticle;
import org.gumel.jojoha.client.BubbleCutParticle;
import org.gumel.jojoha.client.BubbleSheetParticle;
import org.gumel.jojoha.client.CentralBarOverlay;
import org.gumel.jojoha.client.ClackerClient;
import org.gumel.jojoha.client.Clairvoyance;
import org.gumel.jojoha.client.CombatBarOverlay;
import org.gumel.jojoha.client.DreamAmbience;
import org.gumel.jojoha.client.DreamSpirit;
import org.gumel.jojoha.client.DreamWater;
import org.gumel.jojoha.client.EmeraldChipParticle;
import org.gumel.jojoha.client.EmeraldSparkleParticle;
import org.gumel.jojoha.client.FactionBanner;
import org.gumel.jojoha.client.FamilySenseHud;
import org.gumel.jojoha.client.FortuneSense;
import org.gumel.jojoha.client.GrappleController;
import org.gumel.jojoha.client.GrappleTarget;
import org.gumel.jojoha.client.GroundBreakerFx;
import org.gumel.jojoha.client.GuardBreakParticle;
import org.gumel.jojoha.client.HamonBubbleParticle;
import org.gumel.jojoha.client.HamonParticle;
import org.gumel.jojoha.client.HeatShimmerParticle;
import org.gumel.jojoha.client.HeavenHud;
import org.gumel.jojoha.client.HeavenWorldFx;
import org.gumel.jojoha.client.ImpactRingParticle;
import org.gumel.jojoha.client.InhaleSmokeParticle;
import org.gumel.jojoha.client.InhaleWindParticle;
import org.gumel.jojoha.client.JojohaShaders;
import org.gumel.jojoha.client.LocalStandAuraEffect;
import org.gumel.jojoha.client.LockOn;
import org.gumel.jojoha.client.LootSense;
import org.gumel.jojoha.client.ModEntityRenderers;
import org.gumel.jojoha.client.ModItemProperties;
import org.gumel.jojoha.client.PhantomAmbience;
import org.gumel.jojoha.client.PilotBody;
import org.gumel.jojoha.client.PilotPose;
import org.gumel.jojoha.client.PlayerMenuScreen;
import org.gumel.jojoha.client.QuestBanner;
import org.gumel.jojoha.client.QuestTracker;
import org.gumel.jojoha.client.RainbowHamon;
import org.gumel.jojoha.client.RawKey;
import org.gumel.jojoha.client.SabreArcFx;
import org.gumel.jojoha.client.ScarfClient;
import org.gumel.jojoha.client.ScreenLanguage;
import org.gumel.jojoha.client.SkullCrushOverlay;
import org.gumel.jojoha.client.SkullFlashFx;
import org.gumel.jojoha.client.SpaceRipperBeam;
import org.gumel.jojoha.client.StandAuraParticle;
import org.gumel.jojoha.client.StandAwakenParticle;
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
import org.gumel.jojoha.client.StandTransformParticle;
import org.gumel.jojoha.client.StandTurn;
import org.gumel.jojoha.client.StingFx;
import org.gumel.jojoha.client.StrataFaces;
import org.gumel.jojoha.client.StunParticle;
import org.gumel.jojoha.client.SunScorch;
import org.gumel.jojoha.client.SunScorchOverlay;
import org.gumel.jojoha.client.TendrilStrands;
import org.gumel.jojoha.client.ThornLashFx;
import org.gumel.jojoha.client.TimeStopChargeBar;
import org.gumel.jojoha.client.TimeStopMoteParticle;
import org.gumel.jojoha.client.TimeStopShader;
import org.gumel.jojoha.client.UltimateWings;
import org.gumel.jojoha.client.VampireColourOverlay;
import org.gumel.jojoha.client.WaterWalk;
import org.gumel.jojoha.client.WorldVfx;
import org.gumel.jojoha.client.ZoomPunchArm;
import org.gumel.jojoha.client.pillar.PillarClient;
import org.gumel.jojoha.client.pillar.PillarParticle;
import org.gumel.jojoha.registry.ModRegistries;
import org.lwjgl.glfw.GLFW;

public final class JojohaFabricClient implements ClientModInitializer {
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

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(OPEN_STATS_KEY);
        KeyBindingHelper.registerKeyBinding(TOGGLE_COMBAT_PAGE_KEY);
        KeyBindingHelper.registerKeyBinding(TOGGLE_COMBAT_BAR_KEY);
        KeyBindingHelper.registerKeyBinding(SUMMON_STAND_KEY);
        KeyBindingHelper.registerKeyBinding(SKILL_MODIFIER_KEY);
        RawKey.setModifier(SKILL_MODIFIER_KEY);
        for (KeyMapping skillKey : STAND_SKILL_KEYS) {
            KeyBindingHelper.registerKeyBinding(skillKey);
        }

        ModelLoadingPlugin.register(plugin ->
                plugin.modifyModelAfterBake().register((model, context) ->
                        model != null && StrataFaces.wraps(context.topLevelId())
                                ? new FabricStrataModel(model) : model));

        ModEntityRenderers.init();
        ModItemProperties.init();
        ParticleFactoryRegistry.getInstance().register(ModRegistries.STAND_AURA.get(), StandAuraParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.TIMESTOP_MOTE.get(), TimeStopMoteParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.STAND_AWAKEN.get(), StandAwakenParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.STAND_AWAKEN_RED.get(), StandAwakenParticle.RedProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.STAND_AWAKEN_BLUE.get(), StandAwakenParticle.BlueProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.STAND_AWAKEN_PINK.get(), StandAwakenParticle.PinkProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.BLOOD_MOTE.get(), BloodMoteParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.HEAT_SHIMMER.get(), HeatShimmerParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.PILLAR_FLESH.get(), PillarParticle.Provider::chunk);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.PILLAR_BONE.get(), PillarParticle.Provider::chunk);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.PILLAR_STONE.get(), PillarParticle.Provider::chunk);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.PILLAR_GUST.get(), PillarParticle.Provider::gust);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.PILLAR_SAND.get(), PillarParticle.Provider::sand);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.PILLAR_STEAM.get(), PillarParticle.Provider::steam);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.PILLAR_BUBBLE.get(), PillarParticle.Provider::bubble);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.PILLAR_GLINT.get(), PillarParticle.Provider::glint);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.STAND_TRANSFORM.get(), StandTransformParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.IMPACT_RING.get(), ImpactRingParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.ANUBIS_SLICE.get(), AnubisSliceParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.STUN.get(), StunParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.GUARD_BREAK.get(), GuardBreakParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.HAMON_RIPPLE.get(), HamonParticle.RippleProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.BUBBLE_DRIFT.get(), HamonBubbleParticle.DriftProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.BUBBLE_SHOT.get(), HamonBubbleParticle.ShotProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.BUBBLE_SHEET.get(), BubbleSheetParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.BUBBLE_CUT.get(), BubbleCutParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.HAMON_SPARK.get(), HamonParticle.SparkProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.EMERALD_SPARKLE.get(), EmeraldSparkleParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.EMERALD_CHIP.get(), EmeraldChipParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.HAMON_MOTE.get(), HamonParticle.MoteProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.HAMON_SPARK_FINE.get(), HamonParticle.FineSparkProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.HAMON_MOTE_FINE.get(), HamonParticle.FineMoteProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.HAMON_AURA.get(), StandAuraParticle.HamonProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.HAMON_RING.get(), ImpactRingParticle.HamonProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.HAMON_RIPPLE_RAINBOW.get(), sprites -> RainbowHamon.wrap(new HamonParticle.RippleProvider(sprites)));
        ParticleFactoryRegistry.getInstance().register(ModRegistries.HAMON_SPARK_RAINBOW.get(), sprites -> RainbowHamon.wrap(new HamonParticle.SparkProvider(sprites)));
        ParticleFactoryRegistry.getInstance().register(ModRegistries.HAMON_MOTE_RAINBOW.get(), sprites -> RainbowHamon.wrap(new HamonParticle.MoteProvider(sprites)));
        ParticleFactoryRegistry.getInstance().register(ModRegistries.HAMON_SPARK_FINE_RAINBOW.get(), sprites -> RainbowHamon.wrap(new HamonParticle.FineSparkProvider(sprites)));
        ParticleFactoryRegistry.getInstance().register(ModRegistries.HAMON_MOTE_FINE_RAINBOW.get(), sprites -> RainbowHamon.wrap(new HamonParticle.FineMoteProvider(sprites)));
        ParticleFactoryRegistry.getInstance().register(ModRegistries.HAMON_AURA_RAINBOW.get(), sprites -> RainbowHamon.wrap(new StandAuraParticle.HamonProvider(sprites)));
        ParticleFactoryRegistry.getInstance().register(ModRegistries.HAMON_RING_RAINBOW.get(), sprites -> RainbowHamon.wrap(new ImpactRingParticle.HamonProvider(sprites)));
        ParticleFactoryRegistry.getInstance().register(ModRegistries.VAMPIRE_RING.get(), ImpactRingParticle.BloodProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.RIPPER_RING.get(), ImpactRingParticle.RipperProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.RIPPER_MIST.get(), BloodMoteParticle.RipperProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.INHALE_WIND.get(), InhaleWindParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.INHALE_SMOKE.get(), InhaleSmokeParticle.Provider::new);

        JojohaShaders.register();

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context ->
                WorldVfx.render(context.matrixStack(), context.camera(),
                        context.tickCounter().getGameTimeDeltaPartialTick(false)));

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            WorldVfx.solid(context.matrixStack(), context.camera(),
                    context.tickCounter().getGameTimeDeltaPartialTick(false));
            PilotBody.render(context.matrixStack(),
                    context.consumers(), context.camera(),
                    context.tickCounter().getGameTimeDeltaPartialTick(false));
        });

        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            TimeStopShader.render(deltaTracker.getGameTimeDeltaPartialTick(false));

            SkullCrushOverlay.render(guiGraphics,
                    guiGraphics.guiWidth(), guiGraphics.guiHeight(),
                    deltaTracker.getGameTimeDeltaPartialTick(false));
            CentralBarOverlay.render(guiGraphics);
            BarrageStruggleHud.render(guiGraphics);
            ComboCounter.render(guiGraphics);
            TimeStopChargeBar.render(guiGraphics);
            VampireColourOverlay.render(guiGraphics);
            SunScorchOverlay.render(guiGraphics);
            FortuneSense.render(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false));
            FamilySenseHud.render(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false));
            StandRitualText.render(guiGraphics);
            DreamSpirit.renderHud(guiGraphics);
            HeavenHud.render(guiGraphics);
            QuestTracker.render(guiGraphics);
            QuestBanner.render(guiGraphics);
            FactionBanner.render(guiGraphics);
            ScreenLanguage.render(guiGraphics);
            StingFx.renderHud(guiGraphics);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            GrappleController.tick();
            UltimateWings.tick();

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
        });
    }
}
