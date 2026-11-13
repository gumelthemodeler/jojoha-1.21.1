package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.client.fx.NearFade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin extends Particle implements NearFade.Marked {
    @Unique
    private boolean jojoha$marked;

    @Unique
    private float jojoha$drawnAlpha = -1F;

    protected SingleQuadParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Override
    public void jojoha$fadeNear() {
        this.jojoha$marked = true;
    }

    @Unique
    private boolean jojoha$fades() {
        return this.jojoha$marked || this.getClass().getName().startsWith("org.gumel.jojoha.");
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void jojoha$thinNearCamera(VertexConsumer buffer, Camera camera, float partialTick, CallbackInfo ci) {
        this.jojoha$drawnAlpha = -1F;
        if (!jojoha$fades()) {
            return;
        }
        Vec3 eye = camera.getPosition();
        float kept = NearFade.kept(eye, this.x, this.y, this.z);
        if (kept < 1F) {
            this.jojoha$drawnAlpha = this.alpha;
            this.alpha *= kept;
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void jojoha$restoreAlpha(VertexConsumer buffer, Camera camera, float partialTick, CallbackInfo ci) {
        if (this.jojoha$drawnAlpha >= 0F) {
            this.alpha = this.jojoha$drawnAlpha;
            this.jojoha$drawnAlpha = -1F;
        }
    }
}
