#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform sampler2D FreeMaskSampler;
uniform sampler2D FreeDepthSampler;

uniform float FreeMask;

uniform mat4 InverseTransform;
uniform mat4 ForwardTransform;

uniform vec3 SphereCentre;

uniform vec4 Field;

uniform vec4 Inner;

uniform vec4 Grade;

uniform vec4 Lens;

uniform vec4 Shake;

uniform float SkyDistance;

in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
const float SKY_DEPTH = 0.9999;

const float FREE_SURFACE_TOLERANCE = 0.06;

const float EDGE_SOFTNESS = 0.35;

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

vec2 squiggle(vec3 position, float scale, float time) {
    float a = sin(position.x * scale + time * 1.7)
            + sin(position.z * scale * 1.3 - time * 1.1)
            + 0.5 * sin(position.y * scale * 2.7 + time * 2.3);
    float b = sin(position.y * scale * 1.1 + time * 1.3)
            + sin((position.x + position.z) * scale * 0.7 + time * 0.9)
            + 0.5 * sin(position.x * scale * 3.1 - time * 2.1);
    return vec2(a, b) * 0.4;
}

const float MAX_INVERT = 0.95;

const float SHORE_DEPTH = 0.1;

const float FROTH_BLOCKS = 0.5;

float shoreline(vec3 outward, float time) {
    float angle = atan(outward.z, outward.x);
    float lift = outward.y;
    float swell = sin(angle * 3.0 + time * 0.09 + 1.3) * 0.5
            + sin(angle * 5.0 - time * 0.13 + 4.1) * 0.3
            + sin(angle * 2.0 + lift * 3.0 + time * 0.06) * 0.45
            + sin(angle * 7.0 + lift * 2.0 - time * 0.2 + 2.2) * 0.15
            + sin(lift * 4.0 - time * 0.1) * 0.25;
    return clamp(swell / 1.65 * 0.5 + 0.5, 0.0, 1.0);
}

float froth(vec3 offset, float time) {
    float a = sin(offset.x * 0.9 + time * 0.3) * sin(offset.z * 0.8 - time * 0.25);
    float b = 0.5 * sin((offset.x - offset.z) * 1.7 + time * 0.45);
    float c = 0.4 * sin(offset.y * 1.3 + (offset.x + offset.z) * 0.6 + time * 0.28);
    return (a + b + c) / 1.9;
}

float shaped(float radius, float amount, float shore, float foam) {
    if (radius <= 0.001) {
        return 0.0;
    }
    float bent = radius * (1.0 - SHORE_DEPTH * amount * shore) + FROTH_BLOCKS * amount * foam;
    return max(bent, 0.0);
}

const float BUBBLE_FRESNEL = 2.5;

const float BUBBLE_BEND = 0.9;

const float BUBBLE_GLOW = 0.22;

const float BAND_INVERT = 1.0;

const float WAKE_HOLD = 0.4;

const float INVERT_LEAD = 1.0;

const float GREY_FEATHER = 0.45;

const float GROUND_RING = 0.12;

const float WIND_LEAD = 1.5;

const float WIND_RIPPLE = 1.6;

const float WIND_SPEED = 2.2;

const float WIND_SWIRL = 0.55;

const float WIND_LIFT = 0.35;

const float WIND_SHIMMER = 2.5;

float shellHit(vec3 direction, float surfaceDistance, bool sky, float radius, out vec3 normal) {
    normal = vec3(0.0, 1.0, 0.0);
    if (radius <= 0.001) {
        return -1.0;
    }
    float along = dot(direction, SphereCentre);
    float centreSqr = dot(SphereCentre, SphereCentre);
    float discriminant = along * along - (centreSqr - radius * radius);
    if (discriminant < 0.0) {
        return -1.0;
    }
    float root = sqrt(discriminant);
    float t = centreSqr > radius * radius ? along - root : along + root;
    if (t <= 0.0 || (!sky && t > surfaceDistance)) {
        return -1.0;
    }
    normal = normalize(direction * t - SphereCentre);
    return t;
}

float bubble(vec3 direction, float surfaceDistance, bool sky, float radius, float amount, out vec2 bend) {
    bend = vec2(0.0);
    if (amount <= 0.001) {
        return 0.0;
    }
    vec3 normal;
    float t = shellHit(direction, surfaceDistance, sky, radius, normal);
    if (t <= 0.0) {
        return 0.0;
    }
    float bent = shaped(radius, amount, shoreline(normal, Shake.z), froth(normal * radius, Shake.z));
    t = shellHit(direction, surfaceDistance, sky, bent, normal);
    if (t <= 0.0) {
        return 0.0;
    }
    float rim = pow(1.0 - abs(dot(direction, normal)), BUBBLE_FRESNEL);
    vec3 hit = direction * t;
    vec2 a = project(hit);
    vec2 b = project(hit + normal * BUBBLE_BEND);
    if (a.x > -999.0 && b.x > -999.0) {
        bend = (b - a) * rim * amount;
    }
    return clamp(rim * BUBBLE_GLOW, 0.0, 1.0) * amount;
}

float band(float distance, float centre, float width) {
    if (width <= 0.001) {
        return 0.0;
    }
    float t = (distance - centre) / width;
    return exp(-t * t);
}

