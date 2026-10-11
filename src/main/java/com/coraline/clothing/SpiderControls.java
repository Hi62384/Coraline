package com.coraline.clothing;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SpiderControls {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("coraline","spider_controls"),()->"1","1"::equals,"1"::equals);
    public record Input(boolean jump){}
    public static void register(){CHANNEL.messageBuilder(Input.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->b.writeBoolean(m.jump())).decoder(b->new Input(b.readBoolean())).consumerMainThread((m,c)->{
        var p=c.get().getSender();if(p==null||!SpiderArmor.superSet(p))return;
        p.getPersistentData().putBoolean("CoralineSpiderJump",m.jump());p.getPersistentData().putInt("CoralineSpiderInputTick",p.tickCount);
    }).add();}
    public static void send(boolean jump){CHANNEL.sendToServer(new Input(jump));}
}
