package com.coraline.feast;
import com.coraline.secrets.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
/** A physical guest report changes the later contact line, never delivers remote scolding or penalties. */
public final class PartyReportDialogue {
    public static void received(VisitData.Scene s,UUID incident,String offense){var n=HouseParty.data(s);n.putUUID("MotherReportIncident",incident);n.putString("MotherReportOffense",offense);}
    /** Queue only at the already-validated contact. Play stored follow-up before reconciliation scolding, without another encounter/penalty. */
    public static OtherMotherDialogue.Pool contactPool(ServerPlayer p,SecretDialogue.Event event,OtherMotherWelcome.State state){
        var s=HouseParty.active(p);if(s==null||!switch(event){case ORDINARY_AREA_CAUGHT,SECRET_AREA_CAUGHT,RESTRICTED_ITEM_CAUGHT->true;default->false;})return null;
        var n=HouseParty.data(s);if(!n.hasUUID("MotherReportIncident")||n.hasUUID("MotherReportSpoken")&&n.getUUID("MotherReportIncident").equals(n.getUUID("MotherReportSpoken")))return null;
        var mother=MotherSearch.mother(p);if(mother==null||mother.level()!=p.level()||!MotherSearch.touching(mother,p)||!mother.hasLineOfSight(p))return null;
        var dialogue=s.loop.secrets.dialogue;dialogue.putString("ContactPreludeFollowupPool",SecretDialogue.pool(event,state).path());dialogue.putString("ContactPreludeFollowupEvent",event.name());dialogue.putString("ContactPreludeFollowupState",state.id());
        n.putUUID("MotherReportSpoken",n.getUUID("MotherReportIncident"));OtherMotherLoop.changed(p);
        return PlayerParty.isPlayerParty(s)?PlayerParty.pool("guest_report",state):HouseParty.pool("guest_report",state);
    }
    private PartyReportDialogue(){}
}
