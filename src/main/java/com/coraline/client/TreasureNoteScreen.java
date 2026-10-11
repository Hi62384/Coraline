package com.coraline.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class TreasureNoteScreen extends Screen {
    private final String clue;
    private TreasureNoteScreen(String title,String clue){super(Component.literal(title));this.clue=clue;}
    public static void receive(String title,String clue){var mc=Minecraft.getInstance();if(title.isEmpty()){if(mc.screen instanceof TreasureNoteScreen)mc.setScreen(null);}else mc.setScreen(new TreasureNoteScreen(title,clue));}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){addRenderableWidget(Button.builder(Component.literal("Keep exploring"),b->onClose()).bounds(width/2-70,height/2+63,140,20).build());}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);int left=width/2-156,top=height/2-90;g.fill(left,top,left+312,top+181,0xFBE5D6AE);g.fill(left+5,top+5,left+307,top+7,0xFF947544);g.drawCenteredString(font,title,width/2,top+17,0xFF594021);int y=top+43;for(var line:font.split(Component.literal(clue),280)){g.drawString(font,line,left+16,y,0xFF322415,false);y+=13;}g.drawCenteredString(font,"Use your Treasure Note to reread this clue.",width/2,top+135,0xFF685536);super.render(g,mx,my,partial);}
}
