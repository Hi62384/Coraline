package com.coraline.postgame;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

final class WybieRewardClaim {
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

    private WybieRewardClaim(){}
}
