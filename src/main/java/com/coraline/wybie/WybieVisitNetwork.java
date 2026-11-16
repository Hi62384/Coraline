package com.coraline.wybie;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
public final class WybieVisitNetwork {
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("coraline","wybie_visit"),()->"1","1"::equals,"1"::equals);
    public record Effect(boolean camera){}
    public record Look(int actor,int ticks){}
    public static void register(){CHANNEL.messageBuilder(Effect.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((v,b)->b.writeBoolean(v.camera)).decoder(b->new Effect(b.readBoolean())).consumerMainThread((v,c)->com.coraline.client.WybieVisitClient.effect(v.camera)).add();CHANNEL.messageBuilder(Look.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder((v,b)->{b.writeInt(v.actor);b.writeInt(v.ticks);}).decoder(b->new Look(b.readInt(),b.readInt())).consumerMainThread((v,c)->com.coraline.client.WybieVisitClient.look(v.actor,v.ticks)).add();}
    public static void look(ServerPlayer p,int actor,int ticks){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Look(actor,ticks));}
    public static void flash(ServerPlayer p){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Effect(false));}
    public static void camera(ServerPlayer p){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Effect(true));}
    public static void finished(ServerPlayer p,String sound){var scene=com.coraline.feast.VisitData.get(p.server).scenes.get(p.getUUID());if(scene!=null&&scene.wybieActor!=null&&p.serverLevel().getEntity(scene.wybieActor) instanceof WybieVisitorEntity actor&&p.getUUID().equals(actor.owner))actor.voiceFinished(sound);}
}
