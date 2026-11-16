package com.coraline.client;

import com.coraline.entity.CharacterEntity;
import net.minecraft.client.model.geom.ModelPart;
import java.util.*;

/** Server-selected pose with render interpolation. No client state may advance event progress. */
public final class ShowActorPoseClient {
    public record Frame(int pose,int tick,float reveal,long received){}
    private static final Map<Integer,Frame> FRAMES=new HashMap<>();
    public static void accept(int entity,int pose,int tick,float reveal){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level!=null)FRAMES.put(entity,new Frame(pose,tick,reveal,mc.level.getGameTime()));}
    public static Frame frame(net.minecraft.world.entity.Entity actor){var f=FRAMES.get(actor.getId());return f!=null&&actor.level().getGameTime()-f.received<100?f:null;}
    public static void apply(CharacterEntity actor,Map<String,ModelPart> bones,float age){
        var f=frame(actor);if(f==null||f.pose==0)return;float t=f.tick+age-(int)age,wave=(float)Math.sin(t*.18);var body=bones.get("body");var head=bones.get("head");if(body==null||head==null)return;
        if(actor.characterId().equals("cat")&&f.pose>=30&&f.pose<=34){
            head.zRot=(float)Math.sin(t*.06)*.06F;bones.get("tail").yRot=(float)Math.sin(t*.085)*.20F;
            if(f.pose==30){body.y+=1.1F;body.xRot=-.15F;for(String side:List.of("right","left")){bones.get(side+"_leg").xRot=-1.0F;var paw=bones.get(side+"_leg_paw");if(paw!=null)paw.xRot=.8F;}head.xRot=-.12F;}
            if(f.pose==31){head.xRot-=.12F;head.zRot+=(float)Math.sin(t*.12)*.035F;}
            if(f.pose==32&&t%100>70)head.yRot+=(float)Math.sin((t%100-70)/30*Math.PI)*.65F;
            if(f.pose==33){body.y+=2;body.xRot=.12F;head.xRot=.45F;var arm=bones.get("right_arm");arm.xRot=-.85F-(float)Math.sin(t*.28)*.16F;var paw=bones.get("right_arm_paw");if(paw!=null)paw.xRot=.40F+(float)Math.sin(t*.28)*.12F;bones.get("left_arm").xRot=.15F;}
            if(f.pose==34){float flinch=(float)Math.sin(Math.min(1,t/20F)*Math.PI);body.y+=flinch;head.xRot-=flinch*.3F;head.zRot+=flinch*.16F;bones.get("tail").xRot-=flinch*.35F;}
            return;
        }
        for(String side:List.of("right","left")){var arm=bones.get(side+"_arm");var leg=bones.get(side+"_leg");var fore=bones.get(side+"_forearm");float sign=side.equals("right")?-1:1;if(arm==null||leg==null)continue;
            switch(f.pose){
                case 1->{arm.zRot=sign*(.9F+wave*.6F);leg.xRot=wave*.5F;body.zRot=wave*.08F;}
                case 2->{arm.zRot=sign*.65F;leg.xRot=-.25F*Math.abs(wave);if(fore!=null)fore.xRot=-.3F;}
                case 3->{arm.xRot=side.equals("right")?1.5F:-.3F;arm.zRot=sign*.35F;if(fore!=null)fore.xRot=-1.9F;head.xRot=.18F;}
                case 4->{arm.xRot=arm.yRot=arm.zRot=0;leg.xRot=0;head.xRot=head.yRot=0;body.xRot=body.yRot=body.zRot=0;}
                case 5->{arm.xRot=-(float)Math.PI;arm.yRot=arm.zRot=0;if(fore!=null)fore.xRot=fore.yRot=fore.zRot=0;leg.xRot=.12F;body.xRot=body.yRot=body.zRot=0;}
                case 6->{arm.xRot=-1.2F;arm.yRot=sign*.5F;if(fore!=null)fore.xRot=-.7F;head.xRot=.3F;}
                case 7->{arm.zRot=sign*(.8F+wave*.15F);body.xRot=.55F;head.xRot=-.35F;}
                case 8->{arm.xRot=-1.1F;arm.yRot=sign*(.4F+wave*.25F);if(fore!=null)fore.xRot=-1.1F;}
                case 10->{arm.xRot=-.55F;arm.zRot=-sign*(.4F+wave*.12F);if(fore!=null)fore.xRot=-.2F;leg.xRot=0;head.zRot=wave*.06F;}
                case 11->{arm.xRot=-1.4F;arm.yRot=sign*.1F;arm.zRot=-sign*.12F;if(fore!=null)fore.xRot=-.12F;leg.xRot=sign*wave*.18F;body.xRot=.10F;}
                case 12->{arm.xRot=-.55F;arm.zRot=-sign*.85F;if(fore!=null)fore.xRot=-.3F;body.xRot=-Math.abs(wave)*.13F;leg.xRot=sign*wave*.3F;head.zRot=-wave*.18F;}
                case 13->{arm.xRot=-1.05F;arm.zRot=-sign*.4F;if(fore!=null)fore.xRot=-.25F;leg.xRot=sign*wave*.28F;head.yRot=wave*.2F;}
                case 14->{arm.xRot=-.4F;arm.zRot=-sign*(.35F+Math.abs(wave)*.4F);leg.xRot=sign*wave*.32F;if(fore!=null)fore.xRot=-.3F;head.zRot=wave*.08F;}
                case 16->{arm.xRot=-.8F;arm.zRot=-sign*.45F;if(fore!=null)fore.xRot=-1.35F;leg.xRot=-1.15F;head.xRot=.25F;}
                case 17->{arm.xRot=-.65F;arm.zRot=-sign*.95F;arm.yRot=0;if(fore!=null)fore.xRot=-.18F;leg.xRot=leg.zRot=0;head.xRot=head.yRot=head.zRot=0;body.xRot=body.yRot=body.zRot=0;}
                case 80->{arm.xRot=-1.15F;arm.zRot=-sign*.35F;if(fore!=null)fore.xRot=-.8F+wave*.05F;leg.xRot=-1.0F;body.xRot=.12F;head.xRot=.35F;}
                case 81->{arm.xRot=-.8F;arm.zRot=-sign*(.55F+wave*.18F);if(fore!=null)fore.xRot=-.45F;head.zRot=wave*.12F;}
                case 82->{arm.xRot=side.equals("right")?-1.5F:-.5F;arm.yRot=sign*.28F;if(fore!=null)fore.xRot=-.25F;head.xRot=.16F;body.xRot=.14F;}
                case 83->{arm.xRot=-1.4F+wave*.16F;arm.yRot=sign*.5F;if(fore!=null)fore.xRot=-.2F-wave*.12F;body.zRot=wave*.12F;head.xRot=-.15F;}
                case 84->{body.xRot=1.35F;if(side.equals("right"))body.y+=8;arm.xRot=.4F;arm.zRot=sign*.5F;head.xRot=.2F;}
                case 85->{body.xRot=1.35F;if(side.equals("right"))body.y+=10;body.xScale=body.zScale=.72F;body.yScale=1.18F;body.zRot=wave*.14F;head.xRot=-1.1F;head.zRot=wave*.12F;arm.xRot=-.25F;arm.zRot=sign*(.18F+wave*.08F);if(fore!=null)fore.xRot=-1.5F;leg.xRot=-.2F+wave*.2F;leg.zRot=sign*.06F;var shin=bones.get(side+"_shin");if(shin!=null)shin.xRot=.4F+(float)Math.sin(t*.18-.8)*.2F;}
                case 86->{boolean holding=side.equals("right");body.zRot=(float)Math.sin(t*.05)*.18F;body.xRot=.12F;arm.xRot=holding?-3.05F:-.7F;arm.zRot=holding?.12F:sign*.65F;if(fore!=null)fore.xRot=holding?-.10F:-1.0F;leg.xRot=.25F+wave*.15F;leg.zRot=sign*.12F;var shin=bones.get(side+"_shin");if(shin!=null)shin.xRot=.4F+wave*.12F;head.zRot=-body.zRot;}
                default->{}
            }
        }
    }
    private ShowActorPoseClient(){}
}
