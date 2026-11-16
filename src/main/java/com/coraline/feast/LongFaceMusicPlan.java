package com.coraline.feast;
/** Shared phase rules for the piano reveal score and cane fade boundary. */
public final class LongFaceMusicPlan {
    public static final String SOUND="he_pulled_a_long_face_music";
    public static int stage(String study,String escape){
        // The escape snapshot takes precedence over a delayed study packet.
        if(!escape.isEmpty())return escape.equals("FIND_CANE")||escape.equals("CANE_COLLECTED")?2:0;
        if(java.util.Set.of("P","Q","R","S").contains(study))return 1;
        if(study.equals("STUDY_REVEAL_COMPLETE"))return 2;
        return 0;
    }
    public static float gain(int stage){return stage==1?.10F:.36F;}
    private LongFaceMusicPlan(){}
}
