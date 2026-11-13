package org.gumel.jojoha.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FortuneSense {
    private static final float MAX_DEPTH = 0.22F;

    private static final float MAX_ALPHA = 0.55F;

    private static final int BANDS = 20;

    private static final float FADE_IN = 0.15F;

    private static final int THREAT_COLOUR = 0xB04AE0;

    private static long dimFrom;

    private static long dimUntil;

    private static final Map<Integer, Long> GLOWING = new HashMap<>();

    private FortuneSense() {
    }

    public static void dim(int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        dimFrom = minecraft.level.getGameTime();
        dimUntil = dimFrom + ticks;
    }

    public static void glow(List<Integer> ids, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        long until = minecraft.level.getGameTime() + ticks;
        for (int id : ids) {
            GLOWING.put(id, until);
        }
    }

    public static void clear() {
        dimUntil = 0L;
        GLOWING.clear();
    }

    public static boolean reveals(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        Long until = GLOWING.get(entity.getId());
        if (until == null || minecraft.level == null) {
            return false;
        }
        if (minecraft.level.getGameTime() > until) {
            GLOWING.remove(entity.getId());
            return false;
        }
        return true;
    }

    public static int colour(Entity entity) {
        return reveals(entity) ? THREAT_COLOUR : -1;
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        float now = minecraft.level.getGameTime() + partialTick;
        if (now >= dimUntil) {
            return;
        }
        float length = Math.max(1F, dimUntil - dimFrom);
        float through = (now - dimFrom) / length;
        float strength = through < FADE_IN ? through / FADE_IN : 1F - (through - FADE_IN) / (1F - FADE_IN);
        strength = Mth.clamp(strength, 0F, 1F);
        if (strength <= 0F) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int depth = Math.max(1, Math.round(Math.min(width, height) * MAX_DEPTH));
        int step = Math.max(1, depth / BANDS);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int inset = 0; inset < depth; inset += step) {
            float along = 1F - (float) inset / depth;
            int alpha = Mth.clamp(Math.round(along * along * MAX_ALPHA * strength * 255F), 0, 255);
            if (alpha <= 0) {
                continue;
            }
            int colour = alpha << 24;
            int near = inset;
            int far = inset + step;
            graphics.fill(0, near, width, far, colour);
            graphics.fill(0, height - far, width, height - near, colour);
            graphics.fill(near, far, far, height - far, colour);
            graphics.fill(width - far, far, width - near, height - far, colour);
        }
        RenderSystem.disableBlend();
    }
}
