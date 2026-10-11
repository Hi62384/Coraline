package com.coraline.client;

import com.coraline.config.OtherWorldVisualConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import java.util.*;

/** All ambience shares one bounded collection and one depth-tested translucent draw. */
final class OtherWorldMotes {
    private static final Random RANDOM=new Random(917);
    private static final List<Mote> MOTES=new ArrayList<>();
    private static final class Mote {
        final Vec3 origin;final int kind,life;final float phase;int age;
        Mote(Vec3 p,int kind){origin=p;this.kind=kind;life=100+RANDOM.nextInt(160);phase=RANDOM.nextFloat()*6.28F;}
    }
    static void clear(){MOTES.clear();}
    static void tick(ClientLevel level,Vec3 player,Collection<OtherWorldNature.Anchor> anchors,ParticleStatus settings){
        int cap=settings==ParticleStatus.MINIMAL?0:OtherWorldVisualConfig.MOTES.get()/(settings==ParticleStatus.DECREASED?2:1);
        MOTES.removeIf(m->++m.age>=m.life||m.origin.distanceToSqr(player)>2500);
        if(cap==0){clear();return;}if(MOTES.size()>=cap||anchors.isEmpty()||RANDOM.nextInt(3)!=0)return;
        int choice=RANDOM.nextInt(anchors.size());OtherWorldNature.Anchor a=null;
        for(var candidate:anchors){if(choice--==0){a=candidate;break;}}
        if(a==null||a.pos().distToCenterSqr(player)>2304)return;
        int kind=a.water()?2:a.leaves()&&RANDOM.nextBoolean()?3:RANDOM.nextDouble()<OtherWorldVisualConfig.FIREFLIES.get()?0:1;
        double y=kind==2?-.08:kind==3?.08:.25+RANDOM.nextDouble()*1.3;
        MOTES.add(new Mote(new Vec3(a.pos().getX()+RANDOM.nextDouble(),a.pos().getY()+y,a.pos().getZ()+RANDOM.nextDouble()),kind));
    }
    static void render(RenderLevelStageEvent event){
        if(MOTES.isEmpty())return;
        var camera=event.getCamera();var right=new org.joml.Vector3f(1,0,0).rotate(camera.rotation());var up=new org.joml.Vector3f(0,1,0).rotate(camera.rotation());
        var cam=camera.getPosition();var m=event.getPoseStack().last().pose();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.depthMask(false);RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try{
            var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            for(var mote:MOTES){
                float t=mote.age+event.getPartialTick(),fade=(float)Math.sin(Math.PI*t/mote.life);
                float pulse=mote.kind==0?.2F+.8F*(float)Math.pow(Math.sin(t*.025+mote.phase),2):1;
                double dx=mote.kind==2?0:Math.sin(t*.012+mote.phase)*.45,dz=mote.kind==2?0:Math.cos(t*.01+mote.phase)*.4;
                double dy=mote.kind==3?-t*.008:mote.kind==2?0:Math.sin(t*.018+mote.phase)*.18;
                var p=mote.origin.add(dx,dy,dz).subtract(cam);float distance=(float)p.length();
                float alpha=fade*pulse*Math.max(0,Math.min(1,(48-distance)/12));
                float r=mote.kind==0?1:mote.kind==3?.65F:.7F,g=mote.kind==0?.86F:mote.kind==3?.78F:.86F,blue=mote.kind==0?.38F:mote.kind==3?.46F:1;
                float size=mote.kind==0?.027F:mote.kind==2?.045F:.025F;
                for(int halo=3;halo>=1;halo--){
                    float radius=size*(halo==3?3.5F:halo==2?1.7F:1),a=alpha*(halo==3?.06F:halo==2?.18F:.8F);
                    for(int side=0;side<8;side++){
                        double angle=side*Math.PI/4,next=(side+1)*Math.PI/4;
                        vertex(b,m,p,right,up,0,0,r,g,blue,a);
                        vertex(b,m,p,right,up,(float)Math.cos(angle)*radius,(float)Math.sin(angle)*radius,r,g,blue,a);
                        vertex(b,m,p,right,up,(float)Math.cos(next)*radius,(float)Math.sin(next)*radius,r,g,blue,a);
                        vertex(b,m,p,right,up,0,0,r,g,blue,a);
                    }
                }
            }BufferUploader.drawWithShader(b.end());
        }finally{RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();}
    }
    private static void vertex(BufferBuilder b,org.joml.Matrix4f m,Vec3 p,org.joml.Vector3f r,org.joml.Vector3f u,float x,float y,float red,float green,float blue,float alpha){
        b.vertex(m,(float)p.x+r.x*x+u.x*y,(float)p.y+r.y*x+u.y*y,(float)p.z+r.z*x+u.z*y).color(red,green,blue,alpha).endVertex();
    }
}
