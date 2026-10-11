package com.coraline.client;

import com.coraline.feast.CarnivalNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Painted one-token challenges: timing, tracking and short memory tests. */
public final class CarnivalScreen extends Screen {
    private static final String[] DUCKS={"SUN","MOON","STAR","CLOVER","BUTTON"};
    private static final int[] COLORS={0xFFE35A4F,0xFFFFB941,0xFF61B6DC,0xFF9A71C7,0xFFF57CAD,0xFF90D471,0xFFF1DB87,0xFF9BD7CA,0xFFEB8D68};
    private CarnivalNetwork.State state;
    private int localAge;
    private CarnivalScreen(CarnivalNetwork.State state){super(Component.literal(state.title()));this.state=state;localAge=state.age();}
    public static void receive(CarnivalNetwork.State state){
        var mc=Minecraft.getInstance();
        if(state.booth()<0){if(mc.screen instanceof CarnivalScreen)mc.setScreen(null);return;}
        if(mc.screen instanceof CarnivalScreen screen){screen.state=state;screen.localAge=state.age();}
        else if(state.open())mc.setScreen(new CarnivalScreen(state));
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void tick(){localAge++;}
    private int left(){return (width-360)/2;}
    private int top(){return (height-228)/2;}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        renderBackground(g);int x=left(),y=top(),type=state.game();
        g.fill(x-5,y-5,x+365,y+233,0xFFF6C253);
        g.fill(x,y,x+360,y+228,0xFF27192D);
        g.fill(x,y,x+360,y+30,0xFFB54949);
        g.drawCenteredString(font,"✦ "+state.title()+" ✦",x+180,y+10,0xFFFFEBB9);
        g.drawString(font,"Time "+state.seconds()+"s",x+15,y+40,0xFFFFFFFF);
        g.drawString(font,"Round "+(state.progress()+1)+" / "+state.required(),x+120,y+40,0xFFFFFFFF);
        g.drawString(font,"Tier "+state.difficulty(),x+280,y+40,0xFFFFCF7D);
        g.drawString(font,"Tokens left: "+state.tokens(),x+15,y+56,0xFFFFCF7D);
        g.drawString(font,"Misses: "+state.failures(),x+255,y+56,0xFFF6A5A5);
        if(type==0||type==3||type==5){
            String task=type==0?"Land the ring at the glowing mark!":type==3?"Strike at the peak of the meter!":"Stop the wheel in the gold sector!";
            g.drawCenteredString(font,task,x+180,y+88,0xFFFFFFFF);
            int cycle=Math.max(1,state.cycle());
            g.fill(x+30,y+120,x+330,y+146,0xFF483B46);
            int zone0=x+30+300*state.windowStart()/cycle,zone1=x+30+300*(state.windowEnd()+1)/cycle;
            g.fill(zone0,y+120,zone1,y+146,0xFFF8CB65);
            int needle=x+30+300*Math.floorMod(localAge,cycle)/cycle;
            g.fill(needle-2,y+114,needle+2,y+152,0xFFFFFFFF);
            button(g,x+105,y+171,150,34,type==0?"THROW RING":type==3?"STRIKE!":"STOP WHEEL",mx,my);
        }else if(type==1||type==4){
            boolean darts=type==1;
            g.drawCenteredString(font,darts?"Hit the moving balloon!":"Remember the lit mirror, then choose it!",x+180,y+81,0xFFFFFFFF);
            int active=darts?Math.floorMod(state.target()+localAge/8,9):state.target();
            for(int i=0;i<9;i++){
                int bx=x+132+(i%3)*35,by=y+95+(i/3)*34;
                g.fill(bx-2,by-2,bx+30,by+30,0xFF79617C);
                g.fill(bx,by,bx+28,by+28,COLORS[i]);
                if(i==active&&(darts||localAge<20))g.fill(bx+8,by+8,bx+20,by+20,0xFFFFFFFF);
            }
            if(!darts)g.drawCenteredString(font,localAge<20?"Watch the glowing mirror":"Choose the mirror you saw",x+180,y+208,0xFFFFCF7D);
        }else if(type==2){
            g.drawCenteredString(font,"Find the duck with the matching symbol!",x+180,y+82,0xFFFFFFFF);
            if(localAge<20)g.drawCenteredString(font,DUCKS[Math.floorMod(state.target(),5)],x+180,y+105,0xFFFFCF7D);
            else for(int i=0;i<5;i++)button(g,x+13+i*69,y+138,65,37,DUCKS[i],mx,my);
        }
        super.render(g,mx,my,delta);
    }
    private void button(GuiGraphics g,int x,int y,int w,int h,String title,int mx,int my){
        boolean hover=mx>=x&&mx<x+w&&my>=y&&my<y+h;
        g.fill(x,y,x+w,y+h,hover?0xFFFFD67B:0xFFDE9B53);
        g.fill(x+2,y+2,x+w-2,y+h-2,hover?0xFF884657:0xFF673B53);
        g.drawCenteredString(font,title,x+w/2,y+h/2-4,0xFFFFFFFF);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return super.mouseClicked(mx,my,button);
        int x=left(),y=top(),type=state.game();
        if((type==0||type==3||type==5)&&mx>=x+105&&mx<x+255&&my>=y+171&&my<y+205){CarnivalNetwork.choose(0);return true;}
        if(type==1||type==4){if(type==4&&localAge<20)return true;
            for(int i=0;i<9;i++){int bx=x+132+(i%3)*35,by=y+95+(i/3)*34;
                if(mx>=bx&&mx<bx+28&&my>=by&&my<by+28){CarnivalNetwork.choose(i);return true;}}
        }
        if(type==2&&localAge>=20)for(int i=0;i<5;i++)if(mx>=x+13+i*69&&mx<x+78+i*69&&my>=y+138&&my<y+175){CarnivalNetwork.choose(i);return true;}
        return super.mouseClicked(mx,my,button);
    }
}
