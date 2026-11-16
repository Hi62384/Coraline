package com.coraline.client.renderer;

import com.coraline.postgame.OtherworldResident;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Button-eyed cloth inhabitants, independently modelled rather than a
 * vanilla villager skin. Role hats and coats distinguish the three trades. */
public final class OtherworldResidentModel extends EntityModel<OtherworldResident> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("coraline","otherworld_resident"),"main");
    private final ModelPart root,head,eyes,body,leftArm,rightArm,leftLeg,rightLeg,cap,goggles,shawl,shield,blade,mask;private int role;
    public OtherworldResidentModel(ModelPart root){this.root=root;head=root.getChild("head");eyes=head.getChild("eyes");body=root.getChild("body");leftArm=root.getChild("left_arm");rightArm=root.getChild("right_arm");leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");cap=root.getChild("cap");goggles=head.getChild("goggles");shawl=root.getChild("shawl");shield=leftArm.getChild("shield");blade=rightArm.getChild("blade");mask=head.getChild("mask");}
    private static CubeListBuilder box(float x,float y,float z,float w,float h,float d){return CubeListBuilder.create().texOffs(0,0).addBox(x,y,z,w,h,d);}
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();var head=root.addOrReplaceChild("head",box(-4,-8,-4,8,8,8).addBox(-4.5F,-9,-3,9,3,6),PartPose.ZERO);
        head.addOrReplaceChild("eyes",box(-3,-5,-4.2F,2,2,.4F).addBox(1,-5,-4.2F,2,2,.4F).addBox(-1.5F,-1.5F,-4.2F,3,.5F,.3F),PartPose.ZERO);
        head.addOrReplaceChild("goggles",box(-3.5F,-5.5F,-4.6F,3,3,.4F).addBox(.5F,-5.5F,-4.6F,3,3,.4F),PartPose.ZERO);
        head.addOrReplaceChild("mask",box(-4.2F,-7,-4.8F,8.4F,6,.5F),PartPose.ZERO);
        root.addOrReplaceChild("cap",box(-5,-9,-5,10,1,10).addBox(-3.5F,-11,-3.5F,7,2,7),PartPose.ZERO);
        root.addOrReplaceChild("body",box(-3.5F,0,-2.5F,7,12,5).addBox(-4,8,-3,8,5,6),PartPose.ZERO);
        root.addOrReplaceChild("shawl",box(-4.5F,0,-3,9,4,6).addBox(-4,4,2.6F,8,8,.5F),PartPose.ZERO);
        root.addOrReplaceChild("left_arm",box(-1.5F,0,-1.5F,3,12,3),PartPose.offset(5,1,0));root.addOrReplaceChild("right_arm",box(-1.5F,0,-1.5F,3,12,3),PartPose.offset(-5,1,0));
        root.getChild("left_arm").addOrReplaceChild("shield",box(-2,3,-3.5F,5,7,1),PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("blade",box(-.5F,7,-.5F,1,9,1).addBox(-2,9,-.5F,4,1,1),PartPose.ZERO);
        root.addOrReplaceChild("left_leg",box(-1.5F,0,-1.5F,3,12,3).addBox(-1.5F,9,-3,3,3,5),PartPose.offset(2,12,0));root.addOrReplaceChild("right_leg",box(-1.5F,0,-1.5F,3,12,3).addBox(-1.5F,9,-3,3,3,5),PartPose.offset(-2,12,0));
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(OtherworldResident entity,float walk,float amount,float age,float yaw,float pitch){root.getAllParts().forEach(ModelPart::resetPose);role=entity.role();head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD;cap.copyFrom(head);leftLeg.xRot=Mth.cos(walk*.65F)*amount*.9F;rightLeg.xRot=-leftLeg.xRot;leftArm.xRot=-leftLeg.xRot*.7F;rightArm.xRot=-rightLeg.xRot*.7F;if(entity.tradingPose()){leftArm.xRot=rightArm.xRot=-1.15F;leftArm.yRot=.18F;rightArm.yRot=-.18F;}if(role==3&&attackTime>0)rightArm.xRot=-1.4F-Mth.sin(Mth.sqrt(attackTime)*Mth.PI)*1.2F;if(role==4&&!entity.freedPose()){leftArm.zRot=-.3F-Mth.sin(age*.2F)*.3F;rightArm.zRot=.3F+Mth.sin(age*.2F)*.3F;leftArm.xRot=-.7F+Mth.sin(age*.15F)*.3F;rightArm.xRot=-.7F-Mth.sin(age*.15F)*.3F;}}
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,float r,float g,float b,float alpha){
        eyes.visible=false;goggles.visible=false;mask.visible=false;head.render(pose,out,light,overlay,.8F,.7F,.58F,alpha);
        float red=role==0?.27F:role==1?.26F:role==3?.2F:.49F,green=role==0?.48F:role==1?.34F:role==3?.38F:.28F,blue=role==0?.42F:role==1?.5F:role==3?.27F:.48F;shield.visible=false;blade.visible=false;
        body.render(pose,out,light,overlay,red,green,blue,alpha);leftArm.render(pose,out,light,overlay,red,green,blue,alpha);rightArm.render(pose,out,light,overlay,red,green,blue,alpha);leftLeg.render(pose,out,light,overlay,.14F,.16F,.2F,alpha);rightLeg.render(pose,out,light,overlay,.14F,.16F,.2F,alpha);
        if(role==0||role==4)cap.render(pose,out,light,overlay,.17F,role==4?.13F:.26F,role==4?.27F:.24F,alpha);if(role==2||role==3)shawl.render(pose,out,light,overlay,role==3?.15F:.27F,role==3?.25F:.17F,role==3?.19F:.3F,alpha);
        if(role==3){pose.pushPose();leftArm.translateAndRotate(pose);shield.visible=true;shield.render(pose,out,light,overlay,.62F,.67F,.68F,alpha);pose.popPose();pose.pushPose();rightArm.translateAndRotate(pose);blade.visible=true;blade.render(pose,out,light,overlay,.8F,.84F,.87F,alpha);pose.popPose();}
        pose.pushPose();head.translateAndRotate(pose);if(role==4){mask.visible=true;mask.render(pose,out,light,overlay,.92F,.91F,.87F,alpha);pose.translate(0,0,-.7F/16);}eyes.visible=true;eyes.render(pose,out,light,overlay,.025F,.025F,.04F,alpha);if(role==1){goggles.visible=true;goggles.render(pose,out,light,overlay,.5F,.65F,.7F,alpha);}pose.popPose();
    }
}
