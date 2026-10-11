package com.coraline.client;

import com.coraline.feast.CakeContestNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Four alternating baking trials: recipe memory, mixing rhythm, oven timing and icing pattern. */
public final class CakeContestScreen extends Screen {
    private static final String[] INGREDIENTS={"FLOUR","SUGAR","EGG","MILK","COCOA"};
    private static final int[] ICING={0xFFB7E5,0xFF78BA,0xB7E8FF,0xF8E9A0,0xF8B7C5,0xA7DBBF,0xD1BAE8,0xF3C8A1,0xA8C5E3};
    private CakeContestNetwork.State state;
    private int localAge;
    private CakeContestScreen(CakeContestNetwork.State state){super(Component.literal("The Pink Palace Cake Contest"));this.state=state;this.localAge=state.age();}
    public static void receive(CakeContestNetwork.State state){
        var mc=Minecraft.getInstance();
        if(state.stage()!=3){if(mc.screen instanceof CakeContestScreen)mc.setScreen(null);return;}
        if(mc.screen instanceof CakeContestScreen screen){screen.state=state;screen.localAge=state.age();}
        else if(state.open())mc.setScreen(new CakeContestScreen(state));
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void tick(){localAge++;}
    private int left(){return (width-320)/2;}
    private int top(){return (height-206)/2;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);
        int x=left(),y=top(),type=state.step()%4;
        g.fill(x-4,y-4,x+324,y+210,0xFF211923);
        g.fill(x,y,x+320,y+28,0xFF8D3D68);
        g.drawCenteredString(font,"THE PINK PALACE BAKE-OFF",x+160,y+9,0xFFF9E7EE);
        g.drawString(font,"Time: "+state.seconds()+"s",x+13,y+37,0xFFFFFFFF);
        g.drawString(font,"Recipe step: "+Math.min(24,state.step()+1)+" / 24",x+118,y+37,0xFFFFFFFF);
        g.drawString(font,"Score: "+state.score(),x+13,y+53,0xFFEEDBA0);
        g.drawString(font,"Mistakes: "+state.mistakes(),x+205,y+53,0xFFF2A8AE);
        if(state.step()>=24){g.drawCenteredString(font,"Cake complete! Wait for the bell.",x+160,y+102,0xFFFFFFFF);return;}
        if(type==0){
            g.drawCenteredString(font,"Memorize the ingredient, then choose it!",x+160,y+74,0xFFFFFFFF);
            if(localAge<20)g.drawCenteredString(font,INGREDIENTS[state.target()%5],x+160,y+91,0xFFFCE3A3);
            else for(int i=0;i<5;i++)button(g,x+11+i*61,y+119,58,34,INGREDIENTS[i],mx,my);
        }else if(type==3){
            g.drawCenteredString(font,"Memorize the icing tile, then click it!",x+160,y+74,0xFFFFFFFF);
            for(int i=0;i<9;i++){
                int bx=x+114+(i%3)*31,by=y+91+(i/3)*29;
                g.fill(bx,by,bx+26,by+24,0xFF4B3041);
                g.fill(bx+2,by+2,bx+24,by+22,ICING[i]);
                if(localAge<20&&i==state.target())g.fill(bx-2,by-2,bx+28,by,0xFFFFFFFF);
            }
            g.drawCenteredString(font,localAge<20?"Watch the glowing tile":"Now choose the same tile",x+160,y+186,0xFFEEDBA0);
        }else{
            boolean mixing=type==1;int cycle=mixing?40:50,window=mixing?17:27;
            g.drawCenteredString(font,mixing?"Mix at the sweet spot!":"Take the cake out before it burns!",x+160,y+79,0xFFFFFFFF);
            g.fill(x+25,y+104,x+295,y+123,0xFF483442);
            g.fill(x+25+(int)(270.0*window/cycle),y+104,x+25+(int)(270.0*(window+7)/cycle),y+123,0xFF80DAAD);
            int needle=x+25+(int)(270.0*Math.floorMod(localAge,cycle)/cycle);
            g.fill(needle-2,y+99,needle+2,y+128,0xFFFFE9A6);
            button(g,x+90,y+148,140,32,mixing?"MIX NOW":"OPEN OVEN",mx,my);
        }
        super.render(g,mx,my,partial);
    }
    private void button(GuiGraphics g,int x,int y,int w,int h,String label,int mx,int my){
        boolean hover=mx>=x&&mx<x+w&&my>=y&&my<y+h;
        g.fill(x,y,x+w,y+h,hover?0xFFE7A6CA:0xFFC075A3);
        g.fill(x+2,y+2,x+w-2,y+h-2,hover?0xFF8B3D69:0xFF6C3455);
        g.drawCenteredString(font,label,x+w/2,y+h/2-4,0xFFFFFFFF);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0||state.step()>=24)return super.mouseClicked(mx,my,button);
        int x=left(),y=top(),type=state.step()%4;
        if(type==0&&localAge>=20){for(int i=0;i<5;i++)if(mx>=x+11+i*61&&mx<x+69+i*61&&my>=y+119&&my<y+153){CakeContestNetwork.choose(i);return true;}}
        else if(type==3&&localAge>=20){for(int i=0;i<9;i++){int bx=x+114+(i%3)*31,by=y+91+(i/3)*29;if(mx>=bx&&mx<bx+26&&my>=by&&my<by+24){CakeContestNetwork.choose(i);return true;}}}
        else if((type==1||type==2)&&mx>=x+90&&mx<x+230&&my>=y+148&&my<y+180){CakeContestNetwork.choose(0);return true;}
        return super.mouseClicked(mx,my,button);
    }
}
