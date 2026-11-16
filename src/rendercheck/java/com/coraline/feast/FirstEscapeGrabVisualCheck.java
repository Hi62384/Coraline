package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Declared checkpoints with real mixer, free look, rescue landing and ordinary face interactions. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class FirstEscapeGrabVisualCheck {
    private static volatile Throwable failure;
    private static void server(Minecraft mc,Runnable task){mc.getSingleplayerServer().execute(()->{try{task.run();}catch(Throwable error){failure=error;}});}
    private static boolean started,refusalStaged,maskClicked,stitchClicked,viewSet;
    private static volatile boolean ready,captureShot,grabShot,prisonReady,rescueReady,teaReady;
    private static volatile Vec3 prisonPosition,hallPosition;
    private static volatile boolean bicycleFixtureReady,bicycleDepartureStarted;private static int canonicalWait;
    private static int ticks,gripFrames,stage,age,guideFrames;private static UUID helperId,bicycleWybie;private static boolean observedMounted,guideMounted,wardrobeClicked,doorClicked;private static volatile boolean guideArrived,passageReady;private static Vec3 guideStart;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("coraline.firstEscapeGrabCheck")||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(failure!=null)throw new IllegalStateException("First Escape connected QA failed",failure);
        if(mc.player==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60)return;
        mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);if(mc.screen!=null)mc.setScreen(null);
        // Camera changes below are declared fixture input; do not mix in the
        // user's hardware mouse while the disposable client runs in background.
        mc.mouseHandler.releaseMouse();
        if(!started){started=true;mc.options.fov().set(70);server(mc,()->setup(mc));return;}if(!ready)return;
        if(++ticks>7000)throw new IllegalStateException("Follow-up native checkpoint timed out");
        if(captureShot&&!refusalStaged){refusalStaged=true;shot(mc,"chair_first_person");checkpoint(mc,"COCOA_F");}
        if(grabShot&&mc.player.getVehicle() instanceof CinematicPropEntity carrier&&carrier.kind()==CinematicPropEntity.GRAB_CARRIER){
            if(++gripFrames==3){mc.player.setYRot(90);mc.player.setXRot(8);}
            if(gripFrames==25){if(Math.abs(mc.player.getXRot()-8)>1||Math.abs(net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot()-90))>1)throw new IllegalStateException("Drag camera overrides actual client free look");shot(mc,"grab_free_look");}
        }
        if(stage==0&&prisonReady&&prisonPosition!=null&&mc.player.distanceToSqr(prisonPosition)<100&&mc.player.getVehicle()==null&&++age>=60){
            shot(mc,"mirror_prison_arrival");stage=1;age=0;
            server(mc,()->{var p=owner(mc);var s=OtherMotherLoop.scene(p);FirstEscapeStory.debug(p,s,"phase_mirror_d","");var c=FirstEscapeScene.prison(s);p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()-2.5,0,0);p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,Vec3.atBottomCenterOf(c.south(2)).add(0,1.1,0));});
        }else if(stage==1&&com.coraline.client.FirstEscapeClient.phase().equals("MIRROR_D")&&++age>=100){
            shot(mc,"prison_sad_emissive_ghosts");stage=2;age=0;checkpoint(mc,"ESCAPE_A");
        }else if(stage==2&&rescueReady&&hallPosition!=null&&mc.player.distanceToSqr(hallPosition)<36&&mc.player.getVehicle()==null&&++age>=40){
            shot(mc,"masked_hall_rescue");stage=3;age=0;checkpoint(mc,"MASK_REMOVAL");
        }else if(stage==3){
            if(!maskClicked&&com.coraline.client.FirstEscapeClient.phase().equals("MASK_REMOVAL")){maskClicked=true;headClick(mc);}
            if(com.coraline.client.FirstEscapeClient.flag("MaskRemoved")){
                if(com.coraline.client.FirstEscapeClient.knockedDown()||com.coraline.client.FirstEscapeClient.inheritedDarkHouse())throw new IllegalStateException("Mask click failed to stand owner upright or rescue retained dark-house lighting");
                if(!viewSet){viewSet=true;mc.options.fov().set(35);faceHelper(mc);}
                if(++age>=70){shot(mc,"wybie_stitched_smile");stage=4;age=0;checkpoint(mc,"STITCH_REMOVAL");}
            }
        }else if(stage==4){
            if(!stitchClicked&&com.coraline.client.FirstEscapeClient.phase().equals("STITCH_REMOVAL")){stitchClicked=true;headClick(mc);}
            if(com.coraline.client.FirstEscapeClient.flag("StitchesRemoved")&&++age>=40){
                shot(mc,"wybie_after_stitch_removal");
                stage=6;age=0;mc.options.fov().set(70);checkpoint(mc,"ESCAPE_C");
            }
        }else if(stage==6){
            if(mc.player.getVehicle() instanceof CinematicPropEntity carrier&&carrier.kind()==CinematicPropEntity.GRAB_CARRIER&&carrier.cue()==2){if(++guideFrames==3){mc.player.setYRot(160);mc.player.setXRot(8);}if(guideFrames==25){if(Math.abs(mc.player.getXRot()-8)>1||Math.abs(net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot()-160))>1)throw new IllegalStateException("Wybie guide overrides player free look");shot(mc,"wybie_running_hand_grip");}}
            if(guideArrived&&++age>=30){shot(mc,"wybie_inside_wardrobe_room");stage=7;age=0;}
        }else if(stage==7){
            if(!wardrobeClicked&&com.coraline.client.FirstEscapeClient.phase().equals("PUSH_WARDROBE")){wardrobeClicked=true;for(var entity:mc.level.entitiesForRendering())if(entity instanceof CinematicPropEntity prop&&prop.kind()==CinematicPropEntity.ROACH_WARDROBE&&mc.player.distanceToSqr(prop)<25){mc.gameMode.interact(mc.player,prop,net.minecraft.world.InteractionHand.MAIN_HAND);break;}}
            if(!doorClicked&&com.coraline.client.FirstEscapeClient.phase().equals("OPEN_LITTLE_DOOR")){doorClicked=true;shot(mc,"fallen_wardrobe_closed_little_door");server(mc,()->{var p=owner(mc);var s=OtherMotherLoop.scene(p);var at=BlockPos.of(FirstEscapeStory.data(s).getLong("LittleDoor"));var block=p.serverLevel().getBlockState(at);if(!block.is(ModBlocks.TINY_DOOR.get())||block.getValue(com.coraline.block.TinyDoorBlock.OPEN))throw new IllegalStateException("Little Door already open behind wardrobe");mc.execute(()->mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(at.getCenter(),net.minecraft.core.Direction.NORTH,at,false)));});}
            if(passageReady&&++age>=30){shot(mc,"little_door_opened_passage");stage=8;age=0;server(mc,()->setupBicycle(mc));}
        }else if(stage==8&&bicycleFixtureReady&&mc.level.dimension()==net.minecraft.world.level.Level.OVERWORLD&&mc.level.hasChunkAt(new BlockPos(114000,100,114000))&&++age>=20){
            server(mc,()->{if(bicycleDepartureStarted)return;var p=owner(mc);var at=new BlockPos(114000,100,114000);if(!p.serverLevel().areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(at).toLong())||!p.serverLevel().areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(at.offset(8,0,-8)).toLong()))return;bicycleDepartureStarted=true;FirstEscapeStory.go(p,OtherMotherLoop.scene(p),FirstEscapeTimeline.Phase.BICYCLE_DEPARTURE);});
            if(bicycleDepartureStarted){stage=5;age=0;}
        }else if(stage==5&&teaReady&&++age>=80){
            shot(mc,"post_departure_tea_house");
            com.mojang.logging.LogUtils.getLogger().info("FIRST_ESCAPE_GRAB_NATIVE_COMPLETE: natural chair, explicit F/G mixer checkpoint, free-look grab/carry/throw, sad emissive prison ghosts, actual hallway rescue/default light/upright mask, normal mask/stitch removal, real running guide through authored doorway with free look, physical wardrobe interaction and closed-door-to-passage click, explicit bicycle checkpoint with natural original-actor retirement and missed-roll tea preparation; not a full story replay");mc.stop();return;
        }
        server(mc,()->{
            var p=owner(mc);var s=OtherMotherLoop.scene(p);var n=FirstEscapeStory.data(s);var phase=FirstEscapeStory.phase(s);
            if(!captureShot&&n.getBoolean("Captured")&&phase==FirstEscapeTimeline.Phase.COCOA_B){if(!(p.getVehicle() instanceof CinematicPropEntity chair&&chair.kind()==CinematicPropEntity.BUG_CHAIR))throw new IllegalStateException("Capture never seated owner");captureShot=true;}
            if(n.getBoolean("Grabbed")&&p.getVehicle() instanceof CinematicPropEntity carrier&&carrier.kind()==CinematicPropEntity.GRAB_CARRIER&&phase==FirstEscapeTimeline.Phase.COCOA_G)grabShot=true;
            if(phase==FirstEscapeTimeline.Phase.MIRROR_A&&n.getBoolean("Grabbed")){if(!p.serverLevel().noCollision(p,p.getBoundingBox()))throw new IllegalStateException("Mirror arrival in blocks");prisonPosition=Vec3.atBottomCenterOf(FirstEscapeScene.prison(s));prisonReady=true;}
            if(phase==FirstEscapeTimeline.Phase.MIRROR_D){int count=0;for(int i=0;i<3;i++){var ghost=FirstEscapeScene.prop(p,s,"Ghost"+i);if(ghost!=null&&ghost.value()>.5F)count++;}if(stage>=2&&count!=3)throw new IllegalStateException("Missing actual prison ghosts");}
            if(phase==FirstEscapeTimeline.Phase.ESCAPE_A&&n.getBoolean("Rescued")){
                var w=ShowActors.actor(p,s,"other_wybie");if(w==null)throw new IllegalStateException("Actual masked helper absent");
                if(helperId==null)helperId=w.getUUID();if(!helperId.equals(w.getUUID()))throw new IllegalStateException("Helper identity changed");
                var hall=FirstEscapeScene.mirror(s);if(Math.abs(p.getY()-hall.getY())>.13||p.distanceToSqr(hall.getCenter())>36||Math.abs(w.getY()-p.getY())>.13)throw new IllegalStateException("Rescue left the original mirror's hall floor: "+p.position()+" helper="+w.position()+" hall="+hall);
                hallPosition=p.position();rescueReady=true;
            }
            if(stage==6){var w=ShowActors.actor(p,s,"other_wybie");if(w!=null){if(guideStart==null)guideStart=w.position();if(p.getVehicle() instanceof CinematicPropEntity grip&&grip.kind()==CinematicPropEntity.GRAB_CARRIER&&grip.cue()==2)guideMounted=true;if(phase==FirstEscapeTimeline.Phase.PUSH_WARDROBE){if(!guideMounted||!helperId.equals(w.getUUID())||w.position().distanceToSqr(guideStart)<64||!FirstEscapeScene.inRoom(s,p.blockPosition())||!FirstEscapeScene.inRoom(s,w.blockPosition())||p.isPassenger())throw new IllegalStateException("Guide did not run/release original helper and owner inside actual room");guideArrived=true;}}}
            if(stage==7&&phase==FirstEscapeTimeline.Phase.ESCAPE_E&&n.getInt("Ticks")>0){var at=BlockPos.of(n.getLong("LittleDoor"));var block=p.serverLevel().getBlockState(at);if(!n.getBoolean("WardrobeFallen")||!n.getBoolean("DecayedTunnel")||!block.getValue(com.coraline.block.TinyDoorBlock.OPEN)||!block.getValue(com.coraline.block.TinyDoorBlock.PORTAL))throw new IllegalStateException("Door interaction advanced without actual formed passage");passageReady=true;}
            if(stage==5){
                var real=ShowActors.actor(p,s,"wybie");
                if(real!=null){if(bicycleWybie==null)bicycleWybie=real.getUUID();if(!bicycleWybie.equals(real.getUUID()))throw new IllegalStateException("Bicycle replaced original Wybie");if(real.getVehicle() instanceof WybieBicycleEntity)observedMounted=true;}
                if(n.getBoolean("Complete")){
                    var stored=CanonicalRealWybie.find(p,s);if(stored==null&&++canonicalWait<80)return;
                    if(!observedMounted||stored==null||!stored.getUUID().equals(bicycleWybie)||!CanonicalRealWybie.dormant(stored)||stored.isRemoved())throw new IllegalStateException("Departure failed to preserve the actual bicycle rider: observedMounted="+observedMounted+" expected="+bicycleWybie+" stored="+(stored==null?"unloaded":stored.getUUID()+" dormant="+CanonicalRealWybie.dormant(stored)+" removed="+stored.isRemoved())+" riddenTicks="+n.getInt("DepartureRiddenTicks"));
                    if(!MissingParentsTea.eligible(s))throw new IllegalStateException("Bicycle completion did not unlock tea aftermath");
                    var tea=MissingParentsTea.data(s);if(tea.getBoolean("Built")&&!teaReady){teaReady=true;var house=BlockPos.of(tea.getLong("House"));p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,house.getCenter().add(0,2,0));com.mojang.logging.LogUtils.getLogger().info("BICYCLE_TEA_NATIVE_OK original={} riddenTicks={} selected={} built={}",bicycleWybie,n.getInt("DepartureRiddenTicks"),tea.getBoolean("Selected"),tea.getBoolean("Built"));}
                }
            }
            if(ticks%100==0){com.mojang.logging.LogUtils.getLogger().info("FOLLOWUP_CHECK stage={} phase={} ticks={} grabbed={} rescued={} blocker={}",stage,phase,n.getInt("Ticks"),n.getBoolean("Grabbed"),n.getBoolean("Rescued"),n.getString("Blocker"));if(stage==6){var runner=ShowActors.actor(p,s,"other_wybie");var grip=FirstEscapeScene.prop(p,s,"GuideGrip");com.mojang.logging.LogUtils.getLogger().info("GUIDE_TRACE owner={} runner={} grip={} node={}/{} goal={} stall={} arrived={} ai={} gravity={}",p.position(),runner==null?null:runner.position(),grip==null?null:grip.position(),n.getInt("GuideNode"),n.getList("GuideRoute",4).size(),BlockPos.of(n.getLong("GuideGoal")),n.getInt("GuideStall"),n.getBoolean("GuideArrived"),runner==null?null:runner.isNoAi(),runner==null?null:runner.isNoGravity());if(phase==FirstEscapeTimeline.Phase.FOLLOW_WYBIE&&n.getInt("Ticks")>400)throw new IllegalStateException("Actual authored-house guide stalled; see GUIDE_TRACE");}}
        });
    }
    private static net.minecraft.server.level.ServerPlayer owner(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);}
    private static void checkpoint(Minecraft mc,String phase){server(mc,()->{var p=owner(mc);FirstEscapeStory.debug(p,OtherMotherLoop.scene(p),"phase_"+phase.toLowerCase(Locale.ROOT),"");});}
    private static void headClick(Minecraft mc){var w=mc.level.getEntity(com.coraline.client.FirstEscapeClient.wybie());if(w==null)throw new IllegalStateException("Client masked helper not synchronized");mc.gameMode.interactAt(mc.player,w,new net.minecraft.world.phys.EntityHitResult(w,w.getEyePosition()),net.minecraft.world.InteractionHand.MAIN_HAND);}
    private static void faceHelper(Minecraft mc){server(mc,()->{var p=owner(mc);var s=OtherMotherLoop.scene(p);var w=ShowActors.actor(p,s,"other_wybie");if(w!=null)p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,w.getEyePosition().add(0,-.06,0));});}
    private static void setupBicycle(Minecraft mc){
        var p=owner(mc);var s=OtherMotherLoop.scene(p);var l=p.server.overworld();var d=DoorData.get(p.server).doors.get(s.door);var at=new BlockPos(114000,100,114000);
        for(int x=-48;x<=48;x++)for(int z=-48;z<=48;z++)for(int y=-6;y<=-1;y++){var pos=at.offset(x,y,z);l.getChunkAt(pos);l.setBlock(pos,(y==-1?net.minecraft.world.level.block.Blocks.GRASS_BLOCK:net.minecraft.world.level.block.Blocks.DIRT).defaultBlockState(),net.minecraft.world.level.block.Block.UPDATE_CLIENTS|net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE|net.minecraft.world.level.block.Block.UPDATE_SUPPRESS_DROPS);}
        d.pos=at;d.dimension=l.dimension().location().toString();l.setBlock(at,com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState(),2);
        p.teleportTo(l,at.getX()+.5,at.getY(),at.getZ()+2.5,0,85);var n=FirstEscapeStory.data(s);n.putLong("SafePlayer",p.blockPosition().asLong());
        var tea=MissingParentsTea.data(s);tea.putBoolean("Rolled",true);tea.putBoolean("Selected",false);tea.putInt("SelectionDelay",600);
        // A cold, far-away QA destination is not a ready gameplay scene. Let
        // both the client and the entity sections arrive before starting the
        // real departure; retain every original rider/UUID assertion below.
        var chunk=new net.minecraft.world.level.ChunkPos(at);for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)l.setChunkForced(chunk.x+dx,chunk.z+dz,true);
        bicycleFixtureReady=true;
    }
    private static void setup(Minecraft mc){
        var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var l=p.server.getLevel(OtherWorld.KEY);var house=new BlockPos(112000,100,112000);p.server.getPlayerList().op(p.getGameProfile());
        var door=new DoorData.Door();door.owner=p.getUUID();door.placed=true;door.pos=house;door.activeUntil=Long.MAX_VALUE;DoorData.get(p.server).doors.put(door.id,door);
        var r=new RealmData.Realm();r.door=door.id;r.house=house;r.ready=true;r.islandComplete=true;r.sourceDoor=house;r.radius=200;r.porch=house.north(35);r.table=com.coraline.secrets.HouseCabinets.at(house,43,6,36);r.piano=com.coraline.secrets.HouseCabinets.at(house,20,6,60);r.doorPos=com.coraline.secrets.HouseCabinets.at(house,14,6,38);RealmData.get(p.server).realms.put(door.id,r);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=door.id;s.stage=VisitData.Stage.DONE;s.table=r.table;s.piano=r.piano;s.loop.phase=OtherMotherLoopState.Phase.BLACK_IS_TRADITIONAL;s.loop.event.putInt("Serial",93);VisitData.get(p.server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        try(var raw=FirstEscapeGrabVisualCheck.class.getResourceAsStream("/assets/coraline/structures/rosewood_house.json.gz");var zip=new java.util.zip.GZIPInputStream(Objects.requireNonNull(raw));var reader=new java.io.InputStreamReader(zip,java.nio.charset.StandardCharsets.UTF_8)){
            var doc=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();var palette=doc.getAsJsonArray("palette");
            for(var element:doc.getAsJsonArray("blocks")){var a=element.getAsJsonArray();int x=a.get(0).getAsInt(),y=a.get(1).getAsInt(),z=a.get(2).getAsInt();if(x<13||x>36||y<5||y>14||z<32||z>54)continue;var spec=palette.get(a.get(3).getAsInt()).getAsJsonObject();var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.tryParse(spec.get("id").getAsString()));var state=Objects.requireNonNull(block).defaultBlockState();for(var entry:spec.getAsJsonObject("properties").entrySet())state=property(state,entry.getKey(),entry.getValue().getAsString());var at=com.coraline.secrets.HouseCabinets.at(house,x,y,z);l.getChunkAt(at);l.setBlock(at,state,net.minecraft.world.level.block.Block.UPDATE_CLIENTS|net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE|net.minecraft.world.level.block.Block.UPDATE_SUPPRESS_DROPS);}
        }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
        var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);n.putString("Phase","CHAIR_CAPTURE");n.putLong("House",house.asLong());n.putLong("Prison",house.above(80).asLong());n.putLong("LittleDoor",r.doorPos.asLong());n.putLong("SafePlayer",FirstEscapeScene.room(s).asLong());n.putUUID("Choice",UUID.randomUUID());
        p.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);p.teleportTo(l,FirstEscapeScene.room(s).getX()+.5,FirstEscapeScene.room(s).getY()+.0625,FirstEscapeScene.room(s).getZ()+.5,180,0);
        FirstEscapeScene.prepareRoom(p,s);FirstEscapeScene.roomProps(p,s);
        // Match RosewoodHouse.build's runtime doorway, not only its imported
        // decorative block slice. The story itself must close this open door.
        l.setBlock(r.doorPos,ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(com.coraline.block.TinyDoorBlock.FACING,net.minecraft.core.Direction.EAST).setValue(com.coraline.block.TinyDoorBlock.OPEN,true).setValue(com.coraline.block.TinyDoorBlock.PORTAL,true),3);
        for(var at:List.of(r.doorPos.above(),r.doorPos.east(),r.doorPos.east().above()))l.setBlock(at,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var localDoor=(DoorBlockEntity)l.getBlockEntity(r.doorPos);localDoor.doorId=door.id;localDoor.setChanged();
        var m=ModEntities.MOM.get().create(l);m.makeOtherParent(0);m.getPersistentData().putUUID("RealmDoor",s.door);m.getPersistentData().putUUID("LoopOwner",s.owner);var couch=FirstEscapeScene.prop(p,s,"BugCouch");m.setPos(couch.position());if(!l.addFreshEntity(m))throw new IllegalStateException("Canonical fixture mother rejected");s.mom=m.getUUID();m.startRiding(couch,true);
        ready=true;
    }
    private static <T extends Comparable<T>> net.minecraft.world.level.block.state.BlockState property(net.minecraft.world.level.block.state.BlockState state,String key,String value){@SuppressWarnings("unchecked") var prop=(net.minecraft.world.level.block.state.properties.Property<T>)state.getBlock().getStateDefinition().getProperty(key);return prop==null?state:prop.getValue(value).map(v->state.setValue(prop,v)).orElse(state);}
    private static void shot(Minecraft mc,String name){try{var dir=java.nio.file.Path.of("../artwork/first_escape_grab_qa");java.nio.file.Files.createDirectories(dir);try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(dir.resolve(name+".png"));}}catch(java.io.IOException ex){throw new IllegalStateException(ex);}}
}
