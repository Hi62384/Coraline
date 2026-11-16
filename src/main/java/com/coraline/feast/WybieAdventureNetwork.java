package com.coraline.feast;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
/** Owner-bound controls; no packet can set a score, a deadline, or a catch outcome. */
public final class WybieAdventureNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("coraline","wybie_adventure"),()->"1","1"::equals,"1"::equals);
    public record Input(int entity,UUID session,float forward,float turn,boolean brake){}
    public record Choice(UUID nonce,boolean finish){}
    public record Snapshot(CompoundTag data){}
    public record Debug(UUID nonce,String command,String value){}
    public static void register(){
        CHANNEL.messageBuilder(Input.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeInt(m.entity);b.writeUUID(m.session);b.writeFloat(m.forward);b.writeFloat(m.turn);b.writeBoolean(m.brake);}).decoder(b->new Input(b.readInt(),b.readUUID(),b.readFloat(),b.readFloat(),b.readBoolean())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null&&p.getVehicle() instanceof WybieBicycleEntity bike&&bike.getId()==m.entity)bike.input(p,m.session,m.forward,m.turn,m.brake);}).add();
        CHANNEL.messageBuilder(Choice.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeUUID(m.nonce);b.writeBoolean(m.finish);}).decoder(b->new Choice(b.readUUID(),b.readBoolean())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)WybieAdventure.choice(p,m.nonce,m.finish);}).add();
        CHANNEL.messageBuilder(Snapshot.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->b.writeNbt(m.data)).decoder(b->new Snapshot(b.readNbt())).consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.WybieAdventureClient.receive(m.data))).add();
        CHANNEL.messageBuilder(Debug.class,3,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeUUID(m.nonce);b.writeUtf(m.command,32);b.writeUtf(m.value,128);}).decoder(b->new Debug(b.readUUID(),b.readUtf(32),b.readUtf(128))).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)WybieAdventure.debugRequest(p,m.nonce,m.command,m.value);}).add();
    }
    public static void debug(UUID nonce,String command,String value){CHANNEL.sendToServer(new Debug(nonce,command,value));}
    public static void input(Input m){CHANNEL.sendToServer(m);}public static void choice(UUID nonce,boolean finish){CHANNEL.sendToServer(new Choice(nonce,finish));}
    public static void send(ServerPlayer p,CompoundTag n){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Snapshot(n));}
    private WybieAdventureNetwork(){}
}
