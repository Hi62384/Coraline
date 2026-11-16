package com.coraline.client.renderer;

import com.coraline.parents.ParentEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Small articulated accumulations, never a white replacement skin or new actor. */
public final class ParentReunionLayer extends RenderLayer<ParentEntity,PlayerModel<ParentEntity>> {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/white_concrete.png");
    private record Profile(double hairTop,double shoulderTop,double sleeveFront,double clothingFront){}
    private static final java.util.Map<String,Profile> PROFILES=new java.util.HashMap<>();
    public ParentReunionLayer(RenderLayerParent<ParentEntity,PlayerModel<ParentEntity>> parent){super(parent);}
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,ParentEntity parent,float limb,float amount,float partial,float age,float yaw,float pitch){
        float snow=parent.reunionFrost();if(parent.isInvisible()||parent.isOtherParent()||snow<=.001F)return;
        renderFrost(getParentModel(),pose,buffers,light,snow,(parent.isMom()?"mom_":"dad_")+parent.skin());
    }
    public static void renderFrost(PlayerModel<?> model,PoseStack pose,MultiBufferSource buffers,int light,float snow){
        renderFrost(model,pose,buffers,light,snow,new Profile(-.515,-.135,-.132,-.132));
    }
    public static void renderFrost(PlayerModel<?> model,PoseStack pose,MultiBufferSource buffers,int light,float snow,String wardrobe){renderFrost(model,pose,buffers,light,snow,PROFILES.computeIfAbsent(wardrobe,ParentReunionLayer::profile));}
    private static Profile profile(String id){
        double hair=-8,shoulder=-2,sleeve=-2,body=-2;
        try(var stream=ParentReunionLayer.class.getResourceAsStream("/assets/coraline/wardrobe/parents/"+id+".json");var reader=new java.io.InputStreamReader(java.util.Objects.requireNonNull(stream),java.nio.charset.StandardCharsets.UTF_8)){
            for(var element:com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("boxes")){var box=element.getAsJsonObject();String part=box.get("part").getAsString();var a=box.getAsJsonArray("box");var r=box.getAsJsonArray("rotation");var transform=new org.joml.Matrix4f().translation(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()).rotateZYX(r.get(2).getAsFloat(),r.get(1).getAsFloat(),r.get(0).getAsFloat());for(int corner=0;corner<8;corner++){var point=transform.transformPosition(new org.joml.Vector3f((corner&1)==0?0:a.get(3).getAsFloat(),(corner&2)==0?0:a.get(4).getAsFloat(),(corner&4)==0?0:a.get(5).getAsFloat()));if(part.equals("head"))hair=Math.min(hair,point.y);if(part.endsWith("arm")){shoulder=Math.min(shoulder,point.y);sleeve=Math.min(sleeve,point.z);}if(part.equals("body"))body=Math.min(body,point.z);}}
        }catch(java.io.IOException failure){throw new IllegalStateException("Cannot align reunion snow to existing wardrobe "+id,failure);}return new Profile(hair/16-.008,shoulder/16-.008,sleeve/16-.008,body/16-.008);
    }
    private static void renderFrost(PlayerModel<?> model,PoseStack pose,MultiBufferSource buffers,int light,float snow,Profile profile){
        float remaining=Math.max(0,Math.min(1,snow));if(remaining<=.001F)return;double area=Math.sqrt(remaining),thickness=.008+.033*remaining;
        int tint=remaining>.65F?0xEDF5F7:remaining>.3F?0xD9E5E9:0xBFCFD5;
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        for(var arm:new ModelPart[]{model.rightArm,model.leftArm}){pose.pushPose();arm.translateAndRotate(pose);
            patch(pose,out,light,-.03,profile.shoulderTop,0,.28*area,thickness,.29*area,tint);
            patch(pose,out,light,-.035,.13,profile.sleeveFront,.16*area,.15*remaining,.012,tint);
            patch(pose,out,light,.01,.43,profile.sleeveFront,.12*area,.09*remaining,.012,tint);pose.popPose();
        }
        pose.pushPose();model.body.translateAndRotate(pose);
        for(int side:new int[]{-1,1}){patch(pose,out,light,side*.13,.025,profile.clothingFront,.12*area,.07*remaining,.014,tint);patch(pose,out,light,side*.16,.58,profile.clothingFront,.10*area,.07*remaining,.014,tint);}
        patch(pose,out,light,.025,.43,profile.clothingFront,.18*area,.06*remaining,.014,tint);pose.popPose();
        pose.pushPose();model.head.translateAndRotate(pose);
        patch(pose,out,light,-.12,profile.hairTop,.035,.18*area,thickness*.65,.22*area,tint);patch(pose,out,light,.14,profile.hairTop+.002,.11,.13*area,thickness*.5,.16*area,tint);pose.popPose();
        // Damp narrow folds remain briefly while the white patches thin/shrink.
        double damp=Math.sin(Math.PI*(1-remaining))*.035;if(damp>.001){out=buffers.getBuffer(RenderType.entityTranslucent(WHITE));pose.pushPose();model.body.translateAndRotate(pose);patch(pose,out,light,-.13,.12,profile.clothingFront-.002,damp,.22,.007,0x899DA5);patch(pose,out,light,.12,.45,profile.clothingFront-.002,damp,.17,.007,0x899DA5);pose.popPose();}
    }
    private static void patch(PoseStack pose,VertexConsumer out,int light,double x,double y,double z,double width,double height,double depth,int color){CinematicPropRenderer.box(pose,out,light,x,y,z,width,height,depth,color);}
}
