package com.coraline.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The branch's environment belongs only to its recipient, never global world time. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class AcceptedButtonEyesClient {
    private static CompoundTag data=new CompoundTag();private static int age=120;private static float stage;
    public static void clear(){data=new CompoundTag();age=120;stage=0;}
    public static void receive(CompoundTag n){
        TraditionalClient.clear();FirstEscapeClient.clear();ParentRescueClient.clear();RescueClient.clear();
        data=n.copy();age=0;AcceptedSewingMusic.update();
        if(phase().startsWith("GHOST_")||phase().equals("ACCEPTED_BUTTON_EYES_MIRROR_PRISON")){
            var ghost=new CompoundTag();ghost.putString("Phase",phase().equals("GHOST_C")?"MIRROR_C":phase().equals("GHOST_D")?"MIRROR_D":"MIRROR_E");ghost.putString("Sound",n.getString("Sound"));ghost.putInt("Ticks",n.getInt("Ticks"));ghost.putBoolean("ClockRunning",n.getBoolean("ClockRunning"));FirstEscapeClient.receive(ghost);
        }
    }
    public static boolean active(){return age<120&&data.contains("Phase");}
    public static String phase(){return active()?data.getString("Phase"):"";}
    public static boolean house(){var mc=Minecraft.getInstance();return active()&&!GhostVictimClient.imprisoned()&&mc.level!=null&&com.coraline.otherworld.OtherWorld.isOther(mc.level)&&!phase().equals("MIRROR_IMPRISONMENT")&&!phase().startsWith("GHOST_")&&!phase().equals("ACCEPTED_BUTTON_EYES_MIRROR_PRISON");}
    public static float stage(){return house()?stage:0;}
    public static long housePosition(){return data.getLong("House");}
    public static float radius(){float t=stage();float[] radii={Math.max(260,data.getInt("RealmRadius")),180,130,90,55,0};int low=Math.min(4,(int)t);return net.minecraft.util.Mth.lerp(t-low,radii[low],radii[low+1]);}
    public static float musicGain(){return house()?Math.max(.75F,1-stage()*.05F):1;}
    public static boolean beldam(int id){return active()&&data.getBoolean("Beldam")&&data.getInt("Mother")==id;}
    public static boolean mother(int id){return active()&&data.getInt("Mother")==id;}
    public static boolean buttonEyes(){return active()&&data.getBoolean("ButtonEyes");}
    public static float opacity(){if(GhostVictimClient.ghost())return GhostVictimClient.opacity();return buttonEyes()?Math.max(.28F,1-stage*.144F):1;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.isPaused())return;
        if(mc.player==null){data=new CompoundTag();age=120;stage=0;return;}age++;float goal=active()?(data.contains("StageFloat")?data.getFloat("StageFloat"):data.getInt("Stage")):0;stage+=Math.max(-.01F,Math.min(.01F,goal-stage));
    }
    @SubscribeEvent public static void blackout(RenderGuiEvent.Post e){float amount=phase().equals("BLACKOUT")?1:phase().equals("PROCEDURE_COMPLETE")?Math.max(0,1-(data.getInt("Age")+age)/15F):0;if(amount<=0)return;var mc=Minecraft.getInstance();e.getGuiGraphics().fill(0,0,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),Math.round(amount*255)<<24);}
    @SubscribeEvent public static void emptyWorld(net.minecraftforge.client.event.RenderLivingEvent.Pre<?,?> e){if(!house()||e.getEntity() instanceof net.minecraft.world.entity.player.Player||e.getEntity().getId()==data.getInt("Mother")||e.getEntity().getId()==GhostVictimClient.victim())return;int threshold=2+Math.floorMod(e.getEntity().getUUID().hashCode(),3);if(stage()>=threshold)e.setCanceled(true);}
    @SubscribeEvent public static void seat(net.minecraftforge.client.event.MovementInputUpdateEvent e){var phase=phase();if(phase.equals("APPROACH_CHAIR")||phase.equals("TAKE_BUTTONS")||phase.equals("TAKE_NEEDLE")||phase.equals("RAISE_NEEDLE")||phase.equals("BLACKOUT")||phase.equals("PROCEDURE_COMPLETE")){e.getInput().shiftKeyDown=false;e.getEntity().setShiftKeyDown(false);}}
    private AcceptedButtonEyesClient(){}
}
