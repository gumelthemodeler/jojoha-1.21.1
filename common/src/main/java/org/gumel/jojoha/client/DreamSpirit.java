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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.dream.DreamPacing;
import org.gumel.jojoha.dream.DreamRealm;
import org.gumel.jojoha.dream.DreamSession;
import org.gumel.jojoha.network.packet.DreamSpeechPacket;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class DreamSpirit {
    private static final float DISTANCE = 60F;
    private static final float SCALE = 13F;

    private static final float[] HAZE_SCALE = {1.035F, 1.08F, 1.14F};
    private static final float[] HAZE_ALPHA = {0.24F, 0.13F, 0.07F};
    private static final float CHEST = 0.55F;

    private static final float[] CALM_GOLD = {1F, 0.8F, 0.42F};
    private static final float[] COLD_GOLD = {0.82F, 0.72F, 0.5F};
    private static final float[] WARM_GOLD = {1F, 0.87F, 0.52F};
    private static final float BODY_ALPHA = 0.62F;

    private static final float FLAME_RISE = 0.055F;
    private static final float FLAME_TAPER = 0.93F;
    private static final int FLAME_LIFE_MIN = 10, FLAME_LIFE_SPAN = 7;
    private static final float FLAME_SIZE_MIN = 0.34F, FLAME_SIZE_SPREAD = 0.16F;
    private static final int FLAMES_PER_TICK = 4;
    private static final int FLAMES_MAX = 140;
    private static final float FLAME_ALPHA = 0.55F;

    private static final float FLAME_SCALE = 2.4F;
    private static final int FLAME_PIXELS = 8;
    private static final float FLAME_PIXEL = (FLAME_SIZE_MIN + FLAME_SIZE_SPREAD * 0.5F) * 1.2F * FLAME_SCALE * SCALE
            / FLAME_PIXELS;

    private static final float LEGS_SHARE = 0.58F;
    private static final float LEGS_DOWN = 0.5F;
    private static final float LEGS_RADIUS = 0.22F;
    private static final float BODY_UP = 0.6F;
    private static final float BODY_RADIUS = 0.3F;

    private static final float BODY_FLAME = 0.65F;

    private static final float FACE_FROM = 0.7F;
    private static final float FACE_HALF = 0.3F;
    private static final float FACE_FADE = 0.2F;
    private static final float DEPTH = 0.55F;

    private static final float GOLDEN_ANGLE = 2.39996F;

    private static final ResourceLocation[] FLAME_FRAMES = new ResourceLocation[8];

    static {
        for (int frame = 0; frame < FLAME_FRAMES.length; frame++) {
            FLAME_FRAMES[frame] = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "dreamer_flame_" + frame);
        }
    }

    private static final float REFLECTED = 0.42F;
    private static final float REFLECTION_SWAY = 0.5F;

    private static final int RISE_FROM = 30;
    private static final int RISE_TICKS = 70;
    private static final int WAKES_AT = 112;
    private static final int SETS_TICKS = 40;

    private static final float ASLEEP = 0.4F;

    private static final int WAKING_TICKS = 14;

    private static final float WANDER_YAW = 17F;
    private static final float WANDER_PITCH = 6F;

    private static final float ABOVE_GAZE = 4F;
    private static final float HIGHEST = -45F;
    private static final float LOWEST = -16F;

    private static final float LEAN_PER_DEGREE = 0.45F;
    private static final float LEAN_MOST = 22F;

    private static final float BOW_PER_DEGREE = 0.6F;
    private static final float BOW_MOST = 28F;

    private static final float WANDER_SPEAKING = 1.0F;
    private static final float WANDER_LISTENING = 0.3F;

    private static final float FOLLOW = 0.045F;

    private static final int LETTERS_PER_CHIME = 3;

    private static final int CALM_COLOR = 0xF2DDA4;
    private static final int COLD_COLOR = 0xC9B78F;
    private static final int WARM_COLOR = 0xFFE9B8;

    private static final String CHOSEN_MARK = String.valueOf((char) 0x00BB) + " ";

    private static final int SPEECH_WIDE = 320;
    private static final int SPEECH_GAP = 10;
    private static final int SPEECH_NARROWEST = 150;
    private static final int SPEECH_HIGH = 34;

    private static final int FADE_TICKS = 20;

    private static final RandomSource RANDOM = RandomSource.create();

    private static final class Flame {
        float x, y, z, lastX, lastY, lastZ, size;
        int age, life;
    }

    private static int here;
    private static boolean placed;
    private static float yaw, pitch, lastYaw, lastPitch;
    private static float wander;
    private static float phase = RANDOM.nextFloat() * 100F;
    private static final List<Flame> FLAMES = new ArrayList<>();
    private static int lit;
    private static LayeredBuffers buffers;

    private static List<String> lines = List.of();
    private static Component lastLine = Component.empty();
    private static int line;
    private static int lineStart;
    private static int mood = DreamSpeechPacket.CALM;
    private static List<Component> answers = List.of();
    private static List<Integer> tones = List.of();
    private static int turn;
    private static boolean answersPosted;
    private static boolean parting;
    private static int setAt = -1;
    private static int lastShown;

    private DreamSpirit() {
    }

    static boolean dreaming() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && minecraft.level.dimension().equals(DreamRealm.DREAM);
    }

    static int age() {
        return here;
    }

    public static void heard(DreamSpeechPacket packet) {
        List<String> said = new ArrayList<>();
        for (Component component : packet.lines()) {
            said.add(component.getString());
        }
        lines = said;
        lastLine = packet.lines().isEmpty() ? Component.empty() : packet.lines().get(packet.lines().size() - 1);
        line = 0;
        lineStart = here;
        lastShown = 0;
        mood = packet.mood();
        answers = packet.replies();
        tones = packet.tones();
        turn = packet.turn();
        answersPosted = false;
        parting = packet.parting();
        if (answers.isEmpty() || turn != DreamPillars.turn()) {
            DreamPillars.withdraw();
        }
        if (packet.waking()) {
            DreamWaking.begin(DreamPacing.ticks(packet.lines()) + DreamSession.PARTING_TICKS);
        }
    }

    public static void tick() {
        DreamWaking.tick();
        DreamPillars.tick();
        if (!dreaming()) {
            if (here != 0) {
                reset();
            }
            return;
        }
        here++;
        if (here == WAKES_AT) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 0.5F));
        }
        speak();
        drift();
        burn();
    }

    private static void reset() {
        here = 0;
        placed = false;
        lines = List.of();
        answers = List.of();
        tones = List.of();
        answersPosted = false;
        parting = false;
        setAt = -1;
        FLAMES.clear();
    }

    private static void speak() {
        if (line >= lines.size()) {
            return;
        }
        String text = lines.get(line);
        int elapsed = here - lineStart;
        int shown = DreamPacing.shown(text, elapsed);
        if (shown > lastShown && !DreamPacing.pausing(text, elapsed)
                && (lastShown == 0 || shown / LETTERS_PER_CHIME != lastShown / LETTERS_PER_CHIME)) {
            chime();
        }
        lastShown = shown;

        boolean last = line == lines.size() - 1;
        float typed = DreamPacing.typing(text);
        if (last && elapsed >= typed && !answersPosted) {
            answersPosted = true;
            post();
        }
        if (!last && elapsed >= typed + DreamPacing.hold(text)) {
            line++;
            lineStart = here;
            lastShown = 0;
        }
        if (last && parting && setAt < 0 && elapsed >= typed + DreamPacing.hold(text)) {
            setAt = here;
        }
    }

    private static void post() {
        if (answers.isEmpty()) {
            return;
        }
        DreamPillars.offer(turn, answers, tones);
    }

    private static void chime() {
        float pitch = switch (mood) {
            case DreamSpeechPacket.COLD -> 0.5F;
            case DreamSpeechPacket.WARM -> 0.75F;
            default -> 0.62F;
        };
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.NOTE_BLOCK_CHIME.value(), pitch + RANDOM.nextFloat() * 0.08F, 0.16F));
    }

    private static void drift() {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return;
        }
        float gazeYaw = camera.getYRot();
        float gazePitch = camera.getXRot();
        if (!placed) {
            placed = true;
            yaw = lastYaw = gazeYaw;
            pitch = lastPitch = Mth.clamp(gazePitch - ABOVE_GAZE - WANDER_PITCH, HIGHEST, LOWEST);
        }
        lastYaw = yaw;
        lastPitch = pitch;

        wander += speaking() ? WANDER_SPEAKING : WANDER_LISTENING;
        float t = (phase + wander) * 0.02F;
        float offsetYaw = WANDER_YAW * (0.7F * Mth.sin(t * 1.3F) + 0.3F * Mth.sin(t * 3.1F + 1.7F));
        float offsetPitch = WANDER_PITCH * (0.75F * Mth.sin(t * 1.9F + 0.6F) + 0.25F * Mth.sin(t * 4.3F));

        float wantYaw = gazeYaw + offsetYaw;
        float wantPitch = Mth.clamp(gazePitch - ABOVE_GAZE + offsetPitch, HIGHEST, LOWEST);
        yaw += Mth.wrapDegrees(wantYaw - yaw) * FOLLOW;
        pitch += (wantPitch - pitch) * FOLLOW;
    }

    private static void burn() {
        for (int i = FLAMES.size() - 1; i >= 0; i--) {
            Flame flame = FLAMES.get(i);
            flame.lastX = flame.x;
            flame.lastY = flame.y;
            flame.lastZ = flame.z;
            if (++flame.age >= flame.life) {
                FLAMES.remove(i);
                continue;
            }
            flame.y += FLAME_RISE;
            flame.x *= FLAME_TAPER;
            flame.z *= FLAME_TAPER;
        }

        if (here < RISE_FROM) {
            return;
        }
        float stirred = here < WAKES_AT ? 1.4F : setAt >= 0 ? 1.8F : 1F;
        int more = Math.round(FLAMES_PER_TICK * stirred);
        for (int i = 0; i < more && FLAMES.size() < FLAMES_MAX; i++) {
            boolean legs = RANDOM.nextFloat() < LEGS_SHARE;
            float out = Mth.sqrt(RANDOM.nextFloat()) * (legs ? LEGS_RADIUS : BODY_RADIUS);
            float round = lit++ * GOLDEN_ANGLE;
            Flame flame = new Flame();
            flame.x = flame.lastX = Mth.cos(round) * out;
            flame.z = flame.lastZ = Mth.sin(round) * out * DEPTH;
            flame.y = flame.lastY = legs ? -LEGS_DOWN * RANDOM.nextFloat() : BODY_UP * RANDOM.nextFloat();
            flame.life = FLAME_LIFE_MIN + RANDOM.nextInt(FLAME_LIFE_SPAN);
            flame.size = (FLAME_SIZE_MIN + RANDOM.nextFloat() * FLAME_SIZE_SPREAD) * (legs ? 1F : BODY_FLAME);
            FLAMES.add(flame);
        }
    }

    private static boolean speaking() {
        if (line >= lines.size()) {
            return false;
        }
        String text = lines.get(line);
        int elapsed = here - lineStart;
        return elapsed < DreamPacing.typing(text) && !DreamPacing.pausing(text, elapsed);
    }

    private static float presence(float partialTick) {
        float now = here + partialTick;
        float gathered = Mth.clamp((now - RISE_FROM) / RISE_TICKS, 0F, 1F);
        gathered = gathered * gathered * (3F - 2F * gathered);
        float amount;
        if (now < WAKES_AT) {
            amount = gathered * ASLEEP;
        } else {
            float woke = Mth.clamp((now - WAKES_AT) / WAKING_TICKS, 0F, 1F);
            amount = ASLEEP + (1F - ASLEEP) * woke + 0.35F * Mth.sin(woke * Mth.PI);
        }
        if (setAt >= 0) {
            float gone = Mth.clamp((now - setAt) / SETS_TICKS, 0F, 1F);
            amount *= 1F - gone * gone;
        }
        return amount;
    }

    private static float[] gold() {
        return switch (mood) {
            case DreamSpeechPacket.COLD -> COLD_GOLD;
            case DreamSpeechPacket.WARM -> WARM_GOLD;
            default -> CALM_GOLD;
        };
    }

    private static int color() {
        return switch (mood) {
            case DreamSpeechPacket.COLD -> COLD_COLOR;
            case DreamSpeechPacket.WARM -> WARM_COLOR;
            default -> CALM_COLOR;
        };
    }

    public static void renderSky(Matrix4f view, float partialTick) {
        if (!dreaming()) {
            return;
        }
        float amount = presence(partialTick);
        if (amount <= 0.001F && FLAMES.isEmpty()) {
            return;
        }
        if (buffers == null) {
            buffers = new LayeredBuffers();
        }
        float time = here + partialTick;

        float drawYaw = lastYaw + Mth.wrapDegrees(yaw - lastYaw) * partialTick;
        float drawPitch = Mth.lerp(partialTick, lastPitch, pitch);
        Vec3 look = Vec3.directionFromRotation(drawPitch, drawYaw);
        Vector3f toward = new Vector3f((float) look.x, (float) look.y, (float) look.z);
        Vector3f right = new Vector3f(toward).cross(0F, 1F, 0F);
        if (right.lengthSquared() < 1.0E-4F) {
            right.set(1F, 0F, 0F);
        }
        right.normalize();
        Vector3f waist = new Vector3f(toward).mul(DISTANCE);

        float above = -drawPitch;
        Quaternionf lean = new Quaternionf().rotateAxis(
                Mth.clamp(above * LEAN_PER_DEGREE, 0F, LEAN_MOST) * Mth.DEG_TO_RAD, right.x, right.y, right.z);
        Vector3f up = lean.transform(new Vector3f(0F, 1F, 0F));
        float bow = Mth.clamp(above * BOW_PER_DEGREE, 0F, BOW_MOST);
        float talking = speaking() ? 1F : 0F;
        float nod = talking * 4F * Mth.sin(time * 0.45F);

        float breath = 0.9F + 0.1F * Mth.sin(time * 0.05F);
        float voice = 1F + talking * (0.08F + 0.06F * Mth.sin(time * 1.7F));
        float strength = amount * breath * voice;
        float[] gold = gold();

        Vector3f chest = new Vector3f(up).mul(SCALE * CHEST).add(waist);
        Vector3f across = new Vector3f(chest).normalize().cross(0F, 1F, 0F);
        if (across.lengthSquared() < 1.0E-4F) {
            across.set(1F, 0F, 0F);
        }
        across.normalize();
        Vector3f rise = new Vector3f(across).cross(new Vector3f(chest).normalize()).normalize();
        float burning = Math.max(amount, here < WAKES_AT
                ? Mth.clamp((here - RISE_FROM) / (float) RISE_TICKS, 0F, 1F) : 0F);

        float fogStart = RenderSystem.getShaderFogStart();
        FogRenderer.setupNoFog();
        RenderSystem.enableBlend();
        DreamPost.skyDepth();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        Matrix4f mirror = new Matrix4f(view).scale(1F, -1F, 1F);
        Vector3f sway = new Vector3f(right).mul(REFLECTION_SWAY * Mth.sin(time * 0.07F));
        float shimmer = REFLECTED * (0.88F + 0.12F * Mth.sin(time * 0.23F));
        flames(mirror, waist, chest, right, up, toward, across, rise, gold, burning * shimmer, partialTick);
        figure(mirror, new Vector3f(waist).add(sway), lean, drawYaw, time, gold, strength * shimmer, bow, nod, talking);

        flames(view, waist, chest, right, up, toward, across, rise, gold, burning, partialTick);
        figure(view, waist, lean, drawYaw, time, gold, strength, bow, nod, talking);

        RenderSystem.setShaderFogStart(fogStart);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void figure(Matrix4f view, Vector3f waist, Quaternionf lean, float facing, float time,
                               float[] gold, float strength, float bow, float nod, float talking) {
        if (strength <= 0.002F) {
            return;
        }
        for (int pass = -1; pass < HAZE_SCALE.length; pass++) {
            float grow = pass < 0 ? 1F : HAZE_SCALE[pass];
            float alpha = (pass < 0 ? BODY_ALPHA : HAZE_ALPHA[pass]) * strength;
            float driftX = pass < 0 ? 0F : 0.03F * (pass + 1) * Mth.sin(time * 0.05F + pass * 2.1F);
            float driftY = pass < 0 ? 0F : 0.02F * (pass + 1) * (1F + Mth.sin(time * 0.04F + pass));

            PoseStack pose = new PoseStack();
            pose.mulPose(view);
            pose.translate(waist.x, waist.y, waist.z);
            pose.mulPose(lean);
            pose.scale(SCALE, SCALE, SCALE);
            pose.translate(driftX, CHEST + driftY, 0F);
            pose.scale(grow, grow, grow);
            pose.translate(0F, -CHEST, 0F);
            SpiritFigure.draw(pose, buffers, facing, time, bow, nod, talking, argb(gold, Math.min(1F, alpha)));
            buffers.endBatch();
        }
    }

    private static int argb(float[] gold, float alpha) {
        return ((int) (alpha * 255F) << 24) | ((int) (gold[0] * 255F) << 16) | ((int) (gold[1] * 255F) << 8)
                | (int) (gold[2] * 255F);
    }

    private static void flames(Matrix4f view, Vector3f waist, Vector3f chest, Vector3f right, Vector3f up,
                               Vector3f toward, Vector3f across, Vector3f rise, float[] gold, float strength,
                               float partialTick) {
        if (strength <= 0.002F || FLAMES.isEmpty()) {
            return;
        }
        TextureAtlas sheet = (TextureAtlas) Minecraft.getInstance().getTextureManager()
                .getTexture(TextureAtlas.LOCATION_PARTICLES);
        float alpha = Math.min(1F, FLAME_ALPHA * strength);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        for (Flame flame : FLAMES) {
            float through = Mth.clamp((flame.age + partialTick) / flame.life, 0F, 1F);
            int cells = Math.round(flame.size * (1F - through * through) * 1.2F * FLAME_SCALE * SCALE / FLAME_PIXEL);
            if (cells <= 0) {
                continue;
            }
            float half = cells * FLAME_PIXEL * 0.5F;
            float x = Mth.lerp(partialTick, flame.lastX, flame.x), y = Mth.lerp(partialTick, flame.lastY, flame.y);
            float clear = faceClear(x, y, half / SCALE);
            if (clear <= 0.01F) {
                continue;
            }
            Vector3f at = snap(place(waist, right, up, toward, x, y, Mth.lerp(partialTick, flame.lastZ, flame.z)),
                    chest, across, rise, cells);
            int frame = Math.min(FLAME_FRAMES.length - 1, flame.age * (FLAME_FRAMES.length - 1) / flame.life);
            TextureAtlasSprite sprite = sheet.getSprite(FLAME_FRAMES[frame]);
            Vector3f side = new Vector3f(across).mul(half), tall = new Vector3f(rise).mul(half);
            float shown = alpha * clear;
            corner(buffer, view, at, side, tall, -1, -1, sprite.getU0(), sprite.getV1(), gold, shown);
            corner(buffer, view, at, side, tall, 1, -1, sprite.getU1(), sprite.getV1(), gold, shown);
            corner(buffer, view, at, side, tall, 1, 1, sprite.getU1(), sprite.getV0(), gold, shown);
            corner(buffer, view, at, side, tall, -1, 1, sprite.getU0(), sprite.getV0(), gold, shown);
        }
        draw(buffer);
    }

    static float faceClear(float x, float y, float half) {
        float into = Math.min(y + half - FACE_FROM, FACE_HALF + half - Math.abs(x));
        return into <= 0F ? 1F : 1F - Mth.clamp(into / FACE_FADE, 0F, 1F);
    }

    private static Vector3f snap(Vector3f at, Vector3f chest, Vector3f across, Vector3f rise, int cells) {
        Vector3f offset = new Vector3f(at).sub(chest);
        float shift = (cells & 1) == 1 ? 0.5F : 0F;
        float x = (Math.round(offset.dot(across) / FLAME_PIXEL - shift) + shift) * FLAME_PIXEL;
        float y = (Math.round(offset.dot(rise) / FLAME_PIXEL - shift) + shift) * FLAME_PIXEL;
        float depth = offset.dot(new Vector3f(chest).normalize());
        return new Vector3f(chest).add(new Vector3f(across).mul(x)).add(new Vector3f(rise).mul(y))
                .add(new Vector3f(chest).normalize().mul(depth));
    }

    private static void corner(BufferBuilder buffer, Matrix4f view, Vector3f at, Vector3f side, Vector3f tall,
                               int sideways, int upward, float u, float v, float[] gold, float alpha) {
        buffer.addVertex(view, at.x + side.x * sideways + tall.x * upward, at.y + side.y * sideways + tall.y * upward,
                        at.z + side.z * sideways + tall.z * upward)
                .setUv(u, v).setColor(gold[0], gold[1], gold[2], alpha);
    }

    private static Vector3f place(Vector3f waist, Vector3f right, Vector3f up, Vector3f back, float x, float y,
                                  float z) {
        return new Vector3f(waist).add(new Vector3f(right).mul(x * SCALE)).add(new Vector3f(up).mul(y * SCALE))
                .add(new Vector3f(back).mul(z * SCALE));
    }

    private static void draw(BufferBuilder buffer) {
        MeshData mesh = buffer.build();
        if (mesh == null) {
            return;
        }
        RenderSystem.enableBlend();
        DreamPost.skyDepth();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        BufferUploader.drawWithShader(mesh);
        RenderSystem.defaultBlendFunc();
    }

    public static void renderHud(GuiGraphics graphics) {
        speech(graphics);
        DreamWaking.renderHud(graphics);
    }

    private static void speech(GuiGraphics graphics) {
        if (!dreaming() || line >= lines.size()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        String text = lines.get(line);
        int elapsed = here - lineStart;
        boolean last = line == lines.size() - 1;

        float alpha = 1F;
        if (last && answers.isEmpty()) {
            float over = elapsed - DreamPacing.typing(text) - DreamPacing.hold(text);
            alpha = 1F - Mth.clamp(over / FADE_TICKS, 0F, 1F);
        }
        if (alpha <= 0.02F) {
            return;
        }

        String said = text.substring(0, Math.min(text.length(), DreamPacing.shown(text, elapsed)));
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        int wrap = Math.min(SPEECH_WIDE, width * 3 / 5);
        int centre = width / 2;
        int chat = chatEdge(minecraft);
        boolean high = false;
        if (centre - wrap / 2 < chat + SPEECH_GAP) {
            int room = width - chat - SPEECH_GAP * 2;
            if (room >= SPEECH_NARROWEST) {
                wrap = Math.min(wrap, room);
                centre = chat + SPEECH_GAP + room / 2;
            } else {
                high = true;
            }
        }
        List<FormattedCharSequence> wrapped = font.split(Component.literal(said), wrap);
        int rows = wrapped.size() * (font.lineHeight + 3);
        int top = high ? SPEECH_HIGH : height - 88 - rows;
        int bottom = top + rows;

        int shade = (int) (alpha * 150) << 24;
        if (high) {
            graphics.fillGradient(0, 0, width, bottom + 22, shade, 0);
        } else {
            graphics.fillGradient(0, top - 22, width, height, 0, shade);
        }

        int argb = ((int) (alpha * 255) << 24) | color();
        for (int i = 0; i < wrapped.size(); i++) {
            FormattedCharSequence row = wrapped.get(i);
            graphics.drawString(font, row, centre - font.width(row) / 2, top + i * (font.lineHeight + 3),
                    argb, true);
        }

        if (last && answersPosted && !answers.isEmpty()) {
            Component looked = DreamPillars.lookedAt();
            if (looked != null) {
                Component said2 = Component.literal(CHOSEN_MARK).append(looked);
                graphics.drawString(font, said2, centre - font.width(said2) / 2, bottom + 6,
                        ((int) (alpha * 255) << 24) | 0xFFF4DA, true);
            } else if (DreamPillars.offering()) {
                Component hint = Component.translatable("message.jojoha.dream.hint",
                        minecraft.options.keyUse.getTranslatedKeyMessage());
                graphics.drawString(font, hint, centre - font.width(hint) / 2, bottom + 6,
                        ((int) (alpha * 140) << 24) | 0xA89878, true);
            }
        }
    }

    private static int chatEdge(Minecraft minecraft) {
        if (minecraft.options.chatVisibility().get() == ChatVisiblity.HIDDEN) {
            return 0;
        }
        ChatComponent chat = minecraft.gui.getChat();
        return Mth.ceil(chat.getWidth() * chat.getScale()) + 12;
    }
}
