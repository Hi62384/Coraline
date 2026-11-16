package com.coraline.feast;

import com.coraline.entity.CharacterEntity;
import com.coraline.otherworld.*;
import com.coraline.parents.ParentEntity;
import com.coraline.secrets.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import static com.coraline.feast.OtherMotherLoopState.Phase.*;
import static com.coraline.feast.WybieAdventureLedger.Mode;

/** Twenty-minute event inside the existing visit director and existing MotherSearch arbitration. */
@Mod.EventBusSubscriber(modid="coraline")
public final class WybieAdventure {
    public static CompoundTag data(VisitData.Scene s){return s.loop.event.getCompound("WybieAdventure");}
    public static boolean visitsAllowed(VisitData.Scene s){return WybieOptionalVisits.allowed(s);}
    public static boolean eligible(ServerPlayer p,VisitData.Scene s){return s!=null&&p.getUUID().equals(s.owner)&&s.stage==VisitData.Stage.DONE&&OtherWorld.isOther(p.level())&&visitsAllowed(s)&&NumberedEventDialogue.ready("wybie_adventure")&&WybieAdventureAudio.introduction().ready()&&s.loop.suspicion<1&&!s.loop.secrets.searching()&&!Reconciliation.active(p)&&s.loop.secrets.search!=SecretsState.Search.SCRIPTED_TRAP;}
    public static VisitData.Scene active(ServerPlayer p){var s=OtherMotherLoop.active(p);return s!=null&&VisitEvents.type(s)==VisitEvents.Type.WYBIE_ADVENTURE&&s.loop.event.getBoolean("WybiePending")&&WybieAdventureLedger.owned(data(s),p.getUUID(),s.door,s.loop.event.getInt("Serial"))?s:null;}
    public static OtherMotherDialogue.Pool pool(String category,OtherMotherWelcome.State state){return new OtherMotherDialogue.Pool("events/wybie_adventure/"+category+"/"+SecretDialogue.stateId(state),2);}
    public static List<OtherMotherDialogue.Pool> pools(){var result=new ArrayList<OtherMotherDialogue.Pool>();for(String category:List.of("invitation","return","search","caught_normal","ban"))for(var state:OtherMotherWelcome.State.values())result.add(pool(category,state));return result;}
    public static boolean prepare(ServerPlayer p,VisitData.Scene s){
        if(!NumberedEventDialogue.ready("wybie_adventure")){p.sendSystemMessage(Component.literal("Wybie Adventure is waiting for its 50 authored Other Mother recordings to be exported and registered."));return false;}var original=WybieAdventureAudio.introduction();if(!original.ready()){p.sendSystemMessage(Component.literal("Wybie Adventure unavailable: "+original.problem()));return false;}if(!eligible(p,s))return false;
        var n=WybieAdventureLedger.begin(p.getUUID(),s.door,s.loop.event.getInt("Serial"),p.getRandom().nextLong());s.loop.event.put("WybieAdventure",n);s.loop.event.putBoolean("WybiePending",true);
        var door=findDoor(p);if(door==null){n.putString("Problem","The actual front door could not be found.");cleanup(p,s);return false;}n.putLong("FrontDoor",door.asLong());
        if(!WybieCourse.generate(p,s,n)){p.sendSystemMessage(Component.literal(n.getString("CourseProblem")));cleanup(p,s);return false;}
        n.putString("IntroductionSound",original.sound());n.putInt("IntroLength",original.ticks());n.put("Companion",new CompoundTag());ensureEntrance(p,s);if(WybieOptionalVisits.actor(p,n.getCompound("Companion"))==null){p.sendSystemMessage(Component.literal("Other Wybie could not reach a safe, unoccupied entrance. The adventure was not started."));cleanup(p,s);return false;}return true;
    }
    /** Runs only after the invitation finishes; the twenty-minute clock has not started. */
    public static void begin(ServerPlayer p,VisitData.Scene s){var n=data(s);n.putBoolean("Activated",true);OtherMotherLoop.phase(p,s,WYBIE_DOOR);WybieAdventureLedger.mode(n,Mode.DOOR);ensureEntrance(p,s);knock(p,s);OtherMotherLoop.changed(p);}
    private static BlockPos findDoor(ServerPlayer p){
        var realm=SecretWorld.realm(p);if(realm==null||realm.porch==null)return null;BlockPos best=null;double distance=Double.MAX_VALUE;
        for(var at:BlockPos.betweenClosed(realm.porch.offset(-6,-2,-6),realm.porch.offset(6,4,8))){if(!p.serverLevel().hasChunkAt(at))continue;var state=p.serverLevel().getBlockState(at);if(!(state.getBlock() instanceof DoorBlock)||state.getValue(DoorBlock.HALF)!=DoubleBlockHalf.LOWER)continue;double d=at.distSqr(realm.porch);if(d<distance){best=at.immutable();distance=d;}}
        return best;
    }
    private static void ensureEntrance(ServerPlayer p,VisitData.Scene s){var n=data(s);var lease=n.getCompound("Companion");var w=WybieOptionalVisits.actor(p,lease);var realm=SecretWorld.realm(p);if(realm==null)return;
        if(w==null){var at=BlockPos.of(n.getLong("FrontDoor"));var outward=new Vec3(at.getX()-realm.house.getX(),0,at.getZ()-realm.house.getZ()).normalize();var outside=BlockPos.containing(at.getCenter().add(outward.scale(2.5)));w=WybieOptionalVisits.acquire(p,s,lease,outside);}
        if(w!=null){w.getNavigation().stop();w.getPersistentData().putString("WybieCompanionMode","WAIT");var at=BlockPos.of(n.getLong("FrontDoor"));w.getLookControl().setLookAt(at.getX()+.5,at.getY()+1,at.getZ()+.5,35,25);float yaw=(float)Math.toDegrees(Math.atan2(-(at.getX()+.5-w.getX()),at.getZ()+.5-w.getZ()));w.setYRot(yaw);w.setYBodyRot(yaw);}
    }
    private static void knock(ServerPlayer p,VisitData.Scene s){var n=data(s);var at=BlockPos.of(n.getLong("FrontDoor"));p.serverLevel().playSound(null,at,SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,SoundSource.BLOCKS,.75F,1.35F);n.putLong("NextKnock",n.getLong("Clock")+240);n.putInt("KnockEcho",8);var m=MotherSearch.mother(p);if(m!=null)m.getLookControl().setLookAt(at.getX()+.5,at.getY()+1,at.getZ()+.5,35,25);p.displayClientMessage(Component.literal("Someone is waiting at the front door."),true);}
    /** Run once before dialogue/pursuit early-returns in OtherMotherLoop. Offline players do not tick. */
    public static void backgroundTick(ServerPlayer p,VisitData.Scene s){
        if(VisitEvents.type(s)!=VisitEvents.Type.WYBIE_ADVENTURE||!s.loop.event.getBoolean("WybiePending")||!WybieAdventureLedger.owned(data(s),p.getUUID(),s.door,s.loop.event.getInt("Serial")))return;
        var n=data(s);if(!p.isAlive()){suspend(p,s);return;}if(s.loop.suspicion>=1||s.loop.secrets.search==SecretsState.Search.SCRIPTED_TRAP){abort(p,s,"Story interruption");return;}
        if(!OtherWorld.isOther(p.level())){abort(p,s,"Left the Other World");return;}if(!n.getBoolean("Activated"))return;
        WybieAdventureLedger.tick(n);var mode=WybieAdventureLedger.mode(n);
        if(n.getInt("KnockEcho")>0){int echo=n.getInt("KnockEcho")-1;n.putInt("KnockEcho",echo);if(echo==0){var at=BlockPos.of(n.getLong("FrontDoor"));p.serverLevel().playSound(null,at,SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,SoundSource.BLOCKS,.6F,1.45F);}}
        if(WybieAdventureLedger.warning(n,6000,"FiveMinuteWarning"))warn(p,s,"Five minutes left. Wybie points toward home.",10);
        if(WybieAdventureLedger.warning(n,1200,"OneMinuteWarning"))warn(p,s,"One minute left. Return to Other Mother.",11);
        if(WybieAdventureLedger.timed(n)&&n.getInt("Remaining")==0&&!n.getBoolean("DeadlineTriggered")){
            n.putBoolean("DeadlineTriggered",true);failRun(p,s,"Time is up");
            if(home(p,s)){finish(p,s,"RETURN");return;}
            n.putBoolean("Late",true);WybieAdventureLedger.mode(n,Mode.SEARCH);s.loop.event.putBoolean("Searching",true);
            // No missed-activity grace roll. Do not overwrite a stronger witnessed chase.
            if(!s.loop.secrets.searching()){s.loop.secrets.search=SecretsState.Search.SEARCHING;s.loop.secrets.pendingTicks=0;s.loop.secrets.target=BlockPos.of(n.getLong("FrontDoor"));}
            n.putBoolean("SearchVoicePending",true);warn(p,s,"Your twenty minutes are over. Other Mother is coming to find you.",11);OtherMotherLoop.changed(p);
        }
        if(n.getBoolean("SearchVoicePending")&&!SecretDialogue.busy(p)&&!Reconciliation.active(p)){if(VisitEvents.speak(p,s,pool("search",SecretDialogue.state(p))))n.remove("SearchVoicePending");}
        if(WybieAdventureLedger.timed(n)){
            if(p.tickCount%20==0)ensureBicycles(p,s);controlCompanion(p,s);
            if(mode==Mode.LEAD)WybieCourse.tickScore(p,s,bike(p,s,false));
            var w=WybieOptionalVisits.actor(p,n.getCompound("Companion"));int missing=w==null?n.getInt("MissingWybie")+1:0;n.putInt("MissingWybie",missing);
            if(missing>=1200){abort(p,s,"Wybie could not be restored safely");return;}
        }
        if(p.tickCount%20==0){send(p,s);OtherMotherLoop.changed(p);}
    }
    public static boolean tick(ServerPlayer p,VisitData.Scene s){
        var n=data(s);if(!n.getBoolean("Pending"))return false;var mode=WybieAdventureLedger.mode(n);
        if(mode==Mode.DOOR){
            if(p.tickCount%40==0)ensureEntrance(p,s);
            if(n.getLong("Clock")>=n.getLong("NextKnock")&&n.getInt("ModeTicks")<WybieAdventureLedger.DOOR_TIMEOUT)knock(p,s);
            var w=WybieOptionalVisits.actor(p,n.getCompound("Companion"));var door=BlockPos.of(n.getLong("FrontDoor"));var state=p.serverLevel().getBlockState(door);
            boolean opened=state.getBlock() instanceof DoorBlock&&state.getValue(DoorBlock.OPEN);
            if(w!=null&&opened&&p.distanceToSqr(door.getCenter())<=36&&p.hasLineOfSight(w)&&n.getBoolean("OwnerOpenedDoor")){intro(p,s);return true;}
            if(n.getInt("ModeTicks")>=WybieAdventureLedger.DOOR_TIMEOUT){abort(p,s,"The visit at the door was not completed");return true;}return true;
        }
        if(mode==Mode.INTRO){
            if(n.getBoolean("ReplayIntro")){n.remove("ReplayIntro");n.putInt("IntroElapsed",0);VisitAudio.play(p,n.getString("IntroductionSound"),false);return true;}
            // The exact decoded recording duration is authoritative; client finish packets cannot skip it.
            n.putInt("IntroElapsed",n.getInt("IntroElapsed")+1);
            if(WybieAdventureLedger.introFinished(n)){OtherMotherLoop.phase(p,s,WYBIE_ACTIVE);ensureBicycles(p,s);var w=WybieOptionalVisits.actor(p,n.getCompound("Companion"));if(w!=null)w.gameGesture(2,60);}
            return true;
        }
        if(mode==Mode.ENDING)return endingTick(p,s);
        // Legitimate arrival at the house after departure can end naturally, without a reset loop.
        if(mode!=Mode.SEARCH&&!s.loop.secrets.searching()&&!Reconciliation.active(p)&&home(p,s)&&n.getBoolean("LeftHome")&&n.getLong("Clock")>n.getLong("LeaveHomeAt")+100){finish(p,s,"RETURN");return true;}
        if(!home(p,s)&&!n.getBoolean("LeftHome")){n.putBoolean("LeftHome",true);n.putLong("LeaveHomeAt",n.getLong("Clock"));}
        return true;
    }
    /** Wait for the leased original; unloaded, dead, foreign or cross-dimension actors cannot wave goodbye. */
    private static boolean endingTick(ServerPlayer p,VisitData.Scene s){
        if(active(p)!=s||p.isDeadOrDying()||p.isSleeping()||s.loop.event.getBoolean("ShowCleanupDeferred")||s.loop.suspicion>=1||s.loop.secrets.searching()||s.loop.secrets.search==SecretsState.Search.SCRIPTED_TRAP||Reconciliation.active(p)||SecretDialogue.busy(p))return true;
        var n=data(s);var lease=n.getCompound("Companion");var w=WybieOptionalVisits.actor(p,lease);
        if(w==null||!w.isAlive()||w.level()!=p.level()||!WybieOptionalVisits.owns(w,p,s,lease)){
            n.remove("FarewellStarted");n.putInt("FarewellTicks",0);
            int missing=n.getInt("EndingMissingTicks")+1;n.putInt("EndingMissingTicks",missing);
            if(missing>=1200)abort(p,s,"Wybie could not return for his farewell");
            return true;
        }
        n.remove("EndingMissingTicks");
        if(!n.getBoolean("ClosingSpoken")){
            String category=n.getString("EndReason").equals("BAN")?"ban":n.getString("EndReason").equals("LATE")?"caught_normal":"return";
            var mother=MotherSearch.mother(p);if(mother==null||mother.distanceToSqr(p)>100||!mother.hasLineOfSight(p)){if(n.getInt("ModeTicks")>1200)abort(p,s,"Closing conversation unavailable");return true;}
            if(!VisitEvents.speak(p,s,pool(category,OtherMotherWelcome.State.fromId(n.getString("ClosingState")))))return true;
            n.putBoolean("ClosingSpoken",true);
        }
        if(!n.getBoolean("FarewellStarted")){
            n.putBoolean("FarewellStarted",true);n.putInt("FarewellTicks",0);n.remove("FarewellAt");
            w.stopRiding();w.getNavigation().stop();w.gameGesture(n.getString("EndReason").equals("BAN")?12:1,80);OtherMotherLoop.changed(p);return true;
        }
        int farewell=n.getInt("FarewellTicks")+1;n.putInt("FarewellTicks",farewell);if(farewell<80)return true;
        n.putBoolean("Completed",n.getString("EndReason").equals("RETURN"));
        WybieAdventureRewards.settle(p,n);WybieOptionalVisits.retire(p,lease);cleanup(p,s);VisitEvents.next(p,s,false);return true;
    }
    private static void intro(ServerPlayer p,VisitData.Scene s){var n=data(s);var recording=WybieAdventureAudio.introduction();if(!recording.ready()){abort(p,s,recording.problem());return;}WybieAdventureLedger.mode(n,Mode.INTRO);OtherMotherLoop.phase(p,s,WYBIE_INTRO);n.putInt("IntroElapsed",0);n.putInt("IntroLength",recording.ticks());VisitAudio.play(p,recording.sound(),false);var w=WybieOptionalVisits.actor(p,n.getCompound("Companion"));if(w!=null)w.gameGesture(1,60);OtherMotherLoop.changed(p);}
    public static WybieBicycleEntity bike(ServerPlayer p,VisitData.Scene s,boolean friend){var n=data(s);String key=friend?"FriendBike":"PlayerBike";return n.hasUUID(key)&&p.serverLevel().getEntity(n.getUUID(key)) instanceof WybieBicycleEntity b&&b.ownedBy(p,s)?b:null;}
    private static void ensureBicycles(ServerPlayer p,VisitData.Scene s){
        var n=data(s);var course=WybieCourse.get(n);var w=WybieOptionalVisits.actor(p,n.getCompound("Companion"));if(course==null||w==null)return;
        for(boolean friend:new boolean[]{false,true}){if(bike(p,s,friend)!=null)continue;String key=friend?"FriendBike":"PlayerBike";
            if(n.hasUUID(key)){
                if(!n.contains(key+"Position")||!p.serverLevel().hasChunkAt(BlockPos.of(n.getLong(key+"Position"))))continue;
                int missing=n.getInt(key+"Missing")+20;n.putInt(key+"Missing",missing);if(missing<200)continue;
            }
            var first=course.gates().get(0);var side=new Vec3(-first.normal().z,0,first.normal().x);var at=course.start().subtract(first.normal().scale(2)).add(side.scale(friend?1.15:-1.15));
            var surface=WybieCourse.surface(p.serverLevel(),at.x,at.z,(int)at.y);if(surface==null)continue;
            var b=WybieAdventureEntities.BICYCLE.get().create(p.serverLevel());if(b==null)continue;b.configure(p.getUUID(),s.door,n.getUUID("Session"),s.loop.event.getInt("Serial"),friend?w.getUUID():p.getUUID(),friend);b.setPos(surface);b.setYRot((float)Math.toDegrees(Math.atan2(-first.normal().x,first.normal().z)));
            if(!p.serverLevel().noCollision(b,b.getBoundingBox()))continue;n.putUUID(key,b.getUUID());n.putLong(key+"Position",b.blockPosition().asLong());if(!p.serverLevel().addFreshEntity(b)){n.remove(key);continue;}n.remove(key+"Missing");
        }
    }
    private static void controlCompanion(ServerPlayer p,VisitData.Scene s){
        var n=data(s);var lease=n.getCompound("Companion");var w=WybieOptionalVisits.actor(p,lease);if(w==null)return;lease.putLong("ActorPosition",w.blockPosition().asLong());var c=WybieCourse.get(n);var mode=WybieAdventureLedger.mode(n);var playerBike=bike(p,s,false);var friendBike=bike(p,s,true);
        if(c==null)return;boolean mounted=playerBike!=null&&p.getVehicle()==playerBike;
        if(mode==Mode.GARDEN){
            if(w.distanceToSqr(c.start())>9){w.getPersistentData().putString("WybieCompanionMode","GARDEN");if(w.tickCount%30==0)w.getNavigation().moveTo(c.start().x,c.start().y,c.start().z,1);}
            else if(friendBike!=null&&w.getVehicle()!=friendBike&&w.distanceToSqr(friendBike)<9)w.startRiding(friendBike);
            if(mounted){WybieAdventureLedger.mode(n,Mode.LEAD);n.putInt("LeadSample",0);n.putInt("PathProgress",0);}
        }
        boolean outside=c.distanceSquared(p.position())>12*12;int away=outside?n.getInt("CourseAway")+1:0;n.putInt("CourseAway",away);
        if((mode==Mode.LEAD||mode==Mode.GARDEN)&&away>60){failRun(p,s,"Exploring together");WybieAdventureLedger.mode(n,Mode.FOLLOW);n.putLong("SwitchAfter",n.getLong("Clock")+160);}
        mode=WybieAdventureLedger.mode(n);
        if(mode==Mode.LEAD&&friendBike!=null){
            if(w.getVehicle()!=friendBike){if(friendBike.tickCount<friendBike.navigationRetryAt)WybieOptionalVisits.follow(p,s,lease);else approachBike(w,friendBike);return;}if(!mounted||w.distanceToSqr(p)>18*18){friendBike.stop();if(w.tickCount%100==0)w.gameGesture(2,45);return;}
            int index=n.getInt("LeadSample");Vec3 target=c.samples().get(index);if(friendBike.distanceToSqr(target)<3){index=(index+2)%c.samples().size();n.putInt("LeadSample",index);target=c.samples().get(index);}
            rideToward(w,friendBike,target,w.distanceToSqr(p)>100?.45F:.75F);
            if(w.tickCount%120==0)w.getLookControl().setLookAt(p,25,20);return;
        }
        if(mode==Mode.FOLLOW||mode==Mode.SEARCH){
            if(mounted&&friendBike!=null){
                if(w.getVehicle()!=friendBike){
                    if(w.distanceToSqr(friendBike)>100||friendBike.tickCount<friendBike.navigationRetryAt){
                        if(w.tickCount%100==0&&!WybieOptionalVisits.visible(p,friendBike.position())){
                            var at=Reconciliation.safe(p,w.blockPosition().east(3));
                            if(at!=null&&!WybieOptionalVisits.visible(p,Vec3.atBottomCenterOf(at))&&p.serverLevel().noCollision(friendBike,friendBike.getDimensions(friendBike.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(at))))friendBike.teleportTo(at.getX()+.5,at.getY(),at.getZ()+.5);
                        }
                        WybieOptionalVisits.follow(p,s,lease);
                    }else approachBike(w,friendBike);
                }
                else if(w.distanceToSqr(p)<25)friendBike.stop();else if(w.tickCount%10==0||friendBike.stuckTicks>20){
                    var path=w.getNavigation().createPath(p,0);if(path==null||!path.canReach()){friendBike.stop();if(friendBike.stuckTicks>80)w.stopRiding();}
                    else{var node=path.getNode(Math.min(3,path.getNodeCount()-1));rideToward(w,friendBike,new Vec3(node.x+.5,node.y,node.z+.5),.8F);}
                }
                WybieDiscoveries.tick(p,s,w,lease);
            }else{
                if(friendBike!=null)friendBike.stop();if(w.isPassenger()&&(friendBike==null||friendBike.speed()<.03))w.stopRiding();WybieOptionalVisits.follow(p,s,lease);
            }
            if(mode==Mode.FOLLOW&&mounted&&n.getLong("Clock")>n.getLong("SwitchAfter")&&p.distanceToSqr(c.start())<16){WybieAdventureLedger.mode(n,Mode.LEAD);n.putInt("LeadSample",0);n.putBoolean("RunActive",false);n.putBoolean("HadBikePosition",false);}
        }
        if(mode==Mode.SEARCH&&w.tickCount%180==0)w.gameGesture(11,60);
    }
    private static void approachBike(CharacterEntity w,WybieBicycleEntity b){if(w.distanceToSqr(b)<4){w.getNavigation().stop();w.startRiding(b);}else if(w.tickCount%30==0)w.getNavigation().moveTo(b,1);b.stop();}
    private static void rideToward(CharacterEntity w,WybieBicycleEntity b,Vec3 target,float throttle){
        var delta=target.subtract(b.position());float targetYaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));float error=WybieBicyclePhysics.wrap(targetYaw-b.getYRot());
        var ahead=b.position().add(-Math.sin(Math.toRadians(b.getYRot()))*2,.5,Math.cos(Math.toRadians(b.getYRot()))*2);boolean blocked=b.level().clip(new net.minecraft.world.level.ClipContext(b.position().add(0,.5,0),ahead,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,b)).getType()!=HitResult.Type.MISS;
        int deltaTicks=Math.max(1,Math.min(20,b.tickCount-b.lastNavigationTick));b.lastNavigationTick=b.tickCount;b.navigationBlockedTicks=blocked?b.navigationBlockedTicks+deltaTicks:0;
        if(b.navigationBlockedTicks>=80){b.stop();w.stopRiding();b.navigationRetryAt=b.tickCount+200;b.navigationBlockedTicks=0;return;}
        // A tight turn keeps a little forward roll instead of braking into an unsteerable standstill.
        b.drive(blocked?0:Math.abs(error)>100?.16F:Math.abs(error)>55?.25F:throttle,Math.max(-1,Math.min(1,error/22)),blocked);w.getNavigation().stop();
    }
    public static void failRun(ServerPlayer p,VisitData.Scene s,String reason){var n=data(s);if(!n.getBoolean("RunActive"))return;int score=WybieAdventureLedger.failRun(n,reason);p.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(),SoundSource.PLAYERS,.35F,.9F);p.displayClientMessage(Component.literal(reason+" · Run "+score+" · Best "+n.getInt("BestScore")),true);var w=WybieOptionalVisits.actor(p,n.getCompound("Companion"));if(w!=null)w.gameGesture(5,45);}
    private static void warn(ServerPlayer p,VisitData.Scene s,String text,int gesture){p.displayClientMessage(Component.literal(text),true);var w=WybieOptionalVisits.actor(p,data(s).getCompound("Companion"));if(w!=null)w.gameGesture(gesture,65);}
    private static boolean home(ServerPlayer p,VisitData.Scene s){var realm=SecretWorld.realm(p);if(realm==null||realm.house==null||OtherWorldEstate.outsideHouse(realm.house,p.blockPosition())||Math.abs(p.getY()-realm.house.getY())>18)return false;var mother=MotherSearch.mother(p);return mother!=null&&mother.distanceToSqr(p)<36&&mother.hasLineOfSight(p)||!OtherWorldEstate.outsideHouse(realm.house,p.blockPosition())&&p.distanceToSqr(s.table.getCenter())<26*26;}
    public static boolean interact(ServerPlayer p,ParentEntity mother){var s=active(p);if(s==null||!mother.isMom()||!Objects.equals(mother.getUUID(),s.mom)||mother.distanceToSqr(p)>36||!mother.hasLineOfSight(p)||!WybieAdventureLedger.timed(data(s))||s.loop.secrets.searching()||SecretDialogue.busy(p)||Reconciliation.active(p))return false;
        if(MotherInterrupts.blackmail(p))return true;var n=data(s);UUID nonce=UUID.randomUUID();n.putUUID("ReturnNonce",nonce);n.putLong("ReturnOfferUntil",n.getLong("Clock")+400);var packet=new CompoundTag();packet.putBoolean("WybieReturnChoice",true);packet.putUUID("Nonce",nonce);WybieAdventureNetwork.send(p,packet);return true;
    }
    public static void choice(ServerPlayer p,UUID nonce,boolean finish){var s=active(p);if(s==null)return;var n=data(s);if(!n.hasUUID("ReturnNonce")||!nonce.equals(n.getUUID("ReturnNonce"))||n.getLong("Clock")>n.getLong("ReturnOfferUntil"))return;n.remove("ReturnNonce");if(finish&&home(p,s)&&!s.loop.secrets.searching()&&!Reconciliation.active(p)&&!SecretDialogue.busy(p))finish(p,s,"RETURN");}
    private static void finish(ServerPlayer p,VisitData.Scene s,String reason){var n=data(s);if(WybieAdventureLedger.mode(n)==Mode.ENDING)return;WybieAdventureLedger.end(n,reason);n.putString("ClosingState",SecretDialogue.state(p).id());var w=WybieOptionalVisits.actor(p,n.getCompound("Companion"));if(w!=null){w.getNavigation().stop();w.getPersistentData().putString("WybieCompanionMode","WAIT");}OtherMotherLoop.phase(p,s,WYBIE_ENDING);for(boolean friend:new boolean[]{false,true}){var b=bike(p,s,friend);if(b!=null){b.stop();b.ejectPassengers();}}OtherMotherLoop.changed(p);}
    /** Only invoke after the existing touching/line-of-sight catch guard. Still independently checks contact. */
    public static boolean caught(ServerPlayer p,OtherMotherWelcome.State state,Set<String> evidence,boolean forbiddenArea){
        var s=active(p);if(s==null)return false;var n=data(s);var m=MotherSearch.mother(p);boolean contact=m!=null&&m.getBoundingBox().inflate(.08,0,.08).intersects(p.getBoundingBox())&&m.hasLineOfSight(p);
        if(!contact||!WybieAdventureLedger.timed(n))return false;
        boolean restricted=evidence.stream().anyMatch(id->id.equals("personal_keyring")||SecretRegistry.ALL.containsKey(id));
        boolean ban=WybieAdventureLedger.banQualifies(n.getBoolean("Pending"),n.getBoolean("TimerStarted"),contact,p.getUUID().equals(s.owner),forbiddenArea,restricted);
        if(!ban&&!n.getBoolean("Late")&&n.getInt("Remaining")>0)return false;
        if(ban){s.loop.story.putBoolean("WybieVisitsForbidden",true);s.loop.story.putUUID("WybieBanSession",n.getUUID("Session"));var w=WybieOptionalVisits.actor(p,n.getCompound("Companion"));if(w!=null){w.getPersistentData().putBoolean("WybieFarewell",true);w.gameGesture(12,120);}}
        n.remove("SearchVoicePending");finish(p,s,ban?"BAN":"LATE");n.putString("ClosingState",state.id());n.putBoolean("Caught",true);n.putBoolean("ClosingSpoken",true);n.putLong("FarewellAt",n.getLong("Clock"));OtherMotherLoop.changed(p);
        // Return false: the existing serious/missed-return confrontation and safeguards still run.
        return false;
    }
    public static OtherMotherDialogue.Pool contactPool(ServerPlayer p,OtherMotherWelcome.State state){var s=active(p);if(s==null||!data(s).getBoolean("Caught"))return null;return pool(data(s).getString("EndReason").equals("BAN")?"ban":"caught_normal",state);}
    public static OtherMotherDialogue.Pool contactPool(ServerPlayer p,SecretDialogue.Event event,OtherMotherWelcome.State state){if(event!=SecretDialogue.Event.ORDINARY_AREA_CAUGHT&&event!=SecretDialogue.Event.SECRET_AREA_CAUGHT&&event!=SecretDialogue.Event.RESTRICTED_ITEM_CAUGHT)return null;return contactPool(p,state);}
    public static OtherMotherDialogue.Pool queuedPool(String path){return pools().stream().filter(pool->pool.path().equals(path)).findFirst().orElse(null);}
    private static WybieBicycleEntity savedBike(ServerPlayer p,VisitData.Scene s,boolean friend){var n=data(s);String key=friend?"FriendBike":"PlayerBike";if(!n.hasUUID(key))return null;for(var level:p.server.getAllLevels())if(level.getEntity(n.getUUID(key)) instanceof WybieBicycleEntity b&&b.ownedBy(p,s))return b;return null;}
    public static void suspend(ServerPlayer p,VisitData.Scene s){
        if(!s.loop.event.getBoolean("WybiePending")||!WybieAdventureLedger.owned(data(s),p.getUUID(),s.door,s.loop.event.getInt("Serial")))return;
        var n=data(s);n.remove("ReturnNonce");n.putBoolean("HadBikePosition",false);n.remove("FarewellStarted");n.putInt("FarewellTicks",0);WybieAdventureLedger.failRun(n,"Adventure interrupted");
        if(WybieAdventureLedger.mode(n)==Mode.INTRO){VisitAudio.stopVoice(p,n.getString("IntroductionSound"));n.putBoolean("ReplayIntro",true);}
        for(boolean friend:new boolean[]{false,true}){var b=savedBike(p,s,friend);if(b!=null){b.stop();b.ejectPassengers();}}
        var w=WybieOptionalVisits.actor(p,n.getCompound("Companion"));if(w!=null){w.stopRiding();w.getNavigation().stop();}
    }
    public static boolean resume(ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s==null||!s.loop.event.getBoolean("WybiePending"))return false;var mode=WybieAdventureLedger.mode(data(s));OtherMotherLoop.phase(p,s,mode==Mode.ENDING?WYBIE_ENDING:mode==Mode.DOOR?WYBIE_DOOR:mode==Mode.INTRO?WYBIE_INTRO:WYBIE_ACTIVE);return true;}
    public static void reconnect(ServerPlayer p,VisitData.Scene s){if(!s.loop.event.getBoolean("WybiePending")||!WybieAdventureLedger.owned(data(s),p.getUUID(),s.door,s.loop.event.getInt("Serial")))return;var n=data(s);n.remove("ReturnNonce");n.putBoolean("HadBikePosition",false);n.remove("FarewellStarted");n.putInt("FarewellTicks",0);if(WybieAdventureLedger.mode(n)==Mode.INTRO)n.putBoolean("ReplayIntro",true);WybieAdventureRewards.deliver(p);send(p,s);}
    private static void abort(ServerPlayer p,VisitData.Scene s,String reason){var n=data(s);n.putBoolean("Completed",false);n.putString("EndReason","ABORT");WybieAdventureLedger.failRun(n,reason);WybieAdventureRewards.settle(p,n);cleanup(p,s);p.sendSystemMessage(Component.literal("Wybie Adventure ended: "+reason+"."));if(s.loop.suspicion<1&&!s.loop.secrets.searching()&&!Reconciliation.active(p))VisitEvents.next(p,s,false);}
    public static void cleanup(ServerPlayer p,VisitData.Scene s){if(!s.loop.event.contains("WybieAdventure"))return;var n=data(s);WybieAdventureRewards.settle(p,n);for(boolean friend:new boolean[]{false,true}){var b=savedBike(p,s,friend);if(b!=null){b.stop();b.ejectPassengers();b.discard();}}WybieCourse.cleanup(p,s,n);for(var level:p.server.getAllLevels())WybieCourse.restoreLoaded(level,s);WybieOptionalVisits.release(p,n.getCompound("Companion"));n.putBoolean("Pending",false);s.loop.event.remove("WybiePending");s.loop.event.remove("WybieAdventure");var packet=new CompoundTag();packet.putBoolean("WybieStatus",true);packet.putBoolean("Close",true);WybieAdventureNetwork.send(p,packet);OtherMotherLoop.changed(p);}
    private static void send(ServerPlayer p,VisitData.Scene s){var n=data(s);var packet=new CompoundTag();packet.putBoolean("WybieStatus",true);packet.putString("Mode",n.getString("Mode"));packet.putInt("Remaining",n.getBoolean("TimerStarted")?n.getInt("Remaining"):-1);packet.putInt("CurrentScore",n.getInt("CurrentScore"));packet.putInt("BestScore",n.getInt("BestScore"));packet.putInt("Checkpoint",n.getInt("Checkpoint"));packet.putBoolean("Banned",s.loop.story.getBoolean("WybieVisitsForbidden"));WybieAdventureNetwork.send(p,packet);}
    public static void debugRequest(ServerPlayer p,UUID nonce,String command,String value){
        if(!p.hasPermissions(2)||!OtherWorld.isOther(p.level()))return;var s=OtherMotherLoop.active(p);if(s==null||!p.getUUID().equals(s.owner))return;
        var root=s.loop.event;long now=OtherMotherLoop.now(p);
        if(command.equals("open")){root.putUUID("WybieDebugNonce",UUID.randomUUID());root.putLong("WybieDebugUntil",now+12000);}
        else{if(!root.hasUUID("WybieDebugNonce")||!nonce.equals(root.getUUID("WybieDebugNonce"))||now>root.getLong("WybieDebugUntil")||now-root.getLong("WybieDebugLast")<2)return;root.putLong("WybieDebugLast",now);debug(p,command+","+value);}
        var packet=new CompoundTag();packet.putBoolean("WybieDebug",true);packet.putUUID("Nonce",root.getUUID("WybieDebugNonce"));packet.put("Event",data(s).copy());packet.putBoolean("Banned",s.loop.story.getBoolean("WybieVisitsForbidden"));packet.putBoolean("Introduced",s.loop.story.getBoolean("WybieIntroductionComplete"));packet.putString("Search",s.loop.secrets.search.name());
        var realm=SecretWorld.realm(p);if(realm!=null&&realm.house!=null)packet.putDouble("HomeDistance",Math.sqrt(p.distanceToSqr(realm.house.getCenter())));
        var w=WybieOptionalVisits.actor(p,data(s).getCompound("Companion"));if(w!=null)packet.putString("WybiePosition",w.blockPosition().toShortString());
        for(boolean friend:new boolean[]{false,true}){var b=bike(p,s,friend);packet.putString(friend?"FriendBike":"PlayerBike",b==null?"missing/unloaded":b.blockPosition().toShortString()+" speed="+String.format(java.util.Locale.ROOT,"%.2f",b.speed())+" mounted="+b.isVehicle());}WybieAdventureNetwork.send(p,packet);
    }
    public static void debug(ServerPlayer p,String input){
        if(!p.hasPermissions(2)||!OtherWorld.isOther(p.level()))return;var s=OtherMotherLoop.active(p);if(s==null)return;
        var args=input.split(",",2);String command=args[0],arg=args.length>1?args[1]:"0";
        if(command.equals("reset_ban")){s.loop.story.remove("WybieVisitsForbidden");s.loop.story.remove("WybieBanSession");OtherMotherLoop.changed(p);return;}
        if(command.equals("status")){p.sendSystemMessage(Component.literal("Wybie introduction complete="+s.loop.story.getBoolean("WybieIntroductionComplete")+", visits forbidden="+s.loop.story.getBoolean("WybieVisitsForbidden")+", search="+s.loop.secrets.search+", event="+data(s)));return;}
        if(command.equals("force")){VisitEvents.start(p,s,VisitEvents.Type.WYBIE_ADVENTURE);return;}
        if(command.equals("voice")){try{int i=Integer.parseInt(arg);if(i==50){var audio=WybieAdventureAudio.introduction();if(audio.ready())VisitAudio.play(p,audio.sound(),false);else p.sendSystemMessage(Component.literal(audio.problem()));}else if(i>=0&&i<50)VisitAudio.play(p,pools().get(i/2).sound(i%2),false);}catch(NumberFormatException ignored){}return;}
        if(active(p)==null)return;var n=data(s);var c=WybieCourse.get(n);var w=WybieOptionalVisits.actor(p,n.getCompound("Companion"));
        try{switch(command){
            case "door"->{WybieAdventureLedger.mode(n,Mode.DOOR);OtherMotherLoop.phase(p,s,WYBIE_DOOR);ensureEntrance(p,s);knock(p,s);}
            case "intro"->{if(w!=null)intro(p,s);}
            case "actor"->ensureEntrance(p,s);
            case "bikes"->ensureBicycles(p,s);
            case "garden"->{if(c!=null){var at=Reconciliation.safe(p,BlockPos.containing(c.start()));if(at!=null)p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),0);}}
            case "regenerate"->{failRun(p,s,"Debug regeneration");WybieCourse.cleanup(p,s,n);n.putLong("Seed",p.getRandom().nextLong());WybieCourse.generate(p,s,n);}
            case "mount_player"->{var b=bike(p,s,false);if(b!=null&&p.distanceToSqr(b)<9)p.startRiding(b);}
            case "mount_wybie"->{var b=bike(p,s,true);if(w!=null&&b!=null)approachBike(w,b);}
            case "lead"->{WybieAdventureLedger.mode(n,Mode.LEAD);n.putInt("LeadSample",0);}
            case "follow"->{failRun(p,s,"Exploring together");WybieAdventureLedger.mode(n,Mode.FOLLOW);}
            case "dismount"->{if(w!=null)w.stopRiding();}
            case "checkpoint"->{if(c!=null&&n.getBoolean("RunActive"))WybieAdventureLedger.checkpoint(n,n.getInt("Checkpoint"),c.gates().size(),100);}
            case "current"->{int score=Math.max(0,Math.min(1_000_000,Integer.parseInt(arg)));n.putInt("CurrentScore",score);n.putInt("BestScore",Math.max(score,n.getInt("BestScore")));}
            case "best"->n.putInt("BestScore",Math.max(0,Math.min(1_000_000,Integer.parseInt(arg))));
            case "failure"->failRun(p,s,"Debug course failure");
            case "reward"->{int tier=Math.max(0,Math.min(4,Integer.parseInt(arg)));p.sendSystemMessage(Component.literal("Tier "+tier+": "+WybieAdventureRewards.prize(tier)));}
            case "rat"->SecretSpies.rat(p,p.blockPosition().north(5));
            case "doll"->SecretSpies.doll(p,p.blockPosition().north(5));
            case "gesture"->{if(w!=null)w.gameGesture(Math.max(0,Math.min(12,Integer.parseInt(arg))),100);}
            case "time"->n.putInt("Remaining",Math.max(0,Math.min(WybieAdventureLedger.DURATION,Integer.parseInt(arg))));
            case "expire"->n.putInt("Remaining",0);
            case "search"->{n.putInt("Remaining",0);n.remove("DeadlineTriggered");}
            case "catch_normal","catch_forbidden"->{
                boolean forbidden=SecretDialogue.secretArea(p,p.blockPosition());for(int i=0;i<p.getInventory().getContainerSize();i++)forbidden|=!SecretTransactions.evidence(p,p.getInventory().getItem(i)).isEmpty();
                boolean wanted=command.equals("catch_forbidden");if(forbidden!=wanted){p.sendSystemMessage(Component.literal(wanted?"Stand in an existing forbidden area or carry registered evidence first.":"Use an ordinary area and remove registered evidence first."));break;}
                var mother=MotherSearch.mother(p);if(mother!=null){n.putBoolean("Late",true);mother.teleportTo(p.getX(),p.getY(),p.getZ());MotherSearch.catchPlayer(p);}
            }
            case "capture"->{var m=MotherSearch.mother(p);if(m!=null&&m.getBoundingBox().inflate(.08,0,.08).intersects(p.getBoundingBox())&&m.hasLineOfSight(p))MotherSearch.catchPlayer(p);else p.sendSystemMessage(Component.literal("Place Mother in physical contact first; debug capture does not bypass the contact guard."));}
            case "evidence"->{if(SecretRegistry.ALL.containsKey(arg))SecretTransactions.issue(p,arg);}
            case "ban"->{s.loop.story.putBoolean("WybieVisitsForbidden",true);finish(p,s,"BAN");if(w!=null){w.getPersistentData().putBoolean("WybieFarewell",true);w.gameGesture(12,100);}}
            case "loot"->{var boxes=s.loop.secrets.world.getCompound("WildernessLootBoxes");for(String key:boxes.getAllKeys()){var at=BlockPos.of(Long.parseLong(key));if(p.serverLevel().hasChunkAt(at)&&p.distanceToSqr(at.getCenter())<48*48){var safe=Reconciliation.safe(p,at.north(3));if(safe!=null){p.teleportTo(p.serverLevel(),safe.getX()+.5,safe.getY(),safe.getZ()+.5,p.getYRot(),0);break;}}}}
            case "structure"->{for(var room:SecretWorld.rooms(s.loop.secrets)){var at=BlockPos.of(room.getLong("Pos"));if(!p.serverLevel().hasChunkAt(at))continue;var safe=Reconciliation.safe(p,at.north(7));if(safe!=null){p.teleportTo(p.serverLevel(),safe.getX()+.5,safe.getY(),safe.getZ()+.5,p.getYRot(),0);break;}}}
            case "complete"->{if(home(p,s))finish(p,s,"RETURN");}
            default->{}
        }}catch(NumberFormatException ignored){}OtherMotherLoop.changed(p);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void door(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock e){if(!(e.getEntity() instanceof ServerPlayer p)||e.getHand()!=InteractionHand.MAIN_HAND||e.isCanceled())return;var s=active(p);if(s==null||WybieAdventureLedger.mode(data(s))!=Mode.DOOR)return;var at=BlockPos.of(data(s).getLong("FrontDoor"));if((e.getPos().equals(at)||e.getPos().equals(at.above()))&&p.distanceToSqr(at.getCenter())<36)data(s).putBoolean("OwnerOpenedDoor",true);}
    @SubscribeEvent public static void restore(net.minecraftforge.event.level.ChunkEvent.Load e){if(!(e.getLevel() instanceof net.minecraft.server.level.ServerLevel level)||!OtherWorld.isOther(level))return;level.getServer().execute(()->{for(var s:VisitData.get(level.getServer()).scenes.values())WybieCourse.restoreLoaded(level,s);});}
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s!=null){suspend(p,s);OtherMotherLoop.changed(p);}}}
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent e){WybieCourse.clearCache();WybieAdventureAudio.clear();}
    private WybieAdventure(){}
}
