package com.coraline.feast;

import com.coraline.secrets.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import java.util.*;

/** Witnesses report only what they saw, after a warning and a physical conversation. */
public final class PartySurveillance {
    /** One spoken warning owns the ordinary party conversation channel until its actual line ends. */
    public static boolean warningSpeechActive(VisitData.Scene s){var n=HouseParty.data(s);return n.getLong("Clock")<n.getLong("WitnessSpeechUntil");}
    public static boolean sees(Mob witness,ServerPlayer p,double distance){
        var secrets=SecretTransactions.state(p);if(witness==null||!witness.isAlive()||!p.isAlive()||witness.level()!=p.level()||secrets==null||p.isInvisible()||secrets.invisibleUntil>OtherMotherLoop.now(p)||witness.distanceToSqr(p)>distance*distance||!witness.hasLineOfSight(p))return false;
        var delta=p.getEyePosition().subtract(witness.getEyePosition());double len=delta.horizontalDistance();if(len<.01)return true;
        double yaw=Math.toRadians(witness.getYHeadRot());return (-Math.sin(yaw)*delta.x+Math.cos(yaw)*delta.z)/len>=.5;
    }
    static String offense(ServerPlayer p,VisitData.Scene s){
        if(!PartyRegions.allowed(p,p.position()))return SecretDialogue.secretArea(p,p.blockPosition())?"forbidden":"outside";
        var party=HouseParty.data(s);if(PlayerParty.isPlayerParty(s)&&party.getLong("StorageAttemptUntil")>party.getLong("Clock")&&p.distanceToSqr(BlockPos.of(party.getLong("StorageAttempt")).getCenter())<25)return "drawer";
        var cabinets=s.loop.secrets.world.getCompound("HouseCabinets").getList("Cabinets",Tag.TAG_COMPOUND);
        for(var tag:cabinets){var c=(CompoundTag)tag;if(c.getBoolean("Open")&&!(PlayerParty.isPlayerParty(s)&&PlayerParty.protectedCabinet(p,c.getInt("Id")))&&p.distanceToSqr(BlockPos.of(c.getLong("Pos")).getCenter())<25)return "drawer";}
        for(var held:List.of(p.getMainHandItem(),p.getOffhandItem()))if(!SecretTransactions.evidence(p,held).isEmpty()||HouseCabinets.forbiddenKey(p,held)||held.is(SecretsItems.JOURNAL.get())&&SecretTransactions.owned(p,held))return "restricted_item";
        var n=HouseParty.data(s);return n.getLong("RestrictedUntil")>n.getLong("Clock")?"forbidden":"";
    }
    /** Existing restricted mechanisms may publish a bounded visible action, never journal contents. */
    public static void restrictedAction(ServerPlayer p,BlockPos at){var s=HouseParty.active(p);if(s!=null&&p.distanceToSqr(at.getCenter())<36){var n=HouseParty.data(s);n.putLong("RestrictedUntil",n.getLong("Clock")+30);n.putLong("RestrictedPosition",at.asLong());}}
    public static void tick(ServerPlayer p,VisitData.Scene s){
        if(p.tickCount%5!=0)return;var n=HouseParty.data(s);long now=n.getLong("Clock");boolean searching=s.loop.secrets.searching();
        boolean absent=PlayerParty.isPlayerParty(s)?PlayerPartyLedger.absence(n,PartyRegions.allowed(p,p.position()),searching,s.loop.suspicion,p.getRandom().nextDouble(),5,p.blockPosition().asLong()):HousePartyLedger.absence(n,PartyRegions.allowed(p,p.position()),searching,s.loop.suspicion,p.getRandom().nextDouble(),5,p.blockPosition().asLong());
        if(absent){
            // The variant-specific absence delay already elapsed; never add a second grace.
            if(PlayerParty.isPlayerParty(s))NumberedEventDialogue.mother(p,"player_party","search");
            BlockPos last=n.contains("LastPermittedPosition")?BlockPos.of(n.getLong("LastPermittedPosition")):s.table;
            seek(p,s,last,"absence",null,"");searching=true;
        }
        if(searching)return;String action=offense(p,s);
        var guests=PartyGuests.guests(s);
        // Keep the first current warning stable. Other witnesses may still physically report
        // an already recorded incident, but cannot replace this speaker every five ticks.
        boolean reportInFlight=guests.stream().anyMatch(g->g.getBoolean("ReportPending")
                &&g.getString("State").equals("REPORTING")&&PartyGuests.entity(p,g)!=null
                &&now-g.getLong("ReportStarted")<200);
        CompoundTag warning=null;
        for(var g:guests)if(g.getString("State").equals("WARNING")&&sees(PartyGuests.entity(p,g),p,14)
                &&action.equals(g.getString("ObservedOffense"))&&!action.isEmpty()){warning=g;break;}
        for(var g:guests){
            var witness=PartyGuests.entity(p,g);if(witness==null||Set.of("QUEUED","ARRIVING","WAITING_AT_DOOR","ENTERING","GONE","LEAVING","REPORTING").contains(g.getString("State")))continue;
            boolean visible=sees(witness,p,14);String seen=visible?action:"";
            if(warning!=null&&g!=warning||warning==null&&(warningSpeechActive(s)||reportInFlight)){
                var cleared=HousePartyLedger.observe(g,"","",p.blockPosition().asLong(),now,0,150);
                if(cleared==HousePartyLedger.Notice.COMPLIED&&g.getString("State").equals("WARNING"))g.putString("State","WANDERING");
                continue;
            }
            String identified=visible?SecretTransactions.evidence(p,p.getMainHandItem()):"";
            var notice=HousePartyLedger.observe(g,seen,identified,p.blockPosition().asLong(),now,5,n.contains("DebugWarningGrace")?Math.max(100,Math.min(200,n.getInt("DebugWarningGrace"))):150);
            if(notice==HousePartyLedger.Notice.WARN){
                warning=g;PartyConversations.close(p,s);PartyMatches.suspend(p,s);g.putString("State","WARNING");
                int grace=(int)Math.max(0,g.getLong("WarningUntil")-now),spoken;
                if(!g.getString("Kind").equals("other_wybie")&&seen.equals("restricted_item")){VisitAudio.caption(p,g.getString("Name"),"You shouldn't be carrying that here. Put it away, or I'll have to tell Other Mother.",100);spoken=100;}
                else if(!g.getString("Kind").equals("other_wybie"))spoken=NumberedEventDialogue.say(p,PlayerParty.isPlayerParty(s)?"player_party":"house_party","guest_warning_"+(PlayerParty.isPlayerParty(s)&&seen.equals("drawer")?"storage":seen)+"_"+(1+p.getRandom().nextInt(2)));
                else {VisitAudio.caption(p,"Other Wybie","Wybie shakes his head and points back toward the party.",100);spoken=100;}
                n.putLong("WitnessSpeechUntil",now+Math.max(0,spoken));
                g.putLong("WarningUntil",now+Math.max(0,spoken)+grace);
                witness.getNavigation().stop();witness.getLookControl().setLookAt(p,30,20);PartyGuests.gesture(witness,3,Math.max(60,spoken));
            }else if(notice==HousePartyLedger.Notice.COMPLIED){if(g.getString("State").equals("WARNING"))g.putString("State","WANDERING");}
            else if(notice==HousePartyLedger.Notice.REPORT){PartyConversations.closeGuest(p,s,g.getUUID("Id"));PartyMatches.suspend(p,s);g.putLong("ReportStarted",now);g.putLong("ReportRetry",0);}
        }
    }
    static void report(Mob guest,ServerPlayer p,VisitData.Scene s,CompoundTag g){
        var mother=MotherSearch.mother(p);if(mother==null||guest.level()!=mother.level())return;long now=HouseParty.data(s).getLong("Clock");
        if(guest.distanceToSqr(mother)<=9&&guest.hasLineOfSight(mother)){
            guest.getNavigation().stop();guest.getLookControl().setLookAt(mother,25,20);
            if(HousePartyLedger.receiveReport(g)){
                PartyReportDialogue.received(s,g.getUUID("Incident"),g.getString("ObservedOffense"));
                if(guest.distanceToSqr(p)<144&&guest.hasLineOfSight(p)&&!g.getString("Kind").equals("other_wybie"))NumberedEventDialogue.say(p,PlayerParty.isPlayerParty(s)?"player_party":"house_party","guest_report_"+(1+p.getRandom().nextInt(2)));
                seek(p,s,BlockPos.of(g.getLong("ObservedPosition")),g.getString("ObservedOffense"),g.getUUID("Incident"),g.getString("IdentifiedSecret"));
                HousePartyLedger.finishReport(g);
            }return;
        }
        if(guest.tickCount%40==0){
            var path=guest.getNavigation().createPath(mother,1);if(path!=null&&path.canReach()){guest.getNavigation().moveTo(path,1.05);g.putLong("ReportRetry",now);}
            else if(now-g.getLong("ReportRetry")>=100){
                // Re-route to a nearby reachable waypoint; no wall-through notification.
                for(var at:BlockPos.betweenClosed(mother.blockPosition().offset(-3,0,-3),mother.blockPosition().offset(3,0,3))){var alternate=guest.getNavigation().createPath(at,0);if(alternate!=null&&alternate.canReach()&&PartyRegions.allowed(p,net.minecraft.world.phys.Vec3.atBottomCenterOf(at))){guest.getNavigation().moveTo(alternate,1);break;}}
                g.putLong("ReportRetry",now);
            }
        }
    }
    private static void seek(ServerPlayer p,VisitData.Scene s,BlockPos last,String offense,UUID incident,String identified){
        var n=HouseParty.data(s);if(incident!=null&&n.hasUUID("LastReport")&&incident.equals(n.getUUID("LastReport")))return;
        if(incident!=null)n.putUUID("LastReport",incident);
        if(SecretRegistry.ALL.containsKey(identified))s.loop.secrets.exposed.add(identified);
        // Pursuit is evidence-based. The existing contact guard owns confrontation and penalties.
        if(s.loop.secrets.search!=SecretsState.Search.DIRECT_CHASE){
            MotherInterrupts.interrupt(p);s.loop.secrets.search=SecretsState.Search.SEARCHING;s.loop.secrets.target=last;
            s.loop.secrets.world.putLong("SpyReportOrigin",last.asLong());
            s.loop.secrets.world.putString("ChaseOffense",Set.of("drawer","restricted_item").contains(offense)?SecretDialogue.Event.RESTRICTED_ITEM_CAUGHT.name():(offense.equals("forbidden")?SecretDialogue.Event.SECRET_AREA_CAUGHT:SecretDialogue.Event.ORDINARY_AREA_CAUGHT).name());
        }
        SecretTransactions.checkpoint(p);
    }
    public static String debugOperator(ServerPlayer p,VisitData.Scene s,String action,String value){
        if(!NumberedDebugActions.operator(p,s)||HouseParty.active(p)!=s)return "No owned operator party.";
        var n=HouseParty.data(s);long now=n.getLong("Clock");
        if(action.equals("grace")){n.putInt("DebugWarningGrace",NumberedDebugActions.integer(value,100,200));OtherMotherLoop.changed(p);return "Operator warning grace set within the supported 100–200 tick range.";}
        if(Reconciliation.active(p)||SecretDialogue.busy(p))return "Wait for actual reconciliation/dialogue to finish.";
        if(action.equals("search")){
            var origin=n.contains("LastPermittedPosition")?BlockPos.of(n.getLong("LastPermittedPosition")):s.table;
            seek(p,s,origin,"absence",null,"");return "Search started from the last permitted position; physical capture still required.";
        }
        String seen=offense(p,s);if(seen.isEmpty())return "No actual visible offense exists. No guest report or Mother knowledge was invented.";
        for(var g:PartyGuests.guests(s)){
            var actor=PartyGuests.entity(p,g);
            if(actor==null||!sees(actor,p,14)||!Set.of("WANDERING","CONVERSING","WARNING").contains(g.getString("State")))continue;
            if(action.equals("warning")){
                g.putInt("Recognition",20);
                var notice=HousePartyLedger.observe(g,seen,SecretTransactions.evidence(p,p.getMainHandItem()),p.blockPosition().asLong(),now,20,n.contains("DebugWarningGrace")?n.getInt("DebugWarningGrace"):150);
                if(notice!=HousePartyLedger.Notice.WARN)return "This witness already has a warning/report in progress.";
                PartyConversations.closeGuest(p,s,g.getUUID("Id"));PartyMatches.suspend(p,s);g.putString("State","WARNING");
                if(!g.getString("Kind").equals("other_wybie"))NumberedEventDialogue.say(p,PlayerParty.isPlayerParty(s)?"player_party":"house_party","guest_warning_"+(PlayerParty.isPlayerParty(s)&&seen.equals("drawer")?"storage":seen)+"_1");
                else VisitAudio.caption(p,"Other Wybie","Wybie shakes his head and points toward the party.",100);
                PartyGuests.gesture(actor,3,60);OtherMotherLoop.changed(p);return "Actual visible offense warned; compliance still cancels the report.";
            }
            if(action.equals("report")&&g.contains("WarningUntil")&&seen.equals(g.getString("ObservedOffense"))){
                g.putLong("WarningUntil",now);
                var notice=HousePartyLedger.observe(g,seen,SecretTransactions.evidence(p,p.getMainHandItem()),p.blockPosition().asLong(),now,5,150);
                if(notice==HousePartyLedger.Notice.REPORT){PartyConversations.closeGuest(p,s,g.getUUID("Id"));PartyMatches.suspend(p,s);g.putLong("ReportStarted",now);g.putLong("ReportRetry",0);OtherMotherLoop.changed(p);return "Ignored warning escalated; the actual witness must physically reach Mother to report.";}
            }
        }
        return "No matching visible witness/previous warning is available.";
    }

    private PartySurveillance(){}
}
