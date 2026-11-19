package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class GhostSupperStateCheck {
    public static void main(String[] args){
        for(int slot=0;slot<6;slot++)require(GhostManifestationTarget.validId("room_light_"+slot),"Numbered light ID rejected");
        require(GhostManifestationTarget.validId("outing_other_village_secret"),"Outing ID rejected");
        require(!GhostManifestationTarget.validId("room_light_0/elsewhere"),"Manifestation path accepted");
        require(!GhostManifestationTarget.validId("a".repeat(61)),"Oversized ID accepted");
        var lamp=new BlockPos(0,2,3);var wall=new BlockPos(0,2,2);
        require(GhostManifestationTarget.visibleHit(HitResult.Type.BLOCK,lamp,lamp),"Solid fixture cannot be seen");
        require(!GhostManifestationTarget.visibleHit(HitResult.Type.BLOCK,wall,lamp),"Intervening wall was ignored");
        require(!GhostManifestationTarget.visibleHit(HitResult.Type.BLOCK,lamp,null),"Unrelated fixture was ignored");
        require(GhostManifestationTarget.aimed(new Vec3(0,0,1),new Vec3(0,0,6)),"Server reach excluded");
        require(!GhostManifestationTarget.aimed(new Vec3(0,0,1),new Vec3(0,0,6.1)),"Excessive reach allowed");

        for(int[] position:new int[][]{{-2,0},{2,0},{0,-2},{0,2}}){
            float facing=GhostSupperGeometry.facing(position[0],position[1],0,0);
            var expected=new Vec3(-position[0],0,-position[1]).normalize();
            require(expected.dot(Vec3.directionFromRotation(0,facing))>.999,"Rider does not face the table");
        }
        for(int appearance=0;appearance<5;appearance++){
            var hip=GhostSupperGeometry.riderOrigin(101,appearance)+.22*GhostSupperGeometry.childScale(appearance);
            require(Math.abs(hip-GhostSupperGeometry.CUSHION_TOP)<1e-7,"Child hip misses cushion");
        }
        require(Math.abs(GhostSupperGeometry.riderOrigin(103,0)+GhostSupperGeometry.PARENT_HIP
                -GhostSupperGeometry.CUSHION_TOP)<1e-7,"Parent hip misses cushion");
        require(!GhostSupperGeometry.isSeat(0),"Other scenes picked up supper offsets");
        require(GhostSupperGeometry.elapsedFromLegacyBeat(6)==1480,"Legacy meal skipped its last window");
        require(GhostSupperGeometry.elapsedFromLegacyBeat(99)==0,"Corrupt beat skipped the meal");
        System.out.println("GhostSupperStateCheck passed");
    }
    private static void require(boolean ok,String detail){if(!ok)throw new AssertionError(detail);}
}
