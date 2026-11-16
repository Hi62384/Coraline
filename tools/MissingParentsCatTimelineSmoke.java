package com.coraline.feast;

/** Pure geometry only. This is not a native gameplay, rendering or audio acceptance test. */
public final class MissingParentsCatTimelineSmoke {
    private static int checks;
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        require(MissingParentsCatTimeline.strokes()==21,"HELP US stroke count");
        require(MissingParentsCatTimeline.progress(-2,100)==0,"Negative clock");
        require(MissingParentsCatTimeline.progress(110,100)==1,"Overrun clock");
        for(int duration:new int[]{1,47,110,181,170}){
            float last=-1;
            for(int tick=0;tick<=duration;tick++){
                float pull=MissingParentsCatTimeline.dollPull(tick,duration);
                require(Float.isFinite(pull)&&pull>=last&&pull<=1,"Retrieval must be bounded and monotonic");
                if(tick<duration)require(pull<1,"Dolls finish before recording end");
                last=pull;
            }
            require(last==1,"Dolls do not finish at recording end");
        }
        for(int stroke=0;stroke<MissingParentsCatTimeline.strokes();stroke++)for(int step=0;step<=100;step++){
            var point=MissingParentsCatTimeline.point(stroke,step/100.0);
            require(Double.isFinite(point[0])&&Math.abs(point[0])<.9&&point[1]>=.88&&point[1]<=1.24,"Writing outside mirror");
        }
        require(MissingParentsCatTimeline.point(0,0)[0]>MissingParentsCatTimeline.point(16,0)[0],"Writing reversed for front-facing viewer");
        require(MissingParentsCatTimeline.point(17,1)[0]>MissingParentsCatTimeline.point(19,0)[0],"S has incorrect upper/lower stems");
        for(int tick=0;tick<=MissingParentsCatTimeline.WRITE_TICKS;tick++){
            float progress=MissingParentsCatTimeline.progress(tick,MissingParentsCatTimeline.WRITE_TICKS);
            var finger=MissingParentsCatTimeline.finger(progress);
            double at=Math.min(.999999,progress)*MissingParentsCatTimeline.strokes();
            var ink=MissingParentsCatTimeline.point((int)at,at-Math.floor(at));
            require(Math.abs(finger[0]-ink[0])<1e-8&&Math.abs(finger[1]-ink[1])<1e-8,"Finger detached from live writing");
        }
        System.out.println("MISSING_PARENTS_CAT_TIMELINE_OK: "+checks+" pure-geometry assertions; NOT native visual/audio acceptance.");
    }
}
