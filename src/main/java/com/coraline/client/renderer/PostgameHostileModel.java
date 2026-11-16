package com.coraline.client.renderer;

import java.util.*;
import com.coraline.postgame.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Twelve custom voxel silhouettes. Existing neutral fabric is reused, tinted
 * per part; no vanilla zombie model/skin or new high-resolution atlas. */
public final class PostgameHostileModel extends EntityModel<OtherWorldHostile> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("coraline","postgame_hostiles"),"main");
    private record Parts(ModelPart head,ModelPart eyes,ModelPart stitches,ModelPart body,ModelPart leftArm,ModelPart rightArm,ModelPart leftLeg,ModelPart rightLeg){}
    private final ModelPart root;
    private boolean concealedVillager;
    private final Map<PostgameHostiles.Kind,Parts> parts=new EnumMap<>(PostgameHostiles.Kind.class);
    private PostgameHostiles.Kind current=PostgameHostiles.Kind.BUTTON_EYED_WANDERER;
    public PostgameHostileModel(ModelPart root){
        this.root=root;
        for(var kind:PostgameHostiles.Kind.values()){
            var group=root.getChild(kind.id());var head=group.getChild("head");
            parts.put(kind,new Parts(head,head.getChild("eyes"),head.getChild("stitches"),group.getChild("body"),group.getChild("left_arm"),group.getChild("right_arm"),group.getChild("left_leg"),group.getChild("right_leg")));
        }
    }
    private static CubeListBuilder cube(float x,float y,float z,float w,float h,float d){return CubeListBuilder.create().texOffs(0,0).addBox(x,y,z,w,h,d);}
    private static void limb(PartDefinition group,String name,float x,float y,float width,float height){group.addOrReplaceChild(name,cube(-width/2,0,-width/2,width,height,width),PartPose.offset(x,y,0));}
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        for(var kind:PostgameHostiles.Kind.values()){
            var group=root.addOrReplaceChild(kind.id(),CubeListBuilder.create(),PartPose.ZERO);
            if(kind==PostgameHostiles.Kind.PAPER_WRAITH){
                var head=group.addOrReplaceChild("head",cube(-3,-6,-2,6,7,3).addBox(-2,-7,-1,4,1,2),PartPose.offset(0,1,0));
                head.addOrReplaceChild("eyes",cube(-2.5F,-4,-2.4F,1.5F,2,.4F).addBox(1,-4,-2.4F,1.5F,2,.4F),PartPose.ZERO);
                head.addOrReplaceChild("stitches",cube(-1,-1.5F,-2.5F,2,.4F,.2F),PartPose.ZERO);
                group.addOrReplaceChild("body",cube(-3,0,-1.5F,6,12,3).addBox(-4,4,-1,1,13,2).addBox(3,6,-1,1,11,2),PartPose.offset(0,2,0));
                group.addOrReplaceChild("left_arm",cube(0,0,-.5F,9,8,1).addBox(7,4,-.5F,3,7,1),PartPose.offset(3,4,0));
                group.addOrReplaceChild("right_arm",cube(-9,0,-.5F,9,8,1).addBox(-10,4,-.5F,3,7,1),PartPose.offset(-3,4,0));
                limb(group,"left_leg",1.5F,14,1,8);limb(group,"right_leg",-1.5F,14,1,6);
            }else if(kind==PostgameHostiles.Kind.ROOTBOUND_HORROR){
                var head=group.addOrReplaceChild("head",cube(-4,-8,-3.5F,8,9,7).addBox(-5,-13,-1,2,6,2).addBox(3,-15,-1,2,8,2).addBox(-7,-13,-1,4,1,2).addBox(3,-14,-1,5,1,2),PartPose.offset(0,-1,0));
                head.addOrReplaceChild("eyes",cube(-3,-5,-4,2,2,.7F).addBox(1,-5,-4,2,2,.7F),PartPose.ZERO);
                head.addOrReplaceChild("stitches",cube(-.3F,-7,-4.3F,.6F,7,.3F),PartPose.ZERO);
                group.addOrReplaceChild("body",cube(-5,0,-4,10,15,8).addBox(-6,2,-4,1,10,8).addBox(5,4,-4,1,9,8),PartPose.offset(0,0,0));
                limb(group,"left_arm",7,1,3,17);limb(group,"right_arm",-7,1,3,17);limb(group,"left_leg",3,14,3,10);limb(group,"right_leg",-3,14,3,10);
            }else if(kind==PostgameHostiles.Kind.VOID_STALKER){
                var head=group.addOrReplaceChild("head",cube(-4.5F,-8,-4,9,9,8).addBox(-5.5F,-9,-3,11,2,7),PartPose.offset(0,-3,0));
                head.addOrReplaceChild("eyes",cube(-3,-5,-4.4F,2,1,.6F).addBox(1,-5,-4.4F,2,1,.6F),PartPose.ZERO);
                head.addOrReplaceChild("stitches",cube(-.4F,-7,-4.7F,.8F,8,.2F),PartPose.ZERO);
                group.addOrReplaceChild("body",cube(-3.5F,0,-2.5F,7,13,5).addBox(-4,8,2,8,10,1).addBox(-5,0,-2,10,2,5),PartPose.offset(0,-2,0));
                limb(group,"left_arm",5,-1,2.5F,17);limb(group,"right_arm",-5,-1,2.5F,17);limb(group,"left_leg",2,11,2.5F,13);limb(group,"right_leg",-2,11,2.5F,13);
            }else if(kind==PostgameHostiles.Kind.FALSE_VILLAGER){
                var head=group.addOrReplaceChild("head",cube(-4,-8,-4,8,9,8),PartPose.offset(0,0,0));
                head.addOrReplaceChild("nose",cube(-1,-2,-6,2,4,2),PartPose.ZERO);
                head.addOrReplaceChild("fangs",cube(-2,0,-4.7F,1,2,1).addBox(1,0,-4.7F,1,2,1),PartPose.ZERO);
                head.addOrReplaceChild("eyes",cube(-3,-4,-4.4F,2,2,.6F).addBox(1,-4,-4.4F,2,2,.6F),PartPose.ZERO);
                head.addOrReplaceChild("stitches",cube(-2.3F,-3.3F,-4.9F,.6F,.6F,.2F).addBox(1.7F,-3.3F,-4.9F,.6F,.6F,.2F),PartPose.ZERO);
                group.addOrReplaceChild("body",cube(-4,0,-3,8,13,6).addBox(-4.5F,9,-3.5F,9,6,7),PartPose.ZERO);
                limb(group,"left_arm",5,2,3,11);limb(group,"right_arm",-5,2,3,11);limb(group,"left_leg",2,13,3,11);limb(group,"right_leg",-2,13,3,11);
            }else if(kind==PostgameHostiles.Kind.THREAD_SPINNER){
                var head=group.addOrReplaceChild("head",cube(-4,-5,-3,8,6,6).addBox(-2,0,-4,1,2,1).addBox(1,0,-4,1,2,1),PartPose.offset(0,4,0));
                head.addOrReplaceChild("eyes",cube(-3,-3,-3.4F,2,2,.6F).addBox(1,-3,-3.4F,2,2,.6F),PartPose.ZERO);
                head.addOrReplaceChild("stitches",cube(-2.3F,-2.3F,-3.9F,.6F,.6F,.2F).addBox(1.7F,-2.3F,-3.9F,.6F,.6F,.2F),PartPose.ZERO);
                var body=group.addOrReplaceChild("body",cube(-4,0,-3,8,10,8).addBox(-5,2,3,10,7,7),PartPose.offset(0,5,0));
                body.addOrReplaceChild("reel_left",cube(0,0,-.5F,6,1,1).addBox(5,0,-.5F,1,5,1),PartPose.offset(4,5,0));
                body.addOrReplaceChild("reel_right",cube(-6,0,-.5F,6,1,1).addBox(-6,0,-.5F,1,5,1),PartPose.offset(-4,5,0));
                limb(group,"left_arm",5,7,2,10);limb(group,"right_arm",-5,7,2,10);limb(group,"left_leg",3,15,2,9);limb(group,"right_leg",-3,15,2,9);
            }else if(kind==PostgameHostiles.Kind.RIFT_GUARDIAN){
                var head=group.addOrReplaceChild("head",cube(-6,-10,-5,12,10,10).addBox(-6,-13,-4,2,4,2).addBox(4,-13,-4,2,4,2),PartPose.offset(0,-12,0));
                head.addOrReplaceChild("eyes",cube(-4,-6,-5.4F,3,2,.6F).addBox(1,-6,-5.4F,3,2,.6F),PartPose.ZERO);
                head.addOrReplaceChild("stitches",cube(-.4F,-9,-5.8F,.8F,9,.2F),PartPose.ZERO);
                group.addOrReplaceChild("body",cube(-7,0,-4.5F,14,23,9).addBox(-8,1,-5,16,4,10).addBox(-6,12,-5,12,4,1),PartPose.offset(0,-13,0));
                limb(group,"left_arm",10,-10,6,27);limb(group,"right_arm",-10,-10,6,27);limb(group,"left_leg",4,10,6,14);limb(group,"right_leg",-4,10,6,14);
            }else if(kind==PostgameHostiles.Kind.STITCHED_HOUND){
                var head=group.addOrReplaceChild("head",cube(-4,-3,-5,8,7,7).addBox(-3,0,-9,6,4,5).addBox(-4,-5,-2,2,3,2).addBox(2,-5,-2,2,3,2),PartPose.offset(0,13,-8));
                head.addOrReplaceChild("eyes",cube(-3.7F,-.5F,-5.3F,2,2,.6F).addBox(1.7F,-.5F,-5.3F,2,2,.6F),PartPose.ZERO);
                head.addOrReplaceChild("stitches",cube(-2.5F,.2F,-5.7F,.6F,.6F,.2F).addBox(2,.2F,-5.7F,.6F,.6F,.2F),PartPose.ZERO);
                group.addOrReplaceChild("body",cube(-5,0,-7,10,7,16).addBox(-1,0,8,2,2,8),PartPose.offset(0,12,0));
                limb(group,"left_arm",3,18,2.5F,6);limb(group,"right_arm",-3,18,2.5F,6);limb(group,"left_leg",3,18,2.5F,6);limb(group,"right_leg",-3,18,2.5F,6);
                group.getChild("left_arm").addOrReplaceChild("paw",cube(-1.6F,4,-3,3.2F,2,4),PartPose.ZERO);group.getChild("right_arm").addOrReplaceChild("paw",cube(-1.6F,4,-3,3.2F,2,4),PartPose.ZERO);
            }else if(kind==PostgameHostiles.Kind.THREADCRAWLER){
                var head=group.addOrReplaceChild("head",cube(-3,-2,-4,6,4,5).addBox(-2.5F,1,-5,1,2,1).addBox(1.5F,1,-5,1,2,1),PartPose.offset(0,16,-6));
                head.addOrReplaceChild("eyes",cube(-2.8F,-1.5F,-4.4F,2,2,.6F).addBox(.8F,-1.5F,-4.4F,2,2,.6F),PartPose.ZERO);
                head.addOrReplaceChild("stitches",cube(-2,-.8F,-4.8F,.5F,.5F,.2F).addBox(1.5F,-.8F,-4.8F,.5F,.5F,.2F),PartPose.ZERO);
                var body=group.addOrReplaceChild("body",cube(-4,0,-5,8,4,13).addBox(-3,-1,0,6,1,6),PartPose.offset(0,16,0));
                body.addOrReplaceChild("middle_left",cube(0,0,-.6F,6,1.2F,1.2F).addBox(5,0,-.6F,1,5,1.2F),PartPose.offset(3,2,1));
                body.addOrReplaceChild("middle_right",cube(-6,0,-.6F,6,1.2F,1.2F).addBox(-6,0,-.6F,1,5,1.2F),PartPose.offset(-3,2,1));
                limb(group,"left_arm",6,18,1.2F,6);limb(group,"right_arm",-6,18,1.2F,6);limb(group,"left_leg",6,18,1.2F,6);limb(group,"right_leg",-6,18,1.2F,6);
            }else if(kind==PostgameHostiles.Kind.PORCELAIN_SENTINEL){
                var head=group.addOrReplaceChild("head",cube(-5.5F,-9,-4.5F,11,10,9).addBox(-6,-11,-4,12,2,8),PartPose.offset(0,-10,0));
                head.addOrReplaceChild("eyes",cube(-3.7F,-5,-5,2.5F,2,.7F).addBox(1.2F,-5,-5,2.5F,2,.7F),PartPose.ZERO);
                head.addOrReplaceChild("stitches",cube(-.3F,-8,-5.3F,.6F,8,.2F).addBox(-2.7F,-4.4F,-5.5F,.6F,.6F,.2F).addBox(2,-4.4F,-5.5F,.6F,.6F,.2F),PartPose.ZERO);
                group.addOrReplaceChild("body",cube(-6,0,-4,12,19,8).addBox(-7,0,-5,14,4,10).addBox(-6,12,-4.5F,12,2,9),PartPose.offset(0,-9,0));
                limb(group,"left_arm",9,-7,5,23);limb(group,"right_arm",-9,-7,5,23);limb(group,"left_leg",3.2F,10,5,14);limb(group,"right_leg",-3.2F,10,5,14);
            }else if(kind==PostgameHostiles.Kind.CLOCKWORK_WATCHER){
                var head=group.addOrReplaceChild("head",cube(-5,-6,-4,10,8,8).addBox(-.5F,-9,-.5F,1,3,1),PartPose.offset(0,6,0));
                head.addOrReplaceChild("eyes",cube(-3.5F,-3,-4.5F,2,3,1).addBox(1.5F,-3,-4.5F,2,3,1),PartPose.ZERO);
                head.addOrReplaceChild("stitches",cube(-4,-5,-4.6F,1,1,.5F).addBox(3,-5,-4.6F,1,1,.5F),PartPose.ZERO);
                group.addOrReplaceChild("body",cube(-4,0,-3,8,9,6).addBox(-3,2,-3.5F,6,4,1),PartPose.offset(0,8,0));
                limb(group,"left_arm",6,9,2,10);limb(group,"right_arm",-6,9,2,10);limb(group,"left_leg",2.5F,17,2,7);limb(group,"right_leg",-2.5F,17,2,7);
            }else{
                boolean soldier=kind==PostgameHostiles.Kind.RIFT_SOLDIER;
                var head=group.addOrReplaceChild("head",cube(-4,-7,-4,8,7,8),PartPose.offset(0,soldier?0:1,0));
                head.addOrReplaceChild("eyes",cube(-2.8F,-4,-4.5F,2,2,.75F).addBox(.8F,-4,-4.5F,2,2,.75F),PartPose.ZERO);
                head.addOrReplaceChild("stitches",cube(-2.2F,-3.45F,-4.95F,.7F,.7F,.2F).addBox(1.4F,-3.45F,-4.95F,.7F,.7F,.2F),PartPose.ZERO);
                group.addOrReplaceChild("body",cube(soldier?-4:-3,0,soldier?-2.5F:-2,soldier?8:6,11,soldier?5:4),PartPose.offset(0,0,0));
                limb(group,"left_arm",soldier?5.5F:4.5F,2,soldier?3:2.5F,11);limb(group,"right_arm",soldier?-5.5F:-4.5F,2,soldier?3:2.5F,11);
                limb(group,"left_leg",soldier?2:1.5F,11,soldier?3:2.5F,13);limb(group,"right_leg",soldier?-2:-1.5F,11,soldier?3:2.5F,13);
            }
        }
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(OtherWorldHostile entity,float swing,float amount,float age,float yaw,float pitch){
        current=entity.kind();concealedVillager=current==PostgameHostiles.Kind.FALSE_VILLAGER&&!entity.revealed();root.getAllParts().forEach(ModelPart::resetPose);var p=parts.get(current);
        p.head.yRot=yaw*Mth.DEG_TO_RAD;p.head.xRot=pitch*Mth.DEG_TO_RAD;
        float stride=Mth.cos(swing*.6662F)*amount*(current==PostgameHostiles.Kind.CLOCKWORK_WATCHER?.55F:1.15F);
        p.leftLeg.xRot=stride;p.rightLeg.xRot=-stride;p.leftArm.xRot=-stride*.7F;p.rightArm.xRot=stride*.7F;
        if(current==PostgameHostiles.Kind.STITCHED_HOUND||current==PostgameHostiles.Kind.THREADCRAWLER){p.leftArm.z=p.rightArm.z=-5;p.leftLeg.z=p.rightLeg.z=6;p.body.y+=(float)Math.sin(age*.16F)*.25F;}
        if(current==PostgameHostiles.Kind.THREADCRAWLER){p.body.getChild("middle_left").zRot=stride*.2F;p.body.getChild("middle_right").zRot=-stride*.2F;}
        if(entity.charging())p.leftArm.xRot=p.rightArm.xRot=-2.1F;
        if(current==PostgameHostiles.Kind.PAPER_WRAITH){p.leftArm.zRot=-.3F-(float)Math.sin(age*.13F)*.3F;p.rightArm.zRot=-p.leftArm.zRot;p.body.y+=(float)Math.sin(age*.08F);p.leftLeg.xRot=.2F;p.rightLeg.xRot=-.15F;}
        if(current==PostgameHostiles.Kind.FALSE_VILLAGER){p.head.getChild("nose").visible=!entity.revealed();p.head.getChild("fangs").visible=entity.revealed();if(!entity.revealed()){p.leftArm.xRot=p.rightArm.xRot=-1;p.leftArm.zRot=.7F;p.rightArm.zRot=-.7F;}}
        if(current==PostgameHostiles.Kind.THREAD_SPINNER){p.body.getChild("reel_left").yRot=(float)Math.sin(age*.14F)*.5F;p.body.getChild("reel_right").yRot=-(float)Math.sin(age*.14F)*.5F;}
        float strike=Mth.sin(attackTime*Mth.PI);p.rightArm.xRot-=strike*1.4F;
        if(current==PostgameHostiles.Kind.CLOCKWORK_WATCHER)p.leftArm.xRot-=strike*1.4F;
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,float red,float green,float blue,float alpha){
        var p=parts.get(current);boolean watcher=current==PostgameHostiles.Kind.CLOCKWORK_WATCHER,soldier=current==PostgameHostiles.Kind.RIFT_SOLDIER;
        float[] cloth=watcher?new float[]{.58F,.42F,.21F}:soldier?new float[]{.2F,.24F,.34F}:new float[]{.16F,.24F,.3F};
        float[] face=watcher?new float[]{.67F,.52F,.29F}:soldier?new float[]{.32F,.34F,.4F}:new float[]{.76F,.62F,.44F};
        if(current==PostgameHostiles.Kind.PORCELAIN_SENTINEL){cloth=new float[]{.76F,.78F,.8F};face=new float[]{.9F,.87F,.8F};}
        else if(current==PostgameHostiles.Kind.STITCHED_HOUND){cloth=new float[]{.27F,.15F,.14F};face=new float[]{.4F,.27F,.2F};}
        else if(current==PostgameHostiles.Kind.THREADCRAWLER){cloth=new float[]{.19F,.23F,.25F};face=new float[]{.28F,.3F,.32F};}
        else if(current==PostgameHostiles.Kind.PAPER_WRAITH){cloth=new float[]{.8F,.78F,.65F};face=new float[]{.94F,.9F,.8F};}
        else if(current==PostgameHostiles.Kind.ROOTBOUND_HORROR){cloth=new float[]{.25F,.3F,.13F};face=new float[]{.35F,.24F,.14F};}
        else if(current==PostgameHostiles.Kind.VOID_STALKER){cloth=new float[]{.08F,.06F,.15F};face=new float[]{.16F,.13F,.24F};}
        else if(current==PostgameHostiles.Kind.FALSE_VILLAGER){cloth=new float[]{.32F,.22F,.15F};face=new float[]{.65F,.5F,.38F};}
        else if(current==PostgameHostiles.Kind.THREAD_SPINNER){cloth=new float[]{.35F,.27F,.43F};face=new float[]{.59F,.55F,.65F};}
        else if(current==PostgameHostiles.Kind.RIFT_GUARDIAN){cloth=new float[]{.24F,.24F,.36F};face=new float[]{.65F,.66F,.72F};}
        p.body.render(pose,buffer,light,overlay,cloth[0]*red,cloth[1]*green,cloth[2]*blue,alpha);
        p.leftArm.render(pose,buffer,light,overlay,cloth[0]*red,cloth[1]*green,cloth[2]*blue,alpha);p.rightArm.render(pose,buffer,light,overlay,cloth[0]*red,cloth[1]*green,cloth[2]*blue,alpha);
        p.leftLeg.render(pose,buffer,light,overlay,.12F*red,.14F*green,.19F*blue,alpha);p.rightLeg.render(pose,buffer,light,overlay,.12F*red,.14F*green,.19F*blue,alpha);
        // The eyes inherit the head transform explicitly, so they cannot remain
        // floating at the old head position when the mob looks or attacks.
        p.eyes.visible=false;p.stitches.visible=false;p.head.render(pose,buffer,light,overlay,face[0]*red,face[1]*green,face[2]*blue,alpha);p.eyes.visible=true;p.stitches.visible=true;
        pose.pushPose();p.head.translateAndRotate(pose);
        p.eyes.render(pose,buffer,light,overlay,watcher?.1F:.035F,watcher?.65F:.035F,watcher?.68F:.05F,alpha);
        if(!concealedVillager)p.stitches.render(pose,buffer,light,overlay,.7F*red,.68F*green,.5F*blue,alpha);pose.popPose();
    }
}
