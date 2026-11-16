package com.coraline.client;
import com.coraline.postgame.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.*;
/** Quantity controls display server-authorized prices; confirmation uses its current nonce. */
public final class MagicMirrorScreen extends AbstractContainerScreen<MagicMirrorMenu> {
    private Button confirm;private Quantity slider;private final List<Button> presets=new ArrayList<>();private int pending,wait;
    public MagicMirrorScreen(MagicMirrorMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=224;imageHeight=232;inventoryLabelY=140;}
    @Override protected void init(){super.init();presets.clear();slider=addRenderableWidget(new Quantity(leftPos+38,topPos+34,174));
        int[] steps={1,8,16,32,-1};for(int i=0;i<steps.length;i++){final int quantity=steps[i];presets.add(addRenderableWidget(Button.builder(Component.literal(quantity<0?"Max":Integer.toString(quantity)),b->select(quantity<0?menu.maximum():quantity)).bounds(leftPos+12+i*41,topPos+56,38,18).build()));}
        confirm=addRenderableWidget(Button.builder(Component.translatable("gui.coraline.mirror.confirm"),b->MagicMirrorNetwork.confirm(menu)).bounds(leftPos+32,topPos+118,160,20).build());
    }
    private void select(int count){if(!menu.ready()||minecraft.gameMode==null)return;int wanted=Math.max(1,Math.min(menu.maximum(),count));if(wanted==pending)return;pending=wanted;wait=0;MagicMirrorNetwork.select(menu,wanted);}
    @Override protected void containerTick(){super.containerTick();if(pending>0&&(menu.quantity()==pending||++wait>30))pending=0;slider.sync();slider.active=menu.ready()&&menu.maximum()>1;
        int[] steps={1,8,16,32,-1};for(int i=0;i<presets.size();i++)presets.get(i).active=menu.ready()&&(steps[i]<0||steps[i]<=menu.maximum());
        confirm.active=pending==0&&menu.ready()&&(menu.status()==0||menu.status()==6);
    }
    private final class Quantity extends AbstractSliderButton {
        Quantity(int x,int y,int width){super(x,y,width,20,Component.empty(),0);sync();}
        void sync(){value=(Math.max(1,pending>0?pending:menu.quantity())-1)/(double)Math.max(1,menu.maximum()-1);updateMessage();}
        @Override protected void updateMessage(){setMessage(Component.literal("Quantity: "+(pending>0?pending:menu.quantity())+" / "+Math.max(1,menu.maximum())));}
        @Override protected void applyValue(){select(1+(int)Math.round(value*Math.max(0,menu.maximum()-1)));updateMessage();}
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        g.fill(leftPos-2,topPos-2,leftPos+imageWidth+2,topPos+imageHeight+2,0xffad915a);g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff211c32);
        g.fill(leftPos+8,topPos+78,leftPos+216,topPos+102,0xff342b46);
        for(var slot:menu.slots){int x=leftPos+slot.x,y=topPos+slot.y;g.fill(x-1,y-1,x+17,y+17,0xff887a99);g.fill(x,y,x+16,y+16,0xff3c354b);}
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        g.drawString(font,title,8,8,0xf3e4bd,false);var item=menu.outputPreview();String name=item.isEmpty()?"Select an inventory item":item.getHoverName().getString();g.drawString(font,font.plainSubstrByWidth(name,208),8,23,0xd5c9e4,false);
        g.drawString(font,"Total: "+menu.cost()+" Other Diamonds",14,81,0xf3e4bd,false);g.drawString(font,"+ 1 Moonstone per duplication",14,93,0xd5c9e4,false);
        g.drawCenteredString(font,pending>0?Component.literal("Updating price..."):Component.translatable("gui.coraline.mirror.status."+menu.status()),imageWidth/2,106,menu.status()==0||menu.status()==6?0x91e5c1:0xf0b397);
        g.drawString(font,playerInventoryTitle,31,140,0xd5c9e4,false);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){renderBackground(g);super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);}
}
