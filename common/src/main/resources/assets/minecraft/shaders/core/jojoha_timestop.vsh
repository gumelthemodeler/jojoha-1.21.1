#version 150

// A fullscreen triangle-pair. The quad is handed in already in clip space, so there is nothing to
// transform - the whole point of a post pass is that the geometry is the screen.

in vec3 Position;

out vec2 texCoord;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    texCoord = Position.xy * 0.5 + 0.5;
}
