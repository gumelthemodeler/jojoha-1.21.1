package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.client.anim.BedrockAnimation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class StunPose {
    public static final String ENTRY = "stunned2";

    public static final String LOOP = "stunned_loop";

    private static final float HEARD_FOR_TICKS = 5F;
    private static final float EASE_TICKS = 4F;

    private static final class Held {
        private final float since;
        private float until;

        private Held(float since, float until) {
            this.since = since;
            this.until = until;
        }
    }

    private static final Map<Integer, Held> HELD = new HashMap<>();
    private static final Map<String, BedrockAnimation> LOADED = new HashMap<>();
    private static final Set<String> TRIED = new HashSet<>();

    private StunPose() {
    }

    public static void heard(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        float now = (float) minecraft.level.getGameTime();
        Held held = HELD.get(entityId);
        if (held == null || now > held.until) {
            HELD.put(entityId, new Held(now, now + HEARD_FOR_TICKS));
        } else {
            held.until = now + HEARD_FOR_TICKS;
        }
    }

    public static void tick(float now) {
        HELD.values().removeIf(held -> now > held.until + EASE_TICKS);
    }

    public static void clear() {
        HELD.clear();
    }

    public static boolean apply(HumanoidModel<?> model, LivingEntity entity, float now) {
        Held held = HELD.get(entity.getId());
        BedrockAnimation entry = animation(ENTRY);
        BedrockAnimation loop = animation(LOOP);
        if (held == null || loop == null || now < held.since) {
            return false;
        }
        float elapsed = now - held.since;
        float weight = Math.min(elapsed / EASE_TICKS, (held.until + EASE_TICKS - now) / EASE_TICKS);
        weight = Mth.clamp(weight, 0F, 1F);
        if (weight <= 0F) {
            return false;
        }
        weight = weight * weight * (3F - 2F * weight);

        float seconds = elapsed / 20F;
        float into = entry == null ? 0F : entry.lengthSeconds();
        if (entry != null && seconds < into) {
            PlayerStabAnimation.pose(model, entry, seconds, weight);
        } else {
            PlayerStabAnimation.pose(model, loop, (seconds - into) % loop.lengthSeconds(), weight);
        }
        return true;
    }

    private static BedrockAnimation animation(String name) {
        if (TRIED.add(name)) {
            BedrockAnimation.load(PlayerMoveAnimation.FILE, name).ifPresent(anim -> LOADED.put(name, anim));
        }
        return LOADED.get(name);
    }
}