void main() {
    float rawDepth = texture(DepthSampler, texCoord).r;
    bool sky = rawDepth >= SKY_DEPTH;
    vec3 position = reconstruct(texCoord, rawDepth);

    float surfaceDistance = length(position);
    vec3 direction = surfaceDistance > 0.0001 ? position / surfaceDistance : vec3(0.0, 0.0, 1.0);

    float free = 1.0;
    if (FreeMask > 0.5 && !sky && texture(FreeMaskSampler, texCoord).a > 0.1) {
        vec3 body = reconstruct(texCoord, texture(FreeDepthSampler, texCoord).r);
        if (distance(body, position) < FREE_SURFACE_TOLERANCE) {
            free = 0.0;
        }
    }

    vec3 offset = sky ? direction * SkyDistance : position - SphereCentre;
    float fromCentre = length(offset);
    vec3 outward = fromCentre > 0.001 ? offset / fromCentre : vec3(0.0, 1.0, 0.0);

    float shore = shoreline(outward, Shake.z);
    float foam = froth(offset, Shake.z);
    float front = shaped(Inner.x, Inner.y, shore, foam);
    float greyRadius = shaped(Field.x, max(Inner.y, Lens.w), shore, foam);

    float insideFront = coverage(direction, surfaceDistance, sky, front);
    float behind = sky ? front - length(SphereCentre) : front - fromCentre;
    float wake = 1.0 - smoothstep(0.0, Lens.y, max(behind, 0.0));
    float leading = sky ? 1.0 : smoothstep(-0.5, INVERT_LEAD, behind);
    float insideInvert = insideFront * pow(wake, WAKE_HOLD) * leading * BAND_INVERT * Inner.y * free;

    float feather = greyRadius * GREY_FEATHER * max(Inner.y, Lens.w) + EDGE_SOFTNESS;
    float seep = sky ? 1.0 : 1.0 - smoothstep(greyRadius - feather, greyRadius, fromCentre);
    float insideGrey = coverage(direction, surfaceDistance, sky, greyRadius) * seep * Field.y * free;

    if (sky) {
        insideGrey *= Field.z;
        insideInvert *= Field.z;
    }

    vec3 colour;
    float frontGust = band(fromCentre, front, Lens.z) * Inner.y;
    float greyGust = band(fromCentre, greyRadius, Lens.z) * Lens.w;
    float gust = sky ? 0.0 : clamp(band(fromCentre, front - WIND_LEAD, Lens.z) * Inner.y + greyGust, 0.0, 1.0) * free;
    float warp = gust;

    vec2 frontBend;
    vec2 greyBend;
    float shell = bubble(direction, surfaceDistance, sky, Inner.x, Inner.y, frontBend)
            + bubble(direction, surfaceDistance, sky, Field.x, Lens.w, greyBend) * 0.6;
    vec2 bend = (frontBend + greyBend) * Lens.x;

    if (warp > 0.001 && Lens.x > 0.0) {
        float rel = fromCentre - front;
        float ripple = sin(rel * WIND_RIPPLE - Shake.z * WIND_SPEED + shore * 6.2831);
        vec3 side = cross(outward, vec3(0.0, 1.0, 0.0));
        side = dot(side, side) > 1.0e-4 ? normalize(side) : vec3(1.0, 0.0, 0.0);
        vec2 swirl = squiggle(offset, Shake.y * 1.6, Shake.z * 1.5);
        vec3 walked = position
                + outward * Lens.x * warp * (0.65 + 0.35 * ripple)
                + side * Lens.x * warp * WIND_SWIRL * swirl.x
                + vec3(0.0, Lens.x * warp * WIND_LIFT * swirl.y, 0.0);
        vec2 projected = project(walked);
        vec2 uv = projected.x > -999.0 ? projected : texCoord;
        vec2 shimmer = squiggle(position, Shake.y * 3.0, Shake.z * 2.0) * Shake.x * WIND_SHIMMER;
        colour = texture(DiffuseSampler, clamp(uv + shimmer * warp + bend, 0.0, 1.0)).rgb;
    } else {
        colour = texture(DiffuseSampler, clamp(texCoord + bend, 0.0, 1.0)).rgb;
    }

    float drained = clamp(insideGrey * (1.0 - insideInvert), 0.0, 1.0);
    if (drained > 0.001) {
        vec3 washed = mix(colour, vec3(dot(colour, LUMA)), Grade.x) * Grade.y;
        colour = mix(colour, washed, drained);
    }

    float inverted = clamp(insideInvert, 0.0, 1.0);
    if (inverted > 0.001) {
        float roll = Inner.z * 0.5 * (1.0 - cos(Inner.w));
        colour = mix(colour, rollHue(1.0 - colour, roll), inverted * MAX_INVERT);
    }

    float rings = (frontGust + greyGust * 0.5) * Grade.z * GROUND_RING * free;
    colour = mix(colour, vec3(1.0), clamp(rings + shell, 0.0, 1.0));

    if (Grade.w > 0.001) {
        colour = mix(colour, vec3(dot(colour, LUMA)), Grade.w);
    }

    fragColor = vec4(colour, 1.0);
}
