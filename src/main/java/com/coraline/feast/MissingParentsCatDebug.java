package com.coraline.feast;

import java.util.*;
import com.coraline.otherworld.*;
import com.coraline.parents.ParentAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;

/** Explicit operator previews use the live controller and real recordings, never fake audio ACKs. */
public final class MissingParentsCatDebug {
    public static Map<String,String> actions(){
        var m=new LinkedHashMap<String,String>();
        m.put("state","Inspect Cat, mirror, doll, playback and tunnel flags");
        m.put("complete_tea","Complete second tea visit / resolve Seeing Stone for testing");
        m.put("eligible","Check prerequisites and force the pending delay to zero");
        m.put("start","Spawn/recover the eligible canonical Cat");m.put("notice","Look at the Cat using actual gaze detection");
        m.put("recover","Replay the current recording checkpoint safely");m.put("mirror","Go to the actual mirror");
        m.put("parents","Preview the freezing parents / begin real B.wav");m.put("writing","Reveal HELP US; B.wav must still finish");
        m.put("enable_break","Enable mirror interaction for operator preview");m.put("break","Right-click the nearby mirror");m.put("shatter","Preview mirror shattering and real D.wav");
        m.put("bed","Go to the canonical parents' bed");m.put("cat_bed","Move the leased Cat beside that bed for testing");m.put("retrieve","Test under-bed retrieval with E.wav");
        for(char c='a';c<='f';c++)m.put("play_"+c,"Preview actual "+Character.toUpperCase(c)+".wav checkpoint");
        m.put("restore","Restore the permanent tunnel after actual F.wav");m.put("day","Set daytime and inspect permanent tunnel");m.put("night","Set nighttime and inspect permanent tunnel");
        m.put("tunnel_normal","Appearance-only normal tunnel preview");m.put("tunnel_decayed","Appearance-only decayed tunnel preview");m.put("preview_clear","Clear tunnel appearance preview");
        m.put("depart","Begin Cat departure after F.wav");m.put("complete","Complete only after F and an offscreen Cat departure");
        m.put("reset","Reset Cat preview, keeping the permanent tunnel and clue receipt");return m;
    }
    private static boolean bed(ServerPlayer p,VisitData.Scene s){
        var f=ParentAccess.family(p.server,s.owner);if(f==null||f.bed==null||!(p.level().getBlockState(f.bed).getBlock() instanceof BedBlock))return false;
        var n=MissingParentsCat.data(s);n.putLong("Bed",f.bed.asLong());n.putLong("BedFoot",MissingParentsCat.bedFoot(p,f.bed).asLong());n.putString("BedFront",MissingParentsCat.bedFront(p,f.bed).getName());return true;
    }
    private static boolean mirror(ServerPlayer p,VisitData.Scene s){var f=ParentAccess.family(p.server,s.owner);return f!=null&&f.bed!=null&&MissingParentsCat.mirror(p,s,f.bed);}
    private static void near(ServerPlayer p,BlockPos at){var safe=OtherWorld.safePlayer(p.serverLevel(),at);if(safe!=null)p.teleportTo(safe.getX()+.5,safe.getY(),safe.getZ()+.5);}
    private static String preview(ServerPlayer p,VisitData.Scene s,int type){
        var d=DoorData.get(p.server).doors.get(s.door);if(d==null||!s.owner.equals(d.owner))return "The owner's Little Door is unavailable.";
        var level=com.coraline.parents.FamilyData.level(p.server,d.dimension);if(level==null)return "Source world unavailable.";level.getChunkAt(d.pos);
        if(!(level.getBlockEntity(d.pos) instanceof DoorBlockEntity block)||!s.door.equals(block.doorId))return "Canonical Little Door unavailable.";
        block.tunnelPreview=type;level.sendBlockUpdated(d.pos,block.getBlockState(),block.getBlockState(),3);return "Appearance preview only; activation and progression are unchanged.";
    }
    public static String run(ServerPlayer p,VisitData.Scene s,String action){
        if(!NumberedDebugActions.operator(p,s))return "Owner/operator authorization required.";
        if(action.equals("state"))return "State refreshed.";
        if(ParentRescueStory.started(s))return "The later return is already active; Cat previews cannot overwrite it.";
        if(p.serverLevel()!=p.server.overworld())return "Return to the Overworld before testing this visit.";
        if(action.equals("complete_tea"))return MissingParentsTea.completeForCatDebug(p,s);
        var n=MissingParentsCat.data(s);
        if(action.equals("reset")){
            if(!ShowActors.releaseHidden(p,s,"cat"))return "Look away from the Cat (including nearby spectators) before resetting.";
            ShowPlayback.stop(p,s);
            for(String key:List.of("Complete","Started","Audio","BFinished","HelpUsComplete","ReadTicks","Writing","MirrorBroken","ShatterAge","DollsPull","DollsRetrieved","DepartAt","MoveTarget","MoveBest","StuckTicks","LostTicks"))n.remove(key);
            n.putBoolean("OperatorPreview",true);n.putInt("Delay",0);MissingParentsCat.go(p,s,MissingParentsCat.Phase.PENDING);return "Cat preview reset. Permanent tunnel, Seeing Stone and parent-doll clue receipts preserved.";
        }
        // Post-completion inspection must remain usable without replaying the story.
        if(action.equals("day")||action.equals("night")){p.serverLevel().setDayTime(action.equals("day")?1000:18000);return "World time changed for explicit operator testing; inspect the same Little Door.";}
        if(action.startsWith("tunnel_")||action.equals("preview_clear"))return preview(p,s,action.equals("tunnel_normal")?1:action.equals("tunnel_decayed")?2:0);
        if(action.equals("restore")){if(!n.getBoolean("FFinished"))return "Play and finish actual F.wav first; this control does not fabricate recording completion.";return StoryTunnel.restore(p,s)?"Permanent decayed tunnel restored.":"Saved entrance/corridor is not ready.";}
        if(!MissingParentsCat.eligible(s))return "Requires completed First Escape/Wybie, sealed story route and completed second tea visit with resolved Seeing Stone. Completed visits do not replay without Reset.";
        if(action.equals("eligible")||action.equals("start")){n.putInt("Delay",0);MissingParentsCat.tick(p,s);OtherMotherLoop.changed(p);return "Eligibility checked; pending Cat kept if no safe appearance point exists.";}
        if(action.equals("recover")){n.putBoolean("Recover",true);OtherMotherLoop.changed(p);return "Current checkpoint will restart; completed discoveries are kept.";}
        var cat=ShowActors.actor(p,s,"cat");if(!n.getBoolean("Started")||cat==null)return "Start the eligible Cat encounter first; an unloaded lease is not permission to clone it.";
        n.putBoolean("OperatorPreview",true);
        switch(action){
            case "notice"->p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,cat.getEyePosition());
            case "mirror"->{if(!mirror(p,s))return n.getString("Blocker");near(p,BlockPos.of(n.getLong("MirrorApproach")));}
            case "parents"->{if(!mirror(p,s))return n.getString("Blocker");n.putBoolean("MirrorBroken",false);n.putInt("Writing",0);n.putBoolean("BFinished",false);n.putInt("ReadTicks",0);MissingParentsCat.go(p,s,MissingParentsCat.Phase.B);}
            case "writing"->{if(MissingParentsCat.phase(s)!=MissingParentsCat.Phase.B)return "Start the parents/B.wav preview first.";n.putInt("Writing",MissingParentsCatTimeline.WRITE_TICKS);}
            case "enable_break"->{if(!mirror(p,s))return n.getString("Blocker");n.putInt("Writing",MissingParentsCatTimeline.WRITE_TICKS);n.putBoolean("HelpUsComplete",true);MissingParentsCat.go(p,s,MissingParentsCat.Phase.WAIT_BREAK);}
            case "break"->{if(!n.contains("Mirror")||!MissingParentsCat.block(p,BlockPos.of(n.getLong("Mirror"))))return "Approach the mirror at its right-click prompt.";}
            case "shatter"->{if(!mirror(p,s))return n.getString("Blocker");MissingParentsCat.shatter(p,s);MissingParentsCat.go(p,s,MissingParentsCat.Phase.D);}
            case "bed","cat_bed","retrieve"->{
                if(!bed(p,s))return "The canonical parents' bed is missing.";var f=ParentAccess.family(p.server,s.owner);var target=BlockPos.of(n.getLong("BedFoot")).relative(MissingParentsCat.bedFront(p,f.bed),2);
                if(action.equals("bed"))near(p,target);
                else{cat.setPos(Vec3.atBottomCenterOf(target));n.putLong("CatAt",cat.blockPosition().asLong());if(action.equals("retrieve")){n.putBoolean("DollsRetrieved",false);n.putFloat("DollsPull",0);MissingParentsCat.go(p,s,MissingParentsCat.Phase.E);}}
            }
            case "depart","complete"->{
                if(!n.getBoolean("FFinished")||!MissingParentsCat.tunnelRestored(s))return "Finish F.wav and restore the permanent tunnel first.";
                if(action.equals("complete")){if(!ShowActors.releaseHidden(p,s,"cat"))return "Let the Cat leave the view of all nearby players.";n.putBoolean("Complete",true);MissingParentsCat.go(p,s,MissingParentsCat.Phase.COMPLETE);}
                else MissingParentsCat.go(p,s,MissingParentsCat.Phase.DEPART);
            }
            default->{
                if(!action.matches("play_[a-f]"))return "Unknown Cat control.";
                var phase=MissingParentsCat.Phase.valueOf(action.substring(5).toUpperCase(Locale.ROOT));
                if(phase.ordinal()>=MissingParentsCat.Phase.B.ordinal()&&phase.ordinal()<=MissingParentsCat.Phase.D.ordinal()&&!mirror(p,s))return n.getString("Blocker");
                if((phase==MissingParentsCat.Phase.E||phase==MissingParentsCat.Phase.F)&&!bed(p,s))return "The canonical parents' bed is missing.";
                if(phase==MissingParentsCat.Phase.B){n.putBoolean("MirrorBroken",false);n.putInt("Writing",0);n.putBoolean("BFinished",false);n.putInt("ReadTicks",0);}
                if(phase==MissingParentsCat.Phase.D)MissingParentsCat.shatter(p,s);
                MissingParentsCat.go(p,s,phase);
            }
        }
        MissingParentsCat.props(p,s);OtherMotherLoop.changed(p);p.server.overworld().getDataStorage().save();return "Owner-scoped Cat preview applied; actual recordings and item receipts remain authoritative.";
    }
    public static List<String> inspect(VisitData.Scene s){var n=MissingParentsCat.data(s);var audio=ShowPlayback.data(s);return List.of(
        "Event "+MissingParentsCat.EVENT+" | phase "+MissingParentsCat.phase(s),
        "Eligible "+MissingParentsCat.eligible(s)+" | delay "+n.getInt("Delay")+" | started "+n.getBoolean("Started")+" | complete "+n.getBoolean("Complete"),
        "Cat "+(n.contains("CatAt")?BlockPos.of(n.getLong("CatAt")):"pending")+" | stuck "+n.getInt("StuckTicks")+" | lost "+n.getInt("LostTicks"),
        "Audio "+audio.getString("Sound")+" | "+ShowPlayback.position(s)+" / "+audio.getInt("Duration")+" | acknowledged "+audio.getBoolean("Started")+" | ended "+audio.getBoolean("Ended"),
        "Mirror "+(n.contains("Mirror")?BlockPos.of(n.getLong("Mirror")):"pending")+" | HELP US "+n.getInt("Writing")+" / "+MissingParentsCatTimeline.WRITE_TICKS+" | broken "+n.getBoolean("MirrorBroken"),
        "Bed "+(n.contains("Bed")?BlockPos.of(n.getLong("Bed")):"pending")+" | dolls retrieved "+n.getBoolean("DollsRetrieved")+" | clue receipt "+s.loop.story.getBoolean("ParentDollsDiscovered"),
        "F finished "+n.getBoolean("FFinished")+" | permanent tunnel "+MissingParentsCat.tunnelRestored(s)+" | operator preview "+n.getBoolean("OperatorPreview"),
        "Blocker "+n.getString("Blocker"));}
    private MissingParentsCatDebug(){}
}
