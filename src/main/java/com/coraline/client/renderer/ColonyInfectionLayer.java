package com.coraline.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.coraline.client.ColonyMobVisuals;
import com.coraline.mixin.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Button geometry follows each existing native head bone and cube bounds.
 * Native textures, wool, coats, equipment and the actual entity stay intact. */
public final class ColonyInfectionLayer<T extends LivingEntity,M extends EntityModel<T>> extends RenderLayer<T,M> {
    private static final ResourceLocation FABRIC=ResourceLocation.fromNamespaceAndPath("coraline","textures/models/sculpted/fabric.png");
    private final ModelPart button,holes;
    public ColonyInfectionLayer(RenderLayerParent<T,M> parent){super(parent);var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("button",CubeListBuilder.create().texOffs(0,0).addBox(-1.2F,-1.2F,-.25F,2.4F,2.4F,.5F),PartPose.ZERO);var dots=CubeListBuilder.create().texOffs(0,0);for(float x:new float[]{-.65F,.25F})for(float y:new float[]{-.65F,.25F})dots.addBox(x,y,-.32F,.4F,.4F,.12F);mesh.getRoot().addOrReplaceChild("holes",dots,PartPose.ZERO);var root=LayerDefinition.create(mesh,64,64).bakeRoot();button=root.getChild("button");holes=root.getChild("holes");}
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,T entity,float walk,float amount,float partial,float age,float yaw,float pitch){if(entity.isInvisible()||entity.isBaby()||!ColonyMobVisuals.infected(entity))return;getParentModel().renderToBuffer(pose,buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity))),light,OverlayTexture.NO_OVERLAY,.42F,.78F,.59F,.22F);if(getParentModel() instanceof InfectionHeadPartsAccess heads)for(var head:heads.coraline$infectionHeads())eyes(head,pose,buffers,light,0);}
    private boolean eyes(ModelPart part,PoseStack pose,MultiBufferSource buffers,int light,int depth){if(depth>4||!part.visible)return false;pose.pushPose();part.translateAndRotate(pose);var access=(InfectionModelPartAccess)(Object)part;ModelPart.Cube face=null;float area=0;for(var cube:access.coraline$infectionCubes()){float next=(cube.maxX-cube.minX)*(cube.maxY-cube.minY);if(next>area){area=next;face=cube;}}if(face!=null&&area>=16){float width=face.maxX-face.minX,cx=(face.minX+face.maxX)/2,cy=face.minY+(face.maxY-face.minY)*.42F;var sink=buffers.getBuffer(RenderType.entityCutoutNoCull(FABRIC));for(int side:new int[]{-1,1}){pose.pushPose();pose.translate((cx+side*width*.24F)/16,cy/16,face.minZ/16-.018);button.render(pose,sink,light,OverlayTexture.NO_OVERLAY,.025F,.025F,.04F,1);holes.render(pose,sink,light,OverlayTexture.NO_OVERLAY,.72F,.8F,.75F,1);pose.popPose();}pose.popPose();return true;}for(var child:access.coraline$infectionChildren().values())if(eyes(child,pose,buffers,light,depth+1)){pose.popPose();return true;}pose.popPose();return false;}
}
