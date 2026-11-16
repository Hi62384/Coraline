package com.coraline.client.renderer;

import com.coraline.postgame.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Code-authored brass apparatus, Moonstone chamber and three moving rings;
 * reuses the existing neutral fabric atlas, without another large texture. */
public final class BreachDriveRenderer implements BlockEntityRenderer<BreachDriveBlockEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("coraline","textures/models/sculpted/fabric.png");
    private final ModelPart base,crystal,a,b,c;
    public BreachDriveRenderer(BlockEntityRendererProvider.Context context){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("base",CubeListBuilder.create().texOffs(0,0).addBox(-7,14,-7,14,2,14).addBox(-5,12,-5,10,2,10)
            .addBox(-6,7,-6,1,7,1).addBox(5,7,-6,1,7,1).addBox(-6,7,5,1,7,1).addBox(5,7,5,1,7,1),PartPose.ZERO);
        root.addOrReplaceChild("crystal",CubeListBuilder.create().texOffs(0,0).addBox(-2.5F,-2.5F,-2.5F,5,5,5).addBox(-1.5F,-4,-1.5F,3,8,3),PartPose.offsetAndRotation(0,7,0,0,.7854F,0));
        for(String name:new String[]{"a","b","c"}){
            var cubes=CubeListBuilder.create().texOffs(0,0);
            for(int i=0;i<24;i++){double angle=i*Math.PI/12;cubes.addBox((float)Math.cos(angle)*6-.5F,-.4F,(float)Math.sin(angle)*6-.5F,1,.8F,1);}
            root.addOrReplaceChild(name,cubes,PartPose.offset(0,7,0));
        }
        var baked=LayerDefinition.create(mesh,64,64).bakeRoot();base=baked.getChild("base");crystal=baked.getChild("crystal");a=baked.getChild("a");b=baked.getChild("b");c=baked.getChild("c");
    }
    @Override public void render(BreachDriveBlockEntity device,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        boolean powered=device.getBlockState().getValue(BreachDriveBlock.POWERED);float angle=powered?(device.getLevel().getGameTime()+partial)*.025F:0;
        a.xRot=.6F;a.yRot=angle;b.zRot=1.05F;b.yRot=-angle*.7F;c.xRot=1.05F;c.zRot=angle*.5F;
        pose.pushPose();pose.translate(.5,1,.5);pose.scale(1,-1,-1);var consumer=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        base.render(pose,consumer,light,OverlayTexture.NO_OVERLAY,.24F,.21F,.19F,1);
        for(var ring:new ModelPart[]{a,b,c})ring.render(pose,consumer,light,OverlayTexture.NO_OVERLAY,.68F,.5F,.25F,1);
        crystal.render(pose,consumer,powered?0xf000f0:light,OverlayTexture.NO_OVERLAY,powered?.4F:.28F,powered?.82F:.35F,powered?1:.42F,1);pose.popPose();
    }
}
