package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import org.gumel.jojoha.client.HeavenSky;
import org.gumel.jojoha.client.MagicianHeat;
import org.gumel.jojoha.client.PhantomAmbience;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class MagicianFogMixin {
    @Shadow
    private static float fogRed;
    @Shadow
    private static float fogGreen;
    @Shadow
    private static float fogBlue;

    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void jojoha$heavenTint(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                          float darken, CallbackInfo ci) {
        float[] tinted = HeavenSky.fog(fogRed, fogGreen, fogBlue, partialTick);
        if (tinted == null) {
            return;
        }
        fogRed = tinted[0];
        fogGreen = tinted[1];
        fogBlue = tinted[2];
        RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
    }

    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void jojoha$heatTint(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                        float darken, CallbackInfo ci) {
        float tint = MagicianHeat.heat(partialTick) * MagicianHeat.FOG_TINT;
        if (tint <= 0.001F) {
            return;
        }
        fogRed = Mth.lerp(tint, fogRed, MagicianHeat.FOG_RED);
        fogGreen = Mth.lerp(tint, fogGreen, MagicianHeat.FOG_GREEN);
        fogBlue = Mth.lerp(tint, fogBlue, MagicianHeat.FOG_BLUE);
        RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
    }

    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void jojoha$hollowMist(Camera camera, FogRenderer.FogMode mode, float farPlane, boolean thick,
                                          float partialTick, CallbackInfo ci) {
        if (mode != FogRenderer.FogMode.FOG_TERRAIN || thick) {
            return;
        }
        float mist = PhantomAmbience.mist(partialTick);
        if (mist <= 0.001F) {
            return;
        }
        RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() * (1F - mist * PhantomAmbience.MIST_START_PULL));
        RenderSystem.setShaderFogEnd(RenderSystem.getShaderFogEnd() * (1F - mist * PhantomAmbience.MIST_END_PULL));
    }

    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void jojoha$heatHaze(Camera camera, FogRenderer.FogMode mode, float farPlane, boolean thick,
                                        float partialTick, CallbackInfo ci) {
        if (mode != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        float pull = Math.max(MagicianHeat.heat(partialTick) * MagicianHeat.FOG_PULL,
                PhantomAmbience.haze(partialTick) * PhantomAmbience.HAZE_PULL);
        if (pull <= 0.001F) {
            return;
        }
        RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() * (1F - pull));
        RenderSystem.setShaderFogEnd(RenderSystem.getShaderFogEnd() * (1F - pull * 0.6F));
    }
}
