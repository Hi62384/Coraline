import com.coraline.client.audio.*;
import net.minecraft.client.sounds.AudioStream;
import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.util.List;

/** Decoder/mixer-observer rules only. Synthetic samples are not scene/audio acceptance. */
public final class PlayedAudioFramesSmoke {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static class Pcm implements AudioStream {
        int left;final boolean nullEof;
        Pcm(int frames,boolean nullEof){left=frames;this.nullEof=nullEof;}
        public AudioFormat getFormat(){return new AudioFormat(1000,16,1,true,false);}
        public ByteBuffer read(int bytes){int count=Math.min(left,Math.max(1,bytes/2));left-=count;return count==0&&nullEof?null:ByteBuffer.allocate(count*2);}
        public void close(){}
    }
    public static void main(String[] args)throws Exception {
        for(int decoded=0;decoded<=1000;decoded++){
            var frames=new PlayedAudioFrames();frames.sample(Long.MAX_VALUE,decoded);
            check(!frames.started()&&!frames.reached(0),"Read-ahead started a recording");
            frames.start();frames.sample(Long.MAX_VALUE,decoded);
            check(frames.frames()==decoded,"Cursor exceeded decoded original frames");
            frames.sample(-1,decoded);frames.sample(0,decoded);
            check(frames.frames()==decoded,"Stale/invalid cursor rewound playback");
            frames.cancel();frames.sample(decoded+100,decoded+100);frames.start();
            check(frames.frames()==decoded&&frames.cancelled(),"Cancellation consumed queued, unheard frames");
        }
        for(boolean nullEof:List.of(false,true))try(var chain=new ContinuousAudioStream(List.of(new Pcm(100,nullEof),new Pcm(200,nullEof)))){
            check(chain.read(600).remaining()==600,"Changed decoded originals");
            chain.read(2);
            check(!chain.segmentStarted(0)&&!chain.segmentEnded(0),"Decode-ahead fabricated a start/end");
            chain.playback().start();chain.heardFrames(0);
            check(chain.segmentStarted(0)&&!chain.segmentEnded(0),"Real source start was not observed");
            chain.heardFrames(99);check(!chain.segmentEnded(0),"First clip ended before its final frame");
            chain.heardFrames(100);check(chain.segmentEnded(0)&&chain.segmentStarted(1)&&!chain.segmentEnded(1),"Exact adjacent boundary lost");
            chain.heardFrames(299);check(!chain.segmentEnded(1),"Last clip ended before its final frame");
            chain.heardFrames(300);check(chain.segmentEnded(1)&&chain.playedSeconds()==.3,"Late binding lost the genuine completed clip");
            chain.playback().release();chain.heardFrames(400);check(chain.segmentEnded(1)&&chain.playback().released(),"Natural release discarded the actual completed boundary");
        }
        try(var chain=new ContinuousAudioStream(List.of(new Pcm(100,false)))){chain.read(200);chain.read(2);chain.playback().start();chain.heardFrames(50);chain.playback().release();chain.heardFrames(100);check(!chain.segmentEnded(0),"Early release fabricated a final frame");}
        try(var chain=new ContinuousAudioStream(List.of(new Pcm(100,false),new Pcm(200,false)))){
            chain.read(600);chain.read(2);chain.playback().start();chain.heardFrames(150);chain.playback().cancel();chain.heardFrames(300);
            check(chain.segmentEnded(0)&&!chain.segmentEnded(1),"Explicit stop counted the queued suffix as heard");
        }
        AudioStream broken=new Pcm(0,false){@Override public ByteBuffer read(int bytes){throw new IllegalStateException("decoder failure");}};
        try(var chain=new ContinuousAudioStream(List.of(broken))){try{chain.read(2);throw new AssertionError("Failed decoder accepted");}catch(IllegalStateException expected){check(chain.failed(),"Lost decoder failure receipt");}}
        System.out.println("PLAYED_AUDIO_FRAMES_OK checks="+checks+"; NOT audible OpenAL/scene acceptance");
    }
}
