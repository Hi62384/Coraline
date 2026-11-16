import com.coraline.feast.*;
import java.util.*;

public final class EventRevisionSmoke {
    public static void main(String[] args){
        var random=new Random(7251);var types=List.of("portal","dinner","games","away","delivery","picnic");var counts=new HashMap<String,Integer>();String previous=null;
        for(int i=0;i<600000;i++){String next=VisitEventSelection.chooseFortySixty(types,t->true,previous,"portal",random::nextInt);if(next.equals(previous))throw new AssertionError("Back-to-back repeat");counts.merge(next,1,Integer::sum);previous=next;}
        if(Math.abs(counts.get("portal")/600000.-.4)>.003)throw new AssertionError(counts);
        for(String type:types)if(!type.equals("portal")&&Math.abs(counts.get(type)/600000.-.12)>.003)throw new AssertionError(counts);
        for(long seed=0;seed<100;seed++)for(int round=0;round<ConstellationPuzzle.ROUNDS;round++){var targets=ConstellationPuzzle.targets(seed,round);if(!ConstellationPuzzle.matches(seed,round,targets)||ConstellationPuzzle.matches(seed,round,List.of(0,0,1)))throw new AssertionError("Puzzle authority");if(ConstellationPuzzle.stars(seed,round).length!=16)throw new AssertionError("Star count");}
        System.out.println("600,000 event draws: no repeats, 40/60 equal distribution; 600 constellation rounds passed. "+counts);
    }
}
