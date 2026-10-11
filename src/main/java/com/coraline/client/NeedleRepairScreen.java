package com.coraline.client;

import com.coraline.clothing.ArtifactNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Choose an actual inventory or armor slot; the server checks the item and cost again. */
public final class NeedleRepairScreen extends Screen {
    private final List<Integer> slots=new ArrayList<>();
    private int offset;
    private NeedleRepairScreen(){super(Component.literal("Beldam's Sewing Needle"));}
    public static void open(){Minecraft.getInstance().setScreen(new NeedleRepairScreen());}
    @Override protected void init(){
        slots.clear();var player=Minecraft.getInstance().player;
        if(player!=null)for(int i=0;i<player.getInventory().getContainerSize();i++){
            ItemStack stack=player.getInventory().getItem(i);
            if(stack.isDamageableItem()&&stack.isDamaged()&&!stack.is(com.coraline.clothing.UtilityItems.SEWING_NEEDLE.get()))slots.add(i);
        }
        int left=(width-260)/2,top=(height-220)/2;
        for(int row=0;row<Math.min(7,slots.size()-offset);row++){
            int slot=slots.get(offset+row),y=top+30+row*24;
            addRenderableWidget(Button.builder(Component.literal("Repair"),b->{ArtifactNetwork.repairNeedle(slot);onClose();}).bounds(left+180,y,70,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Close"),b->onClose()).bounds(left+85,top+195,90,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);int left=(width-260)/2,top=(height-220)/2;
        g.fill(left-6,top-6,left+266,top+222,0xEE171625);
        g.drawCenteredString(font,"Choose an item to repair",width/2,top+10,0xFFE1D7F0);
        var player=Minecraft.getInstance().player;
        if(slots.isEmpty())g.drawCenteredString(font,"No damaged items in your inventory.",width/2,top+90,0xFFC8BDD8);
        if(player!=null)for(int row=0;row<Math.min(7,slots.size()-offset);row++){
            ItemStack stack=player.getInventory().getItem(slots.get(offset+row));int y=top+30+row*24;
            g.renderItem(stack,left+4,y+2);
            g.drawString(font,font.plainSubstrByWidth(stack.getHoverName().getString(),145),left+25,y+1,0xFFE6DEF0);
            g.drawString(font,"Restores durability for 1 string",left+25,y+11,0xFFAAA0BD);
        }
        if(slots.size()>7)g.drawCenteredString(font,(offset+1)+"–"+Math.min(offset+7,slots.size())+" / "+slots.size()+"  •  scroll",width/2,top+185,0xFFBFB4CD);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta){
        if(slots.size()<=7)return super.mouseScrolled(x,y,delta);
        offset=Math.max(0,Math.min(slots.size()-7,offset+(delta>0?-1:1)));rebuildWidgets();return true;
    }
    @Override public boolean isPauseScreen(){return false;}
}
