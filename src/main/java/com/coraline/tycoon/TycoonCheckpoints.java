package com.coraline.tycoon;
import java.io.IOException;
import java.util.UUID;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
/** The irreversible unlock pairs a durable owner intent with the consumed-item player receipt. */
public final class TycoonCheckpoints {
    public static boolean home(ServerPlayer p)throws IOException {return home(p.server,TycoonWorld.home(p));}
    public static boolean home(net.minecraft.server.MinecraftServer server,TycoonData.Home h)throws IOException {
        var file=server.getWorldPath(LevelResource.ROOT).resolve("data/coraline_tycoon.dat");if(java.nio.file.Files.isSymbolicLink(file)||java.nio.file.Files.isSymbolicLink(file.getParent()))return false;
        TycoonData.get(server).setDirty();server.overworld().getDataStorage().save();var saved=com.coraline.rebirth.StoryRunStore.read(file).getCompound("data");for(var tag:saved.getList("Homes",10)){var row=(CompoundTag)tag;if(row.hasUUID("Owner")&&h.owner.equals(row.getUUID("Owner")))return row.equals(h.save());}return false;
    }
    public static UUID receipt(ServerPlayer p){var persisted=p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);return persisted.hasUUID("TycoonUnlockReceipt")?persisted.getUUID("TycoonUnlockReceipt"):null;}
    public static void receipt(ServerPlayer p,UUID id){var persisted=p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);if(id==null)persisted.remove("TycoonUnlockReceipt");else persisted.putUUID("TycoonUnlockReceipt",id);p.getPersistentData().put(Player.PERSISTED_NBT_TAG,persisted);}
    public static boolean player(ServerPlayer p)throws IOException {p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();p.server.getPlayerList().saveAll();var saved=com.coraline.rebirth.StoryRunStore.read(p.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat"));var data=saved.getCompound("ForgeData").getCompound(Player.PERSISTED_NBT_TAG);boolean marker=receipt(p)==null?!data.hasUUID("TycoonUnlockReceipt"):data.hasUUID("TycoonUnlockReceipt")&&receipt(p).equals(data.getUUID("TycoonUnlockReceipt"));var placement=TycoonPlacements.receipt(p);boolean paid=placement==null?!data.hasUUID("TycoonPlacementReceipt"):data.hasUUID("TycoonPlacementReceipt")&&placement.equals(data.getUUID("TycoonPlacementReceipt"));var transaction=TycoonTransactions.receipt(p);boolean charged=transaction==null?!data.hasUUID("TycoonTransactionReceipt"):data.hasUUID("TycoonTransactionReceipt")&&transaction.equals(data.getUUID("TycoonTransactionReceipt"));return marker&&paid&&charged&&com.coraline.palace.PalaceKeyCheckpoint.inventory(saved,p.getInventory().save(new ListTag()));}
    public static boolean reconcile(ServerPlayer p){var h=TycoonWorld.home(p);if(h.active()||!h.state.hasUUID("UnlockIntent")||!h.state.getBoolean("PlatformBuilt")||!h.state.getUUID("UnlockIntent").equals(receipt(p)))return false;h.state.putBoolean("Activated",true);h.milestone("world_woven",50);TycoonWorld.dirty(p);return true;}
    public static boolean commit(ServerPlayer p){var h=TycoonWorld.home(p);if(h.active())return false;if(!p.getMainHandItem().is(TycoonContent.WEAVER.get()))return false;
        if(!h.state.hasUUID("UnlockIntent"))h.state.putUUID("UnlockIntent",UUID.randomUUID());UUID id=h.state.getUUID("UnlockIntent"),old=receipt(p);var original=p.getMainHandItem().copy();boolean consumed=false,committed=false;
        try{if(!home(p))throw new IOException("Owner intent read-back failed");p.getMainHandItem().shrink(1);consumed=true;receipt(p,id);if(!player(p))throw new IOException("Consumed Weaver receipt read-back failed");committed=true;h.state.putBoolean("Activated",true);h.milestone("world_woven",50);TycoonWorld.dirty(p);if(!home(p))TycoonWorld.message(p,"Unlock receipt saved; owner checkpoint will reconcile on reload.");return true;}
        catch(IOException|RuntimeException failure){if(committed){h.state.putBoolean("Activated",true);TycoonWorld.dirty(p);TycoonWorld.message(p,"Your consumed-Weaver receipt is safe; the owner checkpoint will reconcile on reload.");return true;}if(consumed){p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,original);receipt(p,old);p.getInventory().setChanged();p.server.getPlayerList().saveAll();}h.state.putBoolean("Activated",false);h.state.putString("UnlockBlocker",failure.getMessage()==null?failure.getClass().getSimpleName():failure.getMessage());TycoonWorld.dirty(p);return false;}
    }
}
