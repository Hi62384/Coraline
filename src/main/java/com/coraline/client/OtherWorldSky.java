package com.coraline.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Dimension-local starlight, flowing aurora curtains, and a four-hole button moon. */
public final class OtherWorldSky extends DimensionSpecialEffects {
    private record Star(Vec3[] corners,float strength,float phase){}
    private static final Star[] STARS=createStars();
    private static final Vec3[][] DOME=createDome();
    private static Vec3[][] createDome(){
        var points=new Vec3[25][65];
        for(int lat=0;lat<=24;lat++)for(int side=0;side<=64;side++){
            double a=side*Math.PI/32,e=-Math.PI/2+lat*Math.PI/24;
            points[lat][side]=new Vec3(Math.cos(a)*Math.cos(e)*100,Math.sin(e)*100,Math.sin(a)*Math.cos(e)*100);
        }return points;
    }
    private static Star[] createStars(){
        var random=new java.util.Random(42173);var stars=new Star[1900];
        for(int i=0;i<stars.length;i++){
            double a=random.nextDouble()*Math.PI*2,y=random.nextDouble()*1.12-.12;
            var normal=new Vec3(Math.cos(a)*Math.sqrt(1-y*y),y,Math.sin(a)*Math.sqrt(1-y*y));var center=normal.scale(95);
            var right=normal.cross(new Vec3(0,1,0)).normalize().scale(.028+Math.pow(random.nextDouble(),3)*.19);
            var up=normal.cross(right).normalize().scale(right.length());
            stars[i]=new Star(new Vec3[]{center.subtract(right).subtract(up),center.add(right).subtract(up),center.add(right).add(up),center.subtract(right).add(up)},.25F+random.nextFloat()*.7F,random.nextFloat()*6.28F);
        }return stars;
    }
    @Override public void adjustLightmapColors(ClientLevel level,float partial,float dark,float flicker,float sky,int block,int skyIndex,org.joml.Vector3f colors){
        if(MineHud.active()){
            // This dimension normally has a bright ambient floor. The mine
            // instead uses local block/lantern light, never global starlight.
            float light=block/15F;light=.008F+.992F*light*light;
            colors.set(light*.92F,light*.96F,light);return;
        }
        if(com.coraline.config.OtherWorldVisualConfig.ENABLED.get())OtherWorldLighting.apply(colors,block,skyIndex);
    }
    public OtherWorldSky(){super(Float.NaN,true,SkyType.NORMAL,false,false);}
    @Override public Vec3 getBrightnessDependentFogColor(Vec3 color,float sun){return new Vec3(.10,.16,.29);}
    @Override public boolean isFoggyAt(int x,int z){return false;}
    @Override public boolean renderClouds(ClientLevel l,int ticks,float partial,PoseStack p,double x,double y,double z,Matrix4f projection){return true;}
    @Override public boolean renderSky(ClientLevel level,int ticks,float partial,PoseStack pose,Camera camera,Matrix4f projection,boolean foggy,Runnable fog){
        if(!com.coraline.config.OtherWorldVisualConfig.ENABLED.get())return false;
        if(foggy)return true;
        fog.run();render(pose,ticks+partial);return true;
    }
    public static void render(PoseStack pose,float time){
        RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        var m=pose.last().pose();
        for(int lat=0;lat<24;lat++)for(int side=0;side<64;side++){
            sky(b,m,side,lat);sky(b,m,side+1,lat);sky(b,m,side+1,lat+1);sky(b,m,side,lat+1);
        }
        float twinkle=com.coraline.config.OtherWorldVisualConfig.TWINKLE.get().floatValue();
        for(var star:STARS){
            float glow=star.strength*com.coraline.config.OtherWorldVisualConfig.STAR_INTENSITY.get().floatValue()*(1-twinkle+twinkle*(float)Math.sin(time*.009+star.phase));
            for(var corner:star.corners)point(b,m,corner,.85F+.07F*(float)Math.sin(star.phase),.91F,1,glow);
        }
        for(int ribbon=0;ribbon<3;ribbon++)for(int i=0;i<160;i++)for(int band=0;band<4;band++){
            aurora(b,m,i,ribbon,band/4F,time);aurora(b,m,i+1,ribbon,band/4F,time);
            aurora(b,m,i+1,ribbon,(band+1)/4F,time);aurora(b,m,i,ribbon,(band+1)/4F,time);
        }
        if(com.coraline.config.OtherWorldVisualConfig.METEORS.get())meteor(b,m,time);
        pose.pushPose();pose.mulPose(Axis.XP.rotationDegrees(48));pose.mulPose(Axis.ZP.rotationDegrees(-25));
        float scale=com.coraline.config.OtherWorldVisualConfig.MOON_SIZE.get().floatValue();pose.scale(scale,1,scale);m=pose.last().pose();
        for(int ring=8;ring>=1;ring--)disk(b,m,0,0,11+ring*1.2F,.35F,.42F,.85F,.025F);
        disk(b,m,0,0,11,.86F,.91F,1,1);
        disk(b,m,0,0,10.65F,.45F,.60F,.79F,1);
        disk(b,m,0,0,10.15F,.88F,.91F,.96F,1);
        disk(b,m,0,0,9.25F,.70F,.79F,.90F,1);
        for(int i=0;i<64;i++){double a=i*Math.PI/32;disk(b,m,(float)Math.cos(a)*9.72F,(float)Math.sin(a)*9.72F,.08F,.97F,.87F,.63F,.8F);}
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
            disk(b,m,x*3,z*3,1.85F,.2F,.25F,.46F,1);
            disk(b,m,x*3,z*3,1.35F,.025F,.025F,.075F,1);
        }
        pose.popPose();BufferUploader.drawWithShader(b.end());
        RenderSystem.disableBlend();RenderSystem.enableCull();RenderSystem.depthMask(true);
    }
    private static void sky(BufferBuilder b,Matrix4f m,int side,int lat){
        double e=-Math.PI/2+lat*Math.PI/24;float horizon=(float)Math.pow(Math.cos(e),6);
        int top=com.coraline.config.OtherWorldVisualConfig.ZENITH.get(),edge=com.coraline.config.OtherWorldVisualConfig.HORIZON.get();
        // Darken the sky dome alone. The lightmap, terrain, hands and UI keep
        // their existing brightness, unlike a global color-grade change.
        float shade=.19F+.09F*horizon;
        point(b,m,DOME[lat][side],
            channel(top,edge,16,horizon)*shade,channel(top,edge,8,horizon)*shade,channel(top,edge,0,horizon)*shade,1);
    }
    private static void aurora(BufferBuilder b,Matrix4f m,int i,int ribbon,float height,float time){
        double a=i*Math.PI/80;double e=.35+ribbon*.15+Math.sin(a*3+time*.0008+ribbon)*.12+height*.23;
        point(b,m,(float)(Math.cos(a)*Math.cos(e)*94),(float)(Math.sin(e)*94),(float)(Math.sin(a)*Math.cos(e)*94),ribbon==1?.47F:.26F,.58F,.94F,com.coraline.config.OtherWorldVisualConfig.AURORA.get().floatValue()*(float)Math.pow(Math.sin(height*Math.PI),2));
    }
    private static float channel(int a,int b,int shift,float t){return (((a>>shift)&255)*(1-t)+((b>>shift)&255)*t)/255F;}
    private static void meteor(BufferBuilder b,Matrix4f m,float time){
        float t=(time%2400)/20F;if(t>1.4F)return;float fade=(float)Math.sin(t/1.4*Math.PI)*.6F;
        for(int i=0;i<18;i++){
            float x=-35+t*24-i*.65F,y=60-t*8+i*.2F,alpha=fade*(1-i/18F);
            point(b,m,new Vec3(x,y,-64),.8F,.9F,1,alpha);point(b,m,new Vec3(x+.7,y-.23,-64),.8F,.9F,1,alpha);
            point(b,m,new Vec3(x+.7,y-.13,-64),.8F,.9F,1,alpha);point(b,m,new Vec3(x,y+.1,-64),.8F,.9F,1,alpha);
        }
    }
    private static void disk(BufferBuilder b,Matrix4f m,float x,float z,float radius,float r,float g,float blue,float alpha){
        int segments=radius<.5?8:80;
        for(int i=0;i<segments;i++){
            double a=i*Math.PI*2/segments,c=(i+1)*Math.PI*2/segments;
            point(b,m,x,85,z,r,g,blue,alpha);point(b,m,x+(float)Math.cos(a)*radius,85,z+(float)Math.sin(a)*radius,r,g,blue,alpha);
            point(b,m,x+(float)Math.cos(c)*radius,85,z+(float)Math.sin(c)*radius,r,g,blue,alpha);point(b,m,x,85,z,r,g,blue,alpha);
        }
    }
    private static void point(BufferBuilder b,Matrix4f m,Vec3 v,float r,float g,float blue,float a){b.vertex(m,(float)v.x,(float)v.y,(float)v.z).color(r,g,blue,a).endVertex();}
    private static void point(BufferBuilder b,Matrix4f m,float x,float y,float z,float r,float g,float blue,float a){b.vertex(m,x,y,z).color(r,g,blue,a).endVertex();}
}
