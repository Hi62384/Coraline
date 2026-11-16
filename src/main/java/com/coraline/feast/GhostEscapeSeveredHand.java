package com.coraline.feast;

import com.coraline.block.TinyDoorBlock;
import com.coraline.otherworld.*;
import com.coraline.registry.ModBlocks;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/** The same actual Hand is cut at the real door, falls under block collision,
 * and is handed to the existing well encounter. No key is consumed or issued. */
final class GhostEscapeSeveredHand {
    private static boolean part(CinematicPropEntity hand,VisitData.Scene s,CompoundTag ghost){
        if(hand==null||!hand.owned(s)||hand.kind()!=RescueProps.HAND||hand.slot()!=0||s.mom==null)return false;
        var tag=hand.getPersistentData();return tag.hasUUID("GhostHandOwner")&&s.owner.equals(tag.getUUID("GhostHandOwner"))&&tag.hasUUID("GhostHandDoor")&&s.door.equals(tag.getUUID("GhostHandDoor"))
            &&tag.hasUUID("GhostHandSession")&&ghost.getUUID("Session").equals(tag.getUUID("GhostHandSession"))&&tag.hasUUID("GhostHandAttempt")&&ghost.getCompound(GhostEscapeProgress.KEY).getUUID("Attempt").equals(tag.getUUID("GhostHandAttempt"))
            &&tag.hasUUID("GhostHandMother")&&s.mom.equals(tag.getUUID("GhostHandMother"));
    }
    private static void remember(VisitData.Scene s,CinematicPropEntity hand){
        for(var scope:java.util.List.of(ParentRescueStory.scenery(s),FirstEscapeScene.context(s)))if(scope.hasUUID("RescueSeveredHand")&&hand.getUUID().equals(scope.getUUID("RescueSeveredHand"))){scope.putLong("RescueSeveredHandPosition",hand.blockPosition().asLong());scope.putString("RescueSeveredHandDimension",hand.level().dimension().location().toString());}
    }
    static void tick(ServerPlayer p,VisitData.Scene s,CompoundTag ghost){
        if(!GhostVictimMirrors.owned(p,s,ghost))return;
        var ending=GhostEscapeEndingProgress.existing(ghost,s.owner,s.door);if(ending==null||!ending.getString("Stage").equals(GhostEscapeEndingProgress.Stage.EPILOGUE_COMPLETE.name())||!ending.getBoolean("AlternativeBeldamVictory")||s.mom==null)return;
        var chapter=RescueChapter.data(s);var scenery=ParentRescueStory.scenery(s);var row=GhostEscapeHandProgress.existing(ghost,s.owner,s.door,s.mom);
        if(ending.contains(GhostEscapeHandProgress.KEY)&&row==null){ending.putString("Blocker","The saved Hand detachment has incompatible provenance; no actor replaced.");return;}
        if(row!=null&&scenery.hasUUID("RescueSeveredHand")&&GhostEscapeHandProgress.handoffReady(ghost,s.owner,s.door,s.mom,scenery.getUUID("RescueSeveredHand"))){
            if(!chapter.getBoolean("HandSevered")){chapter.putBoolean("HandSevered",true);OtherMotherLoop.changed(p);}return;
        }
        var hand=RescueProps.find(p,s,"SeveredHand");
        if(row==null&&chapter.getBoolean("HandSevered")){
            // A previously completed normal cut keeps its original actor.
            if(scenery.hasUUID("RescueSeveredHand")&&hand!=null&&hand.kind()==RescueProps.HAND&&hand.slot()==0&&hand.owned(s)){if(GhostEscapeHandProgress.begin(ghost,s.owner,s.door,s.mom,hand.getUUID(),true,true)!=null)OtherMotherLoop.changed(p);}
            else ending.putString("Blocker","Waiting for the original severed Hand; no substitute created.");return;
        }
        var source=DoorData.get(p.server).doors.get(s.door);var level=source==null?null:com.coraline.parents.FamilyData.level(p.server,source.dimension);
        if(source==null||source.revoked||!s.owner.equals(source.owner)||level==null||p.serverLevel()!=level)return;
        level.getChunkAt(source.pos);var state=level.getBlockState(source.pos);
        if(!state.is(ModBlocks.TINY_DOOR.get())||state.getValue(TinyDoorBlock.OPEN)||state.getValue(TinyDoorBlock.PORTAL)||!(level.getBlockEntity(source.pos) instanceof DoorBlockEntity door)||!s.door.equals(door.doorId))return;
        if(row==null){
            if(scenery.contains("RescueSeveredHand")||FirstEscapeScene.context(s).contains("RescueSeveredHand")){
                if(!part(hand,s,ghost)){ending.putString("Blocker","The recorded Hand is unavailable or has different provenance; it was not replaced.");return;}
            }else{
                if(p.distanceToSqr(source.pos.getCenter())>64||!GhostEscapeEnding.hasKey(p,s)){FirstEscapeStory.hint(p,"Bring your Little Door key back to the sealed door.");return;}
                var mother=AcceptedButtonEyesWorld.mother(p,s);
                if(mother==null){var other=p.server.getLevel(OtherWorld.KEY);if(other!=null){LoopProps.hold(other,s.loop.motherPos);var route=ghost.getCompound("MotherBedtimeExit");if(route.contains("Goal"))LoopProps.hold(other,net.minecraft.core.BlockPos.of(route.getLong("Goal")));}return;}
                if(!mother.isOtherParent()||!mother.isMom()||!mother.getPersistentData().hasUUID("RealmDoor")||!s.door.equals(mother.getPersistentData().getUUID("RealmDoor")))return;
                if(mother.getPersistentData().hasUUID("LoopOwner")&&!s.owner.equals(mother.getPersistentData().getUUID("LoopOwner")))return;
                var feet=source.pos.relative(state.getValue(TinyDoorBlock.FACING));var at=Vec3.atBottomCenterOf(feet).add(0,.65,0);
                if(!level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),net.minecraft.core.Direction.UP)||!level.noCollision(null,CinematicPropEntity.TYPE.get().getDimensions().makeBoundingBox(at))){ending.putString("Blocker","Clear a solid landing beside the Little Door for the reaching Hand.");return;}
                hand=RescueProps.ensure(p,s,"SeveredHand",RescueProps.HAND,0,at);if(hand==null)return;
                var tag=hand.getPersistentData();tag.putUUID("GhostHandOwner",s.owner);tag.putUUID("GhostHandDoor",s.door);tag.putUUID("GhostHandSession",ghost.getUUID("Session"));tag.putUUID("GhostHandAttempt",ghost.getCompound(GhostEscapeProgress.KEY).getUUID("Attempt"));tag.putUUID("GhostHandMother",s.mom);
                hand.setYRot(state.getValue(TinyDoorBlock.FACING).toYRot());hand.frame(0,0,1);
            }
            row=GhostEscapeHandProgress.begin(ghost,s.owner,s.door,s.mom,hand.getUUID(),part(hand,s,ghost),false);if(row==null)return;OtherMotherLoop.changed(p);
        }
        if(hand==null||!row.getUUID("Hand").equals(hand.getUUID())||!part(hand,s,ghost)||hand.level()!=level)return;
        hand.noPhysics=false;hand.setNoGravity(true);hand.setInvisible(false);
        if(row.getString("Phase").equals(GhostEscapeHandProgress.Phase.REACHING.name())){FirstEscapeStory.hint(p,"The Hand is caught in the frame. Use your key on the door.");return;}
        if(row.getString("Phase").equals(GhostEscapeHandProgress.Phase.FALLING.name())){
            var feet=source.pos.relative(state.getValue(TinyDoorBlock.FACING));if(!level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),net.minecraft.core.Direction.UP)){ending.putString("Blocker","Restore the solid landing beside the Little Door so the same Hand can finish falling.");return;}
            hand.frame(1,0,1);hand.move(MoverType.SELF,new Vec3(0,-.08,0));remember(s,hand);
            if(GhostEscapeHandProgress.landed(ghost,s.owner,s.door,s.mom,hand.getUUID(),hand.onGround())){chapter.putBoolean("HandSevered",true);ending.remove("Blocker");OtherMotherLoop.changed(p);}
        }
    }
    static boolean closeOnHand(ServerPlayer p,VisitData.Scene s,CompoundTag ghost){
        if(!GhostVictimMirrors.owned(p,s,ghost)||s.mom==null)return false;var row=GhostEscapeHandProgress.existing(ghost,s.owner,s.door,s.mom);if(row==null||!row.getString("Phase").equals(GhostEscapeHandProgress.Phase.REACHING.name()))return false;
        var hand=RescueProps.find(p,s,"SeveredHand");if(!part(hand,s,ghost)||!row.getUUID("Hand").equals(hand.getUUID())||hand.level()!=p.level()||p.distanceToSqr(hand)>25||!p.hasLineOfSight(hand))return true;
        if(!GhostEscapeEnding.hasKey(p,s)){FirstEscapeStory.hint(p,"Use your original Little Door key to shut the Hand off.");return true;}
        var source=DoorData.get(p.server).doors.get(s.door);if(source==null||source.revoked||!s.owner.equals(source.owner)||p.serverLevel()!=com.coraline.parents.FamilyData.level(p.server,source.dimension))return true;
        if(p.distanceToSqr(source.pos.getCenter())>16||!(p.level().getBlockEntity(source.pos) instanceof DoorBlockEntity door)||!s.door.equals(door.doorId))return true;
        var state=p.level().getBlockState(source.pos);boolean closed=state.is(ModBlocks.TINY_DOOR.get())&&!state.getValue(TinyDoorBlock.OPEN)&&!state.getValue(TinyDoorBlock.PORTAL);
        if(GhostEscapeHandProgress.cut(ghost,s.owner,s.door,s.mom,hand.getUUID(),closed,true,true)){hand.frame(1,0,1);p.serverLevel().playSound(null,source.pos,net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE,net.minecraft.sounds.SoundSource.BLOCKS,1,.7F);OtherMotherLoop.changed(p);}return true;
    }
    private GhostEscapeSeveredHand(){}
}
