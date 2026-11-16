package com.coraline.client;

import com.coraline.rebirth.RebirthNetwork;
import com.coraline.rebirth.StoryRun;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Local first-person atmosphere and two-stage consent, never a client-side story reset. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class RebirthClient {
    private static String phase="";
    private static long ticks,received;
    private static boolean quiet;
    public static void receive(CompoundTag n){
        if(n==null)return;var mc=Minecraft.getInstance();
        switch(n.getString("Mode")){
            case "explain","confirm"->mc.setScreen(new ConsentScreen(n));
            case "close"->{if(mc.screen instanceof ConsentScreen)mc.setScreen(null);}
            case "scene"->{phase=n.getString("Phase");ticks=n.getLong("Ticks");received=mc.level==null?0:mc.level.getGameTime();}
            case "clear"->{phase="";restoreSound();}
            default->{}
        }
    }
    private static void restoreSound(){if(!quiet)return;var mc=Minecraft.getInstance();for(var source:new net.minecraft.sounds.SoundSource[]{net.minecraft.sounds.SoundSource.AMBIENT,net.minecraft.sounds.SoundSource.MUSIC})mc.getSoundManager().updateSourceVolume(source,mc.options.getSoundSourceVolume(source));quiet=false;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null){phase="";restoreSound();return;}
        if(!phase.isEmpty()&&mc.level.getGameTime()-received>60){phase="";restoreSound();return;}
        if(!phase.isEmpty()){
            quiet=true;for(var source:new net.minecraft.sounds.SoundSource[]{net.minecraft.sounds.SoundSource.AMBIENT,net.minecraft.sounds.SoundSource.MUSIC})mc.getSoundManager().updateSourceVolume(source,mc.options.getSoundSourceVolume(source)*.08F);
        }else restoreSound();
    }
    @SubscribeEvent public static void overlay(RenderGuiEvent.Post e){
        var mc=Minecraft.getInstance();if(phase.isEmpty()||mc.player==null||mc.options.hideGui)return;
        int w=e.getWindow().getGuiScaledWidth(),h=e.getWindow().getGuiScaledHeight();var g=e.getGuiGraphics();
        double t=ticks+(mc.level==null?0:Math.min(20,mc.level.getGameTime()-received));
        int dim=phase.equals("REWINDING")?(int)(45+Math.sin(t*.065)*18):28;
        g.fill(0,0,w,h,(dim<<24)|0x110719);
        for(int i=0;i<9;i++){int inset=i*3;g.fill(inset,inset,w-inset,inset+2,0x160D0515);g.fill(inset,h-inset-2,w-inset,h-inset,0x160D0515);}
        if(phase.equals("REWINDING")){
            String[] memories={"The Little Door","The doll","Button eyes","The garden","The Mirror Prison","The ghost children","The Magic Well"};
            int i=Math.min(memories.length-1,(int)t/20);int alpha=(int)(Math.sin((t%20)*Math.PI/20)*160);
            g.drawCenteredString(mc.font,memories[i],w/2,h/3,(alpha<<24)|0xC5A7D6);
            // Short reversing stitch motifs, not another world/loaded duplicate scene.
            for(int q=0;q<18;q++){int x=(int)((q*37-t*1.4)%Math.max(1,w));int y=h/3+24+(q%3)*5;g.fill(x,y,x+12,y+1,0x667E6597);}
        }
        RenderSystem.enableBlend();
    }
    private static final class ConsentScreen extends Screen {
        private final java.util.UUID nonce;
        private final boolean second;
        private int x,y,w,h,held,pulse;
        private boolean holding;
        ConsentScreen(CompoundTag n){super(Component.literal("The Ritual of the Last Thread"));nonce=n.getUUID("Nonce");second=n.getString("Mode").equals("confirm");}
        @Override public boolean isPauseScreen(){return false;}
        @Override protected void init(){
            w=Math.min(390,width-24);h=Math.min(245,height-20);x=(width-w)/2;y=(height-h)/2;
            addRenderableWidget(Button.builder(Component.literal(second?"Cancel":"Leave the threads untouched"),b->onClose()).bounds(x+12,y+h-31,w-24,20).build());
            if(!second)addRenderableWidget(Button.builder(Component.literal("Begin the ritual"),b->RebirthNetwork.input(nonce,"begin")).bounds(x+12,y+h-57,w-24,20).build());
        }
        @Override public void onClose(){holding=false;RebirthNetwork.input(nonce,"cancel");super.onClose();}
        private boolean inside(double mx,double my){return second&&mx>=x+12&&mx<x+w-12&&my>=y+h-62&&my<y+h-38;}
        @Override public boolean mouseClicked(double mx,double my,int button){if(button==0&&inside(mx,my)){holding=true;held=pulse=0;RebirthNetwork.input(nonce,"hold");return true;}return super.mouseClicked(mx,my,button);}
        @Override public boolean mouseReleased(double mx,double my,int button){if(button==0&&holding){holding=false;held=0;RebirthNetwork.input(nonce,"release");return true;}return super.mouseReleased(mx,my,button);}
        @Override public void tick(){super.tick();if(holding){held++;if(++pulse%4==0)RebirthNetwork.input(nonce,"hold");}}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            renderBackground(g);g.fill(x-2,y-2,x+w+2,y+h+2,0xFF715676);g.fill(x,y,x+w,y+h,0xF514101A);
            g.drawCenteredString(font,title,width/2,y+14,0xFFE0C6EB);
            String text=second?"This will sacrifice both of your rescued parents and restart the entire Coraline storyline. Their current lives and your story progression will be lost.\n\nThis cannot be cancelled after commitment. Each life thread must then be severed by your own hand."
                :"The threads of this story have reached their end. To weave them again, two lives must be surrendered.\n\nYour parents have returned to you. They are loving, helpful, and safe.\n\nTheir lives can be sacrificed to undo the story. Your Void Hearts and permanent possessions remain; your main storyline and secrets do not.";
            g.drawWordWrap(font,Component.literal(text),x+15,y+42,w-30,0xFFD3C9D7);
            if(second){int top=y+h-62;g.fill(x+12,top,x+w-12,top+24,holding?0xFF683650:0xFF382737);g.fill(x+12,top+22,x+12+(int)((w-24)*Math.min(1,held/60F)),top+24,0xFFB68FCF);g.drawCenteredString(font,"Sacrifice my parents — hold 3 seconds",width/2,top+8,0xFFF0DCEB);}
            super.render(g,mx,my,partial);
        }
    }
    private RebirthClient(){}
}
