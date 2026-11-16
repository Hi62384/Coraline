import com.coraline.client.ShowPlaybackClient;
import com.coraline.feast.ShowPlaybackNetwork;
import java.util.*;
/** Executes the production client observer with a recording-only transport double. */
public final class PlaybackObservationSmoke {
    private static int checks;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    private static void bind(UUID nonce){ShowPlaybackClient.bind(new ShowPlaybackNetwork.Bind(nonce,"reunion_a",false,UUID.randomUUID(),UUID.randomUUID(),7,623));ShowPlaybackNetwork.signals.clear();}
    public static void main(String[] args){
        for(int i=0;i<100;i++){
            UUID current=UUID.randomUUID(),old=UUID.randomUUID();bind(current);
            ShowPlaybackClient.observedFrames(old,"reunion_a",true,true,false);check(ShowPlaybackNetwork.signals.isEmpty(),"Old same-name instance acknowledged new retry");
            ShowPlaybackClient.observedFrames(current,"reunion_b",true,true,false);check(ShowPlaybackNetwork.signals.isEmpty(),"Foreign recording acknowledged checkpoint");
            ShowPlaybackClient.observedFrames(current,"reunion_a",false,false,false);check(ShowPlaybackNetwork.signals.isEmpty(),"Decode-ahead acknowledged start");
            ShowPlaybackClient.observedFrames(current,"reunion_a",true,false,false);check(ShowPlaybackNetwork.signals.size()==1&&ShowPlaybackNetwork.signals.get(0).kind()==0,"Lost actual start");
            ShowPlaybackClient.observedFrames(current,"reunion_a",true,false,false);check(ShowPlaybackNetwork.signals.size()==1,"Repeated start duplicated acknowledgement");
            ShowPlaybackClient.observedFrames(current,"reunion_a",true,true,false);check(ShowPlaybackNetwork.signals.size()==2&&ShowPlaybackNetwork.signals.get(1).kind()==1,"Lost actual final frame");
            ShowPlaybackClient.observedFrames(current,"reunion_a",true,true,false);check(ShowPlaybackNetwork.signals.size()==2,"Duplicated final receipt");
            UUID next=UUID.randomUUID();bind(next);ShowPlaybackClient.observedFrames(current,"reunion_a",true,true,false);check(ShowPlaybackNetwork.signals.isEmpty(),"Rebind accepted prior receipt");
            ShowPlaybackClient.observedFrames(next,"reunion_a",true,true,false);check(ShowPlaybackNetwork.signals.size()==2&&ShowPlaybackNetwork.signals.get(0).kind()==0&&ShowPlaybackNetwork.signals.get(1).kind()==1,"Late completed adjacent binding lost start/end order");
            bind(next);ShowPlaybackClient.observedFrames(next,"reunion_a",false,false,true);check(ShowPlaybackNetwork.signals.size()==1&&ShowPlaybackNetwork.signals.get(0).kind()==2,"Decoder failure was lost or became success");
            ShowPlaybackClient.observedFrames(next,"reunion_a",true,true,false);check(ShowPlaybackNetwork.signals.size()==1,"Failed instance later manufactured completion");
            ShowPlaybackClient.clear();ShowPlaybackNetwork.signals.clear();ShowPlaybackClient.observedFrames(next,"reunion_a",true,true,false);check(ShowPlaybackNetwork.signals.isEmpty(),"Logged-out observer acknowledged a recording");
        }
        System.out.println("PLAYBACK_OBSERVATION_OK checks="+checks+"; real production observer, test transport; NOT network/audio/scene acceptance");
    }
}
