package com.coraline.feast;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

/** Normal playback uses real acknowledgements; bounded safety recovery can
 * skip a failed recording without granting rewards or offline progress. */
public final class ShowPlayback {
    private static final java.util.Map<UUID,Long> AUDIBLE_START=new java.util.HashMap<>();
    private static final java.util.Map<UUID,Long> REQUESTED_AT=new java.util.HashMap<>();
    public static CompoundTag data(VisitData.Scene s){if(!s.loop.event.contains("ShowPlayback"))s.loop.event.put("ShowPlayback",new CompoundTag());return s.loop.event.getCompound("ShowPlayback");}
    public static boolean start(ServerPlayer p,VisitData.Scene s,String sound,boolean music){
        if(!s.owner.equals(p.getUUID()))return false;
        var n=data(s);if(n.getString("Sound").equals(sound)&&!n.getBoolean("Finished")&&ownsSession(p,s)
                &&n.hasUUID("Nonce")&&REQUESTED_AT.containsKey(n.getUUID("Nonce")))return true;
        int duration=ShowRecordings.duration(sound);if(duration<=0){n.putString("Blocker","Original recording unavailable: "+sound);return false;}
        stop(p,s);n.putUUID("Nonce",UUID.randomUUID());n.putUUID("SessionOwner",s.owner);n.putUUID("SessionRealm",s.door);n.putInt("SessionSerial",s.loop.event.getInt("Serial"));n.putString("Sound",sound);n.putBoolean("Music",music);n.putInt("Duration",duration);n.putInt("Position",0);n.putInt("LoadingTicks",0);n.putInt("EndWait",0);n.putBoolean("Started",false);n.putBoolean("Ended",false);n.putBoolean("Finished",false);n.remove("Blocker");
        n.remove("StartAckTimedOut");n.remove("EndAckTimedOut");n.remove("SafetySkipped");n.remove("SafetyLastGameTick");n.putInt("SafetyTicks",0);REQUESTED_AT.put(n.getUUID("Nonce"),System.nanoTime());ShowPlaybackNetwork.bind(p,n.getUUID("Nonce"),sound,music,s.owner,s.door,s.loop.event.getInt("Serial"),duration);VisitAudio.play(p,sound,music);OtherMotherLoop.changed(p);return true;
    }
    public static void signal(ServerPlayer p,UUID nonce,String sound,int signal){
        var s=OtherMotherLoop.scene(p);if(s==null||!s.owner.equals(p.getUUID()))return;var n=data(s);
        if(!ownsSession(p,s)||debugSeeking(s)||!n.hasUUID("Nonce")||!nonce.equals(n.getUUID("Nonce"))||!sound.equals(n.getString("Sound"))||n.getBoolean("Finished"))return;
        if(signal==0){n.putBoolean("Started",true);AUDIBLE_START.putIfAbsent(nonce,System.nanoTime());if(n.getBoolean("StartAckTimedOut")){n.remove("StartAckTimedOut");n.remove("Blocker");}}
        // Completion acknowledgements never authorize early server progression.
        if(signal==1&&n.getBoolean("Started")){n.putBoolean("Ended",true);if(n.getBoolean("EndAckTimedOut")){n.remove("EndAckTimedOut");n.remove("Blocker");}}
        if(signal==2){n.remove("StartAckTimedOut");n.remove("EndAckTimedOut");n.putString("Blocker","Recording failed to play: "+sound);}
    }
    public static boolean tick(ServerPlayer p,VisitData.Scene s){
        if(!s.owner.equals(p.getUUID()))return false;
        var n=data(s);if(debugSeeking(s)){ShowAudioSeek.tick(p,s);return false;}if(n.getString("Sound").isEmpty()||n.getBoolean("Finished"))return n.getBoolean("Finished");
        // Saved NBT survives a server restart, but the acknowledged mixer run
        // and in-memory request clock do not. An orphaned started=true recording
        // previously ticked silently for Duration+600, unable to finish itself.
        // Report the interrupted session immediately so the owning chapter can
        // retry this same recording checkpoint with a fresh nonce.
        if(!n.hasUUID("Nonce")||!REQUESTED_AT.containsKey(n.getUUID("Nonce"))){
            n.putString("Blocker","The saved recording lost its playback session after a restart; retrying the same checkpoint.");
            OtherMotherLoop.changed(p);return false;
        }
        // Before Blocker: missing acknowledgements used to return forever.
        // Count calls only, never elapsed offline time or /time command jumps.
        long gameTick=p.server.overworld().getGameTime();
        if(!n.contains("SafetyLastGameTick")||n.getLong("SafetyLastGameTick")!=gameTick){n.putInt("SafetyTicks",Math.min(144000,n.getInt("SafetyTicks")+1));n.putLong("SafetyLastGameTick",gameTick);}
        int safetyBudget=n.getBoolean("Started")?Math.max(600,n.getInt("Duration")+600):600;
        if(n.getInt("SafetyTicks")>=safetyBudget){safetyFinish(p,s,"Recording exceeded its loading/completion safety grace");return true;}
        if(!n.getString("Blocker").isEmpty())return false;
        if(!n.getBoolean("Started")){var request=REQUESTED_AT.get(n.getUUID("Nonce"));int wait=request==null?n.getInt("LoadingTicks")+1:StoryPlaybackClock.elapsedTicks(request,System.nanoTime());n.putInt("LoadingTicks",wait);if(wait>=200){n.putBoolean("StartAckTimedOut",true);n.putString("Blocker","Waiting for recording playback; leave safely and retry after audio is available.");}return false;}
        int pos=Math.min(n.getInt("Duration"),n.getInt("Position")+1);
        // The mixer continues through slow server ticks. A genuine end ACK plus
        // the full elapsed recording duration may catch up a lagging cursor.
        Long audible=n.hasUUID("Nonce")?AUDIBLE_START.get(n.getUUID("Nonce")):null;long now=System.nanoTime();boolean elapsed=audible!=null&&StoryPlaybackClock.complete(audible,now,n.getInt("Duration"));
        if(audible!=null)pos=Math.min(pos,StoryPlaybackClock.elapsedTicks(audible,now));if(n.getBoolean("Ended")&&elapsed)pos=n.getInt("Duration");n.putInt("Position",pos);
        if(pos>=n.getInt("Duration")&&!n.getBoolean("Ended")){int wait=audible==null?0:StoryPlaybackClock.endWaitTicks(audible,now,n.getInt("Duration"));n.putInt("EndWait",wait);if(wait>=200){n.putBoolean("EndAckTimedOut",true);n.putString("Blocker","Recording-end acknowledgement is unavailable; safe recovery required.");}}
        if(pos>=n.getInt("Duration")&&n.getBoolean("Ended")&&elapsed){n.putBoolean("Finished",true);return true;}return false;
    }
    public static boolean ownsSession(ServerPlayer p,VisitData.Scene s){var n=data(s);return s.owner.equals(p.getUUID())&&n.hasUUID("SessionOwner")&&s.owner.equals(n.getUUID("SessionOwner"))&&n.hasUUID("SessionRealm")&&s.door.equals(n.getUUID("SessionRealm"))&&n.getInt("SessionSerial")==s.loop.event.getInt("Serial");}
    public static boolean debugSeeking(VisitData.Scene s){return data(s).getBoolean("DebugSeek");}
    public static int position(VisitData.Scene s){return data(s).getInt("Position");}
    /** Physical cues follow acknowledged audible time even during slow server
     * ticks. This does not finish a recording or authorize dialogue progression. */
    public static int audiblePosition(VisitData.Scene s){var n=data(s);Long start=n.getBoolean("Started")&&n.hasUUID("Nonce")?AUDIBLE_START.get(n.getUUID("Nonce")):null;return start==null?position(s):Math.min(n.getInt("Duration"),Math.max(position(s),StoryPlaybackClock.elapsedTicks(start,System.nanoTime())));}
    /** Elapsed acknowledged playback time, including time after a short song ends. */
    public static int audibleElapsedTicks(VisitData.Scene s){var n=data(s);Long start=n.getBoolean("Started")&&n.hasUUID("Nonce")?AUDIBLE_START.get(n.getUUID("Nonce")):null;return start==null?position(s):Math.max(position(s),StoryPlaybackClock.elapsedTicks(start,System.nanoTime()));}
    public static boolean started(VisitData.Scene s){return data(s).getBoolean("Started");}
    public static boolean finished(VisitData.Scene s){return data(s).getBoolean("Finished");}
    static void safetyFinish(ServerPlayer p,VisitData.Scene s,String reason){
        if(!ownsSession(p,s))return;var n=data(s);String sound=n.getString("Sound");if(!sound.isEmpty()){if(n.getBoolean("Music"))VisitAudio.stopMusic(p,sound);else VisitAudio.stopVoice(p,sound);}
        n.putInt("Position",n.getInt("Duration"));n.putBoolean("Finished",true);n.putBoolean("SafetySkipped",true);n.putString("SafetyReason",reason);n.remove("Blocker");
        if(n.hasUUID("Nonce")){AUDIBLE_START.remove(n.getUUID("Nonce"));REQUESTED_AT.remove(n.getUUID("Nonce"));}OtherMotherLoop.changed(p);
    }
    /** Keep an already playing PCM run alive only at an explicitly immediate edge. */
    public static void transition(ServerPlayer p,VisitData.Scene s,String next){
        var n=data(s);if(s.owner.equals(p.getUUID())&&n.getBoolean("Finished")&&!n.getBoolean("SafetySkipped")&&!next.isEmpty()&&StoryAudioContinuity.next(n.getString("Sound")).equals(next)&&ownsSession(p,s)&&!debugSeeking(s)){
            if(n.hasUUID("Nonce")){AUDIBLE_START.remove(n.getUUID("Nonce"));REQUESTED_AT.remove(n.getUUID("Nonce"));}n.remove("Sound");n.remove("Nonce");n.putBoolean("Started",false);
        }else stop(p,s);
    }
    public static void stop(ServerPlayer p,VisitData.Scene s){if(!s.owner.equals(p.getUUID()))return;ShowAudioSeek.stopping(p,s);var n=data(s);String sound=n.getString("Sound");if(!sound.isEmpty()){if(n.getBoolean("Music"))VisitAudio.stopMusic(p,sound);else VisitAudio.stopVoice(p,sound);}if(n.hasUUID("Nonce")){AUDIBLE_START.remove(n.getUUID("Nonce"));REQUESTED_AT.remove(n.getUUID("Nonce"));}n.remove("Sound");n.remove("Nonce");n.putBoolean("Started",false);}
    /** Recovery deliberately restarts only the current safe recording checkpoint, never skips it. */
    public static void recover(ServerPlayer p,VisitData.Scene s){if(!s.owner.equals(p.getUUID()))return;var n=data(s);String sound=n.getString("Sound");boolean music=n.getBoolean("Music"),done=n.getBoolean("Finished");stop(p,s);if(!sound.isEmpty()&&!done)start(p,s,sound,music);}
    private ShowPlayback(){}
}
