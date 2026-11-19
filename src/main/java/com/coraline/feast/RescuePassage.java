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
    private static final int CELL_COUNT=(LENGTH+5)*5*4,BUILD_BUDGET=96;

    private static BlockPos cell(BlockPos origin,int index){
        return origin.offset(index/20-2,index%4-1,index/4%5-2);
    }
    private static net.minecraft.world.level.block.state.BlockState planned(int index){
        int x=index/20-2,z=index/4%5-2,y=index%4-1;
        boolean shell=y==-1||y==2||Math.abs(z)==2||x==-2||x==LENGTH+2;
        if(!shell&&y==0&&x>7&&x<LENGTH-4&&x%9==0&&z==(x%18==0?-1:1))
            return Blocks.MOSS_BLOCK.defaultBlockState();
        return (shell?ModBlocks.TUNNEL_FABRIC.get():Blocks.AIR).defaultBlockState();
    }
    private static boolean blocked(ServerPlayer p,CompoundTag n,int index,String reason){
        n.putString("Blocker","Rescue passage cell "+index+": "+reason);
        OtherMotherLoop.changed(p);
        return false;
    }
    public static boolean prepare(ServerPlayer p,VisitData.Scene s){
        if(!RescueChapter.owns(p,s))return false;
        var n=RescueChapter.data(s);
        var level=p.server.getLevel(ActivityWorld.KEY);
        if(level==null)return false;
        var origin=origin(p,s);
        var journal=n.getCompound("PassageCells");
        n.put("PassageCells",journal);
        if(n.getBoolean("PassageReady"))return verify(p,n,level,origin,journal);

        int cursor=n.getInt("PassageBuild");
        if(cursor<0||cursor>CELL_COUNT)return blocked(p,n,cursor,"invalid saved construction cursor.");
        int end=Math.min(CELL_COUNT,cursor+BUILD_BUDGET);
        var lookup=level.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        // Record the original state and intended placement for an entire bounded
        // batch before changing blocks. An interrupted batch can be replayed.
        for(int i=cursor;i<end;i++){
            var at=cell(origin,i);LoopProps.hold(level,at);level.getChunkAt(at);
            var expected=planned(i);var actual=level.getBlockState(at);
            var key=Long.toString(at.asLong());
            if(!journal.contains(key)){
                if(!actual.isAir()||level.getBlockEntity(at)!=null)
                    return blocked(p,n,i,"unowned block obstructs the original crawl.");
                var row=new CompoundTag();
                row.put("Original",NbtUtils.writeBlockState(actual));
                row.put("Placed",NbtUtils.writeBlockState(expected));
                journal.put(key,row);
            }else{
                var row=journal.getCompound(key);
                if(!row.contains("Original",Tag.TAG_COMPOUND)||!row.contains("Placed",Tag.TAG_COMPOUND))
                    return blocked(p,n,i,"incomplete saved placement receipt.");
                var owned=NbtUtils.readBlockState(lookup,row.getCompound("Placed"));
                var original=NbtUtils.readBlockState(lookup,row.getCompound("Original"));
                if(!owned.equals(expected)||level.getBlockEntity(at)!=null
                        ||!actual.equals(owned)&&!actual.equals(original))
                    return blocked(p,n,i,"an unrelated edit or conflicting receipt must be preserved.");
            }
        }
        OtherMotherLoop.changed(p);
        p.server.overworld().getDataStorage().save();
        // Advance the durable cursor only after each physical block is confirmed.
        for(int i=cursor;i<end;i++){
            var at=cell(origin,i);var expected=planned(i);
            if(!level.getBlockState(at).equals(expected)){
                level.setBlock(at,expected,2);
                if(!level.getBlockState(at).equals(expected))
                    return blocked(p,n,i,"block placement was refused; the journal is retained.");
            }
            n.putInt("PassageBuild",i+1);
        }
        if(end==CELL_COUNT){n.putBoolean("PassageReady",true);n.remove("PassageVerified");n.putInt("PassageVerify",0);}
        n.remove("Blocker");OtherMotherLoop.changed(p);
        return n.getBoolean("PassageReady")&&verify(p,n,level,origin,journal);
    }

    /** Validate previously written blocks too, including old PassageReady saves
     * that were marked complete without a physical readback pass. */
    private static boolean verify(ServerPlayer p,CompoundTag n,ServerLevel level,BlockPos origin,CompoundTag journal){
        if(n.getBoolean("PassageVerified"))return true;
        var lookup=level.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        int start=n.getInt("PassageVerify");
        if(start<0||start>CELL_COUNT)return blocked(p,n,start,"invalid verification cursor.");
        int end=Math.min(CELL_COUNT,start+BUILD_BUDGET);
        for(int i=start;i<end;i++){
            var at=cell(origin,i);level.getChunkAt(at);
            var row=journal.getCompound(Long.toString(at.asLong()));
            if(!row.contains("Original",Tag.TAG_COMPOUND)||!row.contains("Placed",Tag.TAG_COMPOUND))
                return blocked(p,n,i,"missing original or placement receipt.");
            var expected=planned(i);
            if(!expected.equals(NbtUtils.readBlockState(lookup,row.getCompound("Placed"))))
                return blocked(p,n,i,"the original passage layout does not match its saved receipt.");
            if(!level.getBlockState(at).equals(expected)){
                // An older build may have committed PassageReady after a
                // refused placement. Retry only if the original state remains
                // intact; never replace an unrelated player edit.
                var original=NbtUtils.readBlockState(lookup,row.getCompound("Original"));
                if(level.getBlockEntity(at)!=null||!level.getBlockState(at).equals(original))
                    return blocked(p,n,i,"the physical tunnel differs from the saved layout.");
                level.setBlock(at,expected,2);
                if(!level.getBlockState(at).equals(expected))
                    return blocked(p,n,i,"the original block is still refusing passage construction.");
            }
            n.putInt("PassageVerify",i+1);
        }
        if(end==CELL_COUNT){n.putBoolean("PassageVerified",true);n.remove("Blocker");}
        OtherMotherLoop.changed(p);
        return n.getBoolean("PassageVerified");
    }
    public static boolean enter(ServerPlayer p,VisitData.Scene s){
        if(!prepare(p,s))return false;
        var level=p.server.getLevel(ActivityWorld.KEY);
        if(level==null)return false;
        var origin=origin(p,s);
        level.getChunkAt(origin);
        var feet=Vec3.atBottomCenterOf(origin);
        var body=new net.minecraft.world.phys.AABB(feet.x-.29,feet.y+.01,feet.z-.29,feet.x+.29,feet.y+.59,feet.z+.29);
        if(!level.getWorldBorder().isWithinBounds(origin)||!level.noCollision(p,body)
                ||!level.getFluidState(origin).isEmpty()||!level.getBlockState(origin.below()).isFaceSturdy(level,origin.below(),net.minecraft.core.Direction.UP))return false;
        p.stopRiding();p.setForcedPose(Pose.SWIMMING);p.setPose(Pose.SWIMMING);
        p.teleportTo(level,feet.x,feet.y,feet.z,-90,0);
        if(p.level()!=level||p.position().distanceToSqr(feet)>.25){p.setForcedPose(null);return false;}
        p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        RescueChapter.data(s).putBoolean("PassageEntered",true);
        OtherMotherLoop.changed(p);
        return true;
    }
    public static double progress(ServerPlayer p,VisitData.Scene s){return p.getX()-origin(p,s).getX()-.5;}
    /** Direct restoration uses the same exact-state receipts as the bounded
     * post-exit cleanup and never forgets an unresolved original block. */
    public static void restore(ServerPlayer p,VisitData.Scene s){
        if(!RescueChapter.owns(p,s))return;
        var level=p.server.getLevel(ActivityWorld.KEY);if(level==null)return;
        var n=RescueChapter.data(s);var cells=n.getCompound("PassageCells");
        var lookup=level.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        boolean changed=false;
        for(String key:new java.util.HashSet<>(cells.getAllKeys())){
            var at=BlockPos.of(Long.parseLong(key));level.getChunkAt(at);
            var row=cells.getCompound(key);
            if(!row.contains("Placed",Tag.TAG_COMPOUND)||!row.contains("Original",Tag.TAG_COMPOUND)
                    ||level.getBlockEntity(at)!=null)continue;
            var expected=NbtUtils.readBlockState(lookup,row.getCompound("Placed"));
            var original=NbtUtils.readBlockState(lookup,row.getCompound("Original"));
            var actual=level.getBlockState(at);
            if(!actual.equals(original)){
                if(!actual.equals(expected))continue;
                level.setBlock(at,original,2);
                if(!level.getBlockState(at).equals(original))continue;
            }
            cells.remove(key);changed=true;
        }
        if(cells.isEmpty()){
            n.remove("PassageCells");n.remove("PassageReady");n.remove("PassageBuild");
            n.remove("PassageVerified");n.remove("PassageVerify");
        }
        if(changed)OtherMotherLoop.changed(p);
    }
    private RescuePassage(){}
}
