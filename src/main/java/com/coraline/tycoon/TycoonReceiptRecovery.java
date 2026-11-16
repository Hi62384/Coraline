package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Recover from native player-file evidence, including revoked/offline actors.
 * This code never loads an inventory snapshot into a player or issues items. */
@Mod.EventBusSubscriber(modid="coraline")
public final class TycoonReceiptRecovery {
    static void prepare(ServerPlayer p,TycoonData.Home h,CompoundTag intent,String receiptKey){
        intent.putUUID("Actor",p.getUUID());intent.putUUID("Owner",h.owner);if(h.state.hasUUID("CapabilityEpoch"))intent.putUUID("Epoch",h.state.getUUID("CapabilityEpoch"));
        intent.put("BeforeInventory",p.getInventory().save(new ListTag()));var data=p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);if(data.hasUUID(receiptKey))intent.putUUID("BeforeReceipt",data.getUUID(receiptKey));else intent.putBoolean("BeforeReceiptAbsent",true);
    }
    static boolean actorBusy(ServerPlayer p,TycoonData.Home current){
        for(var h:TycoonData.get(p.server).homes.values())if(h!=current)for(String key:List.of("PendingTransaction","PendingPlacement"))if(h.state.contains(key,Tag.TAG_COMPOUND)){var n=h.state.getCompound(key);if((n.hasUUID("Actor")?n.getUUID("Actor"):h.owner).equals(p.getUUID()))return true;}return false;
    }
    private static void audit(TycoonData.Home h,CompoundTag intent,UUID actor,long now,String kind){
        var list=h.state.getList("TransactionAudit",Tag.TAG_COMPOUND);if(list.stream().anyMatch(raw->((CompoundTag)raw).hasUUID("Receipt")&&((CompoundTag)raw).getUUID("Receipt").equals(intent.getUUID("Receipt"))))return;
        var row=new CompoundTag();row.putUUID("Receipt",intent.getUUID("Receipt"));row.putUUID("Actor",actor);row.putUUID("Owner",h.owner);row.putString("Kind",kind);row.putInt("Cost",intent.getInt(kind.equals("PLACEMENT")?"Fee":"Cost"));row.putLong("At",now);if(list.size()>=256)list.remove(0);list.add(row);h.state.put("TransactionAudit",list);
    }
    static boolean recover(MinecraftServer server,TycoonData.Home h,String key){
        if(!server.isSameThread()||!h.state.contains(key,Tag.TAG_COMPOUND))return false;var intent=h.state.getCompound(key);if(!intent.hasUUID("Receipt"))return false;
        UUID actor=intent.hasUUID("Actor")?intent.getUUID("Actor"):h.owner;
        if(intent.hasUUID("Owner")&&!intent.getUUID("Owner").equals(h.owner)||intent.hasUUID("Epoch")&&(!h.state.hasUUID("CapabilityEpoch")||!intent.getUUID("Epoch").equals(h.state.getUUID("CapabilityEpoch"))))return false;
        boolean placement=key.equals("PendingPlacement");String receiptKey=placement?"TycoonPlacementReceipt":"TycoonTransactionReceipt";
        try{
            if(!intent.getBoolean("RecoveryApplied")&&!intent.getBoolean("AbortVerified")){
                var live=server.getPlayerList().getPlayer(actor);
                if(live!=null){var marker=live.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);if(marker.hasUUID(receiptKey)&&marker.getUUID(receiptKey).equals(intent.getUUID("Receipt"))&&!TycoonCheckpoints.player(live))return false;}
                var file=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(actor+".dat");if(java.nio.file.Files.isSymbolicLink(file)||java.nio.file.Files.isSymbolicLink(file.getParent()))return false;
                var saved=com.coraline.rebirth.StoryRunStore.read(file);var data=saved.getCompound("ForgeData").getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
                boolean paid=data.hasUUID(receiptKey)&&data.getUUID(receiptKey).equals(intent.getUUID("Receipt"));
                if(paid&&live!=null){var marker=live.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);if(!marker.hasUUID(receiptKey)||!marker.getUUID(receiptKey).equals(intent.getUUID("Receipt")))return false;}
                if(!paid){
                    boolean before=intent.hasUUID("BeforeReceipt")?data.hasUUID(receiptKey)&&data.getUUID(receiptKey).equals(intent.getUUID("BeforeReceipt")):intent.getBoolean("BeforeReceiptAbsent")&&!data.hasUUID(receiptKey);
                    if(!before||!intent.contains("BeforeInventory",Tag.TAG_LIST)||!com.coraline.palace.PalaceKeyCheckpoint.inventory(saved,intent.getList("BeforeInventory",Tag.TAG_COMPOUND)))return false;
                    intent.putBoolean("AbortVerified",true);
                }else{
                    if(placement){var row=intent.getCompound("Resident");if(!row.hasUUID("Id")||!row.hasUUID("Entity"))return false;
                        if(intent.contains("WarehouseAfter",Tag.TAG_COMPOUND))h.state.put("Warehouse",intent.getCompound("WarehouseAfter").copy());
                        var id=row.getUUID("Id");h.residents.putIfAbsent(id,new TycoonData.Resident(id,row.copy()));h.state.getCompound("Candidates").remove(id.toString());h.state.putLong("PlacementAfter",Math.max(h.state.getLong("PlacementAfter"),intent.getLong("PlacementAfter")));h.milestone("first_resident",50);
                    }else{var patch=intent.getCompound("HomePatch");TycoonTransactions.applyHomePatch(h,patch);if(intent.hasUUID("Resident")){var r=h.residents.get(intent.getUUID("Resident"));if(r!=null){var values=intent.getCompound("ResidentPatch");for(String field:values.getAllKeys())r.n.put(field,values.get(field).copy());}}if(patch.contains("Terrain")||patch.contains("HouseTier")||patch.contains("SkyTier"))TycoonLandscape.recalculate(h);}
                    audit(h,intent,actor,server.overworld().getGameTime(),placement?"PLACEMENT":"PURCHASE");intent.putBoolean("RecoveryApplied",true);
                }
            }
            // First persist applied metadata together with its journal. Only then
            // release the reservation; an uncertain second save retains it.
            if(!TycoonCheckpoints.home(server,h))return false;
            boolean committed=intent.getBoolean("RecoveryApplied");var retained=intent.copy();h.state.remove(key);TycoonData.get(server).setDirty();
            if(!TycoonCheckpoints.home(server,h)){h.state.put(key,retained);TycoonData.get(server).setDirty();return false;}
            return committed;
        }catch(java.io.IOException|RuntimeException unavailable){return false;}
    }
    public static boolean readyForGlobalReset(MinecraftServer server){
        if(!server.isSameThread())return false;
        for(var h:TycoonData.get(server).homes.values()){recover(server,h,"PendingTransaction");recover(server,h,"PendingPlacement");if(h.state.contains("PendingTransaction")||h.state.contains("PendingPlacement"))return false;}return true;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        var server=event.getServer();if(event.phase!=TickEvent.Phase.END||server.getTickCount()%20!=0||com.coraline.postgame.TrueHeartBattle.resetting(server))return;
        for(var h:TycoonData.get(server).homes.values()){recover(server,h,"PendingTransaction");recover(server,h,"PendingPlacement");}
    }
    private TycoonReceiptRecovery(){}
}
