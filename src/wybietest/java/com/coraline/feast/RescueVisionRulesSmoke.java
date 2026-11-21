package com.coraline.feast;

import java.util.Random;

/** Run as a standalone Java-17 program; exercises actual dependency-free
 * production rules, not a simulated Minecraft world or save file. */
public final class RescueVisionRulesSmoke {
    private static long checks;
    private static void check(boolean ok,String message){
        checks++;
        if(!ok)throw new AssertionError(message);
    }
    public static void main(String[] args){
        float[] special={Float.NaN,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY,
                -100f,0f,.001f,.5f,1f,9f,20f,2000f};
        for(boolean hasSaved:new boolean[]{false,true})
            for(float saved:special)for(float current:special)for(float max:special){
                float cap=Float.isFinite(max)&&max>0?max:20;
                float from=hasSaved&&Float.isFinite(saved)&&saved>0?saved
                        :Float.isFinite(current)&&current>0?current:1;
                float expected=Math.max(Math.min(cap,.01F),Math.min(cap,from));
                float actual=RescueVisionRules.restoredHealth(hasSaved,saved,current,max);
                check(Float.isFinite(actual)&&actual>0&&actual<=cap,"Dream restored lethal health");
                check(actual==expected,"Dream restored wrong saved health");
            }
        var random=new Random(20261121);
        for(int i=0;i<100_000;i++){
            boolean hasSaved=random.nextBoolean();
            float original=random.nextFloat()*100-30,live=random.nextFloat()*50;
            int savedFood=random.nextInt(60)-20,currentFood=random.nextInt(60)-20;
            int food=RescueVisionRules.restoredFood(hasSaved,savedFood,currentFood);
            check(food>=0&&food<=20,"Food out of bounds");
            if(hasSaved&&savedFood>=0&&savedFood<=20)
                check(food==savedFood,"Valid saved hunger changed");
            float savedSat=i%97==0?Float.NaN:original;
            float currentSat=i%109==0?Float.POSITIVE_INFINITY:live;
            float sat=RescueVisionRules.restoredSaturation(hasSaved,savedSat,currentSat,food);
            check(Float.isFinite(sat)&&sat>=0&&sat<=food,"Saturation out of bounds");
            float savedHealth=i%91==0?Float.NaN:original;
            float restored=RescueVisionRules.restoredHealth(hasSaved,savedHealth,live,20);
            check(Float.isFinite(restored)&&restored>0&&restored<=20,"Invalid health after wake-up");
        }
        System.out.println("RESCUE_VISION_RULES_PASSED checks="+checks+
                "; native Minecraft/Forge vision restore remains a separate test");
    }
    private RescueVisionRulesSmoke(){}
}
