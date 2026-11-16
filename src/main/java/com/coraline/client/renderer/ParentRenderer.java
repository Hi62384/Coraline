package com.coraline.client.renderer;
import com.coraline.parents.ParentEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public final class ParentRenderer extends MobRenderer<ParentEntity,PlayerModel<ParentEntity>> {
    public ParentRenderer(EntityRendererProvider.Context c,boolean mom){super(c,new PlayerModel<ParentEntity>(c.bakeLayer(mom?ModelLayers.PLAYER_SLIM:ModelLayers.PLAYER),mom){
        @Override public void setupAnim(ParentEntity p,float limb,float amount,float age,float yaw,float pitch){body.zRot=0;head.zRot=0;super.setupAnim(p,limb,amount,age,yaw,pitch);
            for(var part:java.util.List.of(head,body,rightArm,leftArm,rightLeg,leftLeg)){part.xScale=part.yScale=part.zScale=1;}
            float morph=Math.max(com.coraline.client.FirstEscapeClient.morph(p.getId()),com.coraline.client.ParentRescueClient.morph(p.getId()));if(morph>0){body.xScale=1-morph*.3F;body.yScale=1+morph*.28F;rightArm.yScale=leftArm.yScale=1+morph*.6F;rightArm.xScale=leftArm.xScale=1-morph*.35F;head.xScale=1-morph*.25F;}
            if(p.familyGesture()==80){rightArm.xRot=leftArm.xRot=-.12F;rightArm.zRot=.24F+(float)Math.sin(age*.19)*.025F;leftArm.zRot=-.24F;head.xRot=.22F;}
            if(p.familyGesture()==81){rightArm.xRot=-1.05F;rightArm.yRot=-.18F;leftArm.xRot=-.18F;head.zRot=-.12F;body.xRot=.09F;}
            if(p.familyGesture()==82){body.xRot=.48F;head.xRot=.58F;rightArm.xRot=.2F;leftArm.xRot=.3F;rightLeg.xRot=.27F;leftLeg.xRot=-.12F;}
            if(p.familyGesture()==60){rightArm.xRot=-1.25F;rightArm.yRot=(float)Math.sin(age*.22F)*.3F;leftArm.xRot=-.9F;}
            if(p.familyGesture()==61){rightArm.xRot=-1.6F+(float)Math.sin(age*.8F)*.08F;rightArm.yRot=-.25F;}
            if(p.familyGesture()==62){rightArm.xRot=leftArm.xRot=-1.35F;rightArm.yRot=-.25F;leftArm.yRot=.25F;}
            if(p.familyGesture()==63){rightArm.xRot=-2.25F;rightArm.yRot=-.3F;}
            if(p.familyGesture()==64){rightArm.xRot=-1.4F;rightArm.zRot=(float)Math.sin(age*.2F)*.18F;}
            if(p.familyGesture()==65){rightArm.xRot=-2.2F;head.xRot=-.2F;}
            if(p.familyGesture()==66){rightArm.xRot=-.45F;rightArm.yRot=0;rightArm.zRot=.14F;leftArm.xRot=-.10F;leftArm.yRot=0;leftArm.zRot=-.10F;head.xRot=.08F;}
            if(p.familyGesture()==67){rightArm.xRot=leftArm.xRot=-.18F;rightArm.zRot=.40F;leftArm.zRot=-.40F;rightArm.yRot=-.10F;leftArm.yRot=.10F;}
            if(p.reunionFrost()>0&&!p.isOtherParent()){float cold=p.reunionFrost();float shiver=(float)Math.sin(age*1.7F)*.008F*cold;body.zRot+=shiver*.4F;rightArm.zRot+=shiver;leftArm.zRot-=shiver;head.zRot+=shiver*.3F;rightArm.xRot-=.05F*cold;leftArm.xRot-=.05F*cold;if(p.familyGesture()==69)rightArm.xRot-=.35F+(float)Math.sin(age*.08F)*.035F;}
            if(p.familyGesture()==40){float lift=com.coraline.feast.CocoaBeetlesChoreography.biteLift(com.coraline.client.FirstEscapeClient.phase(),com.coraline.client.FirstEscapeClient.time(),com.coraline.client.FirstEscapeClient.duration());rightArm.xRot=-1.05F-lift*.85F;rightArm.yRot=-.4F-lift*.1F;head.xRot=.15F*lift;rightSleeve.copyFrom(rightArm);}
            if(p.familyGesture()==41){body.zRot=(float)Math.sin(age*.4)*morph*.08F;head.zRot=-morph*.13F;}
            if(p.familyGesture()==42){rightArm.xRot=-1.55F;rightArm.zRot=-.3F;rightSleeve.copyFrom(rightArm);}
            if(com.coraline.client.FirstEscapeClient.angry(p.getId())){head.xRot+=.10F;head.zRot=0;body.xRot=.06F;if(p.familyGesture()!=67){leftArm.zRot=.16F;rightArm.zRot=-.16F;}}
            // The piano hand stretches only the mouth layer, never hides the head.
            head.visible=hat.visible=true;
            if(p.familyGesture()==30){rightArm.xRot=-1.45F;rightArm.yRot=-.5F;leftArm.xRot=-1.4F;leftArm.yRot=.5F;rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);}
            if(p.familyGesture()==31){rightArm.xRot=-.7F;rightSleeve.copyFrom(rightArm);}
            if(p.familyGesture()==32){rightArm.xRot=leftArm.xRot=-2.1F;rightArm.yRot=-.25F;leftArm.yRot=.25F;rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);}
            if(p.isUsingItem()&&(p.getUseItem().getUseAnimation()==net.minecraft.world.item.UseAnim.EAT||p.getUseItem().getUseAnimation()==net.minecraft.world.item.UseAnim.DRINK)){rightArm.xRot=-1.55F+(float)Math.sin(age*1.5F)*.08F;rightArm.yRot=-.28F;rightSleeve.copyFrom(rightArm);}
            int gesture=p.familyGesture();float wave=(float)Math.sin(age*.18F);if(gesture==1){rightArm.zRot=-1.4F+wave*.4F;leftArm.zRot=1.4F-wave*.4F;}if(gesture==2||gesture==6){rightArm.xRot=-1.2F+wave*.25F;leftArm.xRot=-.8F;}if(gesture==3)head.zRot=wave*.4F;if(gesture==4){rightArm.xRot=leftArm.xRot=-1.5F;}if(gesture==5){body.xRot=.15F;head.zRot=wave*.2F;}if(gesture==7)rightArm.xRot=-1.2F;if(gesture==8){head.xRot=.5F;rightArm.xRot=-2;}if(gesture>0){hat.copyFrom(head);rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);}
            if(p.getVehicle() instanceof com.coraline.feast.ScenePropEntity prop&&prop.kind()==com.coraline.feast.ScenePropEntity.PIANO){
                rightLeg.xRot=leftLeg.xRot=-1.35F;rightLeg.yRot=.18F;leftLeg.yRot=-.18F;
                if(prop.mode()>0){rightArm.xRot=-1.2F+(prop.mode()==3?(float)Math.max(0,Math.sin(Math.min(12,com.coraline.client.TraditionalClient.time()%60)*Math.PI/12))*.12F:(float)Math.sin(age*.65)*.18F);leftArm.xRot=prop.mode()==3?-1.2F:-1.2F+(float)Math.sin(age*.65+2)*.18F;rightArm.yRot=-.18F;leftArm.yRot=.18F;if(prop.mode()==2){body.zRot=(float)Math.sin(age*.25)*.12F;head.zRot=(float)Math.sin(age*.25)*.16F;}}
                jacket.copyFrom(body);hat.copyFrom(head);rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);rightPants.copyFrom(rightLeg);leftPants.copyFrom(leftLeg);
            }
        }
    },.4F);
        addLayer(new ParentWardrobeLayer(this));
        addLayer(new ParentReunionLayer(this));
        addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this,c.getItemInHandRenderer()));
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<ParentEntity,PlayerModel<ParentEntity>>(this){@Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,ParentEntity parent,float limb,float amount,float partial,float age,float yaw,float pitch){FirstEscapePresentation.heldBeetle(parent,getParentModel(),pose,buffers,light);FirstEscapePresentation.angryFace(parent,getParentModel(),pose,buffers,light);}});
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<ParentEntity,PlayerModel<ParentEntity>>(this){
            @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,ParentEntity parent,float limb,float amount,float partial,float age,float yaw,float pitch){if(parent.isOtherParent()&&!parent.isInvisible()){if(!com.coraline.client.ParentRescueClient.disguised(parent.getId()))DollVisual.buttons(getParentModel().head,pose,buffers,light);TraditionalPresentation.parent(parent,getParentModel(),pose,buffers,light,ParentRenderer.this.getTextureLocation(parent));}}
        });
    }
    @Override public ResourceLocation getTextureLocation(ParentEntity p){return ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/parents/"+(p.isMom()?"mom_":"dad_")+p.skin()+".png");}
    @Override public void render(ParentEntity p,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
        if(p.isInvisible())return; // Concealed story actors have no eyes, held props or labels.
        if(RescuePresentation.mother(p,yaw,partial,pose,buffers,light))return;
        if(FirstEscapePresentation.mother(p,yaw,partial,pose,buffers,light))return;
        super.render(p,yaw,partial,pose,buffers,light);
        if(com.coraline.client.AcceptedButtonEyesClient.mother(p.getId())||com.coraline.client.ParentRescueClient.mother()==p.getId()||com.coraline.client.FirstEscapeClient.active()&&p.getId()==com.coraline.client.FirstEscapeClient.mother())return;
        if(!p.isOtherParent()||!p.isMom()||p.deliveryResident()||entityRenderDispatcher.distanceToSqr(p)>4096)return;
        pose.pushPose();pose.translate(0,p.getBbHeight()+.9,0);pose.mulPose(entityRenderDispatcher.cameraOrientation());pose.scale(-.018F,-.018F,.018F);
        var font=getFont();String state=com.coraline.feast.OtherMotherLoopState.classify(p.trust(),p.suspicion()).id().replace('_',' ');
        String[] lines={meter("Trust",p.trust()),meter("Suspicion",p.suspicion()),state};
        for(int i=0;i<lines.length;i++)font.drawInBatch(lines[i],-font.width(lines[i])/2F,i*11,i==0?0xFF8DE7C4:i==1?0xFFFFCE8A:0xFFFFFFFF,false,pose.last().pose(),buffers,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0x990D1326,15728880);
        pose.popPose();
    }
    private static String meter(String label,float value){int bars=Math.round(value*10);return label+" ["+"|".repeat(bars)+"·".repeat(10-bars)+"] "+String.format(java.util.Locale.ROOT,"%.2f",value);}
}
