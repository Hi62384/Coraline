package com.coraline.feast;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Four continuous stage acts driven by the original recording's measured cursor. */
public final class TheatreOpeningChoreography {
    record StagePose(double x,double y,double z,float yaw,int pose){}
    /** Geometry is independent of world state so act boundaries can be regression-tested. */
    static StagePose position(int side,double fraction){
        if((side!=-1&&side!=1)||!Double.isFinite(fraction))throw new IllegalArgumentException("Invalid opening pose input");
        double u=ScriptedShowTimeline.clamp(fraction),x=side*2.6,z=0;int pose=10;float yaw=0;
        if(u>=.14&&u<.30){double q=(u-.14)/.16;x+=side*Math.sin(q*Math.PI*4)*.8;z=Math.sin(q*Math.PI*2)*.8;pose=13;}
        else if(u>=.30&&u<.52){double q=(u-.30)/.22,beat=Math.sin(q*Math.PI*4);x=side*(2.6-1.3*Math.sin(q*Math.PI*2)*Math.sin(q*Math.PI*2));z=side*.2*beat;boolean pushing=(q<.5)==(side<0);pose=pushing?11:12;yaw=side*65;}
        else if(u>=.52&&u<.82){double q=(u-.52)/.30,angle=q*Math.PI*4;x=side*2.6*Math.cos(angle);z=side*1.3*Math.sin(angle);pose=14;yaw=(float)(side*Math.sin(angle)*40);}
        else if(u>=.82){double q=ScriptedShowTimeline.smooth((u-.82)/.18);x=side*(2.6-1.5*q);pose=q>=.97?7:10;}
        double tilt=boatTilt(u),height=1.60+Math.sin(tilt)*x;
        return new StagePose(Math.cos(tilt)*x,height,-28+z,yaw,pose);
    }
    public static double boatTilt(double fraction){double u=ScriptedShowTimeline.clamp(fraction);double envelope=Math.sin(Math.PI*u);return envelope*(Math.sin(u*Math.PI*12)*.055+(u>.3&&u<.52?Math.sin((u-.3)/.22*Math.PI*4)*.075:0));}
    static void frame(ServerPlayer p,VisitData.Scene s,Mob spink,Mob forcible,int tick,int duration){
        if(duration<=0||!TheatreSite.inside(p,s))return;
        double u=ScriptedShowTimeline.clamp(tick/(double)duration);
        Vec3 center=Vec3.atBottomCenterOf(TheatreSite.center(p,s));
        int act=u<.14?0:u<.30?1:u<.52?2:u<.82?3:4;
        var scenery=TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.OCEAN_SET,TheatreSite.center(p,s).north(28),0);if(scenery!=null)scenery.frame(0,tick,duration);
        for(int side:new int[]{-1,1}){
            Mob actor=side<0?spink:forcible;if(actor==null||actor.level()!=p.level())continue;
            var point=position(side,u);
            double collapse=ScriptedShowTimeline.smooth((tick-Math.max(0,duration-200))/200.0);
            ShowActors.position(actor,center.add(point.x()+side*collapse*.8,point.y()-collapse*1.55,point.z()+Math.sin(tick*.2)*collapse*.20),point.yaw(),collapse>.05?12:point.pose(),tick,(float)-u);
            if(collapse>0&&tick%15==0)p.serverLevel().sendParticles(ParticleTypes.POOF,center.x+side*4,center.y+1,center.z-28,5,.7,.2,.5,.02);
        }
        if(tick%20==0)ShowStageLighting.theatre(p,s,act==4?5:act,tick);
        // Bounded visible flowers use existing item particles, leaving no world edits or loot.
        if(act==2&&tick%30==0){
            double angle=(u-.55)/.27*Math.PI*4;
            for(int side:new int[]{-1,1})p.serverLevel().sendParticles(
                new ItemParticleOption(ParticleTypes.ITEM,new ItemStack(side<0?Items.POPPY:Items.ALLIUM)),
                center.x+side*(4+Math.cos(angle)*2),center.y+3+Math.sin(angle)*.8,
                center.z-30,5,.45,.35,.45,.025);
        }
        if(act==3&&tick==duration)p.serverLevel().sendParticles(ParticleTypes.FIREWORK,
            center.x,center.y+4,center.z-28,12,3,.5,1,.02);
    }
    private TheatreOpeningChoreography(){}
}
