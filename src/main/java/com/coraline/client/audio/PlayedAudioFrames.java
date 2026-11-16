package com.coraline.client.audio;

/** Read-only mixer progress. Decoding ahead, wall time and cancellation never mean playback. */
public final class PlayedAudioFrames {
    private long frames;
    private boolean started, cancelled, released;
    public synchronized void start(){if(!cancelled)started=true;}
    public synchronized void sample(long consumed,long decoded){if(started&&!cancelled&&!released&&consumed>=0&&decoded>=0)frames=Math.max(frames,Math.min(consumed,decoded));}
    public synchronized void cancel(){cancelled=true;}
    public synchronized boolean started(){return started;}
    public synchronized long frames(){return frames;}
    public synchronized boolean cancelled(){return cancelled;}
    public synchronized void release(){released=true;}
    public synchronized boolean released(){return released;}
    public synchronized boolean reached(long boundary){return started&&boundary>=0&&frames>=boundary;}
}
