package com.coraline.postgame;

import java.util.UUID;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** Server-to-client appearance only; there is no client conversion command. */
public final class ColonyMobNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline","colony_mob_visuals"),()->"1","1"::equals,"1"::equals);
    private record Appearance(UUID entity,boolean infected){}
    public static void register(){CHANNEL.messageBuilder(Appearance.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeUUID(m.entity);b.writeBoolean(m.infected);}).decoder(b->new Appearance(b.readUUID(),b.readBoolean())).consumerMainThread((m,c)->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->com.coraline.client.ColonyMobVisuals.update(m.entity,m.infected))).add();}
    static void send(Mob mob,boolean infected){CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->mob),new Appearance(mob.getUUID(),infected));}
    static void send(ServerPlayer player,Mob mob,boolean infected){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Appearance(mob.getUUID(),infected));}
    private ColonyMobNetwork(){}
}
