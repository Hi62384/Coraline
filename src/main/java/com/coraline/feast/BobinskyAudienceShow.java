package com.coraline.feast;

import com.coraline.secrets.HouseCabinets;
import com.coraline.otherworld.RealmData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/** Compact party performance reuses the exact mouse music and actual Bobinsky, with no wager. */
public final class BobinskyAudienceShow {
    private static BlockPos stage(ServerPlayer p,VisitData.Scene s){var party=s.loop.event.getCompound("Party");if(party.contains("CircusStage"))return BlockPos.of(party.getLong("CircusStage"));var realm=RealmData.get(p.server).realms.get(s.door);return realm==null||realm.house==null?null:HouseCabinets.at(realm.house,32,24,40);}
    public static boolean start(ServerPlayer p,VisitData.Scene s,Mob bobinsky){if(!MouseCircusSite.inAttic(p,s)||!(bobinsky instanceof com.coraline.entity.CharacterEntity c)||!c.characterId().equals("other_bobinsky")||stage(p,s)==null)return false;var at=stage(p,s);for(var check:BlockPos.betweenClosed(at.offset(-4,0,-4),at.offset(4,5,4)))if(!p.serverLevel().getBlockState(check).isAir())return false;if(!PartyShowPlayback.start(p,s,"PartyMouseShow","mouse_circus_music",bobinsky))return false;MouseCircusChoreography.revealScaled(p,s,at,.22);return true;}
    public static void tick(ServerPlayer p,VisitData.Scene s){if(!playing(s))return;var n=PartyShowPlayback.data(s,"PartyMouseShow");PartyShowPlayback.tick(p,s,n);if(!MouseCircusSite.inAttic(p,s)){if(!playing(s)&&stage(p,s)!=null)MouseCircusChoreography.cleanup(p,s,stage(p,s));return;}var c=stage(p,s);if(c==null||n.getBoolean("NextTrack"))return;MouseCircusChoreography.frameScaled(p,s,c,n.getInt("Position"),n.getInt("Duration"),.22);for(var actor:PartyShowPlayback.actors(p,n))ShowPlaybackNetwork.pose(actor,playing(s)?1:7,n.getInt("Position"),1);if(!playing(s))MouseCircusChoreography.cleanup(p,s,c);}
    public static boolean playing(VisitData.Scene s){return PartyShowPlayback.playing(s,"PartyMouseShow");}
    public static void requestFinish(VisitData.Scene s){PartyShowPlayback.data(s,"PartyMouseShow").putBoolean("FinishRequested",true);}
    public static int remaining(VisitData.Scene s){return PartyShowPlayback.remaining(s,"PartyMouseShow");}
    public static String blocker(VisitData.Scene s){return PartyShowPlayback.data(s,"PartyMouseShow").getString("Blocker");}
    public static void suspend(ServerPlayer p,VisitData.Scene s){PartyShowPlayback.suspend(p,s,"PartyMouseShow");}
    public static boolean cleanup(ServerPlayer p,VisitData.Scene s){boolean restored=PartyShowPlayback.cleanup(p,s,"PartyMouseShow");var c=stage(p,s);if(restored&&c!=null)MouseCircusChoreography.cleanup(p,s,c);return restored;}
    private BobinskyAudienceShow(){}
}
