package com.coraline.client.renderer;

import com.coraline.feast.*;
import com.coraline.tea.TeaVisitorEntity;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Small native meshes and articulated motions; no billboard candy, sticks or hidden stone sprites. */
public final class MissingParentsTeaPresentation {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png");
    public static MissingParentsTea.Phase phase(TeaVisitorEntity tea){int i=tea.phase()-100;return i>=0&&i<MissingParentsTea.Phase.values().length?MissingParentsTea.Phase.values()[i]:null;}
    public static float time(TeaVisitorEntity tea,float partial){return tea.elapsed()+(tea.storyClock()?partial:0);}
    public static boolean pose(TeaVisitorEntity tea,Map<String,ModelPart> bones,float age){var state=phase(tea);if(state==null)return false;float t=time(tea,age-(int)age);boolean spink=tea.characterId().equals("miss_spink");var body=bones.get("body");var head=bones.get("head");
        if(tea.isPassenger())for(String side:List.of("left","right")){bones.get(side+"_leg").xRot=-1.3F;var shin=bones.get(side+"_shin");if(shin!=null)shin.xRot=1.25F;}
        if(!spink&&state==MissingParentsTea.Phase.STORY_D&&tea.storyMotion()>0&&tea.storyMotion()<4){for(String side:List.of("left","right")){bones.get(side+"_arm").xRot=tea.storyMotion()==2?-.95F:-1.3F;bones.get(side+"_arm").yRot=side.equals("left")?.42F:-.42F;bones.get(side+"_forearm").xRot=-.8F;}body.xRot=tea.storyMotion()==2?.09F:.25F;}
        if(spink&&state==MissingParentsTea.Phase.STORY_F){float rise=Math.min(1,t/14),lower=Math.min(1,Math.max(0,(t-20)/12));float alternating=(float)Math.sin(t*1.25)*.38F*lower;for(String side:List.of("left","right")){var arm=bones.get(side+"_arm");var fore=bones.get(side+"_forearm");arm.xRot=-2.8F*rise+1.4F*lower+(side.equals("left")?alternating:-alternating);arm.zRot=side.equals("left")?.2F:-.2F;fore.xRot=-.15F-.3F*lower;}body.xRot=.12F*lower;head.xRot=t<28?-.15F:.45F;}
        if(spink&&state==MissingParentsTea.Phase.STORY_G){float lean=Math.min(1,Math.max(0,(t-12)/8));body.xRot=.32F*lean;head.xRot=.4F*lean;}
        if(state==MissingParentsTea.Phase.STORY_E||state==MissingParentsTea.Phase.STORY_H){bones.get("right_arm").xRot=-.8F;head.xRot=.15F;}
        if(state==MissingParentsTea.Phase.STORY_I&&t>100){bones.get("right_arm").zRot=-1.3F+(float)Math.sin(t*.15)*.15F;}
        return true;
    }
    private static void vertex(PoseStack p,VertexConsumer b,int light,double x,double y,double z,double nx,double ny,double nz,int color){b.vertex(p.last().pose(),(float)x,(float)y,(float)z).color(((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,1).uv(.5F,.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),(float)nx,(float)ny,(float)nz).endVertex();}
    private static void oval(PoseStack p,VertexConsumer b,int light,double x,double y,double z,double rx,double ry,double rz,int color){for(int j=0;j<6;j++)for(int i=0;i<16;i++)for(int[] ix:new int[][]{{j,i},{j+1,i},{j+1,i+1},{j,i+1}}){double a=-Math.PI/2+ix[0]*Math.PI/6,t=ix[1]*Math.PI/8,nx=Math.cos(a)*Math.cos(t),ny=Math.sin(a),nz=Math.cos(a)*Math.sin(t);vertex(p,b,light,x+nx*rx,y+ny*ry,z+nz*rz,nx,ny,nz,color);}}
    public static void bowl(PoseStack p,MultiBufferSource buffers,int light,int contents,float crush,float dustTime){var b=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        // Open ceramic hemisphere with inside, outside and rolled lip.
        for(int i=0;i<40;i++)for(int band=0;band<5;band++)for(int[] ix:new int[][]{{band,i},{band+1,i},{band+1,i+1},{band,i+1}}){double a=ix[1]*Math.PI/20,r=.12+ix[0]*.035,y=ix[0]*.035;int color=band%2==0?0xEFE1C4:0xB3CDC8;vertex(p,b,light,Math.cos(a)*r,y,Math.sin(a)*r,Math.cos(a),.4,Math.sin(a),color);}
        for(int i=0;i<32;i++){double a=i*Math.PI/16;oval(p,b,light,Math.cos(a)*.295,.18,Math.sin(a)*.295,.025,.025,.025,0xD2B76D);}oval(p,b,light,0,.015,0,.14,.018,.14,0xE4DDD0);
        boolean reveal=contents>=3;float powder=contents>=2?1:crush;float dust=reveal?Math.max(0,1-Math.max(0,dustTime-20)/19):1;
        if(contents<4&&contents>=3){p.pushPose();p.translate(0,.085,0);p.mulPose(Axis.XP.rotationDegrees(90));p.scale(.43F,.43F,.43F);SeeingStoneRenderer.stone(p,buffers,light);p.popPose();}
        if(contents<=2||contents==3&&dust>0){for(int i=0;i<18;i++){double a=i*2.399,r=.045*Math.sqrt(i),x=Math.cos(a)*r,z=Math.sin(a)*r;int color=i%2==0?0xED92B7:0x80BBD7;if(powder<1&&i/18F>=powder){p.pushPose();p.translate(x,.10+(i%3)*.027,z);p.mulPose(Axis.YP.rotationDegrees(i*47));oval(p,b,light,0,0,0,.075,.035,.03,color);p.popPose();}else if(dust>0)oval(p,b,light,x,.055+(i%3)*.005,z,.055,.01*dust,.045,color);}}
        if(contents==3&&dustTime>=20&&dustTime<46)for(int i=0;i<14;i++){double age=(dustTime-20+i%3)/25;oval(p,b,light,(i%4-1.5)*.045+age*.45,.1+age*.35,(i/4-1.5)*.05+Math.sin(i*3)*age*.1,.012,.009,.01,i%2==0?0xED92B7:0x80BBD7);}
    }
    public static boolean prop(CinematicPropEntity e,PoseStack p,MultiBufferSource b,int light,float partial){if(e.kind()!=CinematicPropEntity.TAFFY_BOWL)return false;if(!e.isInvisible())bowl(p,b,light,e.cue(),e.value(),e.time()+partial);return true;}
    public static void held(TeaVisitorEntity tea,CharacterModel model,PoseStack p,MultiBufferSource buffers,int light,float partial){var state=phase(tea);if(state==null)return;boolean spink=tea.characterId().equals("miss_spink");var b=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        if(spink&&state==MissingParentsTea.Phase.STORY_F){for(String side:List.of("left","right")){p.pushPose();model.transformBone(side+"_forearm",p);p.translate(0,.32,-.04);float lower=Math.min(1,Math.max(0,(time(tea,partial)-20)/12));p.mulPose(Axis.XP.rotationDegrees(-18+lower*118));needle(p,b,light);p.popPose();}}
        if(!spink&&state==MissingParentsTea.Phase.STORY_D){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level==null)return;var owner=tea.getPersistentData(); // Hand attachment uses the synced scene cue, never client inventory state.
            // Server supplies a separate hand flag; pickup/putdown occurs only after reaching the counter/table.
            if(tea.hasCup()){p.pushPose();var left=new PoseStack();var right=new PoseStack();model.transformBone("left_forearm",left);model.transformBone("right_forearm",right);var a=left.last().pose().transformPosition(new org.joml.Vector3f(0,.35F,-.05F));var c=right.last().pose().transformPosition(new org.joml.Vector3f(0,.35F,-.05F));a.add(c).mul(.5F);p.translate(a.x,a.y,a.z);p.scale(1,-1,-1);bowl(p,buffers,light,1,0,0);p.popPose();}}
    }
    private static void needle(PoseStack p,VertexConsumer b,int light){for(int band=0;band<2;band++)for(int i=0;i<10;i++)for(int[] ix:new int[][]{{0,i},{1,i},{1,i+1},{0,i+1}}){double a=ix[1]*Math.PI/5,y=band==0?ix[0]*.64:.64+ix[0]*.10,r=band==0?.009:.009*(1-ix[0]);vertex(p,b,light,Math.cos(a)*r,y,Math.sin(a)*r,Math.cos(a),0,Math.sin(a),i%3==0?0xECF1F4:0x9DAAB9);}oval(p,b,light,0,-.02,0,.025,.022,.025,0xD6BB78);}
    private MissingParentsTeaPresentation(){}
}
