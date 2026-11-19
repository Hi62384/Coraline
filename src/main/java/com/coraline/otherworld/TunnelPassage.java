package com.coraline.otherworld;

import com.coraline.block.TinyDoorBlock;
import com.coraline.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Restorable local entrance; the dimension change is hidden inside matching fabric. */
@Mod.EventBusSubscriber(modid="coraline")
public final class TunnelPassage {
    public static final double TRANSFER_DEPTH=2.2;
    public static double depth(Player p,BlockPos door,Direction facing){var delta=p.position().subtract(Vec3.atBottomCenterOf(door));return -delta.x*facing.getStepX()-delta.z*facing.getStepZ();}
    public static Vec3 inside(BlockPos door,Direction facing,double depth){return Vec3.atBottomCenterOf(door).add(-facing.getStepX()*depth,0,-facing.getStepZ()*depth);}
    public static boolean contains(Player player,DoorBlockEntity door){var pos=door.getBlockPos();var facing=door.getBlockState().getValue(TinyDoorBlock.FACING);double along=depth(player,pos,facing),side=Math.abs((player.getX()-pos.getX()-.5)*facing.getStepZ()-(player.getZ()-pos.getZ()-.5)*facing.getStepX());return along>=0&&along<=3.5&&side<.7&&Math.abs(player.getY()-pos.getY())<.6;}
    public static final int VERSION=5;

    private static BlockPos cell(BlockPos origin,Direction facing,int depth){
        return origin.relative(facing,-depth);
    }

    public static boolean ensure(ServerLevel level,DoorBlockEntity door){
        if(door.doorId==null||door.storyPassageBlocked||door.hasPostgameBinding())return false;
        var origin=door.getBlockPos();
        var facing=door.getBlockState().getValue(TinyDoorBlock.FACING);
        var journal=TunnelPassageJournal.get(level);
        if(!journal.usable())return false;
        String key=TunnelPassageJournal.key(level,origin);
        var row=journal.rows.getCompound(key);
        if(!row.isEmpty()&&(!row.hasUUID("Door")||!door.doorId.equals(row.getUUID("Door"))))return false;
        if(row.getBoolean("Restoring")||door.passageVersion<VERSION&&!door.passageOriginals.isEmpty()){
            if(!restore(level,door))return false;
            row=new CompoundTag();
        }
        if(row.isEmpty()){
            var originals=new ListTag();
            for(int distance=1;distance<=3;distance++){
                var at=cell(origin,facing,distance);
                if(!level.hasChunkAt(at)||level.isOutsideBuildHeight(at)||level.getBlockEntity(at)!=null)return false;
                var original=level.getBlockState(at);
                if(!original.isAir())originals.add(entry(at,original,Blocks.AIR.defaultBlockState()));
                var floor=at.below();
                if(!level.hasChunkAt(floor)||level.isOutsideBuildHeight(floor))return false;
                if(level.getBlockState(floor).isAir())originals.add(entry(floor,Blocks.AIR.defaultBlockState(),ModBlocks.TUNNEL_FABRIC.get().defaultBlockState().setValue(ModBlocks.TUNNEL_LINED,true)));
            }
            row.putUUID("Door",door.doorId);row.putLong("Origin",origin.asLong());
            row.putString("Level",level.dimension().location().toString());
            row.putString("Facing",facing.getName());row.put("Cells",originals);
            journal.rows.put(key,row);
            if(!journal.checkpoint(level)){journal.rows.remove(key);journal.setDirty();return false;}
        }
        if(!facing.getName().equals(row.getString("Facing"))||!row.contains("Cells",Tag.TAG_LIST))return false;
        var cells=row.getList("Cells",Tag.TAG_COMPOUND);
        boolean wasBuilt=door.passageBuilt;
        if(door.passageVersion!=VERSION||!cells.equals(door.passageOriginals)){
            door.passageOriginals=cells.copy();door.passageVersion=VERSION;door.setChanged();
        }
        boolean built=true;
        for(var value:cells){
            var n=(CompoundTag)value;var at=BlockPos.of(n.getLong("Pos"));
            if(!validEntry(n)||!validCell(origin,facing,at)||!level.hasChunkAt(at)||level.getBlockEntity(at)!=null){built=false;continue;}
            var from=state(level,n,"Original");var to=state(level,n,"Placed");
            var current=level.getBlockState(at);
            var decision=TunnelPassageTransition.decide(false,true,current.equals(from),current.equals(to),!collisionOccupied(level,at,to));
            if(decision==TunnelPassageTransition.Decision.VERIFIED)continue;
            if(decision==TunnelPassageTransition.Decision.WAIT){built=false;continue;}
            level.setBlock(at,to,2|16);
            if(!level.getBlockState(at).equals(to))built=false;
        }
        for(int distance=1;distance<=3;distance++){
            var at=cell(origin,facing,distance);
            if(!level.hasChunkAt(at)||!level.getBlockState(at).isAir())built=false;
        }
        if(wasBuilt!=built){
            door.passageBuilt=built;door.setChanged();
            level.sendBlockUpdated(origin,door.getBlockState(),door.getBlockState(),3);
        }
        return built;
    }

