package com.coraline.feast;

import com.coraline.otherworld.MouseEntity;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;
import java.util.*;

/** Reuses actual mouse models and costume variations, with deterministic show-only ownership. */
public final class MouseCircusChoreography {
    public static final int COUNT=48;
    private static void clock(ServerPlayer p,VisitData.Scene s,BlockPos c,int cue,int elapsed,int duration,double scale){var clock=TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.CIRCUS_CLOCK,c,0);if(clock!=null){clock.grabTarget(new Vec3(scale,0,0));clock.frame(cue,elapsed,duration);}}
    private static List<MouseEntity> mice(ServerLevel l,VisitData.Scene s,BlockPos c){return l.getEntitiesOfClass(MouseEntity.class,new AABB(c).inflate(60),e->s.door.equals(e.doorId)&&e.getPersistentData().hasUUID("ShowMouseOwner")&&s.owner.equals(e.getPersistentData().getUUID("ShowMouseOwner"))&&e.getPersistentData().getInt("ShowSerial")==s.loop.event.getInt("Serial"));}
    public static int count(ServerPlayer p,VisitData.Scene s,BlockPos center){var level=p.server.getLevel(ActivityWorld.KEY);return level==null?0:mice(level,s,center).size();}
    public static void reveal(ServerPlayer p,VisitData.Scene s,BlockPos c){revealScaled(p,s,c,1);}
    public static void revealScaled(ServerPlayer p,VisitData.Scene s,BlockPos c,double scale){
        if(!s.owner.equals(p.getUUID()))return;var l=p.serverLevel();var actors=mice(l,s,c);Set<Integer> present=new HashSet<>();for(var m:actors){int slot=m.getPersistentData().getInt("ShowSlot");if(!present.add(slot))m.discard();}
        for(int i=0;i<COUNT;i++)if(!present.contains(i)){var m=ModEntities.MOUSE.get().create(l);if(m==null)continue;m.circusStyle(i%6);m.doorId=s.door;m.setNoAi(true);m.setInvulnerable(true);m.setNoGravity(true);m.noPhysics=true;m.getPersistentData().putBoolean("CoralineDecoration",true);m.getPersistentData().putUUID("ShowMouseOwner",s.owner);m.getPersistentData().putInt("ShowSerial",s.loop.event.getInt("Serial"));m.getPersistentData().putInt("ShowSlot",i);double a=i*Math.PI*2/COUNT;m.setPos(c.getX()+.5+Math.cos(a)*16*scale,c.getY()+.15*scale,c.getZ()+.5+Math.sin(a)*16*scale);l.getChunkAt(m.blockPosition());if(l.addFreshEntity(m))ShowPlaybackNetwork.pose(m,9,0,(float)Math.max(.45,scale));}
        l.sendParticles(ParticleTypes.POOF,c.getX()+.5,c.getY()+1,c.getZ()+.5,36,12*scale,.5*scale,12*scale,.03);
    }
    public static void introduction(ServerPlayer p,VisitData.Scene s,BlockPos c,int elapsed){if(elapsed<0)return;revealMissing(p,s,c);
        var rig=TheatreSite.ensure(p,s,p.serverLevel(),CinematicPropEntity.MOUSE_TRAPEZES,c,0);
        if(rig!=null)rig.frame(-1,elapsed,(float)ScriptedShowTimeline.smooth(elapsed/12.0));
        clock(p,s,c,0,elapsed,60,1);if(elapsed%4!=0)return;
        if(elapsed==0)ShowStageLighting.circus(p,s,0,0);double u=ScriptedShowTimeline.smooth(elapsed/60.0);for(var m:mice(p.serverLevel(),s,c)){int i=m.getPersistentData().getInt("ShowSlot");m.showSlot(i);double a=i*Math.PI*2/COUNT;var from=new ScriptedShowTimeline.Point(Math.cos(a)*16,.1,Math.sin(a)*16,0,0);move(m,c,from.blend(ScriptedShowTimeline.star(i,COUNT),u));}}
    private static void revealMissing(ServerPlayer p,VisitData.Scene s,BlockPos c){if(p.tickCount%40==0&&mice(p.serverLevel(),s,c).size()<COUNT)reveal(p,s,c);}
    public static void frame(ServerPlayer p,VisitData.Scene s,BlockPos c,int elapsed,int duration){
        if(duration<=0)return;revealMissing(p,s,c);double progress=Math.max(0,Math.min(1,elapsed/(double)duration));int act=Math.min(4,(int)(progress*5));if(elapsed%20==0)ShowStageLighting.circus(p,s,act,elapsed);
        clock(p,s,c,1,elapsed,duration,1);
        if(elapsed%4==0||elapsed>=duration)for(var m:mice(p.serverLevel(),s,c)){
            m.showSlot(m.getPersistentData().getInt("ShowSlot"));
            move(m,c,ScriptedShowTimeline.mouse(m.getPersistentData().getInt("ShowSlot"),COUNT,progress));
            if(elapsed>=duration)m.setDeltaMovement(Vec3.ZERO);
        }
        for(int kind:new int[]{CinematicPropEntity.HOOPS,CinematicPropEntity.BEAMS,CinematicPropEntity.MOUSE_TRAPEZES}){
            boolean enabled=kind==CinematicPropEntity.HOOPS&&act==1||kind==CinematicPropEntity.BEAMS&&act==2||kind==CinematicPropEntity.MOUSE_TRAPEZES&&act<4;
            var existing=p.serverLevel().getEntitiesOfClass(CinematicPropEntity.class,new AABB(c).inflate(3),e->e.owned(s)&&e.kind()==kind);
            if(enabled){var prop=TheatreSite.ensure(p,s,p.serverLevel(),kind,c,0);if(prop!=null)prop.frame(act,elapsed,(float)(progress*5-act));}else existing.forEach(Entity::discard);
        }
    }
    public static void frameScaled(ServerPlayer p,VisitData.Scene s,BlockPos c,int elapsed,int duration,double scale){if(duration<=0)return;clock(p,s,c,1,elapsed,duration,scale);if(elapsed%4!=0&&elapsed<duration)return;for(var m:mice(p.serverLevel(),s,c)){m.showSlot(m.getPersistentData().getInt("ShowSlot"));var q=ScriptedShowTimeline.mouse(m.getPersistentData().getInt("ShowSlot"),COUNT,Math.max(0,Math.min(1,elapsed/(double)duration)));move(m,c,new ScriptedShowTimeline.Point(q.x()*scale,q.y()*scale,q.z()*scale,q.pitch(),q.roll()));if(p.tickCount%40==0)ShowPlaybackNetwork.pose(m,9,elapsed,(float)Math.max(.45,scale));}}
    private static void move(MouseEntity m,BlockPos c,ScriptedShowTimeline.Point point){Vec3 next=new Vec3(c.getX()+.5+point.x(),c.getY()+point.y(),c.getZ()+.5+point.z()),delta=next.subtract(m.position());if(delta.horizontalDistanceSqr()>.00001){float yaw=(float)(Math.atan2(-delta.x,delta.z)*180/Math.PI);m.setYRot(yaw);m.setYBodyRot(yaw);}m.setNoAi(true);m.setNoGravity(true);m.setInvulnerable(true);m.noPhysics=true;m.getNavigation().stop();m.setXRot(point.pitch());m.setDeltaMovement(Vec3.ZERO);m.setPos(next);m.fallDistance=0;m.getPersistentData().putFloat("ShowRoll",point.roll());}
    public static void cleanup(ServerPlayer p,VisitData.Scene s,BlockPos c){
        for(var l:p.server.getAllLevels()){
            mice(l,s,c).forEach(Entity::discard);
            l.getEntitiesOfClass(CinematicPropEntity.class,new AABB(c).inflate(3),e->e.owned(s)&&(e.kind()==CinematicPropEntity.HOOPS||e.kind()==CinematicPropEntity.BEAMS||e.kind()==CinematicPropEntity.MOUSE_TRAPEZES||e.kind()==CinematicPropEntity.CIRCUS_CLOCK)).forEach(Entity::discard);
        }
    }
    private MouseCircusChoreography(){}
}
