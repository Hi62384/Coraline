package com.coraline.feast;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/** Party-only cast leases. Stops accepting new sequences but never truncates an accepted recording. */
final class PartyShowPlayback {
    static CompoundTag data(VisitData.Scene s,String key){if(!s.loop.event.contains(key))s.loop.event.put(key,new CompoundTag());return s.loop.event.getCompound(key);}
    static boolean start(ServerPlayer p,VisitData.Scene s,String key,String sound,Mob... actors){
        if(!s.owner.equals(p.getUUID())||playing(s,"PartyTheatreShow")||playing(s,"PartyMouseShow"))return false;var n=data(s,key);int duration=ShowRecordings.duration(sound);if(duration<=0){n.putString("Blocker","Original recording unavailable: "+ShowRecordings.SOURCES.get(sound));return false;}
        // Party attractions use two complete musical sections and one short intermission.
        // The once-only Theatre/MouseCircus directors do not use this party adapter.
        int showDuration=duration*2+80;if(showDuration>PlayerPartyLedger.MAX_SHOW_DRAIN){n.putString("Blocker","Party performance exceeds the safe full-recording drain bound");return false;}
        if(n.contains("Actors")&&!restore(p,s,key,n)){n.putString("Blocker","A previous party performer has not safely restored");return false;}
        for(var actor:actors)if(actor==null||actor.level()!=p.level()||!actor.getPersistentData().hasUUID("PartyOwner")||!s.owner.equals(actor.getPersistentData().getUUID("PartyOwner")))return false;
        for(var actor:actors)if(actor.getPersistentData().contains(PartyShowRestoration.KEY)){n.putString("Blocker","A performer still owns another show restoration marker");return false;}
        var list=new ListTag();for(var actor:actors){if(!PartyShowRestoration.capture(p,s,key,actor))return false;var a=new CompoundTag();a.putUUID("Entity",actor.getUUID());a.putBoolean("NoAI",actor.isNoAi());a.putBoolean("Gravity",actor.isNoGravity());a.putBoolean("Physics",actor.noPhysics);list.add(a);actor.stopRiding();actor.setNoAi(true);actor.setNoGravity(true);actor.noPhysics=true;}
        n.put("Actors",list);n.putString("Sound",sound);n.putInt("MediaDuration",duration);n.putInt("Duration",showDuration);n.putInt("Cycles",2);n.putInt("Cycle",0);n.putInt("Intermission",0);n.putBoolean("NextTrack",false);n.putInt("Position",0);n.putInt("SpeechStage",0);n.putBoolean("Playing",true);n.putBoolean("FinishRequested",false);n.remove("Blocker");if(!ShowPlayback.start(p,s,sound,true)){restore(p,s,key,n);n.putBoolean("Playing",false);return false;}OtherMotherLoop.changed(p);return true;
    }
    static List<Mob> actors(ServerPlayer p,CompoundTag n){var result=new ArrayList<Mob>();for(var t:n.getList("Actors",Tag.TAG_COMPOUND)){var a=(CompoundTag)t;for(var l:p.server.getAllLevels())if(l.getEntity(a.getUUID("Entity")) instanceof Mob m)result.add(m);}return result;}
    static boolean playing(VisitData.Scene s,String key){return data(s,key).getBoolean("Playing");}
    static int remaining(VisitData.Scene s,String key){var n=data(s,key);return Math.max(0,n.getInt("Duration")-n.getInt("Position"));}
    static void tick(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        if(!s.owner.equals(p.getUUID())||!n.getBoolean("Playing"))return;String key=n.getString("Sound").equals("theatre_music")?"PartyTheatreShow":"PartyMouseShow";
        if(n.getBoolean("Resume")){
            if(!restore(p,s,key,n))return;
            var present=actors(p,n);if(present.size()!=n.getList("Actors",Tag.TAG_COMPOUND).size()){n.putString("Blocker","A resumed party performer is not loaded");return;}
            for(var saved:n.getList("Actors",Tag.TAG_COMPOUND))((CompoundTag)saved).putBoolean("Restored",false);
            for(var actor:present){if(!PartyShowRestoration.capture(p,s,key,actor))return;actor.setNoAi(true);actor.setNoGravity(true);actor.noPhysics=true;}
            if(!n.getBoolean("NextTrack")&&!ShowPlayback.start(p,s,n.getString("Sound"),true)){restore(p,s,key,n);return;}n.putBoolean("Resume",false);
        }
        if(n.getBoolean("NextTrack")){
            int pause=n.getInt("Intermission");if(pause>0){n.putInt("Intermission",pause-1);n.putInt("Position",n.getInt("Position")+1);return;}
            if(!ShowPlayback.start(p,s,n.getString("Sound"),true))return;n.putBoolean("NextTrack",false);
        }
        int mediaDuration=n.contains("MediaDuration")?n.getInt("MediaDuration"):n.getInt("Duration");
        boolean end=ShowPlayback.tick(p,s);n.putInt("Position",n.getInt("Cycle")*(mediaDuration+80)+ShowPlayback.position(s));String issue=ShowPlayback.data(s).getString("Blocker");if(!issue.isEmpty())n.putString("Blocker",issue);
        if(ShowPlayback.started(s)&&issue.isEmpty())performanceLine(p,s,n);
        if(end&&n.getInt("Cycle")+1<Math.max(1,n.getInt("Cycles"))){
            // Stop only after the acknowledged full recording end; never overlap or truncate.
            ShowPlayback.stop(p,s);n.putInt("Cycle",n.getInt("Cycle")+1);n.putInt("Intermission",80);n.putBoolean("NextTrack",true);OtherMotherLoop.changed(p);return;
        }
        if(end){n.putBoolean("Playing",false);n.putBoolean("Complete",true);restore(p,s,key,n);OtherMotherLoop.changed(p);}
    }
    private static void performanceLine(ServerPlayer p,VisitData.Scene s,CompoundTag show){
        if(!PlayerParty.isPlayerParty(s)||PartySurveillance.warningSpeechActive(s)||s.loop.secrets.searching()||com.coraline.secrets.SecretDialogue.busy(p)||com.coraline.secrets.Reconciliation.active(p))return;
        var party=HouseParty.data(s);long now=party.getLong("Clock");if(now<party.getLong("HostVoiceUntil")||now<party.getLong("VoiceUntil")||party.getCompound("Conversation").hasUUID("Guest"))return;
        boolean theatre=show.getString("Sound").equals("theatre_music");
        if(theatre?!TheatreSite.inside(p,s):!MouseCircusSite.inAttic(p,s))return;
        String[] lines=theatre?new String[]{"spink_theatre_start","forcible_theatre_start","spink_theatre_act","forcible_theatre_finish"}:new String[]{"bobinsky_show_start","bobinsky_show_middle","bobinsky_show_finish"};
        double[] thresholds=theatre?new double[]{0,.16,.55,.9}:new double[]{0,.4,.9};
        int stage=show.getInt("SpeechStage");if(stage>=lines.length||show.getInt("Position")<show.getInt("Duration")*thresholds[stage])return;
        int ticks=NumberedEventDialogue.say(p,"player_party",lines[stage]);if(ticks<=0)return;
        show.putInt("SpeechStage",stage+1);party.putLong("HostVoiceUntil",now+ticks);party.putLong("VoiceUntil",now+ticks);OtherMotherLoop.changed(p);
    }
    static void suspend(ServerPlayer p,VisitData.Scene s,String key){if(!s.owner.equals(p.getUUID()))return;var n=data(s,key);if(!n.getBoolean("Playing"))return;ShowPlayback.stop(p,s);n.putBoolean("Resume",true);restore(p,s,key,n);}
    static boolean cleanup(ServerPlayer p,VisitData.Scene s,String key){if(!s.owner.equals(p.getUUID()))return false;var n=data(s,key);if(n.getBoolean("Playing"))ShowPlayback.stop(p,s);n.putBoolean("Playing",false);n.putBoolean("Resume",false);return restore(p,s,key,n);}
    private static boolean restore(ServerPlayer p,VisitData.Scene s,String key,CompoundTag n){
        boolean complete=true;
        for(var t:n.getList("Actors",Tag.TAG_COMPOUND)){var a=(CompoundTag)t;if(a.getBoolean("Restored"))continue;Mob actor=null;for(var level:p.server.getAllLevels())if(level.getEntity(a.getUUID("Entity")) instanceof Mob found){actor=found;break;}
            if(actor==null||!PartyShowRestoration.restore(p,s,key,actor,a))complete=false;else a.putBoolean("Restored",true);
        }
        n.putBoolean("RestorePending",!complete);if(!complete)n.putString("Blocker","A party performer is unloaded or awaiting owner-safe restoration");OtherMotherLoop.changed(p);return complete;
    }
    private PartyShowPlayback(){}
}
