package com.coraline.client.renderer;

import com.coraline.otherworld.MouseEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class MouseRenderer extends MobRenderer<MouseEntity,MouseRenderer.Model> {
    @Override public void render(MouseEntity mouse,float yaw,float partial,PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){pose.pushPose();CircusMousePresentation.align(mouse,partial,pose);super.render(mouse,yaw,partial,pose,buffers,light);pose.popPose();}
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("coraline","mouse"),"main");
    @Override protected void setupRotations(MouseEntity mouse,PoseStack pose,float age,float yaw,float partial){super.setupRotations(mouse,pose,age,yaw,partial);if(mouse.circusStyle()>=0&&Math.abs(mouse.getXRot())>.01F){pose.translate(0,.18,0);pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(mouse.getXRot()));pose.translate(0,-.18,0);}}
    @Override protected void scale(MouseEntity mouse,PoseStack pose,float partial){if(mouse.circusStyle()>=0){var frame=com.coraline.client.ShowActorPoseClient.frame(mouse);float scale=frame!=null&&frame.pose()==9?Math.max(.45F,frame.reveal()):1F;float ugly=mouse.ugly();pose.scale(1.65F*scale*(1+ugly*.9F),1.65F*scale*(1+ugly*.55F),1.65F*scale*(1+ugly*1.2F));}}
    public MouseRenderer(EntityRendererProvider.Context context){super(context,new Model(context.bakeLayer(LAYER)),.15F);
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<MouseEntity,Model>(this){
            @Override public void render(PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,MouseEntity mouse,float walk,float amount,float partial,float age,float yaw,float pitch){
                var eyes=getParentModel().root.getChild("eyes");eyes.visible=true;eyes.render(pose,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/black_concrete.png"))),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);eyes.visible=false;
                if(mouse.spying()){var stitches=getParentModel().root.getChild("button_stitches");stitches.visible=true;stitches.render(pose,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/light_gray_concrete.png"))),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);stitches.visible=false;}
                var ears=getParentModel().root.getChild("inner_ears");ears.visible=true;ears.render(pose,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/pink_wool.png"))),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);ears.visible=false;
                if(mouse.catPose()==1){var horn=getParentModel().root.getChild("trumpet");horn.visible=true;horn.render(pose,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/gold_block.png"))),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);horn.visible=false;}
                if(mouse.ugly()>0){for(String name:new String[]{"mange","teeth"}){var part=getParentModel().root.getChild(name);part.visible=true;part.render(pose,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/"+(name.equals("teeth")?"bone_block_side":"brown_terracotta")+".png"))),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,1,1,1,mouse.ugly());part.visible=false;}}
                if(mouse.circusStyle()>=0&&mouse.ugly()<1){
                    String color=switch(mouse.circusStyle()){case 0->"light_blue_wool";case 1->"green_wool";case 2->"yellow_wool";
                        case 3->"magenta_wool";case 4->"red_wool";default->"cyan_wool";};
                    var bow=getParentModel().root.getChild("costume_bow");bow.visible=true;
                    bow.render(pose,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/"+color+".png"))),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,1,1,1,1-mouse.ugly());
                    bow.visible=false;
                    if(mouse.circusStyle()==4||mouse.circusStyle()==5){var hat=getParentModel().root.getChild("costume_hat");hat.visible=true;
                        hat.render(pose,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/"+color+".png"))),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,1,1,1,1-mouse.ugly());hat.visible=false;}
                }
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(MouseEntity mouse){return ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/"+switch(mouse.circusStyle()){
        case 0->"white_wool";case 1->"brown_wool";case 2->"orange_wool";case 3->"light_gray_wool";case 4->"black_wool";case 5->"purple_wool";default->"gray_wool";}+".png");}
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("trumpet",CubeListBuilder.create().addBox(-.3F,-2,-9,.6F,.6F,4).addBox(-1,-2.5F,-10,2,1.6F,1).addBox(-1.4F,-2.9F,-10.4F,2.8F,2.4F,.5F).addBox(-.4F,-1.4F,-7,.8F,1.1F,.5F),PartPose.offset(0,24,0));
        root.addOrReplaceChild("teeth",CubeListBuilder.create().addBox(-.8F,-1.4F,-6.5F,.55F,1,.6F).addBox(.25F,-1.4F,-6.5F,.55F,1,.6F),PartPose.offset(0,24,0));
        root.addOrReplaceChild("mange",CubeListBuilder.create().addBox(-2.2F,-3.3F,-2,1.7F,1.4F,2).addBox(.5F,-3.5F,.8F,1.8F,1.8F,2.7F).addBox(-.5F,-3.8F,-3,1.2F,.9F,1.8F),PartPose.offset(0,24,0));
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-2,-3,-3,4,3,7),PartPose.offset(0,24,0));
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-1.5F,-3,-6,3,2.5F,3).addBox(-2.5F,-5,-4,2,2,1).addBox(.5F,-5,-4,2,2,1),PartPose.offset(0,24,0));
        root.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(0,0).addBox(-.4F,-.6F,3.5F,.8F,.6F,7),PartPose.offset(0,24,0));
        root.addOrReplaceChild("eyes",CubeListBuilder.create().addBox(-1.65F,-2.4F,-5.9F,.4F,.5F,.6F).addBox(1.25F,-2.4F,-5.9F,.4F,.5F,.6F),PartPose.offset(0,24,0));
        var stitches=CubeListBuilder.create();for(float x:new float[]{-1.72F,1.66F})for(float y:new float[]{-2.30F,-2.10F})for(float z:new float[]{-5.8F,-5.55F})stitches.addBox(x,y,z,.08F,.08F,.08F);root.addOrReplaceChild("button_stitches",stitches,PartPose.offset(0,24,0));
        root.addOrReplaceChild("inner_ears",CubeListBuilder.create().addBox(-2.1F,-4.6F,-4.05F,1.2F,1.2F,.2F).addBox(.9F,-4.6F,-4.05F,1.2F,1.2F,.2F),PartPose.offset(0,24,0));
        root.addOrReplaceChild("costume_bow",CubeListBuilder.create().addBox(-2F,-1.2F,-5F,1.5F,1F,.3F).addBox(.5F,-1.2F,-5F,1.5F,1F,.3F),PartPose.offset(0,24,0));
        root.addOrReplaceChild("costume_hat",CubeListBuilder.create().addBox(-1.3F,-5.8F,-5.2F,2.6F,.4F,2.6F).addBox(-.8F,-7.1F,-4.8F,1.6F,1.5F,1.8F),PartPose.offset(0,24,0));
        return LayerDefinition.create(mesh,16,16);
    }
    public static final class Model extends EntityModel<MouseEntity>{
        private final ModelPart root;
        public Model(ModelPart root){this.root=root;for(String name:new String[]{"button_stitches","eyes","inner_ears","costume_bow","costume_hat","trumpet","teeth","mange"})root.getChild(name).visible=false;}
        @Override public void setupAnim(MouseEntity entity,float walk,float amount,float age,float yaw,float pitch){root.getChild("tail").yRot=entity.catPose()>=3?0:(float)Math.sin(age*.45)*.2F;root.getChild("body").y=24-(float)Math.abs(Math.sin(walk*2))*amount*.7F;root.getChild("body").zRot=entity.catPose()==2?(float)Math.sin(age*1.2)*.2F:0;}
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,float r,float g,float b,float alpha){root.render(pose,buffer,light,overlay,r,g,b,alpha);}
    }
}
