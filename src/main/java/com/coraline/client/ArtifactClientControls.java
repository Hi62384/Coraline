package com.coraline.client;

import com.coraline.clothing.ArtifactNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/** Client input is forwarded to the server-authoritative borrowed body. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class ArtifactClientControls {
    private static int soulMode;
    private static boolean dream,useWasDown,attackWasDown,swapWasDown;
    private static int dreamCamera=-1;
    private static float dreamYaw,dreamPitch;
    private static net.minecraft.client.CameraType previousCamera;
    private static void firstPerson(boolean active){
        var mc=Minecraft.getInstance();
        if(active){if(previousCamera==null)previousCamera=mc.options.getCameraType();mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);}
        else if(previousCamera!=null){mc.options.setCameraType(previousCamera);previousCamera=null;}
    }
    private static final boolean[] HOTBAR_WAS=new boolean[9];
    private static List<ItemStack> soulItems=new ArrayList<>();
    private static int selected;
    public static boolean remoteView(){return dream||soulMode>0;}
    /** Drain vanilla click queues without clearing held keys used by the remote controls. */
    public static void consumeBodyActions(){
        var mc=Minecraft.getInstance();
        if(mc.options.keyChat.consumeClick())mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen(""));
        for(var key:mc.options.keyMappings)while(key.consumeClick()){}
    }
    @SubscribeEvent public static void scroll(net.minecraftforge.client.event.InputEvent.MouseScrollingEvent event){
        if(!remoteView())return;
        event.setCanceled(true);
        var mc=Minecraft.getInstance();
        if(soulMode==2&&mc.screen==null&&mc.getCameraEntity() instanceof Mob mob){
            selected=Math.floorMod(selected-(int)Math.signum(event.getScrollDelta()),9);
            ArtifactNetwork.action(mob.getUUID(),20+selected);
        }
    }
    public static void soulMode(int value,int camera){
        var mc=Minecraft.getInstance();soulMode=value;dreamCamera=camera;firstPerson(value>0);
        var entity=mc.level==null?null:mc.level.getEntity(camera);
        if(entity!=null){dreamYaw=entity.getYRot();dreamPitch=entity.getXRot();mc.setCameraEntity(entity);}
        if(value==0){soulItems=List.of();if(mc.player!=null)mc.setCameraEntity(mc.player);}
        useWasDown=mc.options.keyUse.isDown();swapWasDown=mc.options.keySwapOffhand.isDown();mc.setScreen(null);
    }
    public static void hotbar(List<ItemStack> items,int slot){soulItems=List.copyOf(items);selected=slot;}
    public static void dreamMode(boolean value,int camera){
        var mc=Minecraft.getInstance();dream=value;dreamCamera=camera;firstPerson(value);
        if(mc.player!=null){dreamYaw=mc.player.getYRot();dreamPitch=mc.player.getXRot();if(!value)mc.setCameraEntity(mc.player);}
        useWasDown=mc.options.keyUse.isDown();mc.setScreen(null);
    }
    public static boolean turnDream(double dx,double dy){
        if(!dream&&soulMode==0)return false;
        dreamYaw+=(float)(dx*.15);dreamPitch=net.minecraft.util.Mth.clamp(dreamPitch+(float)(dy*.15),-90,90);return true;
    }
    @SubscribeEvent public static void camera(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles e){
        if(dream||soulMode>0){e.setYaw(dreamYaw);e.setPitch(dreamPitch);e.setRoll(0);}
    }
    @SubscribeEvent public static void hand(net.minecraftforge.client.event.RenderHandEvent e){if(dream||soulMode>0)e.setCanceled(true);}
    @SubscribeEvent public static void input(net.minecraftforge.client.event.MovementInputUpdateEvent e){
        if(!dream&&soulMode==0)return;
        var input=e.getInput();input.forwardImpulse=0;input.leftImpulse=0;input.jumping=false;input.shiftKeyDown=false;
        input.up=false;input.down=false;input.left=false;input.right=false;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.player==null){soulMode=0;dream=false;firstPerson(false);return;}
        boolean use=mc.options.keyUse.isDown(),attack=mc.options.keyAttack.isDown(),swap=mc.options.keySwapOffhand.isDown();
        if((soulMode>0||dream)&&mc.screen==null){
            var view=mc.level.getEntity(dreamCamera);
            if(view!=null&&mc.getCameraEntity()!=view)mc.setCameraEntity(view);
            firstPerson(true);
            if(view!=null&&soulMode==2){view.setYRot(dreamYaw);view.setXRot(dreamPitch);}
            if(dream&&use&&!useWasDown)ArtifactNetwork.exitDream();
            if(soulMode>0&&swap&&!swapWasDown)ArtifactNetwork.action(new UUID(0,0),5);
            if(soulMode==2){
                float forward=(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0);
                float strafe=(mc.options.keyLeft.isDown()?1:0)-(mc.options.keyRight.isDown()?1:0);
                ArtifactNetwork.move(strafe,forward,mc.options.keyJump.isDown(),mc.options.keyShift.isDown(),dreamYaw,dreamPitch);
                if(attack&&!attackWasDown){
                    if(mc.hitResult instanceof EntityHitResult hit)ArtifactNetwork.soulAttack(hit.getEntity().getId());
                    else if(mc.hitResult instanceof BlockHitResult hit)ArtifactNetwork.soulBlock(hit.getBlockPos(),hit.getDirection(),false);
                }
                if(use&&!useWasDown){
                    if(mc.hitResult instanceof BlockHitResult hit&&mc.getCameraEntity() instanceof Mob mob&&mob.getMainHandItem().getItem() instanceof BlockItem)
                        ArtifactNetwork.soulBlock(hit.getBlockPos(),hit.getDirection(),true);
                    else ArtifactNetwork.soulAbility(mc.hitResult instanceof EntityHitResult hit?hit.getEntity().getId():-1);
                }
                if(mc.getCameraEntity() instanceof Mob mob)for(int i=0;i<9;i++){
                    boolean down=mc.options.keyHotbarSlots[i].isDown();
                    if(down&&!HOTBAR_WAS[i])ArtifactNetwork.action(mob.getUUID(),20+i);
                    HOTBAR_WAS[i]=down;
                }
            }
            if(dream){
                var camera=mc.level.getEntity(dreamCamera);
                if(camera!=null&&mc.getCameraEntity()!=camera)mc.setCameraEntity(camera);
                float forward=(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0);
                float strafe=(mc.options.keyLeft.isDown()?1:0)-(mc.options.keyRight.isDown()?1:0);
                ArtifactNetwork.moveDream(strafe,forward,mc.options.keyJump.isDown(),mc.options.keyShift.isDown(),dreamYaw,dreamPitch);
            }
        }
        useWasDown=use;attackWasDown=attack;swapWasDown=swap;
    }
    @SubscribeEvent public static void overlay(RenderGuiEvent.Post event){
        var g=event.getGuiGraphics();int w=event.getWindow().getGuiScaledWidth(),h=event.getWindow().getGuiScaledHeight();
        if(soulMode>0){
            g.drawCenteredString(Minecraft.getInstance().font,soulMode==2?"SOUL CONTROL  •  F: return  •  right-click: ability":"SOUL VIEW  •  F: return",w/2,h-44,0xFFD9CEFA);
            if(soulMode==2)for(int i=0;i<9;i++){
                int x=w/2-91+i*20,y=h-24;
                g.fill(x,y,x+19,y+19,i==selected?0xCCB493E8:0xAA29253A);
                if(i<soulItems.size())g.renderItem(soulItems.get(i),x+1,y+1);
            }
        }
        if(!dream)return;
        long time=System.currentTimeMillis()/50;int shimmer=(int)(12+8*Math.sin(time*.07));
        g.fillGradient(0,0,w,h,(shimmer<<24)|0x5A4D98,0x446F8FC7);
        for(int i=0;i<20;i++){
            int x=Math.floorMod(i*137+(int)(time*(.12+i%3*.04)),w);
            int y=Math.floorMod(i*89-(int)(time*(.09+i%4*.03)),h);
            g.fill(x,y,x+2,y+2,0x669ADEF8);
        }
        g.drawCenteredString(Minecraft.getInstance().font,"DREAMING  •  Right-click to return",w/2,h-42,0xFFDDD8FF);
    }
}
