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
        if(!data.contains("WebCells")){data.remove("WebReady");data.remove("WebBuild");}
        if(!data.contains("PassageCells")){data.remove("PassageReady");data.remove("PassageBuild");}
        // The persistent Hand, original Cat, original Mother and child UUIDs are
        // deliberately absent. Subsequent chapters still own those identities.
        for(String key:new String[]{"Web","WebDoor","PursuingDoor","ThrownStone","PresentedEye0","PresentedEye1","PresentedEye2","WeakPoint"})RescueProps.retire(player,scene,key);
    }
    private static int drain(ServerPlayer player,VisitData.Scene scene,String key,int budget){
        var data=RescueChapter.data(scene);if(!data.contains(key)||budget==0)return budget;
        var level=player.server.getLevel(ActivityWorld.KEY);if(level==null)return budget;
        var cells=data.getCompound(key);var lookup=level.holderLookup(Registries.BLOCK);
        // Copy only the bounded worklist, not the complete several-thousand-cell journal.
        var work=new java.util.ArrayList<String>(budget);
        for(String position:cells.getAllKeys()){work.add(position);if(work.size()==budget)break;}
        for(String position:work){var at=BlockPos.of(Long.parseLong(position));level.getChunkAt(at);var entry=cells.getCompound(position);
            if(entry.contains("Placed")&&level.getBlockEntity(at)==null&&level.getBlockState(at).equals(NbtUtils.readBlockState(lookup,entry.getCompound("Placed"))))level.setBlock(at,NbtUtils.readBlockState(lookup,entry.getCompound("Original")),2);
            cells.remove(position);budget--;
        }
        if(cells.isEmpty())data.remove(key);
        if(!work.isEmpty())OtherMotherLoop.changed(player);
        return budget;
    }
    private RescueSiteCleanup(){}
}
