package com.coraline.tycoon;

import java.io.IOException;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** Paid placement uses a prepared owner row and a matching saved-player receipt.
 * Recovery resumes that exact resident/entity identity; it never charges again.
 */
public final class TycoonPlacements {
    static UUID receipt(ServerPlayer p){var n=p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);return n.hasUUID("TycoonPlacementReceipt")?n.getUUID("TycoonPlacementReceipt"):null;}
    private static void receipt(ServerPlayer p,UUID id){var n=p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);if(id==null)n.remove("TycoonPlacementReceipt");else n.putUUID("TycoonPlacementReceipt",id);p.getPersistentData().put(Player.PERSISTED_NBT_TAG,n);}

    public static String commit(ServerPlayer p,TycoonData.Resident chosen,BlockPos bed,BlockPos arrival,int fee){
        var h=TycoonWorld.home(p);if(TycoonReceiptRecovery.actorBusy(p,h))return "Finish recovery of your earlier household transaction first.";if(h.state.contains("PendingPlacement")||h.state.contains("PendingTransaction"))return "A saved placement is still arriving; no second fee charged.";
        var actor=chosen.n.getBoolean("OriginalGhostHelper")?GhostRescueCandidates.placementActor(p,chosen):TycoonContent.NPC.get().create(p.server.overworld());if(actor==null)return "Waiting for the original resident to load; no fee charged.";
        var row=chosen.save();row.putString("Status","ACTIVE");row.putUUID("Entity",actor.getUUID());row.putLong("Bed",bed.asLong());row.putLong("ArrivedAt",h.clock());row.putLong("LastPosition",arrival.asLong());row.putLong("Arrival",arrival.asLong());row.putString("Task","ARRIVING");row.putBoolean("SpawnPending",true);row.putInt("BodyAdmissionVersion",1);
        var pending=new CompoundTag();pending.putUUID("Receipt",UUID.randomUUID());pending.putUUID("Actor",p.getUUID());pending.putUUID("Owner",h.owner);pending.put("Resident",row);pending.putInt("Fee",fee);pending.putLong("PlacementAfter",h.clock()+TycoonConfig.PLACEMENT_TICKS.get());boolean warehouse=TycoonShop.sharedFunds(p,Items.EMERALD);if(warehouse){var stock=h.state.getCompound("Warehouse").copy();String key=TycoonWarehouse.key(Items.EMERALD);if(stock.getLong(key)<fee)return "Not enough household supply emeralds.";stock.putLong(key,stock.getLong(key)-fee);pending.put("WarehouseAfter",stock);}TycoonReceiptRecovery.prepare(p,h,pending,"TycoonPlacementReceipt");h.state.put("PendingPlacement",pending);
        var inventory=p.getInventory().save(new net.minecraft.nbt.ListTag());var oldReceipt=receipt(p);boolean paid=false,committed=false,saveAttempted=false;
        try{
            if(!TycoonCheckpoints.player(p))throw new IOException("Original actor inventory did not save");
            if(!TycoonCheckpoints.home(p))throw new IOException("Placement intent did not save");
            if(!warehouse&&!TycoonShop.take(p,Items.EMERALD,fee))throw new IOException("Placement fee changed before payment");paid=true;receipt(p,pending.getUUID("Receipt"));
            saveAttempted=true;
            if(!TycoonCheckpoints.player(p))throw new IOException("Placement payment receipt did not save");committed=true;
            if(!reconcile(p))return "Payment saved; this same placement is waiting for its household checkpoint.";spawn(p,h.residents.get(chosen.id),actor);return chosen.name()+" is arriving. One bed assigned; next placement after "+TycoonConfig.PLACEMENT_TICKS.get()/1200+" active minutes.";
        }catch(IOException|RuntimeException failure){
            if(committed||saveAttempted){TycoonWorld.dirty(p);return "Payment retained with its native receipt. This same resident will resume arrival after recovery; no additional fee is needed.";}
            if(paid){p.getInventory().load(inventory);receipt(p,oldReceipt);p.server.getPlayerList().saveAll();}TycoonReceiptRecovery.recover(p.server,h,"PendingPlacement");TycoonWorld.dirty(p);return "Placement paused with its original receipt retained: "+failure.getMessage();
        }
    }

    public static boolean reconcile(ServerPlayer p){return TycoonReceiptRecovery.recover(p.server,TycoonWorld.home(p),"PendingPlacement");}

    private static void spawn(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor)throws IOException{
        if(r==null||!r.active()||!r.n.getBoolean("SpawnPending"))return;
        var at=BlockPos.of(r.n.getLong("Arrival"));var l=p.server.overworld();l.getChunkAt(at);
        var existing=l.getEntity(r.n.getUUID("Entity"));if(!r.n.getBoolean("OriginalGhostHelper")&&existing!=null&&(!(existing instanceof TycoonNpc body)||!body.isAlive()||!TycoonWorld.owner(p).equals(body.owner)||!r.id.equals(body.resident))){r.n.putString("ArrivalBlocker","Original resident identity mismatch; admission paused.");TycoonWorld.dirty(p);return;}
        if(r.n.getBoolean("OriginalGhostHelper")){
            actor=GhostRescueCandidates.placementActor(p,r);if(actor==null||!actor.getUUID().equals(r.n.getUUID("Entity")))return;
            if(actor.level()!=l){
                // Permanent agency custody is independent of the resettable
                // story scene. placementActor authenticated the exact owner,
                // rescue attempt and original actor before this transition.
                var destination=net.minecraft.world.phys.Vec3.atBottomCenterOf(at);
                var moved=actor.changeDimension(l,new net.minecraftforge.common.util.ITeleporter(){
                    @Override public net.minecraft.world.entity.Entity placeEntity(net.minecraft.world.entity.Entity entity,net.minecraft.server.level.ServerLevel from,net.minecraft.server.level.ServerLevel to,float yaw,java.util.function.Function<Boolean,net.minecraft.world.entity.Entity> reposition){
                        var same=reposition.apply(false);if(same!=null)same.setPos(destination);return same;
                    }
                });
                if(!(moved instanceof TycoonNpc same)||!same.getUUID().equals(r.n.getUUID("Entity")))return;actor=same;
            }
            var custody=actor.getPersistentData();
            if(custody.contains("GhostEscapeChild")&&custody.hasUUID("ShowTravelOwner")&&TycoonWorld.owner(p).equals(custody.getUUID("ShowTravelOwner"))){
                custody.remove("GhostEscapeChild");custody.remove("ShowTravelOwner");
                var scene=com.coraline.feast.VisitData.get(p.server).scenes.get(TycoonWorld.owner(p));if(scene!=null&&TycoonWorld.owner(p).equals(scene.owner)){scene.loop.event.getCompound("ShowTravelGuests").remove("GhostEscapeChild:"+actor.getUUID());com.coraline.feast.OtherMotherLoop.changed(p);}
            }
            com.coraline.feast.GhostVictimActors.release(actor);actor.configure(TycoonWorld.owner(p),r.id,false,r.n.getInt("Appearance"));actor.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
            r.n.putString("GhostHelperDimension",l.dimension().location().toString());r.n.putLong("GhostHelperPosition",at.asLong());
        }else if(l.getEntity(r.n.getUUID("Entity"))==null){
            if(r.n.getBoolean("ActorIssued")||r.n.getInt("BodyAdmissionVersion")!=1){r.n.putString("ArrivalBlocker","Waiting for the original resident body; no substitute issued.");TycoonWorld.dirty(p);return;}
            for(var world:p.server.getAllLevels())if(world.getEntity(r.n.getUUID("Entity"))!=null){r.n.putString("ArrivalBlocker","The original resident is in another world; waiting for native custody recovery.");TycoonWorld.dirty(p);return;}
            if(actor==null)actor=TycoonContent.NPC.get().create(l);if(actor==null)return;
            actor.setUUID(r.n.getUUID("Entity"));actor.configure(TycoonWorld.owner(p),r.id,false,r.n.getInt("Appearance"));actor.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
            r.n.putBoolean("ActorIssued",true);TycoonWorld.dirty(p);if(!TycoonCheckpoints.home(p))return;
            if(!l.addFreshEntity(actor)){r.n.putString("ArrivalBlocker","Native body admission was refused; no second body issued.");TycoonWorld.dirty(p);return;}
        }
        // Placement is rare (one active hour): flush entity/chunk state before
        // retiring the recovery marker, rather than saving every AI tick.
        p.server.saveEverything(true,true,true);r.n.putBoolean("SpawnPending",false);TycoonWorld.dirty(p);if(!TycoonCheckpoints.home(p)){r.n.putBoolean("SpawnPending",true);throw new IOException("Arrival checkpoint needs retry");}
    }

    public static void tick(ServerPlayer p){
        var h=TycoonWorld.home(p);reconcile(p);
        for(var r:h.residents.values())if(r.active()&&r.n.getBoolean("SpawnPending")){
            var at=BlockPos.of(r.n.getLong("Arrival"));p.server.overworld().getChunkAt(at);
            // Give vanilla's asynchronous persistent-entity loader time to finish.
            int wait=r.n.getInt("SpawnWait")+20;r.n.putInt("SpawnWait",wait);if(wait<100)continue;
            try{spawn(p,r,null);}catch(IOException failure){r.n.putString("ArrivalBlocker",failure.getMessage());}break;
        }
    }
    private TycoonPlacements(){}
    static String commitSupplied(ServerPlayer p,TycoonData.Resident chosen,BlockPos bed,BlockPos arrival,int fee){
        var h=TycoonWorld.home(p);if(h.state.contains("PendingPlacement")||h.state.contains("PendingTransaction")||h.residents.containsKey(chosen.id)||h.cooldown()>0||TycoonWarehouse.count(h,Items.EMERALD)<fee)return "Placement resources or cooldown unavailable.";
        var actor=chosen.n.getBoolean("OriginalGhostHelper")?GhostRescueCandidates.placementActor(p,chosen):TycoonContent.NPC.get().create(p.server.overworld());if(actor==null)return "Waiting for the original resident to load; no supplies charged.";var row=chosen.save();row.putString("Status","ACTIVE");row.putUUID("Entity",actor.getUUID());row.putLong("Bed",bed.asLong());row.putLong("ArrivedAt",h.clock());row.putLong("LastPosition",arrival.asLong());row.putLong("Arrival",arrival.asLong());row.putString("Task","ARRIVING");row.putBoolean("SpawnPending",true);row.putInt("BodyAdmissionVersion",1);
        TycoonWarehouse.take(h,Items.EMERALD,fee);h.residents.put(chosen.id,new TycoonData.Resident(chosen.id,row));h.state.getCompound("Candidates").remove(chosen.id.toString());h.state.putLong("PlacementAfter",h.clock()+TycoonConfig.PLACEMENT_TICKS.get());h.milestone("first_resident",50);TycoonWorld.dirty(p);
        try{if(!TycoonCheckpoints.home(p))return "Paid supply placement is pending its durable checkpoint.";spawn(p,h.residents.get(chosen.id),actor);}catch(IOException failure){return "Same paid resident arrival will recover; no second fee.";}return chosen.name()+" is arriving from the agency. Normal one-hour placement cooldown applies.";
    }
}
