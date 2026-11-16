#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;
void main() {
    vec4 fabric = texture(Sampler0, texCoord0);
    if (fabric.a < 0.1) discard;
    float gray = dot(fabric.rgb, vec3(0.2126, 0.7152, 0.0722));
    // Cool, faded folds without the friendly shimmer or saturated blue.
    fragColor = vec4(vec3(gray * 0.20, gray * 0.29, gray * 0.16), fabric.a) * vertexColor * ColorModulator;
}
