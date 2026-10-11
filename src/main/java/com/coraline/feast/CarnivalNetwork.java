package com.coraline.feast;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Server-authoritative carnival challenges; the client only paints the current challenge. */
public final class CarnivalNetwork {
    public record State(int booth,String title,int game,int difficulty,int tokens,int seconds,int progress,int required,int failures,int age,int target,int cycle,int windowStart,int windowEnd,boolean open){}
    public record Choice(int value){}
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath("coraline","carnival"),()->"1","1"::equals,"1"::equals);
    private CarnivalNetwork(){}
    public static void register(){
        CHANNEL.messageBuilder(State.class,0,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{b.writeVarInt(m.booth);b.writeUtf(m.title);b.writeVarInt(m.game);b.writeVarInt(m.difficulty);b.writeVarInt(m.tokens);
                b.writeVarInt(m.seconds);b.writeVarInt(m.progress);b.writeVarInt(m.required);b.writeVarInt(m.failures);b.writeVarInt(m.age);
                b.writeVarInt(m.target);b.writeVarInt(m.cycle);b.writeVarInt(m.windowStart);b.writeVarInt(m.windowEnd);b.writeBoolean(m.open);})
            .decoder(b->new State(b.readVarInt(),b.readUtf(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean()))
            .consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.CarnivalScreen.receive(m))).add();
        CHANNEL.messageBuilder(Choice.class,1,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->b.writeVarInt(m.value)).decoder(b->new Choice(b.readVarInt()))
            .consumerMainThread((m,c)->{ServerPlayer p=c.get().getSender();if(p!=null)CarnivalActivity.action(p,m.value);}).add();
    }
    public static void send(ServerPlayer p,VisitData.Scene s,boolean open){
        var l=s.loop;if(l.carnivalBooth<0){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new State(-1,"",0,0,l.carnivalTokensRemaining,0,0,0,0,0,0,1,0,0,false));return;}
        var booth=CarnivalActivity.Booth.values()[l.carnivalBooth];long now=OtherMotherLoop.now(p);
        CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new State(l.carnivalBooth,booth.title,booth.game.ordinal(),booth.difficulty,l.carnivalTokensRemaining,
            (int)Math.max(0,(l.carnivalGameUntil-now+19)/20),l.carnivalProgress,booth.steps,l.carnivalFailures,(int)Math.max(0,now-l.carnivalRoundAt),
            l.carnivalTarget,CarnivalActivity.cycle(booth),CarnivalActivity.windowStart(booth),CarnivalActivity.windowEnd(booth),open));
    }
    public static void choose(int value){CHANNEL.sendToServer(new Choice(value));}
}
