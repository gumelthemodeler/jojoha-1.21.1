#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D PaletteSampler;

uniform vec4 Swap;

in vec2 texCoord;

out vec4 fragColor;

vec3 tone(int row, float luma) {
    float x = clamp(luma, 0.0, 1.0) * 7.0;
    int low = int(floor(x));
    int high = min(low + 1, 7);
    vec3 dark = texelFetch(PaletteSampler, ivec2(low, row), 0).rgb;
    vec3 bright = texelFetch(PaletteSampler, ivec2(high, row), 0).rgb;
    return mix(dark, bright, x - float(low));
}

void main() {
    vec4 source = texture(DiffuseSampler, texCoord);

    float strength = clamp(Swap.x, 0.0, 1.0);
    if (strength <= 0.0) {
        fragColor = source;
        return;
    }

    float high = max(source.r, max(source.g, source.b));
    float low = min(source.r, min(source.g, source.b));
    float chroma = high - low;
    float luma = dot(source.rgb, vec3(0.299, 0.587, 0.114));

    float hue = 0.0;
    if (chroma > 0.0001) {
        if (high == source.r) {
            hue = mod((source.g - source.b) / chroma, 6.0);
        } else if (high == source.g) {
            hue = (source.b - source.r) / chroma + 2.0;
        } else {
            hue = (source.r - source.g) / chroma + 4.0;
        }
    }

    int first = int(floor(hue)) % 6;
    int second = (first + 1) % 6;
    vec3 tinted = mix(tone(first, luma), tone(second, luma), fract(hue));

    float saturation = high > 0.0 ? chroma / high : 0.0;
    vec3 mapped = mix(tone(6, luma), tinted, smoothstep(0.08, 0.3, saturation));

    fragColor = vec4(mix(source.rgb, mapped, strength), source.a);
}
