package com.coraline.client;
import com.coraline.parents.ParentsMenu;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ParentsScreen extends AbstractContainerScreen<ParentsMenu> {
    private static final String[] MOM=com.coraline.parents.ParentTasks.MOM_TASKS;
    private static final String[] DAD=com.coraline.parents.ParentTasks.DAD_TASKS;
    private int page;private boolean upgraded;
    public ParentsScreen(ParentsMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=220;imageHeight=252;}
    @Override protected void init(){
        super.init();
        upgraded=menu.rescued();
        for(int side=menu.isMom()?0:1;side<=(menu.isMom()?0:1);side++){
            String[] labels=side==0?MOM:DAD;
            int maximum=com.coraline.parents.ParentTasks.maximum(menu.isMom(),menu.rescued())+1;
            for(int i=page*6;i<Math.min(maximum,page*6+6);i++){final int id=side*100+i;
                addRenderableWidget(Button.builder(Component.literal(labels[i]),button->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id))
                        .bounds(leftPos+12,topPos+49+(i-page*6)*24,196,20).build());
            }
            if(maximum>6)addRenderableWidget(Button.builder(Component.literal("Tasks "+(page+1)+" / "+((maximum+5)/6)),b->{page=(page+1)%((maximum+5)/6);rebuildWidgets();}).bounds(leftPos+12,topPos+196,196,18).build());
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
        g.drawString(font,"Current: "+labels[selected],12,220,0x302635,false);
        g.drawString(font,menu.rescued()?"Rescued: faster, better deliveries":"Deliveries go to the bed chest.",12,234,0x504A43,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float delta){if(upgraded!=menu.rescued())rebuildWidgets();renderBackground(g);super.render(g,x,y,delta);}
}