    private static CompoundTag entry(BlockPos pos,net.minecraft.world.level.block.state.BlockState original,
            net.minecraft.world.level.block.state.BlockState placed){
        var row=new CompoundTag();row.putLong("Pos",pos.asLong());
        row.put("Original",NbtUtils.writeBlockState(original));row.put("Placed",NbtUtils.writeBlockState(placed));return row;
    }
    private static net.minecraft.world.level.block.state.BlockState state(ServerLevel level,CompoundTag row,String key){
        return NbtUtils.readBlockState(level.holderLookup(net.minecraft.core.registries.Registries.BLOCK),row.getCompound(key));
    }
    private static boolean validEntry(CompoundTag entry){
        return entry.contains("Pos",Tag.TAG_LONG)&&entry.getCompound("Original").contains("Name",Tag.TAG_STRING)
                &&entry.getCompound("Placed").contains("Name",Tag.TAG_STRING);
    }

    private static boolean validCell(BlockPos origin,Direction facing,BlockPos at){
        var delta=at.subtract(origin);
        int depth=-delta.getX()*facing.getStepX()-delta.getZ()*facing.getStepZ();
        int side=delta.getX()*facing.getStepZ()-delta.getZ()*facing.getStepX();
        return depth>=1&&depth<=4&&Math.abs(side)<=1&&Math.abs(delta.getY())<=1;
    }

    public static boolean restore(ServerLevel level,DoorBlockEntity door){
        var origin=door.getBlockPos();
        var facing=door.getBlockState().getValue(TinyDoorBlock.FACING);
        var journal=TunnelPassageJournal.get(level);
        if(!journal.usable())return false;
        String key=TunnelPassageJournal.key(level,origin);
        var row=journal.rows.getCompound(key);
        if(row.isEmpty()){
            if(door.passageOriginals.isEmpty()){door.passageBuilt=false;door.setChanged();return true;}
            if(door.doorId==null)return false;
            row.putUUID("Door",door.doorId);row.putLong("Origin",origin.asLong());row.putString("Level",level.dimension().location().toString());
            row.putString("Facing",facing.getName());row.put("Cells",door.passageOriginals.copy());
            journal.rows.put(key,row);
        }else if(!row.hasUUID("Door")||!row.getUUID("Door").equals(door.doorId))return false;
        if(!row.getBoolean("Restoring")){
            row.putBoolean("Restoring",true);
            if(!journal.checkpoint(level)){row.remove("Restoring");journal.setDirty();return false;}
        }
        door.passageBuilt=false;door.setChanged();
        return restoreRow(level,journal,key,row,door);
    }

