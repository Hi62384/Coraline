package com.coraline.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.Vec3;

/** Owner-specific procedural skies; no raster assets or global sky changes. */
public final class TycoonSky extends DimensionSpecialEffects {
    private static final float[][] TOP={{1,1,1},{.22F,.52F,.92F},{.17F,.46F,.86F},{.39F,.17F,.46F},{.12F,.13F,.36F},{.025F,.035F,.14F}};
    private static final float[][] EDGE={{1,1,1},{.84F,.93F,1},{.95F,.86F,.69F},{1,.57F,.33F},{.47F,.63F,.79F},{.36F,.22F,.52F}};
    public TycoonSky(){super(Float.NaN,true,SkyType.NORMAL,true,false);}
    @Override public Vec3 getBrightnessDependentFogColor(Vec3 color,float sun){var c=EDGE[TycoonWorldClientState.sky()];return new Vec3(c[0],c[1],c[2]);}
    @Override public boolean isFoggyAt(int x,int z){return false;}
    @Override public float[] getSunriseColor(float time,float partial){return null;}
    @Override public void adjustLightmapColors(net.minecraft.client.multiplayer.ClientLevel l,float partial,float dark,float flicker,float sky,int block,int skyIndex,org.joml.Vector3f color){int tier=TycoonWorldClientState.sky();if(tier==0)color.set(1,1,1);else{float local=Math.max(tier>=4?.75F:.87F,block/15F);color.set(local,tier==3?local*.93F:local,local);}}
    @Override public boolean renderClouds(net.minecraft.client.multiplayer.ClientLevel l,int ticks,float partial,PoseStack pose,double x,double y,double z,org.joml.Matrix4f projection){return true;}
    @Override public boolean renderSky(net.minecraft.client.multiplayer.ClientLevel l,int ticks,float partial,PoseStack pose,net.minecraft.client.Camera camera,org.joml.Matrix4f projection,boolean foggy,Runnable fog){
        fog.run();int tier=TycoonWorldClientState.sky();if(tier==0||foggy)return true;float time=ticks+partial;
        RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try{
            var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);var matrix=pose.last().pose();
            for(int lat=0;lat<20;lat++)for(int side=0;side<64;side++)for(int corner=0;corner<4;corner++){
                int s=side+(corner==1||corner==2?1:0),a=lat+(corner>=2?1:0);double az=s*Math.PI/32,e=-Math.PI/2+a*Math.PI/20;float blend=(float)Math.pow(Math.cos(e),4);var top=TOP[tier];var edge=EDGE[tier];vertex(b,matrix,Math.cos(az)*Math.cos(e)*100,Math.sin(e)*100,Math.sin(az)*Math.cos(e)*100,top[0]*(1-blend)+edge[0]*blend,top[1]*(1-blend)+edge[1]*blend,top[2]*(1-blend)+edge[2]*blend,1);
            }
            for(int band=0;band<(tier>=2?4:2);band++)for(int i=0;i<80;i++){double a=i*Math.PI/40+time*.000025*(band%2==0?1:-1),height=.18+band*.13+Math.sin(a*3+band)*.035;strip(b,matrix,a,a+Math.PI/40,height,height+.055,94,tier==3?1:.94F,tier==3?.73F:.94F,tier>=4?1:.96F,tier>=4?.16F:.25F);}
            if(tier>=4){var random=new java.util.Random(71429);for(int i=0;i<(tier==5?360:100);i++){double az=random.nextDouble()*Math.PI*2,e=.12+random.nextDouble()*1.3;quad(b,matrix,Math.cos(az)*Math.cos(e)*92,Math.sin(e)*92,Math.sin(az)*Math.cos(e)*92,.06+random.nextDouble()*.14,.78F,.9F,1,.55F+.35F*(float)Math.sin(time*.012+i));}for(int ribbon=0;ribbon<(tier==5?3:1);ribbon++)for(int i=0;i<100;i++){double a=i*Math.PI/50,height=.5+ribbon*.18+Math.sin(a*3+time*.001+ribbon)*.09;strip(b,matrix,a,a+Math.PI/50,height,height+.13,90,.4F,.8F,.9F,.19F);}}
            if(tier<=3){double az=.9,e=tier==3?.24:.75;quad(b,matrix,Math.cos(az)*Math.cos(e)*89,Math.sin(e)*89,Math.sin(az)*Math.cos(e)*89,tier==3?5:3,1,.91F,.63F,.9F);}
            BufferUploader.drawWithShader(b.end());
        }finally{RenderSystem.disableBlend();RenderSystem.enableCull();RenderSystem.depthMask(true);}
        return true;
    }
    private static void strip(BufferBuilder b,org.joml.Matrix4f m,double a,double next,double low,double high,double radius,float r,float g,float blue,float alpha){for(int i=0;i<4;i++){double az=i==1||i==2?next:a,e=i>=2?high:low;vertex(b,m,Math.cos(az)*Math.cos(e)*radius,Math.sin(e)*radius,Math.sin(az)*Math.cos(e)*radius,r,g,blue,alpha);}}
    private static void quad(BufferBuilder b,org.joml.Matrix4f m,double x,double y,double z,double size,float r,float g,float blue,float alpha){vertex(b,m,x-size,y-size,z,r,g,blue,alpha);vertex(b,m,x+size,y-size,z,r,g,blue,alpha);vertex(b,m,x+size,y+size,z,r,g,blue,alpha);vertex(b,m,x-size,y+size,z,r,g,blue,alpha);}
    private static void vertex(BufferBuilder b,org.joml.Matrix4f m,double x,double y,double z,float r,float g,float blue,float alpha){b.vertex(m,(float)x,(float)y,(float)z).color(r,g,blue,alpha).endVertex();}
}
