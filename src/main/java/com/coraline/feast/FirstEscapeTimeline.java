package com.coraline.feast;

/** Ordered story checkpoints. Interaction states cannot advance merely by waiting. */
public final class FirstEscapeTimeline {
    public enum Phase {
        FIND_CANE,CANE_COLLECTED,PRY_DOOR,ROOM_DARK,ROOM_TRANSFORMING,COCOA_A,CHAIR_CAPTURE,
        COCOA_B,COCOA_BITE,COCOA_C,COCOA_D,APOLOGY_CHOICE,COCOA_E,COCOA_F,COCOA_G,MIRROR_DRAG,MIRROR_THROW,
        MIRROR_A,MIRROR_RETRACT,MIRROR_B,MIRROR_C,MIRROR_D,MIRROR_E,
        ESCAPE_A,MASK_REMOVAL,MASK_ANIMATION,ESCAPE_B,STITCH_REMOVAL,STITCH_ANIMATION,ESCAPE_C,FOLLOW_WYBIE,
        PUSH_WARDROBE,PUSHING,ESCAPE_D,OPEN_LITTLE_DOOR,ESCAPE_E,ESCAPE_F,ESCAPE_G,ESCAPE_PUSH,ESCAPE_TUNNEL,
        MISSING_A,WYBIE_GAZE,MISSING_B,SHOW_DOOR,MISSING_C,MISSING_D,THROW_SHOE,MISSING_E,MISSING_F,MISSING_G,BICYCLE_DEPARTURE,ESCAPE_COMPLETE
    }
    public static String sound(Phase phase){String value=phase.name();for(String prefix:new String[]{"COCOA_","MIRROR_","ESCAPE_","MISSING_"})if(value.startsWith(prefix)&&value.length()==prefix.length()+1){String part=switch(prefix){case "COCOA_"->"cocoa_beetles";case "MIRROR_"->"mirror_prison";case "ESCAPE_"->"first_escape";default->"missing_parents";};return part+"_"+value.substring(prefix.length()).toLowerCase(java.util.Locale.ROOT);}return "";}
    public static Phase after(Phase p){return switch(p){
        case COCOA_A->Phase.CHAIR_CAPTURE;case COCOA_B->Phase.COCOA_BITE;case COCOA_C->Phase.COCOA_D;case COCOA_D->Phase.APOLOGY_CHOICE;case COCOA_E->Phase.COCOA_F;case COCOA_F->Phase.COCOA_G;case COCOA_G->Phase.MIRROR_DRAG;
        case MIRROR_A->Phase.MIRROR_RETRACT;case MIRROR_B->Phase.MIRROR_C;case MIRROR_C->Phase.MIRROR_D;case MIRROR_D->Phase.MIRROR_E;case MIRROR_E->Phase.ESCAPE_A;
        case ESCAPE_A->Phase.MASK_REMOVAL;case ESCAPE_B->Phase.STITCH_REMOVAL;case ESCAPE_C->Phase.FOLLOW_WYBIE;case ESCAPE_D->Phase.OPEN_LITTLE_DOOR;case ESCAPE_E->Phase.ESCAPE_F;case ESCAPE_F->Phase.ESCAPE_G;case ESCAPE_G->Phase.ESCAPE_PUSH;
        case MISSING_A->Phase.WYBIE_GAZE;case MISSING_B->Phase.SHOW_DOOR;case MISSING_C->Phase.MISSING_D;case MISSING_D->Phase.THROW_SHOE;case MISSING_E->Phase.MISSING_F;case MISSING_F->Phase.MISSING_G;case MISSING_G->Phase.BICYCLE_DEPARTURE;default->null;};}
    public static boolean prison(Phase p){return p.ordinal()>=Phase.MIRROR_A.ordinal()&&p.ordinal()<=Phase.MIRROR_E.ordinal();}
    public static boolean overworld(Phase p){return p.ordinal()>=Phase.MISSING_A.ordinal();}
    public static boolean seated(Phase p){return p.ordinal()>=Phase.COCOA_B.ordinal()&&p.ordinal()<=Phase.COCOA_G.ordinal();}
    public static boolean wardrobeBlocksPassage(Phase p){return p.ordinal()>=Phase.ROOM_DARK.ordinal()&&p.ordinal()<Phase.OPEN_LITTLE_DOOR.ordinal();}
    private FirstEscapeTimeline(){}
}
