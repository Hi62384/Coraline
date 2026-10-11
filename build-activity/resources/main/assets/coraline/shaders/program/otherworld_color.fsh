#version 150
uniform sampler2D DiffuseSampler;
uniform float Saturation;
uniform float Whiteout;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec4 original = texture(DiffuseSampler, texCoord);
    float luma = dot(original.rgb, vec3(0.2126, 0.7152, 0.0722));
    // Preserve dark detail and luminous whites rather than clipping every bright color.
    float chroma = max(max(original.r, original.g), original.b)-min(min(original.r, original.g), original.b);
    float boost = 1.0+(Saturation-1.0)*(1.0-chroma*0.45);
    // This is the already-composited world framebuffer, not a translucent sprite.
    // Sky/mote drawing leaves fractional alpha behind; carrying it into blit creates black halos.
    vec3 graded = clamp(mix(vec3(luma), original.rgb, boost),0.0,1.0);
    fragColor = vec4(mix(graded,vec3(1.0),clamp(Whiteout,0.0,1.0)),1.0);
}
