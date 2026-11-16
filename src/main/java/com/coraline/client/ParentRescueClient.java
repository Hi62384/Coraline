package com.coraline.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Owner-only scene state; expires on disconnect or missing updates. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class ParentRescueClient {
    private static CompoundTag state=new CompoundTag();private static int age=121;
    public static void clear(){state=new CompoundTag();age=121;}
    public static void receive(CompoundTag packet){state=packet.copy();age=0;}
    public static boolean active(){return age<120&&net.minecraft.client.Minecraft.getInstance().level!=null;}
    public static float colorBlend(float partial){
        if(!active())return 1;
        if(state.getBoolean("Evil"))return 1;
        if(!phase().equals("C"))return 0;
        int actual=VisitAudioClient.storyTicks(state.getString("Sound"));
        float clock=actual>=0?actual:state.getInt("Ticks")+age;
        float progress=Math.max(state.getFloat("Morph"),Math.min(1,(clock+partial)/Math.max(1,state.getInt("Duration"))));
        return progress*progress*(3-2*progress);
    }
    public static boolean colorOverride(){return active()&&!phase().equals("PARENT_RESCUE_KITCHEN_MOTHER_DISAPPEARED");}
    public static net.minecraft.core.BlockPos tunnelOrigin(){return active()&&state.contains("EscapeTunnel")?net.minecraft.core.BlockPos.of(state.getLong("EscapeTunnel")):null;}
    public static net.minecraft.core.Direction tunnelDirection(){var direction=net.minecraft.core.Direction.byName(state.getString("EscapeDirection"));return direction==null?net.minecraft.core.Direction.SOUTH:direction;}
    public static String phase(){return active()?state.getString("Phase"):"";}
    public static int mother(){return active()?state.getInt("Mother"):-1;}
    public static boolean crawl(){return phase().equals("A")||phase().equals("TUNNEL_EXIT");}
    public static float morph(int actor){return actor!=mother()?0:state.getBoolean("Evil")?1:state.getFloat("Morph");}
    public static boolean disguised(int actor){return actor==mother()&&morph(actor)<.55F;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase==TickEvent.Phase.END){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level==null){state=new CompoundTag();age=121;}else if(!mc.isPaused())age++;}}
    private ParentRescueClient(){}
}
