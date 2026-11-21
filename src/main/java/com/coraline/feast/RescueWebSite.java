package com.coraline.feast;

import com.coraline.block.StoryWebBlock;
import com.coraline.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.Vec3;

/** Journalled incremental owner-allocated web; never edits a neighbour's venue. */
public final class RescueWebSite {
    public static BlockPos center(ServerPlayer p,VisitData.Scene s){return ShowSiteData.get(p.server).center(s.owner,false).offset(150,-40,100);}
    public static Vec3 at(ServerPlayer p,VisitData.Scene s,int lane,double u){var c=center(p,s);var v=RescueWebLayout.point(lane,u);return new Vec3(c.getX()+v.x(),c.getY()+v.y(),c.getZ()+v.z());}
    public static boolean prepare(ServerPlayer p,VisitData.Scene s){
        if(!RescueChapter.owns(p,s))return false;
        var n=RescueChapter.data(s);if(n.getBoolean("WebReady"))return verify(p,s);var l=p.server.getLevel(ActivityWorld.KEY);if(l==null)return false;
        int cursor=n.getInt("WebBuild"),end=Math.min(3*(RescueWebLayout.SAMPLES+1),cursor+54);
        var journal=n.getCompound("WebCells");n.put("WebCells",journal);
        for(;cursor<end;cursor++){int lane=cursor/(RescueWebLayout.SAMPLES+1),sample=cursor%(RescueWebLayout.SAMPLES+1);var point=at(p,s,lane,sample/(double)RescueWebLayout.SAMPLES);if(!floor(l,journal,point))return blocked(p,s,cursor);
            if(sample%(RescueWebLayout.SAMPLES/6)==0&&sample>0&&sample<RescueWebLayout.SAMPLES&&lane<2){var other=at(p,s,lane+1,sample/(double)RescueWebLayout.SAMPLES);for(int i=0;i<=12;i++)if(!floor(l,journal,point.lerp(other,i/12.0)))return blocked(p,s,cursor);}
        }
        n.put("WebCells",journal);n.putInt("WebBuild",cursor);
        if(cursor==3*(RescueWebLayout.SAMPLES+1)){for(double u:new double[]{0,1}){var v=at(p,s,1,u);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(!floor(l,journal,v.add(x,0,z)))return blocked(p,s,cursor);}n.remove("Blocker");n.putBoolean("WebReady",true);n.remove("WebVerified");n.putInt("WebVerify",0);OtherMotherLoop.changed(p);}
        return n.getBoolean("WebReady")&&verify(p,s);
    }
    /** A saved WebReady flag is not proof that its physical journalled blocks
     * survived a reload, an interrupted build or an unrelated later edit.
     * Verification runs in small bounded batches before any fresh web entry. */
    static boolean webVerificationRequired(boolean ready,boolean verified,int receipts){
        return ready&&(!verified||receipts<=0);
    }
    /** Reconstruct the authored web's EXPECTED receipt coordinates without
     * mutating the world. A partial saved journal cannot certify WebReady just
     * because the handful of surviving rows happen to validate. */
    private static java.util.Set<Long> expectedCells(ServerPlayer p,VisitData.Scene s){
        var expected=new java.util.HashSet<Long>();
        int samples=RescueWebLayout.SAMPLES;
        for(int lane=0;lane<RescueWebLayout.LANES;lane++)
            for(int sample=0;sample<=samples;sample++){
                var point=at(p,s,lane,sample/(double)samples);
                expected.add(BlockPos.containing(point.x,point.y-.001,point.z).asLong());
                if(sample%(samples/6)==0&&sample>0&&sample<samples&&lane<RescueWebLayout.LANES-1){
                    var adjacent=at(p,s,lane+1,sample/(double)samples);
                    for(int i=0;i<=12;i++){
                        var cross=point.lerp(adjacent,i/12.0);
                        expected.add(BlockPos.containing(cross.x,cross.y-.001,cross.z).asLong());
                    }
                }
            }
        for(double u:new double[]{0,1}){
            var endpoint=at(p,s,1,u);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
                var point=endpoint.add(x,0,z);
                expected.add(BlockPos.containing(point.x,point.y-.001,point.z).asLong());
            }
        }
        return expected;
    }
    private static boolean verify(ServerPlayer p,VisitData.Scene s){
        var n=RescueChapter.data(s);var cells=n.getCompound("WebCells");
        var keys=new java.util.ArrayList<>(cells.getAllKeys());
        if(!webVerificationRequired(n.getBoolean("WebReady"),n.getBoolean("WebVerified"),keys.size()))return true;
        if(keys.isEmpty()){n.putString("Blocker","The original web receipts are missing; cannot trust its saved ready flag.");return false;}
        // Check geometry coverage, not merely the rows that happened to be
        // persisted. A partial WebReady save may omit entire cross-threads.
        var expected=expectedCells(p,s);
        for(long packed:expected)if(!cells.contains(Long.toString(packed))){
            n.putString("Blocker","Saved web geometry is incomplete; the original cell journal must be restored.");
            return false;
        }
        var level=p.server.getLevel(ActivityWorld.KEY);if(level==null)return false;
        keys.sort(String::compareTo);
        int cursor=n.getInt("WebVerify");
        if(cursor<0||cursor>keys.size()){n.putString("Blocker","The saved web verification cursor is invalid.");return false;}
        int end=Math.min(keys.size(),cursor+96);
        var lookup=level.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        for(int i=cursor;i<end;i++){
            String key=keys.get(i);long packed;
            try{packed=Long.parseLong(key);}catch(NumberFormatException invalid){
                n.putString("Blocker","Invalid saved web cell reference.");return false;
            }
            var pos=BlockPos.of(packed);
            if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos)){
                n.putString("Blocker","A saved web cell is outside the permitted world.");return false;
            }
            var receipt=cells.getCompound(key);
            if(!receipt.contains("Original",Tag.TAG_COMPOUND)||!receipt.contains("Placed",Tag.TAG_COMPOUND)){
                n.putString("Blocker","The original web cell journal is incomplete at "+pos.toShortString());return false;
            }
            level.getChunkAt(pos);
            var original=NbtUtils.readBlockState(lookup,receipt.getCompound("Original"));
            var placed=NbtUtils.readBlockState(lookup,receipt.getCompound("Placed"));
            var previous=receipt.contains("PreviousPlaced",Tag.TAG_COMPOUND)
                    ?NbtUtils.readBlockState(lookup,receipt.getCompound("PreviousPlaced")):null;
            var actual=level.getBlockState(pos);
            if(level.getBlockEntity(pos)!=null||!actual.equals(placed)
                    &&!actual.equals(original)&&(previous==null||!actual.equals(previous))){
                n.putString("Blocker","The web at "+pos.toShortString()+" contains an unrelated edit. It was preserved.");return false;
            }
            if(!actual.equals(placed)){
                level.setBlock(pos,placed,2);
                if(!level.getBlockState(pos).equals(placed)){
                    n.putString("Blocker","Unable to restore the original web at "+pos.toShortString());return false;
                }
            }
            // The new state was read back; the interrupted older-state
            // receipt no longer needs to be kept alongside the committed one.
            receipt.remove("PreviousPlaced");
            n.putInt("WebVerify",i+1);
        }
        if(end==keys.size()){n.putBoolean("WebVerified",true);n.remove("WebVerify");n.remove("Blocker");}
        OtherMotherLoop.changed(p);
        return n.getBoolean("WebVerified");
    }
    private static boolean blocked(ServerPlayer p,VisitData.Scene s,int cursor){var n=RescueChapter.data(s);n.putInt("WebBuild",cursor);n.putString("Blocker","The owner-allocated web route is obstructed; existing blocks were preserved.");OtherMotherLoop.changed(p);return false;}
    private static boolean floor(ServerLevel l,CompoundTag journal,Vec3 point){
        var pos=BlockPos.containing(point.x,point.y-.001,point.z);
        l.getChunkAt(pos);
        var old=l.getBlockState(pos);
        String key=Long.toString(pos.asLong());
        boolean recorded=journal.contains(key);
        if(!recorded&&!old.isAir())return false; // Do not claim someone else's web/block.
        var receipt=journal.getCompound(key);
        var lookup=l.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        if(recorded){
            if(!receipt.contains("Original",Tag.TAG_COMPOUND))return false;
            var original=NbtUtils.readBlockState(lookup,receipt.getCompound("Original"));
            boolean match=old.equals(original);
            if(receipt.contains("Placed",Tag.TAG_COMPOUND))
                match|=old.equals(NbtUtils.readBlockState(lookup,receipt.getCompound("Placed")));
            if(receipt.contains("PreviousPlaced",Tag.TAG_COMPOUND))
                match|=old.equals(NbtUtils.readBlockState(lookup,receipt.getCompound("PreviousPlaced")));
            if(!match||l.getBlockEntity(pos)!=null)return false;
        }
        int height=Math.max(1,Math.min(16,(int)Math.ceil((point.y-pos.getY())*16)));
        if(old.is(ModBlocks.STORY_WEB.get()))height=Math.max(height,old.getValue(StoryWebBlock.HEIGHT));
        if(!recorded){
            var cell=new CompoundTag();
            cell.put("Original",NbtUtils.writeBlockState(old));
            journal.put(key,cell);
            receipt=cell;
        }
        var state=ModBlocks.STORY_WEB.get().defaultBlockState().setValue(StoryWebBlock.HEIGHT,height);
        // The previous owned state is recoverable if saving interrupts after
        // a new placement intent but before the block change is committed.
        receipt.put("PreviousPlaced",NbtUtils.writeBlockState(old));
        receipt.putInt("RestHeight",height);
        receipt.put("Placed",NbtUtils.writeBlockState(state));
        l.setBlock(pos,state,2);
        if(!l.getBlockState(pos).equals(state))return false;
        receipt.remove("PreviousPlaced");
        LoopProps.hold(l,pos);
        return true;
    }
    /** A quiet, bounded weight depression changes the same tops the mesh reads. */
    public static void sag(ServerPlayer p,VisitData.Scene s){
        if(!RescueChapter.owns(p,s)||p.tickCount%4!=0||p.serverLevel()!=p.server.getLevel(ActivityWorld.KEY))return;var n=RescueChapter.data(s);if(!n.getBoolean("WebReady"))return;var l=p.serverLevel();var cells=n.getCompound("WebCells");var lookup=l.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        for(String key:cells.getAllKeys()){var at=BlockPos.of(Long.parseLong(key));if(!l.hasChunkAt(at))continue;
            var cell=cells.getCompound(key);var old=l.getBlockState(at);
            if(!old.is(ModBlocks.STORY_WEB.get())||!cell.contains("Placed",Tag.TAG_COMPOUND))continue;
            var saved=NbtUtils.readBlockState(lookup,cell.getCompound("Placed"));
            if(!old.equals(saved)){
                // A crash may have saved the intended sag but not its physical
                // block. Resume only a matching previously-owned web cell.
                if(cell.contains("PreviousPlaced",Tag.TAG_COMPOUND)
                        &&old.equals(NbtUtils.readBlockState(lookup,cell.getCompound("PreviousPlaced")))){
                    l.setBlock(at,saved,2);
                    if(l.getBlockState(at).equals(saved))cell.remove("PreviousPlaced");
                }
                continue;
            }
            int current=old.getValue(StoryWebBlock.HEIGHT);if(!cell.contains("RestHeight"))cell.putInt("RestHeight",current);int rest=cell.getInt("RestHeight");double top=at.getY()+rest/16.;double distance=Math.hypot(p.getX()-at.getX()-.5,p.getZ()-at.getZ()-.5);boolean bearing=Math.abs(p.getY()-top)<.35&&Math.abs(p.getDeltaMovement().y)<.2;int target=RescueWebLayout.weightedHeight(rest,distance,bearing);int height=current+Integer.signum(target-current);
            if(height!=current){
                var state=old.setValue(StoryWebBlock.HEIGHT,height);
                cell.put("PreviousPlaced",NbtUtils.writeBlockState(old));
                cell.put("Placed",NbtUtils.writeBlockState(state));
                l.setBlock(at,state,2);
                if(l.getBlockState(at).equals(state))cell.remove("PreviousPlaced");
            }
        }
    }
    public static void restore(ServerPlayer p,VisitData.Scene s){if(!RescueChapter.owns(p,s))return;var l=p.server.getLevel(ActivityWorld.KEY);if(l==null)return;var n=RescueChapter.data(s);var cells=n.getCompound("WebCells");var lookup=l.holderLookup(net.minecraft.core.registries.Registries.BLOCK);for(String key:cells.getAllKeys()){var pos=BlockPos.of(Long.parseLong(key));l.getChunkAt(pos);var cell=cells.getCompound(key);if(l.getBlockEntity(pos)==null&&l.getBlockState(pos).equals(NbtUtils.readBlockState(lookup,cell.getCompound("Placed"))))l.setBlock(pos,NbtUtils.readBlockState(lookup,cell.getCompound("Original")),2);}n.remove("WebCells");n.remove("WebBuild");n.remove("WebReady");n.remove("WebVerified");n.remove("WebVerify");n.remove("WebEntryRecheck");OtherMotherLoop.changed(p);}
    private RescueWebSite(){}
}
