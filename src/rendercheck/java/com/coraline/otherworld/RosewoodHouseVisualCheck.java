package com.coraline.otherworld;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Opt-in native screenshots in an isolated development world, never packaged. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class RosewoodHouseVisualCheck {
    private static boolean started,modelsChecked;private static volatile boolean ready;private static int ticks;
    private static final BlockPos CENTER=new BlockPos(900,80,900);
    // Blueprint camera/target coordinates; the same compact mapping as story anchors.
    private static final int[][] VIEWS={{34,15,-10,34,12,12},{46,9,55,44,7,65},{29,10,54,20,7,61},{28,10,36,15,8,46},{34,9,49,33,9,56},{47,19,29,41,16,23},{21,19,60,20,16,66},{34,28,34,32,26,20},{34,26,60,33,34,49},{45,2,18,30,0,35},{48,2,62,43,0,66},{4,26,63,9,25,65},{75,18,-14,34,9,35},{30,8,-4,26,2,6},{29,10,4,21,12,9},{0,24,48,7,20,59},{61,9,33,51,8,33},{83,45,-134,11,10,-44},{34,10,6,23,10,-24},{38,11,-13,28,11,-39},{-54,25,-59,-14,7,-49},{25,13,27,25,16,30}};
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("coraline.rosewoodHouseVisualCheck")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60)return;
        mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;if(mc.screen!=null)mc.setScreen(null);
        if(!started){started=true;mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var level=p.server.overworld();
            // A fresh rolling hillside, rather than the flat world used by older
            // captures, makes the terrain transition and foundation visible.
            for(int x=-100;x<=70;x++)for(int z=-175;z<=80;z++){
                int top=76+(int)Math.floor(x/16.0+z/18.0+Math.sin(x*.13));
                for(int y=60;y<=top;y++)level.setBlock(new BlockPos(CENTER.getX()+x,y,CENTER.getZ()+z),(y==top?net.minecraft.world.level.block.Blocks.GRASS_BLOCK:y>=top-2?net.minecraft.world.level.block.Blocks.DIRT:net.minecraft.world.level.block.Blocks.STONE).defaultBlockState(),2);
                for(int y=top+1;y<=125;y++)if(!level.isEmptyBlock(new BlockPos(CENTER.getX()+x,y,CENTER.getZ()+z)))level.setBlock(new BlockPos(CENTER.getX()+x,y,CENTER.getZ()+z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
            }
            var realm=new RealmData.Realm();realm.door=java.util.UUID.randomUUID();realm.ready=true;realm.house=CENTER;
            RosewoodHouse.build(level,CENTER,new DoorData.Door(),realm);
            RosewoodGarden.build(level,level,CENTER,realm);
            RealmData.get(p.server).realms.put(realm.door,realm);
            var scene=new com.coraline.feast.VisitData.Scene();scene.owner=p.getUUID();scene.door=realm.door;scene.table=realm.table;scene.piano=realm.piano;scene.stage=com.coraline.feast.VisitData.Stage.DONE;scene.loop.visits=1;scene.loop.secrets.visit=1;scene.wybieVisited=scene.bobinskyVisited=scene.teaVisited=true;
            com.coraline.feast.VisitData.get(p.server).scenes.put(p.getUUID(),scene);
            com.coraline.secrets.HouseCabinets.prepare(p);
            level.setDayTime(1000);level.setWeatherParameters(100000,0,false,false);
            p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,10000,0,false,false));
            ready=true;
        });}
        if(!ready)return;
        if(!modelsChecked){checkModels(mc);captureDining(mc);captureStorage(mc);modelsChecked=true;if(Boolean.getBoolean("coraline.storageOnlyVisualCheck")){mc.stop();return;}}
        ticks++;int index=(ticks-1)/100;if(index>=VIEWS.length){com.mojang.logging.LogUtils.getLogger().info("ROSEWOOD_NATIVE_SCREENSHOTS_OK: twenty-two house/garden/cabinet/access viewpoints captured");mc.stop();return;}
        var v=VIEWS[index];var camera=RosewoodHouse.at(CENTER,v[0],v[1],v[2]);var target=RosewoodHouse.at(CENTER,v[3],v[4],v[5]);
        var position=Vec3.atCenterOf(camera);var delta=Vec3.atCenterOf(target).subtract(position.add(0,mc.player.getEyeHeight(),0));
        float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance()));
        mc.player.setPos(position);mc.player.setDeltaMovement(Vec3.ZERO);mc.player.setYRot(yaw);mc.player.setXRot(pitch);
        if(ticks%100==1)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.teleportTo(p.server.overworld(),position.x,position.y,position.z,yaw,pitch);});
        if(ticks%100==90)try{var out=java.nio.file.Path.of("../artwork/rosewood_repair_qa");java.nio.file.Files.createDirectories(out);try(var screenshot=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){screenshot.writeToFile(out.resolve("view_"+index+".png"));}}catch(Exception e){throw new RuntimeException(e);}
    }
    private static void checkModels(Minecraft mc){
        for(var block:net.minecraft.core.registries.BuiltInRegistries.BLOCK){
            var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            if(!id.getNamespace().equals("coraline")||!id.getPath().startsWith("rosewood_"))continue;
            for(var state:block.getStateDefinition().getPossibleStates()){
                var model=mc.getBlockRenderer().getBlockModelShaper().getBlockModel(state);
                var faces=new java.util.ArrayList<net.minecraft.core.Direction>(java.util.List.of(net.minecraft.core.Direction.values()));faces.add(null);
                double minX=10,maxX=-10,minZ=10,maxZ=-10;
                for(var face:faces)for(var quad:model.getQuads(state,face,net.minecraft.util.RandomSource.create(0))){
                    if(quad.getSprite().contents().name().getPath().equals("missingno"))throw new IllegalStateException("Missing furniture texture: "+state);
                    var vertices=quad.getVertices();int stride=vertices.length/4;
                    for(int i=0;i<4;i++){double x=Float.intBitsToFloat(vertices[i*stride]),z=Float.intBitsToFloat(vertices[i*stride+2]);minX=Math.min(minX,x);maxX=Math.max(maxX,x);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);}
                }
                if(block instanceof RosewoodGlassDoorBlock){
                    var collision=state.getCollisionShape(mc.level,mc.player.blockPosition()).bounds();
                    double error=collision.getXsize()<.3?Math.abs((minX+maxX)/2-(collision.minX+collision.maxX)/2):Math.abs((minZ+maxZ)/2-(collision.minZ+collision.maxZ)/2);
                    if(error>.08)throw new IllegalStateException("Glass-door render/collision mismatch "+state+" error="+error);
                }
            }
        }
        com.mojang.logging.LogUtils.getLogger().info("ROSEWOOD_NATIVE_MODELS_OK: all furniture sprites and glass-door collision orientations checked");
    }
    private static void captureDining(Minecraft mc){
        var target=new com.mojang.blaze3d.pipeline.TextureTarget(1100,700,true,Minecraft.ON_OSX);
        var projection=new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix());var sorting=com.mojang.blaze3d.systems.RenderSystem.getVertexSorting();var view=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();view.pushPose();
        try{
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,1100,700,0,-1000,1000),com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);view.setIdentity();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            target.setClearColor(.28F,.32F,.36F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            var buffers=net.minecraft.client.renderer.MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.BufferBuilder(256));var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.translate(550,390,0);pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(28));pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-35));pose.scale(68,-68,68);
            var dispatcher=mc.getEntityRenderDispatcher();dispatcher.setRenderShadow(false);var table=com.coraline.registry.ModEntities.SCENE_PROP.get().create(mc.level);table.kind(com.coraline.feast.ScenePropEntity.TABLE);table.longDining(true);dispatcher.render(table,0,0,0,0,0,pose,buffers,15728880);
            for(int slot=0;slot<9;slot++){
                var chair=com.coraline.registry.ModEntities.SCENE_PROP.get().create(mc.level);chair.kind(com.coraline.feast.ScenePropEntity.CHAIR);float yaw=slot==0?0:slot<3?180:slot%2==1?-90:90;chair.setYRot(yaw);
                double x=slot==0?0:slot==1?-.9:slot==2?.9:slot%2==1?-2.25:2.25,z=slot<3?(slot==0?-5.05:5.05):((slot-3)/2-1)*2.6;dispatcher.render(chair,x,0,z,yaw,0,pose,buffers,15728880);chair.discard();
            }
            buffers.endBatch();var out=java.nio.file.Path.of("../artwork/rosewood_repair_qa");java.nio.file.Files.createDirectories(out);try(var shot=net.minecraft.client.Screenshot.takeScreenshot(target)){shot.writeToFile(out.resolve("dining.png"));}table.discard();dispatcher.setRenderShadow(true);
            com.mojang.logging.LogUtils.getLogger().info("ROSEWOOD_DINING_RENDER_OK: long table and nine attached-back chairs rendered with native entity renderer");
        }catch(Exception e){throw new RuntimeException(e);}finally{view.popPose();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection,sorting);target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);}
    }
    private static void captureStorage(Minecraft mc){
        var target=new com.mojang.blaze3d.pipeline.TextureTarget(1200,760,true,Minecraft.ON_OSX);
        var projection=new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix());var sorting=com.mojang.blaze3d.systems.RenderSystem.getVertexSorting();var view=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();view.pushPose();
        try{
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,1200,760,0,-1000,1000),com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);view.setIdentity();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();com.mojang.blaze3d.platform.Lighting.setupFor3DItems();target.setClearColor(.16F,.18F,.2F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            var buffers=net.minecraft.client.renderer.MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.BufferBuilder(256));
            for(int i=0;i<4;i++){
                var block=(i<2?com.coraline.registry.ModBlocks.ROSEWOOD_DRAWER:com.coraline.registry.ModBlocks.ROSEWOOD_CABINET).get();var state=block.defaultBlockState().setValue(com.coraline.secrets.HouseStorageBlock.FACING,net.minecraft.core.Direction.SOUTH).setValue(com.coraline.secrets.HouseStorageBlock.OPEN,i%2==1);
                var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.translate(160+i%2*600,300+i/2*350,0);pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-28));pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-28));pose.scale(200,-200,200);
                mc.getBlockRenderer().renderSingleBlock(state,pose,buffers,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                if(i%2==1){
                    var items=new net.minecraft.world.item.ItemStack[]{new net.minecraft.world.item.ItemStack(com.coraline.secrets.SecretsItems.KEY.get()),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD)};
                    for(int slot=0;slot<2;slot++){pose.pushPose();pose.translate(.3+slot*.38,i<2?.515:.25,.82);if(i<2)pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));pose.scale(.22F,.22F,.22F);mc.getItemRenderer().renderStatic(items[slot],net.minecraft.world.item.ItemDisplayContext.FIXED,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,pose,buffers,mc.level,0);pose.popPose();}
                }
            }
            buffers.endBatch();var out=java.nio.file.Path.of("../artwork/rosewood_repair_qa");java.nio.file.Files.createDirectories(out);try(var shot=net.minecraft.client.Screenshot.takeScreenshot(target)){shot.writeToFile(out.resolve("storage.png"));}
            com.mojang.logging.LogUtils.getLogger().info("ROSEWOOD_STORAGE_RENDER_OK: native closed/open drawers and cabinets, contents and unique key icon");
            target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            var actor=com.coraline.registry.ModEntities.BOBINSKY_VISITOR.get().create(mc.level);actor.setOnGround(true);var state=new net.minecraft.nbt.CompoundTag();state.putInt("VisitPhase",3);actor.readAdditionalSaveData(state);
            var model=new com.coraline.client.renderer.CharacterModel("bobinsky");model.setupAnim(actor,0,0,120,0,0);
            for(int i=0;i<3;i++){var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.translate(200+i*400,930,0);pose.scale(800,800,-800);model.renderSpeech(pose,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline","characters/cast_atlas.png"))),15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,1,1,1,1,i/2F);}
            buffers.endBatch();try(var shot=net.minecraft.client.Screenshot.takeScreenshot(target)){shot.writeToFile(out.resolve("bobinsky_mouth.png"));}actor.discard();
            com.mojang.logging.LogUtils.getLogger().info("BOBINSKY_MOUTH_RENDER_OK: native quiet, half-open and loud speech samples");
        }catch(Exception e){throw new RuntimeException(e);}finally{view.popPose();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection,sorting);target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);}
    }
}
