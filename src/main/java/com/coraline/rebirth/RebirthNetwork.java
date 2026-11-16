package com.coraline.rebirth;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

/** Nonces belong to a nearby owner's portrait session; client-supplied elapsed time is never trusted. */
public final class RebirthNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("coraline","last_thread"),()->"1","1"::equals,"1"::equals);
    public record View(CompoundTag data){}
    public record Input(UUID nonce,String action){}
    public static void register(){
        CHANNEL.messageBuilder(View.class,0,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->b.writeNbt(m.data)).decoder(b->new View(b.readNbt()))
            .consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.coraline.client.RebirthClient.receive(m.data))).add();
        CHANNEL.messageBuilder(Input.class,1,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->{b.writeUUID(m.nonce);b.writeUtf(m.action,24);}).decoder(b->new Input(b.readUUID(),b.readUtf(24)))
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)StoryRebirth.input(p,m.nonce,m.action);}).add();
    }
    public static void send(ServerPlayer p,CompoundTag n){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new View(n));}
    public static void input(UUID nonce,String action){CHANNEL.sendToServer(new Input(nonce,action));}
    private RebirthNetwork(){}
}
