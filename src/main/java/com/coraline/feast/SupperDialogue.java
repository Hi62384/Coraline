package com.coraline.feast;

/** Measured audio durations keep supper dialogue ordered and prevent the cake cutting it off. */
final class SupperDialogue {
    static final String FATHER="other_father_golden_chicken_breast";
    static int motherStart(int index){
        int tick=100+VisitAudio.length(FATHER)+VisitConfig.SUPPER_VOICE_GAP.get();
        for(int i=0;i<index;i++)tick+=VisitAudio.length("other_mother_supper_"+i)+VisitConfig.SUPPER_VOICE_GAP.get();
        return tick;
    }
    static String cueAt(int elapsed){
        if(elapsed==100)return FATHER;
        for(int i=0;i<3;i++)if(elapsed==motherStart(i))return "other_mother_supper_"+i;
        return null;
    }
    static int endTick(){return Math.max(VisitConfig.FEAST_TICKS.get(),motherStart(2)+VisitAudio.length("other_mother_supper_2")+VisitConfig.SUPPER_VOICE_GAP.get());}
}
