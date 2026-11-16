package com.coraline.client.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;

/** Adjacent originals share an OpenAL source and PCM buffer, with zero inserted samples. */
public final class ContinuousAudioStream implements AudioStream {
    private final List<AudioStream> streams;
    private final AudioFormat format;
    private final long[] ends;
    private int index;
    private long frames;
    private ByteBuffer pending;
    private boolean closed;
    private volatile boolean failed;
    private final PlayedAudioFrames playback=new PlayedAudioFrames();
    public ContinuousAudioStream(List<AudioStream> sources)throws IOException{
        if(sources.isEmpty())throw new IOException("Empty story run");streams=List.copyOf(sources);format=streams.get(0).getFormat();ends=new long[streams.size()];Arrays.fill(ends,-1);
        for(var stream:streams)if(!format.matches(stream.getFormat())){close();throw new IOException("Adjacent story recordings have different PCM formats");}
    }
    @Override public AudioFormat getFormat(){return format;}
    public synchronized double startSeconds(int segment){return segment==0?0:ends[segment-1]<0?-1:ends[segment-1]/(double)format.getSampleRate();}
    public synchronized double endSeconds(int segment){return ends[segment]<0?-1:ends[segment]/(double)format.getSampleRate();}
    public PlayedAudioFrames playback(){return playback;}
    public synchronized void heardFrames(long consumed){playback.sample(consumed,frames);}
    public synchronized boolean segmentStarted(int segment){return playback.reached(segment==0?0:ends[segment-1]);}
    public synchronized boolean segmentEnded(int segment){return playback.reached(ends[segment]);}
    public double playedSeconds(){return playback.started()?playback.frames()/(double)format.getSampleRate():-1;}
    public boolean failed(){return failed;}
    @Override public synchronized ByteBuffer read(int requested)throws IOException{
        try{return readFrames(requested);}catch(IOException|RuntimeException failure){failed=true;throw failure;}
    }
    private ByteBuffer readFrames(int requested)throws IOException{
        if(closed)throw new IOException("Story run closed");int frameSize=format.getFrameSize();int size=requested-requested%frameSize;if(size<=0)throw new IOException("PCM request is not a complete frame");
        ByteBuffer result=ByteBuffer.allocateDirect(size);
        while(result.hasRemaining()&&index<streams.size()){
            if(pending==null||!pending.hasRemaining()){
                pending=streams.get(index).read(Math.min(16384,result.remaining()));
                if(pending==null||!pending.hasRemaining()){ends[index]=frames;streams.get(index).close();index++;pending=null;continue;}
                if(pending.remaining()%frameSize!=0)throw new IOException("Invalid PCM decoder frame");
            }
            // STB may return a whole Vorbis packet, larger than our request.
            // Keep its unconsumed suffix; never drop or duplicate that boundary.
            int count=Math.min(result.remaining(),pending.remaining()),limit=pending.limit();
            pending.limit(pending.position()+count);result.put(pending);pending.limit(limit);frames+=count/frameSize;
        }
        result.flip();return result;
    }
    @Override public synchronized void close()throws IOException{if(closed)return;closed=true;IOException failure=null;for(var stream:streams)try{stream.close();}catch(IOException e){failure=e;}if(failure!=null)throw failure;}
}
