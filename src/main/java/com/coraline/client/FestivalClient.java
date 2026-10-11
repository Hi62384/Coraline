package com.coraline.client;

import com.coraline.feast.*;
import net.minecraft.client.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Rebindable four-lane highway. Music and notes run on the same synced score clock. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class FestivalClient {
    public static final KeyMapping[] KEYS={key(0,GLFW.GLFW_KEY_Z),key(1,GLFW.GLFW_KEY_X),key(2,GLFW.GLFW_KEY_C),key(3,GLFW.GLFW_KEY_V)};
    private static KeyMapping key(int n,int code){return new KeyMapping("key.coraline.festival_"+n,code,"key.categories.coraline_festival");}
    @Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {@SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){for(var key:KEYS)e.register(key);}}
    private static FestivalNetwork.State state;private static long received;private static final boolean[] down=new boolean[4];
    public static void receive(FestivalNetwork.State s){
        if(s.elapsed()==-999){state=null;return;}
        if(state==null||state.generation()!=s.generation()){java.util.Arrays.fill(down,false);Minecraft.getInstance().getMusicManager().stopPlaying();}
        state=s;received=System.nanoTime();
    }
    public static boolean active(){return state!=null;}
    @SubscribeEvent public static void hand(RenderHandEvent e){if(active())e.setCanceled(true);}
    private static double elapsed(){return state.elapsed()+Math.min(10,(System.nanoTime()-received)/50_000_000.0);}
    @SubscribeEvent public static void movement(MovementInputUpdateEvent e){if(!active())return;var i=e.getInput();i.forwardImpulse=0;i.leftImpulse=0;i.jumping=false;i.shiftKeyDown=false;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||!ActivityWorld.isActivity(mc.level)||(state!=null&&System.nanoTime()-received>5_000_000_000L)){state=null;return;}
        if(state==null)return;
        // Vanilla ambience may restart while a performance is in progress.
        mc.getMusicManager().stopPlaying();
        for(int lane=0;lane<4;lane++){boolean held=KEYS[lane].isDown();if(mc.screen==null&&held&&!down[lane])FestivalNetwork.press(state.generation(),lane);down[lane]=held;while(KEYS[lane].consumeClick()){} }
        // Only performed note-block voices are audible, never a vanilla backing track.

    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post e){
        if(state==null)return;var mc=Minecraft.getInstance();var g=e.getGuiGraphics();int w=e.getWindow().getGuiScaledWidth(),h=e.getWindow().getGuiScaledHeight();
        int x=w/2-64,top=h-154,line=h-53;double t=elapsed();int[] colors={0xFFFFB858,0xFF77E5C5,0xFFA897FF,0xFFFF8EBB};
        g.fill(x-5,top-18,x+133,line+24,0xAF111327);g.drawCenteredString(mc.font,FestivalChart.track(state.track()).title(),w/2,top-42,0xFFE0D5FF);
        int seconds=Math.max(0,(int)Math.ceil((FestivalChart.track(state.track()).duration()-Math.max(0,t))/20));
        g.drawCenteredString(mc.font,String.format("%d:%02d  Crowd %d  ×%d",seconds/60,seconds%60,state.crowd(),state.streak()),w/2,top-14,0xFFFFE9AD);
        for(int lane=0;lane<4;lane++){int lx=x+lane*32;g.fill(lx,top,lx+1,line,0x555A5172);g.fill(lx+3,line,lx+29,line+3,colors[lane]);g.drawCenteredString(mc.font,KEYS[lane].getTranslatedKeyMessage(),lx+16,line+8,colors[lane]);}
        var chart=state.chart();var resolved=state.resolved();for(int i=0;i<chart.length;i++){
            double approach=FestivalChart.approach(chart[i]/4,FestivalChart.track(state.track()).duration());double dt=chart[i]/4-t;if(dt>approach||dt< -FestivalChart.WINDOW||(i<resolved.length&&resolved[i]!=0))continue;
            int lx=x+chart[i]%4*32,y=(int)(line-dt/approach*(line-top));g.fill(lx+5,y-3,lx+27,y+3,colors[chart[i]%4]);
        }
        if(t<0)g.drawCenteredString(mc.font,"Ready  "+Math.max(1,(int)Math.ceil(-t/20)),w/2,top+30,0xFFFFFFFF);
        else g.drawCenteredString(mc.font,switch(state.grade()){case 1->"PERFECT";case 2->"GOOD";case 3->"MISS";default->"";},w/2,top-29,state.grade()==3?0xFFFF9898:0xFFAAFFDB);
    }
}
