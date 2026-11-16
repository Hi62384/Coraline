import com.coraline.client.audio.*;
import com.mojang.blaze3d.audio.OggAudioStream;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.openal.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Real, silent unity-pitch OpenAL consumption of untouched originals; not Minecraft acceptance. */
public final class StoryAudioOpenALSmoke {
    private static int checks;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    private static ContinuousAudioStream open(Path root)throws Exception{
        var json=com.google.gson.JsonParser.parseString(Files.readString(root.resolve("sounds.json"))).getAsJsonObject();var sources=new ArrayList<AudioStream>();
        for(String name:List.of("reunion_a","reunion_b")){String path=json.getAsJsonObject(name).getAsJsonArray("sounds").get(0).getAsJsonObject().get("name").getAsString().replace("coraline:","");sources.add(new CloseOnceAudioStream(new OggAudioStream(Files.newInputStream(root.resolve("sounds/"+path+".ogg")))));}
        return new ContinuousAudioStream(sources);
    }
    private static boolean queue(int source,ContinuousAudioStream stream)throws Exception{
        var format=stream.getFormat();var data=stream.read((int)format.getSampleRate()*format.getFrameSize());if(!data.hasRemaining())return false;int buffer=AL10.alGenBuffers();AL10.alBufferData(buffer,format.getChannels()==1?AL10.AL_FORMAT_MONO16:AL10.AL_FORMAT_STEREO16,data,(int)format.getSampleRate());AL10.alSourceQueueBuffers(source,buffer);return true;
    }
    private static void process(int source,ContinuousAudioStream stream,StoryAudioMixerCursor cursor,boolean refill)throws Exception{
        cursor.sample();int count=AL10.alGetSourcei(source,AL10.AL_BUFFERS_PROCESSED);if(count>0){int[] buffers=new int[count];AL10.alSourceUnqueueBuffers(source,buffers);cursor.consumed(buffers);AL10.alDeleteBuffers(buffers);if(refill)for(int i=0;i<count;i++)queue(source,stream);}
        check(AL10.alGetError()==AL10.AL_NO_ERROR,"OpenAL buffer/cursor error");
    }
    private static void run(Path root,boolean cancel)throws Exception{
        int source=AL10.alGenSources();AL10.alSourcef(source,AL10.AL_GAIN,0);AL10.alSourcef(source,AL10.AL_PITCH,1);
        try(var stream=open(root)){
            var cursor=new StoryAudioMixerCursor(source,stream);for(int i=0;i<4;i++)queue(source,stream);
            check(!stream.segmentStarted(0),"Decoded preload fabricated a playback start");AL10.alSourcePlay(source);cursor.start();long deadline=System.nanoTime()+90_000_000_000L;boolean paused=false;
            while(System.nanoTime()<deadline){
                process(source,stream,cursor,true);
                if(cancel&&stream.playedSeconds()>1){
                    cursor.cancel();AL10.alSourceStop(source);long before=stream.playback().frames();process(source,stream,cursor,false);cursor.beforeDestroy();cursor.released();
                    check(stream.playback().frames()==before&&!stream.segmentEnded(0),"Stop counted queued unheard suffix");System.out.println("OPENAL_CANCEL_VERIFIED frames="+before);return;
                }
                if(!cancel&&!paused&&stream.playedSeconds()>2){
                    AL10.alSourcePause(source);cursor.sample();long before=stream.playback().frames();Thread.sleep(600);cursor.sample();check(stream.playback().frames()==before,"Pause advanced the mixer cursor");AL10.alSourcePlay(source);paused=true;System.out.println("OPENAL_PAUSE_VERIFIED frames="+before);
                }
                if(AL10.alGetSourcei(source,AL10.AL_SOURCE_STATE)==AL10.AL_STOPPED){
                    process(source,stream,cursor,false);cursor.beforeDestroy();cursor.released();check(!stream.playback().cancelled(),"Natural drain was treated as explicit cancellation");check(stream.segmentEnded(0)&&stream.segmentEnded(1),"Natural drain lost original clip-end receipts");check(Math.abs(stream.playedSeconds()-stream.endSeconds(1))<1e-9,"Mixer consumed count differs from original decoder end");System.out.println("OPENAL_ORIGINALS_COMPLETE frames="+stream.playback().frames()+" seconds="+stream.playedSeconds());return;
                }
                Thread.sleep(20);
            }
            throw new AssertionError("Original playback did not finish within finite diagnostic bound");
        }finally{AL10.alDeleteSources(source);}
    }
    public static void main(String[] args)throws Exception{
        long device=ALC10.alcOpenDevice((ByteBuffer)null);check(device!=0,"No native audio device");long context=ALC10.alcCreateContext(device,(IntBuffer)null);check(context!=0&&ALC10.alcMakeContextCurrent(context),"No native audio context");AL.createCapabilities(ALC.createCapabilities(device));
        try{run(Path.of(args[0]),false);run(Path.of(args[0]),true);System.out.println("STORY_AUDIO_OPENAL_OK checks="+checks+"; muted unity-pitch originals; NOT Minecraft mixin/scene or audible acceptance");}finally{ALC10.alcMakeContextCurrent(0);ALC10.alcDestroyContext(context);ALC10.alcCloseDevice(device);}
    }
}
