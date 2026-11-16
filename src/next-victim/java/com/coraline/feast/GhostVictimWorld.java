package com.coraline.feast;

import com.coraline.block.TinyDoorBlock;
import com.coraline.otherworld.*;
import com.coraline.registry.ModBlocks;
import com.coraline.secrets.HouseCabinets;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Reuses the actual owner house, furniture and Little Door. The client reverses
 * deterioration gradually; this class never replaces the estate with a copy. */
public final class GhostVictimWorld {
    public static BlockPos at(VisitData.Scene s,int x,int y,int z){return HouseCabinets.at(AcceptedButtonEyesWorld.house(s),x,y,z);}
    public static BlockPos hall(VisitData.Scene s){return at(s,34,6,44);}
    public static BlockPos supper(VisitData.Scene s){return at(s,44,6,37);}
    public static BlockPos kitchen(VisitData.Scene s){return at(s,47,6,47);}
    public static BlockPos bedroom(VisitData.Scene s){return at(s,44,15,23);}
    public static boolean prepare(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        if(!GhostVictimMirrors.owned(p,s,n))return false;
        var level=AcceptedButtonEyesWorld.level(p);var realm=AcceptedButtonEyesWorld.realm(p,s);
        if(level==null||realm==null||realm.house==null||realm.doorPos==null)return false;
        level.getChunkAt(realm.doorPos);
        var accepted=AcceptedButtonEyes.data(s);var old=level.getBlockState(realm.doorPos);
        if(!old.is(ModBlocks.TINY_DOOR.get())){n.putString("Blocker","The owner's Little Door is missing.");return false;}
        var open=old.setValue(TinyDoorBlock.OPEN,true).setValue(TinyDoorBlock.PORTAL,true);
        level.setBlock(realm.doorPos,open,3);
        if(level.getBlockEntity(realm.doorPos) instanceof DoorBlockEntity door){
            door.storyBricked=false;door.setChanged();level.sendBlockUpdated(realm.doorPos,open,open,3);
            var backing=realm.doorPos.relative(open.getValue(TinyDoorBlock.FACING).getOpposite());
            if(level.getBlockState(backing).is(Blocks.BRICKS))FirstEscapeScene.place(p,s,backing,Blocks.AIR.defaultBlockState());
            TunnelPassage.ensure(level,door);
        }
        // Restore only the captured bed cells that are still empty. No chests,
        // inventories, unrelated builds or user-changed furniture are replaced.
        if(!n.getBoolean("ChildBedRestored")){
            var saved=accepted.getList("OriginalBed",Tag.TAG_COMPOUND);boolean complete=!saved.isEmpty();
            for(Tag entry:saved){var cell=(CompoundTag)entry;var pos=BlockPos.of(cell.getLong("Position"));level.getChunkAt(pos);
                var state=NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),cell.getCompound("State"));
                if(level.getBlockState(pos).isAir())level.setBlock(pos,state,2|16);
                complete&=level.getBlockState(pos).equals(state);
                if(level.getBlockState(pos).equals(state)&&cell.contains("BlockEntity")&&level.getBlockEntity(pos)!=null)level.getBlockEntity(pos).load(cell.getCompound("BlockEntity"));
            }
            if(complete)n.putBoolean("ChildBedRestored",true);
        }
        if(!mirrors(p,s,n))return false;n.remove("Blocker");OtherMotherLoop.changed(p);return true;
    }
    private static boolean mirrors(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        String[] ids={"main_hall","living_room","bedroom","upstairs_corridor"};
        int[][] cells={{35,6,27},{28,6,51},{48,15,23},{35,15,48}};float[] yaw={180,0,270,0};
        var level=AcceptedButtonEyesWorld.level(p);
        for(int i=0;i<ids.length;i++){
            var position=at(s,cells[i][0],cells[i][1],cells[i][2]);
            var prop=FirstEscapeScene.ensure(p,s,"VictimMirror_"+ids[i],CinematicPropEntity.STORY_MIRROR,i,level,Vec3.atBottomCenterOf(position));
            if(prop==null)return false;prop.setYRot(yaw[i]);prop.frame(0,0,0);
            GhostVictimMirrors.register(n,ids[i],position,yaw[i]);
            n.getCompound("Mirrors").getCompound(ids[i]).putUUID("Entity",prop.getUUID());
        }
        return true;
    }
    public static double deterioration(CompoundTag n){
        return switch(GhostVictimProgress.phase(n)){
            case ENTER_MIRROR,EMPTY_WORLD->5;
            case RECONSTRUCTION->5*(1-Math.min(1,GhostVictimProgress.age(n)/(20.*GhostVictimConfig.REBUILD_SECONDS.get())));
            case NEW_VICTIM_RESET->{double duration=20.*GhostVictimConfig.REBUILD_SECONDS.get(),age=GhostVictimProgress.age(n);
                yield age<duration/2?5*Math.min(1,age/(duration/2)):5*(1-Math.min(1,(age-duration/2)/duration));}
            default->0;
        };
    }
    public static boolean ordinaryDoor(VisitData.Scene s,BlockPos position){
        for(var room:HouseCabinets.ROOMS)for(int[] door:room.doors()){
            var at=at(s,door[0],door[1],door[2]);if(at.equals(position)||at.above().equals(position))return false;
        }
        return !OtherWorldEstate.outsideHouse(AcceptedButtonEyesWorld.house(s),position);
    }
    private GhostVictimWorld(){}
}