    private static boolean restoreRow(ServerLevel level,TunnelPassageJournal journal,String key,CompoundTag row,DoorBlockEntity door){
        var origin=BlockPos.of(row.getLong("Origin"));
        var facing=Direction.byName(row.getString("Facing"));
        if(facing==null||!facing.getAxis().isHorizontal()||!row.contains("Cells",Tag.TAG_LIST))return false;
        boolean pending=false;
        for(var value:row.getList("Cells",Tag.TAG_COMPOUND)){
            var n=(CompoundTag)value;var at=BlockPos.of(n.getLong("Pos"));
            if(!validEntry(n)||!validCell(origin,facing,at)||!level.hasChunkAt(at)){pending=true;continue;}
            var placed=state(level,n,"Placed");var original=state(level,n,"Original");
            var current=level.getBlockState(at);
            var decision=TunnelPassageTransition.decide(true,true,current.equals(original),current.equals(placed),!collisionOccupied(level,at,original));
            if(decision==TunnelPassageTransition.Decision.VERIFIED)continue;
            if(decision==TunnelPassageTransition.Decision.WAIT){pending=true;continue;}
            level.setBlock(at,original,2|16);
            if(!level.getBlockState(at).equals(original))pending=true;
        }
        if(pending)return false;
        // The intent survives until the affected chunk writes have been flushed.
        if(!row.getList("Cells",Tag.TAG_COMPOUND).isEmpty())level.getChunkSource().save(true);
        if(door!=null){door.passageOriginals=new ListTag();door.setChanged();}
        journal.rows.remove(key);
        if(!journal.checkpoint(level)){journal.rows.put(key,row);journal.setDirty();return false;}
        return true;
    }

    public static boolean hasJournal(ServerLevel level,DoorBlockEntity door){
        return door.passageBuilt||!door.passageOriginals.isEmpty()
                ||TunnelPassageJournal.get(level).rows.contains(TunnelPassageJournal.key(level,door.getBlockPos()));
    }

    private static boolean collisionOccupied(ServerLevel level,BlockPos at,net.minecraft.world.level.block.state.BlockState state){
        for(var part:state.getCollisionShape(level,at).toAabbs()){
            if(occupied(level,part.move(at)))return true;
        }
        return false;
    }

