package com.coraline.feast;

import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared manifestation identifier and visibility rules. */
public final class GhostManifestationTarget {
    private static final Pattern ID=Pattern.compile("[a-z_][a-z0-9_]{0,59}");

    public static boolean validId(String id){return id!=null&&ID.matcher(id).matches();}

    public static boolean aimed(Vec3 look,Vec3 delta){
        double distance=delta.lengthSqr();
        return distance>.001&&distance<=36&&look.dot(delta.normalize())>=.92;
    }

    public static boolean clear(Level level,Entity observer,Vec3 target,BlockPos fixture){
        var ray=level.clip(new ClipContext(observer.getEyePosition(),target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,observer));
        return visibleHit(ray.getType(),ray.getBlockPos(),fixture);
    }

    static boolean visibleHit(HitResult.Type type,BlockPos hit,BlockPos fixture){
        return type==HitResult.Type.MISS||type==HitResult.Type.BLOCK&&fixture!=null&&fixture.equals(hit);
    }

    private GhostManifestationTarget(){}
}
