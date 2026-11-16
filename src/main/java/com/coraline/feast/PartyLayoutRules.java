package com.coraline.feast;
import java.util.*;
/** Pure layout rules shared by real resource parsing, NPC formation, storage guards and cake rendering. */
public final class PartyLayoutRules {
    public record Tile(int x,int y,int z){}
    public record Strip(int first,int last,int y,int z){}
    public record Offset(double x,double z){}
    public static List<Strip> strips(Collection<Tile> tiles){
        Objects.requireNonNull(tiles);var rows=new TreeMap<Long,TreeSet<Integer>>();
        for(var tile:tiles){Objects.requireNonNull(tile);long key=((long)tile.y()<<32)|(tile.z()&0xffffffffL);rows.computeIfAbsent(key,k->new TreeSet<>()).add(tile.x());}
        var result=new ArrayList<Strip>();for(var row:rows.entrySet()){Integer first=null,last=null;for(int x:row.getValue()){if(first!=null&&(long)x!=(long)last+1){result.add(new Strip(first,last,(int)(row.getKey()>>32),(int)(long)row.getKey()));first=x;}if(first==null)first=x;last=x;}if(first!=null)result.add(new Strip(first,last,(int)(row.getKey()>>32),(int)(long)row.getKey()));}return List.copyOf(result);
    }
    public static Offset crowd(int index){if(index<0)throw new IllegalArgumentException("Negative crowd index");double angle=Math.toRadians(-120+(index%10)*240./9),radius=4.5+(index/10)*1.8;return new Offset(Math.sin(angle)*radius,Math.cos(angle)*radius);}
    public static int cakeSlices(int value){return Math.max(0,Math.min(12,value));}
    public static boolean protectedRoom(String room){return "attic".equals(room)||"basement".equals(room)||"archive".equals(room);}
    private PartyLayoutRules(){}
}
