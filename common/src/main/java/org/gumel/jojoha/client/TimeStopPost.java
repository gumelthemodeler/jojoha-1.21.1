package org.gumel.jojoha.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.StandEntity;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the time stop over the finished frame.
 *
 * <p>One pass, one program, no geometry. What replaced a drawn sphere and twelve overridden vanilla
 * shaders - see {@code jojoha_timestop.fsh} for why that arrangement was the wrong shape.
 *
 * <h2>Reading the frame back</h2>
 *
 * <p><b>Main</b> is what the world was drawn into, and is read for both colour and depth.
 * <b>Scratch</b> is where the graded image is written, because sampling a texture attached to the
 * framebuffer currently bound for writing is undefined - so the pass reads main, writes scratch, and
 * blits scratch back. That is the whole of the plumbing.
 *
 * <h2>How the unfrozen are kept out of the grade, and why not by drawing them</h2>
 *
 * <p>Bodies still living in real time must not be drained with the scenery. The obvious way to say
 * which pixels those are is to render them a second time into a mask buffer and test its alpha, and
 * that is what this did. It went wrong three times, in three unrelated ways, because a second render
 * is not a cheap question - it is the entire entity pipeline run again, and it brings every one of
 * that pipeline's assumptions with it:
 *
 * <ul>
 *   <li>vanilla's output state shards rebind the framebuffer mid-batch, but only under Fabulous
 *       graphics, so the mask came out empty on some machines and not others;</li>
 *   <li>flushing the shared vertex buffer consumed level geometry that had not been drawn yet, so
 *       the Stand was spent filling in its own silhouette and never reached the screen;</li>
 *   <li>and a depth bias with a slope term changes size as the camera turns, which opened and closed
 *       person-shaped holes through solid walls.</li>
 * </ul>
 *
 * <p>Each of those had a fix. The pattern was the problem: all three produced the same symptom - a
 * body-shaped tear in the grade with no body in it - and none was visible from the code that caused
 * it.
 *
 * <p>So the mask is gone. The unfrozen are described to the shader as what they are: a couple of
 * boxes in world space. A pixel is exempt when the point it reconstructs to lies inside one. No
 * second render, no extra framebuffer, no shared vertex buffer, no render types, and nothing that
 * behaves differently on one graphics setting than another.
 *
 * <p>It is a volume test rather than a silhouette, so world geometry genuinely intersecting a body -
 * the floor beneath it, a wall it leans on - keeps its colour too. That is a real cost, and it is
 * bounded by the size of a player. A tear showing the world through a body is bounded by nothing,
 * and is what this replaced.
 */
public final class TimeStopPost {
    /** The distance the sky is treated as sitting at, so the boundary rings never land on it. */
    private static final float SKY_DISTANCE = 512F;

    /**
     * How far past a body its exemption reaches, in blocks.
     *
     * <p>Enough to cover what a model hangs outside its collision box - hair, a cape, a Stand's
     * shoulders - without taking a bite out of the ground underneath. Which is why the floor gets
     * almost none of it: the bottom face is the one with something solid pressed against it.
     */
    private static final float BODY_MARGIN = 0.05F;
    private static final float FLOOR_MARGIN = 0.02F;

    /** A box nothing can be inside, for a slot with no body in it. */
    private static final float NOWHERE_MIN = 1F;
    private static final float NOWHERE_MAX = -1F;

    private static RenderTarget scratch;

    private TimeStopPost() {
    }

    /**
     * Called once a frame from each loader's level render hook, after the world is drawn.
     *
     * @param modelView  the world's model-view matrix, for turning depth back into position
     * @param projection the projection it was drawn with
     */
    public static void render(Matrix4f modelView, Matrix4f projection, Camera camera,
                              float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ShaderInstance shader = JojohaShaders.timeStop();

        if (level == null || shader == null || modelView == null || projection == null) {
            return;
        }

        float strength = TimeStopShader.strength();
        float globalDrain = TimeStopShader.globalDrain();
        if (strength <= 0.001F && globalDrain <= 0.001F) {
            // Nothing to grade. Worth the early return: this runs every frame of every session.
            return;
        }

        RenderTarget main = minecraft.getMainRenderTarget();
        RenderTarget scratchTarget = ensure(scratch, main.width, main.height);
        scratch = scratchTarget;

        Vec3 cameraPos = camera.getPosition();

        Matrix4f forward = new Matrix4f(projection).mul(modelView);
        Matrix4f inverse = new Matrix4f(forward).invert();
        Vec3 centre = TimeStopView.centre().subtract(cameraPos);

        shader.setSampler("DiffuseSampler", main.getColorTextureId());
        shader.setSampler("DepthSampler", main.getDepthTextureId());

        shader.safeGetUniform("InverseTransform").set(inverse);
        shader.safeGetUniform("ForwardTransform").set(forward);
        shader.safeGetUniform("SphereCentre").set(
                (float) centre.x, (float) centre.y, (float) centre.z);

        shader.safeGetUniform("Field").set(TimeStopShader.radius(), TimeStopShader.drain(),
                TimeStopShader.skyShare(), TimeStopShader.ringWidth());
        shader.safeGetUniform("Inner").set(TimeStopShader.innerRadius(),
                TimeStopShader.innerStrength(), TimeStopShader.hueRoll(),
                TimeStopShader.huePhase());
        shader.safeGetUniform("Grade").set(TimeStopShader.desaturation(), TimeStopShader.darken(),
                TimeStopShader.ringStrength(), globalDrain);
        shader.safeGetUniform("Lens").set(TimeStopShader.pull(), TimeStopShader.ringOffset(),
                0F, 0F);
        shader.safeGetUniform("Break").set(TimeStopShader.shatter(),
                TimeStopShader.greyShatter(), TimeStopShader.shardScale(), 0F);
        shader.safeGetUniform("Shake").set(TimeStopShader.squiggle(), TimeStopShader.waveScale(),
                TimeStopShader.wavePhase(), 0F);
        shader.safeGetUniform("SkyDistance").set(SKY_DISTANCE);

        setExemptions(shader, minecraft, level, cameraPos, partialTick);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.viewport(0, 0, main.width, main.height);

        scratchTarget.bindWrite(false);
        RenderSystem.setShader(() -> shader);

        BufferBuilder builder = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(-1F, -1F, 0F);
        builder.addVertex(1F, -1F, 0F);
        builder.addVertex(1F, 1F, 0F);
        builder.addVertex(-1F, 1F, 0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());

        shader.clear();
        scratchTarget.unbindWrite();

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, scratchTarget.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);

