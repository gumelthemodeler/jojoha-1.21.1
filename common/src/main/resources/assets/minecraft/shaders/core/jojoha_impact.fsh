#version 150

uniform sampler2D DiffuseSampler;

uniform vec4 Impact;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 source = texture(DiffuseSampler, texCoord);

    float strength = clamp(Impact.x, 0.0, 1.0);
    if (strength <= 0.0) {
        fragColor = source;
        return;
    }

    float luma = dot(source.rgb, vec3(0.299, 0.587, 0.114));

    if (Impact.w > 2.5) {
        vec3 sepia = vec3(dot(source.rgb, vec3(0.393, 0.769, 0.189)),
                          dot(source.rgb, vec3(0.349, 0.686, 0.168)),
                          dot(source.rgb, vec3(0.272, 0.534, 0.131)));
        fragColor = vec4(mix(source.rgb, clamp(sepia, 0.0, 1.0), strength), source.a);
        return;
    }

    if (Impact.w > 0.5) {
        float ink = smoothstep(Impact.z - 0.03, Impact.z + 0.03, luma);
        if (Impact.w > 1.5) {
            ink = 1.0 - ink;
        }
        fragColor = vec4(mix(source.rgb, vec3(ink), strength), source.a);
        return;
    }

    float contrast = mix(1.0, Impact.y, strength);
    float pushed = clamp((luma - Impact.z) * contrast + Impact.z, 0.0, 1.0);

    fragColor = vec4(mix(source.rgb, vec3(pushed), strength), source.a);
}
