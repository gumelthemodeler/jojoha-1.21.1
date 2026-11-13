#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform vec4 Erasure;
uniform vec4 Tone;
uniform vec4 Rewrite;
uniform mat4 InverseTransform;
uniform vec4 Realm;
uniform vec4 RealmCentre;
uniform vec4 RealmGrid;

in vec2 texCoord;

out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
const vec3 SEPIA_LIGHT = vec3(1.07, 0.86, 0.62);
const vec3 SEPIA_LIFT = vec3(0.05, 0.02, 0.0);
const vec3 GOLD_LIGHT = vec3(1.25, 1.02, 0.55);
const vec3 GOLD_LIFT = vec3(0.04, 0.03, 0.0);
const float REWIND_WOBBLE = 0.0035;
const float REWIND_BANDS = 90.0;
const vec3 REWRITE_SHADOW = vec3(0.22, 0.12, 0.42);
const vec3 REWRITE_LIGHT = vec3(1.18, 1.0, 0.62);
const float REWRITE_SPLIT = 0.008;
const float REWRITE_TEAR = 0.035;
const float REWRITE_ROWS = 36.0;
const float SHOCK_PUSH = 0.045;
const vec3 HEAVEN_LOW = vec3(0.46, 0.34, 0.2);
const vec3 HEAVEN_HIGH = vec3(1.12, 1.04, 0.86);
const vec3 GRID_GOLD = vec3(1.0, 0.78, 0.32);
const vec3 RIM_GLOW = vec3(1.0, 0.86, 0.55);
const vec3 SKY_HIGH = vec3(1.0, 0.97, 0.88);
const vec3 SKY_LOW = vec3(1.0, 0.8, 0.45);
const float GRID_EDGE = 0.44;
const float GRID_FADE_FROM = 20.0;
const float GRID_FADE_TO = 64.0;
const float GLITCH_SHARE = 0.965;

float hash(float n) {
    return fract(sin(n * 91.3458) * 47453.5453);
}

