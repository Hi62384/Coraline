package com.coraline.client;
import com.coraline.feast.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

/** Paged merchandise browser; all stock and purchases remain server-authoritative. */
public final class MallScreen extends Screen {
    private final CompoundTag data;private boolean sent;private int page;
    private MallScreen(CompoundTag data){super(Component.literal("Shopping Mall"));this.data=data;
        var list=data.getList("Products",10);
        for(int i=0;i<list.size();i++)if(list.getCompound(i).getInt("Index")==data.getInt("Selected"))page=i/4;
    }
    public static void receive(CompoundTag data){var mc=Minecraft.getInstance();if(data==null||!data.hasUUID("Nonce")){if(mc.screen instanceof MallScreen)mc.setScreen(null);return;}mc.setScreen(new MallScreen(data));}
    @Override public boolean isPauseScreen(){return false;}
    private CompoundTag product(){for(var t:data.getList("Products",10))if(((CompoundTag)t).getInt("Index")==data.getInt("Selected"))return (CompoundTag)t;return new CompoundTag();}
    private void request(int index,int action){if(sent)return;sent=true;ExpeditionNetwork.buy(data.getUUID("Nonce"),index,action);}
    private int panelWidth(){return Math.min(390,width-12);}
    private int left(){return (width-panelWidth())/2;}
    private int listWidth(){return panelWidth()/2;}
    @Override protected void init(){
        int left=left(),lw=listWidth();var products=data.getList("Products",10);
        for(int i=page*4;i<Math.min(products.size(),page*4+4);i++){
            var p=products.getCompound(i);var stack=ItemStack.of(p.getCompound("Item"));String price=" "+p.getInt("Price");
            String name=font.plainSubstrByWidth(stack.getHoverName().getString(),lw-30-font.width(price));
            addRenderableWidget(Button.builder(Component.literal(name+price),b->request(p.getInt("Index"),0)).bounds(left+25,57+(i%4)*23,lw-30,20).build());
        }
        int pages=(products.size()+3)/4;
        var prev=addRenderableWidget(Button.builder(Component.literal("<"),b->{page--;rebuildWidgets();}).bounds(left+10,153,24,18).build());prev.active=page>0;
        var next=addRenderableWidget(Button.builder(Component.literal(">"),b->{page++;rebuildWidgets();}).bounds(left+lw-34,153,24,18).build());next.active=page+1<pages;
        var item=product();boolean ok=item.getInt("Stock")>0&&data.getInt("Balance")>=item.getInt("Price");
        var buy=addRenderableWidget(Button.builder(Component.literal(data.getBoolean("Confirm")?"Confirm purchase":ok?"Buy — "+item.getInt("Price")+" tokens":item.getInt("Stock")<=0?"Sold out":"Not enough tokens"),b->request(data.getInt("Selected"),1)).bounds(left+8,height-31,panelWidth()-104,20).build());buy.active=ok;
        addRenderableWidget(Button.builder(Component.literal("Close"),b->onClose()).bounds(left+panelWidth()-90,height-31,82,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);int left=left(),lw=listWidth(),store=data.getInt("Store");
        g.fill(left,6,left+panelWidth(),height-6,0xF02D2338);
        g.drawCenteredString(font,MallStock.STORES[store],width/2,14,0xFFF1D493);
        g.drawCenteredString(font,MallStock.CATEGORIES[store],width/2,27,0xFFBEADD4);
        g.drawCenteredString(font,"Shopping Tokens: "+data.getInt("Balance")+" • This visit's selection",width/2,40,0xFFF8E6BA);
        var products=data.getList("Products",10);
        for(int i=page*4;i<Math.min(products.size(),page*4+4);i++)g.renderItem(ItemStack.of(products.getCompound(i).getCompound("Item")),left+7,59+(i%4)*23);
        g.drawCenteredString(font,(page+1)+" / "+((products.size()+3)/4),left+lw/2,157,0xFFEAE4F1);
        var p=product();var stack=ItemStack.of(p.getCompound("Item"));int detail=left+lw+8;
        g.renderItem(stack,detail,61);g.renderItemDecorations(font,stack,detail,61);
        g.drawString(font,p.getInt("Price")+" tokens • Stock: "+p.getInt("Stock"),detail,83,0xFFFFD966);
        int y=98;for(var line:font.split(Component.literal(stack.getHoverName().getString()+" ×"+stack.getCount()),panelWidth()-lw-18)){g.drawString(font,line,detail,y,0xFFFFFFFF,false);y+=10;}
        y+=6;for(var line:font.split(Component.literal(p.getString("Description")),panelWidth()-lw-18)){if(y>height-58)break;g.drawString(font,line,detail,y,0xFFEAE4F1,false);y+=10;}
        if(data.getBoolean("Confirm"))g.drawCenteredString(font,"Final purchase — no refunds",width/2,height-45,0xFFFFBB66);
        super.render(g,mx,my,partial);
    }
}
