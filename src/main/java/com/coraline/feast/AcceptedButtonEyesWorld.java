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
    static ParentEntity mother(ServerPlayer p,VisitData.Scene s){
        var actor=parent(p,s,s.mom);if(actor instanceof ParentEntity&&actor.isMom()&&actor.isOtherParent()
                &&actor.getPersistentData().hasUUID("RealmDoor")&&s.door.equals(actor.getPersistentData().getUUID("RealmDoor")))return actor;
        // The recorded Other Mother can simply be in an unloaded chunk after
        // a restart or dimension trip. Request her last saved location and
        // keep waiting for that UUID; never let the generic visit initializer
        // create a second canonical actor for this route.
        if(s.mom!=null){var level=level(p);var at=s.loop.motherPos;if(level!=null){level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(at),2,at);return level.getEntity(s.mom) instanceof ParentEntity loaded&&loaded.isMom()&&loaded.isOtherParent()&&loaded.getPersistentData().hasUUID("RealmDoor")&&s.door.equals(loaded.getPersistentData().getUUID("RealmDoor"))?loaded:null;}}
        return null;
    }
    static ParentEntity father(ServerPlayer p,VisitData.Scene s){
        var actor=parent(p,s,s.dad);
        return authenticParent(p,s,actor,false)?actor:null;
    }
    private static boolean authenticParent(ServerPlayer p,VisitData.Scene s,ParentEntity actor,boolean mom){
        var realm=realm(p,s);
        return actor!=null&&actor.isAlive()&&actor.isMom()==mom&&actor.isOtherParent()&&realm!=null
                &&actor.getPersistentData().hasUUID("RealmDoor")&&s.door.equals(actor.getPersistentData().getUUID("RealmDoor"))
                &&(!actor.getPersistentData().contains("AcceptedOwner")||actor.getPersistentData().hasUUID("AcceptedOwner")&&s.owner.equals(actor.getPersistentData().getUUID("AcceptedOwner")))
                &&(!actor.getPersistentData().contains("AcceptedDoor")||actor.getPersistentData().hasUUID("AcceptedDoor")&&s.door.equals(actor.getPersistentData().getUUID("AcceptedDoor")))
                &&(!actor.getPersistentData().contains("RealmGeneration")||actor.getPersistentData().getInt("RealmGeneration")==realm.parentGeneration);
    }
    static ParentEntity recoverSupperParent(ServerPlayer p,VisitData.Scene s,boolean mom,BlockPos near){
        var level=level(p);
        var realm=realm(p,s);
        var id=mom?s.mom:s.dad;
        if(id==null||level==null||realm==null||!s.owner.equals(p.getUUID()))return null;
        for(var world:p.server.getAllLevels()){
            var existing=world.getEntity(id);
            if(existing==null)continue;
            if(!(existing instanceof ParentEntity actor)||!authenticParent(p,s,actor,mom))return null;
            if(actor.level()==level)return actor;
            var landing=net.minecraft.core.BlockPos.findClosestMatch(near,3,1,at->level.hasChunkAt(at)
                    &&FirstEscapeScene.inHouseOrPorch(s,at)&&com.coraline.tycoon.TycoonWorld.safeFeet(level,at)).orElse(null);
            if(landing==null)return null;
            var moved=ShowTravel.guest(actor,p,s,level,Vec3.atBottomCenterOf(landing),true,"AcceptedParents");
            return moved instanceof ParentEntity same&&same.getUUID().equals(id)&&same.level()==level?same:null;
        }
        var saved=GhostVictimEntry.data(s);
        var key=mom?"SupperMotherPosition":"SupperFatherPosition";
        var original=saved.contains(key,net.minecraft.nbt.Tag.TAG_LONG)?BlockPos.of(saved.getLong(key)):mom?s.loop.motherPos:s.piano;
        if(original==null||!FirstEscapeScene.inHouseOrPorch(s,original))original=near;
        LoopProps.hold(level,original);
        if(!level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(original).toLong()))return null;
        var family=com.coraline.parents.ParentAccess.family(p.server,s.owner);
        if(family==null)return null;
        var feet=net.minecraft.core.BlockPos.findClosestMatch(near,3,1,at->level.hasChunkAt(at)&&FirstEscapeScene.inHouseOrPorch(s,at)
                &&com.coraline.tycoon.TycoonWorld.safeFeet(level,at)).orElse(null);
        if(feet==null||com.coraline.rebirth.LegacyActorRecovery.check(p.server,id)!=com.coraline.rebirth.LegacyActorRecovery.Result.ABSENT)return null;
        ParentEntity actor=(mom?com.coraline.registry.ModEntities.MOM.get():com.coraline.registry.ModEntities.DAD.get()).create(level);
        if(actor==null)return null;
        actor.setUUID(id);
        actor.makeOtherParent(Math.max(0,mom?family.momSkin:family.dadSkin));
        var mark=actor.getPersistentData();
        mark.putUUID("OriginalFamily",family.id);
        mark.putUUID("RealmDoor",s.door);
        mark.putInt("RealmGeneration",realm.parentGeneration);
        mark.putLong("OtherHome",feet.asLong());
        mark.putUUID("AcceptedOwner",s.owner);
        mark.putUUID("AcceptedDoor",s.door);
        actor.setPos(Vec3.atBottomCenterOf(feet));
        if(!level.noCollision(actor)||!level.addFreshEntity(actor)||level.getEntity(id)!=actor)return null;
        saved.putLong(key,feet.asLong());
        OtherMotherLoop.changed(p);
        return p.server.saveEverything(true,true,true)?actor:null;
    }
    private static ParentEntity parent(ServerPlayer p,VisitData.Scene s,java.util.UUID id){if(id==null)return null;for(var l:p.server.getAllLevels())if(l.getEntity(id) instanceof ParentEntity actor)return actor;return null;}
    static boolean parents(ServerPlayer p,VisitData.Scene s){
        var l=level(p);var c=chair(s);var mom=mother(p,s);var dad=father(p,s);
        if(mom==null){AcceptedButtonEyesProgress.data(s).putString("Blocker","Waiting for the original Other Mother to load. Her saved actor is being requested; no replacement will be created.");return false;}
        if(dad==null){VisitEvents.ensureFather(p,s);dad=father(p,s);}
        if(dad==null&&s.dad!=null&&p.tickCount%20==0)dad=recoverSupperParent(p,s,false,c.west(2).north());
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
