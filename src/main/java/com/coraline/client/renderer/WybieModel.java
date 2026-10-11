package com.coraline.client.renderer;
import com.coraline.entity.WybieEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public final class WybieModel extends PlayerModel<WybieEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("coraline", "wybie"), "main");
    private final ModelPart bike, frontWheel, rearWheel, leftShin, rightShin;
    public WybieModel(ModelPart root) {
        super(root, false);
        bike = root.getChild("bike");
        frontWheel = bike.getChild("front_wheel"); rearWheel = bike.getChild("rear_wheel");
        leftShin=root.getChild("left_shin"); rightShin=root.getChild("right_shin");
    }
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        CubeListBuilder curls=CubeListBuilder.create();
        for(int x=-4;x<=2;x+=3) for(int z=-4;z<=2;z+=3)
            curls.texOffs(0,0).addBox(x,-9.5F-((x+z)%2==0?0.5F:0),z,3,2,3);
        mesh.getRoot().getChild("head").addOrReplaceChild("curls",curls,PartPose.ZERO);
        mesh.getRoot().addOrReplaceChild("right_shin",CubeListBuilder.create().texOffs(0,18).addBox(-2,0,-2,4,10,4),PartPose.offsetAndRotation(-1.9F,10.8F,-4.7F,0.5F,0,0));
        mesh.getRoot().addOrReplaceChild("left_shin",CubeListBuilder.create().texOffs(16,50).addBox(-2,0,-2,4,10,4),PartPose.offsetAndRotation(1.9F,10.8F,-4.7F,0.5F,0,0));
        PartDefinition bike = mesh.getRoot().addOrReplaceChild("bike", CubeListBuilder.create(), PartPose.ZERO);
        bar(bike,"top_tube",0,11,-1,0,1.5F,1.5F,14,1);
        bar(bike,"seat_post",0,12,5,-0.3F,1.5F,10,1.5F,1);
        bar(bike,"down_tube",0,15,-1,-0.75F,1.5F,1.5F,17,1);
        bar(bike,"chain_stay",0,20,7,0,1.5F,1.5F,12,1);
        bar(bike,"rear_stay",0,15,9,0.8F,1.5F,13,1.5F,1);
        for(int side : new int[]{-1,1}) bar(bike,"fork"+side,side*1.8F,13,-10,-0.15F,1,15,1,1);
        bar(bike,"handlebar",0,5,-9,0,13,1.5F,1.5F,1);
        bar(bike,"saddle",0,8,4,0,6,1.5F,6,0);
        bar(bike,"crank",0,19,1,0,8,1,1,1);
        for(int side : new int[]{-1,1}) bar(bike,"pedal"+side,side*5,19,1,0,3,1,3,0);
        wheel(bike,"front_wheel",-11); wheel(bike,"rear_wheel",13);
        return LayerDefinition.create(mesh,64,64);
    }
    private static void wheel(PartDefinition bike,String name,float z) {
        PartDefinition wheel = bike.addOrReplaceChild(name,CubeListBuilder.create(),PartPose.offset(0,18,z));
        for(int i=0;i<16;i++) {
            float angle=(float)(i*Math.PI/8);
            bar(wheel,"tire"+i,0,(float)Math.cos(angle)*5,(float)Math.sin(angle)*5,angle,2,1.4F,2.2F,0);
        }
        for(int i=0;i<6;i++) bar(wheel,"spoke"+i,0,0,0,(float)(i*Math.PI/6),0.5F,10,0.5F,1);
        bar(wheel,"hub",0,0,0,0,4,1.5F,1.5F,1);
    }
    private static void bar(PartDefinition parent,String name,float x,float y,float z,float rx,float w,float h,float d,int color) {
        parent.addOrReplaceChild(name,CubeListBuilder.create().texOffs(color==0?8:136,520)
                .addBox(-w/2,-h/2,-d/2,w,h,d,CubeDeformation.NONE,16,16),PartPose.offsetAndRotation(x,y,z,rx,0,0));
    }
    @Override public void setupAnim(WybieEntity entity,float swing,float amount,float age,float yaw,float pitch) {
        super.setupAnim(entity,swing,amount,age,yaw,pitch);
        head.y=hat.y=-5; body.y=jacket.y=-5;
        leftArm.y=rightArm.y=leftSleeve.y=rightSleeve.y=-3;
        leftLeg.y=rightLeg.y=leftPants.y=rightPants.y=7;
        leftArm.xRot=rightArm.xRot=-0.9F;
        leftArm.zRot=0.1F; rightArm.zRot=-0.1F;
        leftLeg.xRot=-1.0F+(float)Math.sin(swing)*amount*0.3F;
        rightLeg.xRot=-1.0F-(float)Math.sin(swing)*amount*0.3F;
        leftSleeve.copyFrom(leftArm); rightSleeve.copyFrom(rightArm);
        leftPants.copyFrom(leftLeg); rightPants.copyFrom(rightLeg);
        leftLeg.yScale=rightLeg.yScale=leftPants.yScale=rightPants.yScale=0.5F;
        leftShin.y=7+6*(float)Math.cos(leftLeg.xRot); leftShin.z=6*(float)Math.sin(leftLeg.xRot);
        rightShin.y=7+6*(float)Math.cos(rightLeg.xRot); rightShin.z=6*(float)Math.sin(rightLeg.xRot);
        frontWheel.xRot=rearWheel.xRot=swing;
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer vertices,int light,int overlay,float r,float g,float b,float a) {
        super.renderToBuffer(pose,vertices,light,overlay,r,g,b,a);
        bike.render(pose,vertices,light,overlay,r,g,b,a);
        leftShin.render(pose,vertices,light,overlay,r,g,b,a); rightShin.render(pose,vertices,light,overlay,r,g,b,a);
    }
}
