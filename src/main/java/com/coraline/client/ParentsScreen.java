package com.coraline.client;
import com.coraline.parents.ParentsMenu;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ParentsScreen extends AbstractContainerScreen<ParentsMenu> {
    private static final String[] MOM={"Rest","Garden","Groceries","Buy clothes","Mine wood","Mine stone"};
    private static final String[] DAD={"Rest","Make dinner","Mine iron","Mine coal"};
    public ParentsScreen(ParentsMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=220;imageHeight=224;}
    @Override protected void init(){
        super.init();
        for(int side=menu.isMom()?0:1;side<=(menu.isMom()?0:1);side++){
            String[] labels=side==0?MOM:DAD;
            for(int i=0;i<labels.length;i++){final int id=side*100+i;
                addRenderableWidget(Button.builder(Component.literal(labels[i]),button->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id))
                        .bounds(leftPos+12,topPos+49+i*24,196,20).build());
            }
        }
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF272435);
        g.fill(leftPos+3,topPos+3,leftPos+imageWidth-3,topPos+imageHeight-3,0xFFE3D8BC);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        g.drawString(font,title,12,10,0x302635,false);
        g.drawString(font,"Choose a task",12,30,0x302635,false);
        String[] labels=menu.isMom()?MOM:DAD;int selected=Math.min(labels.length-1,Math.max(0,menu.selected(menu.isMom())));
        g.drawString(font,"Current: "+labels[selected],12,200,0x302635,false);
        g.drawString(font,"Deliveries go to the bed chest.",12,212,0x504A43,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float delta){renderBackground(g);super.render(g,x,y,delta);}
}
