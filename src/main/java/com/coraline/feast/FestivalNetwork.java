package com.coraline.feast;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** Rhythm input only; ownership, rewards and lifecycle remain in the activity ledger. */
public final class FestivalNetwork {
    public record State(int generation,int elapsed,int track,int crowd,int streak,int accuracy,int grade,int[] chart,int[] resolved){}
    public record Press(int generation,int lane){}
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("coraline","festival"),()->"2","2"::equals,"2"::equals);
    public static void register(){
        CHANNEL.messageBuilder(State.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeInt(m.generation);b.writeInt(m.elapsed);b.writeInt(m.track);b.writeInt(m.crowd);b.writeInt(m.streak);b.writeInt(m.accuracy);b.writeInt(m.grade);b.writeVarIntArray(m.chart);b.writeVarIntArray(m.resolved);})
            .decoder(b->new State(b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readVarIntArray(1024),b.readVarIntArray(1024)))
            .consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.FestivalClient.receive(m))).add();
        CHANNEL.messageBuilder(Press.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeInt(m.generation);b.writeInt(m.lane);}).decoder(b->new Press(b.readInt(),b.readInt()))
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)MusicFestivalActivity.press(p,m.generation,m.lane);}).add();
    }
    public static void send(ServerPlayer p,State state){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),state);}
    public static void clear(ServerPlayer p){send(p,new State(0,-999,0,0,0,0,0,new int[0],new int[0]));}
    public static void press(int generation,int lane){CHANNEL.sendToServer(new Press(generation,lane));}
    private FestivalNetwork(){}
}
