package com.coraline.multiplayer;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.coraline.feast.*;

/** Each guest's actual inventory is held in their own native player file. The
 * shared scene only authorizes return; it never supplies an owner's inventory. */
public final class GroupInventoryEscrow {
    private static final String BACKUP="CoralineAcceptedInventoryEscrow";
    public static void capture(ServerPlayer owner,VisitData.Scene scene){
        if(!owner.getUUID().equals(scene.owner))return;var accepted=AcceptedButtonEyesProgress.data(scene);var root=accepted.getCompound("InventoryEscrow");
        if(!accepted.getBoolean("InventoryConfiscated")||!accepted.getBoolean("EscrowStored")||!root.hasUUID("Transaction"))return;
        var data=StorySessionData.get(owner.server);var frame=data.frame(owner.getUUID());if(frame==null)return;
        for(var guest:data.participants(owner.server,scene.owner,true))if(!guest.getUUID().equals(scene.owner)&&guest.serverLevel()==owner.serverLevel()&&data.admittedOwner(guest.getUUID()).filter(scene.owner::equals).isPresent()){
            var lane=data.inventoryLane(guest,scene);if(lane==null)continue;
            if(lane.hasUUID("OwnerEscrow")&&!lane.getUUID("OwnerEscrow").equals(root.getUUID("Transaction")))continue;
            var nativeBackup=guest.getPersistentData().getCompound(BACKUP);
            if(lane.getBoolean("EscrowStored")&&lane.getBoolean("InventoryConfiscated")&&lane.hasUUID("Session")&&lane.getUUID("Session").equals(frame.id())&&nativeBackup.hasUUID("CoopSession")&&nativeBackup.getUUID("CoopSession").equals(frame.id())&&nativeBackup.getBoolean("Stored")&&nativeBackup.getString("CaptureState").equals("CLEARED")&&nativeBackup.getCompound("Snapshot").equals(lane.getCompound("InventoryEscrow")))continue;
            lane.putUUID("OwnerEscrow",root.getUUID("Transaction"));lane.putUUID("Session",frame.id());if(AcceptedButtonEyesInventory.capture(guest,scene))guest.getPersistentData().getCompound(BACKUP).putUUID("CoopSession",lane.getUUID("Session"));data.touch(owner.server);
        }
    }
    private static boolean authorized(ServerPlayer p,CompoundTag lane,CompoundTag snapshot){
        var witness=StorySessionData.get(p.server).inventoryWitness(p);var backup=p.getPersistentData().getCompound(BACKUP);if(!witness.hasUUID("Session")||!lane.hasUUID("Session")||!witness.getUUID("Session").equals(lane.getUUID("Session")))return false;
        if(!backup.hasUUID("CoopSession")){if(!backup.getCompound("Snapshot").equals(snapshot))return false;backup.putUUID("CoopSession",lane.getUUID("Session"));}
        if(!backup.getUUID("CoopSession").equals(lane.getUUID("Session")))return false;return witness.hasUUID("Door")&&witness.hasUUID("OwnerEscrow")&&lane.hasUUID("OwnerEscrow")&&witness.getUUID("OwnerEscrow").equals(lane.getUUID("OwnerEscrow"))&&AcceptedButtonEyesInventory.validSnapshot(snapshot,p,witness.getUUID("Door"));
    }
    public static boolean held(ServerPlayer p){var lane=StorySessionData.get(p.server).guestInventory(p);return lane!=null&&(p.getPersistentData().getCompound(BACKUP).getBoolean("Stored")||lane.getBoolean("EscrowStored")&&!lane.getBoolean("EscrowRestored"));}
    public static boolean restore(ServerPlayer p){
        var data=StorySessionData.get(p.server);var lane=data.guestInventory(p);if(lane==null||!lane.getBoolean("EscrowStored"))return false;var backup=p.getPersistentData().getCompound(BACKUP);var snapshot=lane.getCompound("InventoryEscrow");
        if(!authorized(p,lane,snapshot)||!backup.getCompound("Snapshot").equals(snapshot))return false;
        if(backup.getBoolean("Restored")&&backup.hasUUID("RestoredTransaction")&&backup.getUUID("RestoredTransaction").equals(snapshot.getUUID("Transaction")))return restorationSaved(p);
        if(!backup.getBoolean("Stored")||!backup.getString("CaptureState").equals("CLEARED"))return false;
        p.closeContainer();
        if(!backup.getBoolean("ReturnApplying")){
            var target=plan(p,snapshot);backup.put("RestoreBefore",p.getInventory().save(new ListTag()));backup.put("RestoreTarget",target.getList("Inventory",Tag.TAG_COMPOUND));backup.put("ReturnOverflow",target.getList("Overflow",Tag.TAG_COMPOUND));backup.putBoolean("ReturnApplying",true);
        }
        var before=backup.getList("RestoreBefore",Tag.TAG_COMPOUND);var target=backup.getList("RestoreTarget",Tag.TAG_COMPOUND);
        var current=new CompoundTag();current.put("Inventory",p.getInventory().save(new ListTag()));
        boolean already=com.coraline.palace.PalaceKeyCheckpoint.inventory(current,target);
        if(!already&&!com.coraline.palace.PalaceKeyCheckpoint.inventory(current,before))return false;
        if(!already){try{p.getInventory().load(target);}catch(RuntimeException interrupted){return false;}}
        backup.putBoolean("Stored",false);backup.putBoolean("Restored",true);backup.putString("CaptureState","RESTORED");backup.putUUID("RestoredTransaction",snapshot.getUUID("Transaction"));
        p.getInventory().setChanged();p.inventoryMenu.broadcastFullState();data.touch(p.server);return restorationSaved(p);
    }
    private static CompoundTag plan(ServerPlayer p,CompoundTag snapshot){return planNative(p.getInventory().save(new ListTag()),snapshot);}
    public static CompoundTag planNative(ListTag inventory,CompoundTag snapshot){
        var main=new net.minecraft.world.SimpleContainer(36);var extra=new java.util.ArrayList<ItemStack>();for(int i=0;i<5;i++)extra.add(ItemStack.EMPTY);var slots=new java.util.HashSet<Integer>();
        for(var raw:inventory){var row=(CompoundTag)raw;int slot=row.getByte("Slot")&255;var stack=ItemStack.of(row);if(!row.contains("Slot",Tag.TAG_BYTE)||!slots.add(slot)||stack.isEmpty())throw new IllegalArgumentException("Invalid native inventory slot");if(slot<36)main.setItem(slot,stack);else if(slot>=100&&slot<=103)extra.set(slot-100,stack);else if(slot==150)extra.set(4,stack);else throw new IllegalArgumentException("Unknown native inventory slot");}
        var overflow=new ListTag();
        for(var raw:snapshot.getList("Inventory",Tag.TAG_COMPOUND)){var row=(CompoundTag)raw;var item=ItemStack.of(row);int slot=row.getByte("Slot")&255;
            if(slot<36&&main.getItem(slot).isEmpty())main.setItem(slot,item);
            else if(slot>=100&&slot<=103&&extra.get(slot-100).isEmpty())extra.set(slot-100,item);
            else if(slot==150&&extra.get(4).isEmpty())extra.set(4,item);
            else insert(main,item,overflow);
        }
        for(var raw:snapshot.getList("Overflow",Tag.TAG_COMPOUND))insert(main,ItemStack.of((CompoundTag)raw),overflow);
        insert(main,ItemStack.of(snapshot.getCompound("Cursor")),overflow);for(var raw:snapshot.getList("Crafting",Tag.TAG_COMPOUND))insert(main,ItemStack.of((CompoundTag)raw),overflow);
        var packed=new ListTag();for(int i=0;i<41;i++){var item=i<36?main.getItem(i):extra.get(i-36);if(item.isEmpty())continue;var row=item.save(new CompoundTag());row.putByte("Slot",(byte)(i<36?i:i<40?100+i-36:150));packed.add(row);}var out=new CompoundTag();out.put("Inventory",packed);out.put("Overflow",overflow);return out;
    }
    private static void insert(net.minecraft.world.SimpleContainer target,ItemStack item,ListTag overflow){if(item.isEmpty())return;var remainder=target.addItem(item.copy());if(!remainder.isEmpty())overflow.add(remainder.save(new CompoundTag()));}
    public static boolean restorationSaved(ServerPlayer p){
        var data=StorySessionData.get(p.server);var lane=data.guestInventory(p);if(lane==null)return false;var snapshot=lane.getCompound("InventoryEscrow");var backup=p.getPersistentData().getCompound(BACKUP);
        if(!authorized(p,lane,snapshot)||!backup.getCompound("Snapshot").equals(snapshot)||!backup.getBoolean("Restored")||!backup.hasUUID("RestoredTransaction")||!backup.getUUID("RestoredTransaction").equals(snapshot.getUUID("Transaction")))return false;
        long now=p.server.overworld().getGameTime(),after=backup.getLong("RestoreSaveRetryAt");
        if(after>now&&after-now<=40)return false;
        backup.putLong("RestoreSaveRetryAt",now+40);
        try{((com.coraline.mixin.MirrorPlayerSaveAccess)p.server.getPlayerList()).coraline$saveMirrorPlayer(p);var file=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat");var saved=com.coraline.rebirth.StoryRunStore.read(file);var nativeBackup=saved.getCompound("ForgeData").getCompound(BACKUP);if(!nativeBackup.getBoolean("Restored")||nativeBackup.getBoolean("Stored")||!nativeBackup.hasUUID("RestoredTransaction")||!nativeBackup.getUUID("RestoredTransaction").equals(snapshot.getUUID("Transaction"))||!(backup.hasUUID("ReturnProof")&&backup.getUUID("ReturnProof").equals(snapshot.getUUID("Transaction"))?com.coraline.palace.PalaceKeyCheckpoint.inventory(saved,p.getInventory().save(new ListTag())):com.coraline.palace.PalaceKeyCheckpoint.inventory(saved,backup.getList("RestoreTarget",Tag.TAG_COMPOUND))))return false;
            backup.putUUID("ReturnProof",snapshot.getUUID("Transaction"));
            lane.putBoolean("EscrowRestored",true);lane.putBoolean("InventoryConfiscated",false);lane.putString("CaptureState","RESTORED");lane.putUUID("RestoredTransaction",snapshot.getUUID("Transaction"));data.touch(p.server);return data.checkpoint(p.server);
        }catch(java.io.IOException|RuntimeException unavailable){return false;}
    }
    private GroupInventoryEscrow(){}
}
