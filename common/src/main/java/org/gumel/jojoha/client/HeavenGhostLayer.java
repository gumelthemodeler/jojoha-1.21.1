package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.heaven.Heaven;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class HeavenGhostLayer extends GeoRenderLayer<StandEntity> {
    private static final int GHOST_TICKS = 8;
    private static final int MAX_GHOSTS = 8;
    private static final double SPACING = 0.45;
    private static final double CLEARANCE = 0.35;
    private static final float RUSH_ALPHA = 0.55F;
    private static final float HIGH_ALPHA = 0.4F;

    private static final float MARK_ALPHA = 0.7F;
    private static final int MARK_POP_TICKS = 3;
    private static final float GHOST_GLOW = 0.35F;
    private static final int GHOST_TINT = 0xFFFFFF;
    private static final float[] SHELL_SCALES = {1.04F, 1.1F};
    private static final float[] SHELL_STRENGTHS = {0.5F, 0.16F};
    private static final float[] TIER_GLOW = {0.55F, 0.75F, 0.95F, 1.2F};
    private static final float FLARE_GLOW = 1.1F;
    private static final String FACE_BONE = "Head";
    private static final float HEAD_CENTRE = 0.86F;
    private static final float[] HEAD_SCALES = {1.08F, 1.2F};
    private static final float[] HEAD_STRENGTHS = {0.45F, 0.14F};
    private static final float FACING_FROM = 0.45F;
    private static final float FACING_TO = 0.9F;
    private static final float FACE_DIM = 0.25F;

    private static final Map<Integer, Trail> TRAILS = new HashMap<>();
    private static final Map<Integer, Map<Integer, Mark>> MARKS = new HashMap<>();

    public HeavenGhostLayer(GeoRenderer<StandEntity> renderer) {
        super(renderer);
    }

    public static void rush(int standId, int ticks) {
        TRAILS.computeIfAbsent(standId, id -> new Trail()).rushLeft = ticks;
    }

    public static Vec3 mark(int standId, int index, Vec3 at, float yaw, int ticks) {
        Map<Integer, Mark> marks = MARKS.computeIfAbsent(standId, id -> new HashMap<>());
        if (index == 0) {
            marks.clear();
        }
        Mark before = marks.get(index - 1);
        marks.put(index, new Mark(at, yaw, ticks));
        return before == null ? null : before.at;
    }

    public static Vec3 shatter(int standId, int index) {
        Map<Integer, Mark> marks = MARKS.get(standId);
        Mark mark = marks == null ? null : marks.remove(index);
        return mark == null ? null : mark.at;
    }

    public static void clear() {
        TRAILS.clear();
        MARKS.clear();
    }

    public static void tick(ClientLevel level) {
        Iterator<Map.Entry<Integer, Map<Integer, Mark>>> marked = MARKS.entrySet().iterator();
        while (marked.hasNext()) {
            Map.Entry<Integer, Map<Integer, Mark>> entry = marked.next();
            entry.getValue().values().removeIf(mark -> ++mark.age > mark.life);
            if (entry.getValue().isEmpty() || !(level.getEntity(entry.getKey()) instanceof StandEntity)) {
                marked.remove();
            }
        }
        Iterator<Map.Entry<Integer, Trail>> iterator = TRAILS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Trail> entry = iterator.next();
            Trail trail = entry.getValue();
            if (trail.rushLeft > 0) {
                trail.rushLeft--;
            }
            trail.ghosts.removeIf(ghost -> ++ghost.age > GHOST_TICKS);
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof StandEntity stand) || stand.isRemoved()) {
                iterator.remove();
                continue;
            }
            if (!high(stand) && trail.rushLeft <= 0 && trail.ghosts.isEmpty()) {
                iterator.remove();
            }
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof StandEntity stand && high(stand)) {
                TRAILS.computeIfAbsent(stand.getId(), id -> new Trail());
            }
        }
    }

    private static boolean high(StandEntity stand) {
        return Heaven.ID.equals(stand.getStandType().id()) && HeavenWorldFx.high(stand);
    }

    @Override
    public void render(PoseStack poseStack, StandEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        if (!Heaven.ID.equals(animatable.getStandType().id())) {
            return;
        }
        float view = StandViewAlpha.of(animatable);
        if (view <= 0.001F) {
            return;
        }
        shells(poseStack, animatable, bakedModel, bufferSource, partialTick, view);
        marks(poseStack, animatable, bakedModel, bufferSource, partialTick, view);
        Trail trail = TRAILS.get(animatable.getId());
        if (trail == null) {
            return;
        }
        boolean high = high(animatable);
        boolean rushing = trail.rushLeft > 0;
        long gameTime = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        if ((high || rushing) && gameTime != trail.lastCapture) {
            Ghost last = trail.ghosts.peekFirst();
            Vec3 here = animatable.position();
            if (last == null || last.at.distanceToSqr(here) >= SPACING * SPACING) {
                trail.lastCapture = gameTime;
                trail.ghosts.addFirst(new Ghost(here, rushing ? HeavenWorldFx.GOLD : HeavenWorldFx.VIOLET));
                while (trail.ghosts.size() > MAX_GHOSTS) {
                    trail.ghosts.removeLast();
                }
            }
        }
        if (trail.ghosts.isEmpty()) {
            return;
        }
        Vec3 now = animatable.getPosition(partialTick);
        RenderType ghostType = RenderType.entityTranslucent(getTextureResource(animatable));
        RenderType glowType = RenderType.eyes(getTextureResource(animatable));
        for (Ghost ghost : trail.ghosts) {
            Vec3 offset = ghost.at.subtract(now);
            if (offset.lengthSqr() < CLEARANCE * CLEARANCE) {
                continue;
            }
            float fade = 1F - (ghost.age + partialTick) / (GHOST_TICKS + 1F);
            float peak = ghost.colour == HeavenWorldFx.GOLD ? RUSH_ALPHA : HIGH_ALPHA;
            float alpha = Mth.clamp(fade * peak * view, 0F, 1F);
            if (alpha <= 0.01F) {
                continue;
            }
            int colour = (Math.round(alpha * 255F) << 24) | GHOST_TINT;
            poseStack.pushPose();
            poseStack.translate(offset.x, offset.y, offset.z);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, ghostType,
                    bufferSource.getBuffer(ghostType), partialTick, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, colour);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glowType,
                    bufferSource.getBuffer(glowType), partialTick, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, HeavenWorldFx.scale(ghost.colour, alpha * GHOST_GLOW));
            poseStack.popPose();
        }
    }

    private void marks(PoseStack poseStack, StandEntity animatable, BakedGeoModel bakedModel,
                       MultiBufferSource bufferSource, float partialTick, float view) {
        Map<Integer, Mark> marks = MARKS.get(animatable.getId());
        if (marks == null || marks.isEmpty()) {
            return;
        }
        Vec3 now = animatable.getPosition(partialTick);
        float body = Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);
        RenderType ghostType = RenderType.entityTranslucent(getTextureResource(animatable));
        RenderType glowType = RenderType.eyes(getTextureResource(animatable));
        for (Mark mark : marks.values()) {
            float pop = Math.min(1F, (mark.age + partialTick) / MARK_POP_TICKS);
            float alpha = Mth.clamp(pop * MARK_ALPHA * view, 0F, 1F);
            if (alpha <= 0.01F) {
                continue;
            }
            Vec3 offset = mark.at.subtract(now);
            poseStack.pushPose();
            poseStack.translate(offset.x, offset.y, offset.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(body - mark.yaw));
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, ghostType,
                    bufferSource.getBuffer(ghostType), partialTick, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, (Math.round(alpha * 255F) << 24) | GHOST_TINT);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glowType,
                    bufferSource.getBuffer(glowType), partialTick, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, HeavenWorldFx.scale(HeavenWorldFx.GOLD, alpha * GHOST_GLOW));
            poseStack.popPose();
        }
    }

    private void shells(PoseStack poseStack, StandEntity animatable, BakedGeoModel bakedModel,
                        MultiBufferSource bufferSource, float partialTick, float view) {
        if (!HeavenWorldFx.heavenly(animatable)) {
            return;
        }
        boolean high = HeavenWorldFx.high(animatable);
        float time = animatable.tickCount + partialTick;
        float flare = HeavenWorldFx.telegraphOf(animatable, partialTick);
        float breath = (0.88F + 0.12F * Mth.sin(time * 0.1F)) * TIER_GLOW[HeavenWorldFx.tierOf(animatable)]
                + FLARE_GLOW * flare;
        RenderType glow = RenderType.eyes(HeavenWorldFx.FLAT);
        GeoBone face = bakedModel.getBone(FACE_BONE).orElse(null);
        List<GeoBone> bones = new ArrayList<>();
        for (GeoBone top : bakedModel.topLevelBones()) {
            collect(top, bones);
        }
        boolean[] hidden = new boolean[bones.size()];
        boolean[] hiding = new boolean[bones.size()];
        for (int i = 0; i < bones.size(); i++) {
            hidden[i] = bones.get(i).isHidden();
            hiding[i] = bones.get(i).isHidingChildren();
        }
        try {
            if (face != null) {
                face.setHidden(true);
            }
            shellPasses(poseStack, animatable, bakedModel, bufferSource, partialTick, high, time, breath * view,
                    animatable.getBbHeight() * 0.5F, glow, SHELL_SCALES, SHELL_STRENGTHS, false);
            if (face != null) {
                restore(bones, hidden, hiding);
                isolate(face, bones);
                shellPasses(poseStack, animatable, bakedModel, bufferSource, partialTick, high, time, breath * view,
                        animatable.getBbHeight() * HEAD_CENTRE, glow, HEAD_SCALES, HEAD_STRENGTHS, true);
            }
        } finally {
            restore(bones, hidden, hiding);
        }
    }

    private static void collect(GeoBone bone, List<GeoBone> into) {
        into.add(bone);
        for (GeoBone child : bone.getChildBones()) {
            collect(child, into);
        }
    }

    private static void restore(List<GeoBone> bones, boolean[] hidden, boolean[] hiding) {
        for (int i = 0; i < bones.size(); i++) {
            bones.get(i).setHidden(hidden[i]);
            bones.get(i).setChildrenHidden(hiding[i]);
        }
    }

    private static void isolate(GeoBone face, List<GeoBone> bones) {
        List<GeoBone> keep = new ArrayList<>();
        collect(face, keep);
        List<GeoBone> path = new ArrayList<>();
        for (GeoBone up = face.getParent(); up != null; up = up.getParent()) {
            path.add(up);
        }
        for (GeoBone bone : bones) {
            if (keep.contains(bone)) {
                continue;
            }
            bone.setHidden(true);
            if (path.contains(bone)) {
                bone.setChildrenHidden(false);
            }
        }
    }

    private void shellPasses(PoseStack poseStack, StandEntity animatable, BakedGeoModel bakedModel,
                             MultiBufferSource bufferSource, float partialTick, boolean high, float time,
                             float strength, float centre, RenderType glow, float[] scales, float[] strengths,
                             boolean trimFace) {
        for (int i = 0; i < scales.length; i++) {
            float grow = scales[i] + 0.006F * Mth.sin(time * 0.12F + i);
            int colour = HeavenWorldFx.scale(high ? HeavenWorldFx.VIOLET : HeavenWorldFx.GOLD,
                    strengths[i] * strength);
            Inside inside = new Inside(bufferSource.getBuffer(glow), trimFace);
            poseStack.pushPose();
            poseStack.translate(0F, centre, 0F);
            poseStack.scale(grow, grow, grow);
            poseStack.translate(0F, -centre, 0F);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow, inside, partialTick,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, colour);
            inside.end();
            poseStack.popPose();
        }
    }

    private static final class Inside implements VertexConsumer {
        private final VertexConsumer out;
        private final boolean trimFace;
        private final float[][] held = new float[4][];
        private float[] current;
        private int count;

        private Inside(VertexConsumer out, boolean trimFace) {
            this.out = out;
            this.trimFace = trimFace;
        }

        private float faceShade() {
            float cx = 0F;
            float cy = 0F;
            float cz = 0F;
            for (float[] v : held) {
                cx += v[0];
                cy += v[1];
                cz += v[2];
            }
            float[] n = held[0];
            float along = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
            float normal = (float) Math.sqrt(n[13] * n[13] + n[14] * n[14] + n[15] * n[15]);
            if (along < 1.0E-5F || normal < 1.0E-5F) {
                return 1F;
            }
            float facing = (cx * n[13] + cy * n[14] + cz * n[15]) / (along * normal);
            float t = Mth.clamp((facing - FACING_FROM) / (FACING_TO - FACING_FROM), 0F, 1F);
            return Mth.lerp(t, 1F, FACE_DIM);
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            push();
            current = new float[16];
            current[0] = x;
            current[1] = y;
            current[2] = z;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            current[3] = red;
            current[4] = green;
            current[5] = blue;
            current[6] = alpha;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            current[7] = u;
            current[8] = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            current[9] = u;
            current[10] = v;
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            current[11] = u;
            current[12] = v;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            current[13] = x;
            current[14] = y;
            current[15] = z;
            return this;
        }

        private void push() {
            if (current == null) {
                return;
            }
            held[count++] = current;
            current = null;
            if (count == 4) {
                float shade = trimFace ? faceShade() : 1F;
                for (int i = 3; i >= 0; i--) {
                    float[] v = held[i];
                    out.addVertex(v[0], v[1], v[2])
                            .setColor((int) (v[3] * shade), (int) (v[4] * shade), (int) (v[5] * shade), (int) v[6])
                            .setUv(v[7], v[8])
                            .setUv1((int) v[9], (int) v[10])
                            .setUv2((int) v[11], (int) v[12])
                            .setNormal(v[13], v[14], v[15]);
                }
                count = 0;
            }
        }

        private void end() {
            push();
            count = 0;
        }
    }

    private static final class Mark {
        private final Vec3 at;
        private final float yaw;
        private final int life;
        private int age;

        private Mark(Vec3 at, float yaw, int life) {
            this.at = at;
            this.yaw = yaw;
            this.life = life;
        }
    }

    private static final class Trail {
        private final Deque<Ghost> ghosts = new ArrayDeque<>();
        private int rushLeft;
        private long lastCapture = Long.MIN_VALUE;
    }

    private static final class Ghost {
        private final Vec3 at;
        private final int colour;
        private int age;

        private Ghost(Vec3 at, int colour) {
            this.at = at;
            this.colour = colour;
        }
    }
}
