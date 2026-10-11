package com.coraline.client;

import com.coraline.clothing.ArtifactNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.UUID;

/** Commands stored souls even when unloaded; Release never depends on entity presence. */
public final class SoulCapturerScreen extends Screen {
    private static final String[] TARGETS={"Passive","Aggressive","Player"};
    private static final String[] MOVES={"Follow","Patrol","Stay"};
    private static final String[] TASKS={"Idle","Forage","Mine","Hunt","Scavenge"};
    private final List<ArtifactNetwork.SoulEntry> entries;
    private final int globalTarget,globalFollow;
    private int offset;
    private UUID pendingRelease;
    private boolean pendingAll;
    private SoulCapturerScreen(ArtifactNetwork.SoulMenu menu){super(Component.literal("Soul Capturer"));entries=menu.entries();globalTarget=menu.target();globalFollow=menu.follow();}
    public static void receive(ArtifactNetwork.SoulMenu menu){Minecraft.getInstance().setScreen(new SoulCapturerScreen(menu));}
    @Override protected void init(){
        int left=(width-470)/2,top=(height-270)/2;
        topButton(left+5,top+30,145,"All targets: "+TARGETS[Math.floorMod(globalTarget,3)],10);
        topButton(left+155,top+30,145,"All move: "+MOVES[Math.floorMod(globalFollow,3)],11);
        topButton(left+305,top+30,160,pendingAll?"Confirm release all":"Release all",12);
        for(int row=0;row<Math.min(3,entries.size()-offset);row++){
            var entry=entries.get(offset+row);int y=top+76+row*53,x=left+4;
            add(x,y,49,"View",entry,0,true);x+=52;
            add(x,y,57,"Control",entry,1,true);x+=60;
            add(x,y,47,"Items",entry,6,false);x+=50;
            add(x,y,66,"T: "+TARGETS[Math.floorMod(entry.target(),3)],entry,2,false);x+=69;
            add(x,y,63,"M: "+MOVES[Math.floorMod(entry.follow(),3)],entry,3,false);x+=66;
            add(x,y,75,TASKS[Math.floorMod(entry.task(),5)],entry,7,false);x+=78;
            add(x,y,68,entry.id().equals(pendingRelease)?"Confirm":"Release",entry,4,false);
        }
        addRenderableWidget(Button.builder(Component.literal("Close"),b->onClose()).bounds(left+190,top+244,90,20).build());
    }
    private void topButton(int x,int y,int w,String label,int action){
        addRenderableWidget(Button.builder(Component.literal(label),b->{
            if(action==12&&!pendingAll){pendingAll=true;rebuildWidgets();return;}
            ArtifactNetwork.action(new UUID(0,0),action);
        }).bounds(x,y,w,20).build());
    }
    private void add(int x,int y,int w,String label,ArtifactNetwork.SoulEntry entry,int action,boolean needsLoaded){
        var button=Button.builder(Component.literal(label),b->{
            if(action==4&&!entry.id().equals(pendingRelease)){pendingRelease=entry.id();rebuildWidgets();return;}
            ArtifactNetwork.action(entry.id(),action);
            if(action==0||action==1||action==6)onClose();
        }).bounds(x,y,w,18).build();
        button.active=!needsLoaded||entry.loaded();addRenderableWidget(button);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);int left=(width-470)/2,top=(height-270)/2;
        g.fill(left-5,top-5,left+475,top+270,0xEE191329);
        g.drawCenteredString(font,"CAPTURED SOULS  ("+entries.size()+" / 10)",width/2,top+8,0xFFE9D6FF);
        g.drawCenteredString(font,"Each soul has its own hotbar and task; global controls affect all",width/2,top+59,0xFFBDB5D4);
        if(entries.isEmpty())g.drawCenteredString(font,"No souls captured yet.",width/2,top+145,0xFFCBBFD8);
        for(int row=0;row<Math.min(3,entries.size()-offset);row++){
            var entry=entries.get(offset+row);int y=top+66+row*53;
            g.drawString(font,entry.name()+(entry.loaded()?"":" (unloaded or dead)"),left+8,y,entry.loaded()?0xFFEADFFF:0xFFAEA4B7);
        }
        if(entries.size()>3)g.drawCenteredString(font,(offset+1)+"–"+Math.min(offset+3,entries.size())+" / "+entries.size()+"  •  scroll",width/2,top+231,0xFFBDB5D4);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta){
        if(entries.size()<=3)return super.mouseScrolled(x,y,delta);
        offset=Math.max(0,Math.min(entries.size()-3,offset+(delta>0?-1:1)));rebuildWidgets();return true;
    }
    @Override public boolean isPauseScreen(){return false;}
}