void main() {
    float rewind = clamp(Tone.x, 0.0, 1.0);
    vec2 uv = texCoord;
    if (rewind > 0.0) {
        float band = sin(uv.y * REWIND_BANDS + Tone.z * 18.0) * sin(uv.y * 7.0 - Tone.z * 3.0);
        uv.x = clamp(uv.x + band * REWIND_WOBBLE * rewind, 0.0, 1.0);
    }

    float rewrite = clamp(Rewrite.x, 0.0, 1.0);
    float shock = clamp(Rewrite.y, 0.0, 1.0);
    vec2 centred = (uv - 0.5) * vec2(Rewrite.z, 1.0);
    float reach = length(centred);
    float ring = 0.0;
    if (shock < 1.0) {
        float front = shock * 1.4;
        float gap = (reach - front) * 9.0;
        ring = exp(-gap * gap) * (1.0 - shock);
        uv -= normalize(centred + vec2(1e-5)) / vec2(Rewrite.z, 1.0) * ring * SHOCK_PUSH;
    }
    if (rewrite > 0.0) {
        float row = floor(uv.y * REWRITE_ROWS);
        float beat = floor(Tone.z * 14.0);
        float tear = step(0.82, hash(row + beat * 7.0)) * (hash(row * 3.1 + beat) - 0.5);
        uv.x += tear * REWRITE_TEAR * rewrite;
        uv += centred * sin(reach * 26.0 - Tone.z * 7.0) * 0.004 * rewrite;
        uv = clamp(uv, 0.0, 1.0);
    }

    vec4 source = texture(DiffuseSampler, uv);
    vec3 colour = source.rgb;
    if (rewrite > 0.0) {
        vec2 split = normalize(centred + vec2(1e-5)) * REWRITE_SPLIT * rewrite * (0.5 + reach);
        colour.r = texture(DiffuseSampler, clamp(uv + split, 0.0, 1.0)).r;
        colour.b = texture(DiffuseSampler, clamp(uv - split, 0.0, 1.0)).b;
    }

    float strength = clamp(Erasure.x, 0.0, 1.0);
    if (strength > 0.0) {
        float depth = texture(DepthSampler, uv).r;
        float luma = dot(colour, vec3(0.299, 0.587, 0.114));
        vec3 drained = mix(colour, vec3(luma), clamp(strength * 1.6, 0.0, 1.0));
        vec3 inverted = mix(drained, vec3(1.0) - drained, Erasure.y * smoothstep(0.6, 1.0, strength));
        float sky = step(Erasure.z, depth) * smoothstep(0.45, 1.0, strength);
        colour = mix(inverted, vec3(0.0), sky);
    }

    float luma = dot(colour, LUMA);
    if (rewind > 0.0) {
        vec3 sepia = luma * SEPIA_LIGHT + SEPIA_LIFT;
        vec2 edge = texCoord - 0.5;
        float vignette = 1.0 - dot(edge, edge) * 1.4;
        colour = mix(colour, sepia * vignette, rewind);
    }

    float gold = clamp(Tone.y, 0.0, 1.0);
    if (gold > 0.0) {
        colour = mix(colour, luma * GOLD_LIGHT + GOLD_LIFT, gold);
    }

    if (rewrite > 0.0) {
        float shade = dot(colour, LUMA);
        vec3 duotone = mix(REWRITE_SHADOW, REWRITE_LIGHT, smoothstep(0.05, 0.9, shade));
        colour = mix(colour, duotone, 0.55 * rewrite);
    }
    colour += vec3(1.0, 0.92, 0.7) * ring * 0.35;

    float realm = clamp(Realm.x, 0.0, 1.0);
    if (realm > 0.0) {
        float surface = texture(DepthSampler, uv).r;
        vec4 clip = vec4(uv * 2.0 - 1.0, surface * 2.0 - 1.0, 1.0);
        vec4 seen = InverseTransform * clip;
        vec3 rel = seen.xyz / seen.w;
        bool sky = surface >= Realm.w;
        float front = Realm.y;
        float away = length(rel - RealmCentre.xyz);
        float inside = sky ? 1.0 - smoothstep(front - 2.0, front, length(RealmCentre.xyz))
                : 1.0 - smoothstep(front - 1.5, front, away);
        if (inside > 0.0) {
            vec3 rewritten;
            if (sky) {
                rewritten = mix(SKY_LOW, SKY_HIGH, clamp(uv.y * 1.2, 0.0, 1.0));
            } else {
                float shade = dot(colour, LUMA);
                rewritten = mix(HEAVEN_LOW, HEAVEN_HIGH, smoothstep(0.0, 0.85, shade));
                vec3 world = rel + RealmGrid.xyz;
                vec3 cell = floor(world + 0.001);
                vec3 edge = smoothstep(vec3(GRID_EDGE), vec3(0.5), abs(fract(world) - 0.5));
                float line = clamp(edge.x * edge.y + edge.y * edge.z + edge.x * edge.z, 0.0, 1.0);
                float fade = 1.0 - smoothstep(GRID_FADE_FROM, GRID_FADE_TO, length(rel));
                float pulse = 0.55 + 0.45 * sin(Realm.z * 3.0 + dot(cell, vec3(0.7, 0.3, 0.5)));
                float glitch = step(GLITCH_SHARE, hash(dot(cell, vec3(12.9898, 78.233, 37.719)) + floor(Realm.z * 5.0)));
                rewritten = mix(rewritten, vec3(1.0) - rewritten * 0.55, glitch);
                rewritten += GRID_GOLD * line * pulse * fade;
            }
            colour = mix(colour, rewritten, inside * realm);
        }
        if (!sky) {
            float gap = (away - front) / 1.4;
            colour += RIM_GLOW * exp(-gap * gap) * realm;
        }
    }

    fragColor = vec4(colour, source.a);
}
