import com.coraline.client.audio.*;
import com.coraline.feast.StoryAudioContinuity;
import com.mojang.blaze3d.audio.OggAudioStream;
import java.nio.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import net.minecraft.client.sounds.AudioStream;

/** Actual STB PCM equality across clip boundaries. This does not claim audible OpenAL QA. */
public final class ContinuousStoryAudioSmoke {
    static long consume(AudioStream stream,MessageDigest digest,int size)throws Exception{long bytes=0;for(;;){var b=stream.read(size);if(!b.hasRemaining())return bytes;bytes+=b.remaining();digest.update(b);}}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);int checks=0;
        for(String start:List.of("theatre_opening_a","theatre_a","cocoa_beetles_e","mirror_prison_b","first_escape_e","missing_parents_c","black_is_traditional_d","parent_rescue_h","missing_parents_tea_e","rescue_bobinsky_a","finale_a","reunion_a","farewell_a")){
            var names=StoryAudioContinuity.run(start);if(names.size()<2)throw new AssertionError("Missing immediate run: "+start);
            var json=com.google.gson.JsonParser.parseString(Files.readString(root.resolve("sounds.json"))).getAsJsonObject();
            var paths=new ArrayList<Path>();
            for(String name:names){String media=json.getAsJsonObject(name).getAsJsonArray("sounds").get(0).getAsJsonObject().get("name").getAsString().replace("coraline:","");paths.add(root.resolve("sounds/"+media+".ogg"));}
            var expected=MessageDigest.getInstance("SHA-256");long total=0;
            for(var path:paths)try(var source=new OggAudioStream(Files.newInputStream(path))){total+=consume(source,expected,16384);}
            byte[] digest=expected.digest();
            for(int request:new int[]{514,8192,16384,65536}){
                var sources=new ArrayList<AudioStream>();for(var path:paths)sources.add(new CloseOnceAudioStream(new OggAudioStream(Files.newInputStream(path))));
                var actual=MessageDigest.getInstance("SHA-256");
                try(var chain=new ContinuousAudioStream(sources)){
                    if(consume(chain,actual,request)!=total||!Arrays.equals(digest,actual.digest()))throw new AssertionError("Inserted, lost or reordered samples at "+start+" request="+request);
                    for(int i=0;i<names.size();i++)if(chain.startSeconds(i)<0||chain.endSeconds(i)<=chain.startSeconds(i))throw new AssertionError("Missing exact decoded boundary");
                }
                checks++;
            }
            System.out.println("PCM_IDENTICAL "+names+" bytes="+total);
        }
        for(String gate:List.of("black_is_traditional_l","cocoa_beetles_a","cocoa_beetles_b","cocoa_beetles_d","first_escape_a","first_escape_b","first_escape_c","parent_rescue_d","parent_rescue_g","parent_rescue_k","missing_parents_tea_d","finale_c","finale_d","finale_g","finale_h","finale_k","finale_m","finale_n","finale_o","finale_q","farewell_b","rescue_bobinsky_c","rescue_bobinsky_d","rescue_bobinsky_e"))if(!StoryAudioContinuity.next(gate).isEmpty())throw new AssertionError("Crossed interaction/scene gate: "+gate);
        System.out.println("CONTINUOUS_STORY_PCM_OK checks="+checks+"; originals unchanged; NOT audible OpenAL acceptance");
    }
}
