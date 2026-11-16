package com.coraline.client;
import com.coraline.feast.WybieAdventureNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;
/** An additive section on the existing Secrets Debugger. The server rechecks operator access. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class WybieAdventureDebugClient {
    @SubscribeEvent public static void debugger(ScreenEvent.Init.Post e){var mc=Minecraft.getInstance();if(!(e.getScreen() instanceof SecretJournalScreen)||mc.player==null||!mc.player.hasPermissions(2))return;e.addListener(Button.builder(Component.literal("Wybie Adventure"),b->WybieAdventureNetwork.debug(new UUID(0,0),"open","")).bounds(Math.max(4,e.getScreen().width-130),4,126,20).build());}
    public static void receive(CompoundTag n){var mc=Minecraft.getInstance();if(mc.screen instanceof Panel p)p.update(n);else mc.setScreen(new Panel(n));}
    private static final class Panel extends Screen {
        private CompoundTag data;private int page,ticks;private EditBox value;
        private static final String[][] ACTIONS={
            {"Force start","force"},{"Front door","door"},{"Original introduction","intro"},{"Restore Wybie","actor"},{"Restore bicycles","bikes"},{"Garden start","garden"},{"Regenerate course","regenerate"},{"Mount my bike","mount_player"},{"Wybie mount","mount_wybie"},{"Lead course","lead"},{"Follow me","follow"},{"Wybie dismount","dismount"},{"Advance checkpoint","checkpoint"},{"Set current score","current"},{"Set best score","best"},{"Fail current run","failure"},{"Inspect reward tier","reward"},{"Nearby real loot","loot"},{"Real secret structure","structure"},{"Spawn test rat","rat"},{"Spawn test doll","doll"},{"Gesture 0–12","gesture"},{"Set remaining ticks","time"},{"Expire deadline","expire"},{"Trigger search","search"},{"Ordinary contact catch","catch_normal"},{"Forbidden contact catch","catch_forbidden"},{"Give evidence ID","evidence"},{"Permanent ban","ban"},{"Ban/status detail","status"},{"Reset ban (operator)","reset_ban"},{"Preview voice 0–50","voice"},{"Finish at home","complete"}};
        Panel(CompoundTag n){super(Component.literal("Wybie Adventure · Secrets Debugger"));data=n;}
        void update(CompoundTag n){data=n;}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void tick(){if(++ticks%20==0)WybieAdventureNetwork.debug(data.getUUID("Nonce"),"refresh","");}
        @Override protected void init(){int left=Math.max(4,width/2-170),top=Math.max(132,height/2-40);value=new EditBox(font,left,top-26,340,20,Component.literal("Argument: ticks, score, tier, voice or evidence ID"));value.setMaxLength(128);value.setValue("0");addRenderableWidget(value);
            for(int i=page*12;i<Math.min(ACTIONS.length,(page+1)*12);i++){var action=ACTIONS[i];int local=i-page*12;addRenderableWidget(Button.builder(Component.literal(action[0]),b->WybieAdventureNetwork.debug(data.getUUID("Nonce"),action[1],value.getValue())).bounds(left+local%2*172,top+local/2*22,168,20).build());}
            addRenderableWidget(Button.builder(Component.literal("Previous"),b->{page=Math.max(0,page-1);rebuildWidgets();}).bounds(left,top+136,108,20).build());addRenderableWidget(Button.builder(Component.literal("Next"),b->{page=Math.min((ACTIONS.length-1)/12,page+1);rebuildWidgets();}).bounds(left+116,top+136,108,20).build());addRenderableWidget(Button.builder(Component.literal("Close"),b->onClose()).bounds(left+232,top+136,108,20).build());
        }
        @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);int left=Math.max(4,width/2-170);var e=data.getCompound("Event");g.drawString(font,title,left,10,0xFFE8CF92);String[] lines={"Phase "+e.getString("Mode")+" · Remaining "+e.getInt("Remaining")+" ticks", "Score "+e.getInt("CurrentScore")+" · Best "+e.getInt("BestScore")+" · Section "+e.getInt("Checkpoint"),"Wybie "+data.getString("WybiePosition"),"Player bike: "+data.getString("PlayerBike"),"Wybie bike: "+data.getString("FriendBike"),"Search "+data.getString("Search")+" · Distance "+Math.round(data.getDouble("HomeDistance")),"Introduced "+data.getBoolean("Introduced")+" · Banned "+data.getBoolean("Banned"),"Hint "+e.getCompound("Companion").getString("LastHint")+" · Spy "+e.getCompound("Companion").getString("LastSpy")};for(int i=0;i<lines.length;i++)g.drawString(font,lines[i],left,26+i*10,0xFFEDE2DA);super.render(g,mx,my,partial);}
    }
    private WybieAdventureDebugClient(){}
}
