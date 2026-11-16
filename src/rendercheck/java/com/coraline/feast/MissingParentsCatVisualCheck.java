package com.coraline.feast;

import java.util.*;
import com.coraline.otherworld.*;
import com.coraline.parents.*;
import com.coraline.registry.ModBlocks;
import com.coraline.block.TinyDoorBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Isolated fixture playthrough, real A-F playback/ACKs and native Cat navigation.
 * This does not substitute for acceptance in a player's original furnished house. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class MissingParentsCatVisualCheck {
    private static boolean started;private static volatile boolean ready,complete;
    private static volatile String phase="",blocker="";private static volatile int elapsed;
    private static long lastTime=-1;private static int ticks,finishedTicks;private static String previous="";
    private static final Set<String> shots=new HashSet<>();
    private static java.util.List<BlockPos> followNodes=java.util.List.of();private static BlockPos followGoal;
    private static volatile net.minecraft.client.resources.sounds.SoundInstance intro;
    private static volatile long introBegan,catAppeared,introEnded;
    private static int introStarts;
    @SubscribeEvent public static void sound(net.minecraftforge.client.event.sound.PlaySoundEvent event){
        if(Boolean.getBoolean("coraline.missingParentsCatVisualCheck")&&event.getSound()!=null&&com.coraline.client.MissingParentsMusic.owns(event.getSound())){intro=event.getSound();introStarts++;}
    }
    private static void intro(Minecraft mc){
        var track=intro;if(track==null)return;long now=System.nanoTime();boolean active=mc.getSoundManager().isActive(track);
        if(introStarts!=1||track.isLooping())throw new AssertionError("Missing Parents intro restarted or looped");
        if(introBegan==0){if(active)introBegan=now;return;}
        if(!active&&introEnded==0){introEnded=now;double length=ShowRecordings.duration(MissingParentsCat.INTRO_MUSIC)/20.,heard=(now-introBegan)/1e9;
            if(heard+.25<length)throw new AssertionError("Missing Parents intro cut before natural EOF: "+heard+" / "+length);
            if(catAppeared==0)throw new AssertionError("Cat had not spawned before intro music ended");double overlap=(now-catAppeared)/1e9;
            if(overlap<3.5||overlap>6.5)throw new AssertionError("Cat did not spawn five seconds before intro EOF: "+overlap);
            System.out.println("MISSING_PARENTS_INTRO_NATIVE_COMPLETE fullSeconds="+heard+" catBeforeEofSeconds="+overlap+" starts="+introStarts+"; actual native arrival and one complete real music stream");
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("coraline.missingParentsCatVisualCheck")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60)return;
        mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(complete?4:2);
        if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)mc.setScreen(null);
        if(!started){started=true;mc.getSingleplayerServer().execute(()->prepare(mc));return;}
        if(!ready)return;
        intro(mc);
        String shot=phase.equals("B")&&elapsed>70?"parents_writing":phase.equals("WAIT_BREAK")?"help_us_complete":phase.equals("C")&&elapsed>50?"mirror_cracks":phase.equals("SHATTER")&&elapsed<20?"mirror_shatter":phase.equals("E")&&elapsed>100?"retrieving_parent_dolls":phase.equals("F")?"parent_dolls_revealed":"";
        if(!shot.isEmpty()&&shots.add(shot))capture(mc,shot);
        if(lastTime==mc.level.getGameTime())return;lastTime=mc.level.getGameTime();
        if(++ticks>4200)throw new IllegalStateException("Cat native fixture stalled: "+phase+" / "+elapsed+" / "+blocker);
        if(!complete)mc.getSingleplayerServer().execute(()->advance(mc));
        else if(CurrentStoryVisualCheck.tick(mc)){if(introEnded==0||catAppeared==0)throw new AssertionError("Missing actual intro/arrival/EOF evidence");capture(mc,"cat_departed");com.mojang.logging.LogUtils.getLogger().info("MISSING_PARENTS_CAT_NATIVE_COMPLETE: isolated fixture, full intro/arrival, real A-F ACKs, Cat navigation, writing, break, retrieval, owner clues and day/night tunnel; NOT original-house or human audio acceptance");mc.stop();}
    }
    private static void prepare(Minecraft mc){
        var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var l=p.server.overworld();var c=new BlockPos(29600,110,29600);
        var old=OtherMotherLoop.scene(p);
        if(old!=null&&MissingParentsCat.data(old).getBoolean("Started")&&!MissingParentsCat.data(old).getBoolean("Complete"))throw new IllegalStateException("Unfinished prior Cat fixture: recover or reset explicitly before another run");
        p.removeAllEffects();p.server.getPlayerList().op(p.getGameProfile());p.stopRiding();p.getInventory().clearContent();
        for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++){
            var at=c.offset(x,0,z);l.getChunkAt(at);l.setBlock(at.below(),Blocks.OAK_PLANKS.defaultBlockState(),2);
            for(int y=0;y<=5;y++)l.setBlock(at.above(y),Blocks.AIR.defaultBlockState(),2);
        }
        l.setDayTime(1000);l.setWeatherParameters(100000,0,false,false);p.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
        p.teleportTo(l,c.getX()+.5,c.getY(),c.getZ()+6.5,0,0);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.table=s.piano=c;s.loop.event.putInt("Serial",81);VisitData.get(p.server).scenes.put(s.owner,s);
        var door=new DoorData.Door();door.id=s.door;door.owner=s.owner;door.pos=c.east(12);door.placed=true;DoorData.get(p.server).doors.put(door.id,door);
        l.setBlock(door.pos,ModBlocks.TINY_DOOR.get().defaultBlockState(),2);((DoorBlockEntity)l.getBlockEntity(door.pos)).doorId=s.door;
        var realm=new RealmData.Realm();realm.door=s.door;realm.sourceDoor=door.pos;realm.house=c.east(200);realm.doorPos=realm.house;realm.ready=realm.islandComplete=true;realm.radius=500;RealmData.get(p.server).realms.put(s.door,realm);
        var family=ParentAccess.family(p.server,s.owner);if(family==null){family=new FamilyData.Family();family.owner=s.owner;FamilyData.get(p.server).families.put(family.id,family);}
        family.bed=c.north(3);family.momSkin=2;family.dadSkin=3;family.homeDim="minecraft:overworld";
        // Exercise the actual four-part Parents' Bed from the user's report,
        // not a vanilla stand-in that would miss the original regression.
        var bed=ModBlocks.PARENTS_BED.get().defaultBlockState().setValue(ParentsBedBlock.FACING,Direction.NORTH);
        for(int part=0;part<4;part++)l.setBlock(ParentsBedBlock.partPos(family.bed,Direction.NORTH,part),bed.setValue(ParentsBedBlock.PART,part),2);
        ((ParentsBedEntity)l.getBlockEntity(family.bed)).familyId=family.id;
        // Deliberately no mirror and no walls: A must resolve its own real
        // bedside presentation, as in the reported open-air player setup.
        com.mojang.logging.LogUtils.getLogger().info("CAT_QA_NO_PREPLACED_MIRROR: open-air bed, production mirror resolution required");
        if(!FirstEscapeStory.finishFirstEscape(p,s,true))throw new IllegalStateException("Canonical First Escape debug completion failed");
        String result=MissingParentsTea.completeForCatDebug(p,s);if(!MissingParentsCat.eligible(s))throw new IllegalStateException("Cat prerequisites failed: "+result);
        MissingParentsCat.data(s).putInt("Delay",0);ready=true;
    }
    private static void advance(Minecraft mc){
        var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var s=OtherMotherLoop.scene(p);var n=MissingParentsCat.data(s);var state=MissingParentsCat.phase(s);
        phase=state.name();elapsed=n.getInt("Ticks");blocker=n.getString("Blocker");
        if(!phase.equals(previous)){previous=phase;com.mojang.logging.LogUtils.getLogger().info("CAT_QA_PHASE: {} / {}",phase,blocker);}
        var cat=ShowActors.actor(p,s,"cat");
        if(cat!=null&&catAppeared==0&&MissingParentsCat.data(s).getBoolean("Started"))catAppeared=System.nanoTime();
        if(ticks%100==0)System.out.println("CAT_QA_NAV: phase="+phase+" player="+p.position()+" cat="+(cat==null?"missing":cat.position())+" target="+(n.contains("MoveTarget")?BlockPos.of(n.getLong("MoveTarget")):"none")+" stuck="+n.getInt("StuckTicks"));
        if(state==MissingParentsCat.Phase.NOTICE&&cat!=null)p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,cat.getEyePosition());
        if((state==MissingParentsCat.Phase.FOLLOW_MIRROR||state==MissingParentsCat.Phase.FOLLOW_BED)&&cat!=null){
            follow(p,cat);
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,cat.getEyePosition());
        }
        if((state==MissingParentsCat.Phase.B||state==MissingParentsCat.Phase.C||state==MissingParentsCat.Phase.D||state==MissingParentsCat.Phase.WAIT_BREAK)&&n.contains("Mirror")){
            if(!n.getBoolean("QAViewingMirror")){
                var face=Direction.byName(n.getString("MirrorFacing"));if(face==null)face=Direction.SOUTH;
                var target=Vec3.atBottomCenterOf(BlockPos.of(n.getLong("Mirror"))).add(Vec3.atLowerCornerOf(face.getNormal()).scale(3.2));
                if(p.level().getBlockCollisions(p,p.getDimensions(p.getPose()).makeBoundingBox(target)).iterator().hasNext())throw new IllegalStateException("Declared front-glass QA camera is obstructed");
                p.teleportTo(target.x,target.y,target.z);n.putBoolean("QAViewingMirror",true);
            }
            var at=BlockPos.of(n.getLong("Mirror"));p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,at.getCenter().add(0,.7,0));
            if(state==MissingParentsCat.Phase.WAIT_BREAK){if(!n.getBoolean("HelpUsComplete")||!n.getBoolean("BFinished"))throw new IllegalStateException("Break prompt bypassed B/writing");
                // Following the Cat stops several metres from the mirror. Stage
                // the fixture's deliberate interaction within the real 4m reach;
                // do not weaken the gameplay range check or wait out a choice.
                if(p.distanceToSqr(at.getCenter())>12){var target=Vec3.atBottomCenterOf(BlockPos.of(n.getLong("MirrorApproach")));if(!p.level().noCollision(p,p.getBoundingBox().move(target.subtract(p.position()))))throw new IllegalStateException("QA mirror interaction approach is obstructed");p.teleportTo(target.x,target.y,target.z);}
                if(!MissingParentsCat.block(p,at))throw new IllegalStateException("In-range mirror interaction was refused");}
        }
        if(state==MissingParentsCat.Phase.E||state==MissingParentsCat.Phase.F){var bed=BlockPos.of(n.getLong("BedFoot"));var front=Direction.byName(n.getString("BedFront"));if(front==null)front=Direction.SOUTH;
            // The Cat was followed from the mirror on the headboard side.
            // Review the actual pulled-out props from the foot, not through
            // the solid mattress; this is declared camera staging, not AI.
            if(!n.getBoolean("QAViewingDolls")){var target=Vec3.atBottomCenterOf(bed.relative(front,3));if(!p.level().noCollision(p,p.getBoundingBox().move(target.subtract(p.position()))))throw new IllegalStateException("QA doll-view approach is obstructed");p.teleportTo(target.x,target.y,target.z);n.putBoolean("QAViewingDolls",true);}
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,Vec3.atBottomCenterOf(bed).add(front.getStepX()*.95,.15,front.getStepZ()*.95));}
        if(state==MissingParentsCat.Phase.DEPART&&cat!=null)p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,p.getEyePosition().subtract(cat.getEyePosition().subtract(p.getEyePosition())));
        if(n.getBoolean("Complete")){
            if(s.loop.story.getCompound("CutsceneSafety").getString("LastSkipped").startsWith("post_escape_cat"))throw new IllegalStateException("Cat fixture required the stall watchdog rather than completing normally");
            if(!n.getBoolean("FFinished")||!n.getBoolean("DollsRetrieved")||!n.getBoolean("MirrorBroken")||!n.getBoolean("TunnelRestored")||ShowActors.actor(p,s,"cat")!=null||ParentRescueStory.started(s))throw new IllegalStateException("Invalid Cat final state");
            int stones=0;for(var item:p.getInventory().items)if(item.is(com.coraline.registry.ModItems.SEEING_STONE.get()))stones+=item.getCount();if(stones!=1)throw new IllegalStateException("Seeing Stone duplicated/lost");
            var door=DoorData.get(p.server).doors.get(s.door);long time=p.serverLevel().getDayTime();
            for(long preview:new long[]{1000,18000}){p.serverLevel().setDayTime(preview);if(!OtherWorld.active(p.serverLevel(),door))throw new IllegalStateException("Permanent tunnel lost by day/night");}p.serverLevel().setDayTime(time);
            if(!(p.level().getBlockEntity(door.pos) instanceof DoorBlockEntity block)||!block.storyDecayed||!block.getBlockState().getValue(TinyDoorBlock.PORTAL))throw new IllegalStateException("Source doorway not restored/decayed");
            if(!n.hasUUID("ParentDolls")||p.serverLevel().getEntity(n.getUUID("ParentDolls"))!=null)throw new IllegalStateException("Temporary parent dolls remained after the cutscene");
            complete=true;
        }
    }
    /** Declared QA input: small clear floor steps, including walking around the
     * solid bed. A straight-line follower used to stop against the mattress. */
    private static void follow(net.minecraft.server.level.ServerPlayer p,net.minecraft.world.entity.Mob cat){
        var delta=cat.position().subtract(p.position()).multiply(1,0,1);if(delta.lengthSqr()<=1.44)return;
        var step=delta.normalize().scale(.15);
        if(followNodes.isEmpty()&&!p.level().getBlockCollisions(p,p.getBoundingBox().expandTowards(step).deflate(.001)).iterator().hasNext()){p.teleportTo(p.getX()+step.x,p.getY(),p.getZ()+step.z);return;}
        var proxy=com.coraline.registry.CharacterCast.ENTITIES.get("wybie").get().create(p.serverLevel());proxy.setPos(p.position());
        var goal=followNodes.isEmpty()?StoryFloorRoute.beside(proxy,new Vec3(cat.getX(),p.getY(),cat.getZ()),1,2.5):followGoal;
        if(goal==null)return;
        if(!goal.equals(followGoal)||followNodes.isEmpty()){followGoal=goal;followNodes=new java.util.ArrayList<>(StoryFloorRoute.plan(proxy,goal));}
        while(!followNodes.isEmpty()&&p.position().distanceToSqr(StoryFloorRoute.feet(p.serverLevel(),followNodes.get(0)))<.04)followNodes.remove(0);
        if(followNodes.isEmpty())return;delta=StoryFloorRoute.feet(p.serverLevel(),followNodes.get(0)).subtract(p.position());step=delta.normalize().scale(Math.min(.15,delta.length()));
        if(!p.level().getBlockCollisions(p,p.getBoundingBox().expandTowards(step).deflate(.001)).iterator().hasNext())p.teleportTo(p.getX()+step.x,p.getY()+step.y,p.getZ()+step.z);
    }
    private static void capture(Minecraft mc,String name){try{var dir=java.nio.file.Path.of("../artwork/missing_parents_cat_qa");java.nio.file.Files.createDirectories(dir);try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(dir.resolve(name+".png"));}}catch(Exception e){throw new RuntimeException(e);}}
    private MissingParentsCatVisualCheck(){}
}
