package com.coraline.tycoon;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class TycoonDolls {
    public static boolean matches(TycoonData.Resident r,UUID id){return id!=null&&r.n.getCompound("Doll").hasUUID("Id")&&id.equals(r.n.getCompound("Doll").getUUID("Id"));}
    public static ItemStack item(ServerPlayer p,TycoonData.Resident r,CompoundTag doll){var stack=new ItemStack(TycoonPhaseTwoContent.DOLL.get());var n=stack.getOrCreateTag();TycoonWorld.tagOwned(p,stack);n.putUUID("Resident",r.id);n.putUUID("Doll",doll.getUUID("Id"));n.putInt("Appearance",r.n.getInt("Appearance"));n.putString("ResidentName",r.name());n.putString("Crafting","CRAFTED");n.putString("Assignment","INVENTORY");return stack;}
    public static String craft(ServerPlayer p,UUID id,boolean replacement){
        var h=TycoonWorld.home(p);var r=h.residents.get(id);if(r==null||!r.active()||r.bed()==null)return "Choose an active owned resident with an assigned bed.";
        var old=r.n.getCompound("Doll");if(old.hasUUID("Id")&&!replacement)return "This resident already has a doll. Use controlled replacement if it was lost.";
        if(replacement&&(!old.hasUUID("Id")||h.clock()<old.getLong("ReplacementAfter")||old.getString("Status").equals("PLACED")))return "Replacement unavailable while a placed doll exists or the replacement cooldown is active.";
        var doll=new CompoundTag();doll.putUUID("Id",UUID.randomUUID());doll.putInt("Appearance",r.n.getInt("Appearance"));doll.putString("Status","INVENTORY");doll.putLong("ReplacementAfter",h.clock()+24000);var patch=new CompoundTag();patch.put("Doll",doll);
        return TycoonTransactions.purchase(p,replacement?TycoonPhaseTwoConfig.REPLACEMENT.get():TycoonPhaseTwoConfig.DOLL.get(),item(p,r,doll),new CompoundTag(),r.id,patch);
    }
    public static boolean give(ServerPlayer p,TycoonNpc actor){
        var stack=p.getMainHandItem();if(!stack.is(TycoonPhaseTwoContent.DOLL.get()))return false;
        var r=TycoonCare.resident(actor);var n=stack.getTag();if(r==null||!r.active()||!TycoonWorld.owner(p).equals(actor.owner)||n==null||!TycoonWorld.owned(p,stack)||!n.hasUUID("Resident")||!r.id.equals(n.getUUID("Resident"))||!n.hasUUID("Doll")||!matches(r,n.getUUID("Doll"))){TycoonWorld.message(p,"This doll belongs to someone else.");return true;}
        var record=r.n.getCompound("Doll");if(!record.getString("Status").equals("INVENTORY")){TycoonWorld.message(p,"That doll is no longer an unplaced active capability.");return true;}
        if(p.serverLevel()!=p.server.overworld()||r.bed()==null||!TycoonRooms.survey(p).beds().contains(r.bed())){TycoonWorld.message(p,"Restore the assigned bed before delivering the doll.");return true;}
        var doll=TycoonPhaseTwoContent.DOLL_ACTOR.get().create(p.serverLevel());if(doll==null)return true;doll.configure(TycoonWorld.owner(p),r.id,record.getUUID("Id"),r.n.getInt("Appearance"));var at=r.bed().getCenter().add(.28,.18,0);doll.setPos(at);
        if(!p.serverLevel().addFreshEntity(doll))return true;record.putUUID("Entity",doll.getUUID());record.putLong("At",r.bed().asLong());record.putString("Status","PLACED");record.putBoolean("Delivered",true);stack.shrink(1);actor.getNavigation().stop();actor.getLookControl().setLookAt(p,30,30);actor.gesture(8,60);r.n.putInt("ActionGesture",8);r.n.putInt("ActionRemaining",60);TycoonWorld.message(p,r.name()+": "+(r.trait("Curiosity")>.55?"You made a tiny version of me? Look at its hair!":r.trait("Sensitivity")>.6?"It looks like me... I'll keep it by my pillow.":"Thank you. I'll put it beside my bed."));TycoonWorld.dirty(p);return true;
    }
    public static boolean valid(ServerPlayer p,TycoonData.Resident r){var n=r.n.getCompound("Doll");if(!n.getString("Status").equals("PLACED")||!n.getBoolean("Delivered")||!n.hasUUID("Entity")||r.bed()==null)return false;p.server.overworld().getChunkAt(r.bed());return p.server.overworld().getEntity(n.getUUID("Entity")) instanceof TycoonDollEntity d&&TycoonWorld.owner(p).equals(d.owner)&&r.id.equals(d.resident)&&matches(r,d.doll);}
    public static void tick(ServerPlayer p){for(var r:TycoonWorld.home(p).residents.values()){var n=r.n.getCompound("Doll");if(!r.active()||!n.getString("Status").equals("PLACED")||!n.hasUUID("Entity")||r.bed()==null)continue;p.server.overworld().getChunkAt(r.bed());if(n.contains("At"))p.server.overworld().getChunkAt(BlockPos.of(n.getLong("At")));var e=p.server.overworld().getEntity(n.getUUID("Entity"));if(e instanceof TycoonDollEntity d){n.remove("MissingTicks");if(!r.bed().equals(BlockPos.of(n.getLong("At")))){d.setPos(r.bed().getCenter().add(.28,.18,0));n.putLong("At",r.bed().asLong());TycoonWorld.dirty(p);}}else if(p.server.overworld().areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(r.bed()).toLong())){n.putInt("MissingTicks",n.getInt("MissingTicks")+100);if(n.getInt("MissingTicks")>=400){n.putString("Status","LOST");TycoonWorld.message(p,r.name()+"'s bedside doll is missing. New invitations require restoring or replacing it.");TycoonWorld.dirty(p);}}}}
    public static void pickup(ServerPlayer p,TycoonDollEntity actor){var r=TycoonWorld.home(p).residents.get(actor.resident);if(r==null||!r.active()||!matches(r,actor.doll)||p.distanceToSqr(actor)>25||p.getInventory().getFreeSlot()<0)return;var n=r.n.getCompound("Doll");if(!n.hasUUID("Entity")||!actor.getUUID().equals(n.getUUID("Entity")))return;if(TycoonWorld.deliver(p,item(p,r,n))){n.putString("Status","INVENTORY");n.remove("Entity");actor.discard();TycoonWorld.dirty(p);}}
    public static String reassign(ServerPlayer p,UUID id){var h=TycoonWorld.home(p);var r=h.residents.get(id);if(r==null||!r.active()||TycoonVisits.visiting(r))return "Choose an active resident who is at home.";var bed=TycoonRooms.survey(p).beds().stream().filter(at->!at.equals(r.bed())&&h.residents.values().stream().noneMatch(other->other.active()&&at.equals(other.bed()))).findFirst().orElse(null);if(bed==null)return "No different safe unoccupied Orphan Bed found.";r.n.putLong("Bed",bed.asLong());TycoonWorld.dirty(p);tick(p);return "Assigned "+r.name()+" to "+bed.toShortString()+"; the same bedside doll follows.";}
    private TycoonDolls(){}
    static boolean craftForWorker(ServerPlayer p,TycoonData.Resident r){
        if(!r.active()||r.bed()==null||r.n.getCompound("Doll").hasUUID("Id"))return false;var doll=new CompoundTag();doll.putUUID("Id",UUID.randomUUID());doll.putString("Status","WORKER_DELIVERY");doll.putInt("Appearance",r.n.getInt("Appearance"));doll.putLong("ReplacementAfter",TycoonWorld.home(p).clock()+24000);var patch=new CompoundTag();patch.put("Doll",doll);return TycoonWarehouse.purchase(p,TycoonPhaseTwoConfig.DOLL.get(),new CompoundTag(),r.id,patch);
    }
    static boolean deliverForWorker(ServerPlayer p,TycoonData.Resident r){
        var record=r.n.getCompound("Doll");if(!r.active()||r.bed()==null||!TycoonRooms.survey(p).beds().contains(r.bed())||!record.getString("Status").equals("WORKER_DELIVERY"))return false;var l=p.server.overworld();l.getChunkAt(r.bed());
        if(!record.hasUUID("Entity")){record.putUUID("Entity",UUID.randomUUID());TycoonWorld.dirty(p);try{if(!TycoonCheckpoints.home(p))return false;}catch(java.io.IOException failed){return false;}}
        if(l.getEntity(record.getUUID("Entity"))==null){var doll=TycoonPhaseTwoContent.DOLL_ACTOR.get().create(l);if(doll==null)return false;doll.setUUID(record.getUUID("Entity"));doll.configure(TycoonWorld.owner(p),r.id,record.getUUID("Id"),r.n.getInt("Appearance"));doll.setPos(r.bed().getCenter().add(.28,.18,0));if(!l.addFreshEntity(doll))return false;p.server.saveEverything(true,true,true);}
        record.putString("Status","PLACED");record.putBoolean("Delivered",true);record.putLong("At",r.bed().asLong());TycoonWorld.dirty(p);return true;
    }
}
