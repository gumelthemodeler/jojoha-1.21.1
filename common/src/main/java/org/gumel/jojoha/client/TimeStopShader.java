package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Everything the time stop looks like, as numbers, one frame at a time.
 *
 * <p>{@link TimeStopPost} draws it and this decides it. The split is on purpose: the pass is all
 * framebuffers and matrices and cares about nothing else, and the shape of the effect - how it
 * arrives, how long it lingers, how hard it bites - is the part anyone will actually want to change.
 *
 * <h2>The shape of a stop</h2>
 *
 * <p>Both balls burst out of the caster together. The outer one reaches the full radius and stays
 * there for the length of the stop: that is where the world has been held, and its edge is a place
 * you can walk to. The inner one holds a beat at full width and then falls back inward to nothing,
 * and what it leaves behind it is the drained world - so the collapse of the inversion is also the
 * arrival of the grey. When time starts again the whole thing fades out over about a second.
 *
 * <h2>What is not here</h2>
 *
 * <p>A sweep. There was a hand that went once round the caster taking whatever it passed, and it had
 * to go: a bearing is undefined on the axis and unstable near it, so it needed the whole test blended
 * away near the middle, plus a seam where the angle wraps - and both of those are things you can see.
 * A radius has no angle in it and so has neither problem.
 *
 * <p>And nothing measured on the screen. An earlier version closed a vignette over the graded world,
 * centred on the viewport rather than on the stop, so the darkest part of the effect stayed in front
 * of the player however far they walked and the stop appeared to follow them about. Every value below
 * is a distance from the stop's own centre or a world position.
 */
public final class TimeStopShader {
    /**
     * How the stop arrives and leaves, in seconds.
     *
     * <p>Out fast, hold briefly, back in slowly. The asymmetry is the whole gesture: something thrown
     * outward and then drawn back is read as one motion with a direction, where a symmetrical pulse
     * is read as a flash.
     */
    private static final float EXPAND_SECONDS = 0.4F;
    private static final float HOLD_SECONDS = 0.25F;
    private static final float CONTRACT_SECONDS = 1.1F;

    /**
     * And the contraction is bounded by the stop it belongs to.
     *
     * <p>A share, not a duration. A second and a bit is fine against a ten second hold and absurd
     * against a tapped one - the inversion would still be collapsing when time restarted, so the
     * thing meant to sweep through and leave would instead be the whole of what anyone saw.
     */
    private static final float CONTRACT_SHARE = 0.45F;
    private static final float CONTRACT_MIN_SECONDS = 0.2F;

    /** How long the colour takes to go once the boundary is out. */
    private static final float DRAIN_FADE_IN_SECONDS = 0.15F;

    /**
     * How long the drained world takes to come apart once time starts again.
     *
     * <p>It used to fade, and fading was the wrong verb for it. A stop does not thin out and let the
     * colour seep back; it fails. The same shell that cracked on the way in cracks again, and the
     * world comes back through the gaps as the plates leave - which also means the ending is made of
     * the same material as the beginning instead of being a dissolve bolted onto it.
     */
    private static final float BREAK_OUT_SECONDS = 0.9F;

    /** How far the stop reaches, read from the system that decides it rather than repeated. */
    private static final float RADIUS = (float) org.gumel.jojoha.stand.skill.TimeStopSystem.RADIUS;

    /**
     * How much of the effect the sky takes.
     *
     * <p>Partial. The sky is drawn at no distance at all, so it is not in the sphere and never can
     * be - but an inverted world with an untouched sky over it reads as a filter applied to part of
     * the screen rather than as a world that has stopped.
     */
    private static final float SKY_SHARE = 0.55F;

    /** How wide the white bands at the boundaries are, in blocks, and how bright. */
    private static final float RING_WIDTH = 1.1F;
    private static final float RING_STRENGTH = 0.9F;

    /** How far behind the inner boundary the second, fainter band trails. Blocks. */
    private static final float RING_OFFSET = 4.5F;

    /** How far the colour goes, and how far the light drops with it. */
    private static final float DESATURATION = 0.80F;
    private static final float DARKEN = 0.88F;

