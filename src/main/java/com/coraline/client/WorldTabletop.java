package com.coraline.client;

import com.coraline.feast.*;
import com.coraline.secrets.SecretsNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Owner-private in-world tabletop controls; moves still use the server nonce/rules. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class WorldTabletop {
    private static CompoundTag packet;
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private static boolean pulling,flicking;
    private static int color,prop,gesture,held;
    public record Tile(float x,float z,float width,float depth,String label,String action) {
        public boolean contains(double px,double pz){return px>=x&&px<x+width&&pz>=z&&pz<z+depth;}
    }
    public static void receive(CompoundTag n){
        var mc=Minecraft.getInstance();if(n.getBoolean("Close")){packet=null;pulling=flicking=false;return;}
        if(packet!=null&&packet.hasUUID("Nonce")&&!packet.getUUID("Nonce").equals(n.getUUID("Nonce"))){pulling=flicking=false;held=0;}
        packet=n.copy();world=mc.level;
        if(mc.screen instanceof TabletopGameScreen)mc.setScreen(null);
    }
    public static CompoundTag visible(ScenePropEntity board,CompoundTag fallback){return matches(board)?packet.getCompound("Game"):fallback;}
    private static boolean matches(ScenePropEntity board){var mc=Minecraft.getInstance();return packet!=null&&world==mc.level&&mc.player!=null&&mc.player.getUUID().equals(board.owner)&&packet.hasUUID("Board")&&packet.getUUID("Board").equals(board.getUUID());}
    private static void send(String action){if(packet!=null&&packet.hasUUID("Nonce"))SecretsNetwork.request(packet.getUUID("Nonce"),"visit_choice",action);}
    private static List<Tile> tiles(CompoundTag n){
        var result=new ArrayList<Tile>();
        if(packet.getString("Phase").equals("GAME_NIGHT_CHOICE")){
            if(packet.getInt("GameReactionRemaining")==0){if(packet.getInt("Matches")<2)result.add(new Tile(-.85F,.45F,.75F,.25F,"Another game","another"));result.add(new Tile(.1F,.45F,.75F,.25F,"Finish tonight","finish"));}return result;
        }
        if(!packet.getString("Phase").equals("GAME_NIGHT_ACTIVE")||n.getBoolean("Done"))return result;
        switch(TabletopGames.game(n)){
            case BUTTON_CARDS->{
                int[] hand=n.getIntArray("Hand0");for(int i=0;i<hand.length;i++){float step=1.7F/Math.max(1,Math.min(10,hand.length));result.add(new Tile(-.85F+i%10*step,-.72F+i/10*.17F,step-.01F,.15F,new String[]{"1","2","3","4","Skip","Rev","+1","Wild"}[hand[i]%8],"card:"+i+":"+color));}
                result.add(new Tile(-.45F,-.15F,.32F,.45F,"Draw","draw"));
                for(int i=0;i<4;i++)result.add(new Tile(.5F,-.2F+i*.17F,.3F,.14F,new String[]{"Red","Blue","Gold","Green"}[i],"color:"+i));
            }
            case THREAD_FOUR->{for(int i=0;i<7;i++)result.add(new Tile(-.63F+i*.18F,.45F,.17F,.25F,""+(i+1),"column:"+i));}
            case WOBBLY_TOWER->{for(int row=0;row<10;row++)for(int piece=0;piece<3;piece++)result.add(new Tile(-.75F+piece*.5F,-.7F+row*.13F,.47F,.12F,""+(row+1),"pull_begin:"+row+":"+piece));}
            case MOUSEBOARD_DASH->{result.add(new Tile(-.85F,.48F,.75F,.24F,"Roll safely","roll:safe"));result.add(new Tile(.1F,.48F,.75F,.24F,"Take shortcut","roll:risky"));}
            case BUTTON_FLICK->result.add(new Tile(-.75F,-.75F,1.5F,1.5F,"Hold, aim, release","flick_begin"));
            case SILENT_CHARADES->{for(int i=0;i<6;i++)result.add(new Tile(-.85F+i%3*.57F,-.65F+i/3*.26F,.53F,.23F,TabletopGames.TOPICS[i],n.getInt("Actor")==0?"select:"+i:"guess:"+i));if(n.getInt("Actor")==0){for(int i=0;i<6;i++)result.add(new Tile(-.85F+i%3*.57F,-.06F+i/3*.26F,.53F,.23F,new String[]{"Flap","Cast","Tick-tock","Eat","Paws","Row"}[i],"gesture:"+i));result.add(new Tile(-.35F,.53F,.7F,.18F,"Act it out","clue:"+prop+":"+gesture));}}
        }return result;
    }
    private static Vec3 point(ScenePropEntity board){
        var mc=Minecraft.getInstance();var eye=mc.player.getEyePosition();var look=mc.player.getLookAngle();
        if(Math.abs(look.y)<.01)return null;double distance=(board.getY()+.1-eye.y)/look.y;
        return distance>0&&distance<=6?eye.add(look.scale(distance)).subtract(board.position()):null;
    }
    @SubscribeEvent public static void mouse(InputEvent.MouseButton.Pre e){
        var mc=Minecraft.getInstance();if(e.getButton()!=1||packet==null||mc.player==null||mc.screen!=null)return;
        if(e.getAction()==0&&(pulling||flicking)){
            if(pulling)send("pull_release");
            if(flicking&&mc.hitResult instanceof EntityHitResult hit&&hit.getEntity() instanceof ScenePropEntity board){var at=point(board);if(at!=null)send("flick:"+Math.max(-60,Math.min(60,Math.toDegrees(Math.atan2(at.x,at.z+.85))))+":"+Math.max(.1,Math.min(.9,held/45.0)));}
            pulling=flicking=false;e.setCanceled(true);return;
        }
        if(e.getAction()!=1||!(mc.hitResult instanceof EntityHitResult hit)||!(hit.getEntity() instanceof ScenePropEntity board)||!matches(board)||board.kind()!=ScenePropEntity.GAME_BOARD)return;
        var at=point(board);if(at==null)return;var rules=packet.getCompound("Game");
        if(!packet.getString("Phase").equals("GAME_NIGHT_CHOICE")&&rules.getInt("Turn")!=0&&TabletopGames.game(rules)!=TabletopGames.Game.SILENT_CHARADES)return;
        for(var tile:tiles(rules))if(tile.contains(at.x,at.z)){
            String action=tile.action;
            if(action.startsWith("color:"))color=Integer.parseInt(action.substring(6));
            else if(action.startsWith("select:"))prop=Integer.parseInt(action.substring(7));
            else if(action.startsWith("gesture:"))gesture=Integer.parseInt(action.substring(8));
            else if(action.equals("flick_begin")){flicking=true;held=0;}
            else{send(action);if(action.startsWith("pull_begin:")){pulling=true;held=0;}}
            e.setCanceled(true);return;
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.level!=world||mc.player==null){packet=null;pulling=flicking=false;}if(!mc.isPaused()&&(pulling||flicking))held++;}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){
        var mc=Minecraft.getInstance();if(packet==null||mc.player==null||mc.screen!=null||mc.options.hideGui)return;var n=packet.getCompound("Game");String text=TabletopGames.game(n).name().replace('_',' ')+" · "+(n.getInt("Turn")==0?"Your turn — right-click the table pieces":TabletopGames.participantName(n,n.getInt("Turn"))+"'s turn");
        if(pulling)text="Hold steady… release right-click near 2 seconds ("+held+" ticks)";if(flicking)text="Aim at the board and release · strength "+Math.min(90,held*100/45)+"%";
        e.getGuiGraphics().drawCenteredString(mc.font,text,mc.getWindow().getGuiScaledWidth()/2,mc.getWindow().getGuiScaledHeight()-64,0xFFF1DC);
    }
    public static void render(ScenePropEntity board,PoseStack pose,MultiBufferSource buffers,int light){
        if(!matches(board))return;var font=Minecraft.getInstance().font;
        for(var tile:tiles(packet.getCompound("Game"))){
            pose.pushPose();pose.translate(tile.x+tile.width/2,.13,tile.z+tile.depth/2);pose.mulPose(Axis.XP.rotationDegrees(90));pose.scale(.0045F,.0045F,.0045F);
            font.drawInBatch(tile.label,-font.width(tile.label)/2F,-4,0xFFF1DC,false,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0xAA302630,light);pose.popPose();
        }
    }
    private WorldTabletop(){}
}
