package com.coraline.feast;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RescueCatThrowGameTests {
    @GameTest(template="empty") public static void faceTargetMatchesRigAndCrosshairThrowsReachItAtOrdinaryRanges(GameTestHelper h){
        var mother=com.coraline.registry.ModEntities.MOM.get().create(h.getLevel());mother.makeOtherParent(0);mother.setPos(10,80,20);
        for(int degrees:new int[]{-180,-90,0,45,90,135}){mother.setYRot(degrees);var aim=BeldamFinale.facePoint(mother);h.assertTrue(aim.y>mother.getEyeY()+.7,"Face target still uses vanilla parent's torso-height eyes");double yaw=Math.toRadians(degrees);var forward=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
            for(double distance:new double[]{2,3,5,8}){var eye=aim.add(forward.scale(distance)).add(0,-.8,0);var look=aim.subtract(eye).normalize();var at=eye.add(look.scale(.65)).add(0,-.45,0);var velocity=BeldamFinale.catThrowVelocity(at,aim);boolean reached=false;for(int tick=0;tick<20;tick++){var next=at.add(velocity);if(BeldamFinale.faceBox(mother).clip(at,next).isPresent()){reached=true;break;}at=next;velocity=velocity.add(0,-.045,0);}h.assertTrue(reached,"Hand-origin throw aimed at real face missed at "+distance+"m / yaw "+degrees);}
            var delta=forward.scale(5);float actual=BeldamFinale.face(mother.position(),mother.position().add(delta));h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(actual-degrees))<.001,"Beldam was facing away from her player");
        }h.succeed();
    }
    @GameTest(template="empty") public static void bodyHitsAndWallsCannotTriggerTheFaceAttack(GameTestHelper h){
        var mother=com.coraline.registry.ModEntities.MOM.get().create(h.getLevel());mother.makeOtherParent(0);mother.setPos(10,80,20);mother.setYRot(0);var face=BeldamFinale.facePoint(mother);var from=face.add(0,0,2);var through=face.add(0,0,-2);var miss=BlockHitResult.miss(through,net.minecraft.core.Direction.NORTH,net.minecraft.core.BlockPos.containing(through));
        h.assertFalse(BeldamFinale.catHitBeforeBlock(mother,from.add(0,-1.3,0),through.add(0,-1.3,0),miss),"A chest/leg hit blinded the Beldam");h.assertTrue(BeldamFinale.catHitBeforeBlock(mother,from,through,miss),"Unobstructed face hit failed");var wall=new BlockHitResult(from.lerp(face,.35),net.minecraft.core.Direction.SOUTH,net.minecraft.core.BlockPos.containing(from),false);h.assertFalse(BeldamFinale.catHitBeforeBlock(mother,from,through,wall),"Face behind an earlier wall took the hit");var behind=new BlockHitResult(face.add(0,0,-1),net.minecraft.core.Direction.SOUTH,net.minecraft.core.BlockPos.containing(through),false);h.assertTrue(BeldamFinale.catHitBeforeBlock(mother,from,through,behind),"A block behind the face incorrectly cancelled its prior impact");h.succeed();
    }
    @GameTest(template="empty") public static void handOriginCannotStartThroughANearbyWall(GameTestHelper h){
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"CatOriginQA"));var at=h.absolutePos(new net.minecraft.core.BlockPos(4,3,4));p.setPos(at.getX()+.5,at.getY(),at.getZ()+.8);p.setYRot(0);p.setXRot(0);var wall=at.south().above();var before=h.getLevel().getBlockState(wall);
        try{h.getLevel().setBlock(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);var origin=BeldamFinale.catThrowOrigin(p);h.assertTrue(origin.z<wall.getZ()&&origin.z>p.getZ(),"Hand-origin spawn crossed the nearby wall before collision tracing began");}finally{h.getLevel().setBlock(wall,before,2);}h.succeed();
    }
    private RescueCatThrowGameTests(){}
}
