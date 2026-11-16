package com.coraline.feast;

import com.coraline.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Journalled physical 52 m crawl, preloaded before S. No exit before the actual end. */
public final class RescuePassage {
    public static final int LENGTH=52;
    public static BlockPos origin(ServerPlayer p,VisitData.Scene s){return ShowSiteData.get(p.server).center(s.owner,false).offset(120,-65,68);}
    public static boolean prepare(ServerPlayer p,VisitData.Scene s){
        if(!RescueChapter.owns(p,s))return false;
        var n=RescueChapter.data(s);if(n.getBoolean("PassageReady"))return true;var l=p.server.getLevel(ActivityWorld.KEY);if(l==null)return false;var c=origin(p,s);var cells=n.getCompound("PassageCells");n.put("PassageCells",cells);int cursor=n.getInt("PassageBuild"),total=(LENGTH+5)*5*4,end=Math.min(total,cursor+96);
        for(;cursor<end;cursor++){int x=cursor/20-2,z=cursor/4%5-2,y=cursor%4-1;var at=c.offset(x,y,z);LoopProps.hold(l,at);boolean shell=y==-1||y==2||Math.abs(z)==2||x==-2||x==LENGTH+2;var state=(shell?ModBlocks.TUNNEL_FABRIC.get():Blocks.AIR).defaultBlockState();if(!shell&&y==0&&x>7&&x<LENGTH-4&&x%9==0&&z==(x%18==0?-1:1))state=Blocks.MOSS_BLOCK.defaultBlockState();var old=l.getBlockState(at);String key=Long.toString(at.asLong());if(!cells.contains(key)){if(!old.isAir()){n.putInt("PassageBuild",cursor);n.putString("Blocker","The owner-allocated crawl corridor is obstructed; no existing block was overwritten.");OtherMotherLoop.changed(p);return false;}var cell=new CompoundTag();cell.put("Original",NbtUtils.writeBlockState(old));cells.put(key,cell);}else if(!old.equals(NbtUtils.readBlockState(l.holderLookup(net.minecraft.core.registries.Registries.BLOCK),cells.getCompound(key).getCompound("Placed")))){n.putInt("PassageBuild",cursor);n.putString("Blocker","The crawl corridor changed after preparation; the replacement was preserved.");OtherMotherLoop.changed(p);return false;}cells.getCompound(key).put("Placed",NbtUtils.writeBlockState(state));l.setBlock(at,state,2);}
        n.put("PassageCells",cells);n.putInt("PassageBuild",cursor);if(cursor==total){n.putBoolean("PassageReady",true);OtherMotherLoop.changed(p);}return n.getBoolean("PassageReady");
    }
    public static boolean enter(ServerPlayer p,VisitData.Scene s){if(!prepare(p,s))return false;var n=RescueChapter.data(s);var l=p.server.getLevel(ActivityWorld.KEY);var c=origin(p,s);l.getChunkAt(c);p.stopRiding();p.setForcedPose(Pose.SWIMMING);p.setPose(Pose.SWIMMING);p.teleportTo(l,c.getX()+.5,c.getY(),c.getZ()+.5,-90,0);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;n.putBoolean("PassageEntered",true);return true;}
    public static double progress(ServerPlayer p,VisitData.Scene s){return p.getX()-origin(p,s).getX()-.5;}
    public static void restore(ServerPlayer p,VisitData.Scene s){if(!RescueChapter.owns(p,s))return;var l=p.server.getLevel(ActivityWorld.KEY);if(l==null)return;var n=RescueChapter.data(s);var cells=n.getCompound("PassageCells");var lookup=l.holderLookup(net.minecraft.core.registries.Registries.BLOCK);for(String key:cells.getAllKeys()){var at=BlockPos.of(Long.parseLong(key));l.getChunkAt(at);var cell=cells.getCompound(key);if(l.getBlockEntity(at)==null&&l.getBlockState(at).equals(NbtUtils.readBlockState(lookup,cell.getCompound("Placed"))))l.setBlock(at,NbtUtils.readBlockState(lookup,cell.getCompound("Original")),2);}n.remove("PassageCells");n.remove("PassageReady");n.remove("PassageBuild");OtherMotherLoop.changed(p);}
    private RescuePassage(){}
}
