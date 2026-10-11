import com.coraline.feast.TemplePath;
import java.util.*;
public class TemplePathCheck {
 public static void main(String[] args){for(int room=0;room<5;room++){var unique=new HashSet<Object>();for(int seed=0;seed<10000;seed++){var path=TemplePath.generate(room,seed);if(!TemplePath.valid(room,path))throw new AssertionError();unique.add(path);}System.out.println(room+": "+unique.size()+" valid distinct routes");}}
}
