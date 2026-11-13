package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import org.gumel.jojoha.client.WorldEventSky;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class VampireNightFogMixin {
    @Shadow
    private static float fogRed;
    @Shadow
    private static float fogGreen;
    @Shadow
    private static float fogBlue;

    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void jojoha$bloodFog(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                        float darken, CallbackInfo ci) {
        float tint = WorldEventSky.blood(level, partialTick) * WorldEventSky.FOG_TINT;
        if (tint <= 0.001F) {
            return;
        }
        fogRed = Mth.lerp(tint, fogRed, WorldEventSky.FOG_RED);
        fogGreen = Mth.lerp(tint, fogGreen, WorldEventSky.FOG_GREEN);
        fogBlue = Mth.lerp(tint, fogBlue, WorldEventSky.FOG_BLUE);
        RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
    }
}
