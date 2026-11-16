package com.coraline.client.renderer;

import com.coraline.postgame.WhiteVoidAvatarEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Fractured porcelain silhouette with independent hollow face and luminous seams. */
public final class WhiteVoidAvatarModel<E extends net.minecraft.world.entity.monster.Monster & com.coraline.postgame.VoidManifestation> extends EntityModel<E> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("coraline","white_void_avatar"),"main");
    private final ModelPart root,head,leftArm,rightArm,leftLeg,rightLeg,face,cracks;
    private int phase=1;
    public WhiteVoidAvatarModel(ModelPart root){this.root=root;head=root.getChild("head");leftArm=root.getChild("left_arm");rightArm=root.getChild("right_arm");leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");face=head.getChild("hollow");cracks=root.getChild("cracks");}
    private static CubeListBuilder box(float x,float y,float z,float w,float h,float d){return CubeListBuilder.create().texOffs(0,0).addBox(x,y,z,w,h,d);}
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("body",box(-8,0,-6,16,36,12).addBox(-11,1,-5,22,5,10).addBox(-6,28,-5,12,10,10),PartPose.offset(0,-40,0));
        var head=root.addOrReplaceChild("head",box(-7,-24,-5,14,24,10).addBox(-9,-26,-3,4,8,6).addBox(5,-23,-3,3,5,6),PartPose.offset(0,-40,0));
        head.addOrReplaceChild("hollow",box(-5,-20,-5.2F,4,8,.3F).addBox(1,-20,-5.2F,4,8,.3F).addBox(-3,-9,-5.2F,6,7,.3F),PartPose.ZERO);
        root.addOrReplaceChild("left_arm",box(-3,0,-3,6,49,6).addBox(-4,48,-4,2,10,2).addBox(0,48,-4,2,13,2),PartPose.offset(13,-37,0));
        root.addOrReplaceChild("right_arm",box(-3,0,-3,6,45,6).addBox(-3,44,-4,2,15,2).addBox(1,44,-4,2,11,2),PartPose.offset(-13,-37,0));
        root.addOrReplaceChild("left_leg",box(-3,0,-3,6,28,6),PartPose.offset(5,-4,0));
        root.addOrReplaceChild("right_leg",box(-3,0,-3,6,28,6),PartPose.offset(-5,-4,0));
        root.addOrReplaceChild("cracks",box(-.6F,-39,-6.2F,1.2F,18,.3F).addBox(-.6F,-22,-6.2F,5,1.2F,.3F).addBox(3.2F,-22,-6.2F,1.2F,15,.3F).addBox(-7,-32,-6.2F,7,1,.3F).addBox(-5,-13,-6.2F,9,1,.3F),PartPose.ZERO);
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(E e,float walk,float amount,float age,float yaw,float pitch){
        root.getAllParts().forEach(ModelPart::resetPose);phase=e.phase();head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD;
        leftLeg.xRot=Mth.cos(walk*.55F)*amount*.65F;rightLeg.xRot=-leftLeg.xRot;
        leftArm.zRot=-.12F-Mth.sin(age*.04F)*.035F;rightArm.zRot=.17F+Mth.cos(age*.05F)*.035F;
        leftArm.xRot=-rightLeg.xRot*.4F;rightArm.xRot=-leftLeg.xRot*.4F;
        if(e.cast()==1){rightArm.xRot=-1.5F;rightArm.yRot=-.2F;}
        if(e.cast()==2){leftArm.xRot=rightArm.xRot=-2.6F;head.xRot=.3F;}
        if(e.cast()==3){leftArm.zRot=-1.2F;rightArm.zRot=1.2F;}
        if(e.cast()==4){head.zRot=Mth.sin(age*.6F)*.14F;}
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer vertices,int light,int overlay,float r,float g,float b,float alpha){
        face.visible=false;cracks.visible=false;root.render(pose,vertices,light,overlay,.91F,.94F,.96F,alpha);
        face.visible=true;pose.pushPose();head.translateAndRotate(pose);face.render(pose,vertices,light,overlay,.025F,.018F,.045F,alpha);pose.popPose();
        cracks.visible=true;cracks.render(pose,vertices,15728880,overlay,1,phase==3?.35F:.78F,phase==3?.6F:1,alpha);
    }
}
