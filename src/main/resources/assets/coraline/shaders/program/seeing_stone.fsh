#version 150
uniform sampler2D DiffuseSampler;
uniform sampler2D SpectralSampler;
uniform mat4 ScreenToStone;
uniform float EyesEnabled;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec4 original = texture(DiffuseSampler, texCoord);
    vec4 nearPoint = ScreenToStone*vec4(texCoord*2.0-1.0,-1.0,1.0);
    vec4 farPoint = ScreenToStone*vec4(texCoord*2.0-1.0,1.0,1.0);
    vec3 origin = nearPoint.xyz/nearPoint.w;
    vec3 ray = farPoint.xyz/farPoint.w-origin;
    // All four inner-lip sections of SeeingStoneRenderer. Checking both sides
    // accounts for thickness and tilt without painting over the mineral rim.
    float inside = 1.0;
    for(int section=0;section<4;section++) {
        float z = section==0 ? 0.075 : section==1 ? 0.035 : section==2 ? -0.035 : -0.075;
        float t = abs(ray.z)>0.000001 ? (z-origin.z)/ray.z : -1.0;
        vec2 p = origin.xy+t*ray.xy;
        float angle = atan(p.y,p.x);
        float boundary = 0.145*(1.0+0.055*sin(angle+0.4));
        if(section==0 || section==3) boundary+=0.025;
        float feather = max(fwidth(length(p)),0.00001);
        inside = min(inside,t>=0.0 ? 1.0-smoothstep(boundary-feather,boundary,length(p)) : 0.0);
    }
    float gray = dot(original.rgb,vec3(0.2126,0.7152,0.0722));
    vec3 inspected = vec3(gray);
    if(EyesEnabled>0.5) {
        vec4 spectral=texture(SpectralSampler,texCoord);
        // Eye attachment has been blended over transparent black; un-premultiply
        // before compositing it once over the grayscale world.
        inspected=mix(inspected,spectral.rgb/max(spectral.a,0.00001),spectral.a);
    }
    fragColor = vec4(mix(original.rgb,inspected,inside),original.a);
}
