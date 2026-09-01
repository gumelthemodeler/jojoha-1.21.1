#version 150

// The time stop, drawn once over the finished frame.
//
// Everything the stop does to the world happens here, in one pass, instead of being spread across a
// dozen overridden core shaders. That was the old arrangement and it was the wrong shape: terrain,
// entities, sky and clouds each had to be taught the same trick separately, every one of them was a
// vanilla file we had replaced wholesale, and anything the mod did not think to override - particles,
// the block breaking overlay, another renderer entirely - simply kept its colour and gave the
// boundary away. A pass over the final image has no such list. If it is on the screen it is in the
// stop.
//
// The sphere is not geometry. Depth is turned back into a camera-relative position per pixel and the
// containment test is an analytic ray against a ball, which is also what removes the halo the drawn
// mesh always had: there is no surface being blended, so there is no edge to bleed.
//
// ---- what the stop looks like ------------------------------------------------------------------
//
// Two balls, doing two different jobs, and both of them arrive by growing.
//
// The OUTER one is where the world has stopped. It swells out of the caster, reaches the full radius
// and holds there for the length of the stop. Inside it the colour is drained and the light pulled
// down a little - that is what being held looks like, and the edge of it is a place you can walk to.
//
// The INNER one is the change itself passing through: the world turned into its own negative, with
// the hue rolled slightly off true so it reads as wrong rather than as a photographic invert. It
// swells with the outer one, holds a beat, then falls back inward to nothing - so what it leaves
// behind it is the drained world. A change that has finished has no business still being drawn.
//
// The two do not overlap. Grey is applied only where the inversion is not, so the collapse of the
// inner ball is also the arrival of the grey - one gesture, not two effects fading past each other.
//
// ---- two things that are deliberately not here ---------------------------------------------------
//
// There was a sweep: a hand that went once round the caster and took whatever it passed. It is gone.
// A bearing is undefined on the axis and unstable near it, so it needed a guard that blended the
// whole test away near the middle and a wrap at the seam where the angle rolls over - and both of
// those are visible. Growing a radius has no angle in it at all and therefore no seam.
//
// And nothing here is screen-space. An earlier pass graded to two inks and closed a vignette over
// it, and the vignette was centred on the viewport rather than on the stop, so however far the
// player walked the darkest part of the effect stayed in front of them and the whole thing appeared
// to travel with them. Every value below is anchored either to a world position or to a distance
// from the stop's own centre.

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform mat4 InverseTransform;
uniform mat4 ForwardTransform;

/** Where the stop is centred, relative to the camera. */
uniform vec3 SphereCentre;

/** x outer radius in blocks, y how drained it is, z how much of it the sky takes, w ring width. */
uniform vec4 Field;

/** x inner radius, y how present the inversion is, z hue roll, w hue phase. */
uniform vec4 Inner;

/** x how far the colour drains, y how far the light drops, z ring strength, w the drain that
    applies everywhere with no sphere behind it. */
uniform vec4 Grade;

/** x radial pull, y ring offset, z and w unused. */
uniform vec4 Lens;

/**
 * x how far the inversion has broken up, y how far the drained world has, z how big the plates are.
 *
 * Two shells, one fracture. They fail at different moments - the inversion as it withdraws, the grey
 * when time starts again - but they break along the same seams, because the plates are a property of
 * the direction from the centre rather than of whichever shell happens to be failing.
 */
uniform vec4 Break;

/** x squiggle amount, y spatial frequency, z radians, w unused. */
uniform vec4 Shake;

/**
 * The two bodies the stop does not hold, as boxes in camera-relative world space.
 *
 * An empty slot is written as a box whose minimum is above its maximum, which no point can be
 * inside - so there is no count to keep in step and no stale box left standing where somebody was.
 */
uniform vec3 FreeMinA;
uniform vec3 FreeMaxA;
uniform vec3 FreeMinB;
uniform vec3 FreeMaxB;

uniform float SkyDistance;

