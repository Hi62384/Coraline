package com.coraline.wybie;
import com.coraline.feast.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
/** Explicit opt-in; runs only in the isolated WybieVisitQA development world. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class WybieVisitVisualCheck {
    private static boolean started;private static volatile boolean ready,finished;private static int ticks;
    private static final Set<Integer> seen=new HashSet<>(),captured=new HashSet<>();
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("coraline.wybieVisitVisualCheck")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.pauseOnLostFocus=false;if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(!started){started=true;mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var level=p.server.overworld();var center=new BlockPos(100,100,100);
            for(int x=-17;x<=17;x++)for(int z=-17;z<=17;z++){level.setBlock(center.offset(x,-1,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);for(int y=0;y<5;y++)level.setBlock(center.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
            level.setDayTime(1000);level.setWeatherParameters(100000,0,false,false);level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,p.server);
            var bed=center.offset(2,0,1);level.setBlock(bed,Blocks.RED_BED.defaultBlockState(),2);level.setBlock(bed.north(),Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD),2);
            p.setRespawnPosition(Level.OVERWORLD,bed,0,false,false);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.teleportTo(level,100.5,100,100.5,0,0);
            var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.table=s.piano=center;s.stage=VisitData.Stage.DONE;s.bobinskyVisited=true;s.teaVisited=true;VisitData.get(p.server).scenes.put(s.owner,s);VisitData.get(p.server).setDirty();ready=true;
        });}
        if(!ready)return;ticks++;mc.player.setDeltaMovement(Vec3.ZERO);
        if(Boolean.getBoolean("coraline.wybieVisitPoseOnly")){if(ticks==120){capturePoses(mc);captureMouthAndParcel(mc);mc.stop();}return;}
        var actors=mc.level.getEntitiesOfClass(WybieVisitorEntity.class,mc.player.getBoundingBox().inflate(64));
        if(!actors.isEmpty()){
            var actor=actors.get(0);int phase=actor.phase();seen.add(phase);
            if(!actor.biking()){
                var delta=actor.getEyePosition().subtract(mc.player.getEyePosition());float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance()));mc.player.setYRot(yaw);mc.player.setXRot(pitch);
            }
            if(actor.elapsed()>=21&&captured.add(phase))capture(mc,"phase_"+phase);
        }
        if(ticks>2400)throw new IllegalStateException("Wybie live visit did not finish; phases="+seen);
        if(ticks%20==0&&!finished)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=VisitData.get(p.server).scenes.get(p.getUUID());
            if(s!=null&&s.wybieVisited&&s.wybieActor==null){
                if(!p.getInventory().contains(new net.minecraft.world.item.ItemStack(com.coraline.registry.ModItems.CAMERA.get())))throw new IllegalStateException("Camera delivery failed");
                if(WybieVisit.eligible(s,true,true,true))throw new IllegalStateException("Second visit remained eligible");
                com.mojang.logging.LogUtils.getLogger().info("WYBIE_LIVE_VISIT_OK: camera delivered, actor removed, one-time flag kept");finished=true;
            }
        });
        if(finished){if(!seen.containsAll(List.of(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14)))throw new IllegalStateException("Skipped dialogue phase: "+seen);capture(mc,"finished");mc.stop();}
    }
    private static void capture(Minecraft mc,String name){try{var out=java.nio.file.Path.of("../artwork/wybie_visit_qa");java.nio.file.Files.createDirectories(out);try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(out.resolve(name+".png"));}}catch(Exception e){throw new RuntimeException(e);}}
    private static void captureMouthAndParcel(Minecraft mc){
        var target=new com.mojang.blaze3d.pipeline.TextureTarget(560,700,true,Minecraft.ON_OSX);
        var projection=new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix());var sorting=com.mojang.blaze3d.systems.RenderSystem.getVertexSorting();
        var modelView=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();modelView.pushPose();
        try{
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,560,700,0,-1000,1000),com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            modelView.setIdentity();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            var actor=com.coraline.registry.ModEntities.WYBIE_VISITOR.get().create(mc.level);actor.setOnGround(true);var state=new net.minecraft.nbt.CompoundTag();state.putInt("VisitPhase",WybieVisitorEntity.A);state.putInt("VisitTime",20);actor.readAdditionalSaveData(state);
            var model=new com.coraline.client.renderer.CharacterModel("wybie");model.setupAnim(actor,0,0,120,0,0);
            for(float speech:new float[]{0,.35F,1}){
                target.setClearColor(.28F,.32F,.36F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
                var buffers=net.minecraft.client.renderer.MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.BufferBuilder(256));var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.translate(280,620,0);pose.scale(780,780,-780);
                model.renderSpeech(pose,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline","characters/cast_atlas.png"))),15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,1,1,1,1,speech);buffers.endBatch();
                try(var image=net.minecraft.client.Screenshot.takeScreenshot(target)){image.writeToFile(java.nio.file.Path.of("../artwork/wybie_visit_qa/mouth_"+(int)(speech*100)+".png"));}
            }
            actor.discard();
            var parcel=com.coraline.registry.ModBlocks.CHEESE_PARCEL.get().defaultBlockState();var baked=mc.getBlockRenderer().getBlockModelShaper().getBlockModel(parcel);
            var directions=new java.util.ArrayList<net.minecraft.core.Direction>(java.util.List.of(net.minecraft.core.Direction.values()));directions.add(null);
            for(var direction:directions)for(var quad:baked.getQuads(parcel,direction,net.minecraft.util.RandomSource.create(0)))if(quad.getSprite().contents().name().getPath().equals("missingno"))throw new IllegalStateException("Missing cheese parcel face texture");
            target.setClearColor(.28F,.32F,.36F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            var buffers=net.minecraft.client.renderer.MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.BufferBuilder(256));var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.translate(280,420,0);pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(25));pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-30));pose.scale(340,-340,340);pose.translate(-.5,0,-.5);
            mc.getBlockRenderer().renderSingleBlock(parcel,pose,buffers,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);buffers.endBatch();
            try(var image=net.minecraft.client.Screenshot.takeScreenshot(target)){image.writeToFile(java.nio.file.Path.of("../artwork/wybie_visit_qa/parcel_texture.png"));}
            com.mojang.logging.LogUtils.getLogger().info("WYBIE_MOUTH_AND_PARCEL_RENDER_OK: closed, soft and loud mouth samples; all parcel face sprites resolved");
        }catch(Exception failure){throw new RuntimeException(failure);}
        finally{modelView.popPose();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection,sorting);target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);}
    }
    public static void capturePoses(Minecraft mc){
        var target=new com.mojang.blaze3d.pipeline.TextureTarget(560,700,true,Minecraft.ON_OSX);
        var projection=new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix());var sorting=com.mojang.blaze3d.systems.RenderSystem.getVertexSorting();
        var modelView=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();modelView.pushPose();
        try{
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,560,700,0,-1000,1000),com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            modelView.setIdentity();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();com.mojang.blaze3d.platform.Lighting.setupFor3DItems();com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            var e=com.coraline.registry.ModEntities.WYBIE_VISITOR.get().create(mc.level);e.tickCount=120;e.setOnGround(true);
            var dispatcher=mc.getEntityRenderDispatcher();dispatcher.setRenderShadow(false);
            for(int phase:new int[]{2,4,5,7,8,9,10,13,14}){
                var n=new net.minecraft.nbt.CompoundTag();n.putInt("VisitPhase",phase);n.putInt("VisitTime",28);e.readAdditionalSaveData(n);
                target.setClearColor(.28F,.32F,.36F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
                var buffers=net.minecraft.client.renderer.MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.BufferBuilder(256));var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.translate(280,615,0);pose.scale(220,-220,220);pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-25));
                dispatcher.render(e,0,0,0,0,0,pose,buffers,15728880);buffers.endBatch();
                try(var image=net.minecraft.client.Screenshot.takeScreenshot(target)){image.writeToFile(java.nio.file.Path.of("../artwork/wybie_visit_qa/pose_"+phase+".png"));}
            }
            e.discard();
            for(String id:new String[]{"bobinsky","miss_spink","miss_forcible"}){
                var actor=id.equals("bobinsky")?com.coraline.registry.ModEntities.BOBINSKY_VISITOR.get().create(mc.level):id.equals("miss_spink")?com.coraline.registry.ModEntities.SPINK_VISITOR.get().create(mc.level):com.coraline.registry.ModEntities.FORCIBLE_VISITOR.get().create(mc.level);actor.tickCount=120;actor.setOnGround(true);
                if(actor instanceof com.coraline.bobinsky.BobinskyVisitorEntity b)b.pose(com.coraline.bobinsky.BobinskyVisit.B,28);
                if(actor instanceof com.coraline.tea.TeaVisitorEntity t){t.pose(com.coraline.tea.TeaVisit.SWIRL,28);t.cup(true);}
                target.setClearColor(.28F,.32F,.36F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);var buffers=net.minecraft.client.renderer.MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.BufferBuilder(256));var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.translate(280,615,0);pose.scale(220,-220,220);pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-25));dispatcher.render(actor,0,0,0,0,0,pose,buffers,15728880);buffers.endBatch();try(var image=net.minecraft.client.Screenshot.takeScreenshot(target)){image.writeToFile(java.nio.file.Path.of("../artwork/wybie_visit_qa/held_"+id+".png"));}actor.discard();
            }
            dispatcher.setRenderShadow(true);com.mojang.logging.LogUtils.getLogger().info("WYBIE_NATIVE_POSES_OK: nine Wybie poses plus Bobinsky parcel and two-handed tea props rendered with actual entity renderers");
        }catch(Exception failure){throw new RuntimeException(failure);}
        finally{modelView.popPose();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection,sorting);target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);}
    }
}
