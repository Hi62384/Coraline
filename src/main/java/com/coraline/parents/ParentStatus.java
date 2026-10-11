package com.coraline.parents;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-authoritative access, mirrored only for the local client's tooltip. */
@Mod.EventBusSubscriber(modid="coraline")
public final class ParentStatus {
    public static boolean clientHasParents;
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("coraline","parent_status"),()->"1","1"::equals,"1"::equals);
    public record Status(boolean hasParents){}
    public static void register(){
        CHANNEL.messageBuilder(Status.class,0,NetworkDirection.PLAY_TO_CLIENT)
                .encoder((msg,buf)->buf.writeBoolean(msg.hasParents()))
                .decoder(buf->new Status(buf.readBoolean()))
                .consumerMainThread((msg,ctx)->clientHasParents=msg.hasParents()).add();
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||player.connection==null)return;
        if(player.tickCount%10!=0)return;
        boolean has=ParentAccess.family(player.server,player.getUUID())!=null;
        var tag=player.getPersistentData();
        if(!tag.contains("CoralineParentStatusSent")||tag.getBoolean("CoralineParentStatusSent")!=has||player.tickCount%100==0){
            CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Status(has));
            tag.putBoolean("CoralineParentStatusSent",has);
        }
    }
}
