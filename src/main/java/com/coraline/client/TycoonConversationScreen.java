package com.coraline.client;
import com.coraline.tycoon.TycoonNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
/** Visitor dialogue deliberately never renders exact relationship scores. */
public final class TycoonConversationScreen extends Screen {
    private CompoundTag data;private int left,top,w,h,page;private boolean assigning;
    private TycoonConversationScreen(CompoundTag n){super(Component.literal(n.getString("VisitorName")));data=n;}
    public static void receive(CompoundTag n){var mc=Minecraft.getInstance();if(mc.screen instanceof TycoonConversationScreen s){s.data=n;s.rebuildWidgets();}else mc.setScreen(new TycoonConversationScreen(n));}
    @Override public boolean isPauseScreen(){return false;}
    private void button(String label,int x,int y,int width,Runnable run){addRenderableWidget(Button.builder(Component.literal(label),b->run.run()).bounds(x,y,width,20).build());}
    @Override protected void init(){
        w=Math.min(560,width-16);h=Math.min(320,height-16);left=(width-w)/2;top=(height-h)/2;
        button(assigning?"Back to conversation":"Send to Structure",left+12,top+94,(w-32)/2,()->{assigning=!assigning;page=0;rebuildWidgets();});
        button("Return to House",left+20+(w-32)/2,top+94,(w-32)/2,()->TycoonNetwork.action(data.getUUID("Nonce"),"talk","returnhouse:1"));
        if(assigning){var attractions=data.getList("Attractions",10);int rows=Math.max(1,(h-176)/24);page=Math.min(page,Math.max(0,(attractions.size()-1)/rows));
            for(int i=page*rows;i<Math.min(attractions.size(),(page+1)*rows);i++){var row=attractions.getCompound(i);var b=Button.builder(Component.literal(row.getString("Name")+" T"+row.getInt("Tier")+" — "+row.getInt("Occupants")+"/"+row.getInt("Capacity")+" — "+row.getInt("Distance")+"m"),unused->TycoonNetwork.action(data.getUUID("Nonce"),"talk","assign:"+row.getUUID("Id"))).bounds(left+12,top+120+(i%rows)*24,w-24,20).build();b.active=row.getBoolean("Available");addRenderableWidget(b);}
            if(attractions.size()>rows){button("<",left+12,top+h-29,30,()->{page=Math.max(0,page-1);rebuildWidgets();});button(">",left+48,top+h-29,30,()->{page=Math.min((attractions.size()-1)/rows,page+1);rebuildWidgets();});}
        }else{
            button(data.getBoolean("ProposalPending")?"Confirm: stay forever, with button eyes":"Would you like to stay here forever?",left+12,top+119,w-24,()->TycoonNetwork.action(data.getUUID("Nonce"),"talk",data.getBoolean("ProposalPending")?"proposal:confirm":"proposal:explain"));
            var choices=data.getList("Choices",10);int rows=Math.max(1,(h-185)/25);page=Math.min(page,Math.max(0,(choices.size()-1)/rows));
            for(int i=page*rows;i<Math.min(choices.size(),(page+1)*rows);i++){var choice=choices.getCompound(i);button(choice.getString("Text"),left+12,top+145+(i%rows)*25,w-24,()->TycoonNetwork.action(data.getUUID("Nonce"),"talk",data.getUUID("Question")+":"+choice.getInt("Id")));}
            if(choices.size()>rows){button("<",left+12,top+h-29,30,()->{page=Math.max(0,page-1);rebuildWidgets();});button(">",left+48,top+h-29,30,()->{page=Math.min((choices.size()-1)/rows,page+1);rebuildWidgets();});}
        }
        button("Goodbye",left+w-82,top+h-29,70,this::onClose);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);g.fill(left,top,left+w,top+h,0xF0181424);g.drawCenteredString(font,title,left+w/2,top+12,0xF1D9AC);String text=data.getString("Result").isBlank()?data.getString("Dialogue"):data.getString("Result");int y=top+36;for(var line:font.split(Component.literal(text),w-28)){if(y>top+86)break;g.drawString(font,line,left+14,y,0xD8CBDD);y+=12;}super.render(g,mx,my,partial);}
}
