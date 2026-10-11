package com.coraline.feast;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ExpeditionNetwork {
    public record MineMap(long seed,BlockPos center){}
    public record Dig(BlockPos pos,boolean held){}
    public record Mode(int mode){}
    public record Mine(int hearts){}
    public record EndStatus(int hearts,int room,int phase,BlockPos center,long elapsed){}
    public record Journal(UUID session,int excluded,List<String> rooms,List<String> clues){}
    public record RoomMark(UUID session,int room,boolean excluded){}
    public record Mall(net.minecraft.nbt.CompoundTag data){}
    public record Buy(UUID nonce,int index,int action){}
    public record Note(String title,String clue){}
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("coraline","expeditions"),()->"3","3"::equals,"3"::equals);
    public static void register(){
        CHANNEL.messageBuilder(RoomMark.class,9,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeUUID(m.session);b.writeInt(m.room);b.writeBoolean(m.excluded);}).decoder(b->new RoomMark(b.readUUID(),b.readInt(),b.readBoolean())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)MansionActivity.markRoom(p,m.session,m.room,m.excluded);}).add();
        CHANNEL.messageBuilder(MineMap.class,8,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeLong(m.seed);b.writeBlockPos(m.center);}).decoder(b->new MineMap(b.readLong(),b.readBlockPos())).consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.MineMapScreen.open(m.seed,m.center))).add();
        CHANNEL.messageBuilder(EndStatus.class,7,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeInt(m.hearts);b.writeInt(m.room);b.writeInt(m.phase);b.writeBlockPos(m.center);b.writeLong(m.elapsed);}).decoder(b->new EndStatus(b.readInt(),b.readInt(),b.readInt(),b.readBlockPos(),b.readLong())).consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.EndCityHud.receive(m))).add();
        CHANNEL.messageBuilder(Mine.class,6,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->b.writeInt(m.hearts)).decoder(b->new Mine(b.readInt())).consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.MineHud.receive(m.hearts))).add();
        CHANNEL.messageBuilder(Note.class,5,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeUtf(m.title,128);b.writeUtf(m.clue,2048);}).decoder(b->new Note(b.readUtf(128),b.readUtf(2048))).consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.TreasureNoteScreen.receive(m.title,m.clue))).add();
        CHANNEL.messageBuilder(Mall.class,3,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->b.writeNbt(m.data)).decoder(b->new Mall(b.readNbt())).consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.MallScreen.receive(m.data))).add();
        CHANNEL.messageBuilder(Buy.class,4,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeUUID(m.nonce);b.writeInt(m.index);b.writeInt(m.action);}).decoder(b->new Buy(b.readUUID(),b.readInt(),b.readInt())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)ShoppingMallActivity.request(p,m.nonce,m.index,m.action);}).add();
        CHANNEL.messageBuilder(Dig.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeBlockPos(m.pos);b.writeBoolean(m.held);}).decoder(b->new Dig(b.readBlockPos(),b.readBoolean())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)MineshaftActivity.hold(p,m.pos,m.held);}).add();
        CHANNEL.messageBuilder(Mode.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->b.writeInt(m.mode)).decoder(b->new Mode(b.readInt())).consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.ExpeditionClient.mode(m.mode))).add();
        CHANNEL.messageBuilder(Journal.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeUUID(m.session);b.writeInt(m.excluded);b.writeCollection(m.rooms,(buf,v)->buf.writeUtf(v,512));b.writeCollection(m.clues,(buf,v)->buf.writeUtf(v,1024));}).decoder(b->new Journal(b.readUUID(),b.readInt(),b.readList(buf->buf.readUtf(512)),b.readList(buf->buf.readUtf(1024)))).consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.ExpeditionClient.journal(m))).add();
    }
    public static void map(ServerPlayer p,long seed,BlockPos center){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new MineMap(seed,center));}
    public static void dig(BlockPos pos,boolean held){CHANNEL.sendToServer(new Dig(pos,held));}
    public static void note(ServerPlayer p,String title,String clue){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Note(title,clue));}
    public static void mall(ServerPlayer p,net.minecraft.nbt.CompoundTag data){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Mall(data));}
    public static void buy(UUID nonce,int index,int action){CHANNEL.sendToServer(new Buy(nonce,index,action));}
    public static void mode(ServerPlayer p,int mode){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Mode(mode));}
    public static void mine(ServerPlayer p,int hearts){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Mine(hearts));}
    static void end(ServerPlayer p,int hearts,int room,int phase,BlockPos center,long elapsed){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new EndStatus(hearts,room,phase,center,elapsed));}
    public static void journal(ServerPlayer p,UUID session,int excluded,List<String> rooms,List<String> clues){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Journal(session,excluded,List.copyOf(rooms),List.copyOf(clues)));}
    public static void markRoom(UUID session,int room,boolean excluded){CHANNEL.sendToServer(new RoomMark(session,room,excluded));}
    private ExpeditionNetwork(){}
}
