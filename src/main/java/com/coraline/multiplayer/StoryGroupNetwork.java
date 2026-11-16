package com.coraline.multiplayer;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Server-authored presentation only; it accepts no client membership or phase claims. */
@Mod.EventBusSubscriber(modid="coraline",bus=Mod.EventBusSubscriber.Bus.MOD)
public final class StoryGroupNetwork {
    private static final UUID NONE=new UUID(0,0);
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("coraline","story_group"),()->"1","1"::equals,"1"::equals);
    public record View(UUID session,UUID owner,long revision,boolean guest,boolean locked,List<UUID> hidden){}
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event){
        CHANNEL.messageBuilder(View.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((v,b)->{
            b.writeUUID(v.session);b.writeUUID(v.owner);b.writeLong(v.revision);b.writeBoolean(v.guest);b.writeBoolean(v.locked);b.writeVarInt(v.hidden.size());for(var id:v.hidden)b.writeUUID(id);
        }).decoder(b->{var session=b.readUUID();var owner=b.readUUID();long revision=b.readLong();boolean guest=b.readBoolean(),locked=b.readBoolean();int count=b.readVarInt();if(count<0||count>64)throw new IllegalArgumentException("Invalid story roster");var hidden=new ArrayList<UUID>();for(int i=0;i<count;i++)hidden.add(b.readUUID());return new View(session,owner,revision,guest,locked,List.copyOf(hidden));
        }).consumerMainThread((v,c)->com.coraline.client.StoryGroupView.accept(v)).add();
    }
    public static void send(ServerPlayer p,StorySessionData.Frame f,boolean locked,List<UUID> hidden){
        var roster=hidden.stream().distinct().limit(64).toList();
        CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new View(f==null?NONE:f.id(),f==null?NONE:f.owner(),f==null?0:f.revision(),f!=null&&!f.owner().equals(p.getUUID()),locked,roster));
    }
    private StoryGroupNetwork(){}
}
