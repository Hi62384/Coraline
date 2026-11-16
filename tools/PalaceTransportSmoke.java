import com.coraline.palace.*;
import java.util.UUID;
import net.minecraft.nbt.*;
import static com.coraline.palace.PalaceTransportProgress.Phase.*;

/** Actual active-tick/reload/owner clock rules, not a truck playthrough. */
public final class PalaceTransportSmoke {
    private static int checks;
    private static long clock;
    private static final UUID OWNER=UUID.randomUUID(),DOOR=UUID.randomUUID(),OTHER=UUID.randomUUID();
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static PalaceTransportProgress reload(PalaceTransportProgress state){
        var before=state.save();var after=PalaceTransportProgress.load(OWNER,DOOR,before);
        check(after.blocker().isEmpty()&&after.save().equals(before),"Transport changed on actual NBT reload");return after;
    }
    private static void ticks(PalaceTransportProgress state,int count,boolean attached){
        var token=state.session();for(int i=0;i<count;i++){
            int before=state.ticks();check(!state.tick(OTHER,token,clock,false)&&!state.tick(OWNER,OTHER,clock,false),"Foreign owner/session advances ride");
            check(state.tick(OWNER,token,++clock,attached),"Real active tick rejected");check(!state.tick(OWNER,token,clock,attached),"Duplicate server tick advances ride twice");check(state.ticks()==before+1,"Offline/wall time caught up the cinematic");
        }
    }
    public static int run(){
        for(int attempt=0;attempt<64;attempt++){
            var state=PalaceTransportProgress.start(OWNER,DOOR);var token=state.session();
            check(!state.motionFinished(OWNER,token,LIFT,true,true),"New registration skips the worker approach");ticks(state,40,false);
            check(!state.motionFinished(OWNER,token,LIFT,false,true),"Workers not physically present lifted player");check(state.motionFinished(OWNER,token,LIFT,true,true),"Actual worker approach not accepted");
            check(!state.tick(OWNER,token,clock+1,false),"Missing physical carrier advanced lift");ticks(state,30,true);check(state.motionFinished(OWNER,token,CARRY,true,false),"Lift completion rejected");
            ticks(state,80,true);check(!state.motionFinished(OWNER,token,THROW,false,true),"Blocked physical carry advanced");check(state.motionFinished(OWNER,token,THROW,true,true),"Actual carry arrival rejected");
            ticks(state,30,true);check(state.motionFinished(OWNER,token,CLOSE,true,true),"Actual cargo landing rejected");ticks(state,40,true);
            check(!state.motionFinished(OWNER,token,TRANSIT,true,false),"Closed doors started ride without an actual preloaded destination");
            check(state.motionFinished(OWNER,token,TRANSIT,true,true)&&state.ticks()==0,"T=0 does not coincide with locked rear doors");
            for(int t=1;t<=160;t++){
                ticks(state,1,true);
                check(PalaceJourneyTiming.truckBeat(state.ticks())==(t<60?PalaceJourneyTiming.TruckBeat.WAIT:t<160?PalaceJourneyTiming.TruckBeat.DRIVING:PalaceJourneyTiming.TruckBeat.TRANSFER_REQUIRED),"3s/5s/8s truck anchors changed");
                if(t<160)check(!state.motionFinished(OWNER,token,ARRIVE,true,true),"Truck transferred before exactly eight seconds");
                if(t==80){
                    state=reload(state);var old=token;check(state.resume(OWNER),"Interrupted ride cannot resume");token=state.session();check(!old.equals(token)&&state.ticks()==80,"Reconnect resets ride or keeps stale session");
                    check(!state.tick(OWNER,old,clock+1,true),"Stale pre-reconnect input advanced ride");clock+=1_000_000;
                }
            }
            check(!state.motionFinished(OWNER,token,ARRIVE,false,true),"Elapsed time alone fabricated destination transfer");check(state.motionFinished(OWNER,token,ARRIVE,true,true),"Actual eight-second transfer rejected");
            ticks(state,40,true);check(state.motionFinished(OWNER,token,EXIT,true,true),"Opened cargo doors rejected");
            check(!state.motionFinished(OWNER,token,COMPLETE,false,true),"Passenger still in truck received completion");check(state.motionFinished(OWNER,token,COMPLETE,true,true),"Actual grounded exit rejected");
            state=reload(state);check(!state.resume(OWNER)&&!state.tick(OWNER,state.session(),++clock,true),"Completed trip restarts on reconnect");
        }
        var valid=PalaceTransportProgress.start(OWNER,DOOR).save();
        for(String key:new String[]{"Version","Owner","Door","Session","Phase","Ticks","Last"}){var bad=valid.copy();bad.remove(key);var state=PalaceTransportProgress.load(OWNER,DOOR,bad);check(!state.blocker().isEmpty(),"Incomplete ride accepted: "+key);var saved=state.save();saved.remove("Blocker");check(saved.equals(bad),"Blocked ride discarded evidence");}
        check(!PalaceTransportProgress.load(OTHER,DOOR,valid).blocker().isEmpty()&&!PalaceTransportProgress.load(OWNER,OTHER,valid).blocker().isEmpty(),"Foreign ride ownership accepted");
        return checks;
    }
}
