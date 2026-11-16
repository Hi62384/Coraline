package com.coraline.client.renderer;

import com.coraline.client.ShowActorPoseClient;
import com.coraline.entity.CharacterEntity;
import com.coraline.feast.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Hands and bars evaluate the same prop cursor, not separately delayed mob interpolation. */
final class TheatreAerialPresentation {
    static void align(CharacterEntity actor,float partial,PoseStack pose){
        var frame=ShowActorPoseClient.frame(actor);if(frame==null||(frame.pose()!=5&&frame.pose()!=16))return;
        int side=actor.characterId().equals("other_miss_spink")?-1:actor.characterId().equals("other_miss_forcible")?1:0;if(side==0)return;
        var props=actor.level().getEntitiesOfClass(CinematicPropEntity.class,actor.getBoundingBox().inflate(72),e->e.kind()==CinematicPropEntity.TRAPEZE);if(props.isEmpty())return;
        var prop=props.stream().min(java.util.Comparator.comparingDouble(actor::distanceToSqr)).orElseThrow();if(prop.cue()<1||prop.value()<=0)return;double tick=prop.visualTime(partial);Vec3 at;double flip=0;
        if(prop.cue()==1){var grab=prop.grabTarget();var point=TheatreAerialGeometry.performer(side,tick,(int)prop.value(),grab.x,grab.y,grab.z);at=new Vec3(point.x(),point.y(),point.z());flip=point.airborne()?point.flip():0;}
        else {var q=TheatreAerialGeometry.carrying(side,tick,(int)prop.value(),prop.cue()==3);at=new Vec3(q.x(),q.y()-TheatreAerialGeometry.GRIP_HEIGHT,q.z());}
        var target=prop.position().add(0,0,15).add(at);var displayed=new Vec3(Mth.lerp(partial,actor.xo,actor.getX()),Mth.lerp(partial,actor.yo,actor.getY()),Mth.lerp(partial,actor.zo,actor.getZ()));var delta=target.subtract(displayed);pose.translate(delta.x,delta.y,delta.z);
        if(flip!=0){pose.translate(0,1.05,0);pose.mulPose(Axis.XP.rotation((float)flip));pose.translate(0,-1.05,0);}
    }
    private TheatreAerialPresentation(){}
}
