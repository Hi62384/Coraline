package com.coraline.client.renderer;

import com.coraline.postgame.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Stepped helical drill, physical frame and white fracture. The same native
 * active state stops the head, conduits, fracture light and powered effects. */
public final class RiftMachineRenderer implements BlockEntityRenderer<RiftMachineEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("coraline","textures/models/sculpted/fabric.png");
    private final ModelPart frame,head,fracture,conduit,coil;
    public RiftMachineRenderer(BlockEntityRendererProvider.Context context){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        var metal=CubeListBuilder.create().texOffs(0,0).addBox(-35,-106,-35,70,6,70).addBox(-3,-100,-3,6,56,6);
        for(int x:new int[]{-32,32})for(int z:new int[]{-32,32})metal.addBox(x-1,-100,z-1,2,100,2);
        root.addOrReplaceChild("frame",metal,PartPose.ZERO);var spin=root.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.ZERO);
        for(int i=0;i<10;i++){float r=2+i*1.65F;spin.addOrReplaceChild("blade_"+i,CubeListBuilder.create().texOffs(0,0).addBox(-r,-19-i*3.2F,-r,r*2,2,r*2),PartPose.rotation(0,i*.55F,0));}
        root.addOrReplaceChild("fracture",CubeListBuilder.create().texOffs(0,0).addBox(-23,-.7F,-23,46,.5F,46),PartPose.ZERO);
        root.addOrReplaceChild("conduit",CubeListBuilder.create().texOffs(0,0).addBox(-7,-2,-7,14,2,14).addBox(-5,-15,-5,10,13,10),PartPose.ZERO);
        root.addOrReplaceChild("coil",CubeListBuilder.create().texOffs(0,0).addBox(-6,-12,-6,12,1,12).addBox(-6,-8,-6,12,1,12).addBox(-6,-4,-6,12,1,12),PartPose.ZERO);
        var baked=LayerDefinition.create(mesh,128,128).bakeRoot();frame=baked.getChild("frame");head=baked.getChild("head");fracture=baked.getChild("fracture");conduit=baked.getChild("conduit");coil=baked.getChild("coil");
    }
    @Override public void render(RiftMachineEntity machine,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var state=machine.getBlockState();boolean core=state.is(com.coraline.registry.ModBlocks.RIFT_DRILL_CORE.get());
        if(!core&&!state.is(com.coraline.registry.ModBlocks.RIFT_DRILL_CONDUIT.get()))return;boolean active=state.getValue(RiftMachineBlock.ACTIVE);
        pose.pushPose();pose.translate(.5,0,.5);pose.scale(1,-1,-1);var consumer=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        if(core){
            frame.render(pose,consumer,light,OverlayTexture.NO_OVERLAY,.26F,.28F,.3F,1);head.yRot=active?(machine.getLevel().getGameTime()+partial)*.045F:0;
            head.render(pose,consumer,light,OverlayTexture.NO_OVERLAY,.62F,.65F,.68F,1);fracture.render(pose,consumer,active?0xf000f0:light,OverlayTexture.NO_OVERLAY,active?.95F:.22F,active?.98F:.24F,active?1:.28F,1);
        }else{
            conduit.render(pose,consumer,light,OverlayTexture.NO_OVERLAY,.22F,.24F,.27F,1);
            coil.render(pose,consumer,active?0xf000f0:light,OverlayTexture.NO_OVERLAY,active?.6F:.24F,active?.85F:.28F,active?1:.3F,1);
        }
        pose.popPose();
    }
}
