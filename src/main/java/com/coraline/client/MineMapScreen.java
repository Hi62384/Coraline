package com.coraline.client;

import com.coraline.feast.MineMaze;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Read-only live floor plan. Opening it neither pauses the expedition nor reveals other floors. */
public final class MineMapScreen extends Screen {
    private final MineMaze.Layout maze;
    private final BlockPos center;
    private double scale,ox,oy;
    private MineMapScreen(long seed,BlockPos center){super(Component.literal("Mine map"));maze=MineMaze.generate(seed);this.center=center;}
    public static void open(long seed,BlockPos center){Minecraft.getInstance().setScreen(new MineMapScreen(seed,center));}
    @Override protected void init(){addRenderableWidget(Button.builder(Component.literal("Close"),b->onClose()).bounds(width/2-40,height-24,80,20).build());}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void tick(){if(!MineHud.active())onClose();}
    private int x(double v){return (int)Math.round(ox+v*scale);}
    private int y(double v){return (int)Math.round(oy+v*scale);}
    private void line(GuiGraphics g,BlockPos a,BlockPos b,int color){
        int ax=x(a.getX()),ay=y(a.getZ()),bx=x(b.getX()),by=y(b.getZ());
        g.fill(Math.min(ax,bx)-1,Math.min(ay,by)-1,Math.max(ax,bx)+2,Math.max(ay,by)+2,color);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);var p=minecraft.player;if(p==null)return;
        int floor=Math.max(0,Math.min(3,(int)Math.floor((center.getY()-p.getY()+7)/14))),size=7+2*floor;
        scale=Math.min((width-50.0)/(size*14),(height-90.0)/(size*14+28));ox=width/2.0;oy=42+(size*14)*scale;
        g.drawCenteredString(font,"MINE MAP — FLOOR "+(floor+1)+" / 4",width/2,10,0xFFFFFF);
        g.drawCenteredString(font,"Cyan: you   Green: exit   Gold: stairs   •   Time continues",width/2,24,0xCCCCCC);
        for(var edge:maze.edges()){
            if(edge.a().tier()==floor&&edge.b().tier()==floor)line(g,edge.a().pos(),edge.b().pos(),0xFFB5A187);
        }
        for(var cell:maze.cells())if(cell.tier()==floor){var at=cell.pos();int px=x(at.getX()),py=y(at.getZ());g.fill(px-2,py-2,px+3,py+3,0xFFE4D5BB);}
        for(var edge:maze.edges())if(edge.a().tier()!=edge.b().tier()){
            var cell=edge.a().tier()==floor?edge.a():edge.b().tier()==floor?edge.b():null;
            if(cell!=null){var at=cell==edge.a()?MineMaze.stairStart(edge):MineMaze.stairEnd(edge);line(g,cell.pos(),at,0xFFDDBB44);g.drawCenteredString(font,cell==edge.a()?"↓":"↑",x(at.getX()),y(at.getZ())-4,0xFFFF55);}
        }
        if(floor==0){line(g,new BlockPos(0,0,-14),new BlockPos(0,0,7),0xFF55FF88);g.drawCenteredString(font,"EXIT",x(0),y(7),0x55FF88);}
        int px=x(p.getX()-center.getX()-.5),py=y(p.getZ()-center.getZ()-.5);
        g.fill(px-3,py-3,px+4,py+4,0xFF003333);g.fill(px-2,py-2,px+3,py+3,0xFF55FFFF);
        double angle=Math.toRadians(p.getYRot());g.fill(px+(int)(-Math.sin(angle)*6)-1,py+(int)(Math.cos(angle)*6)-1,px+(int)(-Math.sin(angle)*6)+2,py+(int)(Math.cos(angle)*6)+2,0xFFFFFFFF);
        super.render(g,mx,my,partial);
    }
}
