package com.coraline.feast;

import com.coraline.block.TinyDoorBlock;
import com.coraline.otherworld.*;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.ModBlocks;
import com.coraline.secrets.HouseCabinets;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.*;

/** Reuses the existing basement and journals only this owner's bed/door edits. */
public final class AcceptedButtonEyesWorld {
    public static final int BUTTONS=88,NEEDLE=89,SEWING_TABLE=90;
    static RealmData.Realm realm(ServerPlayer p,VisitData.Scene s){return RealmData.get(p.server).realms.get(s.door);}
    static ServerLevel level(ServerPlayer p){return p.server.getLevel(OtherWorld.KEY);}
    static BlockPos house(VisitData.Scene s){return BlockPos.of(AcceptedButtonEyesProgress.scenery(s).getLong("House"));}
    static BlockPos chair(VisitData.Scene s){return BlockPos.of(AcceptedButtonEyesProgress.scenery(s).getLong("SewingChairAt"));}
    static BlockPos upstairs(VisitData.Scene s){return HouseCabinets.at(house(s),34,6,23);}
    static boolean prepare(ServerPlayer p,VisitData.Scene s){
        var r=realm(p,s);var l=level(p);if(r==null||r.house==null||l==null)return false;
        var n=AcceptedButtonEyesProgress.scenery(s);n.putLong("House",r.house.asLong());n.putLong("LittleDoor",r.doorPos.asLong());
        if(!n.contains("Prison"))n.putLong("Prison",r.house.above(80).asLong());
        ShowHouseAccess.open(p,s,"basement");
        if(!n.contains("SewingChairAt")){
            BlockPos found=null;var center=HouseCabinets.at(r.house,34,-1,22);
            for(int radius=0;radius<=4&&found==null;radius++)for(int x=-radius;x<=radius&&found==null;x++)for(int z=-radius;z<=radius;z++){
                var at=center.offset(x,0,z);l.getChunkAt(at);if(!l.getBlockState(at.below()).isFaceSturdy(l,at.below(),Direction.UP))continue;
                boolean clear=true;for(int dx=-1;dx<=2;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=0;dy<=2;dy++)clear&=l.getBlockState(at.offset(dx,dy,dz)).getCollisionShape(l,at.offset(dx,dy,dz)).isEmpty()&&l.getFluidState(at.offset(dx,dy,dz)).isEmpty();
                if(clear){found=at;break;}
            }
            if(found==null){AcceptedButtonEyesProgress.data(s).putString("Blocker","Clear a small, dry space in the existing basement for the sewing chair.");return false;}
            n.putLong("SewingChairAt",found.asLong());
        }
        var c=chair(s);var seat=FirstEscapeScene.ensure(p,s,"SewingChair",CinematicPropEntity.SEAT,0,l,Vec3.atBottomCenterOf(c));if(seat!=null)seat.setYRot(-90);
        var table=FirstEscapeScene.ensure(p,s,"SewingTable",SEWING_TABLE,0,l,Vec3.atBottomCenterOf(c.east(2)));if(table!=null){if(table.kind()!=SEWING_TABLE)table.configure(s,SEWING_TABLE,0);table.setYRot(0);}
        if(!AcceptedButtonEyesProgress.data(s).getBoolean("SewingCompleted")){
            var buttons=FirstEscapeScene.ensure(p,s,"SewingButtons",BUTTONS,0,l,Vec3.atBottomCenterOf(c.east(2)).add(-.25,.94,0));if(buttons!=null&&buttons.kind()!=BUTTONS)buttons.configure(s,BUTTONS,0);
            var needle=FirstEscapeScene.ensure(p,s,"SewingNeedle",NEEDLE,0,l,Vec3.atBottomCenterOf(c.east(2)).add(.25,.95,0));if(needle!=null&&needle.kind()!=NEEDLE)needle.configure(s,NEEDLE,0);
        }
        FirstEscapeScene.light(p,s,c.above(3),13);return seat!=null&&table!=null;
    }
    static ParentEntity mother(ServerPlayer p,VisitData.Scene s){return parent(p,s,s.mom);}
    static ParentEntity father(ServerPlayer p,VisitData.Scene s){return parent(p,s,s.dad);}
    private static ParentEntity parent(ServerPlayer p,VisitData.Scene s,java.util.UUID id){if(id==null)return null;for(var l:p.server.getAllLevels())if(l.getEntity(id) instanceof ParentEntity actor)return actor;return null;}
    static boolean parents(ServerPlayer p,VisitData.Scene s){
        var l=level(p);var c=chair(s);var mom=mother(p,s);var dad=father(p,s);
        if(mom==null){if(!OtherMotherLoop.prepare(p,realm(p,s)))return false;mom=mother(p,s);}
        if(dad==null){VisitEvents.ensureFather(p,s);dad=father(p,s);}
        if(mom==null||dad==null)return false;s.mom=mom.getUUID();s.dad=dad.getUUID();
        for(var actor:new ParentEntity[]{mom,dad}){
            if(actor==null)return false;var at=Vec3.atBottomCenterOf(actor.isMom()?c.east():c.west(2).north());
            if(actor.level()!=l){var moved=ShowTravel.guest(actor,p,s,l,at,true,"AcceptedParents");if(!(moved instanceof ParentEntity replacement))return false;actor=replacement;}
            if(!actor.getPersistentData().getBoolean("AcceptedBasementPlaced")){actor.stopRiding();if(!actor.isMom())actor.setPos(at);actor.getPersistentData().putBoolean("AcceptedBasementPlaced",true);}
            actor.getPersistentData().putUUID("AcceptedOwner",s.owner);actor.getPersistentData().putUUID("AcceptedDoor",s.door);
            actor.setInvisible(false);actor.setNoGravity(false);actor.noPhysics=false;actor.setNoAi(false);actor.eventGaze(true);
        }
        return true;
    }
    static void sealAndRemoveBed(ServerPlayer p,VisitData.Scene s){
        var r=realm(p,s);var l=level(p);if(r==null||l==null)return;var n=AcceptedButtonEyesProgress.data(s);
        l.getChunkAt(r.doorPos);var door=l.getBlockState(r.doorPos);
        if(door.is(ModBlocks.TINY_DOOR.get())){
            if(!n.contains("OriginalLittleDoor"))n.put("OriginalLittleDoor",NbtUtils.writeBlockState(door));
            var sealed=door.setValue(TinyDoorBlock.OPEN,true).setValue(TinyDoorBlock.PORTAL,false);if(!sealed.equals(door))l.setBlock(r.doorPos,sealed,3);n.putBoolean("LittleDoorSealed",true);
            if(l.getBlockEntity(r.doorPos) instanceof DoorBlockEntity entity&&!entity.storyBricked){entity.storyBricked=true;entity.setChanged();l.sendBlockUpdated(r.doorPos,l.getBlockState(r.doorPos),l.getBlockState(r.doorPos),3);}
            var backing=r.doorPos.relative(door.getValue(TinyDoorBlock.FACING).getOpposite());if(!l.getBlockState(backing).is(Blocks.BRICKS))FirstEscapeScene.place(p,s,backing,Blocks.BRICKS.defaultBlockState());
        }
        if(r.bed==null)return;
        if(n.getBoolean("BedRemoved")){for(Tag row:n.getList("OriginalBed",Tag.TAG_COMPOUND)){var at=BlockPos.of(((CompoundTag)row).getLong("Position"));if(l.getBlockState(at).getBlock() instanceof BedBlock)l.setBlock(at,Blocks.AIR.defaultBlockState(),2);}return;}
        var state=l.getBlockState(r.bed);if(!(state.getBlock() instanceof BedBlock))return;
        var partner=r.bed.relative(state.getValue(BedBlock.PART)==BedPart.FOOT?state.getValue(BedBlock.FACING):state.getValue(BedBlock.FACING).getOpposite());
        var saved=new ListTag();for(var at:new BlockPos[]{r.bed,partner}){var block=l.getBlockState(at);if(!(block.getBlock() instanceof BedBlock))continue;var cell=new CompoundTag();cell.putLong("Position",at.asLong());cell.put("State",NbtUtils.writeBlockState(block));var be=l.getBlockEntity(at);if(be!=null)cell.put("BlockEntity",be.saveWithFullMetadata());saved.add(cell);}
        n.put("OriginalBed",saved);OtherMotherLoop.changed(p);
        for(Tag row:saved)l.setBlock(BlockPos.of(((CompoundTag)row).getLong("Position")),Blocks.AIR.defaultBlockState(),2);
        n.putBoolean("BedRemoved",true);OtherMotherLoop.changed(p);
    }
    static boolean transfer(ServerPlayer p,VisitData.Scene s,BlockPos near){
        var l=level(p);if(l==null)return false;var at=landing(l,near);if(at==null)return false;
        p.stopRiding();p.setForcedPose(null);p.teleportTo(l,at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),0);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;return true;
    }
    static BlockPos landing(ServerLevel l,BlockPos near){
        l.getChunkAt(near);var original=OtherWorld.safePlayer(l,near);
        if(original.distSqr(near)<=25&&l.getFluidState(original).isEmpty()&&l.getFluidState(original.above()).isEmpty())return original;
        // The generic column-first search can select the basement underneath a
        // carpeted hall before checking a clear neighbouring floor tile. Keep
        // this story transfer within its existing five-block arrival bound.
        BlockPos best=null;double distance=26;
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-2;y<=2;y++){
            var at=near.offset(x,y,z);double candidate=at.distSqr(near);if(candidate>=distance||candidate>25)continue;
            l.getChunkAt(at);
            if(!l.isEmptyBlock(at)||!l.isEmptyBlock(at.above())||!l.getFluidState(at).isEmpty()||!l.getFluidState(at.above()).isEmpty()||!l.getBlockState(at.below()).isFaceSturdy(l,at.below(),Direction.UP))continue;
            best=at;distance=candidate;
        }
        return best;
    }
    private AcceptedButtonEyesWorld(){}
}
