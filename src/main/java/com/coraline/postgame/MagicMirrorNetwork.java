package com.coraline.postgame;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.*;

public final class MagicMirrorNetwork {
    private record Confirm(int menu,UUID quote){}
    private record Select(int menu,int quantity){}
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("coraline","magic_mirror"),()->"1","1"::equals,"1"::equals);
    public static void register(){CHANNEL.messageBuilder(Confirm.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeVarInt(m.menu);b.writeUUID(m.quote);}).decoder(b->new Confirm(b.readVarInt(),b.readUUID())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null&&p.containerMenu instanceof MagicMirrorMenu mirror&&mirror.containerId==m.menu)mirror.confirm(p,m.quote);}).add();
        CHANNEL.messageBuilder(Select.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeVarInt(m.menu);b.writeVarInt(m.quantity);}).decoder(b->new Select(b.readVarInt(),b.readVarInt())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null&&p.containerMenu instanceof MagicMirrorMenu mirror&&mirror.containerId==m.menu)mirror.selectQuantity(p,m.quantity);}).add();
    }
    public static void confirm(MagicMirrorMenu menu){CHANNEL.sendToServer(new Confirm(menu.containerId,menu.quoteNonce()));}
    public static void select(MagicMirrorMenu menu,int quantity){CHANNEL.sendToServer(new Select(menu.containerId,quantity));}
    private MagicMirrorNetwork(){}
}
