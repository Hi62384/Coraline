package com.coraline.otherworld;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Read-only presentation metadata, sent to each visitor; never changes portal state. */
@Mod.EventBusSubscriber(modid="coraline")
public final class RealmVisualState {
    public record View(BlockPos center,BlockPos porch,int radius,boolean tunnel,long whiteoutAge){}
    public static View client;
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath("coraline","realm_visuals"),()->"1","1"::equals,"1"::equals);
    public static void register(){
        CHANNEL.messageBuilder(View.class,0,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((v,b)->{b.writeBlockPos(v.center);b.writeBlockPos(v.porch);b.writeVarInt(v.radius);b.writeBoolean(v.tunnel);b.writeLong(v.whiteoutAge);})
            .decoder(b->new View(b.readBlockPos(),b.readBlockPos(),b.readVarInt(),b.readBoolean(),b.readLong()))
            .consumerMainThread((v,c)->client=v).add();
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||p.connection==null||!OtherWorld.isOther(p.level()))return;
        var n=p.getPersistentData();if(!n.hasUUID("CoralineVisitDoor"))return;
        var r=RealmData.get(p.server).realms.get(n.getUUID("CoralineVisitDoor"));if(r==null)return;
        if(p.tickCount%20!=0&&!n.contains("CoralineWhiteoutStart"))return;
        BlockPos center=r.sourceDoor==null?r.doorPos:r.sourceDoor.offset(r.dx,0,r.dz);
        long whiteoutAge=n.contains("CoralineWhiteoutStart")?p.server.overworld().getGameTime()-n.getLong("CoralineWhiteoutStart"):-1;
        CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new View(center,r.porch==null?r.doorPos:r.porch,r.radius,n.getBoolean("CoralineInTunnel"),whiteoutAge));
    }
}
