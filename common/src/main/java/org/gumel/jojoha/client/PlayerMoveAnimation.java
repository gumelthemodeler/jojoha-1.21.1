package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.anim.BedrockAnimation;
import org.gumel.jojoha.client.anim.ClipContact;
import org.gumel.jojoha.client.fp.FirstPersonPose;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.network.packet.PlayerMovePosePacket;
import org.gumel.jojoha.pillar.PillarPoses;
import org.gumel.jojoha.vampire.VampirePoses;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PlayerMoveAnimation {
    public static final ResourceLocation FILE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "animations/playeranimations.animation.json");

    private static final String ROOT = "body";

    private static final float HIP_PIVOT = (24F - 12F) / 16F;

    private static final int ONCE = PlayerMovePosePacket.ONCE;

    private static final int HELD = PlayerMovePosePacket.HELD;

    private record Active(float startTick, String name, int mode, boolean continuing, float frozenUntil) {
    }

    private static final Map<String, Optional<BedrockAnimation>> LOADED = new HashMap<>();
    private static final Map<Integer, Active> ACTIVE = new HashMap<>();

    private PlayerMoveAnimation() {
    }

    public static void begin(int entityId, String name, float clientTimeTicks) {
        begin(entityId, name, ONCE, clientTimeTicks);
    }

    public static void begin(int entityId, String name, int mode, float clientTimeTicks) {
        if (name == null || name.isEmpty()) {
            ACTIVE.remove(entityId);
            return;
        }
        BedrockAnimation anim = animation(name);
        if (anim == null || echo(entityId, name, mode, clientTimeTicks)) {
            return;
        }
        Active held = ACTIVE.get(entityId);
        Float holdAt = MovementFx.HOLD_AT.get(name);
        if (mode == ONCE && holdAt != null && held != null && held.mode() == HELD && held.name().equals(name)) {
            ACTIVE.put(entityId, new Active(Math.max(held.startTick(), clientTimeTicks - holdAt * 20F), name, mode,
                    true, Float.NEGATIVE_INFINITY));
            return;
        }
        ACTIVE.put(entityId, new Active(clientTimeTicks, name, mode, picksUpFrom(entityId, anim, clientTimeTicks),
                Float.NEGATIVE_INFINITY));
    }

    private static final float ECHO_TICKS = 8F;

    private static boolean echo(int entityId, String name, int mode, float clientTimeTicks) {
        Active same = ACTIVE.get(entityId);
        LocalPlayer local = Minecraft.getInstance().player;
        return same != null && mode == ONCE && same.mode() == ONCE && same.name().equals(name)
                && name.contains("parry") && local != null && local.getId() == entityId
                && clientTimeTicks - same.startTick() < ECHO_TICKS;
    }

    public static void freeze(int entityId, int ticks, float clientTimeTicks) {
        Active active = ACTIVE.get(entityId);
        if (active == null || ticks <= 0) {
            return;
        }
        ACTIVE.put(entityId, new Active(active.startTick() + ticks, active.name(), active.mode(), active.continuing(),
                Math.max(clientTimeTicks, active.frozenUntil()) + ticks));
    }

    public static void catchAt(int entityId, int ticks, float clientTimeTicks) {
        Active active = ACTIVE.get(entityId);
        BedrockAnimation anim = active == null ? null : animation(active.name());
        if (anim == null || ticks <= 0) {
            return;
        }
        float held = Math.max(clientTimeTicks, active.frozenUntil());
        float clash = ClipContact.clamp(anim, at(active, anim, clientTimeTicks));
        ACTIVE.put(entityId, new Active(held - clash * 20F + ticks, active.name(), active.mode(), active.continuing(),
                held + ticks));
    }

    private static boolean picksUpFrom(int entityId, BedrockAnimation next, float clientTimeTicks) {
        Active active = ACTIVE.get(entityId);
        BedrockAnimation anim = active == null ? null : animation(active.name());
        if (anim == null) {
            return false;
        }
        float leaving = at(active, anim, clientTimeTicks);
        for (String bone : LIMBS) {
            Vector3f was = anim.drives(bone) ? anim.sample(bone, BedrockAnimation.Channel.ROTATION, leaving) : null;
            Vector3f now = next.drives(bone) ? next.sample(bone, BedrockAnimation.Channel.ROTATION, 0F) : null;
            Vector3f from = was == null ? new Vector3f() : was;
            Vector3f to = now == null ? new Vector3f() : now;
            if (from.distance(to) > REST_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    private static float at(Active active, BedrockAnimation anim, float clientTimeTicks) {
        float seconds = Math.min((Math.max(clientTimeTicks, active.frozenUntil()) - active.startTick()) / 20F,
                anim.lengthSeconds());
        if (active.mode() == HELD && MovementFx.LOOPING_HOLDS.contains(active.name())
                && anim.lengthSeconds() > 0F) {
            float running = (Math.max(clientTimeTicks, active.frozenUntil()) - active.startTick()) / 20F;
            return Math.max(0F, running) % anim.lengthSeconds();
        }
        Float holdAt = active.mode() == HELD ? MovementFx.HOLD_AT.get(active.name()) : null;
        return holdAt == null ? seconds : Math.min(seconds, holdAt);
    }

    public record Pose(BedrockAnimation anim, float seconds, float weight) {
    }

    public static Pose pose(int entityId, float clientTimeTicks) {
        Active active = ACTIVE.get(entityId);
        BedrockAnimation anim = active == null ? null : animation(active.name());
        if (anim == null) {
            return null;
        }
        float elapsed = Math.max(clientTimeTicks, active.frozenUntil()) - active.startTick();
        if (elapsed < 0F || active.mode() == ONCE && elapsed > anim.lengthTicks()) {
            return null;
        }
        float seconds = at(active, anim, clientTimeTicks);
        return new Pose(anim, seconds, weight(active, anim, seconds));
    }

    public static boolean playing(Player player, String name) {
        Active active = ACTIVE.get(player.getId());
        return active != null && active.name().equals(name);
    }

    public static float progress(Player player, String name) {
        Active active = ACTIVE.get(player.getId());
        BedrockAnimation anim = active == null ? null : animation(name);
        Minecraft minecraft = Minecraft.getInstance();
        if (anim == null || !active.name().equals(name) || minecraft.level == null || anim.lengthTicks() <= 0F) {
            return 0F;
        }
        float now = (float) minecraft.level.getGameTime()
                + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        return Mth.clamp((Math.max(now, active.frozenUntil()) - active.startTick())
                / anim.lengthTicks(), 0F, 1F);
    }

    public static void tick(float clientTimeTicks) {
        ACTIVE.entrySet().removeIf(entry -> {
            BedrockAnimation anim = animation(entry.getValue().name());
            if (anim == null) {
                return true;
            }
            return entry.getValue().mode() == ONCE
                    && Math.max(clientTimeTicks, entry.getValue().frozenUntil()) - entry.getValue().startTick()
                    > anim.lengthTicks();
        });
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static boolean apply(PlayerModel<?> model, Player player, float clientTimeTicks) {
        BedrockAnimation anim = playing(player, clientTimeTicks);
        if (anim == null) {
            return false;
        }
        Active active = ACTIVE.get(player.getId());
        float seconds = at(active, anim, clientTimeTicks);
        PlayerStabAnimation.pose(model, anim, seconds, weight(active, anim, seconds));
        return true;
    }

    private static final float EASE_IN_SECONDS = 2F / 20F;
    private static final float EASE_OUT_SECONDS = 4F / 20F;

    private static final float REST_TOLERANCE = (float) Math.toRadians(2.0);

    private static float weight(Active active, BedrockAnimation anim, float seconds) {
        float weight = 1F;
        if (!atRest(anim, 0F) && !active.continuing()) {
            weight = Math.min(weight, seconds / EASE_IN_SECONDS);
        }
        if (active.mode() == ONCE && !atRest(anim, anim.lengthSeconds())) {
            weight = Math.min(weight, (anim.lengthSeconds() - seconds) / EASE_OUT_SECONDS);
        }
        weight = Mth.clamp(weight, 0F, 1F);
        return weight * weight * (3F - 2F * weight);
    }

    private static boolean atRest(BedrockAnimation anim, float seconds) {
        for (String bone : LIMBS) {
            Vector3f rotation = anim.drives(bone)
                    ? anim.sample(bone, BedrockAnimation.Channel.ROTATION, seconds) : null;
            if (rotation != null && Math.max(Math.abs(rotation.x), Math.max(Math.abs(rotation.y),
                    Math.abs(rotation.z))) > REST_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    private static final String[] LIMBS = {"torso", "head", "right_arm", "left_arm", "right_leg", "left_leg"};

    public static void root(PoseStack poseStack, Player player, float clientTimeTicks) {
        BedrockAnimation anim = playing(player, clientTimeTicks);
        if (anim == null || !anim.drives(ROOT)) {
            return;
        }
        float seconds = at(ACTIVE.get(player.getId()), anim, clientTimeTicks);
        Vector3f position = anim.sample(ROOT, BedrockAnimation.Channel.POSITION, seconds);
        if (position != null) {
            poseStack.translate(position.x / 16F, position.y / 16F, position.z / 16F);
        }
        Vector3f rotation = anim.sample(ROOT, BedrockAnimation.Channel.ROTATION, seconds);
        if (rotation != null) {
            poseStack.translate(0F, HIP_PIVOT, 0F);
            poseStack.mulPose(new Quaternionf().rotationZYX(rotation.z, rotation.y, rotation.x));
            poseStack.translate(0F, -HIP_PIVOT, 0F);
        }
    }

    private static BedrockAnimation playing(Player player, float clientTimeTicks) {
        Active active = ACTIVE.get(player.getId());
        if (active == null) {
            return null;
        }
        BedrockAnimation anim = animation(active.name());
        if (anim == null) {
            return null;
        }
        float elapsed = Math.max(clientTimeTicks, active.frozenUntil()) - active.startTick();
        if (elapsed < 0F) {
            return null;
        }
        return active.mode() == ONCE && elapsed > anim.lengthTicks() ? null : anim;
    }

    private static BedrockAnimation animation(String name) {
        return LOADED.computeIfAbsent(name, n -> BedrockAnimation.load(fileOf(n), n)).orElse(null);
    }

    public static final ResourceLocation VAMPIRE_FILE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "animations/vampireplayeranimations.json");

    public static final ResourceLocation PILLAR_FILE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "animations/pillarman.animations.json");

    static ResourceLocation fileOf(String name) {
        if (PillarPoses.ALL.contains(name)) {
            return PILLAR_FILE;
        }
        if (MovementFx.HAMON_POSES.contains(name)) {
            return FirstPersonPose.POSES;
        }
        return VampirePoses.ALL.contains(name) ? VAMPIRE_FILE : FILE;
    }
}
