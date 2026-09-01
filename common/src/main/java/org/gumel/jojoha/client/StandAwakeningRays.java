package org.gumel.jojoha.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.item.StandArrowRitual;
import org.gumel.jojoha.mixin.client.EnderDragonRendererInvoker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Ender Dragon's death rays, erupting from a player as their Stand awakens.
 *
 * <p>This is literally vanilla's effect, not a lookalike: {@code renderRays} is reached through
 * {@link EnderDragonRendererInvoker}. Vanilla drives it with the dragon's death timer expressed
 * as a 0..1 progress value, so all this has to supply is its own (much shorter) ramp.
 */
public final class StandAwakeningRays {
    /**
     * How long the rays are on screen, and how long the white then takes to bleed off afterwards.
     *
     * <p>Both come from {@link StandArrowRitual} rather than being defined here, because the
     * server schedules the burst and the Stand's arrival for the exact tick this fade finishes -
     * two copies of these numbers would drift and put the bang in the middle of the glow.
     */
    public static final int DURATION_TICKS = StandArrowRitual.AWAKEN_RAYS_TICKS;
    private static final int GLOW_FADE_TICKS = StandArrowRitual.AWAKEN_GLOW_FADE_TICKS;
    private static final int TOTAL_TICKS = DURATION_TICKS + GLOW_FADE_TICKS;

    /**
     * How long the mask takes to stop burning, and why it is not the body's fourteen.
     *
     * <p>The body's glow is white light over skin: it can drop out quickly because the skin
     * underneath is the thing you were looking at anyway. The mask <em>is</em> the light - there is
     * nothing behind it to return to - so the same fade reads as a lamp being switched off rather
     * than as something dying down. Nearly three times as long, and eased at both ends.
     */
    private static final int BURN_FADE_TICKS = 40;
    private static final int BURN_TOTAL_TICKS = DURATION_TICKS + BURN_FADE_TICKS;

    /**
     * Fraction of the ray window spent climbing to full before the count starts receding.
     *
     * <p>Winding the count back down is what stops the ending reading as a hard cut. Fading alpha
     * alone dims every shaft at once, which still looks like someone switched the effect off;
     * lowering vanilla's progress instead retires the beams a few at a time (its ray loop is
     * seeded, so a smaller count drops the most recently generated ones and leaves the rest
     * exactly where they were), so the burst visibly thins out before it goes.
     */
    private static final float RAY_RISE_FRACTION = 0.45F;

    /**
     * Vanilla's ray progress only ever climbs to this here, never to 1.
     *
     * <p>Past 0.8 the dragon effect enters its finale: {@code renderRays} starts fading the beam
     * cores toward white while simultaneously inflating their length and width, which collapses
     * the whole thing into an expanding white blob. That reads as the dragon dissolving, but for a
     * Stand awakening it just swallows the rays. Stopping at 0.8 keeps the growing shafts of light
     * and drops the blob entirely.
     */
    private static final float MAX_RAY_PROGRESS = 0.8F;

    private static final int FADE_IN_TICKS = 10;
    private static final int FADE_OUT_TICKS = 12;

    /** Camera shake: hardest at the instant of awakening, easing off over this window. */
    private static final int SHAKE_TICKS = 20;
    /** Peak camera deflection in degrees. Enough to feel like a shock without inducing motion sickness. */
    private static final float SHAKE_DEGREES = 1.6F;

    /**
     * Scratch buffer for the rays. They're drawn through a private buffer flushed immediately
     * rather than the shared one, because the fade needs {@link RenderSystem#setShaderColor} to be
     * in effect at <em>draw</em> time - geometry handed to the shared buffer source isn't drawn
     * until its batch is flushed much later, long after any colour we set has been reset.
     */
    private static final ByteBufferBuilder RAY_BUFFER = new ByteBufferBuilder(1536);

    /** Player UUID -> client game time (ticks) the burst began. */
    private static final Map<UUID, Float> ACTIVE = new HashMap<>();

    private StandAwakeningRays() {
    }

    public static void begin(UUID playerId, float clientTimeTicks) {
        begin(playerId, clientTimeTicks, 1F, 1F, 1F);
    }

