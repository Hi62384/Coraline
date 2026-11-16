import com.coraline.palace.*;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import static com.coraline.palace.PalaceWellProgress.Phase.*;
import static com.coraline.palace.PalaceWellProgress.WybieLife.*;

/** Saved state rules, not physical gameplay or inventory-custody acceptance. */
public final class PalaceWellSmoke {
    private static int checks;
    private static final UUID OWNER=UUID.randomUUID(), DOOR=UUID.randomUUID(), HAND=UUID.randomUUID(), WYBIE=UUID.randomUUID(), OTHER=UUID.randomUUID();
    private static final PalaceWellProgress.Prerequisites READY=new PalaceWellProgress.Prerequisites(true,true,true,true,true,true,true);
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static PalaceWellProgress start(boolean previouslyDead){return PalaceWellProgress.start(OWNER,DOOR,HAND,previouslyDead?null:WYBIE,previouslyDead,READY).orElseThrow();}
    private static PalaceWellProgress reload(PalaceWellProgress state){var saved=state.save();var loaded=PalaceWellProgress.load(OWNER,DOOR,saved);check(loaded.blocker().isBlank(),"Valid well save blocked: "+loaded.blocker());check(loaded.save().equals(saved),"Reload changed the encounter checkpoint");return loaded;}
    private static PalaceWellProgress rescue(double length){
        var state=start(false);var token=state.session();
        for(int i=0;i<100;i++)check(state.activeTick(OWNER,token,i),"Drag tick rejected");
        check(state.phase()==INTRO_A&&state.recording().orElseThrow()=='A',"A started before/after the five-second drag");
        for(char cue='A';cue<='D';cue++){
            check(!state.finishIntro(OWNER,token,cue,false,true,length),"Unheard intro skipped");
            check(!state.finishIntro(OWNER,token,cue,true,false,length),"Unfinished physical action skipped");
            check(!state.finishIntro(OTHER,token,cue,true,true,length),"Foreign player advanced encounter");
            check(state.finishIntro(OWNER,token,cue,true,true,length),"Valid intro gate failed");
        }
        check(state.phase()==RESCUE&&state.recording().isEmpty(),"D did not enter the actual rescue window");
        check(state.rescueTicks()==PalaceJourneyTiming.rescueWindow(length),"Reachable route window not retained");
        return state;
    }
    private static void finishFight(PalaceWellProgress state,boolean alive){
        var token=state.session();check(state.phase()==BOSS,"Fight not entered");
        check(state.wybieCanAssist()==alive,"Dead/missing Wybie can assist fight");
        check(!state.defeated(OWNER,token,OTHER),"Different hand completed fight");
        check(state.defeated(OWNER,token,HAND),"Canonical hand defeat rejected");
        check(!state.shattered(OWNER,token,HAND,false),"Hand disappeared without physical rock smash");
        check(state.shattered(OWNER,token,HAND,true),"Physical rock smash not recorded");
        check(!state.wrapped(OWNER,token,false),"Missing cloth/rock/fragments accepted");
        check(state.wrapped(OWNER,token,true),"Physical bundle rejected");
        var receipt=UUID.randomUUID();
        check(!state.keyBundled(OWNER,token,OTHER,DOOR,receipt),"Another owner's key accepted");
        check(!state.keyBundled(OWNER,token,OWNER,OTHER,receipt),"Different Little Door key accepted");
        check(!state.keyBundled(OWNER,token,OWNER,DOOR,null),"Missing real custody receipt accepted");
        check(state.keyBundled(OWNER,token,OWNER,DOOR,receipt),"Original key receipt rejected");
        check(!state.keyBundled(OWNER,token,OWNER,DOOR,UUID.randomUUID()),"Second key custody transaction accepted");
        check((state.recording().orElse(null)==(alive?Character.valueOf('E'):null)),"Dead branch played E");
        state=reload(state);token=state.session();
        check(!state.disposed(OWNER,token,UUID.randomUUID(),true,true),"Wrong bundle disposed");
        check(!state.disposed(OWNER,token,receipt,false,true),"Bundle not physically down shaft accepted");
        if(alive)check(!state.disposed(OWNER,token,receipt,true,false),"Surviving E recording truncated");
        check(state.disposed(OWNER,token,receipt,true,alive),"Irreversible disposal rejected");
        check(!state.retryBoss(OWNER,token)&&!state.keyBundled(OWNER,token,OWNER,DOOR,receipt),"Disposed key/fight resurrected by retry");
        check(state.beginFarewell(OWNER,token),"Ending branch failed");
        if(alive){
            for(char cue='F';cue<='I';cue++){
                check(state.recording().orElseThrow()==cue,"Farewell recording skipped/reordered");
                check(!state.finishFarewell(OWNER,token,cue,false,true),"Farewell truncated");
                check(!state.finishFarewell(OWNER,token,cue,true,false),"Photo/physical motion skipped");
                check(state.finishFarewell(OWNER,token,cue,true,true),"Real farewell gate failed");
                state=reload(state);token=state.session();
            }
            check(state.phase()==DEPARTING,"Completed before natural bicycle departure");
            check(!state.departed(OWNER,token,OTHER,true)&&!state.departed(OWNER,token,WYBIE,false),"Replacement/visible Wybie counted as departed");
            check(state.departed(OWNER,token,WYBIE,true),"Original safe departure rejected");
        }
        state=reload(state);
        check(state.phase()==COMPLETE&&state.recording().isEmpty(),"Ending not terminal");
        check(state.wybieLife()==(alive?ALIVE:state.originalWybie().isPresent()?DEAD_AT_WELL:DEAD_BEFORE_WELL),"Completion changed canonical life state");
        check(!state.resume(OWNER)&&!state.retryBoss(OWNER,state.session())&&!state.activeTick(OWNER,state.session(),999999),"Terminal state restarted");
    }
    private static void blocked(CompoundTag root,String reason){
        root.putString("UnknownEvidence","retain original");var state=PalaceWellProgress.load(OWNER,DOOR,root);
        check(!state.blocker().isBlank(),reason);check(!state.resume(OWNER)&&!state.activeTick(OWNER,state.session(),999999)&&state.recording().isEmpty(),"Corrupt state still executes");
        var retained=state.save();retained.remove("Blocker");check(retained.equals(root),"Quarantine destroyed original evidence");
        check(!PalaceWellProgress.load(OWNER,DOOR,state.save()).blocker().isBlank(),"Reload escaped quarantine");
    }
    public static int run(){
        for(int missing=0;missing<7;missing++){
            var ready=new PalaceWellProgress.Prerequisites(missing!=0,missing!=1,missing!=2,missing!=3,missing!=4,missing!=5,missing!=6);
            check(PalaceWellProgress.start(OWNER,DOOR,HAND,WYBIE,false,ready).isEmpty(),"Well started before epilogue prerequisite "+missing);
        }
        check(PalaceWellProgress.start(OWNER,DOOR,null,WYBIE,false,READY).isEmpty(),"Replacement hand permitted");
        check(PalaceWellProgress.start(OWNER,DOOR,HAND,null,false,READY).isEmpty(),"Missing original interpreted as death/respawn permission");
        var drag=start(false);var token=drag.session();
        for(int i=0;i<99;i++){
            check(!drag.activeTick(OTHER,token,i)&&!drag.activeTick(OWNER,OTHER,i),"Foreign/stale tick accepted");
            check(drag.activeTick(OWNER,token,i)&&!drag.activeTick(OWNER,token,i),"Duplicate tick advanced clock");
            drag=reload(drag);check(drag.phase()==DRAG&&drag.recording().isEmpty(),"Drag abbreviated after reload");
        }
        check(drag.activeTick(OWNER,token,1000000)&&drag.phase()==INTRO_A,"Offline time incorrectly counted as active ticks");
        check(!drag.finishIntro(OWNER,token,'B',true,true,8),"Out-of-order recording advanced");
        for(char cue='A';cue<='C';cue++)check(drag.finishIntro(OWNER,token,cue,true,true,8),"Intro prerequisite failed");
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,33})check(!drag.finishIntro(OWNER,token,'D',true,true,invalid)&&drag.phase()==INTRO_D,"Unreachable rescue route accepted");
        for(int length=0;length<=32;length++){
            // Success on the last legal active tick, with reload every step.
            var state=rescue(length);token=state.session();int window=state.rescueTicks();
            for(int i=1;i<window-1;i++){check(state.activeTick(OWNER,token,100+i),"Rescue tick failed");state=reload(state);}
            long hitTick=100+window-1;
            check(!state.rescueHit(OTHER,token,HAND,hitTick,true,true,true),"Foreign melee rescued Wybie");
            check(!state.rescueHit(OWNER,OTHER,HAND,hitTick,true,true,true),"Stale melee rescued Wybie");
            check(!state.rescueHit(OWNER,token,OTHER,hitTick,true,true,true),"Wrong target rescued Wybie");
            check(!state.rescueHit(OWNER,token,HAND,hitTick,false,true,true),"Ranged rescue accepted");
            check(!state.rescueHit(OWNER,token,HAND,hitTick,true,false,true),"Unreachable melee accepted");
            check(!state.rescueHit(OWNER,token,HAND,hitTick,true,true,false),"Attack through wall accepted");
            check(state.rescueHit(OWNER,token,HAND,hitTick,true,true,true)&&state.phase()==RESCUED,"Last legal rescue hit lost");
            check(!state.wybieCanAssist()&&!state.defeated(OWNER,token,HAND),"Fight started before physical climb-out");
            check(!state.climbedOut(OWNER,token,OTHER,true)&&!state.climbedOut(OWNER,token,WYBIE,false),"Fake climb-out accepted");
            check(state.climbedOut(OWNER,token,WYBIE,true),"Original climb-out rejected");finishFight(state,true);
            // Deadline failure must be identical for either callback ordering.
            for(boolean attackFirst:new boolean[]{false,true}){
                state=rescue(length);token=state.session();
                for(int i=1;i<window;i++)check(state.activeTick(OWNER,token,100+i),"Pre-deadline clock failed");
                if(attackFirst)check(!state.rescueHit(OWNER,token,HAND,100+window,true,true,true),"Pre-tick deadline hit won");
                check(state.activeTick(OWNER,token,100+window)&&state.phase()==BOSS&&state.wybieLife()==DEAD_AT_WELL,"Deadline did not permanently record well death");
                check(!state.rescueHit(OWNER,token,HAND,100+window,true,true,true),"Post-tick deadline hit resurrected Wybie");
                state=reload(state);
                for(int retry=0;retry<10;retry++){
                    token=state.session();check(state.retryBoss(OWNER,token),"Boss retry failed");check(!state.defeated(OWNER,token,HAND),"Stale attempt accepted after retry");
                    state=reload(state);check(state.wybieLife()==DEAD_AT_WELL&&!state.wybieCanAssist()&&state.recording().isEmpty(),"Retry resurrected Wybie or replayed A-D");
                }
                finishFight(state,false);
            }
        }
        var solo=start(true);token=solo.session();for(int i=0;i<100;i++)check(solo.activeTick(OWNER,token,i),"Solo drag failed");
        check(solo.phase()==BOSS&&solo.recording().isEmpty()&&!solo.wybieCanAssist(),"Previously dead branch played A-D or spawned assistance");finishFight(solo,false);
        var paused=rescue(8);token=paused.session();for(int i=0;i<30;i++)check(paused.activeTick(OWNER,token,200+i),"Pause fixture tick failed");
        paused=reload(paused);check(!paused.resume(OTHER)&&paused.resume(OWNER),"Reconnect owner gate failed");
        check(paused.activeTicks()==30&&!paused.activeTick(OWNER,token,999999),"Reconnect reset window or accepted old nonce");
        check(paused.activeTick(OWNER,paused.session(),0)&&paused.activeTicks()==31,"Reconnect failed to resume active cursor after world clock restore");
        var saved=start(false).save();
        var corrupt=saved.copy();corrupt.putInt("Version",99);blocked(corrupt,"Unknown schema accepted");
        corrupt=saved.copy();corrupt.putUUID("Owner",OTHER);blocked(corrupt,"Owner transplant accepted");
        corrupt=saved.copy();corrupt.putUUID("Door",OTHER);blocked(corrupt,"Door transplant accepted");
        corrupt=saved.copy();corrupt.remove("Hand");blocked(corrupt,"Missing original hand rerolled");
        corrupt=saved.copy();corrupt.remove("Wybie");blocked(corrupt,"Missing original Wybie respawned");
        corrupt=saved.copy();corrupt.putString("Phase","COMPLETE");blocked(corrupt,"Completion without original key receipt accepted");
        corrupt=saved.copy();corrupt.putString("WybieLife","DEAD_AT_WELL");blocked(corrupt,"Impossible death state accepted");
        corrupt=saved.copy();corrupt.putInt("Ticks",100);blocked(corrupt,"Overrun drag state accepted");
        corrupt=saved.copy();corrupt.putInt("Attempts",-1);blocked(corrupt,"Invalid attempt count accepted");
        corrupt=saved.copy();corrupt.putLong("Ticks",0);blocked(corrupt,"Wrong numeric type silently normalized");
        corrupt=saved.copy();corrupt.putUUID("BundleReceipt",OTHER);blocked(corrupt,"Premature disposal receipt accepted");
        corrupt=saved.copy();corrupt.putString("Phase","INTRO_A");corrupt.putString("WybieLife","DEAD_BEFORE_WELL");blocked(corrupt,"Dead Wybie speaking accepted");
        return checks;
    }
}
