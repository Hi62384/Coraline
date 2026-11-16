package com.coraline.feast;

import com.coraline.secrets.*;
import net.minecraft.server.level.ServerPlayer;

/** Thin component of the existing five-minute attendance and physical-contact search system. */
public final class ShowAttendance {
    public static void begin(ServerPlayer p,VisitData.Scene s){var n=s.loop.event;n.putInt("ArrivalRemaining",6000);n.putInt("GraceRemaining",0);n.putBoolean("GraceRolled",false);n.putBoolean("Searching",false);n.putBoolean("ShowAttended",false);OtherMotherLoop.changed(p);}
    public static boolean tick(ServerPlayer p,VisitData.Scene s,boolean destination,String event,String prefix){
        var n=s.loop.event;if(n.getBoolean("ShowAttended"))return true;if(destination&&!s.loop.secrets.searching()&&!Reconciliation.active(p)){arrive(p,s);return true;}
        if(p.isSleeping()||Reconciliation.active(p)||SecretDialogue.busy(p)||s.loop.secrets.searching())return false;
        int remaining=n.getInt("ArrivalRemaining");if(remaining>0){n.putInt("ArrivalRemaining",remaining-1);return false;}
        if(!n.getBoolean("GraceRolled")){n.putBoolean("GraceRolled",true);n.putInt("GraceRemaining",FamilyDinner.graceTicks(s.loop.suspicion,p.getRandom().nextDouble()));OtherMotherLoop.changed(p);return false;}
        int grace=n.getInt("GraceRemaining");if(grace>0){n.putInt("GraceRemaining",grace-1);return false;}
        if(!n.getBoolean("Searching")){n.putBoolean("Searching",true);s.loop.secrets.search=SecretsState.Search.SEARCHING;s.loop.secrets.target=p.blockPosition();
            // Search voice is only delivered in Mother's physical vicinity. No remote scolding.
            var mother=MotherSearch.mother(p);if(mother!=null&&mother.distanceToSqr(p)<1600&&mother.hasLineOfSight(p))NumberedEventDialogue.mother(p,event,prefix);
            OtherMotherLoop.changed(p);
        }return false;
    }
    public static void arrive(ServerPlayer p,VisitData.Scene s){var n=s.loop.event;boolean late=n.getInt("ArrivalRemaining")<=0;n.putBoolean("Late",late);n.putBoolean("ShowAttended",true);n.putInt("ArrivalRemaining",0);n.putInt("GraceRemaining",0);n.putBoolean("Searching",false);MotherSearch.attended(p);if(!n.getBoolean("ArrivalAwarded")){n.putBoolean("ArrivalAwarded",true);OtherMotherLoop.relationship(p,s.loop.trust+(late?.015F:.035F),s.loop.suspicion+(late?.01F:0));}OtherMotherLoop.changed(p);}
    public static void reconciled(VisitData.Scene s){s.loop.event.putBoolean("Searching",false);s.loop.event.putBoolean("GraceRolled",true);s.loop.event.putInt("GraceRemaining",1200);}
    private ShowAttendance(){}
}
