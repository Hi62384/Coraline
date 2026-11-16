package com.coraline.wybie;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
/** Resolve scripted poses out of solid terrain without snapping intentional flights. */
public final class VisitGrounding {
    public static void lift(Entity e){if(e.level().isClientSide||e.isPassenger()||e.isNoGravity())return;var box=e.getBoundingBox();if(e.level().noCollision(e,box))return;for(int i=1;i<=48;i++){double lift=i/16.0;if(e.level().noCollision(e,box.move(0,lift,0))){e.setPos(e.getX(),e.getY()+lift,e.getZ());e.setDeltaMovement(Vec3.ZERO);e.setOnGround(true);return;}}}
    public static boolean stand(Entity e,Vec3 at){var before=e.position();e.setPos(at);lift(e);if(e.level().noCollision(e,e.getBoundingBox()))return true;e.setPos(before);return false;}
}