    /**
     * How far off true the inverted hue is rolled, and how fast the roll breathes.
     *
     * <p>Small. A plain negative is a photographic operation and reads as one; a negative whose hues
     * are also sliding reads as the world being wrong. Past about a tenth of a turn it stops being
     * wrong and starts being a rainbow.
     */
    private static final float HUE_ROLL = 0.07F;
    private static final float HUE_RADIANS_PER_SECOND = 9.0F;

    /**
     * How hard the world is dragged toward the middle of the stop.
     *
     * <p>The fraction of the way to the centre the far end of the smear reaches. This is the loudest
     * single number in the effect and the one most worth arguing about.
     */
    private static final float PULL = 0.34F;

    /** How far the warp wanders off the radius, in screen widths, and at what spatial frequency. */
    private static final float SQUIGGLE = 0.008F;
    private static final float WAVE_SCALE = 0.55F;
    private static final float WAVE_RADIANS_PER_SECOND = 6.0F;

    /**
     * How coarsely the collapsing shell is cut into shards.
     *
     * <p>Lattice cells per unit of direction, so higher is smaller pieces. Around four and a half
     * gives plates big enough to read as slabs of a broken shell at forty-five blocks out; past
     * about ten they are small enough that the break-up reads as noise on the boundary instead.
     */
    private static final float SHARD_SCALE = 4.5F;

    /** A frame this long or longer is treated as this long, so a stall cannot skip the arrival. */
    private static final float MAX_FRAME_SECONDS = 0.1F;

    private static boolean stoppedNow;

    /** True from the moment a stop lands until it has finished fading out. */
    private static boolean live;

    private static float stoppedSeconds;
    private static float releasedSeconds;
    private static long lastFrameNanos;

    /** Where the stop is centred relative to the camera, refreshed every frame. */
    private static float centreX;
    private static float centreY;
    private static float centreZ;

    private TimeStopShader() {
    }

    /** Called whenever the client's picture of the stop changes. */
    public static void setPhase(boolean stopped) {
        stoppedNow = stopped;
    }

    public static void clear() {
        stoppedNow = false;
        live = false;
        stoppedSeconds = 0F;
        releasedSeconds = 0F;
        lastFrameNanos = 0L;
        TerrainCurve.clear();
    }

    /**
     * Advances the effect. Call once per frame.
     *
     * <p>Per frame rather than per tick because these are eases - sampled twenty times a second they
     * would arrive in visible steps, and the arrival is under half a second long to begin with.
     */
    public static void render(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }

        long now = System.nanoTime();
        float deltaSeconds = lastFrameNanos == 0L ? 0F : (now - lastFrameNanos) / 1.0E9F;
        lastFrameNanos = now;
        float step = Math.min(deltaSeconds, MAX_FRAME_SECONDS);

        if (stoppedNow) {
            if (!live) {
                live = true;
                stoppedSeconds = 0F;
            }
            stoppedSeconds += step;
            releasedSeconds = 0F;
        } else if (live) {
            releasedSeconds += step;
            if (releasedSeconds >= BREAK_OUT_SECONDS) {
                live = false;
                stoppedSeconds = 0F;
                releasedSeconds = 0F;
            }
        }

