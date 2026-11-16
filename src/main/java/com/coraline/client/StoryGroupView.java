package com.coraline.client;

import java.util.*;
import com.coraline.multiplayer.StoryGroupNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Pairwise presentation has a short lease and never changes global player visibility. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class StoryGroupView {
    private static StoryGroupNetwork.View view;
    private static int age=100;
    public static void accept(StoryGroupNetwork.View next){view=next;age=0;}
    public static boolean guest(){return view!=null&&age<40&&view.guest();}
    public static boolean locked(){return guest()&&view.locked();}
    public static boolean pair(UUID a,UUID b){return view!=null&&age<40&&view.locked()&&!a.equals(b)&&view.hidden().contains(a)&&view.hidden().contains(b);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END){if(Minecraft.getInstance().level==null){view=null;age=100;}else if(age<100)age++;}}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){view=null;age=100;}
    @SubscribeEvent public static void input(MovementInputUpdateEvent e){if(locked()){var input=e.getInput();input.forwardImpulse=0;input.leftImpulse=0;input.jumping=false;input.shiftKeyDown=false;}}
    @SubscribeEvent public static void render(RenderPlayerEvent.Pre e){var local=Minecraft.getInstance().player;if(local!=null&&pair(local.getUUID(),e.getEntity().getUUID()))e.setCanceled(true);}
    private StoryGroupView(){}
}
