package org.gumel.jojoha.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.level.ModBiomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class PhantomSkyMixin {
    private static final Vec3 JOJOHA$NIGHT = new Vec3(0.043, 0.055, 0.180);

    private static final Vec3 JOJOHA$BONE_NIGHT = new Vec3(0.145, 0.055, 0.215);

    private static final double JOJOHA$SAMPLE_DROP = 2.0;

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void jojoha$phantomNight(Vec3 pos, float partialTick,
                                     CallbackInfoReturnable<Vec3> cir) {
        ClientLevel level = (ClientLevel) (Object) this;

        BlockPos at = BlockPos.containing(pos.x, pos.y - JOJOHA$SAMPLE_DROP, pos.z);

        Vec3 target;
        if (level.getBiome(at).is(ModBiomes.IS_MOORS)) {
            target = JOJOHA$NIGHT;
        } else if (level.getBiome(at).is(ModBiomes.IS_BONELANDS)) {
            target = JOJOHA$BONE_NIGHT;
        } else {
            return;
        }

        float day = Mth.clamp(Mth.cos(level.getTimeOfDay(partialTick) * Mth.TWO_PI) * 2F + 0.5F,
                0F, 1F);
        float night = 1F - day;
        if (night <= 0.001F) {
            return;
        }

        cir.setReturnValue(cir.getReturnValue().lerp(target, night));
    }
}
