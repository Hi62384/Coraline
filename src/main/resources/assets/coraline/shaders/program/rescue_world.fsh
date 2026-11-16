#version 150
uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform mat4 InverseViewProjection;
uniform vec2 OutSize;
uniform vec3 CameraHouse;
uniform vec3 RoomHouse;
uniform float Collapse;
uniform float Radius;
uniform float GreenRoom;
uniform float BlurRadius;
uniform float AcceptedSky;
uniform float TheatreDark;
uniform vec4 Drain0;
uniform vec4 Drain1;
uniform vec4 Drain2;
float drained(vec3 world,vec4 wave){return wave.w<=0.0?0.0:1.0-smoothstep(max(0.0,wave.w-2.0),wave.w,length(world-wave.xyz));}
in vec2 texCoord;
out vec4 fragColor;
void main(){
    vec3 color=texture(DiffuseSampler,texCoord).rgb;
    if(BlurRadius>0.001){
        vec2 d=vec2(BlurRadius)/OutSize;
        color*=0.20;
        color+=(texture(DiffuseSampler,texCoord+vec2(d.x,0)).rgb+texture(DiffuseSampler,texCoord-vec2(d.x,0)).rgb+texture(DiffuseSampler,texCoord+vec2(0,d.y)).rgb+texture(DiffuseSampler,texCoord-vec2(0,d.y)).rgb)*0.12;
        color+=(texture(DiffuseSampler,texCoord+d).rgb+texture(DiffuseSampler,texCoord-d).rgb+texture(DiffuseSampler,texCoord+vec2(d.x,-d.y)).rgb+texture(DiffuseSampler,texCoord+vec2(-d.x,d.y)).rgb)*0.08;
    }
    if(Collapse>0.0||GreenRoom>0.0||Drain0.w>0.0||Drain1.w>0.0||Drain2.w>0.0){
        float depth=texture(DepthSampler,texCoord).r;
        vec4 relative=InverseViewProjection*vec4(texCoord*2.0-1.0,depth*2.0-1.0,1.0);
        vec3 world=relative.xyz/max(abs(relative.w),0.000001)*sign(relative.w)+CameraHouse;
        // The house and its surrounding approach remain outside every drain.
        bool protectedHouse=abs(world.x)<=38.5&&world.z>=-48.5&&world.z<=46.5;
        if(depth<0.99999&&!protectedHouse){
            float drain=max(drained(world,Drain0),max(drained(world,Drain1),drained(world,Drain2)));
            color=mix(color,vec3(dot(color,vec3(0.2126,0.7152,0.0722))),drain);
        }
        bool refuge=abs(world.x)<=34.5&&world.z>=-44.5&&world.z<=42.5;
        float erased=refuge?0.0:smoothstep(Radius-1.5,Radius+0.5,length(world.xz));
        // Sky rays beyond the collapse radius must share the white boundary;
        // leaving them gray silhouettes the erased terrain against the sky.
        if(depth>=0.99999)erased=max(erased,AcceptedSky);
        color=mix(color,vec3(1.0),Collapse*erased);
        vec3 room=world-RoomHouse;
        float local=(1.0-smoothstep(9.0,12.0,length(room.xz)))*(1.0-smoothstep(6.0,9.0,abs(room.y)));
        color=mix(color,color*vec3(0.46,1.10,0.56)+vec3(0.015,0.045,0.005),clamp(local*GreenRoom,0.0,1.0));
    }
    float luminance=dot(color,vec3(0.2126,0.7152,0.0722));
    color=mix(color,vec3(luminance),AcceptedSky*0.65);
    color=mix(color,color*vec3(0.42,0.39,0.49),TheatreDark);
    fragColor=vec4(clamp(color,0.0,1.0),1.0);
}
