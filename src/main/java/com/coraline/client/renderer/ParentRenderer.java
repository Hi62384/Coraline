package com.coraline.client.renderer;
import com.coraline.parents.ParentEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public final class ParentRenderer extends MobRenderer<ParentEntity,PlayerModel<ParentEntity>> {
    public ParentRenderer(EntityRendererProvider.Context c,boolean mom){super(c,new PlayerModel<ParentEntity>(c.bakeLayer(mom?ModelLayers.PLAYER_SLIM:ModelLayers.PLAYER),mom){
        @Override public void setupAnim(ParentEntity p,float limb,float amount,float age,float yaw,float pitch){body.zRot=0;head.zRot=0;super.setupAnim(p,limb,amount,age,yaw,pitch);
            if(p.getVehicle() instanceof com.coraline.feast.ScenePropEntity prop&&prop.kind()==com.coraline.feast.ScenePropEntity.PIANO){
                rightLeg.xRot=leftLeg.xRot=-1.35F;rightLeg.yRot=.18F;leftLeg.yRot=-.18F;
                if(prop.mode()>0){rightArm.xRot=-1.2F+(float)Math.sin(age*.65)*.18F;leftArm.xRot=-1.2F+(float)Math.sin(age*.65+2)*.18F;rightArm.yRot=-.18F;leftArm.yRot=.18F;if(prop.mode()==2){body.zRot=(float)Math.sin(age*.25)*.12F;head.zRot=(float)Math.sin(age*.25)*.16F;}}
                jacket.copyFrom(body);hat.copyFrom(head);rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);rightPants.copyFrom(rightLeg);leftPants.copyFrom(leftLeg);
            }
        }
    },.4F);
        addLayer(new ParentWardrobeLayer(this));
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<ParentEntity,PlayerModel<ParentEntity>>(this){
            @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,ParentEntity parent,float limb,float amount,float partial,float age,float yaw,float pitch){if(parent.isOtherParent())DollVisual.buttons(getParentModel().head,pose,buffers,light);}
        });
    }
    @Override public ResourceLocation getTextureLocation(ParentEntity p){return ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/parents/"+(p.isMom()?"mom_":"dad_")+p.skin()+".png");}
    @Override public void render(ParentEntity p,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
        super.render(p,yaw,partial,pose,buffers,light);
        if(!p.isOtherParent()||!p.isMom()||entityRenderDispatcher.distanceToSqr(p)>4096)return;
        pose.pushPose();pose.translate(0,p.getBbHeight()+.9,0);pose.mulPose(entityRenderDispatcher.cameraOrientation());pose.scale(-.018F,-.018F,.018F);
        var font=getFont();String state=com.coraline.feast.OtherMotherLoopState.classify(p.trust(),p.suspicion()).id().replace('_',' ');
        String[] lines={meter("Trust",p.trust()),meter("Suspicion",p.suspicion()),state};
        for(int i=0;i<lines.length;i++)font.drawInBatch(lines[i],-font.width(lines[i])/2F,i*11,i==0?0xFF8DE7C4:i==1?0xFFFFCE8A:0xFFFFFFFF,false,pose.last().pose(),buffers,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0x990D1326,15728880);
        pose.popPose();
    }
    private static String meter(String label,float value){int bars=Math.round(value*10);return label+" ["+"|".repeat(bars)+"·".repeat(10-bars)+"] "+String.format(java.util.Locale.ROOT,"%.2f",value);}
}
