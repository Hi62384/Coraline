package com.coraline.postgame;

import java.util.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.io.IOException;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** A payment and its output occupy the same saved player inventory snapshot.
 * The durable receipt resolves disconnect/crash ambiguity without ever
 * granting an output during recovery. One pending journal per player. */
@Mod.EventBusSubscriber(modid="coraline")
final class MirrorTransactions {
    enum Result { COMMITTED, FAILED, FROZEN, COOLDOWN }
    private static final String RECEIPT="CoralineMirrorLastReceipt";
    private static final Map<MinecraftServer,Map<UUID,Long>> ATTEMPTS=new WeakHashMap<>();
    private static void savePlayer(ServerPlayer p){((com.coraline.mixin.MirrorPlayerSaveAccess)p.server.getPlayerList()).coraline$saveMirrorPlayer(p);}
    private static CompoundTag persisted(ServerPlayer p)throws IOException{
        var root=p.getPersistentData();if(root.contains(Player.PERSISTED_NBT_TAG)&&!root.contains(Player.PERSISTED_NBT_TAG,Tag.TAG_COMPOUND))throw new IOException("Unsupported player persistence");
        var n=root.getCompound(Player.PERSISTED_NBT_TAG);if(n.contains(RECEIPT)&&!n.hasUUID(RECEIPT))throw new IOException("Unsupported Mirror receipt");return n;
    }
    private static Tag receipt(CompoundTag playerData){return playerData.getCompound("ForgeData").getCompound(Player.PERSISTED_NBT_TAG).get(RECEIPT);}
    private static Path journal(ServerPlayer p)throws IOException{
        var data=p.server.getWorldPath(LevelResource.ROOT).resolve("data");var folder=data.resolve("coraline_mirror_transfers");
        if(Files.isSymbolicLink(data)||Files.isSymbolicLink(folder))throw new IOException("Ambiguous transfer directory");Files.createDirectories(folder);
        var file=folder.resolve(p.getUUID()+".dat");if(Files.isSymbolicLink(file))throw new IOException("Ambiguous transfer journal");return file;
    }
    private static CompoundTag diskPlayer(ServerPlayer p)throws IOException{return com.coraline.rebirth.StoryRunStore.read(p.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat"));}
    private static void write(Path target,CompoundTag data)throws IOException{
        var temporary=Files.createTempFile(target.getParent(),"mirror-",".pending");
        try{NbtIo.writeCompressed(data,temporary.toFile());try(var channel=FileChannel.open(temporary,StandardOpenOption.WRITE)){channel.force(true);}
            if(!com.coraline.rebirth.StoryRunStore.read(temporary).equals(data))throw new IOException("Transfer journal mismatch");
            Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temporary);}
    }
    private static boolean valid(ServerPlayer p,CompoundTag n){
        if(!n.contains("Version",Tag.TAG_INT)||n.getInt("Version")!=1||!n.hasUUID("Owner")||!n.getUUID("Owner").equals(p.getUUID())||!n.hasUUID("Nonce")||!n.contains("Phase",Tag.TAG_STRING)||!Set.of("PREPARED","COMMITTED").contains(n.getString("Phase"))||!n.contains("Changes",Tag.TAG_LIST))return false;
        if(n.contains("BeforeReceipt")&&!n.hasUUID("BeforeReceipt"))return false;var rows=n.getList("Changes",Tag.TAG_COMPOUND);if(rows.isEmpty()||rows.size()>37||rows.size()!=((ListTag)n.get("Changes")).size())return false;
        var slots=new HashSet<Integer>();for(var raw:rows){var row=(CompoundTag)raw;int slot=row.getInt("Slot");if(!row.contains("Slot",Tag.TAG_INT)||slot<0||slot>40||slot>=36&&slot<40||!slots.add(slot)||!row.contains("Before",Tag.TAG_COMPOUND)||!row.contains("After",Tag.TAG_COMPOUND))return false;}return true;
    }
    static boolean recover(ServerPlayer p){
        try{
            var path=journal(p);if(Files.notExists(path,LinkOption.NOFOLLOW_LINKS))return true;if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))return false;
            var record=com.coraline.rebirth.StoryRunStore.read(path);if(!valid(p,record))return false;
            if(record.getString("Phase").equals("COMMITTED")){Files.delete(path);return true;}
            var saved=receipt(diskPlayer(p));var nonce=NbtUtils.createUUID(record.getUUID("Nonce"));var prior=record.get("BeforeReceipt");
            if(Objects.equals(saved,nonce)||Objects.equals(saved,prior)){
                // Both outcomes are already represented by the loaded player's
                // inventory. Recovery never inserts, refunds or replays items.
                var live=persisted(p).get(RECEIPT);if(!Objects.equals(live,saved))return false;Files.delete(path);return true;
            }
            return false;
        }catch(IOException|RuntimeException bad){return false;}
    }
    private static boolean savedSlots(ServerPlayer p,MirrorWallet.Plan plan,boolean after,Tag expectedReceipt)throws IOException{
        var file=diskPlayer(p);if(!Objects.equals(receipt(file),expectedReceipt))return false;
        var inventory=file.getList("Inventory",Tag.TAG_COMPOUND);
        for(var change:plan.changes()){
            int target=change.slot()==40?150:change.slot();ItemStack actual=ItemStack.EMPTY;
            for(var raw:inventory){var n=(CompoundTag)raw;if((n.getByte("Slot")&255)==target){actual=ItemStack.of(n);break;}}
            if(!ItemStack.matches(actual,after?change.after():change.before()))return false;
        }return true;
    }
    private static final String RESEARCH_RECEIPT="CoralineResearchPayment";
    static Result researchPayment(ServerPlayer p,UUID nonce){return externalStatus(p,nonce,RESEARCH_RECEIPT);}
    static Result questRewardStatus(ServerPlayer p,UUID nonce,boolean coraline){return externalStatus(p,nonce,coraline?"CoralineQuestReward":"WybieQuestReward");}
    private static Result externalStatus(ServerPlayer p,UUID nonce,String key){try{var live=persisted(p).get(key);var disk=diskPlayer(p).getCompound("ForgeData").getCompound(Player.PERSISTED_NBT_TAG).get(key);if(!Objects.equals(live,disk)||live!=null&&!(live instanceof IntArrayTag))return Result.FROZEN;return Objects.equals(live,NbtUtils.createUUID(nonce))?Result.COMMITTED:Result.FAILED;}catch(IOException|RuntimeException failed){return Result.FROZEN;}}
    static Result commitResearch(ServerPlayer p,MirrorWallet.Plan plan,UUID nonce){return commit(p,plan,nonce,RESEARCH_RECEIPT);}
    static Result commitQuest(ServerPlayer p,MirrorWallet.Plan plan,UUID nonce,boolean coraline){return commit(p,plan,nonce,coraline?"CoralineQuestReward":"WybieQuestReward");}
    static Result commit(ServerPlayer p,MirrorWallet.Plan plan,UUID nonce){return commit(p,plan,nonce,null);}
    private static Result commit(ServerPlayer p,MirrorWallet.Plan plan,UUID nonce,String extraReceipt){
        if(!p.server.isSameThread()||!p.isAlive()||p.isSpectator()||!MirrorWallet.matches(p,plan,false)||!recover(p))return Result.FROZEN;
        long now=p.server.overworld().getGameTime();var attempts=ATTEMPTS.computeIfAbsent(p.server,s->new HashMap<>());Long last=attempts.get(p.getUUID());if(last!=null&&now>=last&&now-last<10)return Result.COOLDOWN;attempts.put(p.getUUID(),now);
        Tag beforeReceipt,beforeExtra;Path target;
        try{
            beforeExtra=extraReceipt==null?null:persisted(p).get(extraReceipt);if(extraReceipt!=null&&beforeExtra!=null&&!persisted(p).hasUUID(extraReceipt))return Result.FROZEN;if(beforeExtra!=null)beforeExtra=beforeExtra.copy();beforeReceipt=persisted(p).get(RECEIPT);if(beforeReceipt!=null)beforeReceipt=beforeReceipt.copy();savePlayer(p);
            if(!savedSlots(p,plan,false,beforeReceipt))return Result.FROZEN;target=journal(p);
            var record=new CompoundTag();record.putInt("Version",1);record.putUUID("Owner",p.getUUID());record.putUUID("Nonce",nonce);record.putString("Phase","PREPARED");if(beforeReceipt!=null)record.put("BeforeReceipt",beforeReceipt.copy());
            var changes=new ListTag();for(var change:plan.changes()){var row=new CompoundTag();row.putInt("Slot",change.slot());row.put("Before",change.before().save(new CompoundTag()));row.put("After",change.after().save(new CompoundTag()));changes.add(row);}record.put("Changes",changes);write(target,record);
            // No player packet can interleave this main-thread mutation/save.
            try{MirrorWallet.apply(p,plan,true);var persisted=persisted(p);persisted.putUUID(RECEIPT,nonce);if(extraReceipt!=null)persisted.putUUID(extraReceipt,nonce);p.getPersistentData().put(Player.PERSISTED_NBT_TAG,persisted);savePlayer(p);if(!savedSlots(p,plan,true,NbtUtils.createUUID(nonce))||extraReceipt!=null&&!Objects.equals(diskPlayer(p).getCompound("ForgeData").getCompound(Player.PERSISTED_NBT_TAG).get(extraReceipt),NbtUtils.createUUID(nonce)))throw new IOException("Unconfirmed Mirror inventory save");}
            catch(IOException|RuntimeException failed){
                // Remove unconfirmed output before it can become spendable.
                try{MirrorWallet.apply(p,plan,false);var restore=persisted(p);if(beforeReceipt==null)restore.remove(RECEIPT);else restore.put(RECEIPT,beforeReceipt.copy());if(extraReceipt!=null){if(beforeExtra==null)restore.remove(extraReceipt);else restore.put(extraReceipt,beforeExtra.copy());}p.getPersistentData().put(Player.PERSISTED_NBT_TAG,restore);savePlayer(p);if(!savedSlots(p,plan,false,beforeReceipt))throw new IOException("Rollback checkpoint is unconfirmed");Files.deleteIfExists(target);return Result.FAILED;}
                catch(IOException|RuntimeException ambiguous){p.closeContainer();p.connection.disconnect(Component.literal("The game could not confirm a safe inventory save. Reconnect after the save problem is resolved."));return Result.FROZEN;}
            }
            record.putString("Phase","COMMITTED");try{write(target,record);Files.deleteIfExists(target);}catch(IOException cleanup){/* Saved player receipt resolves this journal without another payout. */}
            p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();return Result.COMMITTED;
        }catch(IOException|RuntimeException failed){return Result.FROZEN;}
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer p&&!recover(p))p.sendSystemMessage(Component.literal("The Magic Mirror is paused while an earlier transfer record needs recovery."));}
    private MirrorTransactions(){}
}
