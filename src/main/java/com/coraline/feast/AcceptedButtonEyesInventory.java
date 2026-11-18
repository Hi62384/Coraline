package com.coraline.feast;

import java.util.UUID;
import com.coraline.registry.ModItems;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Owner-only escrow. Restoration is restricted to a proven alternate escape. */
@Mod.EventBusSubscriber(modid="coraline")
public final class AcceptedButtonEyesInventory {
    private static final String BACKUP="CoralineAcceptedInventoryEscrow";
    private record Capture(UUID player,ListTag overflow){}
    private static final ThreadLocal<Capture> CAPTURING=new ThreadLocal<>();
    private static CompoundTag inventoryState(ServerPlayer p,VisitData.Scene scene){return com.coraline.multiplayer.StorySessionData.get(p.server).inventoryLane(p,scene);}
    private static boolean sameBase(CompoundTag a,CompoundTag b){
        if(!a.hasUUID("Transaction")||!a.getUUID("Transaction").equals(b.getUUID("Transaction")))return false;
        for(String key:new String[]{"Owner","Door","Inventory","Cursor","Crafting"})if(a.get(key)==null||b.get(key)==null||!a.get(key).equals(b.get(key)))return false;
        return true;
    }
    private static boolean progressSuperset(CompoundTag a,CompoundTag b){
        var ar=a.getCompound("ActivityRewards");var br=b.getCompound("ActivityRewards");
        for(String key:br.getAllKeys())if(br.getBoolean(key)&&!ar.getBoolean(key))return false;
        var counts=new java.util.HashMap<String,Integer>();for(Tag row:a.getList("Overflow",Tag.TAG_COMPOUND)){String key=row.toString();counts.merge(key,1,Integer::sum);}
        for(Tag row:b.getList("Overflow",Tag.TAG_COMPOUND)){String key=row.toString();int count=counts.getOrDefault(key,0);if(count==0)return false;counts.put(key,count-1);}
        return true;
    }
    /** Reconcile the two durable mirrors only when one is a provable monotonic
     * superset of the other. Ambiguous item transactions stay quarantined. */
    private static boolean reconcileSnapshots(ServerPlayer p,VisitData.Scene scene,CompoundTag n,CompoundTag backup){
        if(!backup.getBoolean("Stored")||!backup.hasUUID("Door")||!scene.door.equals(backup.getUUID("Door"))||!validSnapshot(backup.getCompound("Snapshot"),p,scene.door))return false;
        var saved=backup.getCompound("Snapshot");if(!n.getBoolean("EscrowStored")){n.put("InventoryEscrow",saved.copy());n.putBoolean("EscrowStored",true);n.putString("CaptureState",backup.getString("CaptureState"));OtherMotherLoop.changed(p);return true;}
        var story=n.getCompound("InventoryEscrow");if(!validSnapshot(story,p,scene.door)||!sameBase(story,saved)){n.putString("Blocker","The original inventory escrow identities conflict. Both records are preserved for recovery.");OtherMotherLoop.changed(p);return false;}
        if(story.equals(saved))return true;
        boolean storyAhead=progressSuperset(story,saved),backupAhead=progressSuperset(saved,story);
        if(storyAhead&&backupAhead){var canonical=story.copy();canonical.putInt("Revision",Math.max(story.getInt("Revision"),saved.getInt("Revision")));n.put("InventoryEscrow",canonical.copy());backup.put("Snapshot",canonical.copy());OtherMotherLoop.changed(p);return true;}
        if(!storyAhead&&!backupAhead){n.putString("Blocker","The interrupted inventory reward records cannot be safely ordered. Both have been preserved.");OtherMotherLoop.changed(p);return false;}
        var canonical=storyAhead?story:saved;canonical.putInt("Revision",Math.max(story.getInt("Revision"),saved.getInt("Revision"))+1);
        n.put("InventoryEscrow",canonical.copy());backup.put("Snapshot",canonical.copy());OtherMotherLoop.changed(p);return true;
    }
    public static boolean locked(ServerPlayer p){
        var sessions=com.coraline.multiplayer.StorySessionData.get(p.server);if(sessions.returning(p.getUUID()))return false;
        var scene=OtherMotherLoop.scene(p);var n=AcceptedButtonEyesProgress.active(scene)?inventoryState(p,scene):null;
        return n!=null&&AcceptedButtonEyesProgress.active(scene)&&n.getBoolean("InventoryConfiscated")&&n.getBoolean("EscrowStored")||p.getPersistentData().getCompound(BACKUP).getBoolean("Stored");
    }
    /** Award doubled prizes visibly; confiscate them only when the activity is left. */
    public static boolean activityReward(ServerPlayer p,VisitData.Scene scene,String key,java.util.List<ItemStack> rewards){
        if(!locked(p)||!AcceptedButtonEyesProgress.active(scene))return false;
        var n=inventoryState(p,scene);if(n==null)return false;
        var backup=p.getPersistentData().getCompound(BACKUP);if(!reconcileSnapshots(p,scene,n,backup))return false;var snapshot=n.getCompound("InventoryEscrow");
        String receipt=scene.loop.generation+":"+key;
        var paid=snapshot.getCompound("ActivityRewards");if(paid.getBoolean(receipt))return true;
        var overflow=snapshot.getList("Overflow",Tag.TAG_COMPOUND);
        for(var reward:rewards)for(int copy=0;copy<2;copy++)if(!reward.isEmpty()){
            var earned=reward.copy();returnStack(p,earned);
            if(!earned.isEmpty())overflow.add(earned.save(new CompoundTag()));
        }
        snapshot.put("Overflow",overflow);paid.putBoolean(receipt,true);snapshot.put("ActivityRewards",paid);snapshot.putInt("Revision",snapshot.getInt("Revision")+1);
        n.putBoolean("ActivityLootPending",true);
        backup.put("Snapshot",snapshot.copy());OtherMotherLoop.changed(p);
        p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();
        return true;
    }
    /** Called only by the Player.drop injection during ordinary menu closing. */
    public static boolean captureOverflow(ServerPlayer p,ItemStack stack){
        var scope=CAPTURING.get();if(scope==null||!scope.player.equals(p.getUUID()))return false;
        if(!stack.isEmpty())scope.overflow.add(stack.copy().save(new CompoundTag()));return true;
    }
    /** Closing the offer screen precedes sewing; retain full-inventory overflow
     * now, without confiscating the player's usable items before the procedure. */
    public static void closeForScene(ServerPlayer p,VisitData.Scene scene){
        if(scene==null||!com.coraline.multiplayer.StoryCoop.member(p,scene)||!AcceptedButtonEyesProgress.active(scene))return;
        var n=inventoryState(p,scene);if(n==null)return;var overflow=n.getList("PendingOverflow",Tag.TAG_COMPOUND);
        if(p.getPersistentData().contains("CoralineAcceptedPendingOverflow",Tag.TAG_LIST)&&overflow.isEmpty())overflow=p.getPersistentData().getList("CoralineAcceptedPendingOverflow",Tag.TAG_COMPOUND);
        n.put("PendingOverflow",overflow);p.getPersistentData().put("CoralineAcceptedPendingOverflow",overflow);OtherMotherLoop.changed(p);
        if(CAPTURING.get()!=null)throw new IllegalStateException("Nested story menu closure");
        CAPTURING.set(new Capture(p.getUUID(),overflow));
        try{p.closeContainer();}finally{CAPTURING.remove();}
    }
    public static boolean capture(ServerPlayer p,VisitData.Scene scene){
        if(!com.coraline.multiplayer.StoryCoop.member(p,scene)||!AcceptedButtonEyesProgress.active(scene))return false;
        var n=inventoryState(p,scene);if(n==null)return false;var backup=p.getPersistentData().getCompound(BACKUP);
        if(n.getBoolean("EscrowRestored")||backup.getBoolean("Restored")&&n.getBoolean("EscrowStored")&&backup.hasUUID("RestoredTransaction")
            &&n.getCompound("InventoryEscrow").hasUUID("Transaction")&&backup.getUUID("RestoredTransaction").equals(n.getCompound("InventoryEscrow").getUUID("Transaction"))){
            n.putString("Blocker","This story's original inventory has already been restored. No second confiscation was performed.");return false;
        }
        if(backup.getBoolean("Stored")){
            if(!backup.hasUUID("Door")||!scene.door.equals(backup.getUUID("Door"))){n.putString("Blocker","An existing inventory escrow belongs to another story. It has been preserved.");return false;}
            if(!validSnapshot(backup.getCompound("Snapshot"),p,scene.door)){n.putString("Blocker","The backup escrow is incomplete. Visible items have not been cleared.");return false;}
            if(n.getBoolean("EscrowStored")&&!reconcileSnapshots(p,scene,n,backup))return false;
            if(!n.getBoolean("EscrowStored")){n.put("InventoryEscrow",backup.getCompound("Snapshot").copy());n.putBoolean("EscrowStored",true);n.putString("CaptureState",backup.getString("CaptureState"));}
        }
        if(n.getBoolean("EscrowStored")){
            if(!validSnapshot(n.getCompound("InventoryEscrow"),p,scene.door)){n.putString("Blocker","The escrow is incomplete. Visible items have not been cleared.");return false;}
            if(!backup.getBoolean("Stored")){
                var copy=new CompoundTag();copy.putBoolean("Stored",true);copy.putUUID("Door",scene.door);copy.put("Snapshot",n.getCompound("InventoryEscrow").copy());copy.putString("CaptureState","PREPARED");
                p.getPersistentData().put(BACKUP,copy);n.putString("CaptureState","PREPARED");
            }
            if(!"CLEARED".equals(n.getString("CaptureState"))||!"CLEARED".equals(p.getPersistentData().getCompound(BACKUP).getString("CaptureState")))finishClear(p,n);
            if(p.getUUID().equals(scene.owner))com.coraline.multiplayer.GroupInventoryEscrow.capture(p,scene);
            return true;
        }
        closeForScene(p,scene);var overflow=n.getList("PendingOverflow",Tag.TAG_COMPOUND);
        if(backup.getBoolean("Restored")&&!backup.getBoolean("Stored"))for(var old:backup.getList("ReturnOverflow",Tag.TAG_COMPOUND))overflow.add(old.copy());
        var snapshot=new CompoundTag();snapshot.putUUID("Transaction",UUID.randomUUID());snapshot.putUUID("Owner",p.getUUID());snapshot.putUUID("Door",scene.door);
        snapshot.put("Inventory",p.getInventory().save(new ListTag()));snapshot.put("Overflow",overflow);
        snapshot.put("Cursor",p.inventoryMenu.getCarried().copy().save(new CompoundTag()));
        var crafting=new ListTag();for(int i=1;i<=4;i++){var stack=p.inventoryMenu.getSlot(i).getItem();if(!stack.isEmpty()){var row=stack.copy().save(new CompoundTag());row.putInt("CraftSlot",i);crafting.add(row);}}
        snapshot.put("Crafting",crafting);
        if(!validSnapshot(snapshot,p,scene.door)){n.putString("Blocker","The pending inventory snapshot is incomplete. Visible items and overflow have been preserved.");return false;}
        n.put("InventoryEscrow",snapshot);n.putBoolean("EscrowStored",true);n.putString("CaptureState","PREPARED");
        var copy=new CompoundTag();copy.putBoolean("Stored",true);copy.putUUID("Door",scene.door);copy.put("Snapshot",snapshot.copy());copy.putString("CaptureState","PREPARED");
        // Backup and visible inventory are serialized together in player.dat;
        // a slower VisitData save can recover the same immutable transaction.
        p.getPersistentData().put(BACKUP,copy);OtherMotherLoop.changed(p);finishClear(p,n);if(p.getUUID().equals(scene.owner))com.coraline.multiplayer.GroupInventoryEscrow.capture(p,scene);return true;
    }
    public static boolean validSnapshot(CompoundTag snapshot,ServerPlayer p,UUID door){return validSnapshot(snapshot,p.getUUID(),door);}
    public static boolean validSnapshot(CompoundTag snapshot,UUID player,UUID door){
        if(!snapshot.hasUUID("Transaction")||!snapshot.hasUUID("Owner")||!snapshot.hasUUID("Door")||!player.equals(snapshot.getUUID("Owner"))||!door.equals(snapshot.getUUID("Door")))return false;
        for(String key:new String[]{"Inventory","Overflow","Crafting"})if(!(snapshot.get(key) instanceof ListTag rows)||!rows.isEmpty()&&rows.getElementType()!=Tag.TAG_COMPOUND)return false;
        var slots=new java.util.HashSet<Integer>();
        for(Tag row:snapshot.getList("Inventory",Tag.TAG_COMPOUND)){
            var item=(CompoundTag)row;int slot=item.getByte("Slot")&255;
            if(!item.contains("Slot",Tag.TAG_BYTE)||!slots.add(slot)||!(slot<36||slot>=100&&slot<=103||slot==150)||ItemStack.of(item).isEmpty())return false;
        }
        slots.clear();for(Tag row:snapshot.getList("Crafting",Tag.TAG_COMPOUND)){var item=(CompoundTag)row;int slot=item.getInt("CraftSlot");if(slot<1||slot>4||!slots.add(slot)||ItemStack.of(item).isEmpty())return false;}
        for(Tag row:snapshot.getList("Overflow",Tag.TAG_COMPOUND))if(ItemStack.of((CompoundTag)row).isEmpty())return false;
        if(!snapshot.contains("Cursor",Tag.TAG_COMPOUND))return false;var cursor=snapshot.getCompound("Cursor");
        return cursor.isEmpty()||!ItemStack.of(cursor).isEmpty()||cursor.getString("id").equals("minecraft:air")&&cursor.getByte("Count")==0;
    }
    private static void finishClear(ServerPlayer p,CompoundTag n){
        p.getInventory().clearContent();p.inventoryMenu.setCarried(ItemStack.EMPTY);
        for(int i=0;i<=4;i++)p.inventoryMenu.getSlot(i).set(ItemStack.EMPTY);
        n.putBoolean("InventoryConfiscated",true);n.putString("CaptureState","CLEARED");
        n.remove("PendingOverflow");p.getPersistentData().remove("CoralineAcceptedPendingOverflow");
        p.getPersistentData().getCompound(BACKUP).putString("CaptureState","CLEARED");
        p.getInventory().setChanged();p.inventoryMenu.broadcastFullState();OtherMotherLoop.changed(p);
    }
    public static boolean approvedTool(ServerPlayer p,ItemStack stack){
        if(stack.is(ModItems.AFTER_NEXT_ACTIVITY_DEBUG.get())&&p.hasPermissions(2))return true;
        var scene=OtherMotherLoop.scene(p);
        if(stack.isEmpty()||!AcceptedButtonEyesProgress.active(scene)||!ActivityWorld.isActivity(p.level())||scene.loop.phase!=OtherMotherLoopState.Phase.ACTIVITY)return false;
        if(scene.loop.activity==OtherMotherActivities.Activity.FISHING_CONTEST)return FishingContestActivity.issued(stack,p,scene);
        if(scene.loop.activity==OtherMotherActivities.Activity.OTHER_END_CITY)return EndCityActivity.canEat(p,stack);
        if(ExpeditionActivities.supports(scene.loop.activity)&&stack.hasTag()){
            var tag=stack.getTag();
            if(stack.getItem() instanceof com.coraline.item.ExpeditionItem&&tag.hasUUID("ExpeditionOwner")
                &&scene.owner.equals(tag.getUUID("ExpeditionOwner"))&&tag.getInt("Generation")==scene.loop.generation)return true;
            if(scene.loop.activity==OtherMotherActivities.Activity.OTHER_MINESHAFT
                &&stack.is(com.coraline.clothing.UtilityItems.MOONLIT_LANTERN.get())&&tag.getBoolean("MineLoan"))return true;
        }
        if(scene.loop.activity==OtherMotherActivities.Activity.CARNIVAL&&stack.hasTag()){
            var tag=stack.getTag();
            if((stack.is(ModItems.CARNIVAL_RING.get())||stack.is(ModItems.CARNIVAL_DART.get())||stack.is(ModItems.CARNIVAL_MALLET.get()))
                &&tag.hasUUID("CoralineCarnivalOwner")&&p.getUUID().equals(tag.getUUID("CoralineCarnivalOwner"))
                &&tag.getInt("CoralineCarnivalGeneration")==scene.loop.generation)return true;
        }
        if(scene.loop.activity!=OtherMotherActivities.Activity.ARCHERY_CONTEST||!stack.hasTag())return false;
        var tag=stack.getTag();return (stack.is(ModItems.ARCHERY_BOW.get())||stack.is(ModItems.ARCHERY_ARROW.get()))&&tag.getBoolean("CoralineArcheryIssued")
            &&tag.hasUUID("CoralineArcheryOwner")&&p.getUUID().equals(tag.getUUID("CoralineArcheryOwner"))&&tag.getInt("CoralineArcheryGeneration")==scene.loop.generation;
    }
    public static void enforce(ServerPlayer p){
        if(!locked(p)||!mayClearVisible(p)||ActivityWorld.isActivity(p.level()))return;
        for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(!stack.isEmpty()&&(i>=9||!approvedTool(p,stack)))p.getInventory().setItem(i,ItemStack.EMPTY);}
        p.inventoryMenu.setCarried(ItemStack.EMPTY);for(int i=0;i<=4;i++)p.inventoryMenu.getSlot(i).set(ItemStack.EMPTY);
        p.getInventory().setChanged();
    }
    private static boolean mayClearVisible(ServerPlayer p){
        var scene=OtherMotherLoop.scene(p);if(!AcceptedButtonEyesProgress.active(scene))return false;
        var n=inventoryState(p,scene);if(n==null)return false;var backup=p.getPersistentData().getCompound(BACKUP);
        return reconcileSnapshots(p,scene,n,backup)&&n.getBoolean("InventoryConfiscated")&&backup.getBoolean("Stored")&&backup.hasUUID("Door")&&scene.door.equals(backup.getUUID("Door"))
            &&"CLEARED".equals(n.getString("CaptureState"))&&"CLEARED".equals(backup.getString("CaptureState"))
            &&validSnapshot(n.getCompound("InventoryEscrow"),p,scene.door)&&validSnapshot(backup.getCompound("Snapshot"),p,scene.door)
            &&n.getCompound("InventoryEscrow").equals(backup.getCompound("Snapshot"));
    }
    /** Called before activity cleanup can remove equipment or earned prizes. */
    public static void confiscateActivityLoot(ServerPlayer p,VisitData.Scene scene){
        if(!locked(p)||!mayClearVisible(p))return;
        var n=inventoryState(p,scene);var snapshot=n.getCompound("InventoryEscrow");
        var overflow=snapshot.getList("Overflow",Tag.TAG_COMPOUND);boolean found=n.getBoolean("ActivityLootPending");
        for(int slot=0;slot<p.getInventory().getContainerSize();slot++){
            var stack=p.getInventory().getItem(slot);
            if(stack.isEmpty()||approvedTool(p,stack))continue;
            overflow.add(stack.copy().save(new CompoundTag()));p.getInventory().setItem(slot,ItemStack.EMPTY);found=true;
        }
        var carried=p.inventoryMenu.getCarried();
        if(!carried.isEmpty()&&!approvedTool(p,carried)){overflow.add(carried.copy().save(new CompoundTag()));p.inventoryMenu.setCarried(ItemStack.EMPTY);found=true;}
        for(int slot=1;slot<=4;slot++){
            var stack=p.inventoryMenu.getSlot(slot).getItem();
            if(!stack.isEmpty()&&!approvedTool(p,stack)){overflow.add(stack.copy().save(new CompoundTag()));p.inventoryMenu.getSlot(slot).set(ItemStack.EMPTY);found=true;}
        }
        snapshot.put("Overflow",overflow);snapshot.putInt("Revision",snapshot.getInt("Revision")+1);p.getPersistentData().getCompound(BACKUP).put("Snapshot",snapshot.copy());
        n.remove("ActivityLootPending");OtherMotherLoop.changed(p);p.getInventory().setChanged();p.inventoryMenu.broadcastFullState();
        if(found)p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Your activity loot was confiscated and stored with your other belongings."));
    }
    public static void cleanTemporary(ServerPlayer p){if(locked(p)&&mayClearVisible(p)){p.getInventory().clearContent();p.inventoryMenu.setCarried(ItemStack.EMPTY);p.inventoryMenu.broadcastFullState();}}
    /** Inventory and the consumed receipt are serialized together in player.dat.
     * A stale VisitData save can reconcile its flags, but cannot claim items twice. */
    public static boolean restoreAfterGhostEscape(ServerPlayer p,VisitData.Scene scene){
        if(com.coraline.multiplayer.StoryCoop.guest(p))return com.coraline.multiplayer.GroupInventoryEscrow.restore(p);
        if(scene==null||!p.server.isSameThread()||!p.getUUID().equals(scene.owner))return false;
        var n=inventoryState(p,scene);if(n==null)return false;var ghost=GhostVictimEntry.data(scene);var escape=ghost.getCompound(GhostEscapeProgress.KEY);
        if(!GhostEscapeProgress.owns(ghost,escape)||!escape.getBoolean("WagerWon")||!escape.getBoolean("PlayerEyesHeld")||GhostEscapeProgress.phase(escape)!=GhostEscapeProgress.Phase.RESTORING)return false;
        var door=com.coraline.otherworld.DoorData.get(p.server).doors.get(scene.door);
        if(door==null||!p.getUUID().equals(door.owner)||p.serverLevel()!=com.coraline.parents.FamilyData.level(p.server,door.dimension))return false;
        com.coraline.multiplayer.StorySessionData.get(p.server).authorizeInventoryReturn(p.server,scene);
        var backup=p.getPersistentData().getCompound(BACKUP);var snapshot=backup.getCompound("Snapshot");
        if(!reconcileSnapshots(p,scene,n,backup)||!backup.hasUUID("Door")||!scene.door.equals(backup.getUUID("Door"))||!n.getBoolean("EscrowStored"))return false;snapshot=backup.getCompound("Snapshot");
        if(!validSnapshot(snapshot,p,scene.door)||!escape.hasUUID("OriginalEscrow")||!escape.getUUID("OriginalEscrow").equals(snapshot.getUUID("Transaction")))return false;
        if(backup.getBoolean("Restored")&&backup.hasUUID("RestoredTransaction")&&backup.getUUID("RestoredTransaction").equals(snapshot.getUUID("Transaction"))){
            restoredFlags(n,backup,snapshot.getUUID("Transaction"));OtherMotherLoop.changed(p);return true;
        }
        if(!backup.getBoolean("Stored")||!n.getBoolean("EscrowStored")||!"CLEARED".equals(backup.getString("CaptureState"))||!n.getCompound("InventoryEscrow").equals(snapshot))return false;
        p.closeContainer();var visible=p.getInventory().save(new ListTag());var carried=p.inventoryMenu.getCarried().copy();
        var oldCrafting=new java.util.ArrayList<ItemStack>();for(int slot=0;slot<=4;slot++)oldCrafting.add(p.inventoryMenu.getSlot(slot).getItem().copy());
        try{
            p.getInventory().load(snapshot.getList("Inventory",Tag.TAG_COMPOUND));p.inventoryMenu.setCarried(ItemStack.EMPTY);
            for(int slot=0;slot<=4;slot++)p.inventoryMenu.getSlot(slot).set(ItemStack.EMPTY);
            var returning=snapshot.getList("Overflow",Tag.TAG_COMPOUND).copy();var cursor=ItemStack.of(snapshot.getCompound("Cursor"));
            if(!cursor.isEmpty())returning.add(cursor.save(new CompoundTag()));
            for(Tag entry:snapshot.getList("Crafting",Tag.TAG_COMPOUND))returning.add(ItemStack.of((CompoundTag)entry).save(new CompoundTag()));
            var remaining=new ListTag();for(Tag entry:returning){var item=ItemStack.of((CompoundTag)entry);returnStack(p,item);if(!item.isEmpty())remaining.add(item.save(new CompoundTag()));}
            // Full-inventory overflow remains recoverable in the same saved
            // receipt, instead of being dropped or silently discarded.
            // Cursor/crafting grids are transient and are not saved with a
            // player's inventory. Return their items to durable slots/overflow.
            backup.put("ReturnOverflow",remaining);backup.putBoolean("Restored",true);backup.putUUID("RestoredTransaction",snapshot.getUUID("Transaction"));
            restoredFlags(n,backup,snapshot.getUUID("Transaction"));
        }catch(RuntimeException failure){
            p.getInventory().load(visible);p.inventoryMenu.setCarried(carried);for(int slot=0;slot<=4;slot++)p.inventoryMenu.getSlot(slot).set(oldCrafting.get(slot));
            n.putString("Blocker","Original inventory restoration was interrupted; the saved escrow is preserved.");return false;
        }
        p.getInventory().setChanged();p.inventoryMenu.broadcastFullState();OtherMotherLoop.changed(p);return true;
    }
    private static void restoredFlags(CompoundTag scene,CompoundTag backup,UUID transaction){
        backup.putBoolean("Stored",false);backup.putString("CaptureState","RESTORED");scene.putString("CaptureState","RESTORED");scene.putBoolean("InventoryConfiscated",false);scene.putBoolean("EscrowRestored",true);scene.putUUID("RestoredTransaction",transaction);
    }
    /** The story must read this durable receipt before advancing past restoration.
     * Merely assigning visible inventory or updating VisitData is insufficient. */
    public static boolean restorationSaved(ServerPlayer p,VisitData.Scene scene){
        if(com.coraline.multiplayer.StoryCoop.guest(p))return com.coraline.multiplayer.GroupInventoryEscrow.restorationSaved(p);
        if(scene==null||!p.server.isSameThread()||!p.getUUID().equals(scene.owner))return false;
        var n=inventoryState(p,scene);if(n==null)return false;var snapshot=n.getCompound("InventoryEscrow");var backup=p.getPersistentData().getCompound(BACKUP);
        if(!validSnapshot(snapshot,p,scene.door)||!backup.getBoolean("Restored")||!backup.hasUUID("RestoredTransaction")||!backup.getUUID("RestoredTransaction").equals(snapshot.getUUID("Transaction")))return false;
        p.server.getPlayerList().saveAll();
        try{
            var saved=com.coraline.rebirth.StoryRunStore.read(p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat"));
            var receipt=saved.getCompound("ForgeData").getCompound(BACKUP);
            return receipt.getBoolean("Restored")&&!receipt.getBoolean("Stored")&&receipt.hasUUID("RestoredTransaction")&&receipt.getUUID("RestoredTransaction").equals(snapshot.getUUID("Transaction"))
                &&com.coraline.palace.PalaceKeyCheckpoint.inventory(saved,p.getInventory().save(new ListTag()));
        }catch(java.io.IOException failure){n.putString("Blocker","Restored inventory is waiting for its saved-player checkpoint; the original escrow remains intact.");return false;}
    }
    @SubscribeEvent public static void returnOverflow(net.minecraftforge.event.TickEvent.PlayerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||p.tickCount%20!=0||locked(p))return;
        var backup=p.getPersistentData().getCompound(BACKUP);if(backup.hasUUID("CoopSession")&&(!backup.hasUUID("ReturnProof")||!backup.getCompound("Snapshot").hasUUID("Transaction")||!backup.getUUID("ReturnProof").equals(backup.getCompound("Snapshot").getUUID("Transaction"))))return;var pending=backup.getList("ReturnOverflow",Tag.TAG_COMPOUND);
        if(!backup.getBoolean("Restored")||pending.isEmpty())return;
        var before=p.getInventory().save(new ListTag());var remaining=new ListTag();
        try{for(Tag entry:pending){var item=ItemStack.of((CompoundTag)entry);returnStack(p,item);if(!item.isEmpty())remaining.add(item.save(new CompoundTag()));}}
        catch(RuntimeException failure){p.getInventory().load(before);return;}
        backup.put("ReturnOverflow",remaining);p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();
    }
    private static void returnStack(ServerPlayer p,ItemStack item){
        // Inventory.add intentionally discards full-inventory creative stacks.
        // Escrow recovery must retain those items too, regardless of game mode.
        for(int slot=0;slot<36&&!item.isEmpty();slot++){
            var held=p.getInventory().getItem(slot);
            if(held.isEmpty()||!ItemStack.isSameItemSameTags(held,item))continue;
            int count=Math.min(item.getCount(),Math.max(0,Math.min(held.getMaxStackSize(),p.getInventory().getMaxStackSize())-held.getCount()));
            held.grow(count);item.shrink(count);
        }
        for(int slot=0;slot<36&&!item.isEmpty();slot++)if(p.getInventory().getItem(slot).isEmpty()){
            int count=Math.min(item.getCount(),Math.min(item.getMaxStackSize(),p.getInventory().getMaxStackSize()));
            var returned=item.copy();returned.setCount(count);p.getInventory().setItem(slot,returned);item.shrink(count);
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void pickup(EntityItemPickupEvent event){if(event.getEntity() instanceof ServerPlayer p&&locked(p))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void experience(PlayerXpEvent.PickupXp event){if(event.getEntity() instanceof ServerPlayer p&&locked(p))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void use(PlayerInteractEvent.RightClickItem event){if(event.getEntity() instanceof ServerPlayer p&&locked(p)&&!approvedTool(p,event.getItemStack()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void containers(PlayerInteractEvent.RightClickBlock event){
        if(event.getEntity() instanceof ServerPlayer p&&locked(p)&&p.level().getBlockState(event.getPos()).getMenuProvider(p.level(),event.getPos())!=null)event.setCanceled(true);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){if(event.getOriginal() instanceof ServerPlayer old&&event.getEntity() instanceof ServerPlayer p&&old.getPersistentData().contains(BACKUP,Tag.TAG_COMPOUND))p.getPersistentData().put(BACKUP,old.getPersistentData().getCompound(BACKUP).copy());}
    private AcceptedButtonEyesInventory(){}
}
