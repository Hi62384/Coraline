package com.coraline.multiplayer;

import com.coraline.feast.*;
import com.coraline.rebirth.StoryRunStore;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Original-item transfers, not item factories. Source removal is proved in
 * the actual native UUID profile before the original stack becomes deliverable.
 * Destination insertion uses the same profile/receipt transaction as rewards.
 * Ordinary inventory entries and unrelated native NBT remain untouched. */
@Mod.EventBusSubscriber(modid="coraline")
public final class QuestItemRecovery extends SavedData {
    private static final String NAME="coraline_quest_item_recovery",RECEIPTS="CoralineQuestItemReceipts",REMOVALS="CoralineQuestItemRemovalReceipts";
    private final CompoundTag rows;
    private boolean blocked;
    private final Set<String> pending=new LinkedHashSet<>();
    private int cursor;
    private QuestItemRecovery(CompoundTag tag){
        rows=tag.getCompound("Rows").copy();blocked=tag.getBoolean("Blocked")||tag.getInt("Version")!=1||!tag.contains("Rows",Tag.TAG_COMPOUND);
        if(rows.size()>65536)blocked=true;
        for(String id:rows.getAllKeys())try{
            var row=rows.getCompound(id);if(!UUID.fromString(id).equals(row.getUUID("Id"))||!row.hasUUID("Owner")||!row.hasUUID("Holder")||!row.hasUUID("Door")
                ||!Set.of("PREPARED","STORED","DELIVERED").contains(row.getString("State")))blocked=true;
            if(!row.getString("State").equals("DELIVERED")){if(row.getList("Items",Tag.TAG_COMPOUND).isEmpty()||row.getList("Items",Tag.TAG_COMPOUND).size()>68)blocked=true;pending.add(id);}
        }catch(RuntimeException corrupt){blocked=true;}
    }
    public static QuestItemRecovery get(MinecraftServer server){
        var file=file(server);return server.overworld().getDataStorage().computeIfAbsent(QuestItemRecovery::new,()->{
            var n=fresh();if(!Files.notExists(file,LinkOption.NOFOLLOW_LINKS))n.putBoolean("Blocked",true);return new QuestItemRecovery(n);
        },NAME);
    }
    private static Path file(MinecraftServer server){return server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");}
    private static Path profile(MinecraftServer server,UUID id){return server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id+".dat");}
    private boolean checkpoint(MinecraftServer server){
        try{if(blocked||Files.isSymbolicLink(file(server))||Files.isSymbolicLink(file(server).getParent()))return false;setDirty();server.overworld().getDataStorage().save();return save(new CompoundTag()).equals(StoryRunStore.read(file(server)).getCompound("data"));}
        catch(IOException|RuntimeException unavailable){return false;}
    }
    private static CompoundTag fresh(){var n=new CompoundTag();n.putInt("Version",1);n.put("Rows",new CompoundTag());return n;}
    private static ListTag ender(ServerPlayer p){var items=new ListTag();for(int i=0;i<p.getEnderChestInventory().getContainerSize();i++){var stack=p.getEnderChestInventory().getItem(i);if(!stack.isEmpty()){var n=stack.save(new CompoundTag());n.putByte("Slot",(byte)i);items.add(n);}}return items;}
    private static void ender(ServerPlayer p,ListTag items){p.getEnderChestInventory().clearContent();for(var tag:items){var row=(CompoundTag)tag;int slot=row.getByte("Slot")&255;if(slot<0||slot>=p.getEnderChestInventory().getContainerSize())throw new IllegalArgumentException("Invalid actual ender slot");p.getEnderChestInventory().setItem(slot,ItemStack.of(row));}}
    private static boolean sameSource(CompoundTag file,CompoundTag row,String prefix){return com.coraline.palace.PalaceKeyCheckpoint.inventory(file,row.getList(prefix,Tag.TAG_COMPOUND))&&file.getList("EnderItems",Tag.TAG_COMPOUND).equals(row.getList(prefix+"Ender",Tag.TAG_COMPOUND));}
    /** Replan only the original stacks not already present. This safely resumes
     * a prepared delivery after the recipient rearranged their inventory, while
     * avoiding a second copy if an earlier save already inserted some items. */
    static ListTag missingDeliveryItems(CompoundTag profile,CompoundTag row){
        var available=new ArrayList<ItemStack>();
        for(String key:List.of("Inventory","EnderItems"))for(Tag entry:profile.getList(key,Tag.TAG_COMPOUND))available.add(ItemStack.of((CompoundTag)entry));
        var missing=new ListTag();
        for(Tag entry:row.getList("Items",Tag.TAG_COMPOUND)){
            var wanted=ItemStack.of((CompoundTag)entry);
            for(var held:available)if(!held.isEmpty()&&ItemStack.isSameItemSameTags(wanted,held)){
                int moved=Math.min(wanted.getCount(),held.getCount());wanted.shrink(moved);held.shrink(moved);if(wanted.isEmpty())break;
            }
            if(!wanted.isEmpty()){var serialized=wanted.save(new CompoundTag());serialized.putByte("Slot",(byte)127);missing.add(serialized);}
        }
        return missing;
    }
    private static void savePlayer(ServerPlayer p){((com.coraline.mixin.MirrorPlayerSaveAccess)p.server.getPlayerList()).coraline$saveMirrorPlayer(p);}
    private static VisitData.Scene scope(ServerPlayer p){
        var frame=StorySessionData.get(p.server).frame(p.getUUID());
        var scene=VisitData.get(p.server).scenes.get(frame==null?p.getUUID():frame.owner());
        return scene!=null&&(frame==null?scene.owner.equals(p.getUUID()):scene.door.equals(frame.door()))?scene:null;
    }
    public static String kind(ItemStack item,VisitData.Scene scene){
        if(scene==null||item.isEmpty()||!item.hasTag())return "";
        if(FirstEscapeStory.validTool(scene,item,false))return "cane";
        if(FirstEscapeStory.validTool(scene,item,true))return "shoe";
        if(ParentRescueStory.key(item,scene))return "key";
        if(StoryItemCustody.stone(item,scene))return "stone";
        for(int i=0;i<3;i++)if(RescueEyes.authentic(item,scene,i))return "eye_"+i;
        return "";
    }
    public static boolean guarded(ServerPlayer p,ItemStack item){return !kind(item,scope(p)).isEmpty();}
    public static boolean reserveTool(ServerPlayer p,VisitData.Scene scene,boolean shoe){
        String kind=shoe?"shoe":"cane";var data=StorySessionData.get(p.server);var known=data.questItem(scene.owner,scene.door,kind);
        if(known.hasUUID("Holder")&&!known.getUUID("Holder").equals(p.getUUID())){var holder=p.server.getPlayerList().getPlayer(known.getUUID("Holder"));if(holder!=null)confirmHeld(holder,scene,kind);if(p.getUUID().equals(scene.owner))recall(p,scene,kind);return false;}
        try{
            if(known.isEmpty())for(UUID id:data.questHolders(scene.owner))if(!id.equals(p.getUUID())){
                var member=p.server.getPlayerList().getPlayer(id);if(member!=null){if(kind(member.containerMenu.getCarried(),scene).equals(kind))return false;savePlayer(member);}
                var nativeMember=StoryRunStore.read(profile(p.server,id));if(!nativeMember.hasUUID("UUID")||!id.equals(nativeMember.getUUID("UUID")))return false;
                int found=0;for(var entries:List.of(nativeMember.getList("Inventory",Tag.TAG_COMPOUND),nativeMember.getList("EnderItems",Tag.TAG_COMPOUND)))for(var tag:entries){var item=ItemStack.of((CompoundTag)tag);if(kind(item,scene).equals(kind))found+=item.getCount();}
                if(found>0){if(found!=1)return false;var frame=data.frame(scene.owner);if(frame==null)return false;var names=new ListTag();names.add(StringTag.valueOf(kind));data.questHolder(p.server,frame.id(),id,UUID.randomUUID(),names);return false;}
            }
            savePlayer(p);var actual=StoryRunStore.read(profile(p.server,p.getUUID()));if(!actual.hasUUID("UUID")||!p.getUUID().equals(actual.getUUID("UUID")))return false;return data.reserveQuestTool(p,scene,kind);
        }catch(IOException|RuntimeException unavailable){return false;}
    }
    public static boolean confirmHeld(ServerPlayer p,VisitData.Scene scene,String kind){
        var data=StorySessionData.get(p.server);var frame=data.frame(p.getUUID());if(frame==null||!frame.owner().equals(scene.owner)||!frame.door().equals(scene.door))return false;
        int held=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(kind(p.getInventory().getItem(i),scene).equals(kind))held+=p.getInventory().getItem(i).getCount();if(held!=1)return false;
        var known=data.questItem(scene.owner,scene.door,kind);if(known.getString("State").equals("HELD")&&known.hasUUID("Holder")&&p.getUUID().equals(known.getUUID("Holder")))return true;
        try{savePlayer(p);var actual=StoryRunStore.read(profile(p.server,p.getUUID()));if(!com.coraline.palace.PalaceKeyCheckpoint.inventory(actual,p.getInventory().save(new ListTag())))return false;var names=new ListTag();names.add(StringTag.valueOf(kind));return data.questHolder(p.server,frame.id(),p.getUUID(),UUID.randomUUID(),names);}catch(IOException|RuntimeException unavailable){return false;}
    }
    public static boolean pendingSource(ServerPlayer p){var data=get(p.server);for(String id:data.pending){var row=data.rows.getCompound(id);if(row.getUUID("Holder").equals(p.getUUID())&&row.getString("State").equals("PREPARED"))return true;}return false;}
    /** Called while the real holder still exists, before vanilla death drops or
     * membership removal. A failed checkpoint leaves the same transfer pending. */
    public static boolean capture(ServerPlayer holder){return capture(holder,"",null);}
    public static boolean handoff(ServerPlayer owner,UUID recipient,String kind){
        var scene=scope(owner);var target=owner.server.getPlayerList().getPlayer(recipient);
        if(scene==null||!owner.getUUID().equals(scene.owner)||target==null||target==owner||!StoryCoop.canAct(owner,scene)||!StoryCoop.canAct(target,scene)||target.serverLevel()!=owner.serverLevel()||target.distanceToSqr(owner)>36||AcceptedButtonEyesInventory.locked(owner)||AcceptedButtonEyesInventory.locked(target)||!Set.of("cane","shoe","key","stone","eye_0","eye_1","eye_2").contains(kind))return false;
        boolean found=false;for(var item:owner.getInventory().items)if(kind(item,scene).equals(kind))found=true;if(!found)return false;
        return capture(owner,kind,recipient);
    }
    private static boolean capture(ServerPlayer holder,String onlyKind,UUID recipient){return capture(holder,onlyKind,recipient,null);}
    private static boolean capture(ServerPlayer holder,String onlyKind,UUID recipient,VisitData.Scene specific){
        var data=get(holder.server);if(data.blocked)return false;var scene=specific==null?scope(holder):specific;if(scene==null)return true;
        if(specific!=null&&!holder.getUUID().equals(scene.owner)&&!StoryCoop.member(holder,scene)&&!StorySessionData.get(holder.server).provesQuestHolder(scene.owner,scene.door,holder.getUUID()))return false;
        for(String key:data.rows.getAllKeys()){
            var row=data.rows.getCompound(key);
            if(row.getUUID("Holder").equals(holder.getUUID())&&row.getString("State").equals("PREPARED"))return data.removeSource(holder.server,row,holder);
        }
        holder.closeContainer();var before=holder.getInventory().save(new ListTag());var after=before.copy();var beforeEnder=ender(holder);var afterEnder=beforeEnder.copy();var original=new ListTag();var kinds=new HashSet<String>();
        for(var source:List.of(after,afterEnder))for(int i=source.size()-1;i>=0;i--){var saved=source.getCompound(i);var item=ItemStack.of(saved);String kind=kind(item,scene);if(kind.isEmpty()||!onlyKind.isEmpty()&&!onlyKind.equals(kind))continue;
            if(item.getCount()!=1||!kinds.add(kind))return false;original.add(saved.copy());source.remove(i);
        }
        if(original.isEmpty())return true;if(data.rows.size()>=65536)return false;
        try{
            savePlayer(holder);if(!com.coraline.palace.PalaceKeyCheckpoint.inventory(StoryRunStore.read(profile(holder.server,holder.getUUID())),before))return false;
            UUID id=UUID.randomUUID();var row=new CompoundTag();row.putUUID("Id",id);row.putUUID("Owner",scene.owner);row.putUUID("Door",scene.door);row.putUUID("Holder",holder.getUUID());if(recipient!=null)row.putUUID("Recipient",recipient);row.putString("State","PREPARED");
            var frame=StorySessionData.get(holder.server).frame(holder.getUUID());if(frame==null||!frame.owner().equals(scene.owner)||!frame.door().equals(scene.door))frame=StorySessionData.get(holder.server).frame(scene.owner);if(frame!=null&&frame.owner().equals(scene.owner)&&frame.door().equals(scene.door))row.putUUID("Session",frame.id());
            var names=new ListTag();for(String name:new java.util.TreeSet<>(kinds))names.add(StringTag.valueOf(name));row.put("Kinds",names);
            row.put("Before",before);row.put("After",after);row.put("BeforeEnder",beforeEnder);row.put("AfterEnder",afterEnder);row.put("Items",original);data.rows.put(id.toString(),row);data.pending.add(id.toString());
            if(!data.checkpoint(holder.server))return false;return data.removeSource(holder.server,row,holder);
        }catch(IOException|RuntimeException unavailable){return false;}
    }
    public static boolean captureOffline(MinecraftServer server,UUID holder,VisitData.Scene scene){
        var live=server.getPlayerList().getPlayer(holder);if(live!=null)return capture(live,"",null,scene);
        var data=get(server);var frame=StorySessionData.get(server).frame(holder);
        if(data.blocked||scene==null)return false;
        if(frame==null||!frame.owner().equals(scene.owner)||!frame.door().equals(scene.door)){if(!StorySessionData.get(server).provesQuestHolder(scene.owner,scene.door,holder))return false;frame=StorySessionData.get(server).frame(scene.owner);if(frame==null)return false;}
        for(String id:data.pending){var row=data.rows.getCompound(id);if(row.getUUID("Holder").equals(holder)&&row.getString("State").equals("PREPARED"))return data.removeSource(server,row,null);}
        try{
            var path=profile(server,holder);if(Files.isSymbolicLink(path)||Files.isSymbolicLink(path.getParent()))return false;
            var nativePlayer=StoryRunStore.read(path);if(!nativePlayer.hasUUID("UUID")||!holder.equals(nativePlayer.getUUID("UUID")))return false;
            var before=nativePlayer.getList("Inventory",Tag.TAG_COMPOUND);var after=before.copy();var beforeEnder=nativePlayer.getList("EnderItems",Tag.TAG_COMPOUND);var afterEnder=beforeEnder.copy();var items=new ListTag();var kinds=new HashSet<String>();
            for(var source:List.of(after,afterEnder))for(int i=source.size()-1;i>=0;i--){var saved=source.getCompound(i);var stack=ItemStack.of(saved);String kind=kind(stack,scene);if(kind.isEmpty())continue;if(stack.getCount()!=1||!kinds.add(kind))return false;items.add(saved.copy());source.remove(i);}
            if(items.isEmpty())return true;if(data.rows.size()>=65536)return false;
            UUID id=UUID.randomUUID();var row=new CompoundTag();row.putUUID("Id",id);row.putUUID("Owner",scene.owner);row.putUUID("Holder",holder);row.putUUID("Door",scene.door);row.putUUID("Session",frame.id());row.putString("State","PREPARED");row.put("Items",items);var names=new ListTag();for(String name:new java.util.TreeSet<>(kinds))names.add(StringTag.valueOf(name));row.put("Kinds",names);row.put("Before",before.copy());row.put("After",after);row.put("BeforeEnder",beforeEnder.copy());row.put("AfterEnder",afterEnder);data.rows.put(id.toString(),row);data.pending.add(id.toString());
            return data.checkpoint(server)&&data.removeSource(server,row,null);
        }catch(IOException|RuntimeException unavailable){return false;}
    }
    public static boolean recall(ServerPlayer owner,VisitData.Scene scene,String kind){
        if(!owner.getUUID().equals(scene.owner))return false;var sessions=StorySessionData.get(owner.server);
        for(UUID holder:sessions.questHolders(scene.owner))if(!holder.equals(scene.owner)){
            var live=owner.server.getPlayerList().getPlayer(holder);
            if(live!=null){if(!capture(live,kind,null,scene))return false;}else if(!captureOffline(owner.server,holder,scene))return false;
        }
        var data=get(owner.server);for(String id:new ArrayList<>(data.pending)){var row=data.rows.getCompound(id);if(row.getUUID("Owner").equals(scene.owner)&&row.getUUID("Door").equals(scene.door)&&!row.hasUUID("Recipient")){if(!data.removeSource(owner.server,row,null)||!data.deliver(owner.server,row))return false;}}return true;
    }
    private boolean removeSource(MinecraftServer server,CompoundTag row,ServerPlayer actualHolder){
        if(!row.getString("State").equals("PREPARED"))return true;
        try{
            if(!checkpoint(server))return false;
            if(row.hasUUID("Session")&&!StorySessionData.get(server).linkCustody(server,row.getUUID("Session"),row.getUUID("Owner"),row.getUUID("Door"),row.getUUID("Holder"),row.getUUID("Id")))return false;
            var path=profile(server,row.getUUID("Holder"));if(Files.isSymbolicLink(path)||Files.isSymbolicLink(path.getParent()))return false;
            var disk=StoryRunStore.read(path);if(!disk.hasUUID("UUID")||!row.getUUID("Holder").equals(disk.getUUID("UUID")))return false;String receipt=row.getUUID("Id").toString();
            if(disk.getCompound("ForgeData").getCompound(REMOVALS).getBoolean(receipt)){row.putString("State","STORED");for(String key:List.of("Before","After","BeforeEnder","AfterEnder"))row.remove(key);return checkpoint(server);}
            var live=actualHolder!=null?actualHolder:server.getPlayerList().getPlayer(row.getUUID("Holder"));
            if(live!=null){
                var current=new CompoundTag();current.put("Inventory",live.getInventory().save(new ListTag()));current.put("EnderItems",ender(live));
                if(sameSource(current,row,"Before")){live.getInventory().load(row.getList("After",Tag.TAG_COMPOUND));ender(live,row.getList("AfterEnder",Tag.TAG_COMPOUND));}
                else if(!sameSource(current,row,"After"))return false;
                var proofs=live.getPersistentData().getCompound(REMOVALS);proofs.putBoolean(receipt,true);live.getPersistentData().put(REMOVALS,proofs);
                live.getInventory().setChanged();live.inventoryMenu.broadcastFullState();savePlayer(live);
            }else{
                var before=StoryRunStore.read(path);
                if(!sameSource(before,row,"Before")&&!sameSource(before,row,"After"))return false;
                var target=before.copy();target.put("Inventory",row.getList("After",Tag.TAG_COMPOUND).copy());target.put("EnderItems",row.getList("AfterEnder",Tag.TAG_COMPOUND).copy());var forge=target.getCompound("ForgeData");var proofs=forge.getCompound(REMOVALS);proofs.putBoolean(receipt,true);forge.put(REMOVALS,proofs);target.put("ForgeData",forge);atomic(path,before,target);
            }
            var verified=StoryRunStore.read(path);if(!sameSource(verified,row,"After")||!verified.getCompound("ForgeData").getCompound(REMOVALS).getBoolean(receipt))return false;
            row.putString("State","STORED");for(String key:List.of("Before","After","BeforeEnder","AfterEnder"))row.remove(key);return checkpoint(server);
        }catch(IOException|RuntimeException unavailable){return false;}
    }
    private boolean deliver(MinecraftServer server,CompoundTag row){
        if(!row.getString("State").equals("STORED"))return row.getString("State").equals("DELIVERED");
        try{
            var storyOwner=row.getUUID("Owner");var owner=row.hasUUID("Recipient")?row.getUUID("Recipient"):storyOwner;
            if(!row.getBoolean("DeliveryPrepared")&&!owner.equals(storyOwner)&&StorySessionData.get(server).admittedOwner(owner).filter(storyOwner::equals).isEmpty()){row.remove("Recipient");owner=storyOwner;if(!checkpoint(server))return false;}
            var live=server.getPlayerList().getPlayer(owner);if(live!=null&&!live.isAlive())return false;
            if(live!=null){live.closeContainer();savePlayer(live);}
            var path=profile(server,owner);if(Files.isSymbolicLink(path)||Files.isSymbolicLink(path.getParent()))return false;
            var before=StoryRunStore.read(path);if(!before.hasUUID("UUID")||!owner.equals(before.getUUID("UUID"))||before.getFloat("Health")<=0)return false;String id=row.getUUID("Id").toString();var receipts=before.getCompound("ForgeData").getCompound(RECEIPTS);
            if(receipts.getBoolean(id)){if(row.hasUUID("Session")&&!StorySessionData.get(server).questHolder(server,row.getUUID("Session"),owner,row.getUUID("Id"),row.getList("Kinds",Tag.TAG_STRING)))return false;row.putString("State","DELIVERED");pending.remove(id);compact(row);boolean metadataSaved=checkpoint(server);if(metadataSaved&&row.hasUUID("Session"))StorySessionData.get(server).finishCustody(server,row.getUUID("Session"),row.getUUID("Id"));return metadataSaved;}
            if(receipts.size()>=65536)return false;
            boolean preparedMatches=row.getBoolean("DeliveryPrepared")
                &&(com.coraline.palace.PalaceKeyCheckpoint.inventory(before,row.getList("TargetBefore",Tag.TAG_COMPOUND))
                    ||com.coraline.palace.PalaceKeyCheckpoint.inventory(before,row.getList("TargetAfter",Tag.TAG_COMPOUND)));
            if(!preparedMatches){
                var snapshot=new CompoundTag();snapshot.put("Inventory",row.getBoolean("DeliveryPrepared")
                    ?missingDeliveryItems(before,row):row.getList("Items",Tag.TAG_COMPOUND).copy());
                var plan=GroupInventoryEscrow.planNative(before.getList("Inventory",Tag.TAG_COMPOUND),snapshot);if(!plan.getList("Overflow",Tag.TAG_COMPOUND).isEmpty())return false;
                row.put("TargetBefore",before.getList("Inventory",Tag.TAG_COMPOUND).copy());row.put("TargetAfter",plan.getList("Inventory",Tag.TAG_COMPOUND));row.putBoolean("DeliveryPrepared",true);if(!checkpoint(server))return false;
            }
            if(!com.coraline.palace.PalaceKeyCheckpoint.inventory(before,row.getList("TargetBefore",Tag.TAG_COMPOUND))&&!com.coraline.palace.PalaceKeyCheckpoint.inventory(before,row.getList("TargetAfter",Tag.TAG_COMPOUND)))return false;
            var target=before.copy();target.put("Inventory",row.getList("TargetAfter",Tag.TAG_COMPOUND).copy());
            var forge=target.getCompound("ForgeData");var proof=forge.getCompound(RECEIPTS);proof.putBoolean(id,true);forge.put(RECEIPTS,proof);target.put("ForgeData",forge);
            if(live!=null){
                live.getInventory().load(target.getList("Inventory",Tag.TAG_COMPOUND));live.getPersistentData().put(RECEIPTS,proof.copy());live.getInventory().setChanged();live.inventoryMenu.broadcastFullState();savePlayer(live);
            }else atomic(path,before,target);
            var saved=StoryRunStore.read(path);if(!saved.getCompound("ForgeData").getCompound(RECEIPTS).getBoolean(id)||!com.coraline.palace.PalaceKeyCheckpoint.inventory(saved,row.getList("TargetAfter",Tag.TAG_COMPOUND)))return false;
            if(row.hasUUID("Session")&&!StorySessionData.get(server).questHolder(server,row.getUUID("Session"),owner,row.getUUID("Id"),row.getList("Kinds",Tag.TAG_STRING)))return false;
            row.putString("State","DELIVERED");pending.remove(id);compact(row);boolean metadataSaved=checkpoint(server);if(metadataSaved&&row.hasUUID("Session"))StorySessionData.get(server).finishCustody(server,row.getUUID("Session"),row.getUUID("Id"));return metadataSaved;
        }catch(IOException|RuntimeException unavailable){return false;}
    }
    private static void compact(CompoundTag row){for(String key:List.of("Items","TargetBefore","TargetAfter"))row.remove(key);}
    private static void atomic(Path file,CompoundTag expected,CompoundTag target)throws IOException{
        if(!StoryRunStore.read(file).equals(expected))throw new IOException("Native custody profile changed");
        var temp=Files.createTempFile(file.getParent(),file.getFileName()+"-quest-",".pending");
        try{NbtIo.writeCompressed(target,temp.toFile());try(var channel=FileChannel.open(temp,StandardOpenOption.WRITE)){channel.force(true);}if(!target.equals(StoryRunStore.read(temp)))throw new IOException("Custody checkpoint mismatch");Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);if(!target.equals(StoryRunStore.read(file)))throw new IOException("Custody commit mismatch");}
        finally{Files.deleteIfExists(temp);}
    }
    /** The complete-ending return boundary drains original items before owner
     * progression/session retirement. Ordinary replay never clears this vault. */
    public static boolean drainForEnding(MinecraftServer server){
        var data=get(server);if(data.blocked)return false;
        for(String id:data.rows.getAllKeys()){var row=data.rows.getCompound(id);if(!data.removeSource(server,row,null)||!data.deliver(server,row))return false;}return true;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)capture(p);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p&&!capture(p)&&pendingSource(p)){e.setCanceled(true);if(p.getHealth()<=0)p.setHealth(1);p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Quest-item recovery is waiting for its save checkpoint."));}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void handoff(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract e){
        if(!(e.getEntity() instanceof ServerPlayer p)||!(e.getTarget() instanceof ServerPlayer target)||!p.isShiftKeyDown()||e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;
        String kind=kind(e.getItemStack(),scope(p));if(kind.isEmpty())return;
        if(handoff(p,target.getUUID(),kind)){e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void waitItem(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem e){if(e.getEntity() instanceof ServerPlayer p&&pendingSource(p))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void waitPickup(net.minecraftforge.event.entity.player.EntityItemPickupEvent e){if(e.getEntity() instanceof ServerPlayer p&&(pendingSource(p)||guarded(p,p.containerMenu.getCarried())))e.setCanceled(true);}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||e.getServer().getTickCount()%20!=0)return;var server=e.getServer();var data=get(server);if(data.blocked||data.pending.isEmpty())return;
        var ids=new ArrayList<>(data.pending);int processed=0;
        for(int tries=0;tries<ids.size()&&processed<2;tries++){var row=data.rows.getCompound(ids.get(Math.floorMod(data.cursor++,ids.size())));if(row.getString("State").equals("DELIVERED"))continue;processed++;if(data.removeSource(server,row,null))data.deliver(server,row);}
    }
    @Override public CompoundTag save(CompoundTag tag){tag.putInt("Version",1);tag.put("Rows",rows.copy());if(blocked)tag.putBoolean("Blocked",true);return tag;}
    private QuestItemRecovery(){this(fresh());}
}
