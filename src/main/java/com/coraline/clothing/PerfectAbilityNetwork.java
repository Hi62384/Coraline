package com.coraline.clothing;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.*;

public final class PerfectAbilityNetwork {
    private record Barrier(){}
    private record Power(java.util.UUID player,boolean active){}
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("coraline","perfect_power"),()->"1","1"::equals,"1"::equals);
    public static void register(){
        CHANNEL.messageBuilder(Barrier.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{}).decoder(b->new Barrier()).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)PerfectAbilities.barrier(p);}).add();
        CHANNEL.messageBuilder(Power.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeUUID(m.player);b.writeBoolean(m.active);}).decoder(b->new Power(b.readUUID(),b.readBoolean())).consumerMainThread((m,c)->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->com.coraline.client.clothing.PerfectPowerVisuals.set(m.player,m.active))).add();
    }
    static void power(net.minecraft.server.level.ServerPlayer p,boolean active){CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),new Power(p.getUUID(),active));}
    static void tracking(net.minecraft.server.level.ServerPlayer viewer,net.minecraft.server.level.ServerPlayer subject){CHANNEL.send(PacketDistributor.PLAYER.with(()->viewer),new Power(subject.getUUID(),PerfectPower.powered(subject)));}
    public static void barrier(){CHANNEL.sendToServer(new Barrier());}
    private PerfectAbilityNetwork(){}
}
