package org.gumel.jojoha.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.dream.DreamRealm;
import org.gumel.jojoha.network.NetworkHandler;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class DreamPillars {
    private static final float ARC_RADIUS = 4.5F;
    private static final float ARC_WIDEST = 130F;
    private static final float ARC_STEP_MOST = 28F;

    private static final float FLOOR_MARGIN = 2F;

    private static final float TALL = 3.5F;
    private static final float REACH = 0.75F;
    private static final float SQUARE = 1F / 8F;

    private static final int CORE = 1;

    private static final int RISE_TICKS = 14;
    private static final int STAGGER_TICKS = 4;
    private static final int SINK_TICKS = 16;

    private static final int CHOSEN_TICKS = 26;
    private static final float CHOSEN_CLIMB = 2.5F;

    private static final float CORE_ALPHA = 0.5F;
    private static final float GLOW_ALPHA = 0.2F;
    private static final int STEPS = 4;
    private static final int FLICKER_TICKS = 5;

    private static final float LOOKED_AT = 1.7F;
    private static final float REFLECTED = 0.35F;

    private static final float AIM_NEAR = 0.55F;
    private static final float AIM_PER_BLOCK = 0.04F;
    private static final float AIM_FARTHEST = 24F;

    private static final int UNANSWERED_TICKS = 60;

    private static final float MOTE_CHANCE = 0.45F;
    private static final int MOTE_LIFE_MIN = 25, MOTE_LIFE_MAX = 55;

    private static final float LABEL_SCALE = 0.022F;
    private static final int LABEL_WRAP = 110;
    private static final float LABEL_ABOVE = 0.5F;

    private static final float[][] TONES = {
            {1F, 0.8F, 0.42F},
            {0.84F, 0.8F, 0.72F},
            {0.86F, 0.9F, 1F},
    };

    private static final RandomSource RANDOM = RandomSource.create();

    private static final class Mote {
        float x, y, rise;
        int age, life;
    }

    private static final class Pillar {
        final int index;
        final double x, z;
        final Component words;
        final float[] tone;
        final int bornAt;
        final float seed = RANDOM.nextFloat() * 100F;
        final List<Mote> motes = new ArrayList<>();
        int sinkAt = -1;
        int chosenAt = -1;

        Pillar(int index, double x, double z, Component words, float[] tone, int bornAt) {
            this.index = index;
            this.x = x;
            this.z = z;
            this.words = words;
            this.tone = tone;
            this.bornAt = bornAt;
        }

        float risen(float now) {
            float up = Mth.clamp((now - bornAt) / RISE_TICKS, 0F, 1F);
            up = up * up * (3F - 2F * up);
            if (sinkAt >= 0) {
                float down = Mth.clamp((now - sinkAt) / SINK_TICKS, 0F, 1F);
                up *= 1F - down * down;
            }
            return up;
        }

        boolean gone(int now) {
            return (sinkAt >= 0 && now - sinkAt > SINK_TICKS) || (chosenAt >= 0 && now - chosenAt > CHOSEN_TICKS);
        }

        boolean standing() {
            return sinkAt < 0 && chosenAt < 0;
        }
    }

    private static final List<Pillar> PILLARS = new ArrayList<>();
    private static int now;
    private static int turn = -1;
    private static Pillar lookedAt;
    private static boolean useWasDown, attackWasDown;
    private static LayeredBuffers words;

    private static List<Component> offered = List.of();
    private static List<Integer> offeredTones = List.of();
    private static int touchedAt = -1;

    private DreamPillars() {
    }

    public static void offer(int offeredTurn, List<Component> answers, List<Integer> tones) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || answers.isEmpty()) {
            return;
        }
        if (offeredTurn == turn && PILLARS.stream().anyMatch(Pillar::standing)) {
            return;
        }
        withdraw();
        turn = offeredTurn;
        offered = List.copyOf(answers);
        offeredTones = List.copyOf(tones);

        int count = answers.size();
        float step = count <= 1 ? 0F : Math.min(ARC_STEP_MOST, ARC_WIDEST / (count - 1));
        Vec3 room = Vec3.atBottomCenterOf(DreamRealm.room(player.getUUID()));
        for (int i = 0; i < count; i++) {
            float yaw = (player.getYRot() + (i - (count - 1) / 2F) * step) * Mth.DEG_TO_RAD;
            double x = player.getX() - Mth.sin(yaw) * ARC_RADIUS;
            double z = player.getZ() + Mth.cos(yaw) * ARC_RADIUS;

            double fromX = x - room.x, fromZ = z - room.z;
            double out = Math.sqrt(fromX * fromX + fromZ * fromZ);
            double most = DreamRealm.FLOOR_RADIUS - FLOOR_MARGIN;
            if (out > most) {
                x = room.x + fromX * most / out;
                z = room.z + fromZ * most / out;
            }

            int tone = i < tones.size() ? Mth.clamp(tones.get(i), 0, TONES.length - 1) : 0;
            PILLARS.add(new Pillar(i, x, z, answers.get(i), TONES[tone], now + i * STAGGER_TICKS));
        }
    }

    public static void withdraw() {
        touchedAt = -1;
        for (Pillar pillar : PILLARS) {
            if (pillar.standing()) {
                pillar.sinkAt = now;
            }
        }
        lookedAt = null;
    }

    static int turn() {
        return turn;
    }

    public static Component lookedAt() {
        return lookedAt == null ? null : lookedAt.words;
    }

    public static boolean offering() {
        for (Pillar pillar : PILLARS) {
            if (pillar.standing()) {
                return true;
            }
        }
        return false;
    }

    static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!DreamSpirit.dreaming() || minecraft.player == null) {
            if (!PILLARS.isEmpty() || turn >= 0) {
                PILLARS.clear();
                turn = -1;
                lookedAt = null;
                touchedAt = -1;
            }
            return;
        }
        now++;
        PILLARS.removeIf(pillar -> pillar.gone(now));
        if (touchedAt >= 0 && now - touchedAt > UNANSWERED_TICKS && !offering()) {
            int again = turn;
            turn = -1;
            offer(again, offered, offeredTones);
        }
        for (Pillar pillar : PILLARS) {
            motes(pillar);
        }

        lookedAt = aim(minecraft.player);
        boolean use = minecraft.options.keyUse.isDown();
        boolean attack = minecraft.options.keyAttack.isDown();
        boolean pressed = (use && !useWasDown) || (attack && !attackWasDown);
        useWasDown = use;
        attackWasDown = attack;
        if (pressed && lookedAt != null && minecraft.screen == null) {
            touch(minecraft, lookedAt);
        }
    }

    private static Pillar aim(LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1F);
        double flat = look.x * look.x + look.z * look.z;
        if (flat < 1.0E-4) {
            return null;
        }
        Pillar best = null;
        double nearest = Double.MAX_VALUE;
        for (Pillar pillar : PILLARS) {
            if (!pillar.standing() || pillar.risen(now) < 0.5F) {
                continue;
            }
            double along = ((pillar.x - eye.x) * look.x + (pillar.z - eye.z) * look.z) / flat;
            if (along <= 0 || along > AIM_FARTHEST) {
                continue;
            }
            double y = eye.y + look.y * along;
            if (y < DreamRealm.FLOOR_Y - 0.5 || y > DreamRealm.FLOOR_Y + TALL + LABEL_ABOVE + 1.0) {
                continue;
            }
            double off = Math.hypot(pillar.x - (eye.x + look.x * along), pillar.z - (eye.z + look.z * along));
            if (off <= AIM_NEAR + AIM_PER_BLOCK * along && off < nearest) {
                nearest = off;
                best = pillar;
            }
        }
        return best;
    }

    private static void touch(Minecraft minecraft, Pillar touched) {
        touched.chosenAt = now;
        touchedAt = now;
        for (Pillar pillar : PILLARS) {
            if (pillar != touched && pillar.standing()) {
                pillar.sinkAt = now;
            }
        }
        lookedAt = null;
        NetworkHandler.sendDreamAnswer(turn, touched.index);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.9F));
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_POWER_SELECT, 1.6F, 0.35F));
    }

    private static void motes(Pillar pillar) {
        for (int i = pillar.motes.size() - 1; i >= 0; i--) {
            Mote mote = pillar.motes.get(i);
            mote.y += mote.rise;
            if (++mote.age >= mote.life) {
                pillar.motes.remove(i);
            }
        }
        if (pillar.risen(now) > 0.3F && RANDOM.nextFloat() < MOTE_CHANCE * (pillar.chosenAt >= 0 ? 3F : 1F)) {
            Mote mote = new Mote();
            mote.x = (RANDOM.nextFloat() - 0.5F) * REACH * 1.2F;
            mote.y = RANDOM.nextFloat() * 0.6F;
            mote.rise = 0.04F + RANDOM.nextFloat() * 0.05F;
            mote.life = MOTE_LIFE_MIN + RANDOM.nextInt(MOTE_LIFE_MAX - MOTE_LIFE_MIN);
            pillar.motes.add(mote);
        }
    }

    static void render(PoseStack poseStack, Camera camera, float partialTick, boolean reflected) {
        if (PILLARS.isEmpty()) {
            return;
        }
        Vec3 eye = camera.getPosition();
        Matrix4f pose = poseStack.last().pose();
        float time = now + partialTick;

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR);
        for (Pillar pillar : PILLARS) {
            light(buffer, pose, eye, pillar, time, reflected);
        }
        draw(buffer);

        if (!reflected) {
            for (Pillar pillar : PILLARS) {
                label(poseStack, camera, eye, pillar, time);
            }
            if (words != null) {
                words.endBatch();
            }
        }
    }

    private static void light(BufferBuilder buffer, Matrix4f pose, Vec3 eye, Pillar pillar, float time,
                              boolean reflected) {
        float risen = pillar.risen(time);
        float flare = 0F, climb = 0F;
        if (pillar.chosenAt >= 0) {
            float since = Mth.clamp((time - pillar.chosenAt) / CHOSEN_TICKS, 0F, 1F);
            flare = Mth.sin(since * Mth.PI) * 1.2F;
            climb = since * since * CHOSEN_CLIMB;
            risen = 1F - since * since;
        }
        if (risen <= 0.01F) {
            return;
        }
        float bright = risen * (pillar == lookedAt ? LOOKED_AT : 1F) * (1F + flare) * (reflected ? REFLECTED : 1F);

        float toX = (float) (pillar.x - eye.x), toZ = (float) (pillar.z - eye.z);
        float flat = Mth.sqrt(toX * toX + toZ * toZ);
        float acrossX = flat < 1.0E-3F ? 1F : toZ / flat, acrossZ = flat < 1.0E-3F ? 0F : -toX / flat;

        int half = Math.round(REACH / SQUARE);
        int rows = Math.round(TALL * Math.min(1F, risen * 1.15F) / SQUARE);
        int beat = (int) (time / FLICKER_TICKS);
        for (int column = -half; column < half; column++) {
            float side = Math.abs(column + 0.5F) / half;
            float top = rows * (0.82F + 0.18F * Mth.sin(time * 0.11F + column * 1.7F + pillar.seed));
            for (int row = 0; row < top; row++) {
                float up = row / Math.max(1F, top);
                boolean core = Math.abs(column + 0.5F) <= CORE;
                float fall = (1F - side) * (1F - up * up * 0.7F);
                float level = step(fall, STEPS);
                if (level <= 0F) {
                    continue;
                }
                int hash = (column * 73856093) ^ (row * 19349663) ^ (beat * 83492791) ^ (pillar.index * 2654435);
                if (Math.floorMod(hash, 19) == 0) {
                    level = Math.min(1F, level + 1F / STEPS);
                }
                float alpha = (core ? CORE_ALPHA : GLOW_ALPHA * level) * bright;
                square(buffer, pose, eye, pillar, acrossX, acrossZ, column, row * SQUARE + climb, 1, alpha, reflected);
            }
        }
        for (Mote mote : pillar.motes) {
            float fade = Mth.sin(mote.age / (float) mote.life * Mth.PI);
            int column = Math.round(mote.x / SQUARE);
            square(buffer, pose, eye, pillar, acrossX, acrossZ, column, Math.round(mote.y / SQUARE) * SQUARE + climb,
                    1, step(fade, 3) * 0.45F * bright, reflected);
        }
    }

    private static void square(BufferBuilder buffer, Matrix4f pose, Vec3 eye, Pillar pillar, float acrossX,
                               float acrossZ, int column, float height, int cells, float alpha, boolean reflected) {
        if (alpha <= 0.002F) {
            return;
        }
        float left = column * SQUARE, right = (column + cells) * SQUARE;
        float bottom = DreamRealm.FLOOR_Y + height, top = bottom + cells * SQUARE;
        if (reflected) {
            bottom = 2F * DreamRealm.FLOOR_Y - bottom;
            top = 2F * DreamRealm.FLOOR_Y - top;
        }
        float x0 = (float) (pillar.x - eye.x) + acrossX * left, z0 = (float) (pillar.z - eye.z) + acrossZ * left;
        float x1 = (float) (pillar.x - eye.x) + acrossX * right, z1 = (float) (pillar.z - eye.z) + acrossZ * right;
        float y0 = (float) (bottom - eye.y), y1 = (float) (top - eye.y);
        float[] tone = pillar.tone;
        buffer.addVertex(pose, x0, y0, z0).setColor(tone[0], tone[1], tone[2], alpha);
        buffer.addVertex(pose, x1, y0, z1).setColor(tone[0], tone[1], tone[2], alpha);
        buffer.addVertex(pose, x1, y1, z1).setColor(tone[0], tone[1], tone[2], alpha);
        buffer.addVertex(pose, x0, y0, z0).setColor(tone[0], tone[1], tone[2], alpha);
        buffer.addVertex(pose, x1, y1, z1).setColor(tone[0], tone[1], tone[2], alpha);
        buffer.addVertex(pose, x0, y1, z0).setColor(tone[0], tone[1], tone[2], alpha);
    }

    private static void label(PoseStack poseStack, Camera camera, Vec3 eye, Pillar pillar, float time) {
        float shown = pillar.risen(time) * (pillar.chosenAt >= 0 ? 0F : 1F);
        if (shown <= 0.05F) {
            return;
        }
        if (words == null) {
            words = new LayeredBuffers();
        }
        Font font = Minecraft.getInstance().font;
        boolean looked = pillar == lookedAt;
        float[] tone = pillar.tone;
        float lift = looked ? 1F : 0.82F;
        int rgb = ((int) (Math.min(1F, tone[0] * lift + (looked ? 0.1F : 0F)) * 255F) << 16)
                | ((int) (Math.min(1F, tone[1] * lift + (looked ? 0.1F : 0F)) * 255F) << 8)
                | (int) (Math.min(1F, tone[2] * lift + (looked ? 0.1F : 0F)) * 255F);
        int alpha = Math.max(8, (int) (shown * (looked ? 255F : 200F)));

        List<FormattedCharSequence> rows = font.split(pillar.words, LABEL_WRAP);
        float scale = LABEL_SCALE * (looked ? 1.12F : 1F);
        poseStack.pushPose();
        poseStack.translate(pillar.x - eye.x, DreamRealm.FLOOR_Y + TALL * pillar.risen(time) + LABEL_ABOVE
                + rows.size() * font.lineHeight * scale - eye.y, pillar.z - eye.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(scale, -scale, scale);
        Matrix4f pose = poseStack.last().pose();
        for (int i = 0; i < rows.size(); i++) {
            FormattedCharSequence row = rows.get(i);
            font.drawInBatch(row, -font.width(row) / 2F, i * (font.lineHeight + 1), (alpha << 24) | rgb, false, pose,
                    words, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }
        poseStack.popPose();
    }

    private static float step(float amount, int steps) {
        return Mth.ceil(Mth.clamp(amount, 0F, 1F) * steps) / (float) steps;
    }

    private static void draw(BufferBuilder buffer) {
        MeshData mesh = buffer.build();
        if (mesh == null) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        BufferUploader.drawWithShader(mesh);
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
