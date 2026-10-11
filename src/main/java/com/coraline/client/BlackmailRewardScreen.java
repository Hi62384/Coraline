package com.coraline.client;

import com.coraline.secrets.SecretsNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;

/** Transaction choice only: never exposes journal collection or deposit controls. */
public final class BlackmailRewardScreen extends Screen {
    private final CompoundTag data;
    public BlackmailRewardScreen(CompoundTag data){super(Component.literal("Choose your payment"));this.data=data;}
    @Override protected void init(){
        int tier=data.getInt("Reward");String[] choices={(4+tier*6)+" emeralds",(1+tier*2)+" diamonds",(8+tier*8)+" gold ingots"};
        for(int i=0;i<3;i++){final int choice=i;addRenderableWidget(Button.builder(Component.literal(choices[i]),b->{SecretsNetwork.request(data.getUUID("Nonce"),"reward",Integer.toString(choice));onClose();}).bounds(width/2-90,height/2-16+i*25,180,20).build());}
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);g.fill(width/2-110,height/2-54,width/2+110,height/2+70,0xF02D2230);g.drawCenteredString(font,title,width/2,height/2-39,0xFFFFDBA0);super.render(g,mx,my,partial);}
}