    /**
     * The same burst in a colour.
     *
     * <p>The beams are drawn through the position_color shader, whose fragment stage multiplies by
     * the shader colour modulator - the same multiply that already fades them in and out. So a tint
     * costs nothing beyond remembering it: white for a Stand tearing loose, red for a mask taking
     * its wearer.
     */
    public static void begin(UUID playerId, float clientTimeTicks, float red, float green, float blue) {
        ACTIVE.put(playerId, clientTimeTicks);
        TINT.put(playerId, new float[] {red, green, blue});
    }

    /** Per-player ray colour, defaulting to white for anything that never asked. */
    private static final java.util.Map<UUID, float[]> TINT = new java.util.HashMap<>();

    /** The colour this player's awakening is running in - white unless something asked otherwise. */
    public static float[] glowTint(UUID playerId) {
        return tint(playerId);
    }

    /** Where each kind of awakening radiates from, as a share of the body's height. */
    private static final float CHEST_HEIGHT = 0.6F;
    private static final float HEAD_HEIGHT = 0.92F;

    private static boolean isWhiteTint(float[] rgb) {
        return rgb[0] >= 0.99F && rgb[1] >= 0.99F && rgb[2] >= 0.99F;
    }

    private static float[] tint(UUID playerId) {
        return TINT.getOrDefault(playerId, WHITE_TINT);
    }

    private static final float[] WHITE_TINT = {1F, 1F, 1F};

    public static void tick(float clientTimeTicks) {
        // The longer of the two windows, or the burn would be cut off by the entry disappearing
        // out from under it rather than by its own curve reaching zero.
        ACTIVE.entrySet().removeIf(entry -> clientTimeTicks - entry.getValue() > BURN_TOTAL_TICKS);
    }

    public static void clear() {
        ACTIVE.clear();
        TINT.clear();
    }

    /**
     * How completely the player should be washed out to white right now, 0 (not at all) to 1
     * (solid white). Read by the {@code LivingEntityRenderer} mixin, which feeds it into vanilla's
     * own white-overlay channel - the same one that flashes a creeper before it detonates.
     */
    public static float whiteFlash(Player player, float clientTimeTicks) {
        Float start = ACTIVE.get(player.getUUID());
        if (start == null) {
            return 0F;
        }

        float elapsed = clientTimeTicks - start;
        if (elapsed < 0F || elapsed > TOTAL_TICKS) {
            return 0F;
        }
        // Solid white for the whole time the rays are burning...
        if (elapsed <= DURATION_TICKS) {
            return 1F;
        }
        // ...then eased back to normal skin once they're spent.
        return Mth.clamp(1F - (elapsed - DURATION_TICKS) / GLOW_FADE_TICKS, 0F, 1F);
    }

    /**
     * How hard a mask worn by this player is burning, 0 to 1.
     *
     * <p>Holds while the rays do, then eases out on a smootherstep - flat at both ends, steepest in
     * the middle. A linear fade on an additive pass is the thing that reads as abrupt: perceived
     * brightness moves fastest near zero, so a straight ramp spends its first half in a range the
     * eye cannot separate and then falls off the end all at once. Easing puts the slow part where
     * the seeing happens.
     */
    public static float burn(Player player, float clientTimeTicks) {
        Float start = ACTIVE.get(player.getUUID());
        if (start == null) {
            return 0F;
        }

        float elapsed = clientTimeTicks - start;
        if (elapsed < 0F || elapsed > BURN_TOTAL_TICKS) {
            return 0F;
        }
        if (elapsed <= DURATION_TICKS) {
            return 1F;
        }

        float remaining = Mth.clamp(1F - (elapsed - DURATION_TICKS) / BURN_FADE_TICKS, 0F, 1F);
        return remaining * remaining * remaining * (remaining * (remaining * 6F - 15F) + 10F);
    }

