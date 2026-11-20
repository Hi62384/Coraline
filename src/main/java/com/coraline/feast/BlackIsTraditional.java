package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.parents.ParentEntity;
import com.coraline.secrets.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** One persistent major-story track in the existing owner's ledger, never a random activity. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="coraline")
public final class BlackIsTraditional {
    public enum Phase { INTRO_A,INTRO_B,FOLLOW_PARENTS,WAITING_FOR_SEAT,C,WAITING_FOR_GIFT_OPEN,GIFT_OPENED,D,E,F,G,H,I,J,K,AWAITING_CHOICE,L,RUN_UPSTAIRS,M,GO_TO_BED,N,O,DARK_HOUSE,P,Q,R,S,STUDY_REVEAL_COMPLETE }
    private static final String KEY="BlackIsTraditional";
    public static CompoundTag data(VisitData.Scene s){if(!s.loop.story.contains(KEY))s.loop.story.put(KEY,new CompoundTag());return s.loop.story.getCompound(KEY);}
    public static boolean active(VisitData.Scene s){return s!=null&&s.loop.phase==OtherMotherLoopState.Phase.BLACK_IS_TRADITIONAL&&data(s).contains("Phase");}
    public static boolean active(ServerPlayer p){return active(OtherMotherLoop.active(p));}
    public static Phase phase(VisitData.Scene s){try{return Phase.valueOf(data(s).getString("Phase"));}catch(IllegalArgumentException bad){return Phase.INTRO_A;}}
    public static boolean proposalSeated(Phase phase){return phase.ordinal()>=Phase.C.ordinal()&&phase.ordinal()<=Phase.AWAITING_CHOICE.ordinal();}
    static boolean locksSeat(ScenePropEntity chair){
        if(chair.level().isClientSide||chair.isRemoved()||chair.kind()!=ScenePropEntity.CHAIR||chair.slot!=0||chair.owner==null||!OtherWorld.isOther(chair.level()))return false;
        var s=VisitData.get(chair.getServer()).scenes.get(chair.owner);var tag=chair.getPersistentData();
        return active(s)&&Objects.equals(s.door,chair.storyDoor)&&tag.contains("VisitEventSerial")&&tag.getInt("VisitEventSerial")==s.loop.event.getInt("Serial")
                &&!FirstEscapeStory.engaged(s)&&proposalSeated(phase(s))&&!data(s).getBoolean("SeatRelease");
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void proposalDismount(net.minecraftforge.event.entity.EntityMountEvent event){
        if(event.isDismounting()&&event.getEntityMounting() instanceof ServerPlayer p&&p.isAlive()&&!p.isRemoved()&&!p.isSpectator()
                &&event.getEntityBeingMounted() instanceof ScenePropEntity chair&&p.getUUID().equals(chair.owner)&&locksSeat(chair)){
            event.setCanceled(true);chair.refreshProposalSeatLock();
            if(p.connection!=null)p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(chair));
        }
    }
    private static void releaseSeat(ServerPlayer p,VisitData.Scene s){
        var n=data(s);n.putBoolean("SeatRelease",true);
        try{if(p.getVehicle() instanceof ScenePropEntity chair)chair.refreshProposalSeatLock();p.stopRiding();}finally{n.remove("SeatRelease");}
    }
    private static void restoreProposalSeat(ServerPlayer p,VisitData.Scene s){
        if(!proposalSeated(phase(s))||p.isPassenger()||p.isSpectator())return;
        var chair=prop(p,s,ScenePropEntity.CHAIR,0);
        if(chair!=null&&locksSeat(chair)&&!chair.isVehicle()&&p.distanceToSqr(chair)<16){chair.refreshProposalSeatLock();p.startRiding(chair,true);}
    }
    public static String sound(Phase phase){String letter=phase==Phase.INTRO_A?"a":phase==Phase.INTRO_B?"b":phase.name().length()==1?phase.name().toLowerCase(Locale.ROOT):"";return letter.isEmpty()?"":"black_is_traditional_"+letter;}
    public static boolean threshold(float trust){return OtherMotherLoopState.clamp(trust)>=1F-0.000001F;}
    public static boolean pending(VisitData.Scene s){return !data(s).getBoolean("Started")&&(threshold(s.loop.trust)||threshold(s.loop.suspicion)||s.loop.story.getBoolean("BlackIsTraditionalQueued"));}
    /** Invoked only at an ordinary event boundary. Failed cleanup retains the boundary for retry. */
    public static boolean begin(ServerPlayer p,VisitData.Scene s){
        if(active(s))return true;
        if(s.owner.equals(p.getUUID())&&OtherWorld.isOther(p.level())&&pending(s)&&threshold(s.loop.suspicion)){Reconciliation.cancel(p);s.loop.secrets.search=SecretsState.Search.NORMAL;s.loop.secrets.pendingTicks=0;}
        if(!s.owner.equals(p.getUUID())||!OtherWorld.isOther(p.level())||!pending(s)||s.loop.secrets.searching()||s.loop.secrets.search==SecretsState.Search.SCRIPTED_TRAP||Reconciliation.active(p)||CatEncounter.active(p))return false;
        var realm=SecretWorld.realm(p);if(realm==null||realm.bed==null||realm.table==null||realm.piano==null)return false;
        for(char c='a';c<='s';c++)if(ShowRecordings.duration("black_is_traditional_"+c)<=0)return false;
        if(!VisitEvents.cleanup(p,s))return true;
        VisitAudio.stop(p);s.table=realm.table;s.piano=realm.piano;s.loop.event.putInt("Serial",s.loop.event.getInt("Serial")+1);s.loop.event.putString("Type",VisitEvents.Type.ACTIVITY.name());
        var n=data(s);n.putBoolean("Started",true);n.putLong("Bed",realm.bed.asLong());n.putLong("StudyDoor",OtherWorldEstate.studyDoor(realm.house).asLong());n.putLong("Kitchen",RosewoodHouse.householdStops(realm.house).get(0).asLong());n.putLong("SafePlayer",p.blockPosition().asLong());
        var forward=p.getLookAngle().multiply(1,0,1).normalize();var side=new Vec3(forward.z,0,-forward.x);
        for(int i=0;i<2;i++){var near=BlockPos.containing(p.position().add(forward.scale(3.5)).add(side.scale(i==0?-1.2:1.2)));var safe=introFloor(p,near);if(safe!=null)n.putLong(i==0?"MotherIntro":"FatherIntro",safe.asLong());}
        s.loop.phase=OtherMotherLoopState.Phase.BLACK_IS_TRADITIONAL;VisitEvents.ensureFather(p,s);go(p,s,Phase.INTRO_A);return true;
    }
    static void go(ServerPlayer p,VisitData.Scene s,Phase next){
        if(next==Phase.DARK_HOUSE||next==Phase.STUDY_REVEAL_COMPLETE)BedroomBarricade.clear(p,s);
        ShowPlayback.transition(p,s,sound(next));var n=data(s);n.putString("Phase",next.name());n.putInt("Ticks",0);n.remove("AudioStarted");n.putUUID("Choice",UUID.randomUUID());n.remove("Blocker");
        if(next==Phase.L){n.putBoolean("Rejected",true);n.remove("RunCueReached");p.stopRiding();n.putLong("SafePlayer",p.blockPosition().asLong());}
        if(next==Phase.N){p.stopRiding();p.getPersistentData().remove("CoralineWakeAt");p.startSleeping(BlockPos.of(n.getLong("Bed")));}
        if(next==Phase.DARK_HOUSE){p.stopSleepInBed(true,false);p.setForcedPose(null);n.putBoolean("SleepDisabled",true);darken(p,s);var realm=SecretWorld.realm(p);var mom=MotherSearch.mother(p);if(mom!=null){mom.stopRiding();mom.getNavigation().stop();var safe=OtherWorld.safePlayer(p.serverLevel(),HouseCabinets.at(realm.house,23,6,44));mom.setPos(Vec3.atBottomCenterOf(safe));}for(var at:BlockPos.betweenClosed(realm.house.offset(-30,0,-36),realm.house.offset(30,4,30)))if(p.serverLevel().getBlockState(at).getBlock() instanceof DoorBlock&&lockedDoor(p,s,at))closeDoor(p,at.immutable());}
        if(next==Phase.P){var at=BlockPos.of(n.getLong("StudyDoor"));closeDoor(p,at);n.putBoolean("StudyLocked",true);}
        if(next==Phase.STUDY_REVEAL_COMPLETE){p.stopRiding();p.setForcedPose(null);n.putBoolean("StudyRevealComplete",true);retirePresentation(p,s);}
        var chair=prop(p,s,ScenePropEntity.CHAIR,0);if(chair!=null)chair.refreshProposalSeatLock();
        OtherMotherLoop.changed(p);sync(p,s);
    }
    static ScenePropEntity prop(ServerPlayer p,VisitData.Scene s,int kind,int slot){return VisitEvents.props(p,s).stream().filter(e->e.kind()==kind&&e.slot==slot).findFirst().orElse(null);}
    static Vec3 seat(VisitData.Scene s,int slot){return Vec3.atBottomCenterOf(s.table).add(slot==0?0:slot==1?-2.35:2.35,0,slot==0?-5.05:-2.6);}
    static void ensure(ServerPlayer p,VisitData.Scene s){
        var n=data(s);LoopProps.hold(p.serverLevel(),s.table);LoopProps.hold(p.serverLevel(),s.piano);LoopProps.hold(p.serverLevel(),BlockPos.of(n.getLong("Bed")));
        if(prop(p,s,ScenePropEntity.TABLE,0)==null)VisitEvents.prop(p,s,ScenePropEntity.TABLE,Vec3.atBottomCenterOf(s.table),0).longDining(true);
        for(int i=0;i<3;i++){var chair=prop(p,s,ScenePropEntity.CHAIR,i);if(chair==null)chair=VisitEvents.prop(p,s,ScenePropEntity.CHAIR,seat(s,i),i);if(n.getInt("SeatLayoutRevision")<3){var at=seat(s,i);if(p.serverLevel().noCollision(new net.minecraft.world.phys.AABB(at.x-.4,at.y+.05,at.z-.4,at.x+.4,at.y+2,at.z+.4)))chair.setPos(at);}chair.setYRot(i==0?0:i==1?-90:90);}n.putInt("SeatLayoutRevision",3);
        if(prop(p,s,ScenePropEntity.PIANO,0)==null)VisitEvents.prop(p,s,ScenePropEntity.PIANO,Vec3.atBottomCenterOf(s.piano),0);
        if(!n.hasUUID("Gift")){var gift=CinematicPropEntity.TYPE.get().create(p.serverLevel());if(gift!=null){gift.configure(s,CinematicPropEntity.BUTTON_GIFT,0);gift.setPos(Vec3.atBottomCenterOf(s.table).add(0,.97,-2));if(p.serverLevel().addFreshEntity(gift))n.putUUID("Gift",gift.getUUID());}}
        if(!n.hasUUID("Hand")){var hand=CinematicPropEntity.TYPE.get().create(p.serverLevel());if(hand!=null){hand.configure(s,CinematicPropEntity.PIANO_HAND,0);hand.setPos(Vec3.atBottomCenterOf(s.piano));if(p.serverLevel().addFreshEntity(hand))n.putUUID("Hand",hand.getUUID());}}
    }
    private static CinematicPropEntity actor(ServerPlayer p,VisitData.Scene s,String key){var n=data(s);return n.hasUUID(key)&&p.serverLevel().getEntity(n.getUUID(key)) instanceof CinematicPropEntity e&&e.owned(s)?e:null;}
    public static boolean tick(ServerPlayer p,VisitData.Scene s){
        if(AcceptedButtonEyes.active(s))return false;
        if(FirstEscapeStory.engaged(s))return FirstEscapeStory.tick(p,s);
        if(active(s)&&phase(s)==Phase.STUDY_REVEAL_COMPLETE&&FirstEscapeStory.begin(p,s))return true;
        if(!active(s)||!s.owner.equals(p.getUUID()))return false;var n=data(s);if(!OtherWorld.isOther(p.level())||!p.isAlive())return true;
        var realm=SecretWorld.realm(p);if(realm==null||realm.house==null)return true;
        if(p.tickCount%20==0){ensure(p,s);for(String key:List.of("MotherPos","FatherPos"))if(n.contains(key))LoopProps.hold(p.serverLevel(),BlockPos.of(n.getLong(key)));}
        restoreProposalSeat(p,s);
        var mom=MotherSearch.mother(p);var dad=VisitEvents.father(p,s);if(mom==null||dad==null){n.putString("Blocker","Waiting for the original parents to load; no replacements created.");return true;}
        n.putLong("MotherPos",mom.blockPosition().asLong());n.putLong("FatherPos",dad.blockPosition().asLong());
        if(n.getBoolean("Recover")){n.remove("Recover");n.putInt("Ticks",0);n.remove("AudioStarted");ShowPlayback.stop(p,s);if(n.getBoolean("Rejected")&&n.contains("SafePlayer")){var safe=Reconciliation.safe(p,BlockPos.of(n.getLong("SafePlayer")));if(safe!=null)p.teleportTo(p.serverLevel(),safe.getX()+.5,safe.getY(),safe.getZ()+.5,p.getYRot(),p.getXRot());}if(phase(s)==Phase.N||phase(s)==Phase.O)p.startSleeping(BlockPos.of(n.getLong("Bed")));}
        var phase=phase(s);if((phase==Phase.N||phase==Phase.O)&&!p.isSleeping())p.startSleeping(BlockPos.of(n.getLong("Bed")));int time=n.getInt("Ticks");String voice=sound(phase);
        if(phase==Phase.M||phase==Phase.GO_TO_BED)BedroomBarricade.tick(p,s);
        if(!voice.isEmpty()&&!n.getBoolean("AudioStarted")){
            if((phase==Phase.INTRO_A)&&(mom.distanceToSqr(p)>36||dad.distanceToSqr(p)>36)){hint(p,"Meet your Other Parents");return true;}
            if(!ShowPlayback.start(p,s,voice,false)){n.putString("Blocker",ShowPlayback.data(s).getString("Blocker"));return true;}n.putBoolean("AudioStarted",true);
        }
        boolean ended=false;
        if(!voice.isEmpty()){ended=ShowPlayback.tick(p,s);time=ShowPlayback.position(s);n.putInt("Ticks",time);}
        else n.putInt("Ticks",++time);
        if(n.getBoolean("Rejected")){
            if(OtherWorldEstate.outsideHouse(realm.house,p.blockPosition())){var safe=BlockPos.of(n.getLong("SafePlayer"));p.teleportTo(p.serverLevel(),safe.getX()+.5,safe.getY(),safe.getZ()+.5,p.getYRot(),p.getXRot());}
            else if(p.onGround()&&p.getVehicle()==null&&!p.isSleeping())n.putLong("SafePlayer",p.blockPosition().asLong());
        }
        if(n.getBoolean("Opened"))n.putInt("GiftOpenAge",n.getInt("GiftOpenAge")+1);
        var gift=actor(p,s,"Gift");if(gift!=null){double slide=phase==Phase.C?Math.min(1,time/35.):phase==Phase.H?1-Math.min(1,time/12.):phase==Phase.I?Math.min(1,time/35.):phase.ordinal()>=Phase.C.ordinal()?1:0;if(phase==Phase.H)n.putBoolean("Away",true);if(phase==Phase.I)n.putBoolean("Away",false);if(n.getBoolean("Away")&&phase!=Phase.H)slide=0;
            gift.setPos(Vec3.atBottomCenterOf(s.table).add(.06*Math.sin(time*.4)*(phase==Phase.H?1:0),.97,-2-slide*1.8));gift.frame(phase.ordinal(),time,n.getBoolean("Opened")?Math.min(1,n.getInt("GiftOpenAge")/15F):0);}
        var hand=actor(p,s,"Hand");if(hand!=null)hand.frame(phase==Phase.Q?1:phase==Phase.S||phase==Phase.STUDY_REVEAL_COMPLETE?2:0,time,phase==Phase.Q?(time<60?Math.min(1,time/10F):Math.max(0,1-(time-60)/20F)):phase==Phase.S||phase==Phase.STUDY_REVEAL_COMPLETE?Math.min(1,time/10F):0);
        if(phase==Phase.J){dad.familyGesture(time<VisitAudio.length(voice)-20?30:31,5);if(time>=VisitAudio.length(voice)-20&&!n.getBoolean("NeedleDropped")){n.putBoolean("NeedleDropped",true);p.serverLevel().playSound(null,s.table,SoundEvents.IRON_TRAPDOOR_CLOSE,SoundSource.BLOCKS,.25F,1.7F);}}
        if(phase==Phase.H&&time<12)p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
        if(phase==Phase.R&&time>=30)dad.familyGesture(32,5);
        var pianoProp=prop(p,s,ScenePropEntity.PIANO,0);if(pianoProp!=null)pianoProp.mode(phase==Phase.DARK_HOUSE?3:0);
        if(phase==Phase.DARK_HOUSE){var piano=prop(p,s,ScenePropEntity.PIANO,0);if(piano!=null&&dad.getVehicle()==piano&&time%60==1){dad.familyGesture(33,12);p.serverLevel().playSound(null,piano.blockPosition(),SoundEvents.NOTE_BLOCK_HARP.get(),SoundSource.BLOCKS,.8F,.7F);}
            if(OtherWorldEstate.insideStudy(realm.house,p.blockPosition())&&p.blockPosition().distSqr(BlockPos.of(n.getLong("StudyDoor")))>2.25&&piano!=null&&dad.getVehicle()==piano)go(p,s,Phase.P);
        }
        if(phase==Phase.FOLLOW_PARENTS){hint(p,"Follow your Other Parents");if(mom.getVehicle()==prop(p,s,ScenePropEntity.CHAIR,1)&&dad.getVehicle()==prop(p,s,ScenePropEntity.CHAIR,2))go(p,s,Phase.WAITING_FOR_SEAT);}
        if(phase==Phase.WAITING_FOR_SEAT)hint(p,"Right-click to sit");
        if(phase==Phase.WAITING_FOR_GIFT_OPEN)hint(p,"Right-click to open the gift");
        if(phase==Phase.GIFT_OPENED&&time>=20)go(p,s,Phase.D);
        if(phase==Phase.AWAITING_CHOICE&&p.tickCount%20==0)sync(p,s);
        if(phase==Phase.L||phase==Phase.RUN_UPSTAIRS){var mid=HouseCabinets.at(realm.house,24,11,27);if(p.getY()>=mid.getY()&&p.distanceToSqr(mid.getCenter())<64)n.putBoolean("RunCueReached",true);if(phase==Phase.RUN_UPSTAIRS&&(n.getBoolean("RunCueReached")||p.getY()>=HouseCabinets.at(realm.house,24,15,27).getY()))go(p,s,Phase.M);}
        if(ended){Phase next=switch(phase){case INTRO_A->Phase.INTRO_B;case INTRO_B->Phase.FOLLOW_PARENTS;case C->Phase.WAITING_FOR_GIFT_OPEN;case D->Phase.E;case E->Phase.F;case F->Phase.G;case G->Phase.H;case H->Phase.I;case I->Phase.J;case J->Phase.K;case K->Phase.AWAITING_CHOICE;case L->Phase.RUN_UPSTAIRS;case M->Phase.GO_TO_BED;case N->Phase.O;case O->Phase.DARK_HOUSE;case P->Phase.Q;case Q->Phase.R;case R->Phase.S;case S->Phase.STUDY_REVEAL_COMPLETE;default->null;};if(next!=null)go(p,s,next);}
        if(p.tickCount%5==0)sync(p,s);if(p.tickCount%20==0)OtherMotherLoop.changed(p);return true;
    }
    static void cutsceneSafety(ServerPlayer p,VisitData.Scene s){
        if(!OtherWorld.isOther(p.level())||FirstEscapeStory.engaged(s))return;var state=phase(s);String voice=sound(state);
        boolean seating=state==Phase.FOLLOW_PARENTS;if(voice.isEmpty()&&!seating&&state!=Phase.GIFT_OPENED)return;
        if(!StoryCutsceneWatchdog.overdue(p,s,"button_offer",state.name(),voice.isEmpty()?600:StoryCutsceneWatchdog.voiceBudget(voice)))return;
        ShowPlayback.safetyFinish(p,s,"Button-eye offer action exceeded its safety window");
        if(seating){for(boolean mom:new boolean[]{true,false}){var actor=mom?MotherSearch.mother(p):VisitEvents.father(p,s);var chair=prop(p,s,ScenePropEntity.CHAIR,mom?1:2);if(actor!=null&&chair!=null&&!chair.isVehicle()){actor.stopRiding();actor.getNavigation().stop();actor.setPos(chair.position());actor.startRiding(chair,true);}}go(p,s,Phase.WAITING_FOR_SEAT);return;}
        Phase next=switch(state){case INTRO_A->Phase.INTRO_B;case INTRO_B->Phase.FOLLOW_PARENTS;case C->Phase.WAITING_FOR_GIFT_OPEN;case GIFT_OPENED->Phase.D;case D->Phase.E;case E->Phase.F;case F->Phase.G;case G->Phase.H;case H->Phase.I;case I->Phase.J;case J->Phase.K;case K->Phase.AWAITING_CHOICE;case L->Phase.RUN_UPSTAIRS;case M->Phase.GO_TO_BED;case N->Phase.O;case O->Phase.DARK_HOUSE;case P->Phase.Q;case Q->Phase.R;case R->Phase.S;case S->Phase.STUDY_REVEAL_COMPLETE;default->null;};
        if(next!=null)go(p,s,next);
    }
    static void hint(ServerPlayer p,String text){if(p.tickCount%30==0)p.displayClientMessage(Component.literal(text),true);}
    public static boolean parentControl(ParentEntity parent,ServerPlayer p,VisitData.Scene s){
        if(FirstEscapeStory.parentControl(parent,p,s))return true;
        if(!active(s))return false;parent.setNoAi(false);parent.setNoGravity(false);parent.noPhysics=false;var n=data(s);var phase=phase(s);var target=p.position();boolean sitting=false;
        if(phase==Phase.INTRO_A||phase==Phase.INTRO_B){String key=parent.isMom()?"MotherIntro":"FatherIntro";target=n.contains(key)?Vec3.atBottomCenterOf(BlockPos.of(n.getLong(key))):p.position();}
        else if(phase.ordinal()>=Phase.FOLLOW_PARENTS.ordinal()&&phase.ordinal()<=Phase.L.ordinal()&&!(phase==Phase.L&&n.getInt("Ticks")>=40)){
            var chair=prop(p,s,ScenePropEntity.CHAIR,parent.isMom()?1:2);if(chair==null)return true;ScenePlacement.prepareWalker(parent);SupperSeating.approach(parent,chair);parent.lookAtStoryPlayer(p);return true;
        }else if(phase==Phase.L)target=p.position();
        else if(parent.isMom()&&n.getBoolean("SleepDisabled")){var realm=SecretWorld.realm(p);target=Vec3.atBottomCenterOf(HouseCabinets.at(realm.house,23,6,44));}
        else if(phase==Phase.RUN_UPSTAIRS||phase==Phase.M||parent.isMom())target=Vec3.atBottomCenterOf(BlockPos.of(n.getLong("Kitchen")));
        else {var piano=prop(p,s,ScenePropEntity.PIANO,0);if(piano==null)return true;target=piano.position();sitting=true;if(parent.getVehicle()!=piano&&parent.distanceToSqr(piano)<2.25&&!piano.isVehicle())parent.startRiding(piano,true);}
        if(!sitting&&parent.isPassenger())parent.stopRiding();ScenePlacement.prepareWalker(parent);
        if(!sitting)recoverApproach(parent,p,s,target);
        if(parent.isPassenger()||parent.distanceToSqr(target)<(phase==Phase.L?6.25:1.5)){parent.getNavigation().stop();}else if(parent.tickCount%20==0||parent.getNavigation().isDone())parent.getNavigation().moveTo(target.x,target.y,target.z,1);
        boolean backToPiano=!parent.isMom()&&(phase==Phase.DARK_HOUSE||phase==Phase.P||phase==Phase.Q||phase==Phase.R&&n.getInt("Ticks")<30);
        if(backToPiano){parent.setYRot(0);parent.setYBodyRot(0);parent.setYHeadRot(0);}else if(!parent.isMom()&&phase==Phase.S){var hand=Vec3.atBottomCenterOf(s.piano).add(.2,1.05,.5);parent.getLookControl().setLookAt(hand.x,hand.y,hand.z,40,30);var delta=hand.subtract(parent.getEyePosition());parent.setYHeadRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));}else parent.lookAtStoryPlayer(p);return true;
    }
    public static boolean interact(ServerPlayer p,ScenePropEntity prop){var s=OtherMotherLoop.active(p);if(!active(s)||!s.owner.equals(prop.owner)||!s.door.equals(prop.storyDoor))return false;
        if(BedroomBarricade.interact(p,s,prop))return true;
        if(prop.kind()==ScenePropEntity.CHAIR&&prop.slot==0&&p.distanceToSqr(prop)<9&&!prop.isVehicle()){
            if(phase(s)==Phase.WAITING_FOR_SEAT&&p.startRiding(prop,true))go(p,s,Phase.C);
            else if(locksSeat(prop)){prop.refreshProposalSeatLock();p.startRiding(prop,true);}
        }return true;
    }
    static void retirePresentation(ServerPlayer p,VisitData.Scene s){
        for(String key:List.of("Gift","Hand")){var actor=actor(p,s,key);if(actor!=null){actor.ejectPassengers();actor.discard();}}
    }
    private static BlockPos introFloor(ServerPlayer p,BlockPos near){
        for(int radius=0;radius<=3;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;var at=near.offset(x,0,z);
            var feet=Vec3.atBottomCenterOf(at);var box=net.minecraft.world.entity.EntityDimensions.fixed(.7F,1.9F).makeBoundingBox(feet);
            if(p.serverLevel().getBlockState(at.below()).isFaceSturdy(p.serverLevel(),at.below(),Direction.UP)&&p.serverLevel().noCollision(box)&&!p.serverLevel().containsAnyLiquid(box))return at;
        }return null;
    }
    private static void recoverApproach(ParentEntity parent,ServerPlayer p,VisitData.Scene s,Vec3 target){
        var n=parent.getPersistentData().getCompound("TraditionalApproach");long now=parent.level().getGameTime();
        long goal=BlockPos.containing(target).asLong();double distance=parent.distanceToSqr(target);
        if(!n.contains("Goal")||n.getLong("Goal")!=goal){n.putLong("Goal",goal);n.putDouble("Distance",distance);n.putLong("Progress",now);}
        if(distance<n.getDouble("Distance")-.25){n.putDouble("Distance",distance);n.putLong("Progress",now);}
        parent.getPersistentData().put("TraditionalApproach",n);
        if(distance<2.25||now-n.getLong("Progress")<60&&distance<144)return;
        // Stay on the authored floor; the generic terrain fallback can select
        // a roof or an outdoor surface when an indoor target is obstructed.
        var realm=SecretWorld.realm(p);var base=BlockPos.containing(target);
        for(int radius=0;radius<=2;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;var at=base.offset(x,0,z);var feet=Vec3.atBottomCenterOf(at);
            if(realm!=null&&!OtherWorldEstate.outsideHouse(realm.house,base)&&OtherWorldEstate.outsideHouse(realm.house,at))continue;
            var box=parent.getBoundingBox().move(feet.subtract(parent.position()));
            if(!parent.level().getBlockState(at.below()).isFaceSturdy(parent.level(),at.below(),Direction.UP)||!parent.level().noCollision(parent,box)||parent.level().containsAnyLiquid(box))continue;
            parent.getNavigation().stop();parent.setDeltaMovement(Vec3.ZERO);parent.teleportTo(feet.x,feet.y,feet.z);n.putLong("Progress",now);return;
        }
    }
    public static boolean interact(ServerPlayer p,VisitData.Scene s,CinematicPropEntity prop){if(!active(s)||FirstEscapeStory.engaged(s)||!prop.owned(s))return false;if(prop.kind()==CinematicPropEntity.BUTTON_GIFT&&phase(s)==Phase.WAITING_FOR_GIFT_OPEN&&p.distanceToSqr(prop)<16){data(s).putBoolean("Opened",true);go(p,s,Phase.GIFT_OPENED);}return true;}
    public static boolean choice(ServerPlayer p,UUID nonce,String answer){var s=OtherMotherLoop.active(p);if(!active(s)||!answer.startsWith("traditional:"))return false;var n=data(s);if(phase(s)!=Phase.AWAITING_CHOICE||!n.hasUUID("Choice")||!nonce.equals(n.getUUID("Choice")))return true;if(answer.equals("traditional:reject"))go(p,s,Phase.L);else if(answer.equals("traditional:accept")){releaseSeat(p,s);AcceptedButtonEyes.begin(p,s,AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL);}return true;}
    public static boolean block(ServerPlayer p,BlockPos at){var s=OtherMotherLoop.active(p);if(!active(s))return false;var n=data(s);var state=p.serverLevel().getBlockState(at);
        if(FirstEscapeStory.engaged(s))return FirstEscapeStory.block(p,at);
        if(state.getBlock() instanceof BedBlock){if(phase(s)==Phase.GO_TO_BED&&at.distSqr(BlockPos.of(n.getLong("Bed")))<=1&&p.distanceToSqr(at.getCenter())<16){if(BedroomBarricade.done(s))go(p,s,Phase.N);else p.displayClientMessage(Component.literal("Push the glowing furniture against the bedroom door first."),true);}else if(n.getBoolean("SleepDisabled"))p.displayClientMessage(Component.literal("you cant sleep..."),true);return true;}
        if(BedroomBarricade.blockDoor(p,s,at)){p.displayClientMessage(Component.literal("The door is locked by the barricade."),true);return true;}
        if(state.getBlock() instanceof DoorBlock&&lockedDoor(p,s,at)){p.displayClientMessage(Component.literal("The door is locked."),true);return true;}return state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get());
    }
    static boolean lockedDoor(ServerPlayer p,VisitData.Scene s,BlockPos at){var n=data(s);var realm=SecretWorld.realm(p);if(realm==null)return false;var state=p.serverLevel().getBlockState(at);if(state.getBlock() instanceof DoorBlock&&state.getValue(DoorBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)at=at.below();
        if(at.asLong()==n.getLong("StudyDoor"))return n.getBoolean("StudyLocked");
        var parlor1=HouseCabinets.at(realm.house,31,6,41);var parlor2=HouseCabinets.at(realm.house,31,6,42);
        boolean parlor=at.equals(parlor1)||at.equals(parlor2);
        var local=at.subtract(realm.house);boolean exterior=local.getY()<=3&&(local.getZ()<=-27||local.getX()<=-24||local.getX()>=25||local.getZ()>=22);
        return n.getBoolean("SleepDisabled")&&(parlor||exterior);
    }
    static void closeDoor(ServerPlayer p,BlockPos at){var state=p.serverLevel().getBlockState(at);if(state.getBlock() instanceof DoorBlock door){if(state.getValue(DoorBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER){at=at.below();state=p.serverLevel().getBlockState(at);}var s=OtherMotherLoop.scene(p);var saved=data(s).getCompound("Doors");String key=Long.toString(at.asLong());if(!saved.contains(key)){var original=new CompoundTag();original.put("Lower",NbtUtils.writeBlockState(state));original.put("Upper",NbtUtils.writeBlockState(p.serverLevel().getBlockState(at.above())));saved.put(key,original);data(s).put("Doors",saved);}door.setOpen(p,p.serverLevel(),state,at,false);}}
    private static void darken(ServerPlayer p,VisitData.Scene s){var n=data(s);if(n.getBoolean("Dark"))return;var realm=SecretWorld.realm(p);var changed=new ListTag();
        for(var at:BlockPos.betweenClosed(realm.house.offset(-30,-1,-35),realm.house.offset(30,32,30))){var state=p.serverLevel().getBlockState(at);if(state.hasBlockEntity())continue;if(state.getLightEmission(p.serverLevel(),at)>0&&!state.is(com.coraline.registry.ModBlocks.TUNNEL_FABRIC.get())&&!state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())){var entry=new CompoundTag();entry.putLong("Pos",at.asLong());entry.put("Original",NbtUtils.writeBlockState(state));changed.add(entry);p.serverLevel().setBlock(at,Blocks.AIR.defaultBlockState(),2);}}
        n.put("Lights",changed);n.putBoolean("Dark",true);OtherMotherLoop.changed(p);
    }
    static void sync(ServerPlayer p,VisitData.Scene s){var n=data(s);var packet=new CompoundTag();packet.putBoolean("BlackIsTraditional",true);packet.putString("Phase",phase(s).name());packet.putInt("Ticks",n.getInt("Ticks"));packet.putString("Sound",sound(phase(s)));packet.putUUID("Choice",n.getUUID("Choice"));packet.putInt("Father",VisitEvents.father(p,s)==null?-1:VisitEvents.father(p,s).getId());packet.putInt("Mother",MotherSearch.mother(p)==null?-1:MotherSearch.mother(p).getId());packet.putLong("Piano",s.piano.asLong());packet.putLong("Table",s.table.asLong());packet.putLong("Bed",n.getLong("Bed"));packet.putBoolean("Prelude",phase(s).ordinal()<Phase.M.ordinal());packet.putBoolean("Dark",n.getBoolean("Dark"));packet.putBoolean("Opened",n.getBoolean("Opened"));packet.putInt("BarricadeProgress",n.getCompound("Barricade").getInt("Completed"));packet.putBoolean("NeedleDropped",n.getBoolean("NeedleDropped"));SecretsNetwork.send(p,packet);}
    public static void recover(ServerPlayer p){var s=OtherMotherLoop.scene(p);if(active(s)){data(s).putBoolean("Recover",true);p.getPersistentData().remove("CoralineWakeAt");p.setForcedPose(null);}}
    public static boolean restoreOnEntry(ServerPlayer p,VisitData.Scene s){if(AcceptedButtonEyes.active(s))return false;var n=data(s);if(!n.getBoolean("Started")||!n.contains("Phase"))return false;s.loop.phase=OtherMotherLoopState.Phase.BLACK_IS_TRADITIONAL;for(String key:List.of("MotherPos","FatherPos"))if(n.contains(key))LoopProps.hold(p.serverLevel(),BlockPos.of(n.getLong(key)));recover(p);return true;}
    public static void reset(ServerPlayer p,VisitData.Scene s){if(!p.hasPermissions(2)||!s.owner.equals(p.getUUID()))return;releaseSeat(p,s);BedroomBarricade.clear(p,s);ShowPlayback.stop(p,s);var n=data(s);var lookup=p.serverLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK);for(var raw:n.getList("Lights",10)){var entry=(CompoundTag)raw;var at=BlockPos.of(entry.getLong("Pos"));if(p.serverLevel().getBlockState(at).isAir())p.serverLevel().setBlock(at,NbtUtils.readBlockState(lookup,entry.getCompound("Original")),2);}var doors=n.getCompound("Doors");for(String key:doors.getAllKeys()){var at=BlockPos.of(Long.parseLong(key));var entry=doors.getCompound(key);var original=NbtUtils.readBlockState(lookup,entry.getCompound("Lower"));if(p.serverLevel().getBlockState(at).getBlock()==original.getBlock()){p.serverLevel().setBlock(at,original,2);p.serverLevel().setBlock(at.above(),NbtUtils.readBlockState(lookup,entry.getCompound("Upper")),2);}}for(String key:List.of("Gift","Hand")){var prop=actor(p,s,key);if(prop!=null)prop.discard();}p.stopRiding();p.stopSleepInBed(true,false);p.setForcedPose(null);s.loop.story.remove(KEY);s.loop.story.remove("BlackIsTraditionalQueued");OtherMotherLoop.phase(p,s,OtherMotherLoopState.Phase.EXPLORING);VisitEvents.cleanup(p,s);var packet=new CompoundTag();packet.putBoolean("BlackIsTraditional",true);SecretsNetwork.send(p,packet);OtherMotherLoop.changed(p);}
    public static String debug(ServerPlayer p,VisitData.Scene s,String command,String argument){
        if(!p.hasPermissions(2)||!p.getUUID().equals(s.owner))return "Operator only.";
        if(command.startsWith("phase_")&&active(s)){go(p,s,Phase.valueOf(command.substring(6).toUpperCase(Locale.ROOT)));return "Preview checkpoint selected.";}
        switch(command){
            case "trust"->OtherMotherLoop.relationship(p,1,s.loop.suspicion);
            case "begin"->{if(VisitEvents.scripted(s)||s.loop.phase==OtherMotherLoopState.Phase.ACTIVITY)return "The current event must finish normally first.";begin(p,s);}
            case "reset"->reset(p,s);
            case "recover"->recover(p);
            case "sit"->{if(active(s)){ensure(p,s);for(var parent:new ParentEntity[]{MotherSearch.mother(p),VisitEvents.father(p,s)})if(parent!=null){var chair=prop(p,s,ScenePropEntity.CHAIR,parent.isMom()?1:2);parent.stopRiding();parent.setPos(chair.position());parent.startRiding(chair,true);}var chair=prop(p,s,ScenePropEntity.CHAIR,0);releaseSeat(p,s);p.teleportTo(p.serverLevel(),chair.getX(),chair.getY()+.1,chair.getZ(),0,0);p.startRiding(chair,true);go(p,s,Phase.C);}}
            case "gift"->{if(active(s)){data(s).putBoolean("Opened",true);go(p,s,Phase.GIFT_OPENED);}}
            case "table"->p.teleportTo(p.serverLevel(),s.table.getX()+.5,s.table.getY(),s.table.getZ()-6,p.getYRot(),0);
            case "bed"->{var r=SecretWorld.realm(p);if(r!=null&&r.bed!=null){var at=OtherWorld.safePlayer(p.serverLevel(),r.bed.east());p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),0);}}
            case "study"->{var r=SecretWorld.realm(p);if(r!=null&&r.piano!=null){var at=OtherWorld.safePlayer(p.serverLevel(),r.piano.south(3));p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),0);}}
            default->{}
        }return "Story phase: "+data(s).getString("Phase");
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer p)recover(p);}
    private BlackIsTraditional(){}
}
