package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.parents.ParentEntity;
import com.coraline.secrets.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static com.coraline.feast.OtherMotherLoopState.Phase.*;

/** One house-party component of the existing visit director; no separate event scheduler. */
public final class HouseParty {
    public static CompoundTag data(VisitData.Scene s){return s.loop.event.getCompound("Party");}
    public static VisitData.Scene active(ServerPlayer p){var shared=OtherMotherLoop.scene(p);if(PlayerParty.isPlayerParty(shared))return PlayerParty.active(p);var s=OtherMotherLoop.active(p);return s!=null&&VisitEvents.type(s)==VisitEvents.Type.HOUSE_PARTY&&HousePartyLedger.owned(data(s),p.getUUID(),s.loop.event.getInt("Serial"))?s:null;}
    public static OtherMotherDialogue.Pool pool(String category,OtherMotherWelcome.State state){return new OtherMotherDialogue.Pool("events/house_party/"+category+"/"+SecretDialogue.stateId(state),2);}
    public static List<OtherMotherDialogue.Pool> pools(){var result=new ArrayList<OtherMotherDialogue.Pool>();for(String category:List.of("invitation","checkin","guest_report","feast","food_warning","completion"))for(var state:OtherMotherWelcome.State.values())result.add(pool(category,state));return result;}
    public static boolean prepare(ServerPlayer p,VisitData.Scene s){
        if(!NumberedEventDialogue.ready("house_party"))return false;
        int capacity=PartyRegions.capacity(p);if(capacity<6)return false;capacity=Math.max(24,capacity*2);
        var n=new CompoundTag();HousePartyLedger.begin(n,p.getUUID(),s.loop.event.getInt("Serial"),p.getRandom().nextLong(),capacity);
        n.putBoolean("Activated",false);s.loop.event.put("Party",n);s.loop.event.putBoolean("PartyPending",true);PartyGuests.roster(p,s);return PartyDecorations.prepare(p,s,false);
    }
    public static void begin(ServerPlayer p,VisitData.Scene s){var n=data(s);n.putBoolean("AwaitingHouse",true);n.putBoolean("Activated",false);n.putString("ResumePhase",PARTY_SOCIAL.name());OtherMotherLoop.phase(p,s,PARTY_SOCIAL);PartyEntrance.prepare(p,s);OtherMotherLoop.changed(p);}
    public static boolean insideHouse(ServerPlayer p,Vec3 at){var r=SecretWorld.realm(p);if(r==null||r.house==null)return false;var north=com.coraline.secrets.HouseCabinets.at(r.house,10,5,13);var south=com.coraline.secrets.HouseCabinets.at(r.house,61,23,68);return new net.minecraft.world.phys.AABB(north,south).contains(at)||PartyRegions.rooms().stream().filter(room->room.id().equals("front_turret_room")).anyMatch(room->room.bounds(r.house).contains(at));}
    public static void status(ServerPlayer p,VisitData.Scene s,CompoundTag out){var n=data(s);out.putBoolean("PartyMusic",n.getBoolean("Activated")&&!n.getBoolean("Suspended")&&!s.loop.secrets.searching()&&insideHouse(p,p.position()));out.putInt("Remaining",n.getBoolean("Activated")?remaining(s):0);out.putString("ShowObjective",n.getBoolean("AwaitingHouse")?"Enter the house with Other Mother":s.loop.phase==PARTY_FEAST?(n.getInt("CelebrationElapsed")>0?"Your celebration":"Party feast"):s.loop.phase==PARTY_ENDING?"Say goodbye to your guests":"House party — talk, explore and play with your guests");PartyKidsGames.status(p,s,out);}
    public static int remaining(VisitData.Scene s){var n=data(s);String phase=s.loop.phase==EXPLORING?n.getString("ResumePhase"):s.loop.phase.name();return switch(phase){case "PARTY_SOCIAL"->Math.max(0,HousePartyLedger.SOCIAL_TICKS-n.getInt("SocialElapsed"));case "PARTY_FEAST"->n.getInt("CelebrationElapsed")>0?Math.max(0,900-n.getInt("CelebrationElapsed")):Math.max(0,n.getInt("FeastDuration")-n.getInt("FeastElapsed"));case "PARTY_ENDING"->Math.max(0,1200-n.getInt("DepartureTicks"));default->0;};}
    /** Called once by the visit director before pursuit/dialogue arbitration. */
    public static void backgroundTick(ServerPlayer p,VisitData.Scene s){
        if(PlayerParty.isPlayerParty(s)){PlayerParty.backgroundTick(p,s);return;}
        if(VisitEvents.type(s)!=VisitEvents.Type.HOUSE_PARTY||!s.loop.event.getBoolean("PartyPending"))return;
        var n=data(s);if(n.getInt("PacingRevision")<2){n.putInt("FeastDuration",1200);n.putInt("PacingRevision",2);OtherMotherLoop.changed(p);}if(s.loop.suspicion>=1||s.loop.secrets.search==SecretsState.Search.SCRIPTED_TRAP){cleanup(p,s);return;}
        if(n.getBoolean("AwaitingHouse")){var mother=MotherSearch.mother(p);if(mother==null||!insideHouse(p,p.position())||!insideHouse(p,mother.position())||mother.distanceToSqr(p)>144)return;n.remove("AwaitingHouse");n.putBoolean("Activated",true);OtherMotherLoop.changed(p);}
        if(!n.getBoolean("Activated")||p.isSleeping())return;
        PartyGuests.upgradeRoster(p,s);
        boolean confrontation=Reconciliation.active(p)||SecretDialogue.busy(p);
        String phase=s.loop.phase==EXPLORING?n.getString("ResumePhase"):s.loop.phase.name();
        HousePartyLedger.advance(n,1,phase.equals(PARTY_SOCIAL.name()),phase.equals(PARTY_FEAST.name()),confrontation);
        if(confrontation)return;
        PartySurveillance.tick(p,s);
        PartyGuests.tick(p,s);
        PartyConversations.tick(p,s);
        PartyKidsGames.tick(p,s);
        PartyMatches.tick(p,s);
        int social=n.getInt("SocialElapsed");
        if(social>=1200&&!n.getBoolean("SocialToast")){n.putBoolean("SocialToast",true);VisitAudio.caption(p,"Other Mother","A toast to our guest of honour! Come join the neighbours, darling.",100);for(var g:PartyGuests.guests(s)){var actor=PartyGuests.entity(p,g);if(actor!=null)PartyGuests.gesture(actor,1,70);}}
        if(s.loop.phase==PARTY_SOCIAL&&p.tickCount%100==0)PartyGameStations.prepare(p,s);
        if(social>=2600&&!n.getBoolean("GamesInvitation")&&p.tickCount%100==0&&PartyGameStations.invite(p,s))n.putBoolean("GamesInvitation",true);
        if(p.tickCount%20==0)OtherMotherLoop.changed(p);
    }
    public static boolean tick(ServerPlayer p,VisitData.Scene s){
        if(PlayerParty.isPlayerParty(s))return PlayerParty.tick(p,s);
        var n=data(s);if(!n.getBoolean("Pending")||PartySurveillance.warningSpeechActive(s))return true;
        if(s.loop.phase==PARTY_SOCIAL&&n.getBoolean("FeastDue")&&!PartyMatches.active(s)){
            PartyConversations.close(p,s);OtherMotherLoop.phase(p,s,PARTY_FEAST_CALL);n.putString("ResumePhase",PARTY_FEAST_CALL.name());
        }
        if(s.loop.phase==PARTY_FEAST_CALL){
            if(p.tickCount%20!=0)return true;
            if(!PartyBanquet.prepare(p,s)){
                n.putInt("SeatRetry",n.getInt("SeatRetry")+1);
                if(n.getInt("SeatRetry")>=60){n.putBoolean("BanquetFailed",true);n.putBoolean("ClosingSpoken",true);OtherMotherLoop.phase(p,s,PARTY_ENDING);n.putString("ResumePhase",PARTY_ENDING.name());PartyGuests.depart(p,s);VisitAudio.caption(p,"House Party","The guests begin saying goodbye. There isn't a safe place to set out the banquet here.",160);}
                return true;
            }
            if(!n.getBoolean("FeastAnnounced")){if(!VisitEvents.speak(p,s,pool("feast",SecretDialogue.state(p))))VisitAudio.caption(p,"House Party","The feast is ready in the dining room. Right-click the glowing seat at the head of the table.",120);n.putBoolean("FeastAnnounced",true);}
            var seat=PartyBanquet.playerChair(p,s);if(seat!=null)seat.setGlowingTag(true);
            OtherMotherLoop.phase(p,s,PARTY_FEAST);n.putString("ResumePhase",PARTY_FEAST.name());
        }
        if(s.loop.phase==PARTY_FEAST){
            if(n.getInt("CelebrationElapsed")==0)PartyBanquet.tick(p,s);
            if(n.getInt("FeastElapsed")>=n.getInt("FeastDuration")){n.putInt("CelebrationElapsed",n.getInt("CelebrationElapsed")+1);if(PartyCelebration.tick(p,s)){OtherMotherLoop.phase(p,s,PARTY_ENDING);n.putString("ResumePhase",PARTY_ENDING.name());PartyGuests.depart(p,s);}}
        }
        if(s.loop.phase==PARTY_ENDING){
            int departure=n.getInt("DepartureTicks")+1;n.putInt("DepartureTicks",departure);
            if(departure>=1200){PartyConversations.close(p,s);VisitAudio.stopVoice(p);for(var guest:PartyGuests.guests(s)){var actor=PartyGuests.entity(p,guest);if(actor!=null)PartyGuests.release(actor);guest.putString("State","GONE");}n.putBoolean("Completed",!n.getBoolean("BanquetFailed"));cleanup(p,s);VisitEvents.next(p,s,false);return true;}
            if(!n.getBoolean("ClosingSpoken")){
                var mother=MotherSearch.mother(p);if(mother==null||mother.distanceToSqr(p)>100||!mother.hasLineOfSight(p))return true;
                if(!VisitEvents.speak(p,s,pool("completion",SecretDialogue.state(p))))return true;n.putBoolean("ClosingSpoken",true);
            }
            if(PartyGuests.departed(p,s)){n.putBoolean("Completed",!n.getBoolean("BanquetFailed"));cleanup(p,s);VisitEvents.next(p,s,false);}
        }return true;
    }
    public static void parentControl(ParentEntity actor,ServerPlayer p,VisitData.Scene s){
        if(PlayerParty.isPlayerParty(s)){PlayerParty.parentControl(actor,p,s);return;}
        if(data(s).getBoolean("AwaitingHouse")){if(actor.isMom()){if(!insideHouse(p,p.position()))VisitEvents.approachForProposal(actor,p);else{var r=SecretWorld.realm(p);if(r!=null&&r.house!=null)TheatreVisit.navigate(actor,com.coraline.secrets.HouseCabinets.at(r.house,34,6,16),p,false);}}return;}
        if(s.loop.phase==PARTY_FEAST&&PartyBanquet.seatHost(actor,p,s))return;
        actor.stopRiding();actor.setMaxUpStep(1.05F);
        if(actor.isMom()&&PartyEntrance.host(actor,p,s))return;
        var n=data(s);String key=actor.isMom()?"Mother":"Father";
        if(actor.tickCount%40!=0)return;
        long now=n.getLong("Clock");if(now<n.getLong(key+"Rest")){actor.getNavigation().stop();if(actor.distanceToSqr(p)<64)actor.lookAtStoryPlayer(p);return;}
        if(n.contains(key+"Target")){
            var target=net.minecraft.core.BlockPos.of(n.getLong(key+"Target"));
            if(actor.distanceToSqr(Vec3.atBottomCenterOf(target))<3||now>n.getLong(key+"Until")){n.remove(key+"Target");n.putLong(key+"Rest",now+120+p.getRandom().nextInt(281));actor.getNavigation().stop();}
            else actor.getNavigation().moveTo(target.getX()+.5,target.getY(),target.getZ()+.5,.8);return;
        }
        PartyRegions.destination(p,actor,p.getRandom().nextInt(PartyRegions.rooms().size()),p.getRandom().nextLong()).ifPresent(at->{n.putLong(key+"Target",at.asLong());n.putLong(key+"Until",now+600);actor.getNavigation().moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,.8);});
    }
    public static void suspend(ServerPlayer p,VisitData.Scene s){if(PlayerParty.isPlayerParty(s)){PlayerParty.suspend(p,s);return;}if(!s.loop.event.getBoolean("PartyPending"))return;var n=data(s);if(s.loop.phase.name().startsWith("PARTY_"))n.putString("ResumePhase",s.loop.phase.name());PartyConversations.close(p,s);PartyMatches.suspend(p,s);}
    public static boolean resume(ServerPlayer p){var s=OtherMotherLoop.scene(p);if(PlayerParty.isPlayerParty(s)){PlayerParty.resume(p,s);return true;}if(s==null||VisitEvents.type(s)!=VisitEvents.Type.HOUSE_PARTY||!s.loop.event.getBoolean("PartyPending"))return false;var n=data(s);HousePartyLedger.reconciled(n);var phase=PARTY_SOCIAL;try{var saved=OtherMotherLoopState.Phase.valueOf(n.getString("ResumePhase"));if(saved.name().startsWith("PARTY_"))phase=saved;}catch(IllegalArgumentException ignored){}OtherMotherLoop.phase(p,s,phase);return true;}
    public static void reconnect(ServerPlayer p,VisitData.Scene s){if(PlayerParty.isPlayerParty(s)){PlayerParty.reconnect(p,s);return;}PartyConversations.close(p,s);PartyMatches.suspend(p,s);PartyEscrow.recover(p,Set.of());}
    public static boolean interact(ServerPlayer p,ParentEntity parent){
        if(PlayerParty.active(p)!=null)return PlayerParty.interact(p,parent);
        var s=active(p);if(s==null||!Objects.equals(parent.getUUID(),parent.isMom()?s.mom:s.dad)||parent.distanceToSqr(p)>36||!parent.hasLineOfSight(p)||s.loop.secrets.searching()||SecretDialogue.busy(p))return false;
        if(parent.isMom()&&MotherInterrupts.blackmail(p))return true;
        var n=data(s);if(!n.getBoolean("Activated")||n.getLong("Clock")<n.getLong("HostVoiceUntil")||PartySurveillance.warningSpeechActive(s))return true;
        PartyConversations.close(p,s);int length;
        if(parent.isMom())length=NumberedEventDialogue.mother(p,"house_party","check");
        else {String category=s.loop.phase==PARTY_FEAST?"feast":s.loop.phase==PARTY_ENDING?"finish":PartyMatches.active(s)?"game":n.getLong("Clock")<2400?"greeting":"social";length=NumberedEventDialogue.say(p,"house_party","father_"+category+"_"+(1+p.getRandom().nextInt(2)));}
        n.putLong("HostVoiceUntil",n.getLong("Clock")+length+80);parent.lookAtStoryPlayer(p);return true;
    }
    public static void debug(ServerPlayer p,String input){
        if(!p.hasPermissions(2)||!OtherWorld.isOther(p.level()))return;var s=OtherMotherLoop.active(p);if(s==null)return;
        String[] args=input.split(",",2);if(args[0].equals("force")){VisitEvents.start(p,s,VisitEvents.Type.HOUSE_PARTY);return;}if(active(p)==null)return;
        var n=data(s);try{switch(args[0]){
            case "social"->begin(p,s);
            case "time"->{if(args.length==2){int ticks=Math.max(0,Math.min(HousePartyLedger.SOCIAL_TICKS,Integer.parseInt(args[1])));n.putInt("SocialElapsed",ticks);n.putBoolean("FeastDue",ticks==HousePartyLedger.SOCIAL_TICKS);}}
            case "feast"->{n.putInt("SocialElapsed",HousePartyLedger.SOCIAL_TICKS);n.putBoolean("FeastDue",true);}
            case "finish"->n.putInt("FeastElapsed",n.getInt("FeastDuration"));
            case "state"->p.sendSystemMessage(net.minecraft.network.chat.Component.literal(n.toString()));
            case "resume"->resume(p);
            default->{}
        }}catch(NumberFormatException ignored){}OtherMotherLoop.changed(p);
    }
    public static boolean cleanup(ServerPlayer p,VisitData.Scene s){
        if(PlayerParty.isPlayerParty(s))return PlayerParty.cleanup(p,s);
        if(!s.loop.event.contains("Party"))return true;if(HouseParty.data(s).getBoolean("Completed"))for(var guest:PartyGuests.guests(s))PartyCelebration.leavePresent(p,s,guest,null);PartyKidsGames.finish(p,s,"");PartyConversations.close(p,s);PartyMatches.cleanup(p,s);PartyEscrow.cancelSerial(p,s.loop.event.getInt("Serial"));PartyBanquet.cleanup(p,s);PartyGuests.cleanup(p,s);PartyDecorations.cleanup(p,s);
        s.loop.event.remove("PartyPending");s.loop.event.remove("Party");return true;
    }
    private HouseParty(){}
}