in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
const float SKY_DEPTH = 0.9999;

/** How far either side of a sphere surface the containment test fades. Blocks. */
const float EDGE_SOFTNESS = 0.35;

/**
 * How many taps the smear is built from, and how the far end is weighted.
 *
 * Fourteen is enough that the trail reads as continuous at the pull strengths used here; the tail is
 * lightened so the streak fades away from the surface rather than ending in a hard ghost.
 */
const int PULL_SAMPLES = 14;
const float PULL_TAIL_WEIGHT = 0.5;

/** How much narrower the second ring is than the first. */
const float SECOND_RING_SCALE = 0.6;

/** The inversion never goes the whole way. A true negative loses every readable surface. */
const float MAX_INVERT = 0.88;

/**
 * How far a shard is thrown off the true radius at full break-up, as a fraction of that radius.
 *
 * A third. Enough that the shell is plainly in pieces rather than merely rough; much more and the
 * near shards pass the camera while the far ones are still out at the boundary, which reads as two
 * separate things rather than as one shell coming apart.
 */
const float SHARD_SPREAD = 0.35;

/**
 * How much of the break-up happens before any piece actually moves.
 *
 * The seams have to be drawn on an intact shell first, or there is nothing to break - a surface that
 * comes apart the instant it is marked reads as noise appearing, where one that visibly cracks and
 * then fails reads as a thing failing.
 */
const float CRACK_LEAD = 0.35;

/** How wide a seam is drawn, in cell units. Small: these are cracks, not grout. */
const float CRACK_WIDTH = 0.055;

/** How far either side of the shell the cracks are visible, in blocks. */
const float SHELL_BAND = 3.0;

/** How brightly a seam burns relative to the boundary rings. */
const float CRACK_GLOW = 1.3;

/**
 * How much of the break-up is spent going out of step.
 *
 * Without this every shard would vanish on the same frame, which is a shell dissolving. Staggering
 * when each one goes is what makes it a shell breaking.
 */
const float SHARD_STAGGER = 0.7;

vec3 reconstruct(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 position = InverseTransform * clip;
    return position.xyz / position.w;
}

vec2 project(vec3 position) {
    vec4 clip = ForwardTransform * vec4(position, 1.0);
    if (clip.w <= 0.0) {
        return vec2(-1000.0);
    }
    return (clip.xy / clip.w) * 0.5 + 0.5;
}

/**
 * How much of this pixel the ball covers.
 *
 * Three cases, and the third is the one a drawn sphere can never get right. Looking at it from
 * outside, the answer is a silhouette against whatever is behind it. Standing inside it, every ray
 * leaving the eye is already in, so the answer is what is nearer than the far wall. And the sky has
 * no position at all, so it is asked only whether the ball lies along the line of sight.
 */
float coverage(vec3 direction, float surfaceDistance, bool sky, float radius) {
    if (radius <= 0.001) {
        return 0.0;
    }

    float along = dot(direction, SphereCentre);
    float centreSqr = dot(SphereCentre, SphereCentre);
    float outside = centreSqr - radius * radius;
    float discriminant = along * along - outside;
    if (discriminant < 0.0) {
        return 0.0;
    }

    float root = sqrt(discriminant);
    float far = along + root;
    if (far <= 0.0) {
        return 0.0;
    }

    if (outside > 0.0) {
        float near = along - root;
        float perpendicular = sqrt(max(0.0, centreSqr - along * along));
        float silhouette = 1.0 - smoothstep(radius - EDGE_SOFTNESS, radius + EDGE_SOFTNESS, perpendicular);
        float behind = sky ? 1.0 : smoothstep(near - EDGE_SOFTNESS, near + EDGE_SOFTNESS, surfaceDistance);
        return silhouette * behind;
    }

    if (sky) {
        return 1.0;
    }
    return 1.0 - smoothstep(far - EDGE_SOFTNESS, far + EDGE_SOFTNESS, surfaceDistance);
}

