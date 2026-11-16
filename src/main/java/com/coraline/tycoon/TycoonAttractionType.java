package com.coraline.tycoon;

import java.util.*;

/** Profiles are offered experiences, not hidden child-match percentages. */
public enum TycoonAttractionType {
    VILLAGE("Other Village",32,6,new double[]{.85,.35,1,.45,.55,.35,.55,.35},"Streets","Community","Murals"),
    WONDERLAND("Wonderland",36,4,new double[]{.8,.95,.45,.95,.55,.15,.2,.7},"Discoveries","Games","Spectacle"),
    CIRCUS("Grand Circus",40,6,new double[]{.3,.8,.8,1,.55,.25,.25,.3},"Acrobatics","Illusions","Audience"),
    GARDEN("Magical Garden",24,4,new double[]{.85,.3,.35,.75,.95,.05,.8,.35},"Botanical Paths","Sculptures","Quiet Grove"),
    RESTAURANT("Candy Restaurant",28,4,new double[]{.25,.4,.85,.65,.55,1,.65,.1},"Kitchen","Shared Tables","Confectionery"),
    GALLERY("Art Gallery",28,4,new double[]{.55,.1,.35,.6,1,.05,.55,.15},"Exhibitions","Studio","Sculpture"),
    MAZE("Adventure Maze",26,3,new double[]{1,.65,.2,.35,.2,.05,.1,1},"Hidden Routes","Puzzles","Discoveries"),
    PLAYGROUND("Playground",20,5,new double[]{.45,1,.85,.35,.2,.05,.25,.55},"Games","Climbing","Team Play"),
    THEATRE("Theatre",34,6,new double[]{.2,.5,.75,.85,.9,.15,.55,.1},"Drama","Stagecraft","Music"),
    RETREAT("Cozy Retreat",20,3,new double[]{.15,.15,.35,.2,.6,.35,1,.05},"Reading Nook","Soft Seating","Scenery");
    public static final String[] INTERESTS={"Explorativeness","Playfulness","Socialness","Aweness","Artisticness","FoodInterest","ComfortPreference","Adventurousness"};
    public final String label;public final int price,capacity;public final double[] profile;public final String[] branches;
    TycoonAttractionType(String label,int price,int capacity,double[] profile,String... branches){this.label=label;this.price=price;this.capacity=capacity;this.profile=profile;this.branches=branches;}
    public String id(){return name().toLowerCase(Locale.ROOT);}
    public static TycoonAttractionType parse(String id){return valueOf(id.toUpperCase(Locale.ROOT));}
    public double quality(int index,int tier,net.minecraft.nbt.CompoundTag branches){
        double base=TycoonAttractionConfig.PROFILES.get(id()).get(index).get();
        int[][] axes=switch(this){
            case VILLAGE->new int[][]{{0,7},{2,6},{4,3}};case WONDERLAND->new int[][]{{0,7},{1,2},{3,4}};
            case CIRCUS->new int[][]{{1,7},{3,4},{2,6}};case GARDEN->new int[][]{{0,7},{4,3},{6,2}};
            case RESTAURANT->new int[][]{{5,6},{2,6},{3,1}};case GALLERY->new int[][]{{4,0},{1,4},{3,4}};
            case MAZE->new int[][]{{0,7},{1,4},{3,0}};case PLAYGROUND->new int[][]{{1,2},{7,0},{2,6}};
            case THEATRE->new int[][]{{4,2},{3,4},{1,4}};case RETREAT->new int[][]{{4,6},{6,2},{3,0}};
        };
        double bonus=0;for(int branch=0;branch<3;branch++)for(int axis:axes[branch])if(axis==index)bonus+=.04*branches.getInt(Integer.toString(branch));
        return Math.min(1,base+.035*(tier-1)+bonus);
    }
}
