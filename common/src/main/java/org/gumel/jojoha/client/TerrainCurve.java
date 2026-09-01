package org.gumel.jojoha.client;

import net.minecraft.util.Mth;

/**
 * The colour front: how far the greyscale has swept out from the caster, and how lit its leading
 * line is.
 *
 * <p>This class used to own a fold as well - a radial displacement that reared the landscape up
 * around the player and had to be walked back down again over the length of the stop. It is gone.
 * Every version of it traded one problem for another: high enough to be dramatic meant terrain
 * standing against the sky, low enough to sit right meant a warp too generic to be worth the
 * machinery, and either way it was a shape held for ten seconds when it wanted to be a moment. What
 * is left is the part that always worked - a line crossing the ground with the world's colour going
 * out behind it.
 *
 * <p>The name is now a lie and is kept only because it is threaded through the shaders' uniform
 * names.
 */
public final class TerrainCurve {
    /**
     * The stop's reach, in blocks, and how long the front takes to get there.
     *
     * <p>The same pair the sphere itself uses, so the ring the front draws on the ground and the
     * shell standing in the air are always in the same place. Two different numbers here is the
     * fastest way to make both look painted on.
     *
     * <p>This is the whole extent of the effect. The front used to carry on past it to four hundred
     * blocks, on the reasoning that a boundary nobody can see is a boundary nobody can complain
     * about - which stopped being true the moment the stop grew a visible edge. The sphere said the
     * stop was twenty blocks across and the colour said it was the whole world; the sphere is the
     * one telling the truth.
     */
    /**
     * The radius of the stop, taken from the system that decides it.
     *
     * <p>Read rather than repeated. These were two 20s that used to be a 20 and a 24, which is how
     * mobs ended up frozen outside anything the player could see - two numbers for one distance stay
     * equal only for as long as nobody edits one of them.
     */
    private static final float SPHERE_RADIUS = (float) org.gumel.jojoha.stand.skill.TimeStopSystem.RADIUS;
    private static final float SPHERE_SECONDS = 0.55F;

    /**
     * Width of the leading edge, in blocks.
     *
     * <p>Wide enough that the colour ramps across it instead of switching along a single ring of
     * vertices, and it is also the band the line itself is drawn in - but no wider. At eleven it was
     * over half the radius, so the boundary of a twenty block sphere was smeared across nine to
     * thirty-one blocks and the stop had no edge you could point at.
     */
    private static final float EDGE_WIDTH = 3F;

    /** See {@link #setOrigin}. A power of two, so the wrap never lands on a fractional block. */
    private static final double ORIGIN_WRAP = 4096.0;

    /**
     * How fast the greyscale arrives and leaves.
     *
     * <p>In quickly behind the front, out slowly - colour draining is the stop landing, and colour
     * returning is it wearing off.
     */
    private static final float DRAIN_RISE_PER_SECOND = 3F;
    private static final float DRAIN_RELEASE_PER_SECOND = 0.85F;

    private static float drain;
    /**
     * How lit the leading line is.
     *
     * <p>Separate from the drain, which it used to be keyed to. The line marks the front, and the
     * front sets out before there is anything to drain - keyed to the drain it would be dark for the
     * first moments of the very sweep it exists to describe.
     */
    private static float line;
    private static float radius;
    private static float elapsed;
    /** How far through the stop we are, 0 to 1 - pushed in by the clock. */
    private static float stopProgress;
    private static float originX;
    private static float originZ;



    private TerrainCurve() {
    }

    /**
     * How much colour the world has lost - the time stop's, or the mask's, whichever is stronger.
     *
     * <p>Both effects want the same thing of the shaders, so they share the uniform rather than each
     * having one. Taking the larger rather than adding them means the two overlapping cannot push
     * past fully grey, and neither can cancel the other out.
     */
    public static float drain() {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null) {
            return drain;
        }

