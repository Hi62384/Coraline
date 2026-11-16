package com.coraline.feast;

/** Pure timeline assertions; not a substitute for native visual/audio acceptance. */
public final class CocoaBeetlesChoreographySmoke {
    private static int assertions;
    private static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        check(CocoaBeetlesChoreography.wardrobe(0)==0,"Wardrobe starts in its side position");
        check(CocoaBeetlesChoreography.wardrobe(70)==1,"Wardrobe fully blocks at tick 70");
        check(CocoaBeetlesChoreography.MUSIC_START-CocoaBeetlesChoreography.WARDROBE_BLOCKED==80,"Music delay is exactly four seconds");
        check(!CocoaBeetlesChoreography.music(149,false)&&CocoaBeetlesChoreography.music(150,false),"Music starts at the correct boundary");
        check(!CocoaBeetlesChoreography.music(1000,true),"Rejection never restarts music");
        check(CocoaBeetlesChoreography.trayOpen(35)==0&&CocoaBeetlesChoreography.trayOpen(65)==1,"Tray is delivered closed before opening");
        check(!CocoaBeetlesChoreography.beetleTaken(147,203)&&CocoaBeetlesChoreography.beetleTaken(148,203),"Pickup waits until the presentation ends");
        check(CocoaBeetlesChoreography.biteLift("COCOA_B",148,203)==0&&CocoaBeetlesChoreography.biteLift("COCOA_B",203,203)==1,"Pinched beetle travels continuously from tray to mouth");
        check(CocoaBeetlesChoreography.beetleRemaining("COCOA_BITE",19)==1&&CocoaBeetlesChoreography.beetleRemaining("COCOA_BITE",29)==0,"Beetle is consumed only at the audible bite");
        check(CocoaBeetlesChoreography.sconce(86,0)==0&&CocoaBeetlesChoreography.sconce(108,0)==1&&CocoaBeetlesChoreography.sconce(108,1)<1,"Sconces reveal separately, not all at once");
        check(CocoaBeetlesChoreography.lampLean(65)==1&&CocoaBeetlesChoreography.lampLean(160)==0,"Mantis leans in and withdraws");
        check(CocoaBeetlesChoreography.morph(.25F,"head")>CocoaBeetlesChoreography.morph(.25F,"body"),"Neck/head leads the body");
        check(CocoaBeetlesChoreography.morph(.25F,"right_arm")==0,"Arms do not elongate before their beat");
        for(String bone:new String[]{"head","body","right_arm","left_forearm","left_leg","right_shin"}){
            check(CocoaBeetlesChoreography.morph(0,bone)==0&&CocoaBeetlesChoreography.morph(1,bone)==1,"Morph endpoints for "+bone);
            float last=0;for(int tick=0;tick<=132;tick++){float value=CocoaBeetlesChoreography.morph(tick/132F,bone);check(value>=last&&value<=1,"Monotonic bounded morph for "+bone);last=value;}
        }
        System.out.println("COCOA_CHOREOGRAPHY_PASSED "+assertions+" assertions");
    }
}
