package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class TargetShudder {
    private static final float SHUDDER_TICKS = 4F;

    private static final double AMPLITUDE = 0.03;

    private static final float STRONGEST = 3F;

    private static final Map<Integer, float[]> KICKS = new HashMap<>();

    private TargetShudder() {
    }

    public static void kick(Entity entity, float strength) {
        float now = clock(0F);
        float[] old = KICKS.get(entity.getId());
        float left = old == null ? 0F : old[0] * Math.max(0F, 1F - (now - old[1]) / SHUDDER_TICKS);
        KICKS.put(entity.getId(), new float[] {Math.min(STRONGEST, Math.max(strength, left + strength * 0.5F)), now});
    }

    public static boolean active() {
        return !KICKS.isEmpty();
    }

    public static Vec3 offset(Entity entity, float partialTick) {
        float[] kick = KICKS.get(entity.getId());
        if (kick == null) {
            return null;
        }
        float now = clock(partialTick);
        float age = now - kick[1];
        if (age < 0F || age >= SHUDDER_TICKS) {
            return null;
        }
        double amount = AMPLITUDE * kick[0] * (1.0 - age / SHUDDER_TICKS);
        double t = now * 2.7;
        return new Vec3(Math.sin(t * 5.3) * amount, Math.cos(t * 4.1) * amount * 0.4, Math.cos(t * 6.7) * amount);
    }

    public static void prune() {
        float now = clock(0F);
        KICKS.values().removeIf(kick -> now - kick[1] >= SHUDDER_TICKS || now < kick[1]);
    }

    public static void clear() {
        KICKS.clear();
    }

    private static float clock(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0F : (float) (minecraft.level.getGameTime() % 100000L) + partialTick;
    }
}