        return Math.max(drain, VampireColour.grey((float) minecraft.level.getGameTime()
                + minecraft.getTimer().getGameTimeDeltaPartialTick(false)));
    }

    public static float line() {
        return line;
    }

    /** How far the front has reached, in blocks. Huge when nothing is sweeping, so nothing is inside. */
    public static float radius() {
        return radius;
    }

    public static float edgeWidth() {
        return EDGE_WIDTH;
    }

    public static float originX() {
        return originX;
    }

    public static float originZ() {
        return originZ;
    }

    /** Told by the clock how far through the stop we are. Drives the colour coming back. */
    public static void setStopProgress(float progress) {
        stopProgress = Mth.clamp(progress, 0F, 1F);
    }

    /**
     * Records where the camera is, so the shader can pin its noise to the world.
     *
     * <p>Vertices arrive at the shader positioned relative to the camera, so noise sampled from them
     * alone would slide across the landscape as the player walked - and a player is not frozen
     * inside their own time stop. Handing the camera's position over lets the shader add it back and
     * sample in world space instead.
     *
     * <p>Wrapped to a few thousand blocks first: a float cannot hold a coordinate in the millions
     * precisely enough to sample noise from, and the pattern would visibly quantise out at the edges
     * of a large world. The cost is a seam every few thousand blocks, which nobody will stand on
     * twice.
     */
    public static void setOrigin(double cameraX, double cameraZ) {
        originX = (float) Mth.positiveModulo(cameraX, ORIGIN_WRAP);
        originZ = (float) Mth.positiveModulo(cameraZ, ORIGIN_WRAP);
    }

    public static boolean isActive() {
        return drain != 0F || line != 0F;
    }

    /**
     * Advances the front.
     *
     * @param foldStrength  whether the front should be sweeping at all
     * @param drainStrength whether the colour should be going yet - held back behind the front, so
     *                      the stop lands on a world that is still in colour and drains after
     * @param deltaSeconds  real time since the last frame, so the sweep runs at the same speed
     *                      whatever the framerate
     */
    public static void tick(float foldStrength, float drainStrength, float deltaSeconds) {
        float held = Mth.clamp(foldStrength, 0F, 1F);
        float draining = Mth.clamp(drainStrength, 0F, 1F);

        // The line lets go at the same pace as the colour, so the front does not blink out from
        // under a world that is still grey.
        line = Mth.approach(line, held,
                (held > line ? DRAIN_RISE_PER_SECOND : DRAIN_RELEASE_PER_SECOND) * deltaSeconds);

        // The colour holds for the whole stop and then simply fades back, over about a second and a
        // quarter. It used to start returning at seventy-two percent of the way through, so the world
        // was already back in colour by the time the stop actually ended - and then the stop ending
        // moved several other things at once on top of that. One fade, at the end, with nothing else
        // happening alongside it, is what makes leaving a stop feel like leaving rather than like a
        // sequence of switches being thrown.
        drain = Mth.approach(drain, draining,
                (draining > drain ? DRAIN_RISE_PER_SECOND : DRAIN_RELEASE_PER_SECOND) * deltaSeconds);

        if (held <= 0F) {
            elapsed = 0F;
            stopProgress = 0F;

            // The radius is held where it was until the grey has finished releasing, so the world
            // fades back to colour where it stands instead of the front snapping past it first.
            if (drain <= 0.001F) {
                drain = 0F;
                line = 0F;
                radius = Float.MAX_VALUE;
            }
            return;
        }

        elapsed += deltaSeconds;

        // Out with the sphere and then held. There is no second, slower stage any more: the front
        // has nowhere further to go, because the edge of the sphere is the edge of the stop.
        radius = SPHERE_RADIUS * smoothstep(0F, SPHERE_SECONDS, elapsed);
    }

    /** Smoothstep, on the interval given. */
    private static float smoothstep(float from, float to, float value) {
        float u = Mth.clamp((value - from) / Math.max(to - from, 0.0001F), 0F, 1F);
        return u * u * (3F - 2F * u);
    }

    public static void clear() {
        drain = 0F;
        line = 0F;
        radius = Float.MAX_VALUE;
        elapsed = 0F;
        stopProgress = 0F;
    }
}
