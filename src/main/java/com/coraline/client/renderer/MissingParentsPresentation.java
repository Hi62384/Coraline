package com.coraline.client.renderer;

import com.coraline.feast.CinematicPropEntity;
import com.coraline.feast.MissingParentsCatTimeline;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Mirror-bound parents and paired dolls use the canonical parent skins, not new NPCs. */
public final class MissingParentsPresentation {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png");
    private static PlayerModel<LivingEntity> mom,dad;private static DollVisual dolls;
    private static void box(PoseStack p,VertexConsumer v,int l,double x,double y,double z,double w,double h,double d,int c){CinematicPropRenderer.box(p,v,l,x,y,z,w,h,d,c);}
    public static boolean prop(CinematicPropEntity e,PoseStack p,MultiBufferSource b,int light,float partial){
        if(e.kind()!=CinematicPropEntity.PARENT_MIRROR&&e.kind()!=CinematicPropEntity.PARENT_DOLLS&&e.kind()!=CinematicPropEntity.RESCUE_MEAL&&e.kind()!=CinematicPropEntity.RESCUE_POT)return false;
        var v=b.getBuffer(RenderType.entityCutoutNoCull(WHITE));p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        if(e.kind()==CinematicPropEntity.RESCUE_POT){box(p,v,light,0,0,0,.55,.08,.5,0x1A1D25);for(int side:new int[]{-1,1}){box(p,v,light,side*.26,.08,0,.04,.28,.5,0x343945);box(p,v,light,side*.36,.19,0,.18,.05,.09,0x20232B);box(p,v,light,0,.08,side*.24,.55,.28,.04,0x343945);}box(p,v,light,0,.24,0,.47,.02,.43,0x8D5F35);}
        else if(e.kind()==CinematicPropEntity.RESCUE_MEAL){box(p,v,light,0,0,0,.65,.035,.65,0xF5DBB9);box(p,v,light,0,.04,0,.35,.10,.28,0xB77B42);for(int i=0;i<5;i++)box(p,v,light,-.2+i*.1,.04,-.23,.07,.06,.08,0x559443);}
        else if(e.kind()==CinematicPropEntity.PARENT_DOLLS){if(dolls==null)dolls=new DollVisual();for(int side=0;side<2;side++){p.pushPose();p.translate((side-.5)*.38,0,(1-e.value())*-1.15);p.mulPose(Axis.XP.rotationDegrees(-90));p.scale(.32F,-.32F,-.32F);dolls.renderSkin(skin(side==0,e.slot()>>(side*7)&127),side==0,p,b,light);p.popPose();}}
        else mirror(e,p,b,v,light,partial);
        p.popPose();return true;
    }
    private static ResourceLocation skin(boolean female,int id){return ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/parents/"+(female?"mom_":"dad_")+id+".png");}
    private static void mirror(CinematicPropEntity e,PoseStack p,MultiBufferSource b,VertexConsumer v,int light,float partial){
        box(p,v,light,0,0,.16,2.2,2.8,.10,0x55434B);
        for(int side:new int[]{-1,1})box(p,v,light,side*1.12,0,.04,.13,2.9,.18,0xAA8860);
        box(p,v,light,0,2.82,.04,2.35,.13,.18,0xAA8860);
        box(p,v,light,0,0,.04,2.35,.12,.18,0xAA8860);
        if(e.cue()==3){
            float u=(e.time()+partial)/MissingParentsCatTimeline.SHATTER_TICKS;
            if(u<1)for(int i=0;i<18;i++){
                double x=((i%6)-2.5)*.32,y=.25+(i/6)*.8,a=i*2.399;
                p.pushPose();p.translate(x+Math.sin(a)*u*.55,y-u*u*1.2,-.08-u*(.15+(i%4)*.12));
                p.mulPose(Axis.ZP.rotation((float)(a*u)));p.mulPose(Axis.YP.rotation((float)(a*u*.7)));
                box(p,v,light,0,0,0,.10+(i%3)*.04,.16+(i%2)*.12,.015,0xB8D2D9);p.popPose();
            }
            return;
        }
        box(p,v,15728880,0,.13,.08,2.08,2.55,.03,e.cue()>0?0x6C91A1:0xB1BEC6);
        if(e.cue()==0){line(p,v,-.75,.5,.45,2.45,.035,0xD6E2E6);line(p,v,-.4,.3,.65,2.2,.018,0xD6E2E6);return;}
        // A shallow cold chamber, rendered with the actual parent meshes and skins.
        for(int row=0;row<5;row++)for(int col=0;col<4;col++)box(p,v,15728880,-.78+col*.52,.15+row*.5,.055,.50,.47,.02,(row+col)%2==0?0x597C8B:0x6B8994);
        for(int i=0;i<13;i++)box(p,v,15728880,-.95+i*.16,2.46,-.01,.025,.10+(i%4)*.045,.02,0xC3D7DD);
        if(mom==null){var models=Minecraft.getInstance().getEntityModels();mom=new PlayerModel<>(models.bakeLayer(ModelLayers.PLAYER_SLIM),true);dad=new PlayerModel<>(models.bakeLayer(ModelLayers.PLAYER),false);}
        float writing=e.cue()==2?1:e.value(),t=e.tickCount+partial;var finger=MissingParentsCatTimeline.finger(writing);
        int writer=writing<13F/MissingParentsCatTimeline.strokes()?0:1;
        for(int side=0;side<2;side++){
            var model=side==0?mom:dad;model.setAllVisible(true);
            // Direct model rendering bypasses LivingEntityRenderer, which
            // normally clears EntityModel's default young=true. Without this,
            // even an enlarged mesh still has the half-size baby body.
            model.young=false;model.riding=false;model.attackTime=0;
            for(var part:new net.minecraft.client.model.geom.ModelPart[]{model.head,model.body,model.leftArm,model.rightArm,model.leftLeg,model.rightLeg})part.resetPose();
            model.head.yRot=(side==0?.12F:-.12F)+(float)Math.sin(t*.025)*.04F;
            model.leftArm.xRot=model.rightArm.xRot=-.6F+(float)Math.sin(t*.8F+side)*.025F;
            model.body.zRot=(float)Math.sin(t*1.8F+side)*.012F;
            if(e.cue()==2){model.head.xRot=-.10F-e.value()*.15F;model.body.xRot=-e.value()*.08F;}
            if(side==writer&&writing<1){model.rightArm.xRot=-1.2F+(float)(1.9-finger[1])*1.8F;model.rightArm.zRot=(float)Math.max(-1.1,Math.min(1.1,(finger[0]-(side-.5)*.92)*-1.8));}
            else{model.leftArm.xRot=-1.18F;model.leftArm.zRot=-.18F;}
            model.hat.copyFrom(model.head);model.rightSleeve.copyFrom(model.rightArm);model.leftSleeve.copyFrom(model.leftArm);model.jacket.copyFrom(model.body);model.leftPants.copyFrom(model.leftLeg);model.rightPants.copyFrom(model.rightLeg);
            p.pushPose();p.translate((side-.5)*.96,.22,.005+(1-Math.min(1,writing*5))*.05);p.scale(-1.05F,-1.05F,.12F);p.translate(0,-1.501,0);
            model.renderToBuffer(p,b.getBuffer(RenderType.entityCutoutNoCull(skin(side==0,e.slot()>>(side*7)&127))),15728880,OverlayTexture.NO_OVERLAY,.36F,.63F,.96F,1);p.popPose();
            v=b.getBuffer(RenderType.entityCutoutNoCull(WHITE));
            // Breath condenses and dissipates without spawning persistent entities.
            double breath=(t+side*31)%90/90.;
            if(breath<.55)box(p,v,15728880,(side-.5)*.92+Math.sin(t*.035)*.025,1.73+breath*.08,-.10,.025+breath*.12,.025+breath*.04,.01,0xB7CCD1);
        }
        // Writing is on the front glass, not behind the parents' faces/bodies.
        // Flush the parents first: centered writing belongs on the FRONT glass,
        // with a dark outline so light and dark clothing cannot swallow it.
        if(b instanceof MultiBufferSource.BufferSource batch)batch.endBatch();
        v=b.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        // Keep the writing close to the actual glass: a half-metre foreground
        // offset made it spill outside the frame at ordinary viewing distance.
        p.pushPose();p.translate(0,0,-.12);writing(p,v,writing);p.popPose();
        if(writing>0&&writing<1)box(p,v,15728880,finger[0],finger[1],-.18,.035,.035,.012,0xF0F7F4);
        if(e.cue()==2)for(int i=0;i<9*e.value();i++){double a=i*2.399;line(p,v,0,1.25,Math.cos(a)*(i%2==0?1:.55),1.25+Math.sin(a)*(i%2==0?1.2:.7),.012,0xD6E4E7);}
    }
    private static void writing(PoseStack p,VertexConsumer v,float progress){
        double count=progress*MissingParentsCatTimeline.strokes();
        for(int i=0;i<Math.min(MissingParentsCatTimeline.strokes(),Math.ceil(count));i++){
            var start=MissingParentsCatTimeline.point(i,0);var end=MissingParentsCatTimeline.point(i,Math.min(1,count-i));
            line(p,v,start[0],start[1],end[0],end[1],.042,0x24333C);
            p.pushPose();p.translate(0,0,-.014);line(p,v,start[0],start[1],end[0],end[1],.022,0xF0F7F4);p.popPose();
        }
    }
    private static void line(PoseStack p,VertexConsumer v,double x,double y,double xx,double yy,double width,int color){double dx=xx-x,dy=yy-y,len=Math.sqrt(dx*dx+dy*dy);if(len<.0001)return;p.pushPose();p.translate(x,y,-.035);p.mulPose(Axis.ZP.rotation((float)-Math.atan2(dx,dy)));box(p,v,15728880,0,0,0,width,len,.012,color);p.popPose();}
    private MissingParentsPresentation(){}
}
