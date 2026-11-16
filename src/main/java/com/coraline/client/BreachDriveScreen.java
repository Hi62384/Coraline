package com.coraline.client;

import java.util.Locale;
import com.coraline.postgame.BreachDriveMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class BreachDriveScreen extends AbstractContainerScreen<BreachDriveMenu> {
    public BreachDriveScreen(BreachDriveMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=194;imageHeight=218;inventoryLabelX=16;inventoryLabelY=121;titleLabelX=10;titleLabelY=8;}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        g.fill(leftPos-2,topPos-2,leftPos+imageWidth+2,topPos+imageHeight+2,0xffa18655);g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xffe5deca);
        for(var slot:menu.slots){int x=leftPos+slot.x,y=topPos+slot.y;g.fill(x-1,y-1,x+17,y+17,0xff746d62);g.fill(x,y,x+16,y+16,0xffaaa391);}
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        super.renderLabels(g,mouseX,mouseY);
        g.drawString(font,String.format(Locale.ROOT,"Breach: %.3f / 2.000",menu.millionths()/1_000_000.0),10,25,0x34303c,false);
        g.drawString(font,menu.powered()?"Powered":"Unpowered",10,39,menu.powered()?0x2d6575:0x813b39,false);
        g.drawString(font,String.format(Locale.ROOT,"Fuel: %.2f game days",menu.fuelTicks()/24000.0),10,53,0x34303c,false);
        g.drawString(font,menu.avatarLocked()?"Growth capped at 2.0":String.format(Locale.ROOT,"Growth: +%.3f / day",menu.dailyMillionths()/1_000_000.0),10,67,0x34303c,false);
        g.drawString(font,menu.avatarLocked()?"Containment: Avatar locked":menu.enabled()?"Containment: available":"Containment: refuel Drive",10,81,0x34303c,false);
        g.drawString(font,"Moonstone",16,99,0x34303c,false);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){renderBackground(g);super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);}
}
