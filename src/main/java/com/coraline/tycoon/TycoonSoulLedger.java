package com.coraline.tycoon;

/** Fixed-point sub-units extend, rather than replace, the existing whole balance. */
public final class TycoonSoulLedger {
    public static final long SCALE=1_000_000L;
    public static boolean canCredit(TycoonData.Home h,long micros){return micros>0&&micros<=Long.MAX_VALUE-SCALE&&Math.max(0,h.state.getLong("SoulEssence"))<=Long.MAX_VALUE-(Math.floorMod(h.state.getLong("SoulFraction"),SCALE)+micros)/SCALE;}
    public static boolean creditMicros(TycoonData.Home h,long micros){
        if(!canCredit(h,micros))return false;
        long whole=Math.max(0,h.state.getLong("SoulEssence")),fraction=Math.floorMod(h.state.getLong("SoulFraction"),SCALE),sum=fraction+micros,carry=sum/SCALE;
        if(whole>Long.MAX_VALUE-carry)return false;h.state.putLong("SoulEssence",whole+carry);h.state.putLong("SoulFraction",sum%SCALE);return true;
    }
    public static String display(TycoonData.Home h){return h.state.getLong("SoulEssence")+String.format(java.util.Locale.ROOT,".%03d",Math.floorMod(h.state.getLong("SoulFraction"),SCALE)/1000);}
    public static boolean canDebit(TycoonData.Home h,long micros,long reserve){
        if(micros<0||reserve<0)return false;long whole=h.state.getLong("SoulEssence"),fraction=Math.floorMod(h.state.getLong("SoulFraction"),SCALE),needed=micros/SCALE+(micros%SCALE>fraction?1:0);return whole>=needed&&whole-needed>=reserve;
    }
    public static boolean debit(TycoonData.Home h,long micros,long reserve){
        if(!canDebit(h,micros,reserve))return false;long units=micros/SCALE,remainder=micros%SCALE,fraction=Math.floorMod(h.state.getLong("SoulFraction"),SCALE);if(remainder>fraction){units++;fraction+=SCALE;}h.state.putLong("SoulEssence",h.state.getLong("SoulEssence")-units);h.state.putLong("SoulFraction",fraction-remainder);return true;
    }
    private TycoonSoulLedger(){}
}
