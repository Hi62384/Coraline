package com.coraline.feast;

/** Pure presentation checks; no Minecraft client, server, or GPU. */
public final class FirstEscapeVisualRulesSmoke {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        for(String phase:new String[]{"MIRROR_A","MIRROR_RETRACT","MIRROR_B","MIRROR_C","MIRROR_D","MIRROR_E"})check(FirstEscapeVisualRules.prison(phase),"Prison phase missed: "+phase);
        for(String phase:new String[]{"MIRROR_DRAG","MIRROR_THROW","ESCAPE_A","COMPLETE",""})check(!FirstEscapeVisualRules.prison(phase),"Darkness leaked into "+phase);
        check(FirstEscapeVisualRules.prisonLight(0)>=.06F&&FirstEscapeVisualRules.prisonLight(15)<.45F,"Prison must be dim but navigable");
        check(FirstEscapeVisualRules.prisonLight(8)>.15F,"Ghost reveal must illuminate nearby floor and bed");
        int finalPixel=FirstEscapeVisualRules.prisonPixel(0xFFB09070);check(finalPixel==0xFFB09070,"Do not multiply prison darkness a second time after gamma");
        check(FirstEscapeVisualRules.prisonLight(-1)==FirstEscapeVisualRules.prisonLight(0)&&FirstEscapeVisualRules.prisonLight(20)==FirstEscapeVisualRules.prisonLight(15),"Unbounded light input");
        check(FirstEscapeVisualRules.ghostSad(1,false,0)&&FirstEscapeVisualRules.ghostSad(2,false,0),"Imprisoned ghosts smile");
        check(!FirstEscapeVisualRules.ghostSad(4,true,100)&&FirstEscapeVisualRules.ghostSad(4,true,240),"Freed farewell expressions regressed");
        double centre=FirstEscapeVisualRules.stitchedSmileY(0);check(FirstEscapeVisualRules.stitchedSmileY(-.105)<centre&&FirstEscapeVisualRules.stitchedSmileY(.105)<centre,"Stitched smile corners not raised");
        for(int i=-10;i<=10;i++){double y=FirstEscapeVisualRules.stitchedSmileY(i*.0105);check(y>=-2.31/16&&y<=-1.51/16,"Stitches escaped the jaw");}
        System.out.println("Prison phase/light bounds, sad captive ghosts, farewell expression and stitched-smile jaw alignment passed.");
    }
    private FirstEscapeVisualRulesSmoke(){}
}
