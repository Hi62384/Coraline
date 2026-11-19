package com.coraline.feast;

/** The booth's saved step numbers and presentation slots share one station layout. */
public final class CakeKitchenRules {
    public enum Station {
        BOWL(0), OVEN(-2), STAND(2), JUDGING(-22);

        final int offset;
        Station(int offset){this.offset=offset;}

        static Station at(int x,int y,int z){
            if(x!=0||y!=0)return null;
            for(var station:values())if(station.offset==z)return station;
            return null;
        }

        static Station workpiece(int slot){
            return switch(slot){case 0->BOWL;case 1->OVEN;case 2->STAND;default->null;};
        }
    }

    public static final int EMPTY=10, ASSEMBLED=11;

    static int mode(Station station,int step,long ovenAge){
        return switch(station){
            case BOWL->step==1?1:0;
            case OVEN->step==3?ovenMode(ovenAge):EMPTY;
            case STAND->step==2?3:step>=4?ASSEMBLED:EMPTY;
            case JUDGING->EMPTY;
        };
    }

    private static int ovenMode(long age){
        if(age<40)return 4;
        if(age<85)return 5;
        if(age<106)return 6;
        if(age<=144)return 7;
        return age<=176?8:9;
    }

    static int bakeQuality(long age){return age<40||age>176?0:age>=106&&age<=144?4:age>=85&&age<=160?3:1;}

    static boolean decorationEditable(int step,int coverage){return step==4&&coverage==0;}

    static boolean ingredientEditable(int id,int selected,int required){
        if(id<0||id>=12)return false;
        int bit=1<<id;
        if((selected&bit)!=0)return (required&bit)==0;
        int missing=required&~selected;
        return missing!=0?id==Integer.numberOfTrailingZeros(missing):(required&bit)==0;
    }

    static boolean actionReady(long now,long last){return now<last||now-last>=4;}

    public static int layers(int stored){return Math.max(1,Math.min(3,stored));}

    private CakeKitchenRules(){}
}
