package com.coraline.client;

import com.coraline.feast.ShowPlaybackNetwork;
import com.coraline.feast.ShowSeekAvailability;
import net.minecraft.client.Minecraft;

/** Existing ordinary playback observations plus a separately gated, server-bound offset audition. */
public final class ShowPlaybackClient {
    private static ShowPlaybackNetwork.Bind bound;
    private static boolean started, ended, seeking;
    public static void bind(ShowPlaybackNetwork.Bind value) {
        ShowSeekClient.cancelAll();
        bound = value; started = false; ended = false; seeking = false;
    }
    public static void observed(String sound, boolean active) {
        if (bound == null || seeking || !bound.sound().equals(sound) || ended) return;
        if (active && !started) { started = true; ShowPlaybackNetwork.signal(bound.nonce(), sound, 0); }
        if (!active && started) { ended = true; ShowPlaybackNetwork.signal(bound.nonce(), sound, 1); }
    }
    /** Decoder boundaries plus consumed OpenAL frames, including a late adjacent binding. */
    public static void observedFrames(java.util.UUID nonce,String sound,boolean began,boolean complete,boolean failed){
        if(bound==null||seeking||!bound.nonce().equals(nonce)||!bound.sound().equals(sound)||ended)return;
        if(began&&!started){started=true;ShowPlaybackNetwork.signal(bound.nonce(),sound,0);}
        if(started&&complete){ended=true;ShowPlaybackNetwork.signal(bound.nonce(),sound,1);}
        else if(failed){ended=true;ShowPlaybackNetwork.signal(bound.nonce(),sound,2);}
    }
    public static ShowPlaybackNetwork.Bind seekSnapshot() { return bound; }
    /** Called only from the operator debugger. Server revalidates all authority and bounds. */
    public static boolean requestSeek(int targetTick) {
        if (!ShowSeekAvailability.productionReady() || bound == null || targetTick < 0 || targetTick >= bound.duration()) return false;
        ShowPlaybackNetwork.requestSeek(bound.nonce(), bound.serial(), bound.sound(), targetTick);
        return true;
    }
    public static void seek(ShowPlaybackNetwork.SeekStart value) {
        var player = Minecraft.getInstance().player;
        if (bound == null || player == null || !player.getUUID().equals(value.owner())
            || !bound.nonce().equals(value.previousNonce()) || !bound.owner().equals(value.owner())
            || !bound.realm().equals(value.realm()) || bound.serial() != value.serial()
            || !bound.sound().equals(value.sound()) || bound.music() != value.music()
            || bound.duration() != value.duration() || value.offsetTick() < 0 || value.offsetTick() >= value.duration()) return;
        seeking = true; started = false; ended = false;
        bound = new ShowPlaybackNetwork.Bind(value.nonce(), value.sound(), value.music(), value.owner(), value.realm(), value.serial(), value.duration());
        ShowSeekClient.start(value);
    }
    public static void clear() { ShowSeekClient.cancelAll(); bound = null; started = false; ended = false; seeking = false; }
    private ShowPlaybackClient() {}
}