    /**
     * Camera deflection for an awakening player, as a (yaw, pitch) degree offset. Zero once the
     * shake has died away.
     *
     * <p>Decays quadratically rather than linearly so the jolt hits hard and then eases out,
     * instead of grinding down at a constant rate and outstaying its welcome.
     */
    public static float[] cameraShake(Player player, float clientTimeTicks) {
        Float start = ACTIVE.get(player.getUUID());
        if (start == null) {
            return null;
        }

        float elapsed = clientTimeTicks - start;
        if (elapsed < 0F || elapsed > SHAKE_TICKS) {
            return null;
        }

        float falloff = 1F - (elapsed / SHAKE_TICKS);
        float intensity = falloff * falloff * SHAKE_DEGREES;

        // Two incommensurable frequencies so the motion never settles into a readable rhythm -
        // a single sine reads as a smooth wobble rather than a shock.
        float yaw = (float) Math.sin(elapsed * 2.7F) * intensity;
        float pitch = (float) Math.cos(elapsed * 3.9F) * intensity;
        return new float[] {yaw, pitch};
    }

    /**
     * Draws the rays around a player mid-awakening. Called from the player renderer with the pose
     * stack already at the entity's origin.
     *
     * <p>Both render types are used, exactly as the dragon does: {@code dragonRays} draws the
     * unoccluded flare and {@code dragonRaysDepth} the depth-tested pass, which together are what
     * make the beams read as passing through the world rather than being pasted over it.
     */
    public static void render(Player player, PoseStack poseStack, MultiBufferSource bufferSource,
                              float clientTimeTicks) {
        Float start = ACTIVE.get(player.getUUID());
        if (start == null) {
            return;
        }

        float elapsed = clientTimeTicks - start;
        if (elapsed < 0F || elapsed > DURATION_TICKS) {
            return;
        }

        float alpha = fadeAlpha(elapsed);
        if (alpha <= 0F) {
            return;
        }

        float progress = rayProgress(elapsed);

        MultiBufferSource.BufferSource rays = MultiBufferSource.immediate(RAY_BUFFER);

        poseStack.pushPose();
        // Where the beams come from. A Stand tears out of the chest, so the white burst is lifted
        // to roughly there; the mask's is the mask, which is on the face - and a red burst radiating
        // from the ribs of someone whose head is the thing lighting up points at the wrong place.
        float[] rgb = tint(player.getUUID());
        float height = player.getBbHeight() * (isWhiteTint(rgb) ? CHEST_HEIGHT : HEAD_HEIGHT);
        poseStack.translate(0.0F, height, 0.0F);
        EnderDragonRendererInvoker.jojoha$renderRays(poseStack, progress, rays.getBuffer(RenderType.dragonRays()));
        EnderDragonRendererInvoker.jojoha$renderRays(poseStack, progress, rays.getBuffer(RenderType.dragonRaysDepth()));
        poseStack.popPose();

        // dragonRays draws with the position_color shader, whose fragment stage multiplies the
        // vertex colour by the shader colour modulator - so this is what actually fades the beams
        // in and out rather than having them snap on at full strength.
        RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], alpha);
        rays.endBatch();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    /** Ramps up at the start and back down at the end, so the burst neither pops on nor cuts out. */
    private static float fadeAlpha(float elapsed) {
        float remaining = DURATION_TICKS - elapsed;
        float in = Mth.clamp(elapsed / FADE_IN_TICKS, 0F, 1F);
        float out = Mth.clamp(remaining / FADE_OUT_TICKS, 0F, 1F);
        return Math.min(in, out);
    }

    /**
     * Vanilla's 0..1 ray progress, which its loop turns into a ray count: climbs to
     * {@link #MAX_RAY_PROGRESS} over the first stretch, then recedes back toward nothing so the
     * shafts retire gradually instead of the whole burst blinking off. See RAY_RISE_FRACTION.
     */
    private static float rayProgress(float elapsed) {
        float t = Mth.clamp(elapsed / DURATION_TICKS, 0F, 1F);
        float shape = t <= RAY_RISE_FRACTION
                ? t / RAY_RISE_FRACTION
                : 1F - (t - RAY_RISE_FRACTION) / (1F - RAY_RISE_FRACTION);
        return Mth.clamp(shape, 0F, 1F) * MAX_RAY_PROGRESS;
    }
}