/** Whether a point falls inside a box. Inclusive; an empty box is inverted and always fails. */
bool within(vec3 point, vec3 low, vec3 high) {
    return all(greaterThanEqual(point, low)) && all(lessThanEqual(point, high));
}

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

/** Where the feature point inside a given cell sits. */
vec3 hash33(vec3 p) {
    vec3 q = vec3(dot(p, vec3(127.1, 311.7, 74.7)),
                  dot(p, vec3(269.5, 183.3, 246.1)),
                  dot(p, vec3(113.5, 271.9, 124.6)));
    return fract(sin(q) * 43758.5453123);
}

vec3 rgbToHsv(vec3 colour) {
    vec4 k = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(colour.bg, k.wz), vec4(colour.gb, k.xy), step(colour.b, colour.g));
    vec4 q = mix(vec4(p.xyw, colour.r), vec4(colour.r, p.yzx), step(p.x, colour.r));
    float d = q.x - min(q.w, q.y);
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + 1.0e-10)), d / (q.x + 1.0e-10), q.x);
}

vec3 hsvToRgb(vec3 hsv) {
    vec4 k = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(hsv.xxx + k.xyz) * 6.0 - k.www);
    return hsv.z * mix(k.xxx, clamp(p - k.xxx, 0.0, 1.0), hsv.y);
}

vec3 rollHue(vec3 colour, float shift) {
    vec3 hsv = rgbToHsv(colour);
    hsv.x = fract(hsv.x + shift);
    return hsvToRgb(hsv);
}

/**
 * A slow wander built from sines that do not divide into one another.
 *
 * Sampled at the world position rather than at the pixel, so the warp sits in the world and the
 * ground writhes under a still camera instead of the picture sliding about when you turn your head.
 */
vec2 squiggle(vec3 position, float scale, float time) {
    float a = sin(position.x * scale + time * 1.7)
            + sin(position.z * scale * 1.3 - time * 1.1)
            + 0.5 * sin(position.y * scale * 2.7 + time * 2.3);
    float b = sin(position.y * scale * 1.1 + time * 1.3)
            + sin((position.x + position.z) * scale * 0.7 + time * 0.9)
            + 0.5 * sin(position.x * scale * 3.1 - time * 2.1);
    return vec2(a, b) * 0.4;
}

/** A band of white where the distance passes through a radius. */
float ringAt(float distance, float radius, float width, float strength) {
    if (strength <= 0.0 || width <= 0.0 || radius <= 0.0) {
        return 0.0;
    }
    float falloff = 1.0 - smoothstep(0.0, width, abs(distance - radius));
    return falloff * falloff * strength;
}

