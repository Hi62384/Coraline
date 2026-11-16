import com.coraline.client.audio.FrameOffsetAudioStream;
import net.minecraft.client.sounds.AudioStream;
import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bounded PCM regression, not native Vorbis/OpenAL or cinematic acceptance. */
public final class ShowAudioStreamSmoke {
    private static int checks;
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static final class Source implements AudioStream {
        final byte[] bytes; final int chunk; int at,closes;
        Source(int frames,int chunk){this.bytes=new byte[frames*4];this.chunk=chunk;for(int i=0;i<bytes.length;i++)bytes[i]=(byte)(i*31+i/257);}
        public AudioFormat getFormat(){return new AudioFormat(16000,16,2,true,false);}
        public ByteBuffer read(int requested){int length=Math.min(bytes.length-at,Math.min(chunk,requested));var result=ByteBuffer.wrap(bytes,at,length).slice();at+=length;return result;}
        public void close(){closes++;}
    }
    private static byte[] remaining(FrameOffsetAudioStream stream)throws IOException{
        var output=new ByteArrayOutputStream();for(int i=0;i<100000;i++){var block=stream.read(4099);if(!block.hasRemaining())return output.toByteArray();byte[] bytes=new byte[block.remaining()];block.get(bytes);output.writeBytes(bytes);}throw new AssertionError("Unbounded PCM stream");
    }
    private interface Attempt{void run()throws IOException;}
    private static void rejects(Attempt action,String message)throws IOException{boolean rejected=false;try{action.run();}catch(IOException expected){rejected=true;}require(rejected,message);}
    public static void main(String[] args)throws Exception{
        for(int chunk:new int[]{4,28,1024,16384})for(int offset:new int[]{0,1,255,256,4095,4096,4097,19999}){
            var source=new Source(20000,chunk);
            try(var stream=new FrameOffsetAudioStream(source,()->false,20000)){
                stream.prepare(offset,System.nanoTime()+5_000_000_000L);
                require(stream.skippedFrames()==offset,"Skipped frame count");
                byte[] result=remaining(stream);
                require(Arrays.equals(result,Arrays.copyOfRange(source.bytes,offset*4,source.bytes.length)),"Offset leaked/skipped/duplicated PCM suffix");
                require(stream.emittedFrames()==20000-offset,"Emitted frame count");
                require(stream.decoderEof(),"Genuine complete EOF not observed");
            }
            require(source.closes==1,"Source close was not exactly once");
        }
        var atEnd=new Source(16,28);try(var stream=new FrameOffsetAudioStream(atEnd,()->false,16)){rejects(()->stream.prepare(16,System.nanoTime()+5_000_000_000L),"Offset at EOF accepted");}
        require(atEnd.closes==1,"EOF preparation leaked source");
        for(int actual:new int[]{15,17}){
            var source=new Source(actual,28);try(var stream=new FrameOffsetAudioStream(source,()->false,16)){rejects(()->{stream.prepare(1,System.nanoTime()+5_000_000_000L);remaining(stream);},"Wrong pinned decoded length accepted");}
            require(source.closes==1,"Length mismatch leaked source");
        }
        var cancelled=new AtomicBoolean(true);var cancelSource=new Source(16,28);
        try(var stream=new FrameOffsetAudioStream(cancelSource,cancelled::get,16)){rejects(()->stream.prepare(1,System.nanoTime()+5_000_000_000L),"Cancelled preparation accepted");}
        require(cancelSource.closes==1,"Cancelled preparation leaked source");
        var partial=new Source(16,5);try(var stream=new FrameOffsetAudioStream(partial,()->false,16)){rejects(()->stream.prepare(1,System.nanoTime()+5_000_000_000L),"Partial interleaved frame accepted");}
        require(partial.closes==1,"Partial-frame failure leaked source");
        var expired=new Source(16,28);try(var stream=new FrameOffsetAudioStream(expired,()->false,16)){rejects(()->stream.prepare(1,System.nanoTime()-1),"Expired preparation accepted");}
        require(expired.closes==1,"Deadline failure leaked source");
        var repeat=new Source(16,28);try(var stream=new FrameOffsetAudioStream(repeat,()->false,16)){stream.prepare(1,System.nanoTime()+5_000_000_000L);rejects(()->stream.prepare(1,System.nanoTime()+5_000_000_000L),"Repeated preparation accepted");}
        require(repeat.closes==1,"Repeated preparation leaked source");
        System.out.println("SHOW_AUDIO_STREAM_SMOKE_OK: "+checks+" assertions; PCM suffix, block boundaries, pinned length, cancellation, deadline and close ownership. NOT native decoder/playback acceptance.");
    }
}
