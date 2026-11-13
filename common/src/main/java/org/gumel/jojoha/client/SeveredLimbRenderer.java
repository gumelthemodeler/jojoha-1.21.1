package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.gumel.jojoha.body.Limbs;
import org.gumel.jojoha.body.SeveredLimb;

import java.util.UUID;

public class SeveredLimbRenderer extends EntityRenderer<SeveredLimb> {
    private static final float LIFT = 0.125F;
    private static final float ARM_MIDDLE_X = 1F, ARM_MIDDLE_Y = 4F, LEG_MIDDLE_Y = 6F;
    private static final float SHADOW = 0.2F;

    private record Look(ModelPart part, ModelPart outer, ResourceLocation texture, float middleX, float middleY) {
    }

    public SeveredLimbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = SHADOW;
    }

    @Override
    public void render(SeveredLimb limb, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        Look look = look(limb);
        if (look != null) {
            Gore.limb(limb);
            poseStack.pushPose();
            poseStack.translate(0F, LIFT - limb.sink(partialTick), 0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-limb.getYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(limb.spin(partialTick)));
            poseStack.scale(-1F, -1F, 1F);
            poseStack.translate(-look.middleX() / 16F, -look.middleY() / 16F, 0F);
            draw(look.part(), look, poseStack, bufferSource, packedLight);
            if (look.outer() != null) {
                draw(look.outer(), look, poseStack, bufferSource, packedLight);
            }
            LimbRegrowLayer.cutEnd(poseStack, bufferSource, look.part(), packedLight);
            poseStack.popPose();
        }
        super.render(limb, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void draw(ModelPart part, Look look, PoseStack poseStack, MultiBufferSource buffers, int light) {
        PartPose saved = part.storePose();
        boolean shown = part.visible;
        part.setPos(0F, 0F, 0F);
        part.setRotation(0F, 0F, 0F);
        part.visible = true;
        part.render(poseStack, buffers.getBuffer(RenderType.entityTranslucent(look.texture())), light,
                OverlayTexture.NO_OVERLAY);
        part.loadPose(saved);
        part.visible = shown;
    }

    private static Look look(SeveredLimb limb) {
        if (limb.look instanceof Look cached) {
            return cached;
        }
        Look found = resolve(limb);
        if (found != null) {
            limb.look = found;
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    private static Look resolve(SeveredLimb limb) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return null;
        }
        Entity body = level.getEntity(limb.source());
        UUID owner = limb.owner().orElse(null);
        String kind = EntityType.getKey(body == null ? EntityType.PIG : body.getType()).toString();
        ResourceLocation texture = null;
        if (body == null || !kind.equals(limb.kind())) {
            body = owner == null ? null : level.getPlayerByUUID(owner);
        }
        if (body == null && owner != null) {
            body = minecraft.player;
            texture = DefaultPlayerSkin.get(owner).texture();
        }
        if (body == null) {
            body = EntityType.byString(limb.kind()).map(type -> type.create(level)).orElse(null);
        }
        if (body == null) {
            return null;
        }
        EntityRenderer<Entity> renderer = (EntityRenderer<Entity>) minecraft.getEntityRenderDispatcher().getRenderer(body);
        if (!(renderer instanceof LivingEntityRenderer<?, ?> living) || !(living.getModel() instanceof HumanoidModel<?> model)) {
            return null;
        }
        if (texture == null) {
            texture = renderer.getTextureLocation(body);
        }
        PlayerModel<?> player = model instanceof PlayerModel<?> wide ? wide : null;
        return switch (limb.limb()) {
            case Limbs.RIGHT_ARM -> new Look(model.rightArm, player == null ? null : player.rightSleeve, texture,
                    -ARM_MIDDLE_X, ARM_MIDDLE_Y);
            case Limbs.LEFT_ARM -> new Look(model.leftArm, player == null ? null : player.leftSleeve, texture,
                    ARM_MIDDLE_X, ARM_MIDDLE_Y);
            case Limbs.RIGHT_LEG -> new Look(model.rightLeg, player == null ? null : player.rightPants, texture,
                    0F, LEG_MIDDLE_Y);
            default -> new Look(model.leftLeg, player == null ? null : player.leftPants, texture, 0F, LEG_MIDDLE_Y);
        };
    }

    @Override
    public ResourceLocation getTextureLocation(SeveredLimb limb) {
        return limb.look instanceof Look look ? look.texture() : DefaultPlayerSkin.getDefaultTexture();
    }
}
