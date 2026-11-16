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
    public static boolean ensure(ServerLevel level,DoorBlockEntity door){
        if(door.doorId==null)return false;
        if(door.passageBuilt&&door.passageVersion>=4)return true;
        // Undo only our journalled old lining, never arbitrary nearby fabric or
        // player edits. Existing open doors migrate as well as new placements.
        if(door.passageBuilt)restore(level,door);
        var facing=door.getBlockState().getValue(TinyDoorBlock.FACING);var right=facing.getClockWise();var origin=door.getBlockPos();
        var originals=new ListTag();
        for(int d=1;d<=4;d++)for(int side=-1;side<=1;side++)for(int y=-1;y<=1;y++){
            if(d==4&&(side!=0||y!=0))continue;
            var pos=origin.relative(facing,-d).relative(right,side).offset(0,y,0);level.getChunkAt(pos);
            if(level.isOutsideBuildHeight(pos))return false;
            // Never remove an inventory, another door, or other live block entity.
            if(level.getBlockEntity(pos)!=null)return false;
            var original=level.getBlockState(pos);var entry=new CompoundTag();entry.putLong("Pos",pos.asLong());entry.put("Original",NbtUtils.writeBlockState(original));
            // A real, sealed crawl passage. Its journal restores every replaced
            // block when the door closes, including terrain behind the entrance.
            var state=side==0&&y==0&&d<4?Blocks.AIR.defaultBlockState():ModBlocks.TUNNEL_FABRIC.get().defaultBlockState().setValue(ModBlocks.TUNNEL_LINED,true);
            entry.put("Placed",NbtUtils.writeBlockState(state));originals.add(entry);
        }
        door.passageOriginals=originals;door.passageBuilt=true;door.passageVersion=4;door.setChanged();
        for(var value:originals){var n=(CompoundTag)value;level.setBlock(BlockPos.of(n.getLong("Pos")),NbtUtils.readBlockState(level.holderLookup(net.minecraft.core.registries.Registries.BLOCK),n.getCompound("Placed")),2|16);}
        level.sendBlockUpdated(origin,door.getBlockState(),door.getBlockState(),3);return true;
    }
    public static void restore(ServerLevel level,DoorBlockEntity door){
        for(var value:door.passageOriginals){var n=(CompoundTag)value;var pos=BlockPos.of(n.getLong("Pos"));var lookup=level.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
            if(level.getBlockState(pos).equals(NbtUtils.readBlockState(lookup,n.getCompound("Placed"))))level.setBlock(pos,NbtUtils.readBlockState(lookup,n.getCompound("Original")),2|16);
        }
        door.passageOriginals=new ListTag();door.passageBuilt=false;door.setChanged();
    }
    public static boolean occupied(ServerLevel level,DoorBlockEntity door){var facing=door.getBlockState().getValue(TinyDoorBlock.FACING);var end=inside(door.getBlockPos(),facing,2);
        return !level.getEntitiesOfClass(Player.class,new AABB(end.x-.7,end.y,end.z-.7,end.x+.7,end.y+1,end.z+.7).inflate(1.3,0,1.3)).isEmpty();
    }
    private static DoorBlockEntity nearby(Player p){
        var origin=p.blockPosition();DoorBlockEntity best=null;double distance=Double.MAX_VALUE;
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-1;y<=1;y++){
            var pos=origin.offset(x,y,z);var state=p.level().getBlockState(pos);
            if(!state.is(ModBlocks.TINY_DOOR.get())||!state.getValue(TinyDoorBlock.OPEN)||!state.getValue(TinyDoorBlock.PORTAL))continue;
            var facing=state.getValue(TinyDoorBlock.FACING);double depth=depth(p,pos,facing),lateral=Math.abs((p.getX()-pos.getX()-.5)*facing.getStepZ()-(p.getZ()-pos.getZ()-.5)*facing.getStepX());
            if(depth<-.95||depth>3.5||lateral>.68||Math.abs(p.getY()-pos.getY())>.55)continue;
            if(p.level().getBlockEntity(pos) instanceof DoorBlockEntity door&&p.distanceToSqr(pos.getCenter())<distance){best=door;distance=p.distanceToSqr(pos.getCenter());}
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
