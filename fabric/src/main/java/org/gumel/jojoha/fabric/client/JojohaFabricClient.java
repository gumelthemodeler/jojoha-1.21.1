package org.gumel.jojoha.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import org.gumel.jojoha.client.CombatBarOverlay;
import org.gumel.jojoha.client.LocalStandAuraEffect;
import org.gumel.jojoha.client.ModEntityRenderers;
import org.gumel.jojoha.client.ImpactRingParticle;
import org.gumel.jojoha.client.InhaleSmokeParticle;
import org.gumel.jojoha.client.InhaleWindParticle;
import org.gumel.jojoha.client.StandAuraParticle;
import org.gumel.jojoha.client.StandAwakenParticle;
import org.gumel.jojoha.client.StandTransformParticle;
import org.gumel.jojoha.client.StandCombatInput;
import org.gumel.jojoha.client.StandGlowTracker;
import org.gumel.jojoha.client.StandRitualEffects;
import org.gumel.jojoha.client.StandRitualText;
import org.gumel.jojoha.client.StandSkillInput;
import org.gumel.jojoha.client.TimeStopShader;
import org.gumel.jojoha.client.StandSummonSound;
import org.gumel.jojoha.hamon.moves.RipplePulseMove;
import org.gumel.jojoha.network.NetworkHandler;
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
    public static final KeyMapping GUARD_STAND_KEY = new KeyMapping(
            "key.jojoha.guard_stand", GLFW.GLFW_KEY_F, "key.categories.jojoha");

    /**
     * The five Stand skill keys, which reach eight slots.
     *
     * <p>Z Y C V B plainly, and the first three again with shift held for slots six to eight - see
     * StandSkillInput. Five bindings rather than eight because eight was three more keys than the
     * game had spare, and the three it was taking were ones players expect to do something else.
     */
    public static final KeyMapping[] STAND_SKILL_KEYS = {
            new KeyMapping("key.jojoha.stand_skill_1", GLFW.GLFW_KEY_Z, "key.categories.jojoha"),
            new KeyMapping("key.jojoha.stand_skill_2", GLFW.GLFW_KEY_Y, "key.categories.jojoha"),
            new KeyMapping("key.jojoha.stand_skill_3", GLFW.GLFW_KEY_C, "key.categories.jojoha"),
            new KeyMapping("key.jojoha.stand_skill_4", GLFW.GLFW_KEY_V, "key.categories.jojoha"),
            new KeyMapping("key.jojoha.stand_skill_5", GLFW.GLFW_KEY_B, "key.categories.jojoha"),
    };

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        KeyBindingHelper.registerKeyBinding(OPEN_STATS_KEY);
        KeyBindingHelper.registerKeyBinding(TOGGLE_COMBAT_PAGE_KEY);
        KeyBindingHelper.registerKeyBinding(TOGGLE_COMBAT_BAR_KEY);
        KeyBindingHelper.registerKeyBinding(SUMMON_STAND_KEY);
        KeyBindingHelper.registerKeyBinding(GUARD_STAND_KEY);
        for (KeyMapping skillKey : STAND_SKILL_KEYS) {
            KeyBindingHelper.registerKeyBinding(skillKey);
        }

        ModEntityRenderers.init();
        org.gumel.jojoha.client.ModItemProperties.init();
        // Architectury's ParticleProviderRegistry is currently broken on Fabric - registers
        // without error but the provider never actually gets wired up, so particles silently
        // never render (see https://github.com/architectury/architectury-api/issues/621).
        // Using Fabric's native registry directly instead.
        ParticleFactoryRegistry.getInstance().register(ModRegistries.STAND_AURA.get(), StandAuraParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.TIMESTOP_MOTE.get(), org.gumel.jojoha.client.TimeStopMoteParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.STAND_AWAKEN.get(), StandAwakenParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.STAND_AWAKEN_RED.get(), StandAwakenParticle.RedProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.STAND_AWAKEN_BLUE.get(), StandAwakenParticle.BlueProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.STAND_AWAKEN_PINK.get(), StandAwakenParticle.PinkProvider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.BLOOD_MOTE.get(), org.gumel.jojoha.client.BloodMoteParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.STAND_TRANSFORM.get(), StandTransformParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.IMPACT_RING.get(), ImpactRingParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.STUN.get(), org.gumel.jojoha.client.StunParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.TANGLED_THORN.get(), org.gumel.jojoha.client.TangledThornParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.GUARD_BREAK.get(), org.gumel.jojoha.client.GuardBreakParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.INHALE_WIND.get(), InhaleWindParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.INHALE_SMOKE.get(), InhaleSmokeParticle.Provider::new);

        // The time stop is graded over the finished frame rather than drawn into it, so it goes
        // last - after particles, weather and everything else that would otherwise keep its colour
        // and give the edge of the stop away.
        org.gumel.jojoha.client.JojohaShaders.register();
        WorldRenderEvents.LAST.register(context -> {
            org.gumel.jojoha.client.TimeStopPost.render(
                    context.positionMatrix(), context.projectionMatrix(), context.camera(),
                    context.tickCounter().getGameTimeDeltaPartialTick(false));

            // And after it. The impact frame is the last word on the frame by definition - it takes
            // the colour out of everything, including whatever the time stop just put in.
            org.gumel.jojoha.client.ImpactFramePost.render();
        });

        // Same pass as the sphere: after translucent terrain, so the box sits behind glass and
        // water rather than over them.
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context ->
                org.gumel.jojoha.client.StandPlacePreview.render(context.matrixStack(),
                        context.consumers(), context.camera(),
                        context.tickCounter().getGameTimeDeltaPartialTick(false)));

        // Same pass, same reason: depth-tested lines belong with the terrain in front of them.
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            float delta = context.tickCounter().getGameTimeDeltaPartialTick(false);
            org.gumel.jojoha.client.GrappleTarget.render(context.matrixStack(),
                    context.consumers(), context.camera(), delta);
            org.gumel.jojoha.client.ThornLashFx.render(context.matrixStack(),
                    context.consumers(), context.camera(), delta);
            org.gumel.jojoha.client.LootSense.render(context.matrixStack(),
                    context.consumers(), context.camera(), delta);
        });

        // The pilot's own body, which vanilla refuses to draw once the camera is on the Stand.
        // See PilotBody - it draws nothing on a loader that already did.
        WorldRenderEvents.AFTER_ENTITIES.register(context ->
                org.gumel.jojoha.client.PilotBody.render(context.matrixStack(),
                        context.consumers(), context.camera(),
                        context.tickCounter().getGameTimeDeltaPartialTick(false)));


        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            TimeStopShader.render(deltaTracker.getGameTimeDeltaPartialTick(false));

            // Before the bars, so the interface stays readable while the world behind it dims.
            org.gumel.jojoha.client.SkullCrushOverlay.render(guiGraphics,
                    guiGraphics.guiWidth(), guiGraphics.guiHeight(),
                    deltaTracker.getGameTimeDeltaPartialTick(false));
            org.gumel.jojoha.client.CentralBarOverlay.render(guiGraphics);
            org.gumel.jojoha.client.TimeStopChargeBar.render(guiGraphics);
            org.gumel.jojoha.client.VampireColourOverlay.render(guiGraphics);
            StandRitualText.render(guiGraphics);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // The rope, before anything reads the player's motion this tick.
            org.gumel.jojoha.client.GrappleController.tick();

            if (org.gumel.jojoha.client.RawKey.pressed(OPEN_STATS_KEY)) {
                if (client.player != null && client.screen == null) {
                    client.setScreen(new org.gumel.jojoha.client.PlayerMenuScreen());
                }
            }

            if (org.gumel.jojoha.client.RawKey.pressed(TOGGLE_COMBAT_PAGE_KEY)) {
                if (client.player != null) {
                    CombatBarOverlay.togglePage();
                }
            }

            if (org.gumel.jojoha.client.RawKey.pressed(TOGGLE_COMBAT_BAR_KEY)) {
                if (client.player != null) {
                    CombatBarOverlay.toggleVisibility();
                }
            }


            LocalStandAuraEffect.tick();
            StandSummonSound.tickClient();
            StandRitualEffects.tick();
            StandGlowTracker.tick();
            org.gumel.jojoha.client.LootSense.tick();
            org.gumel.jojoha.client.GrappleTarget.tick();
            org.gumel.jojoha.client.ThornLashFx.tick();
            org.gumel.jojoha.client.StandBarrageAudio.tick();
            StandCombatInput.tick(GUARD_STAND_KEY, SUMMON_STAND_KEY);
            org.gumel.jojoha.client.StandStretch.tick();
            StandSkillInput.tick(STAND_SKILL_KEYS);
            org.gumel.jojoha.client.PilotPose.tick(net.minecraft.client.Minecraft.getInstance());
            org.gumel.jojoha.client.SkullFlashFx.tick();
            org.gumel.jojoha.client.PhantomAmbience.tick();
            org.gumel.jojoha.client.CombatBarOverlay.tickClientState();
        });
    }
}