        main.bindWrite(true);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /** Hands the shader the boxes the grade must not touch. Two slots, both always written. */
    private static void setExemptions(ShaderInstance shader, Minecraft minecraft, ClientLevel level,
                                      Vec3 cameraPos, float partialTick) {
        List<Entity> free = unfrozen(minecraft, level);

        for (int slot = 0; slot < 2; slot++) {
            String min = slot == 0 ? "FreeMinA" : "FreeMinB";
            String max = slot == 0 ? "FreeMaxA" : "FreeMaxB";

            if (slot >= free.size()) {
                // Written every frame either way. A slot left at whatever it held last frame is a
                // patch of colour standing where somebody used to be.
                shader.safeGetUniform(min).set(NOWHERE_MIN, NOWHERE_MIN, NOWHERE_MIN);
                shader.safeGetUniform(max).set(NOWHERE_MAX, NOWHERE_MAX, NOWHERE_MAX);
                continue;
            }

            Entity body = free.get(slot);
            float x = (float) (Mth.lerp(partialTick, body.xOld, body.getX()) - cameraPos.x);
            float y = (float) (Mth.lerp(partialTick, body.yOld, body.getY()) - cameraPos.y);
            float z = (float) (Mth.lerp(partialTick, body.zOld, body.getZ()) - cameraPos.z);

            float half = body.getBbWidth() * 0.5F + BODY_MARGIN;
            float tall = body.getBbHeight() + BODY_MARGIN;

            shader.safeGetUniform(min).set(x - half, y + FLOOR_MARGIN, z - half);
            shader.safeGetUniform(max).set(x + half, y + tall, z + half);
        }
    }

    /**
     * The local player, if the stop does not hold them, and whatever Stand of theirs is out.
     *
     * <p>Only the local player, because only the local client knows whether the local player can
     * move. Another player standing unfrozen beside you still greys over, and will until the server
     * says who is immune.
     */
    private static List<Entity> unfrozen(Minecraft minecraft, ClientLevel level) {
        List<Entity> bodies = new ArrayList<>(2);
        if (minecraft.player == null || !movesFreely()) {
            return bodies;
        }

        // In first person the body is not drawn, so exempting the space it stands in would only take
        // the colour out of whatever is actually there - the floor, usually. The held item is drawn
        // after this pass entirely and is never graded either way.
        if (!minecraft.options.getCameraType().isFirstPerson()
                || minecraft.getCameraEntity() != minecraft.player) {
            bodies.add(minecraft.player);
        }

        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof StandEntity stand && stand.getOwner() == minecraft.player) {
                bodies.add(stand);
                break;
            }
        }

        return bodies;
    }

    /**
     * Whether the local player is still living in real time while the world round them is not.
     *
     * <p>Two ways to be: holding the stop yourself, or being able to throw one at all - anyone with
     * time stop unlocked walks out of everyone elses, which is what TimeStopSystem.catchPlayers
     * decides on the server and what this has to agree with on the client.
     */
    private static boolean movesFreely() {
        return ClientPlayerDataCache.data.timeStopHeldTicks > 0
                || !ClientPlayerDataCache.data.isTimeStopFrozen();
    }

    private static RenderTarget ensure(RenderTarget target, int width, int height) {
        if (target == null) {
            RenderTarget created = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            created.setFilterMode(GL30.GL_NEAREST);
            return created;
        }

        if (target.width != width || target.height != height) {
            target.resize(width, height, Minecraft.ON_OSX);
        }
        return target;
    }
}
