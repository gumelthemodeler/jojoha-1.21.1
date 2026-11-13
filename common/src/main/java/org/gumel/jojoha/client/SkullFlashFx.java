package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.SkullBlock;
import org.gumel.jojoha.network.packet.SkullFlashPacket;
import org.gumel.jojoha.stand.skill.moves.SkullCrusherSkill;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class SkullFlashFx {
    private static final ResourceLocation SKULL_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

    private static final float SKULL_HEIGHT = 0.5F;

    private static final float SKULL_SCALE = 0.92F;

    private static final float HEAD_LIFT = 0.08F;

    private static final float BODY_ALPHA_OPEN = 0.22F;

    private static final float BODY_RETURN_AT = 0.8F;

    private static final float POP_OUT = SkullCrusherSkill.POP_AT;

    private static final float POP_TICKS =
            SkullCrusherSkill.POP_TICKS;
    private static final float POP_SWELL = 1.3F;
    private static final float POP_SWELL_AT = 0.3F;

    private static final float RATTLE_MAX = 0.032F;
    private static final float RATTLE_FREQ = 3.1F;

    private static final float SHAKE_YAW = 0.85F;
    private static final float SHAKE_PITCH = 0.55F;

    private static final float KICK_DEGREES = 3.6F;
    private static final int KICK_TICKS = 5;

    private static final int FLASH_TICKS = 3;

    private static final List<Flash> ACTIVE = new ArrayList<>();

    private static Map<SkullBlock.Type, SkullModelBase> models;

    private static HumanoidModel<LivingEntity> bones;

    private SkullFlashFx() {
    }

    public static float bodyAlpha(LivingEntity entity) {
        for (int i = 0; i < ACTIVE.size(); i++) {
            Flash flash = ACTIVE.get(i);
            if (flash.victimId != entity.getId()) {
                continue;
            }

            if (flash.phase == SkullFlashPacket.WINDUP) {
                return -1F;
            }

            float progress = 1F - flash.share();
            return progress < BODY_RETURN_AT ? BODY_ALPHA_OPEN : -1F;
        }
        return -1F;
    }

    public static void begin(int victimId, int attackerId, int phase, int ticks) {
        ACTIVE.removeIf(flash -> flash.victimId == victimId);
        ACTIVE.add(new Flash(victimId, attackerId, phase, ticks));

        if (phase == SkullFlashPacket.SHATTER) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null
                    && (minecraft.player.getId() == attackerId
                        || minecraft.player.getId() == victimId)) {
                ImpactFrame.begin(1F);
            }
        }
    }

    public static void tick() {
        Iterator<Flash> it = ACTIVE.iterator();
        while (it.hasNext()) {
            if (--it.next().remaining <= 0) {
                it.remove();
            }
        }
    }

    public static float focus(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0F;
        }

        for (int i = 0; i < ACTIVE.size(); i++) {
            Flash flash = ACTIVE.get(i);
            if (flash.attackerId != minecraft.player.getId()) {
                continue;
            }

            float progress = 1F - flash.share(partialTick);
            if (flash.phase == SkullFlashPacket.WINDUP) {
                return progress;
            }
            return progress < BODY_RETURN_AT ? 1F
                    : 1F - (progress - BODY_RETURN_AT) / (1F - BODY_RETURN_AT);
        }
        return 0F;
    }

    public static float flash(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0F;
        }

        for (int i = 0; i < ACTIVE.size(); i++) {
            Flash flash = ACTIVE.get(i);
            if (flash.attackerId != minecraft.player.getId()
                    || flash.phase != SkullFlashPacket.SHATTER) {
                continue;
            }

            float elapsedTicks = (1F - flash.share(partialTick)) * flash.ticks;
            if (elapsedTicks >= FLASH_TICKS) {
                return 0F;
            }

            float left = 1F - elapsedTicks / FLASH_TICKS;
            return left * left;
        }
        return 0F;
    }

    public static float[] cameraShake() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }

        for (Flash flash : ACTIVE) {
            if (flash.attackerId != minecraft.player.getId()) {
                continue;
            }

            float time = minecraft.player.tickCount + flash.remaining;

            if (flash.phase == SkullFlashPacket.WINDUP) {
                float weight = 1F - flash.share();
                return new float[]{
                        Mth.sin(time * 2.7F) * SHAKE_YAW * weight,
                        Mth.cos(time * 3.4F) * SHAKE_PITCH * weight};
            }

            float elapsedTicks = (1F - flash.share()) * flash.ticks;
            if (elapsedTicks >= KICK_TICKS) {
                return null;
            }

            float decay = 1F - elapsedTicks / KICK_TICKS;
            float bite = decay * decay;
            float sign = (flash.remaining % 2 == 0) ? 1F : -1F;
            return new float[]{
                    sign * KICK_DEGREES * bite,
                    sign * KICK_DEGREES * 0.45F * bite};
        }
        return null;
    }

    public static void renderInside(LivingEntity victim, PoseStack poseStack,
                                    MultiBufferSource buffers, int light, float partialTick) {
        Flash flash = null;
        for (int i = 0; i < ACTIVE.size(); i++) {
            if (ACTIVE.get(i).victimId == victim.getId()) {
                flash = ACTIVE.get(i);
                break;
            }
        }
        if (flash == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (models == null) {
            models = SkullBlockRenderer.createSkullRenderers(minecraft.getEntityModels());
        }

        SkullModelBase model = models.get(SkullBlock.Types.SKELETON);
        if (model == null) {
            return;
        }

        RenderType bone = RenderType.entitySolid(SKULL_TEXTURE);

        float yaw = Mth.rotLerp(partialTick, victim.yHeadRotO, victim.yHeadRot) + 180F;

        float base = victim.getEyeHeight() + HEAD_LIFT - SKULL_HEIGHT * SKULL_SCALE / 2F;

        poseStack.pushPose();
        poseStack.translate(0F, base, 0F);

        if (flash.phase == SkullFlashPacket.WINDUP) {
            windup(poseStack, buffers, model, bone, yaw, flash, partialTick, light);
        } else {
            shatter(poseStack, buffers, model, bone, yaw, flash, partialTick, light);
        }

        poseStack.popPose();
    }

    private static void windup(PoseStack poseStack, MultiBufferSource buffers, SkullModelBase model,
                               RenderType through, float yaw, Flash flash, float partialTick,
                               int light) {
        draw(poseStack, buffers, model, through, yaw, SKULL_SCALE, light);
    }

    private static void shatter(PoseStack poseStack, MultiBufferSource buffers, SkullModelBase model,
                                RenderType through, float yaw, Flash flash, float partialTick,
                                int light) {
        float progress = 1F - flash.share(partialTick);
        if (progress >= POP_OUT) {
            return;
        }

        float elapsed = progress * flash.ticks;
        float popAt = POP_OUT * flash.ticks - POP_TICKS;

        float pop;
        if (elapsed < popAt) {
            pop = 1F;

            if (popAt > 0F) {
                float build = elapsed / popAt;
                float shake = RATTLE_MAX * build * build;
                poseStack.translate(
                        Mth.sin(elapsed * RATTLE_FREQ) * shake,
                        Mth.sin(elapsed * RATTLE_FREQ * 1.37F) * shake * 0.55F,
                        Mth.cos(elapsed * RATTLE_FREQ * 1.11F) * shake);
            }
        } else {
            float u = Math.min(1F, (elapsed - popAt) / POP_TICKS);
            pop = u < POP_SWELL_AT
                    ? Mth.lerp(u / POP_SWELL_AT, 1F, POP_SWELL)
                    : Mth.lerp((u - POP_SWELL_AT) / (1F - POP_SWELL_AT), POP_SWELL, 0F);
        }

        if (pop <= 0.02F) {
            return;
        }

        draw(poseStack, buffers, model, through, yaw, SKULL_SCALE * pop, light);
    }

    private static void draw(PoseStack poseStack, MultiBufferSource buffers, SkullModelBase model,
                             RenderType through, float yaw, float scale, int light) {
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);

        poseStack.translate(-0.5F, 0F, -0.5F);

        SkullBlockRenderer.renderSkull(null, yaw, 0F, poseStack, buffers,
                LightTexture.FULL_BRIGHT, model, through);
        poseStack.popPose();
    }

    private static final class Flash {
        private final int victimId;
        private final int attackerId;
        private final int phase;
        private final int ticks;
        private int remaining;

        private Flash(int victimId, int attackerId, int phase, int ticks) {
            this.victimId = victimId;
            this.attackerId = attackerId;
            this.phase = phase;
            this.ticks = Math.max(1, ticks);
            this.remaining = this.ticks;
        }

        private float share() {
            return remaining / (float) ticks;
        }

        private float share(float partialTick) {
            return Mth.clamp((remaining - partialTick) / ticks, 0F, 1F);
        }
    }
    public static void renderSkeleton(HumanoidModel<?> source, PoseStack poseStack,
                                      MultiBufferSource buffers, int light, int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        if (bones == null) {
            bones = new HumanoidModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.SKELETON));

            bones.head.visible = false;
            bones.hat.visible = false;
        }

        bones.body.copyFrom(source.body);
        bones.rightArm.copyFrom(source.rightArm);
        bones.leftArm.copyFrom(source.leftArm);
        bones.rightLeg.copyFrom(source.rightLeg);
        bones.leftLeg.copyFrom(source.leftLeg);

        bones.young = source.young;

        bones.renderToBuffer(poseStack,
                buffers.getBuffer(RenderType.entityCutoutNoCull(SKULL_TEXTURE)),
                light, overlay, 0xFFFFFFFF);
    }

}
