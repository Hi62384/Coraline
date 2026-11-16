package com.coraline.palace;

/** Pure choreography constraints for the future physical truck/hand controller.
 * None of these values acknowledges audio or invents a completed world action.
 */
public final class PalaceJourneyTiming {
    public static final int TRUCK_WAIT=60,TRUCK_DRIVE=100,TRUCK_TRANSFER=TRUCK_WAIT+TRUCK_DRIVE;
    public static final int HAND_DRAG=100,WYBIE_START_RIDING=40;
    public enum TruckBeat { WAIT,DRIVING,TRANSFER_REQUIRED }
    public static TruckBeat truckBeat(int activeTicks){if(activeTicks<0)throw new IllegalArgumentException("Negative journey cursor");return activeTicks<TRUCK_WAIT?TruckBeat.WAIT:activeTicks<TRUCK_TRANSFER?TruckBeat.DRIVING:TruckBeat.TRANSFER_REQUIRED;}
    public static double truckRock(int activeTicks,double partial){
        if(!Double.isFinite(partial)||partial<0||partial>1)throw new IllegalArgumentException("Invalid render fraction");
        if(truckBeat(activeTicks)!=TruckBeat.DRIVING)return 0;double time=activeTicks+partial-TRUCK_WAIT;
        double envelope=Math.min(1,time/8);return (Math.sin(time*1.17)*.24+Math.sin(time*.43)*.13)*envelope;
    }
    /** Reach exact tongs capture at the actual A end, without truncating A.
     * A <= 2 s is an authored timing conflict, never permission to seek/skip it.
     */
    public static double downhillProgress(double recordingTick,int actualDuration){
        if(!Double.isFinite(recordingTick)||actualDuration<=WYBIE_START_RIDING)throw new IllegalArgumentException("A must leave riding time after its required two-second delay");
        double progress=Math.max(0,Math.min(1,(recordingTick-WYBIE_START_RIDING)/(actualDuration-WYBIE_START_RIDING)));return progress*progress*(3-2*progress);
    }
    /** Tune the 5–8 s rescue window to an actually surveyed reachable route.
     * Never use straight-line distance through rocks as proof of reachability.
     */
    public static int rescueWindow(double reachableRouteLength){
        if(!Double.isFinite(reachableRouteLength)||reachableRouteLength<0||reachableRouteLength>32)throw new IllegalArgumentException("No fair bounded well-rescue approach");
        return Math.max(100,Math.min(160,(int)Math.ceil(reachableRouteLength/4.3*20)+50));
    }
    /** No optional-visit ban is interpreted as death. Saved death is irreversible
     * across ordinary fight retries; later speech is forbidden on either loss.
     */
    public static boolean canPlayWybieRecording(char cue,boolean previouslyDead,boolean diedAtWell){
        if(cue<'A'||cue>'I')throw new IllegalArgumentException("Unknown hand recording");
        return !previouslyDead&&(!diedAtWell||cue<='D');
    }
    private PalaceJourneyTiming(){}
}
