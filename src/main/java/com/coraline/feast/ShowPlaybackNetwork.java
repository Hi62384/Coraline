package com.coraline.feast;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Protocol 2: ordinary playback binding plus independently gated, nonce-bound operator audio seeks. */
@Mod.EventBusSubscriber(modid="coraline",bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ShowPlaybackNetwork {
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("coraline","show_playback"),()->"2","2"::equals,"2"::equals);
    public record Bind(UUID nonce,String sound,boolean music,UUID owner,UUID realm,int serial,int duration){}
    public record Signal(UUID nonce,String sound,int signal){}
    public record Pose(int entity,int pose,int tick,float reveal){}
    public record SeekRequest(UUID nonce,int serial,String sound,int targetTick){}
    public record SeekStart(UUID nonce,UUID previousNonce,UUID owner,UUID realm,int serial,String sound,boolean music,int offsetTick,int duration){}
    public record SeekSignal(UUID nonce,int serial,String sound,int status){}
    public record SeekCancel(UUID nonce){}
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event){
        CHANNEL.messageBuilder(Bind.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((v,b)->{b.writeUUID(v.nonce);b.writeUtf(v.sound,128);b.writeBoolean(v.music);b.writeUUID(v.owner);b.writeUUID(v.realm);b.writeInt(v.serial);b.writeInt(v.duration);}).decoder(b->new Bind(b.readUUID(),b.readUtf(128),b.readBoolean(),b.readUUID(),b.readUUID(),b.readInt(),b.readInt())).consumerMainThread((v,c)->com.coraline.client.ShowPlaybackClient.bind(v)).add();
        CHANNEL.messageBuilder(Signal.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((v,b)->{b.writeUUID(v.nonce);b.writeUtf(v.sound,128);b.writeInt(v.signal);}).decoder(b->new Signal(b.readUUID(),b.readUtf(128),b.readInt())).consumerMainThread((v,c)->{var p=c.get().getSender();if(p!=null&&v.signal>=0&&v.signal<=2)ShowPlayback.signal(p,v.nonce,v.sound,v.signal);}).add();
        CHANNEL.messageBuilder(Pose.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder((v,b)->{b.writeInt(v.entity);b.writeInt(v.pose);b.writeInt(v.tick);b.writeFloat(v.reveal);}).decoder(b->new Pose(b.readInt(),b.readInt(),b.readInt(),b.readFloat())).consumerMainThread((v,c)->com.coraline.client.ShowActorPoseClient.accept(v.entity,v.pose,v.tick,v.reveal)).add();
        CHANNEL.messageBuilder(SeekRequest.class,3,NetworkDirection.PLAY_TO_SERVER).encoder((v,b)->{b.writeUUID(v.nonce);b.writeInt(v.serial);b.writeUtf(v.sound,128);b.writeInt(v.targetTick);}).decoder(b->new SeekRequest(b.readUUID(),b.readInt(),b.readUtf(128),b.readInt())).consumerMainThread((v,c)->{var p=c.get().getSender();if(p!=null)ShowAudioSeek.request(p,v.nonce,v.serial,v.sound,v.targetTick);}).add();
        CHANNEL.messageBuilder(SeekStart.class,4,NetworkDirection.PLAY_TO_CLIENT).encoder((v,b)->{b.writeUUID(v.nonce);b.writeUUID(v.previousNonce);b.writeUUID(v.owner);b.writeUUID(v.realm);b.writeInt(v.serial);b.writeUtf(v.sound,128);b.writeBoolean(v.music);b.writeInt(v.offsetTick);b.writeInt(v.duration);}).decoder(b->new SeekStart(b.readUUID(),b.readUUID(),b.readUUID(),b.readUUID(),b.readInt(),b.readUtf(128),b.readBoolean(),b.readInt(),b.readInt())).consumerMainThread((v,c)->com.coraline.client.ShowPlaybackClient.seek(v)).add();
        CHANNEL.messageBuilder(SeekSignal.class,5,NetworkDirection.PLAY_TO_SERVER).encoder((v,b)->{b.writeUUID(v.nonce);b.writeInt(v.serial);b.writeUtf(v.sound,128);b.writeInt(v.status);}).decoder(b->new SeekSignal(b.readUUID(),b.readInt(),b.readUtf(128),b.readInt())).consumerMainThread((v,c)->{var p=c.get().getSender();if(p!=null&&v.status>=0&&v.status<=2)ShowAudioSeek.signal(p,v.nonce,v.serial,v.sound,v.status);}).add();
        CHANNEL.messageBuilder(SeekCancel.class,6,NetworkDirection.PLAY_TO_CLIENT).encoder((v,b)->b.writeUUID(v.nonce)).decoder(b->new SeekCancel(b.readUUID())).consumerMainThread((v,c)->com.coraline.client.ShowSeekClient.cancel(v.nonce)).add();
    }
    public static void pose(net.minecraft.world.entity.Mob actor,int pose,int tick,float reveal){CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->actor),new Pose(actor.getId(),pose,tick,reveal));}
    public static void bind(ServerPlayer p,UUID nonce,String sound,boolean music,UUID owner,UUID realm,int serial,int duration){for(var recipient:com.coraline.multiplayer.StorySessionData.get(p.server).audience(p))CHANNEL.send(PacketDistributor.PLAYER.with(()->recipient),new Bind(nonce,sound,music,owner,realm,serial,duration));}
    public static void signal(UUID nonce,String sound,int signal){CHANNEL.sendToServer(new Signal(nonce,sound,signal));}
    public static void requestSeek(UUID nonce,int serial,String sound,int targetTick){CHANNEL.sendToServer(new SeekRequest(nonce,serial,sound,targetTick));}
    public static void seek(ServerPlayer p,SeekStart start){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),start);}
    public static void seekSignal(UUID nonce,int serial,String sound,int status){CHANNEL.sendToServer(new SeekSignal(nonce,serial,sound,status));}
    public static void cancelSeek(ServerPlayer p,UUID nonce){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new SeekCancel(nonce));}
    private ShowPlaybackNetwork(){}
}
