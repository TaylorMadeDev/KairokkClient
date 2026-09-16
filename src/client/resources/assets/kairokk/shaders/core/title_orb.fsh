#version 330

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    // Signed-distance circle: this is evaluated per pixel, not approximated with rectangles.
    float distanceFromCenter = length(texCoord0 * 2.0 - 1.0);
    float falloff = pow(max(0.0, 1.0 - distanceFromCenter), 2.05);
    float alpha = vertexColor.a * falloff;

    if (alpha < 0.003) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, alpha);
}
