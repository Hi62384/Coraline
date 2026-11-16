package com.coraline.client.renderer;

import com.coraline.feast.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Two wheels, spokes, diamond frame, fork, pedals, seat and handlebars, all true model geometry. */
public final class WybieBicycleRenderer extends EntityRenderer<WybieBicycleEntity> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("coraline","wybie_bicycle"),"main");
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/wybie_bicycle_model.png");
    private final ModelPart model;
    public WybieBicycleRenderer(EntityRendererProvider.Context context){super(context);model=context.bakeLayer(LAYER);shadowRadius=.7F;}
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        wheel(root,"front_wheel",-11);wheel(root,"rear_wheel",11);
        bar(root,"chainstay",0,8,3,0,8,11,1.25F,0);bar(root,"seatstay",0,16,4,0,8,11,1.25F,0);
        bar(root,"seatpost",0,8,3,0,17,4,1.4F,0);bar(root,"top_tube",0,16,4,0,15,-8,1.45F,0);bar(root,"down_tube",0,8,3,0,15,-8,1.45F,0);
        bar(root,"fork_left",-1.3F,8,-11,-1.3F,17,-7,1,16);bar(root,"fork_right",1.3F,8,-11,1.3F,17,-7,1,16);
        root.addOrReplaceChild("seat",CubeListBuilder.create().texOffs(32,0).addBox(-3.3F,-1,-2.4F,6.6F,2,4.8F),PartPose.offset(0,17,4.5F));
        bar(root,"stem",0,15,-8,0,21,-6,1.2F,16);bar(root,"handlebar",-5,21,-6,5,21,-6,1.2F,16);
        root.addOrReplaceChild("left_grip",CubeListBuilder.create().texOffs(32,0).addBox(-2,-.85F,-.85F,3,1.7F,1.7F),PartPose.offset(-4.8F,21,-6));
        root.addOrReplaceChild("right_grip",CubeListBuilder.create().texOffs(32,0).addBox(-1,-.85F,-.85F,3,1.7F,1.7F),PartPose.offset(4.8F,21,-6));
        var crank=root.addOrReplaceChild("crank",CubeListBuilder.create().texOffs(16,0).addBox(-2,-.7F,-.7F,4,1.4F,1.4F),PartPose.offset(0,8,3));
        crank.addOrReplaceChild("arm_left",CubeListBuilder.create().texOffs(16,0).addBox(-.5F,-3,-.5F,1,3,1),PartPose.offset(-2,0,0));
        crank.addOrReplaceChild("arm_right",CubeListBuilder.create().texOffs(16,0).addBox(-.5F,0,-.5F,1,3,1),PartPose.offset(2,0,0));
        crank.addOrReplaceChild("pedal_left",CubeListBuilder.create().texOffs(32,0).addBox(-2,-.5F,-1.4F,3,1,2.8F),PartPose.offset(-2,-3,0));
        crank.addOrReplaceChild("pedal_right",CubeListBuilder.create().texOffs(32,0).addBox(-1,-.5F,-1.4F,3,1,2.8F),PartPose.offset(2,3,0));
        return LayerDefinition.create(mesh,64,32);
    }
    private static void wheel(PartDefinition root,String name,float z){
        var wheel=root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(16,0).addBox(-2,-.7F,-.7F,4,1.4F,1.4F),PartPose.offset(0,6,z));
        for(int i=0;i<16;i++){float angle=i*Mth.TWO_PI/16;wheel.addOrReplaceChild("tire"+i,CubeListBuilder.create().texOffs(32,0).addBox(-.9F,-1.35F,-.8F,1.8F,2.7F,1.6F),PartPose.offsetAndRotation(0,Mth.cos(angle)*5.15F,Mth.sin(angle)*5.15F,angle-Mth.HALF_PI,0,0));}
        for(int i=0;i<8;i++){float angle=i*Mth.TWO_PI/8;wheel.addOrReplaceChild("spoke"+i,CubeListBuilder.create().texOffs(16,0).addBox(-.25F,0,-.25F,.5F,5,.5F),PartPose.rotation(angle,0,0));}
    }
    private static void bar(PartDefinition parent,String name,float x,float y,float z,float ex,float ey,float ez,float width,int u){
        float dx=ex-x,dy=ey-y,dz=ez-z,length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);float rz=-(float)Math.atan2(dx,Math.sqrt(dy*dy+dz*dz)),rx=(float)Math.atan2(dz,dy);
        parent.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,0).addBox(-width/2,0,-width/2,width,length,width),PartPose.offsetAndRotation(x,y,z,rx,0,rz));
    }
    @Override public void render(WybieBicycleEntity bike,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(180-yaw));
        // Geometry uses positive y upwards and front -z; no model-origin lift is necessary.
        float wheel=Mth.lerp(partial,bike.previousWheelRotation,bike.wheelRotation);model.getChild("front_wheel").xRot=-wheel;model.getChild("rear_wheel").xRot=-wheel;model.getChild("crank").xRot=-wheel*.5F;model.getChild("front_wheel").yRot=bike.visualSteering()*.28F;model.getChild("handlebar").yRot=bike.visualSteering()*.28F;
        float r=bike.friend()?.77F:1F,g=bike.friend()?1F:.77F,b=bike.friend()?.86F:1F;model.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,OverlayTexture.NO_OVERLAY,r,g,b,1);pose.popPose();super.render(bike,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(WybieBicycleEntity e){return TEXTURE;}
    @Mod.EventBusSubscriber(modid="coraline",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void layer(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(LAYER,WybieBicycleRenderer::layer);}
        @SubscribeEvent public static void renderer(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(WybieAdventureEntities.BICYCLE.get(),WybieBicycleRenderer::new);}
    }
}
