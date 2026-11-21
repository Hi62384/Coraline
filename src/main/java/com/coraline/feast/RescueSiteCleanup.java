package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;

/** Bounded retirement after the real exit; journals survive logout and foreign edits. */
public final class RescueSiteCleanup {
    public static void tick(ServerPlayer player,VisitData.Scene scene){
        if(!RescueChapter.owns(player,scene))return;
        var data=RescueChapter.data(scene);
        if(!data.getBoolean("CrawlReachedOverworld")||player.serverLevel()==player.server.getLevel(ActivityWorld.KEY))return;
        int remaining=96;
        remaining=drain(player,scene,"WebCells",remaining);
        drain(player,scene,"PassageCells",remaining);
        if(!data.contains("WebCells")){data.remove("WebReady");data.remove("WebBuild");data.remove("WebVerified");data.remove("WebVerify");data.remove("WebEntryRecheck");}
        if(!data.contains("PassageCells")){data.remove("PassageReady");data.remove("PassageBuild");data.remove("PassageVerified");data.remove("PassageVerify");data.remove("PassageCellsCleanupOffset");}
        // The persistent Hand, original Cat, original Mother and child UUIDs are
        // deliberately absent. Subsequent chapters still own those identities.
        for(String key:new String[]{"Web","WebDoor","PursuingDoor","ThrownStone","PresentedEye0","PresentedEye1","PresentedEye2","WeakPoint"})RescueProps.retire(player,scene,key);
    }
    private static int drain(ServerPlayer player,VisitData.Scene scene,String key,int budget){
        var data=RescueChapter.data(scene);if(!data.contains(key)||budget==0)return budget;
        var level=player.server.getLevel(ActivityWorld.KEY);if(level==null)return budget;
        var cells=data.getCompound(key);var lookup=level.holderLookup(Registries.BLOCK);
        // Rotate through bounded slices, so an unrelated player edit near the
        // start cannot indefinitely starve later cleanup cells.
        var keys=cells.getAllKeys();
        int total=keys.size(),offset=Math.floorMod(data.getInt(key+"CleanupOffset"),Math.max(1,total));
        var work=new java.util.ArrayList<String>(Math.min(total,budget));
        int index=0;
        for(String position:keys){if(index++<offset)continue;work.add(position);if(work.size()>=budget)break;}
        if(work.size()<budget){index=0;for(String position:keys){if(index++>=offset)break;work.add(position);if(work.size()>=budget)break;}}
        data.putInt(key+"CleanupOffset",(offset+work.size())%Math.max(1,total));
        boolean changed=false;
        for(String position:work){
            var at=BlockPos.of(Long.parseLong(position));level.getChunkAt(at);
            var entry=cells.getCompound(position);
            if(!entry.contains("Placed",net.minecraft.nbt.Tag.TAG_COMPOUND)
                    ||!entry.contains("Original",net.minecraft.nbt.Tag.TAG_COMPOUND)
                    ||level.getBlockEntity(at)!=null)continue;
            var placed=NbtUtils.readBlockState(lookup,entry.getCompound("Placed"));
            var original=NbtUtils.readBlockState(lookup,entry.getCompound("Original"));
            var previous=entry.contains("PreviousPlaced",net.minecraft.nbt.Tag.TAG_COMPOUND)
                    ?NbtUtils.readBlockState(lookup,entry.getCompound("PreviousPlaced")):null;
            var current=level.getBlockState(at);
            // A web-sag crash can leave either of two journalled OWNED states.
            // Retire both safely, but never overwrite a third-party edit.
            if(!current.equals(original)){
                if(!current.equals(placed)&&(previous==null||!current.equals(previous)))continue;
                level.setBlock(at,original,2);
                if(!level.getBlockState(at).equals(original))continue;
            }
            cells.remove(position);budget--;changed=true;
        }
        if(cells.isEmpty()){data.remove(key);data.remove(key+"CleanupOffset");}
        if(changed)OtherMotherLoop.changed(player);
        return budget;
    }
    private RescueSiteCleanup(){}
}
