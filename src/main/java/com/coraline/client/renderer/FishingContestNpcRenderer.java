package com.coraline.client.renderer;

import com.coraline.feast.FishingContestNpcEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import net.minecraft.resources.ResourceLocation;

/** Reuses the varied human skin set while sewing buttons over the eyes. */
public final class FishingContestNpcRenderer extends MobRenderer<FishingContestNpcEntity,PlayerModel<FishingContestNpcEntity>> {
    public FishingContestNpcRenderer(EntityRendererProvider.Context context){super(context,new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER),false){
        @Override public void setupAnim(FishingContestNpcEntity npc,float limb,float amount,float age,float yaw,float pitch){
            super.setupAnim(npc,limb,amount,age,yaw,pitch);
            if(npc.fishing()){
                int phase=Math.floorMod(npc.tickCount+npc.slot()*31,120);
                float cast=phase<12?phase/12F:phase<18?1F:phase<106?1F:Math.max(0,(120-phase)/14F);
                rightArm.xRot=-.4F-cast*1.3F+(phase>18&&phase<106?(float)Math.sin(age*.18F)*.06F:0);
                rightArm.yRot=-.25F;rightSleeve.copyFrom(rightArm);
            }else if(npc.baking()){
                rightArm.xRot=-1.15F+(float)Math.sin(age*.8F)*.38F;
                rightArm.zRot=(float)Math.sin(age*.8F)*.22F;
                leftArm.xRot=-.9F+(float)Math.sin(age*.8F+1.6F)*.22F;
                body.yRot=(float)Math.sin(age*.35F)*.06F;
                rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);jacket.copyFrom(body);
            }
        }
    },.4F);
        addLayer(new ItemInHandLayer<>(this,context.getItemInHandRenderer()));
        addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,new net.minecraft.client.model.HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),new net.minecraft.client.model.HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),context.getModelManager()));
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<FishingContestNpcEntity,PlayerModel<FishingContestNpcEntity>>(this){
            @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,FishingContestNpcEntity npc,float limb,float amount,float partial,float age,float yaw,float pitch){
                DollVisual.buttons(getParentModel().head,pose,buffers,light);
            }
        });
    }
    @Override public void render(FishingContestNpcEntity npc,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        super.render(npc,yaw,partial,pose,buffers,light);
        if(!npc.fishing())return;
        int phase=Math.floorMod(npc.tickCount+npc.slot()*31,120);
        if(phase<18||phase>=112)return;
        float reach=phase<26?(phase-18)/8F:phase>104?(112-phase)/8F:1F;
        float tx=(npc.slot()==0?6:npc.slot()==1?-6:0)*reach;
        float tz=(npc.slot()>=2?6:0)*reach;
        float ty=-.65F+(float)Math.sin((npc.tickCount+partial)*.15F)*.045F;
        Matrix4f matrix=pose.last().pose();Matrix3f normal=pose.last().normal();
        VertexConsumer line=buffers.getBuffer(RenderType.lines());
        line.vertex(matrix,0,1.4F,0).color(31,25,20,255).normal(normal,0,1,0).endVertex();
        line.vertex(matrix,tx,ty,tz).color(31,25,20,255).normal(normal,0,1,0).endVertex();
        float r=.09F;
        line.vertex(matrix,tx-r,ty,tz).color(220,45,48,255).normal(normal,0,1,0).endVertex();
        line.vertex(matrix,tx+r,ty,tz).color(220,45,48,255).normal(normal,0,1,0).endVertex();
        line.vertex(matrix,tx,ty-r,tz).color(245,245,242,255).normal(normal,0,1,0).endVertex();
        line.vertex(matrix,tx,ty+r,tz).color(245,245,242,255).normal(normal,0,1,0).endVertex();
    }
    @Override public ResourceLocation getTextureLocation(FishingContestNpcEntity npc){
        return ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/parents/"+(npc.female()?"mom_":"dad_")+npc.skin()+".png");
    }
}
