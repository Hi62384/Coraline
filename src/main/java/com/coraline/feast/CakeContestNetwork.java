package com.coraline.feast;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** The client displays the recipe; the server alone owns the clock and scoring. */
public final class CakeContestNetwork {
    public record State(int stage,int seconds,int step,int score,int mistakes,int age,int target,boolean open){}
    public record Choice(int value){}
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath("coraline","cake_contest"),()->"1","1"::equals,"1"::equals);
    private CakeContestNetwork(){}
    public static void register(){
        CHANNEL.messageBuilder(State.class,0,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{b.writeVarInt(m.stage);b.writeVarInt(m.seconds);b.writeVarInt(m.step);b.writeVarInt(m.score);b.writeVarInt(m.mistakes);b.writeVarInt(m.age);b.writeVarInt(m.target);b.writeBoolean(m.open);})
            .decoder(b->new State(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean()))
            .consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.CakeContestScreen.receive(m))).add();
        CHANNEL.messageBuilder(Choice.class,1,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->b.writeVarInt(m.value)).decoder(b->new Choice(b.readVarInt()))
            .consumerMainThread((m,c)->{ServerPlayer p=c.get().getSender();if(p!=null)CakeContestActivity.action(p,m.value);}).add();
    }
    public static void send(ServerPlayer p,VisitData.Scene s,boolean open){
        var l=s.loop;long now=OtherMotherLoop.now(p);
        CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new State(l.cakeStage,(int)Math.max(0,(l.cakeBakeUntil-now+19)/20),
            l.cakeStep,l.cakeScore,l.cakeMistakes,(int)Math.max(0,now-l.cakeStepAt),CakeContestActivity.targetFor(l),open));
    }
    public static void choose(int value){CHANNEL.sendToServer(new Choice(value));}
}
