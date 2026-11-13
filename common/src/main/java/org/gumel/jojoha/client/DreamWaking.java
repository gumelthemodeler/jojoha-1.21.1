package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

public final class DreamWaking {
    private static final int WHITENS_TICKS = 34;

    private static final int HOLD_TICKS = 10;
    private static final int FADE_TICKS = 45;

    private static final float SHAKE_FIRST = 0.35F;
    private static final float SHAKE_LAST = 2.8F;
    private static final int RING_TICKS = 18;

    private static final int GIVE_UP_TICKS = 200;

    private static final int WHITE = 0xFFFBF2;

    private static int clock;
    private static int began = -1;
    private static int leavesAt;
    private static int arrivedAt = -1;

    private DreamWaking() {
    }

    public static void begin(int ticksUntilHome) {
        began = clock;
        leavesAt = clock + Math.max(1, ticksUntilHome);
        arrivedAt = -1;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PORTAL_TRIGGER, 0.75F, 0.45F));
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.GENERIC_EXPLODE.value(), 0.45F, 0.2F));
    }

    static void tick() {
        clock++;
        if (began < 0) {
            return;
        }
        if (arrivedAt < 0 && !DreamSpirit.dreaming() && clock > began + 2) {
            arrivedAt = clock;
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4F, 0.5F));
        }
        boolean faded = arrivedAt >= 0 && clock - arrivedAt > HOLD_TICKS + FADE_TICKS;
        boolean stuck = arrivedAt < 0 && clock > leavesAt + GIVE_UP_TICKS;
        if (faded || stuck) {
            began = -1;
        }
    }

    static float white(float partialTick) {
        if (began < 0) {
            return 0F;
        }
        float now = clock + partialTick;
        if (arrivedAt >= 0) {
            float since = now - arrivedAt;
            return since < HOLD_TICKS ? 1F : 1F - Mth.clamp((since - HOLD_TICKS) / FADE_TICKS, 0F, 1F);
        }
        float coming = Mth.clamp((now - (leavesAt - WHITENS_TICKS)) / WHITENS_TICKS, 0F, 1F);
        float first = 0.12F * Mth.clamp((now - began) / 6F, 0F, 1F);
        return Math.max(first, coming * coming);
    }

    public static float[] cameraShake(float partialTick) {
        if (began < 0) {
            return null;
        }
        float now = clock + partialTick;
        float strength;
        if (arrivedAt >= 0) {
            float since = (now - arrivedAt) / RING_TICKS;
            if (since >= 1F) {
                return null;
            }
            strength = SHAKE_LAST * (1F - since) * (1F - since);
        } else {
            float through = Mth.clamp((now - began) / Math.max(1F, leavesAt - began), 0F, 1F);
            strength = SHAKE_FIRST + (SHAKE_LAST - SHAKE_FIRST) * through * through;
        }
        float yaw = (Mth.sin(now * 2.7F) * 0.6F + Mth.sin(now * 7.3F + 1.1F) * 0.4F) * strength;
        float pitch = (Mth.sin(now * 3.3F + 0.7F) * 0.6F + Mth.sin(now * 8.9F) * 0.4F) * strength * 0.7F;
        return new float[]{yaw, pitch};
    }

    static void renderHud(GuiGraphics graphics) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float alpha = white(partialTick);
        if (alpha <= 0.003F) {
            return;
        }
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), ((int) (alpha * 255F) << 24) | WHITE);
    }
}
