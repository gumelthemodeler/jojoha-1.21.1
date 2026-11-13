package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.client.HermitSkins;
import org.gumel.jojoha.client.ThornRope;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(EntityRenderer.class)
public abstract class LeashRopeMixin {
    private static final double STAND_SEARCH = 12.0;

    private static final double SLACK = 0.55;

    @Inject(method = "renderLeash(Lnet/minecraft/world/entity/Entity;F"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
            + "Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"), cancellable = true)
    private void jojoha$thornLeash(Entity leashed, float partialTick, PoseStack poseStack,
                                   MultiBufferSource buffers, Entity holder, CallbackInfo ci) {
        if (!(holder instanceof Player player) || !jojoha$hasHermitPurple(player)) {
            return;
        }

        Vec3 from = jojoha$at(leashed, partialTick)
                .add(0, leashed.getBbHeight() * 0.6, 0);

        Vec3 to = player.getRopeHoldPosition(partialTick);

        Vec3 origin = jojoha$at(leashed, partialTick);
        Vec3 start = from.subtract(origin);
        Vec3 end = to.subtract(origin);

        poseStack.pushPose();
        poseStack.translate(start.x, start.y, start.z);

        Vec3 span = end.subtract(start);
        ThornRope.draw(poseStack, buffers, leashed.level(), span, from, span.length() + SLACK,
                HermitSkins.of(player));

        poseStack.popPose();
        ci.cancel();
    }

    private static Vec3 jojoha$at(Entity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }

    private static boolean jojoha$hasHermitPurple(Player player) {
        if (player == Minecraft.getInstance().player) {
            var data = ClientPlayerDataCache.data;
            return data != null && data.stand.isPresent()
                    && StandTypes.HERMIT_PURPLE_ID.equals(data.stand.standId());
        }

        List<StandEntity> stands = player.level().getEntitiesOfClass(StandEntity.class,
                player.getBoundingBox().inflate(STAND_SEARCH),
                stand -> stand.getOwnerUuid().filter(player.getUUID()::equals).isPresent());

        for (StandEntity stand : stands) {
            if (StandTypes.HERMIT_PURPLE_ID.equals(stand.getStandType().id())) {
                return true;
            }
        }
        return false;
    }
}
