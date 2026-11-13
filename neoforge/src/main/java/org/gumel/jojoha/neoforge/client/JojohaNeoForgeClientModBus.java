package org.gumel.jojoha.neoforge.client;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.AnubisSliceParticle;
import org.gumel.jojoha.client.BloodMoteParticle;
import org.gumel.jojoha.client.BubbleCutParticle;
import org.gumel.jojoha.client.BubbleSheetParticle;
import org.gumel.jojoha.client.EmeraldChipParticle;
import org.gumel.jojoha.client.EmeraldSparkleParticle;
import org.gumel.jojoha.client.GuardBreakParticle;
import org.gumel.jojoha.client.HamonBubbleParticle;
import org.gumel.jojoha.client.HamonParticle;
import org.gumel.jojoha.client.HeatShimmerParticle;
import org.gumel.jojoha.client.ImpactRingParticle;
import org.gumel.jojoha.client.InhaleSmokeParticle;
import org.gumel.jojoha.client.InhaleWindParticle;
import org.gumel.jojoha.client.JojohaShaders;
import org.gumel.jojoha.client.ModEntityRenderers;
import org.gumel.jojoha.client.ModItemProperties;
import org.gumel.jojoha.client.RainbowHamon;
import org.gumel.jojoha.client.RawKey;
import org.gumel.jojoha.client.StandAuraParticle;
import org.gumel.jojoha.client.StandAwakenParticle;
import org.gumel.jojoha.client.StandTransformParticle;
import org.gumel.jojoha.client.StrataFaces;
import org.gumel.jojoha.client.StunParticle;
import org.gumel.jojoha.client.TimeStopMoteParticle;
import org.gumel.jojoha.client.pillar.PillarParticle;
import org.gumel.jojoha.registry.ModRegistries;

@EventBusSubscriber(modid = Jojoha.MOD_ID, value = Dist.CLIENT)
public final class JojohaNeoForgeClientModBus {
    private JojohaNeoForgeClientModBus() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        JojohaShaders.register();

