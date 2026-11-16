package com.coraline.secrets;

import com.coraline.feast.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Owner-scoped presentation queue sharing the visit dialogue scheduler and classifier. */
public final class SecretDialogue {
    public enum Event { ORDINARY_AREA_CAUGHT("ordinary"), SECRET_AREA_CAUGHT("secret"), RESTRICTED_ITEM_CAUGHT("item"), BLACKMAIL_OFFER("offer"), BLACKMAIL_ACCEPTED("accept"), BLACKMAIL_REJECTED("reject"), DELIVERY_OPENED("delivery");
        public final String id; Event(String id){this.id=id;}
    }
    public static String stateId(OtherMotherWelcome.State state){return switch(state){
        case LOW_TRUST_LOW_SUSPICION->"low_low";case MID_TRUST_MID_SUSPICION->"mid_mid";
        case HIGH_TRUST_LOW_SUSPICION->"high_low";case HIGH_SUSPICION_LOW_TRUST->"low_high";case HIGH_TRUST_HIGH_SUSPICION->"high_high";};}
    public static OtherMotherDialogue.Pool pool(Event event,OtherMotherWelcome.State state){return event==Event.DELIVERY_OPENED?SpecialDelivery.pool("caught_opening",state):new OtherMotherDialogue.Pool("secrets/"+event.id+"/"+stateId(state),2);}
    public static List<OtherMotherDialogue.Pool> pools(){var out=new ArrayList<OtherMotherDialogue.Pool>();for(var e:Event.values())for(var s:OtherMotherWelcome.State.values())out.add(pool(e,s));return out;}
    public static OtherMotherWelcome.State state(ServerPlayer p){var l=OtherMotherLoop.scene(p).loop;return OtherMotherLoopState.classify(l.trust,l.suspicion);}
    public static boolean busy(ServerPlayer p){var s=SecretTransactions.state(p);return s!=null&&(Reconciliation.active(p)||!s.dialogue.getString("Queue").isEmpty()||!completed(p)||s.dialogue.contains("Offer"));}
    public static boolean completed(ServerPlayer p){var n=SecretTransactions.state(p).dialogue;long now=OtherMotherLoop.now(p);return n.getLong("Until")<=now&&(n.getString("Playing").isEmpty()||n.getBoolean("Finished")||now>=n.getLong("Until")+20);}
    public static void confront(ServerPlayer p,Event event,OtherMotherWelcome.State state){if(Reconciliation.active(p))return;if(queue(p,event,state,-1)&&!Reconciliation.begin(p,Reconciliation.category(event),state,false)&&SecretsRuntime.active(p)){var l=OtherMotherLoop.scene(p).loop;if(l.suspicion>=1||l.secrets.search==SecretsState.Search.SCRIPTED_TRAP)OtherMotherLoop.clearActivityWindow(p);}}
    public static boolean queueScolding(ServerPlayer p,Reconciliation.Category category,OtherMotherWelcome.State state){var s=SecretTransactions.state(p);if(s==null||!s.dialogue.getString("Queue").isEmpty()||s.dialogue.contains("ContactPreludeFollowupPool"))return false;var pool=Reconciliation.pool(Reconciliation.Category.SERIOUS,state);s.dialogue.putString("Queue",pool.sound(0));s.dialogue.putString("Pool",pool.path());s.dialogue.putString("Event","SCOLD");s.dialogue.putString("State",state.id());SecretTransactions.dirty(p);return true;}
    public static void queue(ServerPlayer p,Event event,OtherMotherWelcome.State state){queue(p,event,state,-1);}
    public static boolean queue(ServerPlayer p,Event event,OtherMotherWelcome.State state,int variation){var s=SecretTransactions.state(p);if(s==null||!s.dialogue.getString("Queue").isEmpty())return false;var pool=MotherGoesOut.contactPool(p,event,state);if(pool==null)pool=WybieAdventure.contactPool(p,event,state);if(pool==null)pool=PartyReportDialogue.contactPool(p,event,state);if(pool!=null)s.dialogue.putString("QueuePool",pool.path());else {pool=pool(event,state);s.dialogue.remove("QueuePool");}var scene=OtherMotherLoop.scene(p);String selected=variation>=0?pool.sound(variation):OtherMotherDialogue.choose(pool,scene.lastDialogue.getOrDefault(pool.path(),""),p.getRandom());s.dialogue.putString("Queue",selected);s.dialogue.putString("Event",event.name());s.dialogue.putString("State",state.id());SecretTransactions.dirty(p);return true;}
    public static boolean secretArea(SecretsState s,net.minecraft.core.BlockPos at){for(var room:SecretWorld.rooms(s)){var c=net.minecraft.core.BlockPos.of(room.getLong("Pos"));if(Math.abs(c.getX()-at.getX())<=7&&Math.abs(c.getZ()-at.getZ())<=7&&at.getY()>=c.getY()-1&&at.getY()<c.getY()+10)return true;}return HouseCabinets.forbidden(s,at);}
    /** The old state-only overload remains conservative for callers without owner context. */
    public static boolean secretArea(ServerPlayer p,net.minecraft.core.BlockPos at){var s=SecretTransactions.state(p);if(s==null)return false;for(var room:SecretWorld.rooms(s)){var c=net.minecraft.core.BlockPos.of(room.getLong("Pos"));if(Math.abs(c.getX()-at.getX())<=7&&Math.abs(c.getZ()-at.getZ())<=7&&at.getY()>=c.getY()-1&&at.getY()<c.getY()+10)return true;}return HouseCabinets.forbidden(s,at)&&!PlayerParty.allowed(p,at.getCenter())&&!VisitEvents.showRoute(p,OtherMotherLoop.active(p),at);}
    private static OtherMotherDialogue.Pool queuedOverride(String path){var pool=MotherGoesOut.queuedPool(path);if(pool==null)pool=WybieAdventure.queuedPool(path);if(pool!=null)return pool;for(var value:HouseParty.pools())if(value.path().equals(path))return value;for(var value:PlayerParty.pools())if(value.path().equals(path))return value;return null;}
    /** Continue the same legitimate contact; never call confront or award another penalty. */
    private static void queueContactFollowup(ServerPlayer p,CompoundTag n){if(!n.getString("Queue").isEmpty()||!n.contains("ContactPreludeFollowupPool"))return;String path=n.getString("ContactPreludeFollowupPool"),eventName=n.getString("ContactPreludeFollowupEvent"),stateName=n.getString("ContactPreludeFollowupState");for(String key:List.of("ContactPreludeFollowupPool","ContactPreludeFollowupEvent","ContactPreludeFollowupState"))n.remove(key);try{var event=Event.valueOf(eventName);if(event!=Event.ORDINARY_AREA_CAUGHT&&event!=Event.SECRET_AREA_CAUGHT&&event!=Event.RESTRICTED_ITEM_CAUGHT)return;var state=OtherMotherWelcome.State.fromId(stateName);var pool=pool(event,state);if(!pool.path().equals(path))return;var scene=OtherMotherLoop.scene(p);String selected=OtherMotherDialogue.choose(pool,scene.lastDialogue.getOrDefault(path,""),p.getRandom());n.putString("Queue",selected);n.putString("Event",eventName);n.putString("State",stateName);n.remove("QueuePool");SecretTransactions.checkpoint(p);}catch(IllegalArgumentException ignored){SecretTransactions.dirty(p);}}
    public static Event caught(boolean foundItems,boolean secretArea){return secretArea?Event.SECRET_AREA_CAUGHT:foundItems?Event.RESTRICTED_ITEM_CAUGHT:Event.ORDINARY_AREA_CAUGHT;}
    static void useSeriousScolding(ServerPlayer p){
        var n=SecretTransactions.state(p).dialogue;
        if(!n.getString("Event").equals("SCOLD")||!(n.getString("Pool").startsWith("scolding/minor/")||n.getString("Queue").startsWith("other_mother_scolding_minor_")||n.getString("Playing").startsWith("other_mother_scolding_minor_")))return;
        if(n.getString("Playing").startsWith("other_mother_scolding_minor_"))VisitAudio.stopVoice(p,n.getString("Playing"));
        for(String key:new String[]{"Queue","Playing","Until","Finished"})n.remove(key);
        queueScolding(p,Reconciliation.Category.SERIOUS,OtherMotherWelcome.State.fromId(n.getString("State")));
    }
    private static void repairPartyReportQueue(ServerPlayer p,CompoundTag n){
        if(!n.getString("QueuePool").startsWith("events/player_party/report/"))return;
        var state=OtherMotherWelcome.State.fromId(n.getString("State"));
        var oldPool=PlayerParty.pool("report",state);var actualPool=PlayerParty.pool("guest_report",state);
        var fixed=PartyReportQueue.repair(n.getString("QueuePool"),n.getString("Queue"),oldPool.path(),actualPool.path());
        if(fixed!=null){n.putString("QueuePool",fixed.pool());n.putString("Queue",fixed.sound());SecretTransactions.checkpoint(p);}
    }
    public static void tick(ServerPlayer p){var s=SecretTransactions.state(p);if(s==null||!SecretsRuntime.active(p)||!p.isAlive())return;long now=OtherMotherLoop.now(p);var n=s.dialogue;
        repairPartyReportQueue(p,n);
        useSeriousScolding(p);
        Reconciliation.hold(p);if(!completed(p))return;
        queueContactFollowup(p,n);
        if(!n.getString("Queue").isEmpty()){
            Event event=null;OtherMotherDialogue.Pool pool;
            if(n.getString("Event").equals("SCOLD")){pool=Reconciliation.pools().stream().filter(v->v.path().equals(n.getString("Pool"))).findFirst().orElse(null);if(pool==null){n.remove("Queue");return;}}
            else {try{event=Event.valueOf(n.getString("Event"));}catch(IllegalArgumentException bad){n.remove("Queue");return;}pool=queuedOverride(n.getString("QueuePool"));if(pool==null)pool=pool(event,OtherMotherWelcome.State.fromId(n.getString("State")));}
            int ticks=OtherMotherDialogue.playSecretSelected(p,pool,n.getString("Queue"));if(ticks==0)return;
            n.putString("PlayingPool",pool.path());n.putString("Playing",n.getString("Queue"));n.putBoolean("Finished",false);n.remove("Queue");n.remove("QueuePool");n.putLong("Until",now+ticks+2);if(event==Event.BLACKMAIL_OFFER&&n.contains("Offer"))n.putBoolean("OfferSpoken",true);SecretTransactions.checkpoint(p);return;
        }
        if(n.getBoolean("OfferSpoken")&&n.contains("Offer")){SecretTransactions.resolveOffer(p);}
        if(n.getString("Queue").isEmpty()&&(!n.contains("Offer")||Reconciliation.active(p))){
            // Repair saves left at the old maximum-suspicion dead end.
            if(!Reconciliation.active(p)&&OtherMotherLoop.scene(p).loop.suspicion>=1&&s.world.getBoolean("CaughtEncounter")&&!n.getString("Playing").isEmpty())
                Reconciliation.begin(p,Reconciliation.Category.SERIOUS,OtherMotherWelcome.State.fromId(n.getString("State")),false);
            Reconciliation.advance(p);
        }
    }
    public static void finished(ServerPlayer p,String sound){var s=SecretTransactions.state(p);if(s!=null){
        var n=s.dialogue;boolean secret=sound.equals(n.getString("Playing")),loop=sound.equals(n.getString("LoopVoice"));
        if(secret)n.putBoolean("Finished",true);if(loop)n.putBoolean("LoopFinished",true);
        // Audio runs in real time, even when server ticks lag. Only accept an
        // early tick-clock completion after the full recording's real duration.
        if((secret||loop)&&sound.equals(n.getString("VoiceClockSound"))&&n.contains("VoiceClockEnd")&&System.currentTimeMillis()+100>=n.getLong("VoiceClockEnd")){
            long end=OtherMotherLoop.now(p)+2;var scene=OtherMotherLoop.scene(p);
            if(secret)n.putLong("Until",Math.min(n.getLong("Until"),end));
            if(loop)scene.loop.deadline=Math.min(scene.loop.deadline,end);
            scene.welcomeAfter=Math.min(scene.welcomeAfter,end);
        }
        SecretTransactions.dirty(p);
    }}
    public static void preview(ServerPlayer p,int event,int state,int variation){if(!p.hasPermissions(2)||!SecretsRuntime.active(p)||busy(p)||event<0||event>=6||state<0||state>=5||variation < -1||variation>1)return;queue(p,Event.values()[event],OtherMotherWelcome.State.values()[state],variation);}
    private SecretDialogue(){}
}
