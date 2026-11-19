package com.coraline.client.renderer;

import com.coraline.otherworld.TunnelCoreEntity;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="coraline",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class TunnelRenderer implements BlockEntityRenderer<TunnelCoreEntity> {
    private static final ResourceLocation FABRIC=texture("tunnel_fabric");
    private static final ResourceLocation DECAYED=texture("tunnel_decayed_fabric");
    private static final ResourceLocation THREAD=texture("tunnel_web_thread");
    private static final int PREVIEW_RINGS=23;

    private static ResourceLocation texture(String path){return ResourceLocation.fromNamespaceAndPath("coraline","textures/block/"+path+".png");}
    public TunnelRenderer(BlockEntityRendererProvider.Context context){}
    @Override public boolean shouldRenderOffScreen(TunnelCoreEntity entity){return true;}

    @Override public void render(TunnelCoreEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        renderFabric(pose,buffers,entity.getLevel()==null?0:entity.getLevel().getGameTime()+partial);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void renderEscape(net.minecraftforge.client.event.RenderLevelStageEvent event){
        boolean rescue=com.coraline.client.ParentRescueClient.crawl();
        if(event.getStage()!=net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES
                ||!rescue&&!com.coraline.client.FirstEscapeClient.phase().equals("ESCAPE_TUNNEL"))return;
        var mc=net.minecraft.client.Minecraft.getInstance();
        var origin=rescue?com.coraline.client.ParentRescueClient.tunnelOrigin():com.coraline.client.FirstEscapeClient.tunnelOrigin();
        if(origin==null||mc.level==null||!com.coraline.otherworld.OtherWorld.isOther(mc.level))return;
        var eye=event.getCamera().getPosition();
        var center=net.minecraft.world.phys.Vec3.atCenterOf(origin);
        var delta=eye.subtract(center);
        var forward=rescue?com.coraline.client.ParentRescueClient.tunnelDirection():com.coraline.client.FirstEscapeClient.tunnelDirection();
        var right=forward.getClockWise();
        double depth=delta.x*forward.getStepX()+delta.z*forward.getStepZ();
        if(depth<0||depth>22||Math.abs(delta.x*right.getStepX()+delta.z*right.getStepZ())>.65||Math.abs(delta.y)>.65)return;
        var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();
        int light=LevelRenderer.getLightColor(mc.level,net.minecraft.core.BlockPos.containing(eye));
        pose.pushPose();pose.translate(-delta.x,-delta.y,-delta.z);
        pose.mulPose(Axis.YP.rotationDegrees(-forward.toYRot()));
        draw(pose,buffers,true,null,0,172,true,light,0);
        buffers.endBatch(RenderType.entityCutoutNoCull(DECAYED));
        buffers.endBatch(RenderType.entityCutoutNoCull(THREAD));
        pose.popPose();
    }

    public static void renderFabric(PoseStack pose,MultiBufferSource buffers,float time){
        pose.pushPose();pose.translate(.5,1.5,0);
        draw(pose,buffers,false,null,-8,144,false,15728880,time);
        pose.popPose();
    }

    static void doorway(PoseStack pose,MultiBufferSource buffers,boolean decayed,PortalAperture.Eye eye,int light,float time){
        if(eye.z()>PREVIEW_RINGS*DeadTunnelGeometry.STEP)return;
        draw(pose,buffers,decayed,eye,0,PREVIEW_RINGS,true,light,time);
    }

    private static void draw(PoseStack pose,MultiBufferSource buffers,boolean dead,PortalAperture.Eye eye,int first,int last,boolean end,int light,float time){
        var fabric=buffers.getBuffer(RenderType.entityCutoutNoCull(dead?DECAYED:FABRIC));
        for(int ring=first;ring<last;ring++)for(int side=0;side<DeadTunnelGeometry.SIDES;side++){
            var a=surface(ring,side,eye!=null,dead,time);
            var b=surface(ring,side+1,eye!=null,dead,time);
            var c=surface(ring+1,side+1,eye!=null,dead,time);
            var d=surface(ring+1,side,eye!=null,dead,time);
            polygon(fabric,pose,eye,List.of(b,a,d,c),dead,false,light);
        }
        if(end){
            for(int side=0;side<DeadTunnelGeometry.SIDES;side++){
                polygon(fabric,pose,eye,DeadTunnelGeometry.end(last,side,eye!=null),dead,true,light);
            }
        }
        if(dead){
            var thread=buffers.getBuffer(RenderType.entityCutoutNoCull(THREAD));
            for(var strand:DeadTunnelGeometry.WEBS){
                if(strand[0].z()<first*DeadTunnelGeometry.STEP||strand[0].z()>(last-.8)*DeadTunnelGeometry.STEP)continue;
                polygon(thread,pose,eye,List.of(strand),true,false,light);
            }
        }
    }

    private static PortalAperture.Vertex surface(int ring,int side,boolean collar,boolean dead,float time){
        var p=DeadTunnelGeometry.wall(ring,side,collar);
        if(dead)return p;
        float pulse=.88F+.12F*(float)Math.sin(side*Math.PI/8-p.z()*.7+time*.008F);
        return new PortalAperture.Vertex(p.x(),p.y(),p.z(),p.u(),p.v(),p.shade()*pulse);
    }

    private static void polygon(VertexConsumer consumer,PoseStack pose,PortalAperture.Eye eye,List<PortalAperture.Vertex> shape,
            boolean decayed,boolean cap,int light){
        var rendered=eye==null?shape:PortalAperture.clip(shape,eye.x(),eye.y(),eye.z());
        if(rendered.size()<3)return;
        var a=shape.get(0);var b=shape.get(1);var c=shape.get(2);
        double tx=b.x()-a.x(),ty=b.y()-a.y(),tz=b.z()-a.z(),ux=c.x()-a.x(),uy=c.y()-a.y(),uz=c.z()-a.z();
        double nx=ty*uz-tz*uy,ny=tz*ux-tx*uz,nz=tx*uy-ty*ux;
        double l=Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(l<1e-9)return;
        float x=(float)(nx/l),y=(float)(ny/l),z=(float)(nz/l);
        if(rendered.size()==4){
            for(var vertex:rendered)emit(consumer,pose,vertex,decayed,cap,light,x,y,z);
            return;
        }
        for(int i=1;i+1<rendered.size();i++){
            emit(consumer,pose,rendered.get(0),decayed,cap,light,x,y,z);
            emit(consumer,pose,rendered.get(i),decayed,cap,light,x,y,z);
            emit(consumer,pose,rendered.get(i+1),decayed,cap,light,x,y,z);
            emit(consumer,pose,rendered.get(i+1),decayed,cap,light,x,y,z);
        }
    }

    private static void emit(VertexConsumer out,PoseStack pose,PortalAperture.Vertex p,boolean dead,boolean cap,int light,float x,float y,float z){
        float shade=p.shade();
        float depth=dead?(float)Math.max(.5,.91-p.z()*.08):.96F;
        float tone=cap?shade:shade*depth;
        out.vertex(pose.last().pose(),(float)p.x(),(float)p.y(),(float)p.z()).color(tone,tone,tone,1F)
                .uv(p.u(),p.v()).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.last().normal(),x,y,z).endVertex();
    }
}