        event.enqueueWork(() -> {
            ModEntityRenderers.init();
            ModItemProperties.init();
        });
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((location, model) ->
                StrataFaces.wraps(location) ? new NeoForgeStrataModel(model) : model);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(JojohaNeoForgeClient.OPEN_STATS_KEY);
        event.register(JojohaNeoForgeClient.TOGGLE_COMBAT_PAGE_KEY);
        event.register(JojohaNeoForgeClient.TOGGLE_COMBAT_BAR_KEY);
        event.register(JojohaNeoForgeClient.SUMMON_STAND_KEY);
        event.register(JojohaNeoForgeClient.SKILL_MODIFIER_KEY);
        RawKey.setModifier(JojohaNeoForgeClient.SKILL_MODIFIER_KEY);
        for (KeyMapping skillKey : JojohaNeoForgeClient.STAND_SKILL_KEYS) {
            event.register(skillKey);
        }
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModRegistries.STAND_AURA.get(), StandAuraParticle.Provider::new);
        event.registerSpriteSet(ModRegistries.TIMESTOP_MOTE.get(), TimeStopMoteParticle.Provider::new);
        event.registerSpriteSet(ModRegistries.STAND_AWAKEN.get(), StandAwakenParticle.Provider::new);
        event.registerSpriteSet(ModRegistries.STAND_AWAKEN_RED.get(), StandAwakenParticle.RedProvider::new);
        event.registerSpriteSet(ModRegistries.STAND_AWAKEN_BLUE.get(), StandAwakenParticle.BlueProvider::new);
        event.registerSpriteSet(ModRegistries.STAND_AWAKEN_PINK.get(), StandAwakenParticle.PinkProvider::new);
        event.registerSpriteSet(ModRegistries.BLOOD_MOTE.get(), BloodMoteParticle.Provider::new);
        event.registerSpriteSet(ModRegistries.HEAT_SHIMMER.get(), HeatShimmerParticle.Provider::new);
        event.registerSpriteSet(ModRegistries.PILLAR_FLESH.get(), PillarParticle.Provider::chunk);
        event.registerSpriteSet(ModRegistries.PILLAR_BONE.get(), PillarParticle.Provider::chunk);
        event.registerSpriteSet(ModRegistries.PILLAR_STONE.get(), PillarParticle.Provider::chunk);
        event.registerSpriteSet(ModRegistries.PILLAR_GUST.get(), PillarParticle.Provider::gust);
        event.registerSpriteSet(ModRegistries.PILLAR_SAND.get(), PillarParticle.Provider::sand);
        event.registerSpriteSet(ModRegistries.PILLAR_STEAM.get(), PillarParticle.Provider::steam);
        event.registerSpriteSet(ModRegistries.PILLAR_BUBBLE.get(), PillarParticle.Provider::bubble);
        event.registerSpriteSet(ModRegistries.PILLAR_GLINT.get(), PillarParticle.Provider::glint);
        event.registerSpriteSet(ModRegistries.STAND_TRANSFORM.get(), StandTransformParticle.Provider::new);
        event.registerSpriteSet(ModRegistries.IMPACT_RING.get(), ImpactRingParticle.Provider::new);
        event.registerSpriteSet(ModRegistries.ANUBIS_SLICE.get(), AnubisSliceParticle.Provider::new);
        event.registerSpriteSet(ModRegistries.STUN.get(), StunParticle.Provider::new);
        event.registerSpriteSet(ModRegistries.GUARD_BREAK.get(), GuardBreakParticle.Provider::new);
        event.registerSpriteSet(ModRegistries.HAMON_RIPPLE.get(), HamonParticle.RippleProvider::new);
        event.registerSpriteSet(ModRegistries.BUBBLE_DRIFT.get(), HamonBubbleParticle.DriftProvider::new);
        event.registerSpriteSet(ModRegistries.BUBBLE_SHOT.get(), HamonBubbleParticle.ShotProvider::new);
        event.registerSpriteSet(ModRegistries.BUBBLE_SHEET.get(), BubbleSheetParticle.Provider::new);
        event.registerSpriteSet(ModRegistries.BUBBLE_CUT.get(), BubbleCutParticle.Provider::new);
        event.registerSpriteSet(ModRegistries.HAMON_SPARK.get(), HamonParticle.SparkProvider::new);
        event.registerSpriteSet(ModRegistries.EMERALD_SPARKLE.get(), EmeraldSparkleParticle.Provider::new);
        event.registerSpriteSet(ModRegistries.EMERALD_CHIP.get(), EmeraldChipParticle.Provider::new);
        event.registerSpriteSet(ModRegistries.HAMON_MOTE.get(), HamonParticle.MoteProvider::new);
        event.registerSpriteSet(ModRegistries.HAMON_SPARK_FINE.get(), HamonParticle.FineSparkProvider::new);
        event.registerSpriteSet(ModRegistries.HAMON_MOTE_FINE.get(), HamonParticle.FineMoteProvider::new);
        event.registerSpriteSet(ModRegistries.HAMON_AURA.get(), StandAuraParticle.HamonProvider::new);
        event.registerSpriteSet(ModRegistries.HAMON_RING.get(), ImpactRingParticle.HamonProvider::new);
        event.registerSpriteSet(ModRegistries.HAMON_RIPPLE_RAINBOW.get(), sprites -> RainbowHamon.wrap(new HamonParticle.RippleProvider(sprites)));
        event.registerSpriteSet(ModRegistries.HAMON_SPARK_RAINBOW.get(), sprites -> RainbowHamon.wrap(new HamonParticle.SparkProvider(sprites)));
        event.registerSpriteSet(ModRegistries.HAMON_MOTE_RAINBOW.get(), sprites -> RainbowHamon.wrap(new HamonParticle.MoteProvider(sprites)));
        event.registerSpriteSet(ModRegistries.HAMON_SPARK_FINE_RAINBOW.get(), sprites -> RainbowHamon.wrap(new HamonParticle.FineSparkProvider(sprites)));
        event.registerSpriteSet(ModRegistries.HAMON_MOTE_FINE_RAINBOW.get(), sprites -> RainbowHamon.wrap(new HamonParticle.FineMoteProvider(sprites)));
        event.registerSpriteSet(ModRegistries.HAMON_AURA_RAINBOW.get(), sprites -> RainbowHamon.wrap(new StandAuraParticle.HamonProvider(sprites)));
        event.registerSpriteSet(ModRegistries.HAMON_RING_RAINBOW.get(), sprites -> RainbowHamon.wrap(new ImpactRingParticle.HamonProvider(sprites)));
        event.registerSpriteSet(ModRegistries.VAMPIRE_RING.get(), ImpactRingParticle.BloodProvider::new);
        event.registerSpriteSet(ModRegistries.RIPPER_RING.get(), ImpactRingParticle.RipperProvider::new);
        event.registerSpriteSet(ModRegistries.RIPPER_MIST.get(), BloodMoteParticle.RipperProvider::new);
        event.registerSpriteSet(ModRegistries.INHALE_WIND.get(), InhaleWindParticle.Provider::new);
        event.registerSpriteSet(ModRegistries.INHALE_SMOKE.get(), InhaleSmokeParticle.Provider::new);
    }
}
