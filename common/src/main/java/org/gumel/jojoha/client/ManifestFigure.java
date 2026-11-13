package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandManifestation;
import org.gumel.jojoha.supporter.SupporterAuras;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

final class ManifestFigure {
    private static final Set<String> CORE_BONES = Set.of("Body", "upperTorso", "Head", "RightArm", "LeftArm",
            "RightLeg", "LeftLeg", "bone", "bone2", "bone3", "bone4");
    private static final String HEAD = "Head";
    private static final String RIG_PREFIX = "barrage";

    static final float FIGURE_HEIGHT = 2.0F;

    private static final float BODY_GLOW = 0.85F;
    private static final float POP_GLOW = 0.8F;
    private static final float SWEEP_EDGE = 0.25F;
    private static final float POP_SCALE = 0.35F;
    private static final float CLINK_VOLUME = 0.8F;
    private static final float CLINK_PITCH_FROM = 1.1F;
    private static final float CLINK_PITCH_RISE = 0.7F;

    private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Jojoha.MOD_ID, "textures/entity/stand/shared/stand_manifest_eye.png");
    private static final float EYES_IGNITE_TICKS = 6F;
    private static final float EYES_FADE_TICKS = 16F;
    private static final float EYE_LIFT = 0.002F;
    private static final float EYE_ROW_TOP = 4F / 8F;
    private static final float EYE_ROW_BOTTOM = 5F / 8F;
    private static final float[][] EYE_COLUMNS = {{1F / 8F, 3F / 8F}, {5F / 8F, 7F / 8F}};

    private static final Map<BakedGeoModel, Plan> PLANS = new WeakHashMap<>();

    private ManifestFigure() {
    }

    record Plan(Set<GeoCube> main, Map<GeoCube, Float> snapAt, GeoCube head, int[] beats) {
    }

    record EyeDraw(Matrix4f pose, Vector3f normal, GeoQuad face) {
    }

    static final class Look {
        final float ticks;
        final int eye;
        final List<EyeDraw> eyes = new ArrayList<>();
        Plan plan;
        boolean glowPass;

        Look(float ticks, int aura) {
            this.ticks = ticks;
            this.eye = aura;
        }

        boolean whole() {
            return ticks >= StandManifestation.FULL_AT;
        }

        boolean glows() {
            return ticks < StandManifestation.FULL_AT + StandManifestation.FLASH_TICKS
                    || ticks >= StandManifestation.DISSOLVE_AT;
        }

        int glow(float height, float flash) {
            float h = height / FIGURE_HEIGHT;
            float t = ticks;
            float g;

            if (t < StandManifestation.SKELETON_AT) {
                float m = 1F - materialized(t);
                g = Math.max(BODY_GLOW, m) * drained(m, h);
            } else if (t < StandManifestation.ARMOR_AT) {
                g = BODY_GLOW;
            } else if (t < StandManifestation.FULL_AT) {
                g = BODY_GLOW * (1F - filled(t));
            } else {
                g = 1F - Mth.clamp((t - StandManifestation.FULL_AT) / StandManifestation.FLASH_TICKS, 0F, 1F);
                if (t >= StandManifestation.DISSOLVE_AT) {
                    float d = dissolved(t);
                    g = Math.max(g, d) * drained(d, h);
                }
            }
            return Math.round(Mth.clamp(Math.max(g, flash * POP_GLOW), 0F, 1F) * 255F);
        }

        int light(int packedLight) {
            return whole() ? packedLight : LightTexture.FULL_BRIGHT;
        }

        float eyeGlow() {
            if (ticks < StandManifestation.SKELETON_AT) {
                return 0F;
            }
            float lit = Mth.clamp((ticks - StandManifestation.SKELETON_AT) / EYES_IGNITE_TICKS, 0F, 1F);
            float faded = Mth.clamp((ticks - StandManifestation.FULL_AT) / EYES_FADE_TICKS, 0F, 1F);
            return lit * (1F - faded);
        }

        int shade(float height, int colour, float flash, int[] overlayOut) {
            float h = height / FIGURE_HEIGHT;
            float t = ticks;
            float alpha;
            float white;

            if (t < StandManifestation.SKELETON_AT) {
                alpha = drained(1F - materialized(t), h);
                white = 1F;
            } else if (t < StandManifestation.ARMOR_AT) {
                alpha = 1F;
                white = 1F;
            } else if (t < StandManifestation.FULL_AT) {
                alpha = 1F;
                white = 1F - filled(t);
            } else {
                alpha = 1F;
                white = 1F - Mth.clamp((t - StandManifestation.FULL_AT) / StandManifestation.FLASH_TICKS, 0F, 1F);
                if (t >= StandManifestation.DISSOLVE_AT) {
                    float d = dissolved(t);
                    alpha *= drained(d, h);
                    white = Math.max(white, d);
                }
            }

            white = Math.max(white, flash);
            overlayOut[0] = OverlayTexture.pack(OverlayTexture.u(white), OverlayTexture.v(false));

            int baseAlpha = (colour >>> 24) & 0xFF;
            int a = Mth.clamp(Math.round(baseAlpha * alpha), 0, 255);
            return (a << 24) | (colour & 0xFFFFFF);
        }

        private static float materialized(float t) {
            return smooth((t - StandManifestation.EXIT_TICKS) / StandManifestation.MATERIALIZE_TICKS);
        }

        private static float filled(float t) {
            return smooth((t - StandManifestation.ARMOR_AT) / StandManifestation.FILL_TICKS);
        }

        private static float dissolved(float t) {
            return Mth.clamp((t - StandManifestation.DISSOLVE_AT) / StandManifestation.DISSOLVE_TICKS, 0F, 1F);
        }

        private static float drained(float d, float h) {
            float drain = (1F - d) * (1F + SWEEP_EDGE);
            return Mth.clamp((drain - h) / SWEEP_EDGE, 0F, 1F);
        }
    }

    static RenderType glowType() {
        return RenderType.eyes(WHITE_TEXTURE);
    }

    static Look look(StandEntity stand, float partialTick) {
        int aura = SupporterAuras.tint(stand.getOwnerUuid(),
                stand.getStandType().auraColorFor(stand.getSkin()));
        return new Look(stand.tickCount + partialTick, aura);
    }

    static Plan plan(BakedGeoModel model) {
        return PLANS.computeIfAbsent(model, ManifestFigure::measure);
    }

    public static void clinkPieces(Minecraft minecraft) {
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof StandEntity vision) || !vision.isManifesting()
                    || vision.tickCount < StandManifestation.SNAP_FROM || vision.tickCount > StandManifestation.SNAP_UNTIL
                    || !(minecraft.getEntityRenderDispatcher().getRenderer(vision) instanceof StandRenderer renderer)) {
                continue;
            }
            int[] beats = plan(renderer.getGeoModel().getBakedModel(renderer.getGeoModel().getModelResource(vision))).beats();
            for (int i = 0; i < beats.length; i++) {
                if (beats[i] == vision.tickCount) {
                    float through = beats.length == 1 ? 0F : i / (float) (beats.length - 1);
                    minecraft.level.playLocalSound(vision.getX(), vision.getY(), vision.getZ(), SoundEvents.CHAIN_PLACE,
                            SoundSource.PLAYERS, CLINK_VOLUME, CLINK_PITCH_FROM + through * CLINK_PITCH_RISE, false);
                }
            }
        }
    }

    static float snapPop(Look look, GeoCube cube, float[] flashOut) {
        if (look.whole() || look.plan == null || look.plan.main().contains(cube)) {
            flashOut[0] = 0F;
            return 1F;
        }

        Float at = look.plan.snapAt().get(cube);
        if (at == null || look.ticks < at) {
            return 0F;
        }

        float p = (look.ticks - at) / StandManifestation.SNAP_POP_TICKS;
        if (p >= 1F) {
            flashOut[0] = 0F;
            return 1F;
        }
        flashOut[0] = 1F - p;
        float left = 1F - p;
        return 1F + POP_SCALE * left * left;
    }

    static void noteHead(Look look, PoseStack poseStack, GeoBone bone) {
        if (look.glowPass || look.plan == null || look.plan.head() == null || !HEAD.equals(bone.getName())
                || look.eyeGlow() <= 0F || bone.isHidden()) {
            return;
        }

        GeoCube head = look.plan.head();
        GeoQuad face = null;
        for (GeoQuad quad : head.quads()) {
            if (quad != null && quad.direction() == Direction.NORTH) {
                face = quad;
            }
        }
        if (face == null) {
            return;
        }

        poseStack.pushPose();
        RenderUtil.translateToPivotPoint(poseStack, head);
        RenderUtil.rotateMatrixAroundCube(poseStack, head);
        RenderUtil.translateAwayFromPivotPoint(poseStack, head);
        Vector3f normal = poseStack.last().normal().transform(new Vector3f(face.normal()));
        look.eyes.add(new EyeDraw(new Matrix4f(poseStack.last().pose()), normal, face));
        poseStack.popPose();
    }

    static void drawEyes(Look look, MultiBufferSource buffers) {
        float glow = look.eyeGlow();
        if (look.eyes.isEmpty() || glow <= 0F) {
            return;
        }

        int colour = (Math.round(glow * 255F) << 24) | (look.eye & 0xFFFFFF);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));

        for (EyeDraw eye : look.eyes) {
            for (float[] columns : EYE_COLUMNS) {
                eyeQuad(buffer, eye, columns[0], columns[1], colour);
            }
        }
        look.eyes.clear();
    }

    private static void eyeQuad(VertexConsumer buffer, EyeDraw eye, float left, float right, int colour) {
        GeoVertex[] corners = eye.face().vertices();
        float minU = Float.MAX_VALUE;
        float maxU = -Float.MAX_VALUE;
        float minV = Float.MAX_VALUE;
        float maxV = -Float.MAX_VALUE;
        for (GeoVertex vertex : corners) {
            minU = Math.min(minU, vertex.texU());
            maxU = Math.max(maxU, vertex.texU());
            minV = Math.min(minV, vertex.texV());
            maxV = Math.max(maxV, vertex.texV());
        }

        Vector3f origin = null;
        Vector3f alongU = null;
        Vector3f alongV = null;
        for (GeoVertex vertex : corners) {
            boolean lowU = vertex.texU() - minU < (maxU - minU) * 0.5F;
            boolean lowV = vertex.texV() - minV < (maxV - minV) * 0.5F;
            if (lowU && lowV) {
                origin = new Vector3f(vertex.position());
            } else if (!lowU && lowV) {
                alongU = new Vector3f(vertex.position());
            } else if (lowU) {
                alongV = new Vector3f(vertex.position());
            }
        }
        if (origin == null || alongU == null || alongV == null) {
            return;
        }
        alongU.sub(origin);
        alongV.sub(origin);

        Vector3f lift = new Vector3f(eye.face().normal()).mul(EYE_LIFT);
        for (GeoVertex vertex : corners) {
            float s = vertex.texU() - minU < (maxU - minU) * 0.5F ? left : right;
            float t = vertex.texV() - minV < (maxV - minV) * 0.5F ? EYE_ROW_TOP : EYE_ROW_BOTTOM;
            Vector3f at = new Vector3f(origin).add(new Vector3f(alongU).mul(s)).add(new Vector3f(alongV).mul(t)).add(lift);
            Vector4f placed = eye.pose().transform(new Vector4f(at, 1F));
            buffer.addVertex(placed.x(), placed.y(), placed.z(), colour,
                    Mth.lerp(s, minU, maxU), Mth.lerp(t, minV, maxV),
                    OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT,
                    eye.normal().x(), eye.normal().y(), eye.normal().z());
        }
    }

    static Vector3f centre(GeoCube cube) {
        Vector3f sum = new Vector3f();
        int count = 0;
        for (GeoQuad quad : cube.quads()) {
            if (quad == null) {
                continue;
            }
            for (GeoVertex vertex : quad.vertices()) {
                sum.add(vertex.position());
                count++;
            }
        }
        return count == 0 ? sum : sum.div(count);
    }

    private static Plan measure(BakedGeoModel model) {
        Set<GeoCube> main = new HashSet<>();
        List<List<GeoCube>> groups = new ArrayList<>();
        GeoCube[] head = new GeoCube[1];

        for (GeoBone bone : model.topLevelBones()) {
            walk(bone, main, groups, head);
        }

        groups.sort(Comparator.comparingDouble(ManifestFigure::lowest));
        Map<GeoCube, Float> snapAt = new HashMap<>();
        int span = StandManifestation.SNAP_UNTIL - StandManifestation.SNAP_FROM;
        for (int i = 0; i < groups.size(); i++) {
            float through = groups.size() == 1 ? 0F : i / (float) (groups.size() - 1);
            float at = Math.round(StandManifestation.SNAP_FROM + span * through);
            for (GeoCube cube : groups.get(i)) {
                snapAt.put(cube, at);
            }
        }
        int[] beats = snapAt.values().stream().mapToInt(Math::round).distinct().sorted().toArray();
        return new Plan(main, snapAt, head[0], beats);
    }

    private static void walk(GeoBone bone, Set<GeoCube> main, List<List<GeoCube>> groups, GeoCube[] head) {
        if (bone.getName().startsWith(RIG_PREFIX)) {
            return;
        }

        if (CORE_BONES.contains(bone.getName())) {
            GeoCube body = largest(bone.getCubes());
            if (body != null) {
                main.add(body);
                if (HEAD.equals(bone.getName())) {
                    head[0] = body;
                }
            }
            for (GeoCube cube : bone.getCubes()) {
                if (cube != body) {
                    groups.add(new ArrayList<>(List.of(cube)));
                }
            }
            for (GeoBone child : bone.getChildBones()) {
                walk(child, main, groups, head);
            }
            return;
        }

        if (bone.getCubes().isEmpty()) {
            for (GeoBone child : bone.getChildBones()) {
                walk(child, main, groups, head);
            }
            return;
        }

        List<GeoCube> group = new ArrayList<>();
        gather(bone, group, main, groups, head);
        groups.add(group);
    }

    private static void gather(GeoBone bone, List<GeoCube> group, Set<GeoCube> main, List<List<GeoCube>> groups,
                               GeoCube[] head) {
        group.addAll(bone.getCubes());
        for (GeoBone child : bone.getChildBones()) {
            if (CORE_BONES.contains(child.getName())) {
                walk(child, main, groups, head);
            } else {
                gather(child, group, main, groups, head);
            }
        }
    }

    private static GeoCube largest(List<GeoCube> cubes) {
        GeoCube best = null;
        double bestVolume = -1.0;
        for (GeoCube cube : cubes) {
            if (cube.inflate() != 0.0) {
                continue;
            }
            double volume = cube.size().x * cube.size().y * cube.size().z;
            if (volume > bestVolume) {
                best = cube;
                bestVolume = volume;
            }
        }
        return best != null || cubes.isEmpty() ? best : cubes.get(0);
    }

    private static double lowest(List<GeoCube> group) {
        double low = Double.MAX_VALUE;
        for (GeoCube cube : group) {
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) {
                    continue;
                }
                for (GeoVertex vertex : quad.vertices()) {
                    low = Math.min(low, vertex.position().y());
                }
            }
        }
        return low;
    }

    private static float smooth(float x) {
        float c = Mth.clamp(x, 0F, 1F);
        return c * c * (3F - 2F * c);
    }
}
