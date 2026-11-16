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
    private final PlayerModel<FishingContestNpcEntity> regular,slim;
    private static PlayerModel<FishingContestNpcEntity> partyModel(EntityRendererProvider.Context context,boolean slim){return new PlayerModel<>(context.bakeLayer(slim?ModelLayers.PLAYER_SLIM:ModelLayers.PLAYER),slim){
        @Override public void setupAnim(FishingContestNpcEntity npc,float limb,float amount,float age,float yaw,float pitch){
            super.setupAnim(npc,limb,amount,age,yaw,pitch);
            if(npc.partyGesture()!=0){
                int gesture=npc.partyGesture();float pulse=(float)Math.sin(age*.35F);
                switch(gesture){
                    case 1->{rightArm.xRot=-1.1F;rightArm.zRot=-.2F+pulse*.12F;}
                    case 2->{head.xRot+=pulse*.08F;leftArm.xRot=-.55F;}
                    case 3->{rightArm.xRot=-1.45F;rightArm.yRot=-.25F;}
                    case 4->{rightArm.xRot=-1.25F+pulse*.16F;leftArm.xRot=-.9F;}
                    case 5->{rightArm.xRot=-1.65F+pulse*.22F;body.xRot=.1F;}
                    case 6->{rightArm.xRot=-1.7F;rightArm.yRot=-.4F;head.xRot=.12F;}
                    case 7->{leftArm.xRot=rightArm.xRot=-1.4F;leftArm.zRot=-.55F;rightArm.zRot=.55F;}
                    case 8->{head.xRot=.25F;leftArm.xRot=-.6F;rightArm.xRot=-.6F;}
                    default->{}
                }
                rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);hat.copyFrom(head);jacket.copyFrom(body);
            }else if(npc.fishing()){
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
    };}
    public FishingContestNpcRenderer(EntityRendererProvider.Context context){super(context,partyModel(context,false),.4F);regular=model;slim=partyModel(context,true);
        addLayer(new ItemInHandLayer<>(this,context.getItemInHandRenderer()));
        addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,new net.minecraft.client.model.HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),new net.minecraft.client.model.HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),context.getModelManager()));
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<FishingContestNpcEntity,PlayerModel<FishingContestNpcEntity>>(this){
            @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,FishingContestNpcEntity npc,float limb,float amount,float partial,float age,float yaw,float pitch){
                DollVisual.buttons(getParentModel().head,pose,buffers,light);
            }
        });
    }
    @Override public void render(FishingContestNpcEntity npc,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        var profile=npc.partyProfile();boolean narrow=false;
        if(profile!=null){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level!=null&&mc.level.getPlayerByUUID(profile.getId()) instanceof net.minecraft.client.player.AbstractClientPlayer player)narrow=player.getModelName().equals("slim");else {var texture=mc.getSkinManager().getInsecureSkinInformation(profile).get(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN);narrow=texture!=null&&"slim".equals(texture.getMetadata("model"));}}
        model=narrow?slim:regular;
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
        var profile=npc.partyProfile();if(profile!=null){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level!=null&&mc.level.getPlayerByUUID(profile.getId()) instanceof net.minecraft.client.player.AbstractClientPlayer player)return player.getSkinTextureLocation();return mc.getSkinManager().getInsecureSkinLocation(profile);}
        return ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/parents/"+(npc.female()?"mom_":"dad_")+npc.skin()+".png");
    }
}
