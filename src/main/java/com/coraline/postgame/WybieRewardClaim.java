package com.coraline.postgame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;

final class WybieRewardClaim {
    static final String KEY="CoralineWybieRescueReward";
    static final String PERSISTED=net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG;
    enum NativeReceipt { PRESENT, ABSENT, UNVERIFIED }
    enum Action { NONE, LEGACY_UNVERIFIED, COMPLETE, CHECKPOINT, DELIVER }

    static Action action(CompoundTag row,CompoundTag receipts,UUID player){
        if(!row.hasUUID("Completion")||"SURVIVED".equals(row.getString("Origin")))return Action.NONE;
        if(!row.hasUUID("RewardRecipient")){
            if(!row.getBoolean("RewardIssued"))return Action.NONE;
            return receipts.getBoolean(row.getUUID("Completion").toString())?Action.COMPLETE:Action.LEGACY_UNVERIFIED;
        }
        if(!player.equals(row.getUUID("RewardRecipient")))return Action.NONE;
        if(row.getBoolean("RewardIssued"))return Action.COMPLETE;
        return receipts.getBoolean(row.getUUID("Completion").toString())?Action.CHECKPOINT:Action.DELIVER;
    }

    static NativeReceipt nativeReceipt(Path current,UUID owner,UUID completion){
        var previous=current.resolveSibling(current.getFileName()+"_old");
        var primary=read(current,owner,completion);
        var backup=Files.notExists(previous,LinkOption.NOFOLLOW_LINKS)
                ?NativeReceipt.ABSENT:read(previous,owner,completion);
        if(primary==NativeReceipt.PRESENT||backup==NativeReceipt.PRESENT)return NativeReceipt.PRESENT;
        return primary==NativeReceipt.ABSENT&&backup==NativeReceipt.ABSENT?NativeReceipt.ABSENT:NativeReceipt.UNVERIFIED;
    }

    private static NativeReceipt read(Path file,UUID owner,UUID completion){
        try{
            if(Files.isSymbolicLink(file.getParent()))return NativeReceipt.UNVERIFIED;
            var player=com.coraline.rebirth.StoryRunStore.read(file);
            if(player==null||!player.hasUUID("UUID")||!owner.equals(player.getUUID("UUID")))return NativeReceipt.UNVERIFIED;
            for(String key:new String[]{"ForgeData",PERSISTED,KEY}){
                if(player.contains(key)&&!player.contains(key,Tag.TAG_COMPOUND))return NativeReceipt.UNVERIFIED;
                player=player.getCompound(key);
            }
            String id=completion.toString();
            if(!player.contains(id))return NativeReceipt.ABSENT;
            return player.contains(id,Tag.TAG_BYTE)&&player.getByte(id)==1?NativeReceipt.PRESENT:NativeReceipt.UNVERIFIED;
        }catch(IOException|RuntimeException unavailable){
            return NativeReceipt.UNVERIFIED;
        }
    }

    private WybieRewardClaim(){}
}
