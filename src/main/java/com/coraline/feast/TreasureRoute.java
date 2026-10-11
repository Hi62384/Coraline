package com.coraline.feast;
import java.util.*;
import net.minecraft.core.BlockPos;

/** Procedural verses refer to the same seeded garden facts used by construction. */
public final class TreasureRoute {
    public record Landmark(String name,int x,int z,String feature,String search,String riddle) {
        public BlockPos at(BlockPos c){return c.offset(x,0,z);}
        public BlockPos note(BlockPos c){return at(c).offset(name.equals("Mushroom Grove")?-5:name.equals("Golden Orchard")?0:-3,0,name.equals("Mushroom Grove")?2:name.equals("Golden Orchard")||name.equals("Lantern Oak")?3:5);}
    }
    public static final List<Landmark> LANDMARKS=List.of(
        new Landmark("Moon Fountain",-25,-25,"white flowers and a crescent","the little barrel beside the white flowers","A crescent watches water, with white petals at its feet."),
        new Landmark("Ruby Bridge",20,-30,"red timber and golden lamps","the barrel at the red bridge's near end","Water passes under red timber; golden lamps guard both banks."),
        new Landmark("Lantern Oak",-45,-60,"a huge oak wearing gold lights","the barrel tucked beside the root bench","Only one great oak wears golden lights. Search where its roots meet a seat."),
        new Landmark("Golden Gazebo",43,-66,"gold trim and roses","the barrel beside the rose-covered entrance","A roof rests on golden shoulders, ringed in red roses."),
        new Landmark("Clock Tower",0,-76,"a clock over four meeting paths","the barrel under the clock's face","I have no voice, yet every hour I speak. Four paths meet at my feet."),
        new Landmark("Mushroom Grove",-35,-105,"three giant red mushrooms","the barrel beside the smallest red cap","Three red umbrellas never close. Search beside the shortest one."),
        new Landmark("Glass Greenhouse",28,-111,"glass roof beside a purple garden","the barrel beside the glass door","Not the green-roofed house: seek the house whose roof lets you see the sky, beside purple flowers."),
        new Landmark("Broken Watchtower",-60,-126,"cracked stone and no clock","the barrel beside the broken arch","Beyond the red mushroom grove stands a cracked tower that tells no time."),
        new Landmark("Wishing Well",4,-47,"a roof over a deep well","the barrel beside the well's bucket","I hold water but am not a pond. My roof shades wishes, not people."),
        new Landmark("Chess Garden",-55,-20,"a black-and-white board with stone pieces","the barrel behind the white king","A silent army stands on black and white. Search behind its pale king."),
        new Landmark("Golden Orchard",58,-22,"three golden-fruited trees","the barrel beneath the middle fruit tree","Three trees bear golden gifts. The middle tree hides a secret at its feet."),
        new Landmark("Emerald Cottage",0,-132,"green roof between mushrooms and glass","the barrel beside the green front door","South of the path between red umbrellas and the house of glass, a green-roofed cottage sleeps.")
    );
    public static int[] generate(long seed){var r=new Random(seed);var ids=new ArrayList<Integer>();for(int i=0;i<LANDMARKS.size();i++)ids.add(i);int first=new int[]{0,1,8}[r.nextInt(3)],last=new int[]{6,7,11}[r.nextInt(3)];ids.remove(Integer.valueOf(first));ids.remove(Integer.valueOf(last));Collections.shuffle(ids,r);int[] route={first,ids.get(0),ids.get(1),ids.get(2),ids.get(3),last};validate(route);return route;}
    public static void validate(int[] route){if(route.length!=6)throw new IllegalArgumentException("Six clues required");var seen=new HashSet<Integer>();double distance=0;int x=0,z=0;for(int id:route){if(id<0||id>=LANDMARKS.size()||!seen.add(id))throw new IllegalArgumentException("Invalid or looping route");var m=LANDMARKS.get(id);distance+=Math.hypot(x-m.x,z-m.z);x=m.x;z=m.z;if(m.feature.isBlank()||m.search.isBlank()||Math.abs(x)>65||z< -140)throw new IllegalArgumentException("Invalid landmark facts");}if(distance>750)throw new IllegalArgumentException("Unfair route length");if(route[5]!=6&&route[5]!=7&&route[5]!=11)throw new IllegalArgumentException("No vault at final destination");}
    public record SealGarden(int flower,int lamps){}
    public static List<SealGarden> gardens(long seed){
        var facts=new ArrayList<SealGarden>();
        for(int flower=0;flower<4;flower++)for(int lamps=2;lamps<=4;lamps++)facts.add(new SealGarden(flower,lamps));
        Collections.shuffle(facts,new Random(seed^0x634AC911DBL));return List.copyOf(facts);
    }
    private static final String[][] PETALS={
        {"My petals borrow the blush of a beating heart.","The colour of a rose's promise clothes my roots.","My flowers wear the shade of a sunset's final ember."},
        {"A cloudless noon has lent its colour to my petals.","My blooms dress as the sea, but never taste its salt.","The sky fell into my flower bed without a single cloud."},
        {"My petals wear winter, yet no frost lives here.","An unwritten page has lent its colour to my flowers.","My blooms share the colour of snow, not its cold."},
        {"My petals wear the robes of royalty.","Red and blue have made a truce inside my flowers.","Twilight's violet ink has stained my flower bed."}
    };
    private static final String[][] LAMPS={
        {"My standing lights equal the faces of a coin.","Count a spider's legs, then quarter them: so many lamps keep watch.","Three flames lost one companion; those that remain are mine."},
        {"My standing lights equal the corners of a triangle.","Halve an insect's legs to count my watching flames.","A pair of lamps invited one more to keep their vigil."},
        {"One standing light keeps watch for every season.","Halve a spider's legs to count my watching flames.","My lamps number twice the faces of a coin."}
    };
    public static String clue(long seed,int[] route,int stage){
        var facts=gardens(seed).get(route[stage]);var random=new Random(seed+stage*7919L);
        String verse=PETALS[facts.flower][random.nextInt(3)]+" "+LAMPS[facts.lamps-2][random.nextInt(3)];
        return verse+(stage==5?" Five seals purchase passage below this garden.":" Where both verses agree, a folded secret waits.");
    }
    public static String clue(int[] route,int stage){return clue(0,route,stage);}
    private TreasureRoute(){}
}
