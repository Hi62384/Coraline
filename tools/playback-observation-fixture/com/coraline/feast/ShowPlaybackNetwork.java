package com.coraline.feast;
import java.util.*;
/** Unit-test transport only. Never packaged or used by Minecraft. */
public final class ShowPlaybackNetwork {
    public record Bind(UUID nonce,String sound,boolean music,UUID owner,UUID realm,int serial,int duration){}
    public record SeekStart(UUID nonce,UUID previousNonce,UUID owner,UUID realm,int serial,String sound,boolean music,int offsetTick,int duration){}
    public record Signal(UUID nonce,String sound,int kind){}
    public static final List<Signal> signals=new ArrayList<>();
    public static void signal(UUID nonce,String sound,int kind){signals.add(new Signal(nonce,sound,kind));}
    public static void requestSeek(UUID nonce,int serial,String sound,int target){}
}
