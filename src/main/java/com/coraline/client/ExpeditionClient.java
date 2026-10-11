package com.coraline.client;
import com.coraline.feast.*;
import com.coraline.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class ExpeditionClient {
    private static int dropLease,ticks;private static boolean digging;
    public static void mode(int mode){dropLease=mode==1?60:0;}
    public static void journal(ExpeditionNetwork.Journal data){Minecraft.getInstance().setScreen(new JournalScreen(data));}
    @SubscribeEvent public static void click(PlayerInteractEvent.LeftClickBlock e){if(e.getEntity().level().isClientSide&&e.getEntity().getMainHandItem().is(ModItems.ACTIVITY_MINING_PICK.get()))e.setCanceled(true);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null){dropLease=0;digging=false;return;}ticks++;
        if(dropLease>0){
            // Keep vanilla airborne acceleration, drag and steering. The lease
            // is only lifecycle state; never inject an extra movement impulse.
            dropLease--;
        }
        boolean held=mc.screen==null&&mc.options.keyAttack.isDown()&&mc.player.getMainHandItem().is(ModItems.ACTIVITY_MINING_PICK.get())&&mc.hitResult instanceof BlockHitResult;
        if(held&&ticks%4==0){ExpeditionNetwork.dig(((BlockHitResult)mc.hitResult).getBlockPos(),true);mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);}
        if(!held&&digging)ExpeditionNetwork.dig(BlockPos.ZERO,false);digging=held;
    }
    private static final class JournalScreen extends Screen {
        private final ExpeditionNetwork.Journal data;private int floor,page;private boolean rules;private int scroll,excluded;
        JournalScreen(ExpeditionNetwork.Journal data){super(Component.literal("Mansion Journal"));this.data=data;this.excluded=data.excluded();}
        @Override public boolean mouseClicked(double mx,double my,int button){
            if(button==0){int left=Math.max(8,width/2-235),w=Math.min(470,width-16);
                for(int wing=0;wing<2;wing++)for(int row=0;row<3;row++){
                    int id=floor*6+wing*3+row,x=left+8+wing*(w/2),y=58+(2-row)*25;
                    if(id<data.rooms().size()&&mx>=x&&mx<x+w/2-16&&my>=y-2&&my<y+21){
                        excluded^=1<<id;ExpeditionNetwork.markRoom(data.session(),id,(excluded&(1<<id))!=0);
                        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK,1));return true;
                    }
                }
            }
            return super.mouseClicked(mx,my,button);
        }
        @Override public boolean isPauseScreen(){return false;}
        @Override protected void init(){addRenderableWidget(Button.builder(Component.literal("How clues work"),b->{rules=!rules;scroll=0;}).bounds(width/2-65,8,130,20).build());addRenderableWidget(Button.builder(Component.literal("Change floor"),b->{floor=1-floor;}).bounds(width/2-150,height-28,96,20).build());addRenderableWidget(Button.builder(Component.literal("Next clue"),b->{page=(page+1)%Math.max(1,data.clues().size());scroll=0;}).bounds(width/2-48,height-28,96,20).build());addRenderableWidget(Button.builder(Component.literal("Close"),b->onClose()).bounds(width/2+54,height-28,96,20).build());}
        @Override public boolean mouseScrolled(double x,double y,double delta){scroll=Math.max(0,scroll+(delta<0?1:-1));return true;}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);int left=Math.max(8,width/2-235),w=Math.min(470,width-16);g.fill(left,8,left+w,height-34,0xF0D9CBAA);g.drawCenteredString(font,"MANSION • "+(floor==0?"Ground floor":"Upper floor"),width/2,32,0xFF332211);g.drawCenteredString(font,"Staircase / far end",width/2,44,0xFF222222);
            g.drawString(font,"WEST WING",left+8,44,0xFF463121,false);g.drawString(font,"EAST WING",left+w-74,44,0xFF463121,false);
            for(int row=2;row>=0;row--){int yy=58+(2-row)*25;for(int wing=0;wing<2;wing++){
                int id=floor*6+wing*3+row;if(id>=data.rooms().size())continue;
                int x=left+8+wing*(w/2),rw=w/2-16;boolean marked=(excluded&(1<<id))!=0,hover=mx>=x&&mx<x+rw&&my>=yy-2&&my<yy+21;
                g.fill(x,yy-2,x+rw,yy+21,marked?0xFFD2B2A0:hover?0xFFF4E8CE:0xFFE8DABD);
                g.drawString(font,marked?"X":"□",x+3,yy+5,marked?0xFF922F2F:0xFF776752,false);
                var lines=font.split(Component.literal(data.rooms().get(id)),rw-20);
                for(int i=0;i<Math.min(2,lines.size());i++)g.drawString(font,lines.get(i),x+16,yy+i*10,marked?0xFF765A4B:0xFF2D241C,false);
            }}
            g.drawCenteredString(font,"Click a room to mark / unmark it with X",width/2,135,0xFF222222);g.drawCenteredString(font,"Entrance / near end",width/2,146,0xFF222222);
            String clue=rules?"All clues describe the jackpot room, NOT your current room. ALLAY: follow its elimination rule. VINDICATOR: its one statement is false; eliminate rooms matching that statement. EVOKER: exactly one of its two statements is true; eliminate rooms where BOTH or NEITHER match. Each witness rules out only 2 or 3 of the 12 rooms. Collect all six clues to leave one answer. Inspect both floors. Scroll to read long clues.":data.clues().isEmpty()?"Read the marked lecterns or speak to the witnesses to collect clues.":"Clue "+(page+1)+" / "+data.clues().size()+": "+data.clues().get(page);int y=162,index=0;var wrapped=font.split(Component.literal(clue),w-20);scroll=Math.min(scroll,Math.max(0,wrapped.size()-Math.max(1,(height-202)/11)));for(var line:wrapped){if(index++<scroll)continue;if(y>height-40)break;g.drawString(font,line,left+10,y,0xFF261B15,false);y+=11;}super.render(g,mx,my,partial);
        }
    }
}