    private static boolean occupied(ServerLevel level,AABB bounds){
        return !level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,bounds,
                actor->actor.isAlive()&&(!(actor instanceof Player player)||!player.isSpectator())).isEmpty();
    }

    public static boolean occupied(ServerLevel level,DoorBlockEntity door){
        var at=door.getBlockPos();
        var facing=door.getBlockState().getValue(TinyDoorBlock.FACING);
        if(collisionOccupied(level,at,door.getBlockState().setValue(TinyDoorBlock.OPEN,false)))return true;
        var row=TunnelPassageJournal.get(level).rows.getCompound(TunnelPassageJournal.key(level,at));
        var cells=row.isEmpty()?door.passageOriginals:row.getList("Cells",Tag.TAG_COMPOUND);
        for(var value:cells){
            var n=(CompoundTag)value;var pos=BlockPos.of(n.getLong("Pos"));
            if(!validEntry(n)||!validCell(at,facing,pos)||!level.hasChunkAt(pos))continue;
            if(level.getBlockState(pos).equals(state(level,n,"Placed"))&&collisionOccupied(level,pos,state(level,n,"Original")))return true;
        }
        return false;
    }

    @SubscribeEvent public static void cleanupOrphaned(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||event.getServer().getTickCount()%40!=0)return;
        var journal=TunnelPassageJournal.get(event.getServer().overworld());
        if(!journal.usable())return;
        var keys=java.util.List.copyOf(journal.rows.getAllKeys());
        for(int i=0;i<Math.min(8,keys.size());i++){
            var key=keys.get(Math.floorMod(journal.orphanCursor++,keys.size()));
            var row=journal.rows.getCompound(key);
            var location=net.minecraft.resources.ResourceLocation.tryParse(row.getString("Level"));
            if(location==null)continue;
            var level=event.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,location));
            var at=BlockPos.of(row.getLong("Origin"));
            if(level==null||!level.hasChunkAt(at)||!key.equals(TunnelPassageJournal.key(level,at)))continue;
            if(level.getBlockEntity(at) instanceof DoorBlockEntity door&&row.hasUUID("Door")&&row.getUUID("Door").equals(door.doorId))continue;
            if(!row.getBoolean("Restoring")){
                row.putBoolean("Restoring",true);
                if(!journal.checkpoint(level)){row.remove("Restoring");journal.setDirty();continue;}
            }
            restoreRow(level,journal,key,row,null);
        }
    }

    private static DoorBlockEntity nearby(Player p){
        var origin=p.blockPosition();DoorBlockEntity best=null;double distance=Double.MAX_VALUE;
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-1;y<=1;y++){
            var pos=origin.offset(x,y,z);var state=p.level().getBlockState(pos);
            if(!state.is(ModBlocks.TINY_DOOR.get())||!state.getValue(TinyDoorBlock.OPEN)||!state.getValue(TinyDoorBlock.PORTAL))continue;
            var facing=state.getValue(TinyDoorBlock.FACING);double depth=depth(p,pos,facing),lateral=Math.abs((p.getX()-pos.getX()-.5)*facing.getStepZ()-(p.getZ()-pos.getZ()-.5)*facing.getStepX());
            if(depth<-.95||depth>3.5||lateral>.68||Math.abs(p.getY()-pos.getY())>.55)continue;
            if(p.level().getBlockEntity(pos) instanceof DoorBlockEntity door&&!door.hasPostgameBinding()&&p.distanceToSqr(pos.getCenter())<distance){best=door;distance=p.distanceToSqr(pos.getCenter());}
        }return best;
    }
    public static void tick(Player p){
        var tag=p.getPersistentData();var door=nearby(p);
        if(door!=null&&door.storyPassageBlocked){if(tag.getBoolean("CoralineAutoCrawl")){tag.remove("CoralineAutoCrawl");if(p.getForcedPose()==Pose.SWIMMING)p.setForcedPose(null);}return;}
        boolean fabric=p.level().getBlockState(p.blockPosition().above()).is(ModBlocks.TUNNEL_FABRIC.get())&&(p.level().getBlockState(p.blockPosition().below()).is(ModBlocks.TUNNEL_FABRIC.get())||p.level().getBlockState(p.blockPosition().below()).is(ModBlocks.TUNNEL_CORE.get()));
        if(door!=null||fabric||tag.getBoolean("CoralineInTunnel")){tag.putBoolean("CoralineAutoCrawl",true);p.setForcedPose(Pose.SWIMMING);p.setPose(Pose.SWIMMING);}
        else if(tag.getBoolean("CoralineAutoCrawl")){tag.remove("CoralineAutoCrawl");if(p.getForcedPose()==Pose.SWIMMING)p.setForcedPose(null);}
        if(!(p instanceof ServerPlayer server))return;
        if(tag.contains("CoralineExitLead")){
            var pos=BlockPos.of(tag.getLong("CoralineExitLead"));var state=server.serverLevel().getBlockState(pos);
            if(!state.is(ModBlocks.TINY_DOOR.get())||depth(p,pos,state.getValue(TinyDoorBlock.FACING))<-.95){
                tag.remove("CoralineExitLead");
                if(server.serverLevel().getBlockEntity(pos) instanceof DoorBlockEntity exit){
                    if(tag.getBoolean("CoralineExitClose")){
                        var record=DoorData.get(server.server).doors.get(exit.doorId);if(record!=null){record.closedNight=Math.floorDiv(server.level().getDayTime(),24000);record.activeUntil=0;DoorData.get(server.server).setDirty();}
                        server.serverLevel().setBlock(pos,state.setValue(TinyDoorBlock.OPEN,false).setValue(TinyDoorBlock.PORTAL,false),3);restore(server.serverLevel(),exit);
                    }
                }
                tag.remove("CoralineExitClose");
            }return; // Walking out must not immediately begin another crossing.
        }
        if(door==null||tag.getBoolean("CoralineInTunnel"))return;
        if(!ensure(server.serverLevel(),door))return;
        long now=server.server.overworld().getGameTime();
        if(depth(p,door.getBlockPos(),door.getBlockState().getValue(TinyDoorBlock.FACING))>=TRANSFER_DEPTH&&now>=tag.getLong("CoralinePortalCooldown")&&now>=tag.getLong("CoralineTunnelAttempt")){
            tag.putLong("CoralineTunnelAttempt",now+20);tag.putBoolean("CoralineSeamlessTunnel",true);OtherWorld.enter(server,door);
            if(!tag.getBoolean("CoralineInTunnel"))tag.remove("CoralineSeamlessTunnel");
        }
    }
    @SubscribeEvent public static void clientTick(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END&&event.player.level().isClientSide)tick(event.player);}
    public static Vec3 turn(Vec3 velocity,float degrees){return velocity.yRot((float)-Math.toRadians(degrees));}
    private TunnelPassage(){}
}
