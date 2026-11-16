package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.ModEntities;
import com.coraline.secrets.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.function.IntSupplier;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import static com.coraline.feast.OtherMotherLoopState.Phase.*;

/** Event categories share the visit director, ledger, voices and pursuit arbitration. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="coraline")
public final class VisitEvents {
    public enum Type { ACTIVITY, EXPLORATION_30_MIN, FAMILY_DINNER, FAMILY_OUTING, FAMILY_GAME_NIGHT, SPECIAL_DELIVERY, HOUSE_PARTY, OTHER_MOTHER_GOES_OUT, PLAYER_PARTY, WYBIE_ADVENTURE, THEATRE, BOBINSKY_MOUSE_CIRCUS }
    public static final int EXPLORATION_TICKS=36000;
    private static final Map<Type,IntSupplier> WEIGHTS=new EnumMap<>(Type.class);
    public record Handler(Function<OtherMotherWelcome.State,OtherMotherDialogue.Pool> introduction,BiConsumer<ServerPlayer,VisitData.Scene> begin,BiPredicate<ServerPlayer,VisitData.Scene> tick,Set<OtherMotherLoopState.Phase> phases){}
    private static final Map<Type,Handler> HANDLERS=new EnumMap<>(Type.class);
    /** Additional categories register one handler; they do not create another visit clock. */
    public static void register(Type type,IntSupplier weight,Handler handler){WEIGHTS.put(type,weight);if(handler!=null)HANDLERS.put(type,handler);}
    static {
        register(Type.ACTIVITY,()->VisitConfig.ACTIVITY_WEIGHT.get(),null);
        register(Type.PLAYER_PARTY,()->VisitConfig.PLAYER_PARTY_WEIGHT.get(),new Handler(state->PlayerParty.pool("invitation",state),PlayerParty::begin,PlayerParty::tick,Set.of(EVENT_INTRO,PARTY_SOCIAL,PARTY_FEAST_CALL,PARTY_FEAST,PARTY_ENDING)));
        register(Type.WYBIE_ADVENTURE,()->VisitConfig.WYBIE_ADVENTURE_WEIGHT.get(),new Handler(state->WybieAdventure.pool("invitation",state),WybieAdventure::begin,WybieAdventure::tick,Set.of(EVENT_INTRO,WYBIE_DOOR,WYBIE_INTRO,WYBIE_ACTIVE,WYBIE_ENDING)));
        register(Type.THEATRE,()->VisitConfig.THEATRE_WEIGHT.get(),new Handler(state->TheatreVisit.pool("invitation",state),TheatreVisit::beginWindow,TheatreVisit::tick,Set.of(EVENT_INTRO,THEATRE_WAIT,THEATRE_ACTIVE,THEATRE_RETURN)));
        register(Type.BOBINSKY_MOUSE_CIRCUS,()->VisitConfig.MOUSE_CIRCUS_WEIGHT.get(),new Handler(state->null,MouseCircusVisit::beginWindow,MouseCircusVisit::tick,Set.of(EVENT_INTRO,CIRCUS_WAIT,CIRCUS_ACTIVE,CIRCUS_RETURN)));
        register(Type.OTHER_MOTHER_GOES_OUT,()->VisitConfig.MOTHER_AWAY_WEIGHT.get(),new Handler(state->MotherGoesOut.pool("departure",state),MotherGoesOut::begin,MotherGoesOut::tick,Set.of(EVENT_INTRO,MOTHER_LEAVING,MOTHER_ABSENT,MOTHER_RETURNING,MOTHER_HOME_CHECK)));
        register(Type.HOUSE_PARTY,()->VisitConfig.HOUSE_PARTY_WEIGHT.get(),new Handler(state->HouseParty.pool("invitation",state),HouseParty::begin,HouseParty::tick,Set.of(EVENT_INTRO,PARTY_SOCIAL,PARTY_FEAST_CALL,PARTY_FEAST,PARTY_ENDING)));
        register(Type.EXPLORATION_30_MIN,()->VisitConfig.EXPLORATION_WEIGHT.get(),new Handler(VisitEvents::pool,(p,s)->{OtherMotherLoop.phase(p,s,FREE_TIME);s.loop.event.putInt("Remaining",EXPLORATION_TICKS);},VisitEvents::tickExploration,Set.of(EVENT_INTRO,FREE_TIME,EVENT_RETURN)));
        register(Type.FAMILY_DINNER,()->VisitConfig.DINNER_WEIGHT.get(),new Handler(state->FamilyDinner.pool("invitation",state),FamilyDinner::beginWindow,FamilyDinner::tick,Set.of(EVENT_INTRO,DINNER_WAIT,DINNER_GREETING,DINNER,DINNER_FINISH)));
        register(Type.FAMILY_OUTING,()->VisitConfig.OUTING_WEIGHT.get(),new Handler(state->FamilyOuting.pool("invitation",state),FamilyOuting::afterInvitation,FamilyOuting::tick,Set.of(EVENT_INTRO,OUTING_DESTINATION,OUTING_WAIT,OUTING_GREETING,OUTING_ACTIVE,OUTING_FINISH,OUTING_RETURN)));
        register(Type.FAMILY_GAME_NIGHT,()->VisitConfig.GAME_WEIGHT.get(),new Handler(state->FamilyGameNight.pool("invitation",state),FamilyGameNight::beginWindow,FamilyGameNight::tick,Set.of(EVENT_INTRO,GAME_NIGHT_WAIT,GAME_NIGHT_GREETING,GAME_NIGHT_INSTRUCTIONS,GAME_NIGHT_ACTIVE,GAME_NIGHT_CHOICE,GAME_NIGHT_FINISH)));
        register(Type.SPECIAL_DELIVERY,()->VisitConfig.DELIVERY_WEIGHT.get(),new Handler(state->SpecialDelivery.pool("invitation",state),SpecialDelivery::begin,SpecialDelivery::tick,Set.of(DELIVERY_PREPARING,EVENT_INTRO,DELIVERY_ACTIVE,DELIVERY_FINISH)));
    }
    public static OtherMotherDialogue.Pool pool(OtherMotherWelcome.State state){return new OtherMotherDialogue.Pool("events/exploration/"+SecretDialogue.stateId(state),2);}
    public static List<OtherMotherDialogue.Pool> pools(){var all=new ArrayList<OtherMotherDialogue.Pool>();for(var s:OtherMotherWelcome.State.values()){all.add(pool(s));for(String category:List.of("invitation","ontime","late","search","finish"))all.add(FamilyDinner.pool(category,s));}all.addAll(FamilyOuting.pools());all.addAll(FamilyGameNight.pools());all.addAll(SpecialDelivery.directorPools());all.addAll(HouseParty.pools());all.addAll(MotherGoesOut.pools());all.addAll(PlayerParty.pools());all.addAll(WybieAdventure.pools());all.addAll(TheatreVisit.pools());return all;}
    public static Type type(VisitData.Scene s){try{return Type.valueOf(s.loop.event.getString("Type"));}catch(IllegalArgumentException ignored){return Type.ACTIVITY;}}
    public static boolean reserved(VisitData.Scene s,BlockPos at){if(!scripted(s))return false;var site=OutingSites.current(s);if(site!=null){if(site.arrival().distSqr(at)<32*32)return true;for(var node:site.approach())if(node.distSqr(at)<12*12)return true;}var house=DeliveryRoute.house(s);if(house!=null&&house.distSqr(at)<32*32)return true;for(var node:DeliveryRoute.nodes(s))if(node.distSqr(at)<12*12)return true;return false;}
    public static boolean authorized(ServerPlayer p){var s=OtherMotherLoop.active(p);return s!=null&&(s.loop.phase==FREE_TIME||s.loop.phase==EVENT_RETURN||s.loop.phase==DINNER_WAIT||s.loop.phase==GAME_NIGHT_WAIT||type(s)==Type.FAMILY_OUTING&&scripted(s)||SpecialDelivery.protectedRoute(p)||HouseParty.active(p)!=null&&PartyRegions.allowed(p,p.position())||MotherGoesOut.unavailable(p)||s.loop.phase==WYBIE_ACTIVE||showRoute(p,s,p.blockPosition()));}
    /** Contextual invitation permission: never exempts storage, secret chambers or unrelated dimensions. */
    public static boolean showDoor(ServerPlayer p,String room){var s=OtherMotherLoop.active(p);return s!=null&&!s.loop.event.getBoolean("ShowCleanupDeferred")&&(room.equals("basement")&&TheatreVisit.basementAccess(p,s)||room.equals("attic")&&MouseCircusVisit.owned(p,s)&&MouseCircusVisit.data(s).getBoolean("InvitationDone")&&(!WybieOptionalVisits.allowed(s)||MouseCircusVisit.data(s).getBoolean("WybieMet")));}
    public static boolean showRoute(ServerPlayer p,VisitData.Scene s,BlockPos at){if(s==null||!s.owner.equals(p.getUUID())||s.loop.event.getBoolean("ShowCleanupDeferred"))return false;boolean theatre=TheatreVisit.basementAccess(p,s),circus=MouseCircusVisit.owned(p,s)&&MouseCircusVisit.data(s).getBoolean("InvitationDone");if(!theatre&&!circus)return false;if(ActivityWorld.isActivity(p.level()))return theatre?TheatreSite.allowed(p,s,p.level().dimension(),at.getCenter()):MouseCircusSite.inside(p,s);if(!OtherWorld.isOther(p.level()))return false;var realm=SecretWorld.realm(p);if(realm==null||realm.house==null)return false;for(var room:HouseCabinets.ROOMS)if((room.id().equals("archive")||room.id().equals("storage"))&&HouseCabinets.contains(realm.house,room,at))return false;for(var room:HouseCabinets.ROOMS)if((theatre&&room.id().equals("basement")||circus&&room.id().equals("attic"))&&HouseCabinets.contains(realm.house,room,at))return true;return ShowHouseAccess.onRoute(p,s,at.getCenter(),circus);}
    public static boolean venueEvent(VisitData.Scene s){return type(s)==Type.PLAYER_PARTY||type(s)==Type.THEATRE||type(s)==Type.BOBINSKY_MOUSE_CIRCUS;}
    public static boolean scripted(VisitData.Scene s){if(BlackIsTraditional.active(s))return true;var handler=HANDLERS.get(type(s));return s.stage==VisitData.Stage.DONE&&handler!=null&&handler.phases.contains(s.loop.phase);}
    private static boolean dinnerWaitingForReconciliation(VisitData.Scene s){return s.stage==VisitData.Stage.DONE&&s.loop.phase==EXPLORING&&s.loop.suspicion<1&&s.loop.secrets.search!=SecretsState.Search.SCRIPTED_TRAP&&(type(s)==Type.FAMILY_DINNER&&s.loop.event.getBoolean("DinnerPending")||type(s)==Type.FAMILY_OUTING&&s.loop.event.getBoolean("OutingPending")||type(s)==Type.FAMILY_GAME_NIGHT&&s.loop.event.getBoolean("GameNightPending")||type(s)==Type.SPECIAL_DELIVERY&&s.loop.event.getBoolean("DeliveryPending")||type(s)==Type.HOUSE_PARTY&&s.loop.event.getBoolean("PartyPending")||type(s)==Type.OTHER_MOTHER_GOES_OUT&&s.loop.event.getBoolean("MotherAwayPending")||type(s)==Type.PLAYER_PARTY&&s.loop.event.getBoolean("PartyPending")||type(s)==Type.WYBIE_ADVENTURE&&s.loop.event.getBoolean("WybiePending")||TheatreVisit.pending(s)||MouseCircusVisit.pending(s));}
    public static boolean keepFather(VisitData.Scene s){return AcceptedButtonEyes.active(s)||scripted(s)||dinnerWaitingForReconciliation(s);}
    public static boolean eligible(ServerPlayer p,VisitData.Scene s,Type type){VisitProgression.refresh(p,s);if(!s.owner.equals(p.getUUID())||s.loop.event.getBoolean("ShowCleanupDeferred"))return false;return switch(type){case SPECIAL_DELIVERY->SpecialDelivery.ready(s);case OTHER_MOTHER_GOES_OUT->NumberedEventDialogue.ready("mother_goes_out");case HOUSE_PARTY->NumberedEventDialogue.ready("house_party");case PLAYER_PARTY->PlayerParty.configured()&&NumberedEventDialogue.ready("player_party");case WYBIE_ADVENTURE->WybieAdventure.eligible(p,s);case THEATRE->TheatreVisit.eligible(p,s);case BOBINSKY_MOUSE_CIRCUS->MouseCircusVisit.eligible(p,s);default->true;};}
    public static Type select(ServerPlayer p,VisitData.Scene s,Type previous){
        var rotation=s.loop.story.getCompound("VisitEventRotation");
        var chosen=VisitEventSelection.chooseFair(WEIGHTS.keySet(),t->WEIGHTS.get(t).getAsInt()>0&&eligible(p,s,t),previous,Type.ACTIVITY,t->rotation.getLong(t.name()),p.getRandom()::nextInt);
        long sequence=rotation.getLong("Sequence")+1;rotation.putLong("Sequence",sequence);rotation.putLong(chosen.name(),sequence);s.loop.story.put("VisitEventRotation",rotation);
        return chosen;
    }
    public static Type select(RandomSource random,Type previous,boolean excludePrevious){
        return VisitEventSelection.chooseFortySixty(WEIGHTS.keySet(),t->WEIGHTS.get(t).getAsInt()>0,previous,Type.ACTIVITY,random::nextInt);
    }
    public static void next(ServerPlayer p,VisitData.Scene s,boolean early){if(FirstEscapeStory.engaged(s))return;if(BlackIsTraditional.active(s)&&BlackIsTraditional.phase(s)==BlackIsTraditional.Phase.STUDY_REVEAL_COMPLETE){FirstEscapeStory.begin(p,s);return;}if(BlackIsTraditional.pending(s)){BlackIsTraditional.begin(p,s);return;}start(p,s,select(p,s,s.loop.event.contains("Type")?type(s):null));}
    public static boolean skipCurrent(ServerPlayer p){
        var s=OtherMotherLoop.active(p);if(!p.isCreative()||s==null||!s.owner.equals(p.getUUID()))return false;
        com.coraline.secrets.CatEncounter.skip(p);Reconciliation.cancel(p);VisitAudio.stop(p);s.loop.secrets.search=SecretsState.Search.NORMAL;s.loop.secrets.pendingTicks=0;
        if(ActivityWorld.isActivity(p.level())&&!venueEvent(s)&&!ActivityWorld.debugReturn(p,s))return false;
        if(type(s)==Type.ACTIVITY){LoopProps.removePortals(p.server,s);s.loop.arrivalDeadline=0;s.loop.secrets.dialogue.remove("OpeningActivityWait");s.loop.secrets.dialogue.remove("ReconciledProposal");}
        if(!cleanup(p,s))return false;clearCancelledVoice(s);int serial=s.loop.event.getInt("Serial");next(p,s,true);return s.loop.event.getInt("Serial")!=serial;
    }
    /** Explicit skip cancels the old audible lease as well as its sound source. */
    static void clearCancelledVoice(VisitData.Scene s){s.welcomeAfter=0;for(String key:List.of("VoiceClockSound","VoiceClockEnd"))s.loop.secrets.dialogue.remove(key);}
    static void fallback(ServerPlayer p,VisitData.Scene s,Type previous){
        var n=s.loop.event;boolean root=!n.getBoolean("FallbackActive");n.putBoolean("FallbackActive",true);var failed=n.getList("FailedEvents",8);failed.add(net.minecraft.nbt.StringTag.valueOf(type(s).name()));n.put("FailedEvents",failed);
        Type avoid=previous;try{avoid=Type.valueOf(n.getString("PreviousEvent"));}catch(IllegalArgumentException ignored){}final Type old=avoid;
        try{var choices=new ArrayList<Type>();for(var candidate:WEIGHTS.keySet())if(candidate!=old&&WEIGHTS.get(candidate).getAsInt()>0&&eligible(p,s,candidate)&&!failed.contains(net.minecraft.nbt.StringTag.valueOf(candidate.name())))choices.add(candidate);
            if(choices.isEmpty()){p.displayClientMessage(Component.literal("No different event can be prepared safely right now."),true);OtherMotherLoop.phase(p,s,EXPLORING);return;}
            start(p,s,VisitEventSelection.chooseFortySixty(choices,choices::contains,old,Type.ACTIVITY,p.getRandom()::nextInt));
        }finally{if(root){n.remove("FallbackActive");n.remove("FailedEvents");}}
    }
    public static void start(ServerPlayer p,VisitData.Scene s,Type type){
        if(BlackIsTraditional.active(s))return;
        if(type!=Type.ACTIVITY&&!OtherWorld.isOther(p.level()))return;
        if(!eligible(p,s,type)){p.displayClientMessage(Component.literal("That event is not ready for this visit yet."),true);return;}
        Type previous=type(s);FamilyOuting.Subtype forced=null;try{if(s.loop.event.contains("ForcedOuting"))forced=FamilyOuting.Subtype.valueOf(s.loop.event.getString("ForcedOuting"));}catch(IllegalArgumentException ignored){}s.loop.event.remove("ForcedOuting");
        if(!cleanup(p,s)){p.displayClientMessage(Component.literal("The previous event is still returning its guests safely."),true);return;}s.loop.event.putString("Type",type.name());s.loop.event.putInt("Serial",s.loop.event.getInt("Serial")+1);
        if(!s.loop.event.getBoolean("FallbackActive"))s.loop.event.putString("PreviousEvent",previous.name());
        s.loop.event.putInt("Visit",s.loop.visits);s.loop.event.putBoolean("Rewarded",false);s.loop.event.remove("EventInvitationFinished");
        s.loop.event.putInt("Remaining",EXPLORATION_TICKS);s.loop.event.putInt("IntroElapsed",0);
        s.loop.arrivalDeadline=0;LoopProps.clean(p.server,s);
        if(type!=Type.ACTIVITY)s.loop.secrets.dialogue.remove("OpeningActivityWait");s.loop.secrets.world.remove("LateSearchScheduled");
        if(s.loop.secrets.search==SecretsState.Search.MISSED_ACTIVITY_PENDING){s.loop.secrets.search=SecretsState.Search.NORMAL;s.loop.secrets.pendingTicks=0;}
        if(type==Type.ACTIVITY){OtherMotherLoop.nextActivity(p,s);return;}
        if(type==Type.FAMILY_OUTING&&!FamilyOuting.prepare(p,s,forced)){fallback(p,s,previous);return;}
        if(type==Type.FAMILY_GAME_NIGHT)FamilyGameNight.prepare(p,s);
        if(type==Type.PLAYER_PARTY&&!PlayerParty.prepare(p,s)){fallback(p,s,previous);return;}
        if(type==Type.WYBIE_ADVENTURE&&!WybieAdventure.prepare(p,s)){fallback(p,s,previous);return;}
        if(type==Type.THEATRE&&!TheatreVisit.prepare(p,s)){fallback(p,s,previous);return;}
        if(type==Type.BOBINSKY_MOUSE_CIRCUS&&!MouseCircusVisit.prepare(p,s)){fallback(p,s,previous);return;}
        if(type==Type.OTHER_MOTHER_GOES_OUT&&!MotherGoesOut.prepare(p,s)){fallback(p,s,previous);return;}
        if(type==Type.HOUSE_PARTY&&!HouseParty.prepare(p,s)){fallback(p,s,previous);return;}
        if(type==Type.SPECIAL_DELIVERY){if(!SpecialDelivery.prepare(p,s))fallback(p,s,previous);return;}
        OtherMotherLoop.phase(p,s,EVENT_INTRO);ensureFather(p,s);
    }
    public static boolean tick(ServerPlayer p,VisitData.Scene s){
        if(BlackIsTraditional.active(s))return BlackIsTraditional.tick(p,s);
        if(!scripted(s)||s.loop.event.getBoolean("ShowCleanupDeferred"))return false;
        var n=s.loop.event;
        if(s.loop.secrets.search==SecretsState.Search.SCRIPTED_TRAP||s.loop.suspicion>=1){if(!cleanup(p,s))ShowLifecycle.deferCleanup(p,s);OtherMotherLoop.phase(p,s,EXPLORING);if(ActivityWorld.isActivity(p.level()))OtherMotherLoop.fail(p,"The performance ended safely. Return for another visit.");return true;}
        if(p.tickCount%20==0){if(OtherWorld.isOther(p.level()))ensureFather(p,s);sendStatus(p,s);OtherMotherLoop.changed(p);}
        if(p.isSleeping()||s.loop.secrets.searching())return true;
        if(s.loop.phase==EVENT_INTRO){
            if(type(s)==Type.PLAYER_PARTY&&!HouseParty.data(s).getBoolean("Prepared")){if(!PlayerParty.ready(p,s)){int attempts=n.getInt("PreparationTicks")+1;n.putInt("PreparationTicks",attempts);if(attempts>=1200){p.displayClientMessage(Component.literal("This party cannot be prepared safely yet."),true);fallback(p,s,type(s));}return true;}}
            if(type(s)==Type.BOBINSKY_MOUSE_CIRCUS){MouseCircusVisit.invitation(p,s);return true;}
            var m=MotherSearch.mother(p);
            // Once the house-party invitation has started, its audible duration
            // must keep advancing even if the player walks around a corner.
            boolean partySpeaking=!n.getString("Voice").isEmpty();
            if(m==null||!partySpeaking&&(m.distanceToSqr(p)>100||!m.hasLineOfSight(p)))return true;
            var handler=HANDLERS.get(type(s));var voice=handler.introduction.apply(SecretDialogue.state(p));
            if(!speak(p,s,voice))return true;
            n.putBoolean("EventInvitationFinished",true);handler.begin.accept(p,s);
            return true;
        }
        return HANDLERS.get(type(s)).tick.test(p,s);
    }
    /** Only automatic presentation beats: choices, travel windows, games,
     * exploration, deliveries and timed activities are never auto-completed. */
    static boolean automaticBeat(OtherMotherLoopState.Phase phase){return switch(phase){
        case EVENT_INTRO,DINNER_GREETING,DINNER_FINISH,OUTING_GREETING,OUTING_FINISH,
            GAME_NIGHT_GREETING,GAME_NIGHT_INSTRUCTIONS,GAME_NIGHT_FINISH,
            DELIVERY_FINISH,PARTY_FEAST_CALL,PARTY_ENDING,MOTHER_LEAVING,
            MOTHER_RETURNING,MOTHER_HOME_CHECK,WYBIE_INTRO,WYBIE_ENDING,
            THEATRE_RETURN,CIRCUS_RETURN->true;
        default->false;
    };}
    static void cutsceneSafety(ServerPlayer p,VisitData.Scene s){
        if(!scripted(s)||!automaticBeat(s.loop.phase)||s.loop.event.getBoolean("ShowCleanupDeferred")||p.isSleeping()||s.loop.secrets.searching()||!OtherWorld.isOther(p.level()))return;
        var n=s.loop.event;String voice=n.getString("Voice");if(voice.isBlank())voice=n.getString("Sound");
        int budget=Math.max(2400,StoryCutsceneWatchdog.voiceBudget(voice));
        if(!StoryCutsceneWatchdog.overdue(p,s,"optional_event",type(s)+":"+s.loop.phase,budget))return;
        // Preserve inventory/custody gates. Deferred guest/venue cleanup keeps
        // its existing recovery path; never delete a guest to satisfy a timer.
        if(!cleanup(p,s)){ShowLifecycle.deferCleanup(p,s);StoryCutsceneWatchdog.retry(s,"optional_event");return;}
        VisitAudio.stop(p);clearCancelledVoice(s);
        OtherMotherLoop.phase(p,s,EXPLORING);
        p.displayClientMessage(Component.literal("The stalled event ended safely. No completion reward was granted."),true);
        OtherMotherLoop.changed(p);
    }
    private static boolean tickExploration(ServerPlayer p,VisitData.Scene s){
        var n=s.loop.event;
        if(s.loop.phase==FREE_TIME){
            int remaining=Math.max(0,n.getInt("Remaining")-1);n.putInt("Remaining",remaining);
            if(remaining==0){stopConversation(p);stopPiano(p);OtherMotherLoop.phase(p,s,EVENT_RETURN);n.putInt("ReturnTicks",0);p.sendSystemMessage(Component.literal("Your free time is over. Other Mother is waiting at the house."));}
            return true;
        }
        if(s.loop.phase==EVENT_RETURN){
            var m=MotherSearch.mother(p);if(m!=null&&m.distanceToSqr(p)<64&&m.hasLineOfSight(p)){next(p,s,false);return true;}
            int waited=n.getInt("ReturnTicks")+1;n.putInt("ReturnTicks",waited);
            if(waited==6000){s.loop.secrets.search=SecretsState.Search.SEARCHING;s.loop.secrets.target=p.blockPosition();p.sendSystemMessage(Component.literal("Other Mother has come to look for you."));}
            return true;
        }return true;
    }
    /** The duration is active ticks, not offline wall time; audio acknowledgements may only finish after duration. */
    static boolean speak(ServerPlayer p,VisitData.Scene s,OtherMotherDialogue.Pool pool){
        var n=s.loop.event;
        if(n.getString("Voice").isEmpty()){
            int length=OtherMotherDialogue.playActivityArrival(p,pool);if(length==0)return false;
            n.putString("Voice",pool.path());n.putString("Sound",s.lastDialogue.get(pool.path()));n.putInt("VoiceLength",length);n.putInt("IntroElapsed",0);n.putBoolean("VoiceFinished",false);OtherMotherLoop.changed(p);return false;
        }
        int elapsed=n.getInt("IntroElapsed")+1;n.putInt("IntroElapsed",elapsed);
        if(elapsed<n.getInt("VoiceLength")||!n.getBoolean("VoiceFinished")&&elapsed<n.getInt("VoiceLength")+40)return false;
        n.remove("Voice");n.remove("Sound");n.remove("IntroElapsed");return true;
    }
    /** An interrupted invitation never skips straight into a running event. */
    static boolean resumeNumberedIntro(ServerPlayer p){var s=OtherMotherLoop.active(p);if(s==null||s.loop.event.getBoolean("ShowCleanupDeferred"))return false;boolean unfinished=switch(type(s)){case HOUSE_PARTY,PLAYER_PARTY->s.loop.event.getBoolean("PartyPending")&&!HouseParty.data(s).getBoolean("Activated");case OTHER_MOTHER_GOES_OUT->MotherGoesOut.pending(s)&&!s.loop.event.getBoolean("EventInvitationFinished");case WYBIE_ADVENTURE->s.loop.event.getBoolean("WybiePending")&&!WybieAdventure.data(s).getBoolean("Activated");case THEATRE->TheatreVisit.pending(s)&&!TheatreVisit.data(s).getBoolean("InvitationDone");case BOBINSKY_MOUSE_CIRCUS->MouseCircusVisit.pending(s)&&!MouseCircusVisit.data(s).getBoolean("InvitationDone");default->false;};if(!unfinished)return false;for(String key:List.of("Voice","Sound","IntroElapsed","VoiceFinished"))s.loop.event.remove(key);if(type(s)==Type.THEATRE)TheatreVisit.data(s).putBoolean("NeedsRecovery",false);if(type(s)==Type.BOBINSKY_MOUSE_CIRCUS){ShowPlayback.stop(p,s);MouseCircusVisit.data(s).putBoolean("InvitationStarted",false);MouseCircusVisit.data(s).putBoolean("NeedsRecovery",false);}OtherMotherLoop.phase(p,s,EVENT_INTRO);return true;}
    public static void voiceFinished(ServerPlayer p,String sound){com.coraline.secrets.CatEncounter.voiceFinished(p,sound);var s=OtherMotherLoop.active(p);if(s==null)return;if(s.loop.event.getString("Sound").equals(sound))s.loop.event.putBoolean("VoiceFinished",true);if(s.loop.event.getString("FatherVoice").equals(sound))s.loop.event.putBoolean("FatherVoiceFinished",true);}
    static ParentEntity father(ServerPlayer p,VisitData.Scene s){return s.dad!=null&&p.serverLevel().getEntity(s.dad) instanceof ParentEntity d?d:null;}
    static void ensureFather(ServerPlayer p,VisitData.Scene s){
        if(type(s)==Type.SPECIAL_DELIVERY||!OtherWorld.isOther(p.level()))return;
        var realm=RealmData.get(p.server).realms.get(s.door);if(realm!=null){if(realm.piano!=null)s.piano=realm.piano;if(realm.table!=null)s.table=realm.table;}
        if(s.piano==null||s.table==null)return;LoopProps.hold(p.serverLevel(),s.piano);LoopProps.hold(p.serverLevel(),s.table);
        var existing=p.serverLevel().getEntitiesOfClass(ParentEntity.class,new AABB(s.piano).inflate(110),e->e.isOtherParent()&&!e.isMom()&&!e.getPersistentData().hasUUID("DeliveryOwner")&&e.getPersistentData().hasUUID("RealmDoor")&&s.door.equals(e.getPersistentData().getUUID("RealmDoor")));
        var dad=father(p,s);if(dad==null&&!existing.isEmpty())dad=existing.get(0);
        if(dad==null){dad=ModEntities.DAD.get().create(p.serverLevel());if(dad==null)return;var family=com.coraline.parents.ParentAccess.family(p.server,p.getUUID());dad.makeOtherParent(family==null?0:Math.max(0,family.dadSkin));if(family!=null)dad.getPersistentData().putUUID("OriginalFamily",family.id);dad.getPersistentData().putUUID("RealmDoor",s.door);if(realm!=null)dad.getPersistentData().putInt("RealmGeneration",realm.parentGeneration);dad.getPersistentData().putLong("OtherHome",s.piano.asLong());dad.setPos(Vec3.atBottomCenterOf(s.piano));p.serverLevel().addFreshEntity(dad);}
        for(var extra:existing)if(extra!=dad)extra.discard();s.dad=dad.getUUID();dad.getPersistentData().putUUID("VisitEventOwner",p.getUUID());dad.eventGaze(true);ScenePlacement.prepareWalker(dad);
        if(type(s)==Type.EXPLORATION_30_MIN||type(s)==Type.OTHER_MOTHER_GOES_OUT){
            var existingPianos=p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(s.piano).inflate(6),e->e.kind()==ScenePropEntity.PIANO&&s.owner.equals(e.owner)&&(e.storyDoor==null||s.door.equals(e.storyDoor)));
            var piano=existingPianos.stream().findFirst().orElse(null);
            if(piano==null)piano=prop(p,s,ScenePropEntity.PIANO,Vec3.atBottomCenterOf(s.piano),0);
            piano.storyDoor=s.door;piano.getPersistentData().putInt("VisitEventSerial",s.loop.event.getInt("Serial"));piano.getPersistentData().remove("LoopGeneration");for(var duplicate:existingPianos)if(duplicate!=piano){duplicate.ejectPassengers();duplicate.discard();}
            piano.mode((s.loop.phase==FREE_TIME||MotherGoesOut.fatherAvailable(p))&&!conversationOpen(p)&&dad.getVehicle()==piano?1:0);if(dad.getVehicle()!=piano&&dad.distanceToSqr(piano)<4)dad.startRiding(piano,true);
        }else if(type(s)==Type.FAMILY_DINNER)FamilyDinner.ensureTable(p,s);
    }
    public static boolean fatherControl(ParentEntity dad){
        if(dad.isMom()||!dad.getPersistentData().hasUUID("VisitEventOwner"))return false;
        var id=dad.getPersistentData().getUUID("VisitEventOwner");var s=VisitData.get(dad.getServer()).scenes.get(id);
        if(s==null||!scripted(s)&&!dinnerWaitingForReconciliation(s)){dad.getPersistentData().remove("VisitEventOwner");return false;}
        var p=dad.getServer().getPlayerList().getPlayer(id);
        if(p==null||p.level()!=dad.level()){dad.getNavigation().stop();return true;}
        if(ParentRescueStory.parentControl(dad))return true;
        if(BlackIsTraditional.parentControl(dad,p,s))return true;
        if(dinnerWaitingForReconciliation(s)){dad.getNavigation().stop();return true;}
        if(TheatreVisit.parentControl(dad,p,s))return true;
        if(type(s)==Type.HOUSE_PARTY||type(s)==Type.PLAYER_PARTY){HouseParty.parentControl(dad,p,s);return true;}
        if(type(s)==Type.FAMILY_OUTING){FamilyOuting.control(dad,p,s);return true;}
        if(type(s)==Type.FAMILY_GAME_NIGHT){FamilyGameNight.parentControl(dad,p,s);return true;}
        if(type(s)==Type.FAMILY_DINNER){FamilyDinner.seatParent(dad,p,s);return true;}
        if(!(dad.getVehicle() instanceof ScenePropEntity current&&current.kind()==ScenePropEntity.PIANO)){
            var piano=props(p,s).stream().filter(e->e.kind()==ScenePropEntity.PIANO).findFirst().orElse(null);if(piano!=null){dad.setMaxUpStep(1.05F);SupperSeating.approach(dad,piano);}return true;
        }
        dad.getNavigation().stop();
        if(conversationOpen(p))dad.lookAtStoryPlayer(p);
        else if(dad.getVehicle() instanceof ScenePropEntity piano){dad.setYRot(piano.getYRot());dad.setYBodyRot(piano.getYRot());dad.setYHeadRot(piano.getYRot());dad.setXRot(12);}
        return true;
    }
    public static boolean motherControl(ParentEntity m,ServerPlayer p,VisitData.Scene s){
        if(ParentRescueStory.parentControl(m))return true;
        if(BlackIsTraditional.parentControl(m,p,s))return true;
        if(!scripted(s))return false;
        if(TheatreVisit.parentControl(m,p,s))return true;
        if((type(s)==Type.HOUSE_PARTY||type(s)==Type.PLAYER_PARTY)&&s.loop.phase!=EVENT_INTRO){HouseParty.parentControl(m,p,s);return true;}
        if(type(s)==Type.FAMILY_OUTING&&s.loop.phase!=EVENT_INTRO&&s.loop.phase!=OUTING_DESTINATION){FamilyOuting.control(m,p,s);return true;}
        if(type(s)==Type.FAMILY_GAME_NIGHT&&s.loop.phase!=EVENT_INTRO){FamilyGameNight.parentControl(m,p,s);return true;}
        if(type(s)==Type.FAMILY_DINNER&&s.loop.phase!=EVENT_INTRO){FamilyDinner.seatParent(m,p,s);return true;}
        if(s.loop.phase==EVENT_INTRO||s.loop.phase==OUTING_DESTINATION||s.loop.phase==EVENT_RETURN){approachForProposal(m,p);return true;}
        if(conversationOpen(p)&&isMotherConversation(p)){m.getNavigation().stop();m.lookAtStoryPlayer(p);return true;}
        var realm=RealmData.get(p.server).realms.get(s.door);if(realm==null||realm.house==null)return true;
        var n=s.loop.event;long now=OtherMotherLoop.now(p);m.setMaxUpStep(1.05F);
        if(now<n.getLong("PatrolRest")){m.getNavigation().stop();if(m.tickCount%80==0)m.setYHeadRot(m.getYRot()+p.getRandom().nextInt(70)-35);return true;}
        if(n.contains("PatrolTarget")){
            var at=BlockPos.of(n.getLong("PatrolTarget"));
            if(m.distanceToSqr(Vec3.atBottomCenterOf(at))<2.25||now-n.getLong("PatrolStarted")>1200){n.remove("PatrolTarget");n.putLong("PatrolRest",now+100+p.getRandom().nextInt(401));m.getNavigation().stop();}
            else if(m.tickCount%40==0&&m.getNavigation().isDone())m.getNavigation().moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,1);
            return true;
        }
        var stops=RosewoodHouse.householdStops(realm.house);int offset=p.getRandom().nextInt(stops.size());
        for(int i=0;i<stops.size();i++){int room=(offset+i)%stops.size();if(n.contains("PatrolRoom")&&n.getInt("PatrolRoom")==room)continue;var anchor=stops.get(room);for(var at:BlockPos.betweenClosed(anchor.offset(-3,0,-3),anchor.offset(3,0,3))){
            if(m.distanceToSqr(at.getCenter())<16||SecretDialogue.secretArea(s.loop.secrets,at))continue;
            var box=m.getDimensions(m.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(at));if(!p.level().noCollision(m,box)||!p.level().getBlockState(at.below()).isCollisionShapeFullBlock(p.level(),at.below()))continue;
            var path=m.getNavigation().createPath(at,0);if(path==null||!path.canReach())continue;
            n.putInt("PatrolRoom",room);n.putLong("PatrolTarget",at.asLong());n.putLong("PatrolStarted",now);m.getNavigation().moveTo(path,1);return true;
        }}n.putLong("PatrolRest",now+100);return true;
    }
    /** Invitations pursue the player through the house, not a stale patrol/seat destination. */
    static void approachForProposal(ParentEntity m,ServerPlayer p){
        m.stopRiding();m.setNoAi(false);m.setNoGravity(false);m.noPhysics=false;m.setMaxUpStep(1.05F);ScenePlacement.prepareWalker(m);m.lookAtStoryPlayer(p);
        if(m.distanceToSqr(p)<=9&&m.hasLineOfSight(p)){m.getNavigation().stop();return;}
        if(m.tickCount%8!=0)return;
        if(m.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav){nav.setCanOpenDoors(true);nav.setCanPassDoors(true);}
        var old=m.getNavigation().getTargetPos();
        if(m.getNavigation().isDone()||old==null||old.distSqr(p.blockPosition())>4){var path=m.getNavigation().createPath(p,1);if(path!=null&&path.getNodeCount()>1)m.getNavigation().moveTo(path,1.35);else m.getNavigation().moveTo(p,1.35);}
        if(m.horizontalCollision&&m.onGround())m.getJumpControl().jump();
    }
    static ScenePropEntity prop(ServerPlayer p,VisitData.Scene s,int kind,Vec3 at,int slot){var e=FirstVisit.spawn(p,s,kind,at.x,at.y,at.z,slot);e.getPersistentData().putInt("VisitEventSerial",s.loop.event.getInt("Serial"));return e;}
    static List<ScenePropEntity> props(ServerPlayer p,VisitData.Scene s){var realm=RealmData.get(p.server).realms.get(s.door);var anchor=s.table!=null?s.table:realm!=null&&realm.house!=null?realm.house:p.blockPosition();return p.serverLevel().getEntitiesOfClass(ScenePropEntity.class,new AABB(anchor).inflate(300),e->s.owner.equals(e.owner)&&s.door.equals(e.storyDoor)&&e.getPersistentData().contains("VisitEventSerial"));}
    public static boolean propTick(ScenePropEntity e){
        if(PartyCelebration.presentTick(e))return true;
        if(!e.getPersistentData().contains("VisitEventSerial"))return false;
        var s=e.owner==null?null:VisitData.get(e.getServer()).scenes.get(e.owner);
        if(s==null||!scripted(s)&&!dinnerWaitingForReconciliation(s)||!Objects.equals(s.door,e.storyDoor)||e.getPersistentData().getInt("VisitEventSerial")!=s.loop.event.getInt("Serial")){e.ejectPassengers();e.discard();}return true;
    }
    public static boolean interact(ServerPlayer p,ParentEntity parent){
        // Major-story actors must never replay the old proposal status packet.
        // Its saved Dark=true lightmap otherwise flashes on for six seconds
        // whenever the player clicks Mother during the later kitchen chapter.
        var major=OtherMotherLoop.scene(p);if(parent.isOtherParent()&&major!=null&&(Objects.equals(parent.getUUID(),major.mom)||Objects.equals(parent.getUUID(),major.dad)||parent.getPersistentData().hasUUID("RealmDoor")&&major.door.equals(parent.getPersistentData().getUUID("RealmDoor")))&&(FirstEscapeStory.engaged(major)||ParentRescueStory.started(major)))return true;
        if(DinnerActivities.parent(p,parent))return true;
        var story=OtherMotherLoop.active(p);if(BlackIsTraditional.active(story)){BlackIsTraditional.sync(p,story);return true;}
        if(MotherGoesOut.unavailable(p)&&parent.isMom())return true;
        if(MotherGoesOut.fatherAvailable(p)&&!parent.isMom()){var scene=OtherMotherLoop.active(p);if(scene!=null&&Objects.equals(parent.getUUID(),scene.dad)&&parent.distanceToSqr(p)<36&&!SecretDialogue.busy(p)){openConversation(p,false);return true;}}
        if(WybieAdventure.interact(p,parent)||HouseParty.interact(p,parent)||SpecialDelivery.motherInteract(p,parent))return true;
        var outing=OtherMotherLoop.active(p);if(outing!=null&&FamilyOuting.join(p,outing,parent))return true;
        var s=OtherMotherLoop.active(p);if(s==null||!scripted(s)||s.loop.phase!=FREE_TIME||!Objects.equals(parent.getUUID(),parent.isMom()?s.mom:s.dad)||parent.distanceToSqr(p)>36||SecretDialogue.busy(p)||s.loop.secrets.searching())return false;
        if(parent.isMom()&&MotherInterrupts.blackmail(p))return true;
        openConversation(p,parent.isMom());return true;
    }
    public static boolean dinnerProp(ServerPlayer p,ScenePropEntity prop){return PartyKidsGames.kick(p,prop)||BlackIsTraditional.interact(p,prop)||PlayerParty.interact(p,prop)||PartyMatches.interact(p,prop)||PartyBanquet.interact(p,prop)||FamilyGameNight.interact(p,prop)||FamilyOuting.interact(p,prop)||FamilyDinner.interact(p,prop);}
    static void sendStatus(ServerPlayer p,VisitData.Scene s){var n=new CompoundTag();n.putBoolean("VisitEventStatus",true);n.putString("Phase",s.loop.phase.name());n.putInt("Remaining",s.loop.phase==FREE_TIME?s.loop.event.getInt("Remaining"):s.loop.phase==DINNER_WAIT||s.loop.phase==OUTING_WAIT||s.loop.phase==THEATRE_WAIT||s.loop.phase==CIRCUS_WAIT?s.loop.event.getInt("ArrivalRemaining"):0);n.putLong("Piano",s.piano==null?0:s.piano.asLong());boolean gameRoom=type(s)==Type.FAMILY_GAME_NIGHT&&(s.loop.phase==GAME_NIGHT_ACTIVE||s.loop.phase==GAME_NIGHT_CHOICE)&&!s.loop.secrets.searching();n.putBoolean("GameRoomMusic",gameRoom);n.putLong("GameRoomPosition",s.table==null?0:s.table.asLong());n.putBoolean("GameRoomMusicPlaying",gameRoom&&s.loop.event.getInt("GameReactionRemaining")==0&&!SecretDialogue.busy(p));var dad=father(p,s);boolean piano=(type(s)==Type.EXPLORATION_30_MIN&&s.loop.phase==FREE_TIME||MotherGoesOut.fatherAvailable(p))&&dad!=null&&dad.getVehicle() instanceof ScenePropEntity prop&&prop.kind()==ScenePropEntity.PIANO;n.putBoolean("PianoActive",piano);n.putBoolean("PianoPlaying",piano&&!conversationOpen(p)&&!s.loop.secrets.searching());if(type(s)==Type.FAMILY_OUTING){var site=OutingSites.current(s);if(site!=null){n.putString("Objective",FamilyOuting.definition(s).name()+" — "+FamilyOuting.definition(s).landmark());n.putLong("OutingWaypoint",site.arrival().asLong());n.putBoolean("OutingMarker",s.loop.phase==OUTING_WAIT||s.loop.phase==OUTING_RETURN);n.putBoolean("ParentsReady",s.loop.event.getBoolean("ParentsReady"));}}if(type(s)==Type.SPECIAL_DELIVERY){var at=DeliveryRoute.house(s);if(at!=null){n.putBoolean("DeliveryMarker",s.loop.phase==DELIVERY_ACTIVE);n.putLong("DeliveryWaypoint",s.loop.event.getBoolean("DeliveryDelivered")||s.loop.event.getBoolean("DeliveryOpened")?SecretWorld.realm(p).porch.asLong():at.asLong());}}if(type(s)==Type.HOUSE_PARTY)HouseParty.status(p,s,n);if(type(s)==Type.THEATRE)TheatreVisit.status(p,s,n);if(type(s)==Type.BOBINSKY_MOUSE_CIRCUS)MouseCircusVisit.status(p,s,n);DinnerActivities.status(p,s,n);SecretsNetwork.send(p,n);}
    static void stopPiano(ServerPlayer p){var n=new CompoundTag();n.putBoolean("VisitEventStatus",true);n.putString("Phase","");SecretsNetwork.send(p,n);}
    public static void suspend(ServerPlayer p,VisitData.Scene s){WybieAdventure.suspend(p,s);TheatreVisit.suspend(p,s);MouseCircusVisit.suspend(p,s);MotherGoesOut.suspend(p,s);HouseParty.suspend(p,s);FamilyDinner.suspend(p,s);FamilyGameNight.suspend(p,s);stopConversation(p);stopPiano(p);var m=MotherSearch.mother(p);var d=father(p,s);if(m!=null)FamilyDinner.finishBite(m);if(d!=null)FamilyDinner.finishBite(d);if(!s.loop.event.getBoolean("DinnerPending")&&!s.loop.event.getBoolean("OutingPending")&&!s.loop.event.getBoolean("GameNightPending")&&!s.loop.event.getBoolean("DeliveryPending")&&!s.loop.event.getBoolean("PartyPending")&&!s.loop.event.getBoolean("MotherAwayPending")&&!s.loop.event.getBoolean("WybiePending")&&!TheatreVisit.pending(s)&&!MouseCircusVisit.pending(s))cleanup(p,s);else if(s.loop.event.getBoolean("OutingPending"))FamilyOuting.release(p,s);}
    public static boolean cleanup(ServerPlayer p,VisitData.Scene s){
        if(!s.owner.equals(p.getUUID()))return false;
        if(!TheatreVisit.cleanup(p,s)||!MouseCircusVisit.cleanup(p,s)||!HouseParty.cleanup(p,s))return false;
        WybieAdventure.cleanup(p,s);MotherGoesOut.cleanup(p,s);
        SpecialDelivery.cleanup(p,s);
        FamilyOuting.restoreDock(p,s);EventChunks.release(p,s);
        FamilyGameNight.cleanup(p,s);
        FamilyOuting.cleanup(p,s);
        s.loop.event.remove("ArrivalAwarded");
        FamilyDinner.forget(p.getUUID());
        DinnerActivities.cleanup(s);
        s.loop.event.remove("QuestionRound");
        stopConversation(p);stopPiano(p);
        if(s.table!=null)for(var prop:props(p,s)){prop.ejectPassengers();prop.discard();}
        var dad=father(p,s);if(dad!=null){FamilyDinner.finishBite(dad);dad.stopRiding();dad.getPersistentData().remove("VisitEventOwner");dad.eventGaze(false);dad.getNavigation().stop();}
        var m=MotherSearch.mother(p);if(m!=null){FamilyDinner.finishBite(m);m.stopRiding();m.getNavigation().stop();}
        for(String key:List.of("Voice","Sound","IntroElapsed","VoiceFinished","PatrolTarget","PatrolRest","ReturnTicks","DinnerPending","DinnerResumePhase","ArrivalRemaining","GraceRemaining","GraceRolled","Searching","MealElapsed","FatherVoice","FatherVoiceElapsed","FatherVoiceLength","Passed","Question","CompletionAwarded","Resuming","MotherGreetingDone","FatherFinished","QuestionOffered","Answered","QuestionAfter","BiteAfter","PassingUntil","RequestDish"))s.loop.event.remove(key);
        return true;
    }
    private record Conversation(UUID nonce,UUID actor,boolean mother,String node,long expires){}
    private static final Map<UUID,Long> CONVERSATION_READY=new HashMap<>();
    private static final Map<UUID,Conversation> CONVERSATIONS=new HashMap<>();
    static UUID nonce(ServerPlayer p){var c=CONVERSATIONS.get(p.getUUID());return c==null?null:c.nonce;}
    static boolean conversationOpen(ServerPlayer p){var c=CONVERSATIONS.get(p.getUUID());if(c!=null&&OtherMotherLoop.now(p)>c.expires){stopConversation(p);return false;}return c!=null;}
    static boolean isMotherConversation(ServerPlayer p){var c=CONVERSATIONS.get(p.getUUID());return c!=null&&c.mother;}
    static void stopConversation(ServerPlayer p){CONVERSATIONS.remove(p.getUUID());CONVERSATION_READY.remove(p.getUUID());var s=OtherMotherLoop.scene(p);if(s!=null&&!s.loop.event.getString("StudyVoice").isEmpty()){VisitAudio.stopVoice(p,s.loop.event.getString("StudyVoice"));s.loop.event.remove("StudyVoice");}var n=new CompoundTag();n.putBoolean("VisitConversation",true);n.putBoolean("Close",true);SecretsNetwork.send(p,n);}
    static void openConversation(ServerPlayer p,boolean mother){
        if(conversationOpen(p))return;var s=OtherMotherLoop.scene(p);var actor=mother?s.mom:s.dad;if(actor==null)return;
        var dad=father(p,s);if(dad!=null&&dad.getVehicle() instanceof ScenePropEntity piano&&piano.kind()==ScenePropEntity.PIANO)piano.mode(0);
        show(p,new Conversation(UUID.randomUUID(),actor,mother,mother?"mother":MotherGoesOut.fatherAvailable(p)?"away_left_"+(1+p.getRandom().nextInt(2)):"root",OtherMotherLoop.now(p)+1200));
        sendStatus(p,s);
    }
    private record Node(String text,String[] options,String[] next,String reward){}
    private static final Map<String,Node> NODES=Map.ofEntries(
        Map.entry("away_left_1",new Node(NumberedEventDialogue.line("mother_goes_out","father_mother_left_1").text(),new String[]{"When will she be back?","Does she leave often?","Can I ask about the house?","Goodbye."},new String[]{"away_when_1","away_left_2","questions","close"},"")),
        Map.entry("away_left_2",new Node(NumberedEventDialogue.line("mother_goes_out","father_mother_left_2").text(),new String[]{"When will she be back?","Can I ask about the house?","Goodbye."},new String[]{"away_when_2","questions","close"},"")),
        Map.entry("away_when_1",new Node(NumberedEventDialogue.line("mother_goes_out","father_when_return_1").text(),new String[]{"Anything unusual around here?","Goodbye."},new String[]{"questions","close"},"")),
        Map.entry("away_when_2",new Node(NumberedEventDialogue.line("mother_goes_out","father_when_return_2").text(),new String[]{"Anything unusual around here?","Goodbye."},new String[]{"questions","close"},"")),
        Map.entry("mother",new Node("Getting bored already, sweetheart? Shall I find something else for us to do?",new String[]{"Yes, let's do something else.","No, I'm still enjoying myself."},new String[]{"end","close"},"")),
        Map.entry("root",new Node("Ah, there you are! Lovely day for a little music, isn't it?",new String[]{"What are you playing?","Can I ask you something?","How are things around here?","I'll leave you to it."},new String[]{"music","questions","family","close"},"")),
        Map.entry("music",new Node("An old melody. Funny how a tune can find its way home.",new String[]{"Where did you learn it?","It sounds familiar.","I'll leave you to it."},new String[]{"melody","melody","close"},"")),
        Map.entry("melody",new Node("Oh, here and there. Some things are older than they look.",new String[]{"Another question.","Enjoy your music."},new String[]{"root","close"},"")),
        Map.entry("questions",new Node("Of course, kiddo. What is it?",new String[]{"The house and its doors.","Those dolls and inventions.","The places beyond the garden.","Never mind."},new String[]{"house","watchers","world","root"},"")),
        Map.entry("house",new Node("Mother does like everything in its proper place. Even the little drawers have their arrangements.",new String[]{"What do you mean by arrangements?","Tell me what's in the locked basement.","That sounds boring."},new String[]{"marks","hesitate","root"},"")),
        Map.entry("marks",new Node("Just a little pattern she uses. Once you've seen it, it's difficult not to notice it everywhere.",new String[]{"I'll keep my eyes open.","Thank you."},new String[]{"root","close"},"drawer_marks")),
        Map.entry("watchers",new Node("Mother is fond of those dolls. Always placing them where they can see everything.",new String[]{"See everything?","What have you been building?","They're rather creepy."},new String[]{"blind","machines","root"},"")),
        Map.entry("blind",new Node("Oh! Not everything, of course. They have to face the right way. Walls get in the way, too.",new String[]{"Funny little things.","Another question."},new String[]{"close","root"},"blind_spots")),
        Map.entry("machines",new Node("Mother has clever machines working behind the scenes.",new String[]{"What do the machines do?","Do you enjoy building them?","Never mind."},new String[]{"gifts","workshop","root"},"")),
        Map.entry("gifts",new Node("They make sure the presents and surprises arrive where they ought to. Quite an arrangement, really.",new String[]{"It must take some planning.","Thank you."},new String[]{"root","close"},"gift_preparation")),
        Map.entry("workshop",new Node("I like making things. Especially when they make someone smile.",new String[]{"Another question.","Goodbye."},new String[]{"root","close"},"")),
        Map.entry("world",new Node("The garden is lovely. Beyond it? Some places have been forgotten for a very long time.",new String[]{"Even the trees seem unusual.","I should go exploring.","What about Other Wybie?"},new String[]{"trees","close","wybie"},"")),
        Map.entry("trees",new Node("You'd be surprised how empty some of those perfect trees are inside.",new String[]{"I'll look more carefully.","Another question."},new String[]{"close","root"},"hollow_trees")),
        Map.entry("family",new Node("Mother does like things perfect. Best not to disturb her arrangements.",new String[]{"What makes her happy?","What about Other Wybie?","Tell me her secrets."},new String[]{"kindness","wybie","hesitate"},"")),
        Map.entry("kindness",new Node("A kind word. A quiet evening. A family sitting down together.",new String[]{"That sounds nice.","Another question."},new String[]{"close","root"},"")),
        Map.entry("wybie",new Node("A helpful fellow. Sometimes quiet is how someone stays out of trouble.",new String[]{"I understand.","Why won't you tell me more?"},new String[]{"root","hesitate"},"")),
        Map.entry("hesitate",new Node("I don't think she'd want me discussing that. Best not to worry yourself.",new String[]{"Of course. Another question.","Tell me anyway."},new String[]{"root","uncomfortable"},"")),
        Map.entry("uncomfortable",new Node("Oh, I really must get back to my music now, kiddo.",new String[]{"Alright."},new String[]{"close"},""))
    );
    static void show(ServerPlayer p,Conversation c){
        int readyTicks=0;
        if(c.mother)IncidentalDialogue.say(p,"Other Mother",NODES.get(c.node).text);
        if(!c.mother){String sound=c.node.startsWith("away_")?"father_mother_goes_out_"+(c.node.startsWith("away_left_")?"mother_left_":"when_return_")+c.node.charAt(c.node.length()-1):"father_study_"+c.node;OtherMotherLoop.scene(p).loop.event.putString("StudyVoice",sound);readyTicks=VisitAudio.length(sound);VisitAudio.play(p,sound,false);}
        CONVERSATIONS.put(p.getUUID(),c);CONVERSATION_READY.put(p.getUUID(),OtherMotherLoop.now(p)+readyTicks);var node=NODES.get(c.node);var n=new CompoundTag();n.putBoolean("VisitConversation",true);n.putUUID("Nonce",c.nonce);n.putString("Speaker",c.mother?"Other Mother":"Other Father");n.putString("Text",node.text);n.put("Options",SecretsState.strings(Arrays.asList(node.options)));n.putInt("ReadyTicks",readyTicks);SecretsNetwork.send(p,n);
    }
    public static void choose(ServerPlayer p,UUID nonce,String choice){
        var c=CONVERSATIONS.get(p.getUUID());var s=OtherMotherLoop.active(p);
        if(c==null||s==null||!c.nonce.equals(nonce)||s.loop.phase!=FREE_TIME&&!(!c.mother&&MotherGoesOut.fatherAvailable(p))||OtherMotherLoop.now(p)>c.expires||s.loop.secrets.searching()||SecretDialogue.busy(p))return;
        var actor=p.serverLevel().getEntity(c.actor);if(!(actor instanceof ParentEntity)||actor.distanceToSqr(p)>49||!Objects.equals(c.actor,c.mother?s.mom:s.dad)){stopConversation(p);return;}
        if(choice.equals("close")){stopConversation(p);return;}
        if(OtherMotherLoop.now(p)<CONVERSATION_READY.getOrDefault(p.getUUID(),Long.MAX_VALUE))return;
        int index;try{index=Integer.parseInt(choice);}catch(NumberFormatException bad){return;}var node=NODES.get(c.node);if(index<0||index>=node.next.length)return;
        String next=node.next[index];
        if(next.equals("end")){stopConversation(p);next(p,s,true);return;}if(next.equals("close")){stopConversation(p);return;}
        var dest=NODES.get(next);if(dest==null)return;
        if(!dest.reward.isEmpty())reward(p,s,dest.reward);
        show(p,new Conversation(UUID.randomUUID(),c.actor,c.mother,next,OtherMotherLoop.now(p)+1200));
    }
    public static List<String> fatherStudyVoices(){return NODES.keySet().stream().filter(id->!id.equals("mother")&&!id.startsWith("away_")).sorted().map(id->"father_study_"+id).toList();}
    static boolean reward(ServerPlayer p,VisitData.Scene s,String id){
        var n=s.loop.event;var secret=SecretRegistry.ALL.get(id);var ledger=s.loop.secrets;
        if(secret==null||secret.tier()!=0||secret.severity()>.3||n.getBoolean("Rewarded")||ledger.journal.contains(id)||ledger.issued.containsValue(id)||ledger.claimed.contains("father:"+s.loop.visits+":"+id))return false;
        n.putBoolean("Rewarded",true);n.putString("Reward",id);ledger.claimed.add("father:"+s.loop.visits+":"+id);SecretTransactions.checkpoint(p);SecretTransactions.issue(p,id);return true;
    }
    public static boolean resumeDinner(ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s==null||!s.loop.event.getBoolean("DinnerPending"))return false;s.loop.secrets.search=SecretsState.Search.NORMAL;s.loop.secrets.pendingTicks=0;s.loop.secrets.searchTicks=0;var n=s.loop.event;for(String key:List.of("Voice","Sound","VoiceFinished","IntroElapsed","FatherVoice","FatherVoiceElapsed","FatherVoiceFinished"))n.remove(key);n.putBoolean("Resuming",true);n.putBoolean("Searching",false);n.putInt("ArrivalRemaining",0);n.putBoolean("GraceRolled",true);n.putInt("GraceRemaining",1200);OtherMotherLoop.phase(p,s,DINNER_WAIT);ensureFather(p,s);return true;}
    public static void debug(ServerPlayer p,String value){
        if(!p.hasPermissions(2))return;var s=OtherMotherLoop.active(p);if(s==null)return;
        if(!OtherWorld.isOther(p.level())){p.displayClientMessage(Component.literal("Return to the Other House before testing household events."),true);return;}
        var parts=value.split(",",2);String command=parts[0],arg=parts.length>1?parts[1]:"0";var n=s.loop.event;
        if(Set.of("arrival","window","ontime","late","grace","resume","meal","finish").contains(command)&&type(s)!=Type.FAMILY_DINNER){p.displayClientMessage(Component.literal("Force Family Dinner first."),true);return;}
        try{switch(command){
            case "explore"->start(p,s,Type.EXPLORATION_30_MIN);
            case "dinner"->start(p,s,Type.FAMILY_DINNER);
            case "activity"->start(p,s,Type.ACTIVITY);
            case "next","end"->next(p,s,true);
            case "time"->n.putInt("Remaining",Math.max(0,Math.min(EXPLORATION_TICKS,Integer.parseInt(arg))));
            case "arrival"->n.putInt("ArrivalRemaining",Math.max(0,Math.min(6000,Integer.parseInt(arg))));
            case "window"->FamilyDinner.beginWindow(p,s);
            case "ontime"->{n.putInt("ArrivalRemaining",100);n.putBoolean("Searching",false);n.putBoolean("Resuming",false);n.remove("ArrivalAwarded");OtherMotherLoop.phase(p,s,DINNER_WAIT);}
            case "late"->{n.putInt("ArrivalRemaining",0);n.putBoolean("GraceRolled",true);n.putInt("GraceRemaining",1200);n.putBoolean("Searching",false);n.putBoolean("Resuming",false);n.remove("ArrivalAwarded");OtherMotherLoop.phase(p,s,DINNER_WAIT);}
            case "grace"->{n.putInt("ArrivalRemaining",0);n.putBoolean("GraceRolled",true);n.putInt("GraceRemaining",0);}
            case "resume"->resumeDinner(p);
            case "meal"->{ensureFather(p,s);OtherMotherLoop.phase(p,s,DINNER);n.putInt("MealElapsed",0);}
            case "finish"->{OtherMotherLoop.phase(p,s,DINNER_FINISH);n.putBoolean("FatherFinished",false);}
            case "mother"->{var m=MotherSearch.mother(p);if(m!=null)p.teleportTo(p.serverLevel(),m.getX()+2,m.getY(),m.getZ(),p.getYRot(),0);}
            case "study"->p.teleportTo(p.serverLevel(),s.piano.getX()+.5,s.piano.getY(),s.piano.getZ()-3.5,p.getYRot(),0);
            case "table"->p.teleportTo(p.serverLevel(),s.table.getX()+.5,s.table.getY(),s.table.getZ()-6,p.getYRot(),0);
            case "talk"->openConversation(p,false);
            case "reward"->reward(p,s,arg);
            case "reset_reward"->{n.putBoolean("Rewarded",false);s.loop.secrets.claimed.removeIf(id->id.startsWith("father:"+s.loop.visits+":"));}
            case "voice"->{int i=Integer.parseInt(arg);var pools=pools();if(i>=0&&i<60){var pool=pools.get(i/2);VisitAudio.play(p,pool.sound(i%2),false);}else if(i>=60&&i<68)VisitAudio.play(p,FamilyDinner.FATHER_VOICES[i-60],false);else if(i>=68&&i<68+fatherStudyVoices().size())VisitAudio.play(p,fatherStudyVoices().get(i-68),false);}
            case "distribution"->{double total=0;for(int i=0;i<10000;i++)total+=FamilyDinner.graceTicks(s.loop.suspicion,p.getRandom().nextDouble())/1200.;p.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,"Dinner grace: 1–10 min, mean %.2f min at current suspicion",total/10000)));}
            case "state"->p.sendSystemMessage(Component.literal(type(s)+" / "+s.loop.phase+" / "+n));
            default->{}
        }}catch(NumberFormatException ignored){}OtherMotherLoop.changed(p);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){CONVERSATIONS.remove(e.getEntity().getUUID());CONVERSATION_READY.remove(e.getEntity().getUUID());FamilyDinner.forget(e.getEntity().getUUID());if(e.getEntity() instanceof ServerPlayer p)OtherMotherLoop.changed(p);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;CONVERSATIONS.remove(p.getUUID());CONVERSATION_READY.remove(p.getUUID());var s=OtherMotherLoop.scene(p);if(s==null)return;
        if(type(s)==Type.HOUSE_PARTY||type(s)==Type.PLAYER_PARTY)HouseParty.reconnect(p,s);if(s.loop.event.getBoolean("WybiePending"))WybieAdventure.reconnect(p,s);if(!s.loop.event.getBoolean("ShowCleanupDeferred")&&OtherMotherLoop.active(p)==s&&!s.loop.secrets.searching()&&!Reconciliation.active(p)){if(!resumeNumberedIntro(p)){if(TheatreVisit.pending(s))TheatreVisit.recover(p,s);if(MouseCircusVisit.pending(s))MouseCircusVisit.recover(p,s);}}if(type(s)==Type.OTHER_MOTHER_GOES_OUT)MotherGoesOut.reconnect(p,s);
        // Hardware-release/pull state cannot survive a disconnected client;
        // preserve the authoritative warning stage and the match itself.
        s.loop.event.remove("DeliveryAwaitRelease");s.loop.event.remove("PullStarted");s.loop.event.remove("TelescopeNonce");
        FamilyDinner.reconnect(p,s);
        if(type(s)==Type.FAMILY_GAME_NIGHT){s.loop.event.putUUID("GameMoveNonce",UUID.randomUUID());if(s.loop.event.hasUUID("GameChoiceNonce"))s.loop.event.putUUID("GameChoiceNonce",UUID.randomUUID());}
        if(!scripted(s))return;
        // Resume gameplay clocks, but replay an unfinished spoken introduction/greeting.
        for(String key:List.of("Voice","Sound","IntroElapsed","VoiceFinished","FatherVoice","FatherVoiceElapsed","FatherVoiceFinished"))s.loop.event.remove(key);s.welcomeAfter=0;OtherMotherLoop.changed(p);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent e){CONVERSATIONS.clear();CONVERSATION_READY.clear();FamilyDinner.clearQuestions();SpecialDelivery.clearCaches();}
    private VisitEvents(){}
}
