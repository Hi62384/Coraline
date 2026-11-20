package com.coraline.feast;

import com.coraline.bobinsky.*;
import com.coraline.tea.*;
import com.coraline.registry.*;
import com.coraline.secrets.SecretTransactions;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Post-escape owner checkpoint, using the ORIGINAL tea house, actors, seats and terrain journal. */
@Mod.EventBusSubscriber(modid="coraline")
public final class MissingParentsTea {
    public enum Phase { ELIGIBLE,WAITING_FOR_KNOCK,KNOCKING,GREETING,FOLLOW_SPINK,WAITING_FOR_SEAT,FORCIBLE_ENTRANCE,STORY_A,STORY_B,STORY_C,STORY_D,STORY_E,STORY_F,STORY_G,STORY_H,STORY_I,WAITING_FOR_EXIT,COMPLETED }
    public static final String KEY="MissingParentsSpinkAndForcible";
    public static CompoundTag data(VisitData.Scene s){if(!s.loop.story.contains(KEY))s.loop.story.put(KEY,new CompoundTag());return s.loop.story.getCompound(KEY);}
    public static boolean isVisit(CompoundTag v){return v.getBoolean("MissingParentsTea");}
    public static Phase phase(CompoundTag n){try{return Phase.valueOf(n.getString("StoryPhase"));}catch(IllegalArgumentException invalid){return Phase.ELIGIBLE;}}
    public static boolean eligible(VisitData.Scene s){return s!=null&&FirstEscapeStory.engaged(s)&&FirstEscapeStory.phase(s)==FirstEscapeTimeline.Phase.ESCAPE_COMPLETE&&FirstEscapeStory.data(s).getBoolean("Complete")&&FirstEscapeStory.data(s).getBoolean("OtherWorldSealed");}
    public static boolean active(VisitData.Scene s){return s!=null&&!AcceptedButtonEyes.active(s)&&s.loop.story.getCompound(KEY).getBoolean("Active");}
    static boolean invitationReady(CompoundTag n){
        if(n.getBoolean("Selected"))return true;
        if(!n.contains("SelectionDelay")){n.putBoolean("SelectionPending",true);n.putInt("SelectionDelay",600);}
        int remaining=Math.max(0,n.getInt("SelectionDelay")-1);n.putInt("SelectionDelay",remaining);
        if(remaining>0)return false;n.putBoolean("Selected",true);n.remove("SelectionPending");return true;
    }
    public static String sound(Phase phase){return phase==Phase.GREETING?"tea_visit_a":phase.name().startsWith("STORY_")?"missing_parents_tea_"+phase.name().substring(6).toLowerCase(Locale.ROOT):"";}
    public static boolean select(ServerPlayer p,VisitData.Scene s,boolean force){
        if(!eligible(s)||!s.owner.equals(p.getUUID())||force&&!p.hasPermissions(2))return false;
        var n=data(s);if(n.getBoolean("Completed"))return false;
        if(!n.getBoolean("Rolled")){n.putBoolean("Rolled",true);n.putDouble("Probability",VisitConfig.MISSING_PARENTS_TEA_CHANCE.get());n.putDouble("Roll",p.getRandom().nextDouble());n.putBoolean("Selected",n.getDouble("Roll")<n.getDouble("Probability"));n.putString("StoryPhase",Phase.ELIGIBLE.name());}
        if(force){n.putBoolean("Selected",true);n.remove("SelectionDelay");n.remove("SelectionPending");}
        else if(!n.getBoolean("Selected")&&!n.contains("SelectionDelay")){n.putBoolean("SelectionPending",true);n.putInt("SelectionDelay",600);}
        OtherMotherLoop.changed(p);p.server.overworld().getDataStorage().save();return n.getBoolean("Selected");
    }
    private static boolean owns(ServerPlayer p,VisitData.Scene s,CompoundTag n){return s.owner.equals(p.getUUID())&&n.hasUUID("Owner")&&s.owner.equals(n.getUUID("Owner"))&&n.hasUUID("Door")&&s.door.equals(n.getUUID("Door"));}
    private static void save(ServerPlayer p,VisitData.Scene s,boolean flush){OtherMotherLoop.changed(p);BobinskyVisitData.get(p.server).setDirty();if(flush)p.server.overworld().getDataStorage().save();}
    private static boolean prepare(ServerPlayer p,VisitData.Scene s){
        var n=data(s);var neighbors=BobinskyVisitData.get(p.server);var previous=neighbors.teaVisits.get(s.owner);
        // Preserve an obsolete tea house's recovery journal without making its
        // deferred cleanup a prerequisite for the story-critical invitation.
        if(previous!=null&&previous!=n&&previous.getBoolean("Active")){
            if(isVisit(previous)&&n.hasUUID("Token")&&previous.hasUUID("Token")&&n.getUUID("Token").equals(previous.getUUID("Token"))){
                n.merge(previous);neighbors.teaVisits.put(s.owner,n);
                if(n.getBoolean("Built"))return true;
                if(n.getBoolean("Building")){finishBuild(p,s);return true;}
            }else n.put("PreviousTeaVisit",previous.copy());
        }
        for(char c='a';c<='i';c++)if(ShowRecordings.duration("missing_parents_tea_"+c)<=0){n.putString("Blocker","Missing original story recording "+c);return false;}
        if(ShowRecordings.duration("tea_visit_a")<=0)return false;
        var at=MissingParentsTeaPlacement.find(p);if(at==null){n.putString("Blocker","Looking for a clear temporary house site; uneven ground is allowed.");return false;}
        var originals=BobinskyHouse.snapshot(p.serverLevel(),at,MissingParentsTeaPlacement.CLEAR_RX,MissingParentsTeaPlacement.CLEAR_RZ);
        n.putBoolean("FloatingLanding",true);n.putBoolean("TerrainClearanceV3",true);
        n.putBoolean("MissingParentsTea",true);n.putBoolean("Tea",true);n.putBoolean("DetailedTeaHouse",true);n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);if(!n.hasUUID("Token"))n.putUUID("Token",UUID.randomUUID());n.putLong("House",at.asLong());n.put("Originals",originals);n.putBoolean("Active",true);n.putBoolean("Building",true);neighbors.teaVisits.put(s.owner,n);save(p,s,true);
        finishBuild(p,s);return true;
    }
    private static void finishBuild(ServerPlayer p,VisitData.Scene s){var n=data(s);var at=center(n);var originals=n.getList("Originals",10);clearSurroundings(p,n);TeaHouse.build(p.serverLevel(),n);
        clearEntrance(p,n);
        var placed=new CompoundTag();for(var t:originals){var cell=(CompoundTag)t;var pos=BlockPos.of(cell.getLong("Pos"));placed.put(Long.toString(pos.asLong()),NbtUtils.writeBlockState(p.serverLevel().getBlockState(pos)));}n.put("Placed",placed);
        n.putString("Bowl","KITCHEN");n.putBoolean("Built",true);n.remove("Building");go(p,s,Phase.WAITING_FOR_KNOCK);TeaVisitTravel.invite(p);save(p,s,true);
    }
    /** Clear only the newly snapshotted outer margin; the builder clears the interior. */
    private static void clearSurroundings(ServerPlayer p,CompoundTag n){
        if(!n.getBoolean("TerrainClearanceV3"))return;var c=center(n);var level=p.serverLevel();
        for(int x=-MissingParentsTeaPlacement.CLEAR_RX;x<=MissingParentsTeaPlacement.CLEAR_RX;x++)for(int z=-MissingParentsTeaPlacement.CLEAR_RZ;z<=MissingParentsTeaPlacement.CLEAR_RZ;z++){
            if(Math.abs(x)<=TeaHouse.RX&&Math.abs(z)<=TeaHouse.RZ)continue;
            for(int y=0;y<=9;y++)level.setBlock(c.offset(x,y,z),Blocks.AIR.defaultBlockState(),2|16);
        }
    }
    /** The landing is part of the saved temporary footprint, including its headroom. */
    private static void clearEntrance(ServerPlayer p,CompoundTag n){
        var level=p.serverLevel();var c=center(n);var placed=n.getCompound("Placed");
        for(int x=-1;x<=1;x++)for(int z=-TeaHouse.RZ-2;z<-TeaHouse.RZ;z++)for(int y=-1;y<=3;y++){
            var at=c.offset(x,y,z);String key=Long.toString(at.asLong());var current=NbtUtils.writeBlockState(level.getBlockState(at));
            // On existing visits, preserve subsequent player edits and block entities.
            if(level.getBlockEntity(at)!=null||placed.contains(key)&&!placed.getCompound(key).equals(current))continue;
            var state=(y==-1?Blocks.STONE_BRICKS:Blocks.AIR).defaultBlockState();level.setBlock(at,state,2|16);
            if(n.contains("Placed"))placed.put(key,NbtUtils.writeBlockState(state));
        }
        n.putBoolean("EntranceClearedV2",true);
    }
    public static void tick(ServerPlayer p,VisitData.Scene s){
        if(!eligible(s)||p.serverLevel()!=p.server.overworld())return;var n=data(s);
        if(!n.getBoolean("Rolled"))select(p,s,false);
        cleanupPreviousVisit(p,n);claim(p,s);if(n.getBoolean("Completed")){if(p.tickCount%100==0)repairRemnants(p,s);return;}
        if(!n.getBoolean("Selected")){
            // This encounter gates the Cat/parents story. A missed initial roll
            // delays the invitation; it must not permanently erase the branch.
            if(!invitationReady(n)){if(p.tickCount%100==0)save(p,s,false);return;}
            save(p,s,true);
        }
        if(n.getBoolean("Building")){BobinskyVisitData.get(p.server).teaVisits.put(s.owner,n);finishBuild(p,s);return;}
        if(!n.getBoolean("Built")){if(p.tickCount%100==0)prepare(p,s);return;}
        BobinskyVisitData.get(p.server).teaVisits.put(s.owner,n);
        if(!owns(p,s,n))return;if(!n.getBoolean("EntranceClearedV2")){clearEntrance(p,n);save(p,s,true);}if(p.tickCount%80==0)BobinskyHouse.hold(p.serverLevel(),center(n));
        if(n.getBoolean("Cleaning")){if(safeToRemove(p,n)&&BobinskyHouse.unseen(p,n))cleanup(p,s);return;}
        if(n.getBoolean("Recover")){recover(p,s);return;}
        var state=phase(n);var spink=actor(p,n,"Spink");var forcible=actor(p,n,"Forcible");
        boolean music=state.ordinal()>=Phase.FOLLOW_SPINK.ordinal()&&BobinskyHouse.bounds(n).contains(p.position());
        if(music!=n.getBoolean("HouseMusicPlaying")){n.putBoolean("HouseMusicPlaying",music);if(music)VisitAudio.play(p,"tea_visit_music",true);else VisitAudio.stopMusic(p,"tea_visit_music");}
        if(state.ordinal()>=Phase.KNOCKING.ordinal()&&(spink==null||forcible==null)){spawn(p,s);return;}
        int t=n.getInt("Ticks");boolean done=false;String voice=sound(state);
        if(!voice.isEmpty()){
            if(!n.getBoolean("AudioStarted")){if(!ShowPlayback.start(p,s,voice,false)){n.putString("Blocker","Recording could not start: "+voice);return;}n.putBoolean("AudioStarted",true);}
            done=ShowPlayback.tick(p,s);t=ShowPlayback.position(s);n.putInt("Ticks",t);
            if(!ShowPlayback.data(s).getString("Blocker").isEmpty()){n.putString("Blocker",ShowPlayback.data(s).getString("Blocker"));release(p,n);open(p,n,true);hint(p,"Audio unavailable. Exit safely; reconnect or use Recover to retry this checkpoint.");return;}
        }else n.putInt("Ticks",++t);
        if(spink!=null){spink.pose(100+state.ordinal(),t);spink.storyClock(voice.isEmpty()||ShowPlayback.started(s));}
        if(forcible!=null){forcible.pose(100+state.ordinal(),t);forcible.storyClock(voice.isEmpty()||ShowPlayback.started(s));forcible.cup(n.getString("Bowl").equals("HAND"));forcible.storyMotion(n.getInt("BowlStep"));}
        if(!n.getBoolean("ExitUnlocked")&&((n.getBoolean("Seated")&&state.ordinal()<Phase.STORY_I.ordinal())||(state==Phase.STORY_I&&t<100)))seatPlayer(p,n);
        if(n.getBoolean("Inside")&&!n.getBoolean("ExitUnlocked")){open(p,n,false);if(!BobinskyHouse.bounds(n).contains(p.position())){var safe=center(n).offset(2,0,-3);p.teleportTo(safe.getX()+.5,safe.getY(),safe.getZ()+.5);}}
        switch(state){
            case WAITING_FOR_KNOCK->{if(p.distanceToSqr(BobinskyHouse.front(n).getCenter())<36)hint(p,"Right-click to knock.");else if(p.tickCount%400==0)TeaVisitTravel.invite(p);}
            case KNOCKING->{if(t==20)sfx(p,n,SoundEvents.WOOD_STEP,.55F);if(t>=45){if(walk(spink,point(n,.5,0,-7.5),.105)||t>=100){gaze(spink,p.getEyePosition());open(p,n,true);go(p,s,Phase.GREETING);}}}
            case GREETING->gaze(spink,p.getEyePosition());
            case FOLLOW_SPINK->{if(BobinskyHouse.bounds(n).deflate(.5).contains(p.position())&&p.getZ()>BobinskyHouse.door(n).getZ()+1.4){n.putBoolean("Inside",true);open(p,n,false);}if(n.getBoolean("Inside")){var chair=point(n,-2.5,.6,1.5);boolean beside=spink.position().subtract(chair).horizontalDistanceSqr()<1.6&&Math.abs(spink.getY()-chair.y)<1.5;if(spink.distanceToSqr(p)<64&&((beside&&seatSpink(p,n))||(route(spink,n,"LeadRoute",new double[][]{{.5,-3.5},{-2.5,-3.5},{-2.5,.5}},.1)&&seatSpink(p,n))))go(p,s,Phase.WAITING_FOR_SEAT);else if(p.tickCount%50<12)gaze(spink,p.getEyePosition());}hint(p,"Follow Miss Spink.");}
            case WAITING_FOR_SEAT->{seatSpink(p,n);gaze(spink,p.getEyePosition());hint(p,"Right-click the chair to sit.");}
            case FORCIBLE_ENTRANCE->{gaze(forcible,spink.getEyePosition());if(walk(forcible,point(n,-3.45,0,2.55),.095))go(p,s,Phase.STORY_A);}
            case STORY_A->{gaze(forcible,spink.getEyePosition());gaze(spink,t>=40?forcible.getEyePosition():p.getEyePosition());}
            case STORY_B->{walk(forcible,point(n,-2.35,0,2.65),.065);gaze(forcible,spink.getEyePosition());gaze(spink,forcible.getEyePosition());}
            case STORY_C->{var target=t>=20?p.getEyePosition():forcible.getEyePosition();gaze(spink,target);gaze(forcible,t>=20?p.getEyePosition():spink.getEyePosition());}
            case STORY_D->{fetchBowl(p,s,forcible,t);gaze(spink,forcible.getEyePosition());if(done&&!n.getBoolean("BowlPlaced"))done=false;}
            case STORY_E->{gaze(spink,p.getEyePosition());gaze(forcible,spink.getEyePosition());}
            case STORY_F->{int duration=ShowRecordings.duration(voice);float crush=crush(t,duration);n.putFloat("Crush",Math.max(n.getFloat("Crush"),crush));gaze(spink,p.getEyePosition());gaze(forcible,spink.getEyePosition());if(t>28&&t<duration&&t%6==0)sfx(p,n,SoundEvents.AMETHYST_BLOCK_STEP,.18F);if(done){n.putFloat("Crush",1);n.putBoolean("Powder",true);}}
            case STORY_G->{gaze(spink,p.getEyePosition());gaze(forcible,spink.getEyePosition());if(t>=20&&!n.getBoolean("Revealed")){n.putBoolean("Revealed",true);sfx(p,n,SoundEvents.PLAYER_BREATH,.35F);save(p,s,true);}}
            case STORY_H->{gaze(spink,p.getEyePosition());gaze(forcible,spink.getEyePosition());if(t>=10)award(p,s);}
            case STORY_I->{gaze(spink,p.getEyePosition());gaze(forcible,p.getEyePosition());if(t>=100&&!n.getBoolean("ExitUnlocked")){release(p,n);n.putBoolean("ExitUnlocked",true);open(p,n,true);save(p,s,true);} }
            case WAITING_FOR_EXIT->{gaze(spink,forcible.getEyePosition());gaze(forcible,spink.getEyePosition());n.putBoolean("ExitUnlocked",true);release(p,n);open(p,n,true);hint(p,"Leave Miss Spink and Miss Forcible's house.");if(!BobinskyHouse.bounds(n).inflate(.75).contains(p.position())){n.putBoolean("PlayerExited",true);if(BobinskyHouse.unseen(p,n)&&safeToRemove(p,n))cleanup(p,s);}}
            default->{}
        }
        bowl(p,s);if(done&&phase(n)==state){Phase next=state==Phase.GREETING?Phase.FOLLOW_SPINK:state==Phase.STORY_I?Phase.WAITING_FOR_EXIT:Phase.values()[state.ordinal()+1];go(p,s,next);}
        if(p.tickCount%20==0){for(String k:List.of("Spink","Forcible")){var e=actor(p,n,k);if(e!=null)n.putLong(k+"Position",e.blockPosition().asLong());}save(p,s,false);}
    }
    static void cutsceneSafety(ServerPlayer p,VisitData.Scene s){
        if(p.serverLevel()!=p.server.overworld())return;var n=data(s);var state=phase(n);String voice=sound(state);
        boolean movement=state==Phase.KNOCKING||state==Phase.FORCIBLE_ENTRANCE||state==Phase.FOLLOW_SPINK&&n.getBoolean("Inside");
        if(voice.isEmpty()&&!movement)return;
        if(!StoryCutsceneWatchdog.overdue(p,s,"post_escape_tea",state.name(),voice.isEmpty()?600:StoryCutsceneWatchdog.voiceBudget(voice)))return;
        ShowPlayback.safetyFinish(p,s,"Tea visit actor/recording exceeded its safety window");
        switch(state){
            case KNOCKING->{open(p,n,true);go(p,s,Phase.GREETING);}
            case FOLLOW_SPINK->{seatSpink(p,n);go(p,s,Phase.WAITING_FOR_SEAT);}
            case FORCIBLE_ENTRANCE->go(p,s,Phase.STORY_A);
            case GREETING->go(p,s,Phase.FOLLOW_SPINK);
            case STORY_D->{n.putString("Bowl","TABLE");n.putBoolean("BowlPlaced",true);n.putInt("BowlStep",4);bowl(p,s);go(p,s,Phase.STORY_E);}
            case STORY_F->{n.putFloat("Crush",1);n.putBoolean("Powder",true);go(p,s,Phase.STORY_G);}
            case STORY_G->{n.putBoolean("Revealed",true);go(p,s,Phase.STORY_H);}
            case STORY_H->{award(p,s);go(p,s,Phase.STORY_I);}
            case STORY_I->{n.putBoolean("ExitUnlocked",true);release(p,n);open(p,n,true);go(p,s,Phase.WAITING_FOR_EXIT);}
            default->{if(state.name().startsWith("STORY_"))go(p,s,Phase.values()[state.ordinal()+1]);}
        }
    }
    public static float crush(int t,int duration){return Math.max(0,Math.min(1,(t-28F)/Math.max(1,duration-28F)));}
    static void go(ServerPlayer p,VisitData.Scene s,Phase next){ShowPlayback.transition(p,s,sound(next));var n=data(s);n.putString("StoryPhase",next.name());n.putInt("Ticks",0);n.remove("AudioStarted");n.remove("Blocker");if(next==Phase.WAITING_FOR_EXIT&&n.getBoolean("FloatingLanding"))TeaVisitTravel.inviteReturn(p);save(p,s,true);}
    private static BlockPos center(CompoundTag n){return BlockPos.of(n.getLong("House"));}
    private static Vec3 point(CompoundTag n,double x,double y,double z){var c=center(n);return new Vec3(c.getX()+x,c.getY()+y,c.getZ()+z);}
    private static TeaVisitorEntity actor(ServerPlayer p,CompoundTag n,String key){
        if(!n.hasUUID(key)||!n.hasUUID("Token")||!n.hasUUID("Owner"))return null;
        if(n.contains(key+"Position"))p.server.overworld().getChunkAt(BlockPos.of(n.getLong(key+"Position")));
        var raw=p.server.overworld().getEntity(n.getUUID(key));
        if(!(raw instanceof TeaVisitorEntity e)||!List.of("Spink","Forcible").contains(key))return null;
        var type=(key.equals("Spink")?ModEntities.SPINK_VISITOR:ModEntities.FORCIBLE_VISITOR).get();
        return e.getType()==type
                &&n.getUUID("Token").equals(e.getPersistentData().getUUID("TeaToken"))
                &&n.getUUID("Owner").equals(e.getPersistentData().getUUID("TeaOwner"))?e:null;
    }
    /** Restore only a temporary tea-visit actor proven truly absent from
     * every native entity region. A saved UUID that is just unloaded or whose
     * file cannot be checked is NEVER authority for a duplicate spawn. */
    static boolean teaActorMissingProof(com.coraline.rebirth.LegacyActorRecovery.Result result){
        return result==com.coraline.rebirth.LegacyActorRecovery.Result.ABSENT;
    }
    private static boolean recoverAbsentVisitor(ServerPlayer p,VisitData.Scene s,CompoundTag n,String key){
        if(!owns(p,s,n)||n.getBoolean("Completed")||!n.hasUUID("Token")||!n.hasUUID(key)
                ||!List.of("Spink","Forcible").contains(key))return false;
        var original=n.getUUID(key);
        if(actor(p,n,key)!=null)return true;
        var level=p.server.overworld();
        var start=key.equals("Spink")?point(n,.5,0,-6.5):point(n,-5.5,0,2.5);
        var at=n.contains(key+"Position",Tag.TAG_LONG)?BlockPos.of(n.getLong(key+"Position")):BlockPos.containing(start);
        if(!level.getWorldBorder().isWithinBounds(at))return false;
        LoopProps.hold(level,at);
        for(var world:p.server.getAllLevels())if(world.getEntity(original)!=null){
            n.putString("Blocker","The recorded "+key+" identity is occupied by another actor.");
            return false;
        }
        var result=com.coraline.rebirth.LegacyActorRecovery.check(p.server,original);
        if(!teaActorMissingProof(result)){
            n.putString("Blocker","Waiting to verify the saved "+key+" identity: "+result);
            return false;
        }
        var actor=(key.equals("Spink")?ModEntities.SPINK_VISITOR:ModEntities.FORCIBLE_VISITOR).get().create(level);
        if(actor==null)return false;
        actor.setUUID(original);
        actor.setPos(Vec3.atBottomCenterOf(at));
        actor.getPersistentData().putUUID("TeaOwner",s.owner);
        actor.getPersistentData().putUUID("TeaToken",n.getUUID("Token"));
        actor.getPersistentData().putBoolean("MissingParentsTea",true);
        actor.setOnGround(true);
        if(!level.addFreshEntity(actor))return false;
        n.putLong(key+"Position",actor.blockPosition().asLong());
        n.remove("Blocker");save(p,s,true);
        return true;
    }
    private static void spawn(ServerPlayer p,VisitData.Scene s){
        var n=data(s);
        boolean created=false;
        for(String key:List.of("Spink","Forcible")){
            if(n.hasUUID(key)){
                if(actor(p,n,key)==null&&p.tickCount%20==0)recoverAbsentVisitor(p,s,n,key);
                continue;
            }
            var e=(key.equals("Spink")?ModEntities.SPINK_VISITOR:ModEntities.FORCIBLE_VISITOR).get().create(p.server.overworld());
            if(e==null)break;
            e.setPos(key.equals("Spink")?point(n,.5,0,-6.5):point(n,-5.5,0,2.5));
            e.getPersistentData().putUUID("TeaOwner",s.owner);
            e.getPersistentData().putUUID("TeaToken",n.getUUID("Token"));
            e.getPersistentData().putBoolean("MissingParentsTea",true);
            e.setOnGround(true);
            if(p.server.overworld().addFreshEntity(e)){
                n.putUUID(key,e.getUUID());n.putLong(key+"Position",e.blockPosition().asLong());
                created=true;
            }
        }
        // Missing saved actors can be probed on every tick of a stalled scene.
        // Do not save all server data on every probe; commit only new identities.
        // recoverAbsentVisitor() writes its own exact-UUID recovery checkpoint.
        if(created)save(p,s,true);
    }
    /** Collision-aware scripted walking in the established open lanes, with native pathfinding recovery. */
    private static boolean walk(TeaVisitorEntity e,Vec3 target,double speed){if(e==null)return false;if(e.isPassenger())e.stopRiding();target=walkFloor(e,target);var delta=target.subtract(e.position()).multiply(1,0,1);if(delta.lengthSqr()<.035){e.getNavigation().stop();return true;}e.setNoAi(false);e.setMaxUpStep(.6F);gaze(e,target.add(0,1.4,0));var step=delta.normalize().scale(Math.min(speed,delta.length()));if(e.level().noCollision(e,e.getBoundingBox().move(step))){e.getNavigation().stop();e.move(MoverType.SELF,step);e.setOnGround(true);}else if(e.tickCount%15==0||e.getNavigation().isDone())e.getNavigation().moveTo(target.x,target.y,target.z,.8);return false;}
    private static boolean route(TeaVisitorEntity e,CompoundTag n,String key,double[][] nodes,double speed){int i=Math.min(n.getInt(key),nodes.length-1);if(walk(e,point(n,nodes[i][0],0,nodes[i][1]),speed)){if(i==nodes.length-1)return true;n.putInt(key,i+1);}return false;}
    private static Vec3 walkFloor(TeaVisitorEntity actor,Vec3 target){
        var at=BlockPos.containing(target);var shape=actor.level().getBlockState(at).getCollisionShape(actor.level(),at);
        // Carpet is floor, not an impassable step. Keep exact horizontal marks
        // while walking above its actual collision surface.
        if(!shape.isEmpty()&&shape.max(Direction.Axis.Y)<=.125)target=target.add(0,shape.max(Direction.Axis.Y),0);
        return target;
    }
    private static void gaze(TeaVisitorEntity e,Vec3 target){if(e==null)return;e.getLookControl().setLookAt(target.x,target.y,target.z,12,10);{float yaw=(float)Math.toDegrees(Math.atan2(-(target.x-e.getX()),target.z-e.getZ()));e.setYRot(net.minecraft.util.Mth.approachDegrees(e.getYRot(),yaw,9));e.setYBodyRot(e.getYRot());e.setYHeadRot(e.getYRot());e.setXRot((float)-Math.toDegrees(Math.atan2(target.y-e.getEyeY(),Math.hypot(target.x-e.getX(),target.z-e.getZ()))));}}
    private static TeaSeatEntity seat(ServerPlayer p,CompoundTag n,String key,Vec3 at){if(n.hasUUID(key)){p.serverLevel().getChunkAt(BlockPos.containing(at));return p.serverLevel().getEntity(n.getUUID(key)) instanceof TeaSeatEntity seat?seat:null;}var seat=ModEntities.TEA_SEAT.get().create(p.serverLevel());if(seat==null)return null;seat.setPos(at);seat.getPersistentData().putUUID("TeaOwner",p.getUUID());seat.getPersistentData().putUUID("TeaToken",n.getUUID("Token"));if(!p.serverLevel().addFreshEntity(seat))return null;n.putUUID(key,seat.getUUID());return seat;}
    static boolean seatSpink(ServerPlayer p,CompoundTag n){var spink=actor(p,n,"Spink");if(spink==null)return false;spink.getNavigation().stop();spink.setJumping(false);spink.setDeltaMovement(Vec3.ZERO);var seat=seat(p,n,"SpinkSeat",point(n,-2.5,.6,1.5));if(seat==null)return false;if(spink.getVehicle()!=seat){spink.stopRiding();if(!spink.startRiding(seat,true))return false;}spink.setNoAi(true);spink.setYBodyRot(-90);spink.setYRot(-90);return true;}
    private static void seatPlayer(ServerPlayer p,CompoundTag n){var seat=seat(p,n,"Seat",point(n,1.5,.6,1.5));if(seat!=null){seat.policy(true,false);if(p.getVehicle()!=seat)p.startRiding(seat,true);}}
    public static void release(ServerPlayer p,CompoundTag n){n.putBoolean("Seated",false);if(p.getVehicle() instanceof TeaSeatEntity actual&&p.getUUID().equals(actual.getPersistentData().getUUID("TeaOwner"))){actual.policy(false,false);p.stopRiding();}if(n.hasUUID("Seat")&&p.server.overworld().getEntity(n.getUUID("Seat")) instanceof TeaSeatEntity seat){seat.policy(false,false);if(p.getVehicle()==seat)p.stopRiding();}n.putBoolean("Seated",false);p.setForcedPose(null);}
    public static boolean seatLocked(CompoundTag n){return n.getBoolean("Seated")&&!n.getBoolean("ExitUnlocked");}
    public static boolean canEnter(CompoundTag n){return phase(n).ordinal()>=Phase.FOLLOW_SPINK.ordinal()&&!n.getBoolean("Completed");}
    public static boolean block(ServerPlayer p,CompoundTag n,BlockPos at){if(!isVisit(n)||!n.getUUID("Owner").equals(p.getUUID()))return false;var s=OtherMotherLoop.scene(p);if(s==null||!owns(p,s,n)||p.getEyePosition().distanceToSqr(at.getCenter())>36)return true;if(BobinskyHouse.isDoor(n,at)&&phase(n)==Phase.WAITING_FOR_KNOCK){spawn(p,s);sfx(p,n,SoundEvents.WOOD_HIT,.8F);go(p,s,Phase.KNOCKING);p.displayClientMessage(Component.literal("You knocked. Miss Spink is coming to the door."),true);}else if(BobinskyHouse.isDoor(n,at)&&phase(n)==Phase.KNOCKING){hint(p,"Miss Spink is coming to the door.");}else if(phase(n)==Phase.WAITING_FOR_SEAT&&at.distSqr(center(n).offset(1,0,1))<=1){n.putBoolean("Seated",true);seatPlayer(p,n);p.setYRot(90);p.setXRot(4);go(p,s,Phase.FORCIBLE_ENTRANCE);}return true;}
    private static void open(ServerPlayer p,CompoundTag n,boolean value){var at=BobinskyHouse.door(n);var state=p.serverLevel().getBlockState(at);if(state.hasProperty(DoorBlock.OPEN)&&state.getValue(DoorBlock.OPEN)!=value){BobinskyHouse.open(p.serverLevel(),n,value);sfx(p,n,value?SoundEvents.WOODEN_DOOR_OPEN:SoundEvents.WOODEN_DOOR_CLOSE,.65F);var placed=n.getCompound("Placed");for(var pos:List.of(at,at.above()))placed.put(Long.toString(pos.asLong()),NbtUtils.writeBlockState(p.serverLevel().getBlockState(pos)));}}
    private static void fetchBowl(ServerPlayer p,VisitData.Scene s,TeaVisitorEntity forcible,int t){var n=data(s);var spink=actor(p,n,"Spink");int step=n.getInt("BowlStep");if(step==0){if(walk(forcible,point(n,-5.55,0,3.5),.135)){n.putInt("BowlStep",1);n.putInt("ReachTicks",0);}}else if(step==1){gaze(forcible,point(n,-6.4,1.4,2.5));int reach=n.getInt("ReachTicks")+1;n.putInt("ReachTicks",reach);if(reach>=16){n.putString("Bowl","HAND");n.putInt("BowlStep",2);save(p,s,true);}}else if(step==2){if(walk(forcible,point(n,-2.3,0,3.5),.135)){n.putInt("BowlStep",3);n.putInt("ReachTicks",0);}}else if(step==3){gaze(forcible,spink.getEyePosition());int reach=n.getInt("ReachTicks")+1;n.putInt("ReachTicks",reach);if(reach>=16){n.putString("Bowl","TABLE");n.putBoolean("BowlPlaced",true);n.putInt("BowlStep",4);save(p,s,true);}}}
    private static void bowl(ServerPlayer p,VisitData.Scene s){var n=data(s);var l=p.server.overworld();CinematicPropEntity prop=null;if(n.hasUUID("BowlProp")){var saved=l.getEntity(n.getUUID("BowlProp"));if(saved instanceof CinematicPropEntity e&&e.kind()==CinematicPropEntity.TAFFY_BOWL)prop=e;else n.remove("BowlProp");}if(prop==null){prop=CinematicPropEntity.TYPE.get().create(l);if(prop==null)return;prop.configure(s,CinematicPropEntity.TAFFY_BOWL,0);prop.setPos(point(n,-6.4,1.55,2.5));if(!l.addFreshEntity(prop))return;n.putUUID("BowlProp",prop.getUUID());}
        if(n.getString("Bowl").equals("HAND")){var f=actor(p,n,"Forcible");if(f!=null){prop.setPos(f.position().add(f.getLookAngle().multiply(.45,0,.45)).add(0,1.05,0));prop.setInvisible(true);}}else {prop.setInvisible(false);var spink=actor(p,n,"Spink");var target=spink==null?point(n,-.8,1.15,1):new Vec3(spink.getX()+spink.getLookAngle().x*.85,point(n,0,1.15,0).y,spink.getZ()+spink.getLookAngle().z*.85);prop.setPos(n.getBoolean("BowlPlaced")?target:point(n,-6.4,1.55,2.5));}
        int contents=n.getBoolean("Granted")?4:n.getBoolean("Revealed")?3:n.getBoolean("Powder")?2:1;prop.frame(contents,phase(n)==Phase.STORY_G?n.getInt("Ticks"):0,n.getFloat("Crush"));}
    public static void award(ServerPlayer p,VisitData.Scene s){if(!owns(p,s,data(s)))return;var n=data(s);if(s.loop.story.getBoolean("SeeingStoneReceipt")){n.putBoolean("Granted",true);return;}s.loop.story.putBoolean("SeeingStoneReceipt",true);s.loop.story.putBoolean("SeeingStonePending",true);n.putBoolean("Granted",true);save(p,s,true);claim(p,s);p.displayClientMessage(Component.literal("Received Seeing Stone"),true);}
    public static void claim(ServerPlayer p,VisitData.Scene s){if(!s.owner.equals(p.getUUID())||!s.loop.story.getBoolean("SeeingStonePending"))return;if(p.getInventory().getFreeSlot()<0){if(p.tickCount%100==0)hint(p,"Seeing Stone is reserved. Free an inventory slot to claim it.");return;}var stone=new ItemStack(ModItems.SEEING_STONE.get());stone.getOrCreateTag().putUUID("StoryOwner",s.owner);stone.getTag().putUUID("StoryDoor",s.door);SecretTransactions.give(p,stone);if(stone.isEmpty()){s.loop.story.putBoolean("SeeingStonePending",false);p.getInventory().setChanged();save(p,s,true);}else if(p.tickCount%100==0)hint(p,"Seeing Stone is reserved. Free an inventory slot to claim it.");}
    private static void cleanupPreviousVisit(ServerPlayer p,CompoundTag visit){
        if(!visit.contains("PreviousTeaVisit"))return;var previous=visit.getCompound("PreviousTeaVisit");
        if(previous.contains("House")&&(!p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,BobinskyHouse.bounds(previous).inflate(1)).isEmpty()||!BobinskyHouse.unseen(p,previous)))return;
        TeaVisit.cancel(p.server,previous,null);visit.remove("PreviousTeaVisit");BobinskyVisitData.get(p.server).setDirty();
    }
    private static boolean safeToRemove(ServerPlayer p,CompoundTag n){return p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,BobinskyHouse.bounds(n).inflate(1)).isEmpty();}
    private static void stopHouseMusic(ServerPlayer p,CompoundTag n){VisitAudio.stopMusic(p,"tea_visit_music");n.remove("HouseMusicPlaying");}
    private static void repairRemnants(ServerPlayer p,VisitData.Scene s){
        var n=data(s);if(n.getBoolean("RemnantRepairComplete")||!n.contains("Placed"))return;
        var placed=n.getCompound("Placed");var level=p.server.overworld();var lookup=level.holderLookup(net.minecraft.core.registries.Registries.BLOCK);boolean unloaded=false;
        for(String key:placed.getAllKeys()){BlockPos at;try{at=BlockPos.of(Long.parseLong(key));}catch(NumberFormatException invalid){continue;}
            if(!level.hasChunkAt(at)){unloaded=true;continue;}var state=level.getBlockState(at);var expected=NbtUtils.readBlockState(lookup,placed.getCompound(key));
            if(state.getBlock()!=expected.getBlock()||level.getBlockEntity(at)!=null)continue;
            if(state.getBlock() instanceof net.minecraft.world.level.block.StairBlock||state.getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock||state.getBlock() instanceof net.minecraft.world.level.block.SlabBlock){
                var below=level.getBlockState(at.below());level.setBlock(at,com.coraline.bobinsky.BobinskyHouse.natural(below)&&below.isFaceSturdy(level,at.below(),net.minecraft.core.Direction.UP)?net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            }
        }
        if(!unloaded){n.putBoolean("RemnantRepairComplete",true);save(p,s,false);}
    }
    private static void cleanup(ServerPlayer p,VisitData.Scene s){var n=data(s);if(!n.getBoolean("Granted")||!n.getBoolean("PlayerExited")||!safeToRemove(p,n)||!BobinskyHouse.unseen(p,n))return;stopHouseMusic(p,n);n.putBoolean("Cleaning",true);save(p,s,true);release(p,n);ShowPlayback.stop(p,s);
        for(String key:List.of("Spink","Forcible","Seat","SpinkSeat","BowlProp"))if(n.hasUUID(key)){var e=p.server.overworld().getEntity(n.getUUID(key));if(e!=null){e.ejectPassengers();e.discard();}}
        // Preserve any independently changed cell rather than overwriting a player's edit.
        var safe=new ListTag();var lookup=p.serverLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK);for(var raw:n.getList("Originals",10)){var cell=(CompoundTag)raw;var at=BlockPos.of(cell.getLong("Pos"));String key=Long.toString(at.asLong());if(n.getCompound("Placed").contains(key)&&p.serverLevel().getBlockState(at).getBlock()==NbtUtils.readBlockState(lookup,n.getCompound("Placed").getCompound(key)).getBlock())safe.add(cell.copy());}n.put("Originals",safe);save(p,s,true);BobinskyHouse.restore(p.server.overworld(),n);n.putBoolean("Completed",true);n.putBoolean("Active",false);n.putBoolean("Built",false);n.putString("StoryPhase",Phase.COMPLETED.name());s.loop.story.putString("MajorStoryPhase","MISSING_PARENTS_SPINK_AND_FORCIBLE_COMPLETE");save(p,s,true);
    }
    private static void recover(ServerPlayer p,VisitData.Scene s){var n=data(s);stopHouseMusic(p,n);BobinskyHouse.hold(p.serverLevel(),center(n));n.remove("Recover");ShowPlayback.stop(p,s);n.remove("AudioStarted");n.putInt("Ticks",0);spawn(p,s);if(phase(n).ordinal()>=Phase.FORCIBLE_ENTRANCE.ordinal()&&phase(n).ordinal()<=Phase.STORY_I.ordinal()&&!n.getBoolean("ExitUnlocked")){n.putBoolean("Seated",true);seatPlayer(p,n);seatSpink(p,n);open(p,n,false);}save(p,s,true);}
    @SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(active(s))data(s).putBoolean("Recover",true);}}
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(active(s)){stopHouseMusic(p,data(s));data(s).putBoolean("Recover",true);ShowPlayback.stop(p,s);save(p,s,true);}}}
    @SubscribeEvent public static void respawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(active(s))data(s).putBoolean("Recover",true);}}
    @SubscribeEvent public static void damage(net.minecraftforge.event.entity.living.LivingAttackEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(active(s)&&data(s).getBoolean("Inside")&&!data(s).getBoolean("ExitUnlocked"))e.setCanceled(true);}}
    private static void hint(ServerPlayer p,String text){if(p.tickCount%30==0)p.displayClientMessage(Component.literal(text),true);}
    private static void prompt(ServerPlayer p,String text){p.sendSystemMessage(Component.literal(text));}
    private static void sfx(ServerPlayer p,CompoundTag n,SoundEvent sound,float volume){p.serverLevel().playSound(null,center(n),sound,SoundSource.BLOCKS,volume,1);}
    public static Map<String,String> debugActions(){var actions=new LinkedHashMap<String,String>();actions.put("state","Inspect post-escape flags, chance, actors, reward and blocker");actions.put("complete_escape","Complete First Escape and Missing Parents Wybie (no reward)");actions.put("force","Force encounter / prepare original house");actions.put("clear","Clear unstarted pending selection");actions.put("house","Go to house entrance");actions.put("spawn","Prepare original house at a safe site");actions.put("knock","Perform actual nearby knock");actions.put("open","Open door for testing");actions.put("table","Go to actual table");actions.put("sit","Seat player and Spink");actions.put("bowl","Place bowl on table");actions.put("powder","Reduce taffy to powder");actions.put("reveal","Reveal stone beneath dust");actions.put("award","Execute once-only H reward cue");actions.put("exit","Release seat / open exit");actions.put("cleanup","Clean house only if empty and offscreen");actions.put("recover","Recover current checkpoint");actions.put("reset","Reset test encounter; keep reward receipt and existing stone");for(var state:Phase.values())actions.put("phase_"+state.name().toLowerCase(Locale.ROOT),"Preview "+state.name().replace('_',' '));return actions;}
    public static List<String> inspect(VisitData.Scene s){var n=data(s);return List.of("Event MISSING_PARENTS_SPINK_AND_FORCIBLE | "+phase(n),"Owner "+s.owner+" | eligible "+eligible(s),"Rolled "+n.getBoolean("Rolled")+" | selected "+n.getBoolean("Selected")+" | probability "+n.getDouble("Probability")+" | roll "+n.getDouble("Roll"),"House built "+n.getBoolean("Built")+" | active "+n.getBoolean("Active")+" | complete "+n.getBoolean("Completed"),"Seated "+n.getBoolean("Seated")+" | bowl "+n.getString("Bowl")+" | bowl step "+n.getInt("BowlStep"),"Crushed "+n.getFloat("Crush")+" | powder "+n.getBoolean("Powder")+" | revealed "+n.getBoolean("Revealed"),"Reward receipt "+s.loop.story.getBoolean("SeeingStoneReceipt")+" | pending "+s.loop.story.getBoolean("SeeingStonePending"),"Exit unlocked "+n.getBoolean("ExitUnlocked")+" | player exited "+n.getBoolean("PlayerExited"),"Recording "+sound(phase(n))+" | cursor "+ShowPlayback.position(s),"Blocker "+n.getString("Blocker"));}
    public static String debug(ServerPlayer p,VisitData.Scene s,String action){if(!p.hasPermissions(2)||!s.owner.equals(p.getUUID()))return "Owner/operator only.";var n=data(s);
        if(action.equals("state"))return "State refreshed.";
        if(action.equals("complete_escape"))return com.coraline.item.FirstEscapeDebugItem.activate(p,false);
        if(action.equals("force")||action.equals("spawn")){if(!eligible(s))return "Complete First Escape first.";select(p,s,true);return n.getBoolean("Built")?"Visit already exists; no duplicate created.":prepare(p,s)?"Original tea house prepared.":"No safe site; preparation remains pending.";}
        if(!eligible(s))return "Not eligible yet.";
        if(action.equals("clear")){if(n.getBoolean("Built"))return "Leave the live encounter and clean it safely first.";n.putBoolean("Selected",false);save(p,s,true);return "Pending selection cleared without reroll.";}
        if(action.equals("reset")){if(n.getBoolean("Built")){if(!n.getBoolean("PlayerExited")||!safeToRemove(p,n)||!BobinskyHouse.unseen(p,n))return "Exit and turn away before resetting the house.";boolean awarded=n.getBoolean("Granted");n.putBoolean("Granted",true);cleanup(p,s);n.putBoolean("Granted",awarded);}s.loop.story.remove(KEY);save(p,s,true);return "Test visit reset. Reward receipt, inventory and completed escape are unchanged.";}
        if(!n.getBoolean("Built"))return "Prepare the visit house first.";
        switch(action){case "house"->{var at=BobinskyHouse.front(n).north();p.teleportTo(at.getX()+.5,at.getY(),at.getZ()+.5);}case "table"->{var at=point(n,2.5,0,1.5);p.teleportTo(at.x,at.y,at.z);}case "knock"->block(p,n,BobinskyHouse.door(n));case "open"->open(p,n,true);case "sit"->{spawn(p,s);seatSpink(p,n);n.putBoolean("Seated",true);seatPlayer(p,n);go(p,s,Phase.FORCIBLE_ENTRANCE);}case "bowl"->{n.putString("Bowl","TABLE");n.putBoolean("BowlPlaced",true);n.putInt("BowlStep",4);}case "powder"->{n.putFloat("Crush",1);n.putBoolean("Powder",true);}case "reveal"->{n.putBoolean("Powder",true);n.putBoolean("Revealed",true);}case "award"->award(p,s);case "exit"->{n.putBoolean("ExitUnlocked",true);release(p,n);open(p,n,true);}case "cleanup"->cleanup(p,s);case "recover"->n.putBoolean("Recover",true);default->{if(action.startsWith("phase_")){var next=Phase.valueOf(action.substring(6).toUpperCase(Locale.ROOT));if(next==Phase.COMPLETED)return "Use guarded physical exit and cleanup.";spawn(p,s);go(p,s,next);}}}save(p,s,true);return "Owner control applied; reward receipts remain enforced.";
    }
    public static String completeForCatDebug(ServerPlayer p,VisitData.Scene s){
        if(!p.hasPermissions(2)||s==null||!s.owner.equals(p.getUUID())||!eligible(s)||!FirstEscapeStory.data(s).getBoolean("WybieDeparted"))return "Complete First Escape and normal Wybie's Missing Parents encounter first.";
        var n=data(s);
        if(n.getBoolean("Built")&&(!safeToRemove(p,n)||!BobinskyHouse.unseen(p,n)))return "Leave the existing tea house and look away before completing it for testing.";
        if(!n.hasUUID("Owner")){n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);n.putUUID("Token",UUID.randomUUID());}
        award(p,s);ShowPlayback.stop(p,s);
        if(n.getBoolean("Built")){n.putBoolean("PlayerExited",true);cleanup(p,s);}
        else{n.putBoolean("Completed",true);n.putBoolean("Active",false);n.putString("StoryPhase",Phase.COMPLETED.name());}
        save(p,s,true);return s.loop.story.getBoolean("SeeingStonePending")?"Visit completed; free a slot for the reserved Seeing Stone before the Cat can start.":"Second tea visit completed; the canonical Seeing Stone receipt is resolved.";
    }
    private MissingParentsTea(){}
}
