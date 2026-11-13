package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.client.fp.FirstPersonPoses;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public final class ParryFeel {
    public static final int FREEZE_TICKS = 2;
    public static final int PERFECT_FREEZE_TICKS = 3;
    private static final float LIFE_SECONDS = 0.8F;

    private static final float SPRING_OMEGA = 20F;
    private static final float SPRING_DECAY = 7F;
    private static final float SPRING_PEAK = peak(SPRING_OMEGA, SPRING_DECAY);
    private static final float JITTER_OMEGA = 55F;
    private static final float JITTER_DECAY = 9F;
    private static final float JITTER = 0.07F;
    private static final float PERFECT_JITTER = 0.11F;
    private static final float PERFECT_SCALE = 1.25F;
    private static final float FIRST_PERSON_SCALE = 0.5F;

    private static final float JOLT = 0.45F;
    private static final float PERFECT_JOLT = 0.75F;

    private static final String[] BONES = {"torso", "head", "right_arm", "left_arm"};

    private record Kick(float start, boolean perfect, boolean weapon, float side, long seed) {
    }

    private static final Map<Integer, Kick> KICKS = new HashMap<>();

    private ParryFeel() {
    }

    public static void heard(int entityId, boolean perfect, boolean weapon, float side) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        float now = now(minecraft);
        int freeze = perfect ? PERFECT_FREEZE_TICKS : FREEZE_TICKS;
        PlayerMoveAnimation.catchAt(entityId, freeze, now);
        Player local = minecraft.player;
        if (local != null && local.getId() == entityId) {
            FirstPersonPoses.catchAt(freeze);
            if (JojohaClientConfig.cameraMotion()) {
                CombatShake.add(local.getX(), local.getEyeY(), local.getZ(), perfect ? PERFECT_JOLT : JOLT);
            }
        }
        KICKS.put(entityId, new Kick(now + freeze, perfect, weapon, Math.signum(side) == 0 ? 1F : Math.signum(side),
                minecraft.level.getRandom().nextLong()));
    }

    public static void clear() {
        KICKS.clear();
    }

    public static boolean apply(PlayerModel<?> model, Player player, float now) {
        if (KICKS.isEmpty()) {
            return false;
        }
        Kick kick = KICKS.get(player.getId());
        if (kick == null) {
            return false;
        }
        if ((now - kick.start) / 20F > LIFE_SECONDS) {
            KICKS.remove(player.getId());
            return false;
        }
        ModelPart[] parts = {model.body, model.head, model.rightArm, model.leftArm};
        boolean moved = false;
        for (int i = 0; i < BONES.length; i++) {
            Vector3f offset = offset(kick, BONES[i], now);
            if (offset == null) {
                continue;
            }
            parts[i].xRot += offset.x;
            parts[i].yRot += offset.y;
            parts[i].zRot += offset.z;
            moved = true;
        }
        if (moved) {
            PlayerStabAnimation.copyOverlays(model);
        }
        return moved;
    }

    public static Vector3f firstPerson(String bone) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || KICKS.isEmpty()) {
            return null;
        }
        Kick kick = KICKS.get(minecraft.player.getId());
        if (kick == null) {
            return null;
        }
        Vector3f offset = offset(kick, bone, now(minecraft));
        return offset == null ? null : offset.mul(FIRST_PERSON_SCALE);
    }

    private static Vector3f offset(Kick kick, String bone, float now) {
        float t = (now - kick.start) / 20F - delay(bone);
        if (t <= 0F || t > LIFE_SECONDS) {
            return null;
        }
        float spring = (float) (Math.exp(-SPRING_DECAY * t) * Math.sin(SPRING_OMEGA * t)) / SPRING_PEAK;
        Vector3f recoil = recoil(bone, kick.weapon, kick.side).mul(spring * (kick.perfect ? PERFECT_SCALE : 1F));

        RandomSource random = RandomSource.create(kick.seed ^ bone.hashCode());
        Vector3f axis = new Vector3f(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F);
        if (axis.lengthSquared() < 1.0E-4F) {
            axis.set(1F, 0F, 0F);
        }
        float phase = random.nextFloat() * Mth.TWO_PI;
        float shake = (kick.perfect ? PERFECT_JITTER : JITTER) * (float) Math.exp(-JITTER_DECAY * t)
                * Mth.sin(JITTER_OMEGA * t + phase);
        return recoil.add(axis.normalize().mul(shake));
    }

    private static Vector3f recoil(String bone, boolean weapon, float side) {
        return switch (bone) {
            case "torso" -> new Vector3f(-0.22F, 0.12F * side, 0F);
            case "head" -> new Vector3f(-0.35F, 0.18F * side, 0.1F * side);
            case "right_arm" -> weapon ? new Vector3f(0.55F, 0.45F * side, 0F) : new Vector3f(0.6F, 0.15F, 0.2F);
            case "left_arm" -> weapon ? new Vector3f(0.25F, -0.2F * side, 0F) : new Vector3f(0.6F, -0.15F, -0.2F);
            default -> new Vector3f();
        };
    }

    private static float delay(String bone) {
        return switch (bone) {
            case "torso" -> 0F;
            case "head" -> 0.1F;
            default -> 0.05F;
        };
    }

    private static float peak(float omega, float decay) {
        float at = (float) Math.atan(omega / decay) / omega;
        return (float) (Math.exp(-decay * at) * Math.sin(omega * at));
    }

    private static float now(Minecraft minecraft) {
        return minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
    }
}
