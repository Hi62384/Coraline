package com.coraline.client.audio;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

/** Read-only OpenAL observations. Every method is called on the owning sound thread. */
public final class StoryAudioMixerCursor {
    private final int source;
    private final ContinuousAudioStream stream;
    private long unqueuedFrames;
    public StoryAudioMixerCursor(int source,ContinuousAudioStream stream){this.source=source;this.stream=stream;}
    public void start(){if(AL10.alGetSourcei(source,AL10.AL_SOURCE_STATE)==AL10.AL_PLAYING)stream.playback().start();}
    public void cancel(){stream.playback().cancel();}
    public void beforeDestroy(){if(AL10.alGetSourcei(source,AL10.AL_SOURCE_STATE)!=AL10.AL_STOPPED)cancel();}
    public void released(){stream.playback().release();}
    public void sample(){stream.heardFrames(unqueuedFrames+Math.max(0,AL10.alGetSourcei(source,AL11.AL_SAMPLE_OFFSET)));}
    /** Called after vanilla unqueues, before it deletes those exact buffer IDs. */
    public void consumed(int[] buffers){
        if(stream.playback().cancelled())return;
        for(int buffer:buffers){int bytes=AL10.alGetBufferi(buffer,AL10.AL_SIZE),bits=AL10.alGetBufferi(buffer,AL10.AL_BITS),channels=AL10.alGetBufferi(buffer,AL10.AL_CHANNELS);if(bits>=8&&channels>0)unqueuedFrames+=bytes/(bits/8*channels);}
        stream.heardFrames(unqueuedFrames);
    }
}
