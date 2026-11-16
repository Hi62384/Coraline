package com.coraline.client.renderer;

import com.coraline.feast.*;
import com.coraline.otherworld.MouseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Deterministic fractional-tick motion; server hitboxes need only sparse corrections. */
final class CircusMousePresentation {
    private static long cachedTick=Long.MIN_VALUE;private static net.minecraft.world.level.Level world;private static java.util.List<CinematicPropEntity> clocks=java.util.List.of();
    static void align(MouseEntity mouse,float partial,PoseStack pose){
        if(mouse.circusStyle()<0||mouse.showSlot()<0)return;
        if(world!=mouse.level()||cachedTick!=mouse.level().getGameTime()){world=mouse.level();cachedTick=world.getGameTime();clocks=world.getEntitiesOfClass(CinematicPropEntity.class,mouse.getBoundingBox().inflate(100),e->e.kind()==CinematicPropEntity.CIRCUS_CLOCK);}
        var clock=clocks.stream().filter(e->e.distanceToSqr(mouse)<100*100).min(java.util.Comparator.comparingDouble(mouse::distanceToSqr)).orElse(null);if(clock==null||clock.value()<=0)return;
        double t=clock.visualTime(partial),scale=clock.grabTarget().x;var point=ScriptedShowTimeline.mouse(mouse.showSlot(),MouseCircusChoreography.COUNT,Math.max(0,Math.min(1,t/clock.value())));
        if(clock.cue()==0){double angle=mouse.showSlot()*Math.PI*2/MouseCircusChoreography.COUNT;point=new ScriptedShowTimeline.Point(Math.cos(angle)*16,.1,Math.sin(angle)*16,0,0).blend(ScriptedShowTimeline.star(mouse.showSlot(),MouseCircusChoreography.COUNT),ScriptedShowTimeline.smooth(t/60.0));}
        var target=clock.position().add(point.x()*scale,point.y()*scale,point.z()*scale);var displayed=new Vec3(Mth.lerp(partial,mouse.xo,mouse.getX()),Mth.lerp(partial,mouse.yo,mouse.getY()),Mth.lerp(partial,mouse.zo,mouse.getZ()));var delta=target.subtract(displayed);pose.translate(delta.x,delta.y,delta.z);
    }
    private CircusMousePresentation(){}
}
