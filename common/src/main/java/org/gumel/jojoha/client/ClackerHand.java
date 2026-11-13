package org.gumel.jojoha.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.ClackerPhysics;
import org.gumel.jojoha.item.ClackersItem;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClackerHand {
    private static final long FRESH_NANOS = 150_000_000L;
    private static final long OFFSET_NANOS = 500_000_000L;
    private static final double MAX_DRIFT = 1.5;
    private static final float FIST_SIDE = 6F / 16F;
    private static final float FIST_DOWN = 11F / 16F;

    private record Grip(Vec3 at, HumanoidArm arm, long nanos) {
    }

    private static final Map<UUID, Grip> SEEN = new HashMap<>();
    private static final Map<UUID, Grip> OFFSETS = new HashMap<>();

    private static final Matrix4f HANDS_BASE = new Matrix4f();
    private static Vector3f fist;
    private static HumanoidArm fistArm;
    private static float fistProjection;
    private static long fistNanos;

    private ClackerHand() {
    }

    public static HumanoidArm arm(Player player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }

    public static void heldInThirdPerson(LivingEntity entity, ItemStack stack, HumanoidArm arm, PoseStack pose) {
        if (!(entity instanceof Player player) || !(stack.getItem() instanceof ClackersItem)) {
            return;
        }
        Vector3f at = pose.last().pose().transformPosition(0F, 0F, 0F, new Vector3f());
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        SEEN.put(player.getUUID(), new Grip(camera.add(at.x(), at.y(), at.z()), arm, System.nanoTime()));
    }

    public static void handsBegin(PoseStack pose) {
        HANDS_BASE.set(pose.last().pose());
    }

    public static void heldInFirstPerson(PoseStack pose, HumanoidArm arm) {
        Vector3f at = new Vector3f(arm == HumanoidArm.RIGHT ? -FIST_SIDE : FIST_SIDE, FIST_DOWN, 0F);
        pose.last().pose().transformPosition(at);
        new Matrix4f(HANDS_BASE).invert().transformPosition(at);
        fist = at;
        fistArm = arm;
        fistProjection = RenderSystem.getProjectionMatrix().m11();
        fistNanos = System.nanoTime();
    }

    public static Vec3 resolve(Player player, InteractionHand hand, Camera camera, float partialTick) {
        Vec3 body = ClackerPhysics.anchor(player, hand, partialTick);
        HumanoidArm arm = arm(player, hand);
        long now = System.nanoTime();
        Vec3 grip = body;
        if (player == Minecraft.getInstance().getCameraEntity() && !camera.isDetached()) {
            if (fist != null && fistArm == arm && now - fistNanos < FRESH_NANOS) {
                float world = RenderSystem.getProjectionMatrix().m11();
                float spread = world > 1.0E-4F ? fistProjection / world : 1F;
                Vector3f view = new Vector3f(fist.x() * spread, fist.y() * spread, fist.z());
                camera.rotation().transform(view);
                grip = camera.getPosition().add(view.x(), view.y(), view.z());
            }
        } else {
            Grip seen = SEEN.get(player.getUUID());
            if (seen != null && seen.arm() == arm && now - seen.nanos() < FRESH_NANOS
                    && seen.at().distanceTo(body) < MAX_DRIFT) {
                grip = seen.at();
            }
        }
        OFFSETS.put(player.getUUID(), new Grip(grip.subtract(player.getPosition(partialTick)), arm, now));
        return grip;
    }

    public static Vec3 forTick(Player player, InteractionHand hand) {
        Grip offset = OFFSETS.get(player.getUUID());
        if (offset == null || offset.arm() != arm(player, hand) || System.nanoTime() - offset.nanos() > OFFSET_NANOS) {
            return null;
        }
        return player.position().add(offset.at());
    }

    public static void forget(UUID player) {
        SEEN.remove(player);
        OFFSETS.remove(player);
    }

    public static void clear() {
        SEEN.clear();
        OFFSETS.clear();
        fist = null;
    }
}