void main() {
    float rawDepth = texture(DepthSampler, texCoord).r;
    bool sky = rawDepth >= SKY_DEPTH;
    vec3 position = reconstruct(texCoord, rawDepth);

    float surfaceDistance = length(position);
    vec3 direction = surfaceDistance > 0.0001 ? position / surfaceDistance : vec3(0.0, 0.0, 1.0);

    // Whether this pixel belongs to something the stop does not hold. Asked of the world position
    // it reconstructs to, not of a silhouette drawn into a buffer - see TimeStopPost for the three
    // separate ways the buffer went wrong.
    //
    // The sky is never exempt. It has no position to test, and nothing unfrozen is ever standing at
    // an infinite distance.
    float free = 1.0;
    if (!sky && (within(position, FreeMinA, FreeMaxA) || within(position, FreeMinB, FreeMaxB))) {
        free = 0.0;
    }

    // Distance from the stop's own centre. This, and not anything measured on the screen, is what
    // every band below is placed against.
    vec3 offset = sky ? direction * SkyDistance : position - SphereCentre;
    float fromCentre = length(offset);

    // Neither shell simply stops. Each one cracks, then comes apart into plates that go their own
    // separate ways - the inversion on its way in, and the drained world when time restarts.
    //
    // For the grey this is the whole gesture rather than an edge effect: the plate a pixel belongs to
    // is decided by its direction from the centre, so when a plate goes it takes the entire cone
    // behind it, and the colour comes back through the gap all the way to the boundary. Seen from
    // inside - which is where the caster always is - the drained world breaks up into pieces and
    // falls away in front of you.
    float shardRadius = Inner.x;
    float shardFade = 1.0;
    float greyRadius = Field.x;
    float greyFade = 1.0;
    float crack = 0.0;

    if (Break.x > 0.001 || Break.y > 0.001) {
        // Cells laid out on the direction from the stop's own centre rather than on a bearing round
        // it. A direction has no wrap and no pole, so the only discontinuities here are the seams
        // themselves - which is the entire point, and the reason the sweep that used to live here
        // could not be saved.
        vec3 facing = fromCentre > 0.001 ? offset / fromCentre : vec3(0.0, 1.0, 0.0);

        // Irregular plates, not a grid. Scattering one feature point per lattice cell and taking
        // whichever is nearest gives cells with straight but arbitrary edges and no shared alignment
        // - which is what a sheet of something brittle does when it fails. Cutting the direction up
        // with floor() alone, as this did at first, only ever produces boxes, and boxes on a sphere
        // read as a tiled pattern rather than as damage.
        vec3 p = facing * Break.z;
        vec3 base = floor(p);
        vec3 within = p - base;

        float nearest = 8.0;
        float second = 8.0;
        vec3 plate = base;

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    vec3 neighbour = vec3(float(i), float(j), float(k));
                    vec3 seed = base + neighbour;
                    vec3 toward = neighbour + hash33(seed) - within;
                    float away = dot(toward, toward);

                    if (away < nearest) {
                        second = nearest;
                        nearest = away;
                        plate = seed;
                    } else if (away < second) {
                        second = away;
                    }
                }
            }
        }

        // A point is on a seam exactly when the two closest feature points are equally far off. The
        // distances themselves are never needed - only how nearly they agree.
        float seam = sqrt(second) - sqrt(nearest);
        float edge = 1.0 - smoothstep(0.0, CRACK_WIDTH, seam);

        // Two independent draws per plate: how far it is thrown off true, and when it goes. Both
        // shells read the same two, so a plate that leaves early does so in either failure and the
        // fracture looks like one structure giving way twice rather than two unrelated patterns.
        float lift = hash13(plate) - 0.5;
        float lead = hash13(plate + 19.0);

        if (Break.x > 0.001) {
            float cracking = smoothstep(0.0, CRACK_LEAD, Break.x);
            float breaking = smoothstep(CRACK_LEAD, 1.0, Break.x);

            shardRadius *= 1.0 + lift * SHARD_SPREAD * breaking;
            shardFade = 1.0 - clamp(breaking * (1.0 + SHARD_STAGGER) - lead * SHARD_STAGGER, 0.0, 1.0);

            // Seams are drawn only near the shell they belong to, so they read as damage to a
            // surface rather than as a lattice filling the whole volume.
            crack = max(crack, edge * cracking * shardFade
                    * (1.0 - smoothstep(0.0, SHELL_BAND, abs(fromCentre - shardRadius))));
        }

        if (Break.y > 0.001) {
            float cracking = smoothstep(0.0, CRACK_LEAD, Break.y);
            float breaking = smoothstep(CRACK_LEAD, 1.0, Break.y);

            greyRadius *= 1.0 + lift * SHARD_SPREAD * breaking;
            greyFade = 1.0 - clamp(breaking * (1.0 + SHARD_STAGGER) - lead * SHARD_STAGGER, 0.0, 1.0);

            crack = max(crack, edge * cracking * greyFade
                    * (1.0 - smoothstep(0.0, SHELL_BAND, abs(fromCentre - greyRadius))));
        }

        crack *= free;
    }

    float insideGrey = coverage(direction, surfaceDistance, sky, greyRadius)
            * Field.y * greyFade * free;
    float insideInvert = coverage(direction, surfaceDistance, sky, shardRadius)
            * Inner.y * shardFade * free;

    // The sky is not in the stop - it is at no distance at all - so it takes a share rather than
    // the whole of it.
    if (sky) {
        insideGrey *= Field.z;
        insideInvert *= Field.z;
    }

    // ---- sampling ------------------------------------------------------------------------------
    // The world under strain, smeared toward the middle of the thing straining it.

    vec3 colour;

    if (insideInvert > 0.001 && !sky && Lens.x > 0.0) {
        vec2 wander = squiggle(position, Shake.y, Shake.z);
        vec2 wanderUv = wander * Shake.x * insideInvert;

        // How far along the line toward the centre the trail reaches. Modulated by the wander so the
        // smear is uneven around the sphere rather than a clean radial zoom.
        float pull = Lens.x * (0.6 + 0.5 * wander.x) * insideInvert;

        vec4 gathered = vec4(0.0);
        float total = 0.0;

        for (int i = 0; i < PULL_SAMPLES; i++) {
            float tap = float(i) / float(PULL_SAMPLES - 1);
            float weight = mix(1.0, PULL_TAIL_WEIGHT, tap);

            // Walked in world space and reprojected, not slid across the screen. A screen-space
            // streak points at wherever the centre happens to be on the monitor; this one points at
            // the centre itself, so it stays correct as the camera turns.
            vec3 walked = position + (SphereCentre - position) * pull * tap;
            vec2 projected = project(walked);
            vec2 uv = projected.x > -999.0 ? projected : texCoord;

            gathered += texture(DiffuseSampler, clamp(uv + wanderUv, 0.0, 1.0)) * weight;
            total += weight;
        }

        colour = (gathered / total).rgb;
    } else {
        colour = texture(DiffuseSampler, texCoord).rgb;
    }

    // ---- the grade -----------------------------------------------------------------------------

    // Drained, but only where the inversion is not. The two are the same event at different times -
    // the change passing through, and what it leaves behind - so they must not stack.
    float drained = clamp(insideGrey * (1.0 - insideInvert), 0.0, 1.0);
    if (drained > 0.001) {
        vec3 washed = mix(colour, vec3(dot(colour, LUMA)), Grade.x) * Grade.y;
        colour = mix(colour, washed, drained);
    }

    // Turned over, and rolled off true. The roll breathes rather than holding, which is the one
    // thing in a stopped world allowed to move: it is the stop itself, not anything caught in it.
    float inverted = clamp(insideInvert, 0.0, 1.0);
    if (inverted > 0.001) {
        float roll = Inner.z * 0.5 * (1.0 - cos(Inner.w));
        colour = mix(colour, rollHue(1.0 - colour, roll), inverted * MAX_INVERT);
    }

    // ---- the edges -----------------------------------------------------------------------------
    // Where each boundary cuts the world. The inner pair travel with the inversion as it swells and
    // falls back, which is what makes the front read as a front rather than as a region changing
    // brightness.

    // The inner pair follow the shards, so the fracture is drawn as broken edges of light rather
    // than having to be inferred from where the inversion stops.
    float rings = ringAt(fromCentre, shardRadius, Field.w, Grade.z * Inner.y * shardFade)
            + ringAt(fromCentre, shardRadius - Lens.y, Field.w * SECOND_RING_SCALE,
                    Grade.z * Inner.y * shardFade * 0.55)
            + ringAt(fromCentre, greyRadius, Field.w, Grade.z * Field.y * greyFade * 0.4);
    rings *= free;
    rings = max(rings, crack * CRACK_GLOW * Grade.z);
    colour = mix(colour, vec3(1.0), clamp(rings, 0.0, 1.0));

    // The turning: a drain over the whole screen with no sphere behind it, which is a different
    // event that happens to want the same arithmetic.
    if (Grade.w > 0.001) {
        colour = mix(colour, vec3(dot(colour, LUMA)), Grade.w);
    }

    fragColor = vec4(colour, 1.0);
}
