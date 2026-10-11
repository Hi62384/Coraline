package com.coraline.client;
import com.coraline.client.renderer.DollVisual;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** Development-only render regression artifact, produced with Minecraft's real skin renderer. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class DollRenderCheck {
    private static boolean done;
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event){
        if(Boolean.getBoolean("coraline.scoldingAudioCheck")||Boolean.getBoolean("coraline.secretsAudioCheck")||Boolean.getBoolean("coraline.secretsVisualCheck")||Boolean.getBoolean("coraline.revisionVisualCheck")||Boolean.getBoolean("coraline.mallBattleHuntVisualCheck")||Boolean.getBoolean("coraline.expeditionVisualCheck")||Boolean.getBoolean("coraline.newActivitiesVisualCheck"))return;
        if(Boolean.getBoolean("coraline.storyPresentationCheck")||Boolean.getBoolean("coraline.feastVisualCheck")||Boolean.getBoolean("coraline.clothingVisualCheck"))return;
        if(done||Minecraft.getInstance().getOverlay()!=null)return;done=true;
        Minecraft mc=Minecraft.getInstance();TextureTarget target=new TextureTarget(512,512,true,Minecraft.ON_OSX);
        Matrix4f projection=new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting=RenderSystem.getVertexSorting();
        try{
            var shadow=new org.joml.Vector3f(.22F,.22F,.22F);var moon=new org.joml.Vector3f(shadow);var lamp=new org.joml.Vector3f(shadow);
            OtherWorldLighting.apply(shadow,0,0);OtherWorldLighting.apply(moon,0,15);OtherWorldLighting.apply(lamp,15,0);
            if(Math.abs(shadow.x-.22F)>.001||moon.z<=moon.x||moon.x<=shadow.x||lamp.x<=lamp.z)throw new IllegalStateException("Moonlight/lamplight contrast regression");
            target.bindWrite(true);target.setClearColor(.24F,.28F,.3F,1);target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,512,512,0,-1000,1000),VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.getModelViewStack().pushPose();RenderSystem.getModelViewStack().setIdentity();RenderSystem.applyModelViewMatrix();
            RenderSystem.enableDepthTest();Lighting.setupForFlatItems();
            var buffers=MultiBufferSource.immediate(new BufferBuilder(256));PoseStack pose=new PoseStack();
            pose.translate(256,175,0);pose.scale(155,155,-155);pose.mulPose(Axis.XP.rotationDegrees(-30));pose.mulPose(Axis.YP.rotationDegrees(20));
            new DollVisual().renderSkin(ResourceLocation.fromNamespaceAndPath("coraline","textures/entity/parents/mom_0.png"),false,pose,buffers,15728880,true);
            buffers.endBatch();
            try(var img=Screenshot.takeScreenshot(target)){
                img.writeToFile(java.nio.file.Path.of("../artwork/doll_native_render.png"));
            }
            System.out.println("CORALINE_RENDER_CHECK: saved artwork/doll_native_render.png");
            target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            Lighting.setupFor3DItems();
            for(int index=0;index<2;index++){
                PoseStack blockPose=new PoseStack();
                blockPose.translate(index==0?130:365,235,0);
                blockPose.scale(150,-150,150);
                blockPose.mulPose(Axis.XP.rotationDegrees(20));
                blockPose.mulPose(Axis.YP.rotationDegrees(155));
                blockPose.translate(-.5,-.5,-.5);
                var state=com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState()
                    .setValue(com.coraline.block.TinyDoorBlock.OPEN,index==1);
                mc.getBlockRenderer().renderSingleBlock(state,blockPose,buffers,15728880,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            }
            PoseStack itemPose=new PoseStack();itemPose.translate(256,405,0);itemPose.scale(100,-100,100);
            mc.getItemRenderer().renderStatic(com.coraline.registry.ModItems.TINY_DOOR.get().getDefaultInstance(),
                net.minecraft.world.item.ItemDisplayContext.GUI,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                itemPose,buffers,null,0);
            buffers.endBatch();
            try(var img=Screenshot.takeScreenshot(target)){img.writeToFile(java.nio.file.Path.of("../artwork/tiny_door_native_render.png"));}
            System.out.println("CORALINE_RENDER_CHECK: saved artwork/tiny_door_native_render.png");
            target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            PoseStack bedPose=new PoseStack();bedPose.translate(256,260,0);bedPose.scale(155,-155,155);
            bedPose.mulPose(Axis.XP.rotationDegrees(28));bedPose.mulPose(Axis.YP.rotationDegrees(155));bedPose.translate(-1,-.35,-1);
            for(int part=0;part<4;part++){
                bedPose.pushPose();bedPose.translate(part%2,0,part/2);
                var state=com.coraline.registry.ModBlocks.PARENTS_BED.get().defaultBlockState()
                    .setValue(com.coraline.parents.ParentsBedBlock.PART,part).setValue(com.coraline.parents.ParentsBedBlock.AVAILABLE,true);
                mc.getBlockRenderer().renderSingleBlock(state,bedPose,buffers,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                bedPose.popPose();
            }
            PoseStack bedItemPose=new PoseStack();bedItemPose.translate(256,435,0);bedItemPose.scale(100,-100,100);
            mc.getItemRenderer().renderStatic(com.coraline.registry.ModItems.PARENTS_BED.get().getDefaultInstance(),
                net.minecraft.world.item.ItemDisplayContext.GUI,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                bedItemPose,buffers,null,0);
            buffers.endBatch();
            try(var img=Screenshot.takeScreenshot(target)){img.writeToFile(java.nio.file.Path.of("../artwork/parents_bed_native_render.png"));}
            System.out.println("CORALINE_RENDER_CHECK: saved artwork/parents_bed_native_render.png");
            target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            PoseStack portalPose=new PoseStack();portalPose.translate(256,260,0);portalPose.scale(340,-340,340);portalPose.mulPose(Axis.YP.rotationDegrees(170));portalPose.translate(-.5,-.5,-.5);
            var portalState=com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(com.coraline.block.TinyDoorBlock.OPEN,true).setValue(com.coraline.block.TinyDoorBlock.PORTAL,true);
            mc.getBlockRenderer().renderSingleBlock(portalState,portalPose,buffers,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            var portalBlock=new com.coraline.otherworld.DoorBlockEntity(net.minecraft.core.BlockPos.ZERO,portalState);
            mc.getBlockEntityRenderDispatcher().getRenderer(portalBlock).render(portalBlock,0,portalPose,buffers,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            buffers.endBatch();
            try(var img=Screenshot.takeScreenshot(target)){img.writeToFile(java.nio.file.Path.of("../artwork/portal_native_render.png"));}
            System.out.println("CORALINE_RENDER_CHECK: saved artwork/portal_native_render.png");
            target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().perspective((float)Math.toRadians(75),1,.05F,1000),VertexSorting.DISTANCE_TO_ORIGIN);
            PoseStack tunnelView=new PoseStack();tunnelView.mulPose(Axis.YP.rotationDegrees(180));tunnelView.translate(-.5,-1.5,-.2);
            com.coraline.client.renderer.TunnelRenderer.renderFabric(tunnelView,buffers,0);buffers.endBatch();
            try(var img=Screenshot.takeScreenshot(target)){img.writeToFile(java.nio.file.Path.of("../artwork/tunnel_v2_native_render.png"));}
            target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            PoseStack doorwayView=new PoseStack();doorwayView.translate(-.5,-1.5,-.5);
            com.coraline.client.renderer.TunnelRenderer.renderFabric(doorwayView,buffers,0);
            doorwayView.pushPose();doorwayView.translate(0,1,-1);
            var returnDoor=com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(com.coraline.block.TinyDoorBlock.PORTAL,true).setValue(com.coraline.block.TinyDoorBlock.FACING,net.minecraft.core.Direction.SOUTH);
            mc.getBlockRenderer().renderSingleBlock(returnDoor,doorwayView,buffers,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            doorwayView.popPose();buffers.endBatch();
            try(var img=Screenshot.takeScreenshot(target)){img.writeToFile(java.nio.file.Path.of("../artwork/tunnel_return_door_native_render.png"));}
            target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            PoseStack skyView=new PoseStack();skyView.mulPose(Axis.XP.rotationDegrees(-90));skyView.mulPose(Axis.ZP.rotationDegrees(25));skyView.mulPose(Axis.XP.rotationDegrees(-48));
            com.coraline.client.OtherWorldSky.render(skyView,0);
            try(var img=Screenshot.takeScreenshot(target)){img.writeToFile(java.nio.file.Path.of("../artwork/other_sky_native_render.png"));}
            target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            PoseStack horizonView=new PoseStack();horizonView.mulPose(Axis.XP.rotationDegrees(8));
            OtherWorldSky.render(horizonView,1200);
            try(var img=Screenshot.takeScreenshot(target)){img.writeToFile(java.nio.file.Path.of("../artwork/other_horizon_native_render.png"));}
            System.out.println("CORALINE_RENDER_CHECK: saved tunnel and button moon sky previews");
        }catch(Exception e){throw new IllegalStateException("Doll render check failed",e);}
        finally{
            RenderSystem.getModelViewStack().popPose();RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection,sorting);target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);
        }
        if(!Boolean.getBoolean("coraline.worldVisualCheck")&&!Boolean.getBoolean("coraline.villageVisualCheck")&&!Boolean.getBoolean("coraline.archeryVisualCheck")&&!Boolean.getBoolean("coraline.cakeVisualCheck")&&!Boolean.getBoolean("coraline.carnivalVisualCheck")&&!Boolean.getBoolean("coraline.dreamParkourVisualCheck")&&!Boolean.getBoolean("coraline.fishingVisualCheck")&&!Boolean.getBoolean("coraline.gardenMazeVisualCheck")&&!Boolean.getBoolean("coraline.grandFeastVisualCheck")&&!Boolean.getBoolean("coraline.debugVisitCheck"))mc.stop();
    }
}
