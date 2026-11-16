package com.coraline.feast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/** A reusable party performance of the original opening song, without special-event flags or rewards. */
public final class TheatreAudienceShow {
    public static boolean start(ServerPlayer p,VisitData.Scene s,Mob spink,Mob forcible){if(!TheatreSite.inside(p,s)||!named(spink,"other_miss_spink")||!named(forcible,"other_miss_forcible"))return false;return PartyShowPlayback.start(p,s,"PartyTheatreShow","theatre_music",spink,forcible);}
    private static boolean named(Mob actor,String id){return actor instanceof com.coraline.entity.CharacterEntity c&&c.characterId().equals(id);}
    public static void tick(ServerPlayer p,VisitData.Scene s){if(!playing(s))return;var n=PartyShowPlayback.data(s,"PartyTheatreShow");PartyShowPlayback.tick(p,s,n);if(!TheatreSite.inside(p,s))return;if(n.getBoolean("NextTrack")){TheatreVisit.curtain(p,s,0);ShowStageLighting.theatre(p,s,-1,0);return;}int at=n.getInt("Position"),duration=n.getInt("Duration");var actors=PartyShowPlayback.actors(p,n);Mob spink=null,forcible=null;for(var actor:actors){if(named(actor,"other_miss_spink"))spink=actor;else if(named(actor,"other_miss_forcible"))forcible=actor;}TheatreOpeningChoreography.frame(p,s,spink,forcible,at,duration);TheatreVisit.curtain(p,s,playing(s)?Math.min(1,ShowPlayback.position(s)/30F):0);}
    public static boolean playing(VisitData.Scene s){return PartyShowPlayback.playing(s,"PartyTheatreShow");}
    public static void requestFinish(VisitData.Scene s){PartyShowPlayback.data(s,"PartyTheatreShow").putBoolean("FinishRequested",true);}
    public static int remaining(VisitData.Scene s){return PartyShowPlayback.remaining(s,"PartyTheatreShow");}
    public static String blocker(VisitData.Scene s){return PartyShowPlayback.data(s,"PartyTheatreShow").getString("Blocker");}
    public static void suspend(ServerPlayer p,VisitData.Scene s){PartyShowPlayback.suspend(p,s,"PartyTheatreShow");}
    public static boolean cleanup(ServerPlayer p,VisitData.Scene s){boolean restored=PartyShowPlayback.cleanup(p,s,"PartyTheatreShow");return restored;}
    private TheatreAudienceShow(){}
}