        // The turning keeps its own envelope, which has nothing to do with any stop. Ticked with
        // nothing asked of it so a forced drain still decays the way it always did.
        TerrainCurve.tick(0F, 0F, step);

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 centre = TimeStopView.centre();
        centreX = (float) (centre.x - camera.x);
        centreY = (float) (centre.y - camera.y);
        centreZ = (float) (centre.z - camera.z);
    }

    /** Smoothstep on the unit interval. */
    private static float smooth(float t) {
        float u = Mth.clamp(t, 0F, 1F);
        return u * u * (3F - 2F * u);
    }

    private static float contractSeconds() {
        return Mth.clamp(TimeStopView.totalTicks() / 20F * CONTRACT_SHARE,
                CONTRACT_MIN_SECONDS, CONTRACT_SECONDS);
    }

    // ---- what the pass reads --------------------------------------------------------------------

    /**
     * Whether there is anything at all to draw.
     *
     * <p>Flat one or zero. It was the fade envelope, back when the stop ended by thinning out; now
     * the stop is either running or its shell is still coming apart, and both of those are drawn at
     * full strength.
     */
    public static float strength() {
        return live ? 1F : 0F;
    }

    /** The outer boundary: swells out with the stop and then stays where it is. */
    public static float radius() {
        if (!live) {
            return 0F;
        }
        return RADIUS * smooth(stoppedSeconds / EXPAND_SECONDS);
    }

    /** How drained the held world is - held back a moment so the boundary arrives first. */
    public static float drain() {
        if (!live) {
            return 0F;
        }

        // Never wound back down. What ends the grey is its shell breaking, plate by plate - see
        // greyShatter - and a drain that also faded would have the pieces going pale as they left.
        return Mth.clamp(stoppedSeconds / DRAIN_FADE_IN_SECONDS, 0F, 1F);
    }

    /**
     * The inner boundary: out with the outer one, a beat at full width, then all the way back in.
     *
     * <p>Zero once it has finished, which is what takes the inversion off the screen and leaves the
     * grey standing on its own.
     */
    public static float innerRadius() {
        if (!live) {
            return 0F;
        }

        if (stoppedSeconds < EXPAND_SECONDS) {
            return RADIUS * smooth(stoppedSeconds / EXPAND_SECONDS);
        }

        float since = stoppedSeconds - EXPAND_SECONDS;
        if (since < HOLD_SECONDS) {
            return RADIUS;
        }

        float back = (since - HOLD_SECONDS) / contractSeconds();
        return back >= 1F ? 0F : RADIUS * smooth(1F - back);
    }

    public static float innerStrength() {
        return innerRadius() <= 0.001F ? 0F : 1F;
    }

    /**
     * How far through breaking up the inner shell is, 0 to 1.
     *
     * <p>Nothing at all until the shell starts back inward, then rising across the whole of the
     * contraction. So the arrival and the beat at full width are a clean sphere, and the shell only
     * loses its shape once it has started to go - a thing that shattered on the way out would be
     * something breaking, rather than something withdrawing.
     */
    public static float shatter() {
        if (!live) {
            return 0F;
        }

        float since = stoppedSeconds - EXPAND_SECONDS;
        if (since < HOLD_SECONDS) {
            return 0F;
        }

        return Mth.clamp((since - HOLD_SECONDS) / contractSeconds(), 0F, 1F);
    }

    /**
     * And the same, for the drained world once time has started again.
     *
     * <p>Zero for the whole of the stop, so the grey is a solid thing right up until the moment it
     * is not. This is what used to be a fade.
     */
    public static float greyShatter() {
        if (!live || stoppedNow) {
            return 0F;
        }
        return Mth.clamp(releasedSeconds / BREAK_OUT_SECONDS, 0F, 1F);
    }

    public static float shardScale() {
        return SHARD_SCALE;
    }

    public static float skyShare() {
        return SKY_SHARE;
    }

    public static float ringWidth() {
        return RING_WIDTH;
    }

    public static float ringStrength() {
        return RING_STRENGTH;
    }

    public static float ringOffset() {
        return RING_OFFSET;
    }

    public static float hueRoll() {
        return HUE_ROLL;
    }

    public static float huePhase() {
        return stoppedSeconds * HUE_RADIANS_PER_SECOND;
    }

    public static float desaturation() {
        return DESATURATION;
    }

    public static float darken() {
        return DARKEN;
    }

    public static float pull() {
        return PULL;
    }

    public static float squiggle() {
        return SQUIGGLE;
    }

    public static float waveScale() {
        return WAVE_SCALE;
    }

    /** How far through the wander the world is, in radians. */
    public static float wavePhase() {
        return stoppedSeconds * WAVE_RADIANS_PER_SECOND;
    }

    /**
     * The drain that applies to the whole screen with no sphere behind it.
     *
     * <p>The turning, and anything else that wants the world grey without wanting it stopped. It
     * arrives here rather than in its own pass because a second fullscreen program to do a
     * desaturate that this one already does would be paying twice for one line of arithmetic.
     */
    public static float globalDrain() {
        return TerrainCurve.drain();
    }

    public static float centreX() {
        return centreX;
    }

    public static float centreY() {
        return centreY;
    }

    public static float centreZ() {
        return centreZ;
    }
}
