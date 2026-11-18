package com.coraline.postgame;

import java.util.HashSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;

public final class RecoveryJournalStateCheck {
    private static void require(boolean condition,String reason){
        if(!condition)throw new AssertionError(reason);
    }

    public static void main(String[] args)throws Exception{
        var generation=UUID.randomUUID();
        var owner=UUID.randomUUID();
        var raid=UUID.randomUUID();
        var actor=UUID.randomUUID();
        var root=new CompoundTag();
        root.putInt("Version",1);
        root.putUUID("Generation",generation);
        root.putLong("NextGlobal",0);
        root.put("Players",new CompoundTag());
        var raids=new CompoundTag();
        root.put("Raids",raids);
        var row=new CompoundTag();
        raids.put(raid.toString(),row);
        row.putUUID("Owner",owner);
        row.putLong("Born",20);
        row.putBoolean("Done",false);
        row.putBoolean("Pending",true);
        var member=new CompoundTag();
        member.putUUID("Entity",actor);
        member.putBoolean("Dead",false);
        var members=new ListTag();
        members.add(member);
        row.put("Members",members);

        var constructor=BreachRaidData.class.getDeclaredConstructor(UUID.class,CompoundTag.class);
        constructor.setAccessible(true);
        var pending=constructor.newInstance(generation,TagParser.parseTag(root.toString()));
        require(pending.raid(raid)!=null,"Reload lost the pending encounter identity");
        require(!pending.member(raid,actor),"Half-admitted entity counts as an active invader");
        pending.reconcile(40,false);
        require(pending.raid(raid).getBoolean("Done"),"Interrupted admission still holds an active slot");
        require(pending.ready(owner,40),"Interrupted admission consumed cooldown");
        row.remove("Pending");
        root.getCompound("Players").putLong(owner.toString(),24020);
        root.putLong("NextGlobal",1220);
        var legacy=constructor.newInstance(generation,TagParser.parseTag(root.toString()));
        legacy.reconcile(40,false);
        require(legacy.member(raid,actor),"Existing active raid failed to load");
        require(!legacy.ready(owner,1220),"Existing raid cooldown cleared on reload");

        var rescue=new CompoundTag();
        rescue.putUUID("Actor",actor);
        rescue.putUUID("Door",UUID.randomUUID());
        rescue.putUUID("WellSession",UUID.randomUUID());
        rescue.putUUID("Completion",UUID.randomUUID());
        rescue.putString("Dimension","minecraft:overworld");
        rescue.putLong("Position",BlockPos.ZERO.asLong());
        rescue.putString("Stage","COMPLETE");
        rescue.putBoolean("RewardIssued",false);
        var valid=WybieRescueData.class.getDeclaredMethod("valid",CompoundTag.class);
        valid.setAccessible(true);
        require(!(Boolean)valid.invoke(null,rescue),"Unpaid reward without an owner was accepted");
        rescue.putUUID("RewardRecipient",owner);
        require((Boolean)valid.invoke(null,TagParser.parseTag(rescue.toString())),"Pinned reward did not survive reload");
        rescue.remove("RewardRecipient");
        rescue.putBoolean("RewardIssued",true);
        require((Boolean)valid.invoke(null,TagParser.parseTag(rescue.toString())),"Legacy issued reward failed to load");

        var receipts=new CompoundTag();
        require(WybieRewardClaim.action(rescue,receipts,owner)==WybieRewardClaim.Action.LEGACY_UNVERIFIED,
                "Older issued marker authorized a second delivery");
        rescue.putUUID("RewardRecipient",owner);
        rescue.putBoolean("RewardIssued",false);
        require(WybieRewardClaim.action(rescue,receipts,actor)==WybieRewardClaim.Action.NONE,"Another player claimed the reward");
        require(WybieRewardClaim.action(rescue,receipts,owner)==WybieRewardClaim.Action.DELIVER,"Unpaid owner could not claim");
        receipts.putBoolean(rescue.getUUID("Completion").toString(),true);
        require(WybieRewardClaim.action(TagParser.parseTag(rescue.toString()),TagParser.parseTag(receipts.toString()),owner)
                ==WybieRewardClaim.Action.CHECKPOINT,"Reconnect attempted to pay an existing receipt again");

        checkNativeRewardReceipts(owner,actor,rescue.getUUID("Completion"));

        var palace=new BlockPos(120,70,90);
        var candidates=new HashSet<BlockPos>();
        for(int i=0;i<BreachDriveController.CANDIDATES;i++){
            var at=BreachDriveController.candidate(palace,i);
            require(candidates.add(at),"Repeated Drive site");
            require(at.distSqr(palace)<72*72,"Drive search strayed outside the garden vicinity");
        }
        require(candidates.contains(palace.offset(28,0,-40)),"Original Drive site omitted");
        System.out.println("RecoveryJournalStateCheck passed");
    }

    private static void checkNativeRewardReceipts(UUID owner,UUID other,UUID completion)throws Exception{
        var folder=java.nio.file.Files.createTempDirectory("coraline-wybie-reward-");
        var current=folder.resolve(owner+".dat");
        var backup=folder.resolve(owner+".dat_old");
        try{
            require(WybieRewardClaim.nativeReceipt(current,owner,completion)==WybieRewardClaim.NativeReceipt.UNVERIFIED,
                    "Missing player file proved non-delivery");
            writePlayer(current,owner,null);
            require(WybieRewardClaim.nativeReceipt(current,owner,completion)==WybieRewardClaim.NativeReceipt.ABSENT,
                    "Verified unpaid player file blocked a pinned reward");
            writePlayer(backup,owner,completion);
            require(WybieRewardClaim.nativeReceipt(current,owner,completion)==WybieRewardClaim.NativeReceipt.PRESENT,
                    "Rolled-back player file hid an older successful payout");
            writePlayer(backup,other,completion);
            require(WybieRewardClaim.nativeReceipt(current,owner,completion)==WybieRewardClaim.NativeReceipt.UNVERIFIED,
                    "Foreign backup was accepted as evidence for this player");
            java.nio.file.Files.writeString(backup,"damaged");
            require(WybieRewardClaim.nativeReceipt(current,owner,completion)==WybieRewardClaim.NativeReceipt.UNVERIFIED,
                    "Damaged backup was treated as unpaid");
            writePlayer(current,owner,completion);
            require(WybieRewardClaim.nativeReceipt(current,owner,completion)==WybieRewardClaim.NativeReceipt.PRESENT,
                    "Native receipt failed to settle an interrupted reward checkpoint");
        }finally{
            java.nio.file.Files.deleteIfExists(current);
            java.nio.file.Files.deleteIfExists(backup);
            java.nio.file.Files.delete(folder);
        }
    }

    private static void writePlayer(java.nio.file.Path file,UUID owner,UUID completion)throws Exception{
        var player=new CompoundTag();
        player.putUUID("UUID",owner);
        if(completion!=null){
            var receipts=new CompoundTag();
            receipts.putBoolean(completion.toString(),true);
            var persisted=new CompoundTag();
            persisted.put(WybieRewardClaim.KEY,receipts);
            var forge=new CompoundTag();
            forge.put(WybieRewardClaim.PERSISTED,persisted);
            player.put("ForgeData",forge);
        }
        net.minecraft.nbt.NbtIo.writeCompressed(player,file.toFile());
    }
}
