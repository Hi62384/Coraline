package com.coraline.feast;

import java.util.*;
import com.coraline.otherworld.*;
import com.coraline.parents.*;
import com.coraline.secrets.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Owner-persisted rescue continuation; physical and acknowledged-audio gates remain server-owned. */
@Mod.EventBusSubscriber(modid="coraline")
public final class RescueChapter {
    public enum Phase {
        GARDEN_SEARCH,MANTIS_INTRO,MANTIS_FIGHT,MANTIS_RELEASE,MANTIS_TAKE,MANTIS_SINK,MANTIS_GONE,
        THEATRE_TRAVEL,THEATRE_SEARCH,WRAPPER_REACH,THEATRE_GRAB,THEATRE_FIGHT,THEATRE_EYE,
        ATTIC_TRAVEL,CLOTHES,ATTIC_SEARCH,BOB_A,BOB_B,BOB_C,FURNACE,BOB_D,BOB_TENT,CIRCUS_TRAVEL,BOB_E,SWARM_TRANSFORM,SWARM_FIGHT,RAT_CHASE,EXTERIOR,COLLAPSE,HOUSE_SAFE,ROOM_LOCKED,
        FINAL_A,FINAL_B,FINAL_C,SHOW_EYES,FINAL_D,FINAL_E,FINAL_F,FINAL_G,HIDING_CHOICE,FINAL_H,WARDROBE_ASIDE,FINAL_I,FINAL_J,FINAL_K,FIGHT_CHOICE,FINAL_M,CAT_THROW,FINAL_N,FINAL_O,FINAL_P,FINAL_Q,WEB_STEALTH,DOOR_KICK,DOOR_STRUGGLE,HAND_FALL,TUNNEL_CHASE,LOCK_DOOR,BANGING,
        REUNION_A,REUNION_B,CAT_NOTICE,FAREWELL_A,FAREWELL_B,EYES_CLOSE,CELESTIAL_OPEN,ORBS,FAREWELL_C,RELEASED_SPIRITS,COMPLETE
    }
    public static CompoundTag data(VisitData.Scene s){var n=ParentRescueStory.data(s);if(!n.contains("Continuation"))n.put("Continuation",new CompoundTag());return n.getCompound("Continuation");}
    public static boolean started(VisitData.Scene s){return s!=null&&ParentRescueStory.data(s).getCompound("Continuation").getBoolean("Started")&&(!AcceptedButtonEyes.active(s)||GhostEscapeEnding.epilogueActive(s));}
    public static Phase phase(VisitData.Scene s){try{return Phase.valueOf(data(s).getString("Phase"));}catch(IllegalArgumentException bad){return Phase.GARDEN_SEARCH;}}
    public static boolean finished(VisitData.Scene s){return started(s)&&phase(s)==Phase.COMPLETE;}
    public static boolean owns(ServerPlayer p,VisitData.Scene s){return s!=null&&s.owner.equals(p.getUUID())&&started(s);}
    /** Operator checkpoint: the Other Father fight is over; the first eye still must be taken. */
    public static String debugAfterOtherFather(ServerPlayer p){
        if(!p.hasPermissions(2))return "Operator permission required.";
        var s=OtherMotherLoop.scene(p);
        if(!owns(p,s)||!OtherWorld.isOther(p.level()))return "Enter your own final visit in the Other World first.";
        var phase=phase(s);
        if(phase.ordinal()<Phase.GARDEN_SEARCH.ordinal()||phase.ordinal()>Phase.MANTIS_FIGHT.ordinal())return "Use this during the garden search or Other Father fight.";
        var n=data(s);ShowPlayback.stop(p,s);RescueBosses.hideBar(p);
        n.putFloat("BossHealth",0);n.putBoolean("GardenDead",false);
        RescueEyes.eye(s,0).putBoolean("Defeated",true);RescueEyes.eye(s,0).putBoolean("Discovered",true);
        go(p,s,Phase.MANTIS_TAKE);sync(p,s);OtherMotherLoop.changed(p);
        return "Other Father defeated. Take the ghost eye from his hand to continue.";
    }
    public static String sound(Phase phase){
        if(phase.name().startsWith("FINAL_")&&phase.name().length()==7)return "finale_"+phase.name().substring(6).toLowerCase(Locale.ROOT);
        return switch(phase){case MANTIS_INTRO->"rescue_mantis_intro";case MANTIS_TAKE->"rescue_mantis_take";case MANTIS_GONE->"rescue_mantis_gone";case THEATRE_GRAB->"rescue_theatre_grab";case CLOTHES->"rescue_clothes";case BOB_A,BOB_B,BOB_C,BOB_D,BOB_E->"rescue_bobinsky_"+phase.name().substring(4).toLowerCase(Locale.ROOT);case TUNNEL_CHASE->"finale_s";case REUNION_A->"reunion_a";case REUNION_B->"reunion_b";case FAREWELL_A->"farewell_a";case FAREWELL_B->"farewell_b";case FAREWELL_C->"farewell_c";default->"";};
    }
    public static void go(ServerPlayer p,VisitData.Scene s,Phase next){
        if(!s.owner.equals(p.getUUID()))return;
        String interlude=next==Phase.THEATRE_TRAVEL?"garden_ghost_eye_music":next==Phase.ATTIC_TRAVEL?"theatre_ghost_eye_music":next==Phase.EXTERIOR?"circus_ghost_eye_music":"";
        if(!interlude.isEmpty()&&!data(s).getBoolean("Played_"+interlude)){VisitAudio.play(p,interlude,true);data(s).putBoolean("Played_"+interlude,true);}
        ShowPlayback.transition(p,s,sound(next));var n=data(s);n.putString("Phase",next.name());n.putInt("Ticks",0);n.remove("Audio");n.remove("AudioTail");n.remove("RecordingFailures");n.remove("RecordingFallbackTicks");n.remove("Blocker");n.putUUID("Input",UUID.randomUUID());n.remove("LastInput");RescueWebHearing.reset(p);
        s.loop.story.putString("MajorStoryPhase","RESCUE_"+next.name());OtherMotherLoop.changed(p);sync(p,s);
    }
    public static boolean begin(ServerPlayer p,VisitData.Scene s){
        var n=ParentRescueStory.data(s);
        if(!s.owner.equals(p.getUUID())||!FirstEscapeStory.data(s).getBoolean("Complete")||!MissingParentsCat.data(s).getBoolean("Complete")||!MissingParentsCat.tunnelRestored(s)||ParentRescueStory.phase(s)!=ParentRescueStory.Phase.PARENT_RESCUE_KITCHEN_MOTHER_DISAPPEARED||!n.getBoolean("Complete")||!n.getBoolean("KeySwallowed")||n.getBoolean("Seated"))return false;
        if(!ParentRescueStory.placeWardrobe(p,s,true,0))return false;
        data(s).putBoolean("Started",true);go(p,s,Phase.GARDEN_SEARCH);return true;
    }
    public static void tick(ServerPlayer p,VisitData.Scene s){
        if(!started(s)&&!begin(p,s))return;if(!owns(p,s)||!p.isAlive())return;
        var n=data(s);var state=phase(s);
        if(n.getBoolean("DebugFinalFightSkipped")&&n.getBoolean("PermanentlySealed")
            &&!ParentRescueStory.data(s).getBoolean("DoorLocked")&&state.ordinal()>=Phase.REUNION_A.ordinal()){
            var door=DoorData.get(p.server).doors.get(s.door);
            if(door!=null&&s.owner.equals(door.owner)){
                var home=com.coraline.parents.FamilyData.level(p.server,door.dimension);
                if(home!=null){
                    home.getChunkAt(door.pos);
                    var block=home.getBlockState(door.pos);
                    if(block.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())
                        &&!block.getValue(com.coraline.block.TinyDoorBlock.OPEN)
                        &&!block.getValue(com.coraline.block.TinyDoorBlock.PORTAL)){
                        ParentRescueStory.data(s).putBoolean("DoorLocked",true);OtherMotherLoop.changed(p);
                    }
                }
            }
        }
        if(n.getBoolean("DebugFinalFightSkipped")&&!n.getBoolean("HandSevered")
            &&n.getBoolean("KeyRecovered")&&n.getBoolean("PermanentlySealed")
            &&state.ordinal()>=Phase.REUNION_A.ordinal()){
            var door=DoorData.get(p.server).doors.get(s.door);
            if(door!=null&&s.owner.equals(door.owner)){
                var hand=RescueProps.ensure(p,s,"SeveredHand",RescueProps.HAND,0,BeldamFinale.handWaitingPosition(p.serverLevel(),door.pos));
                if(hand!=null){BeldamFinale.parkHand(p,s,hand,door.pos);n.putBoolean("HandSevered",true);OtherMotherLoop.changed(p);}
            }
        }
        if(state.ordinal()>=Phase.LOCK_DOOR.ordinal()){
            BeldamFinale.reconcileHand(p,s);
            com.coraline.doll.DollBondData.get(p.server).destroyAfterBeldam(s.owner);
        }
        if(n.getBoolean("BossLossPending")){RescueBosses.lose(p,s);return;}
        if(s.loop.story.getBoolean("DebugSkipFinalFightPending")&&state.ordinal()>=Phase.FINAL_A.ordinal()&&state.ordinal()<Phase.LOCK_DOOR.ordinal()&&RescueEyes.all(s)){
            var result=BeldamFinale.debugSkipFight(p,s);if(phase(s)==Phase.LOCK_DOOR){p.sendSystemMessage(net.minecraft.network.chat.Component.literal(result));return;}
        }
        RescueSiteCleanup.tick(p,s);
        if(ParentRescueStory.data(s).getBoolean("EmergedAtDoor")&&FirstEscapeStory.data(s).getBoolean("EscapeTunnelBuilt"))StoryTunnel.retireCrawl(p,s);
        boolean mantis=state.ordinal()>=Phase.MANTIS_INTRO.ordinal()&&state.ordinal()<=Phase.MANTIS_SINK.ordinal()&&OtherWorld.isOther(p.level());
        if(mantis){long now=p.serverLevel().getGameTime();if(!n.getBoolean("MantisMusic")||now>=n.getLong("MantisMusicUntil")){VisitAudio.play(p,"other_father_boss_music",true);n.putBoolean("MantisMusic",true);n.putLong("MantisMusicUntil",now+Math.max(20,ShowRecordings.duration("other_father_boss_music")));}}
        else if(n.getBoolean("MantisMusic")){VisitAudio.stopMusic(p,"other_father_boss_music");n.remove("MantisMusic");}
        // Retire the legacy rescue Theatre.mp3 stream; the client owns one continuous boss loop.
        if(n.getBoolean("TheatreMusic")){VisitAudio.stopMusic(p,"rescue_theatre_music");n.remove("TheatreMusic");}
        if(state==Phase.COMPLETE){RescueBosses.hideBar(p);RescueEpilogue.departAfterWaking(p,s);if(p.tickCount%20==0)PalaceJourney.afterWake(p,s);return;}
        if(n.getBoolean("Recover")){n.remove("Recover");recover(p,s);if(n.getBoolean("Recover"))return;state=phase(s);}
        if(!BeldamFinale.originalExitReady(p,s)){if(p.tickCount%5==0)sync(p,s);return;}
        if(!RescueEncounters.presentationReady(p,s,state)||!RescueEpilogue.presentationReady(p,s)){
            if(p.tickCount%5==0)sync(p,s);
            return;
        }
        // ROOM_LOCKED is a handoff, not a final fight: let its checkpoint
        // revalidate Cat/eye custody and reopen the real doors on older saves.
        // The actual FINAL_A through FINAL_O confrontation still enforces the
        // sealed-room boundary in BeldamFinale.preflight().
        if(state.ordinal()>=Phase.FINAL_A.ordinal()&&state.ordinal()<=Phase.BANGING.ordinal()&&!BeldamFinale.preflight(p,s)){sync(p,s);return;}
        if(state==Phase.FAREWELL_C){
            if(n.getBoolean("DreamDialogueComplete")||n.getBoolean("VisionRestored")){
                RescueEpilogue.tick(p,s,state,n.getInt("Ticks"),true);
                if(p.tickCount%5==0)sync(p,s);
                return;
            }
            if(p.serverLevel()!=p.server.getLevel(ActivityWorld.KEY)){
                RescueEpilogue.recoverVision(p,s);
                return;
            }
            if(RescueEpilogue.visionActors(p,s)==null){
                if(n.getBoolean("Audio")){ShowPlayback.stop(p,s);n.remove("Audio");n.remove("AudioTail");}
                if(p.tickCount%5==0)sync(p,s);
                return;
            }
        }
        String recording=sound(state);boolean done=false;int t=n.getInt("Ticks");
        if(!recording.isEmpty()){
            boolean fallback=RecordingRecovery.exhausted(n);
            if(!fallback){
                if(!n.getBoolean("Audio")&&ShowPlayback.start(p,s,recording,false))n.putBoolean("Audio",true);
                if(n.getBoolean("Audio"))done=ShowPlayback.tick(p,s);
                if(!n.getBoolean("Audio")||ShowPlayback.data(s).getBoolean("SafetySkipped")
                        ||!ShowPlayback.data(s).getString("Blocker").isEmpty()){
                    ShowPlayback.stop(p,s);
                    n.remove("Audio");n.remove("AudioTail");
                    RecordingRecovery.failed(n);OtherMotherLoop.changed(p);
                    if(RecordingRecovery.exhausted(n))p.displayClientMessage(net.minecraft.network.chat.Component.literal("This recording is unavailable. The scene will continue with its required actions."),true);
                    sync(p,s);return;
                }
                t=ShowPlayback.position(s);
                if(done){int tail=n.getInt("AudioTail");t+=tail;n.putInt("AudioTail",tail+1);}
            }else{
                t=RecordingRecovery.advance(n);
                done=RecordingRecovery.dialogueWindowEnded(t,ShowRecordings.duration(recording));
            }
            n.putInt("Ticks",t);
        }else n.putInt("Ticks",++t);
        if(state.ordinal()<=Phase.ROOM_LOCKED.ordinal())RescueEncounters.tick(p,s,state,t,done);
        else if(state.ordinal()<=Phase.BANGING.ordinal())BeldamFinale.tick(p,s,state,t,done);
        else RescueEpilogue.tick(p,s,state,t,done);
        if(n.getBoolean("WebEntered")&&state.ordinal()<=Phase.DOOR_STRUGGLE.ordinal())RescueWebSite.sag(p,s);
        if(p.tickCount%5==0)sync(p,s);if(p.tickCount%20==0)OtherMotherLoop.changed(p);
    }
    static final class RecordingRecovery {
        static boolean exhausted(CompoundTag state){return state.getInt("RecordingFailures")>=3;}
        static void failed(CompoundTag state){
            if(!exhausted(state))state.putInt("RecordingFailures",Math.max(0,state.getInt("RecordingFailures"))+1);
        }
        static int advance(CompoundTag state){
            int t=Math.max(0,state.getInt("RecordingFallbackTicks"));
            if(t<Integer.MAX_VALUE)t++;
            state.putInt("RecordingFallbackTicks",t);return t;
        }
        static boolean dialogueWindowEnded(int ticks,int duration){return ticks>=Math.max(100,duration);}
    }
    static void cutsceneSafety(ServerPlayer p,VisitData.Scene s){
        var state=phase(s);String voice=sound(state);
        // Fights, choices, retrievals, travel and stealth are gameplay, not
        // cinematics. A watchdog never pretends these challenges were won.
        // Do not blanket-exclude everything after Bobinsky: that also disables
        // recovery for his dialogue, the scripted finale and the reunion/dream.
        // The voiced 52-block tunnel chase remains a physical player objective.
        if(RescueBosses.fighting(state)||state==Phase.TUNNEL_CHASE)return;
        boolean motion=java.util.EnumSet.of(Phase.MANTIS_RELEASE,Phase.THEATRE_GRAB,Phase.ATTIC_SEARCH,Phase.FURNACE,Phase.BOB_TENT,Phase.SWARM_TRANSFORM,Phase.WARDROBE_ASIDE,Phase.DOOR_KICK,Phase.HAND_FALL,Phase.BANGING,Phase.EYES_CLOSE,Phase.CELESTIAL_OPEN,Phase.ORBS).contains(state);
        if(voice.isEmpty()&&!motion)return;
        // This independent watchdog runs even when the chapter tick is paused.
        // Do not finish reunion/Cat dialogue or Bobinsky beats while their
        // original actors, viewer and world are missing after reload.
        if(!BeldamFinale.originalExitReady(p,s)
                ||!RescueEncounters.presentationReady(p,s,state)||!RescueEpilogue.presentationReady(p,s)){
            StoryCutsceneWatchdog.retry(s,"rescue_finale");
            return;
        }
        // Custodied quest items and a valid destination are real prerequisites,
        // not permission to mint a replacement key or discard a full inventory.
        if(state.ordinal()>=Phase.FINAL_A.ordinal()&&state.ordinal()<=Phase.TUNNEL_CHASE.ordinal()&&!BeldamFinale.preflight(p,s))return;
        if(!StoryCutsceneWatchdog.overdue(p,s,"rescue_finale",state.name(),voice.isEmpty()?600:StoryCutsceneWatchdog.voiceBudget(voice)))return;
        ShowPlayback.safetyFinish(p,s,"Rescue cinematic exceeded its safety window");int tail=Math.max(1200,ShowRecordings.duration(voice)+600);data(s).putInt("Ticks",tail);
        if(state.ordinal()<=Phase.ROOM_LOCKED.ordinal())RescueEncounters.tick(p,s,state,tail,true);
        else if(state.ordinal()<=Phase.BANGING.ordinal())BeldamFinale.tick(p,s,state,tail,true);
        else RescueEpilogue.tick(p,s,state,tail,true);
        if(phase(s)==state)StoryCutsceneWatchdog.retry(s,"rescue_finale");
        sync(p,s);
    }
    public static void recover(ServerPlayer p,VisitData.Scene s){
        var state=phase(s);
        if(RescueBosses.fighting(state)){
            if(!RescueBosses.resume(p,s))data(s).putBoolean("Recover",true);
            return;
        }
        if(state==Phase.RAT_CHASE){RescueEncounters.retry(p,s,state);return;}
        if(state==Phase.FINAL_O||state==Phase.FINAL_P||state==Phase.FINAL_Q){
            // The player can disconnect after web generation but before its
            // actual landing. A saved WebEntryReady/WebVerified flag is not
            // fresh proof that the arena or canonical cast survived a reload.
            var web=data(s);
            web.remove("WebEntryReady");web.remove("WebEntered");web.remove("WebEntryRecheck");
            web.remove("WebVerified");web.putInt("WebVerify",0);
            go(p,s,state);
            return;
        }
        if(state==Phase.COLLAPSE){
            if(RescueEncounters.restoreIncompleteCollapse(p,s))return;
            var level=p.server.getLevel(OtherWorld.KEY);
            if(p.serverLevel()!=level&&!transfer(p,level,BlockPos.of(data(s).getLong("CollapseStart"))))data(s).putBoolean("Recover",true);
            return;
        }
        if(state==Phase.CAT_THROW||state==Phase.WEB_STEALTH||state==Phase.DOOR_STRUGGLE||state==Phase.TUNNEL_CHASE){BeldamFinale.retry(p,s,state,"Retrying this challenge.");return;}
        if(state.ordinal()>=Phase.EYES_CLOSE.ordinal()&&state!=Phase.RELEASED_SPIRITS&&state!=Phase.COMPLETE){RescueEpilogue.recoverVision(p,s);return;}
        go(p,s,state);
    }
    public static void input(ServerPlayer p,UUID nonce,String action){
        var s=OtherMotherLoop.scene(p);if(!owns(p,s)||!data(s).hasUUID("Input")||!nonce.equals(data(s).getUUID("Input")))return;
        var n=data(s);long now=p.serverLevel().getGameTime();if(n.contains("LastInput")&&now-n.getLong("LastInput")<3)return;n.putLong("LastInput",now);
        if(action.equals("theatre_eye")){RescueEncounters.pullTheatreEye(p,s);return;}
        if(action.equals("take_eye")){RescueEncounters.takeOfferedEye(p,s);return;}
        BeldamFinale.input(p,s,action);
    }
    public static boolean interact(ServerPlayer p,Entity target){
        var s=OtherMotherLoop.scene(p);if(!owns(p,s)||target.level()!=p.level()||p.distanceToSqr(target)>25||!p.hasLineOfSight(target))return false;
        if(target instanceof CinematicPropEntity prop&&!prop.owned(s))return false;
        return RescueEncounters.interact(p,s,target)||BeldamFinale.interact(p,s,target);
    }
    public static void sync(ServerPlayer p,VisitData.Scene s){
        var state=phase(s);var n=data(s);var packet=new CompoundTag();
        packet.putBoolean("AlternateGhostEscape",n.getBoolean("AlternateGhostEscape"));
        for(String key:List.of("Ticks","Input","Blocker","Cat","CatHeld","CatFlying","CatRescued","GardenDead","TheatreDead","BeldamBlind","BeldamMorph","Struggle","VisionBlur","FarewellEyesClosing","GhostsSad","CollapseRadius","CollapseInitialRadius","HouseSafe","GreenHouse","WebEntered","PermanentlySealed"))if(n.contains(key))packet.put(key,n.get(key).copy());
        for(int i=0;i<3;i++){
            var eye=RescueEyes.eye(s,i);
            if((i!=0||state.ordinal()>=Phase.MANTIS_GONE.ordinal())&&OtherWorld.isOther(p.level())&&eye.getBoolean("Claimed")&&eye.contains("DrainStarted")&&eye.contains("DrainOrigin")&&eye.getString("DrainDimension").equals(p.level().dimension().location().toString())){
                packet.putLong("DrainOrigin"+i,eye.getLong("DrainOrigin"));
                packet.putFloat("DrainRadius"+i,Math.min(i==0?86:58,Math.max(0,p.serverLevel().getGameTime()-eye.getLong("DrainStarted"))*.22F));
            }
        }
        packet.putLong("Room",FirstEscapeScene.room(s).asLong());
        packet.putBoolean("RescueChapter",true);packet.putString("Phase",phase(s).name());packet.putString("Sound",sound(phase(s)));packet.putInt("Duration",ShowRecordings.duration(sound(phase(s))));packet.putBoolean("ClockRunning",sound(phase(s)).isEmpty()||ShowPlayback.started(s));packet.putLong("House",ParentRescueStory.scenery(s).getLong("House"));packet.putLong("Garden",RescueEncounters.garden(p,s).asLong());packet.putLong("Theatre",TheatreSite.center(p,s).asLong());packet.putLong("Circus",MouseCircusSite.center(p,s).asLong());packet.putBoolean("AllEyes",RescueEyes.all(s));var mother=ParentRescueStory.mother(p,s);packet.putInt("Mother",mother==null?-1:mother.getId());packet.putFloat("BossPattern",n.getInt("BossPattern"));packet.putFloat("BossBeat",n.getInt("BossBeat"));SecretsNetwork.send(p,packet);
    }
    public static void hint(ServerPlayer p,String message){FirstEscapeStory.hint(p,message);}
    public static boolean transfer(ServerPlayer p,ServerLevel level,BlockPos at){
        if(level==null)return false;level.getChunkAt(at);var safe=OtherWorld.safePlayer(level,at);Vec3 target=safe==null?null:Vec3.atBottomCenterOf(safe);var body=target==null?null:p.getDimensions(Pose.STANDING).makeBoundingBox(target);
        if(body==null||!level.getWorldBorder().isWithinBounds(safe)||level.containsAnyLiquid(body)||level.getBlockCollisions(p,body).iterator().hasNext()||!level.getBlockCollisions(p,new net.minecraft.world.phys.AABB(body.minX,body.minY-1,body.minZ,body.maxX,body.minY,body.maxZ)).iterator().hasNext()){data(OtherMotherLoop.scene(p)).putString("Blocker","The saved challenge checkpoint lacks safe footing or body clearance.");return false;}
        p.stopRiding();p.setForcedPose(null);p.setPose(Pose.STANDING);p.teleportTo(level,target.x,target.y,target.z,p.getYRot(),p.getXRot());p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);return p.serverLevel()==level&&p.position().distanceToSqr(target)<.000001;
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST) public static void interact(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event){if(event.getEntity() instanceof ServerPlayer p&&event.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND&&interact(p,event.getTarget())){event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);}}
    private static boolean protectedFinalDoor(ServerLevel level,BlockPos at){
        if(!OtherWorld.isOther(level))return false;
        return VisitData.get(level.getServer()).scenes.values().stream().anyMatch(scene->BeldamFinale.lockedDoor(scene,at));
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void block(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        if(event.getEntity() instanceof ServerPlayer p&&(protectedFinalDoor(p.serverLevel(),event.getPos())
                ||event.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND&&BeldamFinale.block(p,event.getPos()))){
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void preventFinalDoorBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event){
        if(event.getLevel() instanceof ServerLevel level&&protectedFinalDoor(level,event.getPos()))event.setCanceled(true);
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST) public static void attack(net.minecraftforge.event.entity.player.AttackEntityEvent event){if(event.getEntity() instanceof ServerPlayer p&&RescueBosses.hit(p,event.getTarget()))event.setCanceled(true);}
    @SubscribeEvent public static void damage(net.minecraftforge.event.entity.living.LivingDamageEvent event){
        if(event.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(!owns(p,s)||finished(s))return;
            if(RescueEpilogue.protectedVision(s)){event.setCanceled(true);return;}
            if(event.getAmount()>=p.getHealth()){event.setCanceled(true);p.setHealth(p.getMaxHealth());p.clearFire();if(RescueBosses.fighting(phase(s)))RescueBosses.lose(p,s);else if(phase(s)==Phase.CAT_THROW||phase(s)==Phase.WEB_STEALTH)AcceptedButtonEyes.begin(p,s,AcceptedButtonEyesProgress.Route.FINAL_BOSS_LOSS);else if(phase(s)==Phase.COLLAPSE)RescueEncounters.retry(p,s,phase(s));else recover(p,s);}
        }else if(event.getSource().getEntity() instanceof ServerPlayer p&&RescueBosses.hit(p,event.getEntity()))event.setCanceled(true);
    }
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(owns(p,s)&&!finished(s)){data(s).putBoolean("Recover",true);VisitAudio.stopMusic(p,"other_father_boss_music");data(s).remove("MantisMusic");if(data(s).getBoolean("TheatreMusic")){VisitAudio.stopMusic(p,"rescue_theatre_music");data(s).remove("TheatreMusic");}ShowPlayback.stop(p,s);RescueBosses.hideBar(p);OtherMotherLoop.changed(p);}}}
    @SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(owns(p,s)&&!finished(s)){var n=data(s);n.putBoolean("Recover",true);n.putUUID("Input",UUID.randomUUID());n.remove("LastInput");n.remove("TheatreMusic");n.remove("MantisMusic");ShowPlayback.stop(p,s);RescueBosses.hideBar(p);OtherMotherLoop.changed(p);}}}
    private RescueChapter(){}
}
