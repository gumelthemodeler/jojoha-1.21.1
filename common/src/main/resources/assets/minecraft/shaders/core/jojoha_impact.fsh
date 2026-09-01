#version 150

// The impact frame: the world drops to black and white for a moment when a skull gives way.
//
// A post pass rather than a drawn overlay. An overlay can only ever add something on top of the
// picture - a wash, a tint, a fill - and every one of those reads as a sheet of colour laid over
// the game. This reaches the pixels themselves and takes the colour out of them, which is the
// thing comic panels and fighting games actually do on a hit, and it costs one fullscreen pass.

uniform sampler2D DiffuseSampler;

// x: how far into the frame we are, nought to one.
// y: how hard the remaining greys are pushed apart.
// z: the brightness the push happens around.
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

    // Rec. 601 weights. The flat average is the obvious thing and it is wrong: it makes a saturated
    // red and a saturated blue the same grey, and a hit landing on a red mob would lose the mob.
    float luma = dot(source.rgb, vec3(0.299, 0.587, 0.114));

    // Pushed apart a little, around the brightness a Minecraft frame actually sits at.
    //
    // The pivot is the whole of this. Contrast around 0.5 is the textbook spelling and it visibly
    // darkened everything, because a lit scene averages nowhere near 0.5 - it is closer to a third,
    // so almost every pixel was below the pivot and every one of them got pushed down. Pivoting at
    // the brightness the picture is really at means the push spreads the greys without moving the
    // overall level, which is the difference between a black and white frame and a dark one.
    float contrast = mix(1.0, Impact.y, strength);
    float pushed = clamp((luma - Impact.z) * contrast + Impact.z, 0.0, 1.0);

    // Neutral, and staying that way. A cold blue cast was tried here and it is a different effect:
    // tinting is a look the picture is put through, where black and white is the colour simply not
    // being there. The second is what an impact frame is.
    fragColor = vec4(mix(source.rgb, vec3(pushed), strength), source.a);
}
