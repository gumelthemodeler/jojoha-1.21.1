package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.client.fp.FirstPersonPoses;
import org.gumel.jojoha.combat.ClackerPhysics;
import org.gumel.jojoha.combat.Clackers;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.item.ClackersItem;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.ClackerSwingPacket;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ClackerClient {
    private static final String PREFIX = "clackers_";
    private static final int CHARGED = 0xFFFFE08A;
    private static final int GHOSTS = 3;
    private static final float GHOST_LEAD = 0.7F;
    private static final int GHOST_ALPHA = 0x78;
    private static final int GHOST_FADE = 0x20;
    private static final float STRING_ROLL = (float) (Math.PI / 4.0);

    private static final Map<UUID, Double> RENDER_YAW = new HashMap<>();
    private static final Map<Item, ResourceLocation> HEAD_TEXTURES = new HashMap<>();
    private static ModelPart head;
    private static ModelPart sidewaysPart;
    private static ModelPart facingPart;
    private static boolean bakeFailed;

    private ClackerClient() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Map<UUID, ClackerPhysics.State> states = Clackers.states(true);
        if (mc.level == null) {
            states.clear();
            RENDER_YAW.clear();
            ClackerHand.clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        for (AbstractClientPlayer player : mc.level.players()) {
            InteractionHand hand = Clackers.heldHand(player);
            Clackers.step(player, hand == null ? null : ClackerHand.forTick(player, hand));
            if (hand == null) {
                ClackerHand.forget(player.getUUID());
            }
        }
        states.keySet().removeIf(uuid -> mc.level.getPlayerByUUID(uuid) == null);
        RENDER_YAW.keySet().retainAll(states.keySet());
    }

    public static boolean attack() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator() || !(player.getMainHandItem().getItem() instanceof ClackersItem)) {
            return false;
        }
        if (Clackers.throwFrom(player)) {
            FirstPersonPoses.play(MovementFx.CLACKER_HIT);
            NetworkHandler.sendClackerThrow();
        }
        return true;
    }

    public static void spun() {
        FirstPersonPoses.play(MovementFx.CLACKER_SPIN);
    }

    public static boolean holdsBack() {
        Player player = Minecraft.getInstance().player;
        return player != null && player.getMainHandItem().getItem() instanceof ClackersItem;
    }

    public static void heard(int playerId, int kind, int targetId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(playerId);
        if (!(entity instanceof Player player)) {
            return;
        }
        ClackerPhysics.State state = Clackers.states(true).get(player.getUUID());
        if (state == null) {
            return;
        }
        if (kind == ClackerSwingPacket.VOLLEY) {
            ClackerPhysics.volley(state, targetId);
        } else if (player == mc.player) {
            return;
        } else if (kind == ClackerSwingPacket.SPIN) {
            ClackerPhysics.spin(state, player);
        } else if (state.hand == InteractionHand.MAIN_HAND) {
            ClackerPhysics.windUp(state);
        }
    }

    public static void render(PoseStack pose, MultiBufferSource buffers, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Map<UUID, ClackerPhysics.State> states = Clackers.states(true);
        if (mc.level == null || states.isEmpty() || !bake()) {
            return;
        }
        Vec3 cam = camera.getPosition();
        List<RenderType> used = new ArrayList<>();
        RenderType sideways = use(used, RenderType.entityCutout(ClackerModels.STRING_TEXTURE));
        RenderType facing = use(used, RenderType.entityCutoutNoCull(ClackerModels.STRING_TEXTURE));
        RenderType ghostString = use(used, RenderType.entityTranslucent(ClackerModels.STRING_TEXTURE));
        for (AbstractClientPlayer player : mc.level.players()) {
            ClackerPhysics.State state = states.get(player.getUUID());
            if (state == null || player.isInvisible() || Clackers.heldHand(player) != state.hand) {
                continue;
            }
            ResourceLocation texture = headTexture(player.getItemInHand(state.hand).getItem());
            RenderType headType = use(used, RenderType.entityCutoutNoCull(texture));
            Vec3 anchor = ClackerHand.resolve(player, state.hand, camera, partialTick);
            double yaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
            double lastYaw = RENDER_YAW.getOrDefault(player.getUUID(), yaw);
            float correction = (float) Math.toRadians(-Mth.wrapDegrees(yaw - lastYaw));
            RENDER_YAW.put(player.getUUID(), yaw);
            Vec3 right = new Vec3(-Math.cos(Math.toRadians(yaw)), 0.0, -Math.sin(Math.toRadians(yaw)));
            boolean charged = state.volleying();
            int ballColour = charged ? CHARGED : -1;

            for (int b = 0; b < state.heads.length; b++) {
                ClackerPhysics.Head ball = state.heads[b];
                Vec3 at = anchor.add(lerp(partialTick, ball.prevPos, ball.pos).subtract(anchor).yRot(correction));
                List<Vec3> curve = new ArrayList<>(ClackerPhysics.STRING_NODES);
                curve.add(anchor);
                if (ball.stringReady) {
                    for (int i = 1; i < ClackerPhysics.STRING_NODES - 1; i++) {
                        Vec3 node = lerp(partialTick, ball.stringPrev[i], ball.string[i]);
                        curve.add(anchor.add(node.subtract(anchor).yRot(correction)));
                    }
                }
                curve.add(at);
                int light = charged ? LightTexture.FULL_BRIGHT : light(mc, at);
                Quaternionf end = string(pose, cam, curve, right, b * STRING_ROLL, mc, charged,
                        buffers, sideways, facing);
                place(pose, cam, at, end);
                head.render(pose, buffers.getBuffer(headType), light, OverlayTexture.NO_OVERLAY, ballColour);
                pose.popPose();
            }

            Entity target = charged ? mc.level.getEntity(state.volleyTarget) : null;
            if (target != null) {
                RenderType ghostHead = use(used, RenderType.entityTranslucent(texture));
                float age = state.volleyAge + partialTick;
                for (int b = 0; b < state.heads.length; b++) {
                    for (int g = 1; g <= GHOSTS; g++) {
                        Vec3 at = ClackerPhysics.volleyPoint(b, age + g * GHOST_LEAD, anchor, target);
                        int colour = ghostColour(g);
                        ghostString(pose, cam, anchor, at, buffers, ghostString, colour);
                        place(pose, cam, at, new Quaternionf());
                        head.render(pose, buffers.getBuffer(ghostHead), LightTexture.FULL_BRIGHT,
                                OverlayTexture.NO_OVERLAY, colour);
                        pose.popPose();
                    }
                }
            }
        }
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            for (RenderType type : used) {
                source.endBatch(type);
            }
        }
    }

    private static RenderType use(List<RenderType> used, RenderType type) {
        if (!used.contains(type)) {
            used.add(type);
        }
        return type;
    }

    private static int ghostColour(int ghost) {
        int alpha = Math.max(0x20, GHOST_ALPHA - (ghost - 1) * GHOST_FADE);
        return alpha << 24 | (CHARGED & 0xFFFFFF);
    }

    private static Quaternionf string(PoseStack pose, Vec3 cam, List<Vec3> curve, Vec3 right, float roll,
                                      Minecraft mc, boolean charged, MultiBufferSource buffers,
                                      RenderType sideways, RenderType facing) {
        Vec3 first = curve.get(curve.size() - 1).subtract(curve.get(0));
        Vec3 fallback = first.lengthSqr() < 1.0E-8 ? new Vec3(0.0, -1.0, 0.0) : first.normalize();
        Vector3f side = null;
        Vector3f along = toVector(fallback);
        int colour = charged ? CHARGED : -1;
        for (Vec3[] link : links(curve, ClackerModels.LINK_LENGTH, fallback)) {
            along = toVector(link[1]);
            if (side == null) {
                side = across(toVector(right), along);
                side.rotateAxis(roll, along.x(), along.y(), along.z());
            }
            side = across(side, along);
            int light = charged ? LightTexture.FULL_BRIGHT : light(mc, link[0]);
            place(pose, cam, link[0], frame(side, along));
            pose.scale(1F, (float) link[2].x / ClackerModels.LINK_LENGTH, 1F);
            sidewaysPart.render(pose, buffers.getBuffer(sideways), light, OverlayTexture.NO_OVERLAY, colour);
            facingPart.render(pose, buffers.getBuffer(facing), light, OverlayTexture.NO_OVERLAY, colour);
            pose.popPose();
        }
        if (side == null) {
            side = across(toVector(right), along);
        }
        return frame(side, along);
    }

    private static void ghostString(PoseStack pose, Vec3 cam, Vec3 from, Vec3 to, MultiBufferSource buffers,
                                    RenderType type, int colour) {
        Vec3 span = to.subtract(from);
        double length = span.length();
        if (length < 0.05) {
            return;
        }
        Vector3f along = toVector(span.scale(1.0 / length));
        Vector3f toCamera = toVector(cam.subtract(from.add(span.scale(0.5))));
        Vector3f side = new Vector3f(along).cross(toCamera);
        side = side.lengthSquared() < 1.0E-6F ? across(new Vector3f(1F, 0F, 0F), along) : side.normalize();
        int pieces = Math.max(1, (int) Math.round(length / ClackerModels.LINK_LENGTH));
        double per = length / pieces;
        for (int i = 0; i < pieces; i++) {
            place(pose, cam, from.add(span.scale((i + 0.5) / pieces)), frame(side, along));
            pose.scale(1F, (float) per / ClackerModels.LINK_LENGTH, 1F);
            facingPart.render(pose, buffers.getBuffer(type), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, colour);
            pose.popPose();
        }
    }

    private static Vector3f toVector(Vec3 v) {
        return new Vector3f((float) v.x, (float) v.y, (float) v.z);
    }

    private static Vector3f across(Vector3f reference, Vector3f along) {
        Vector3f side = new Vector3f(reference).sub(new Vector3f(along).mul(reference.dot(along)));
        if (side.lengthSquared() < 1.0E-6F) {
            Vector3f spare = Math.abs(along.x()) < 0.9F ? new Vector3f(1F, 0F, 0F) : new Vector3f(0F, 0F, 1F);
            side = spare.sub(new Vector3f(along).mul(spare.dot(along)));
        }
        return side.normalize();
    }

    private static Quaternionf frame(Vector3f side, Vector3f along) {
        Vector3f depth = new Vector3f(side).cross(along).normalize();
        Matrix3f basis = new Matrix3f(side.x(), side.y(), side.z(), along.x(), along.y(), along.z(),
                depth.x(), depth.y(), depth.z());
        return new Quaternionf().setFromNormalized(basis);
    }

    private static void place(PoseStack pose, Vec3 cam, Vec3 at, Quaternionf turn) {
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        pose.mulPose(turn);
        pose.scale(-1F, -1F, 1F);
    }

    private static int light(Minecraft mc, Vec3 at) {
        return LevelRenderer.getLightColor(mc.level, BlockPos.containing(at));
    }

    private static Vec3 lerp(float t, Vec3 from, Vec3 to) {
        return new Vec3(Mth.lerp(t, from.x, to.x), Mth.lerp(t, from.y, to.y), Mth.lerp(t, from.z, to.z));
    }

    private static ResourceLocation headTexture(Item item) {
        return HEAD_TEXTURES.computeIfAbsent(item, it -> {
            String path = BuiltInRegistries.ITEM.getKey(it).getPath();
            return ClackerModels.headTexture(path.startsWith(PREFIX) ? path.substring(PREFIX.length()) : "white");
        });
    }

    private static List<Vec3[]> links(List<Vec3> curve, double segment, Vec3 fallback) {
        int n = curve.size();
        double[] along = new double[n];
        double total = 0.0;
        for (int i = 1; i < n; i++) {
            total += curve.get(i).distanceTo(curve.get(i - 1));
            along[i] = total;
        }
        List<Vec3[]> out = new ArrayList<>();
        if (total < 0.02) {
            return out;
        }
        int count = Math.max(1, (int) Math.round(total / segment));
        double per = total / count;
        int seg = 1;
        for (int i = 0; i < count; i++) {
            double s = (i + 0.5) * per;
            while (seg < n - 1 && along[seg] < s) {
                seg++;
            }
            double start = along[seg - 1];
            double length = along[seg] - start;
            double f = length < 1.0E-6 ? 0.0 : (s - start) / length;
            Vec3 a = curve.get(seg - 1);
            Vec3 b = curve.get(seg);
            Vec3 dir = b.subtract(a);
            out.add(new Vec3[]{a.add(dir.scale(f)), dir.lengthSqr() < 1.0E-8 ? fallback : dir.normalize(),
                    new Vec3(per, 0.0, 0.0)});
        }
        return out;
    }

    private static boolean bake() {
        if (bakeFailed) {
            return false;
        }
        if (head != null) {
            return true;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            head = mc.getEntityModels().bakeLayer(ClackerModels.HEAD);
            ModelPart string = mc.getEntityModels().bakeLayer(ClackerModels.STRING);
            sidewaysPart = string.getChild(ClackerModels.SIDEWAYS);
            facingPart = string.getChild(ClackerModels.FACING);
            return true;
        } catch (IllegalArgumentException e) {
            bakeFailed = true;
            return false;
        }
    }
}
