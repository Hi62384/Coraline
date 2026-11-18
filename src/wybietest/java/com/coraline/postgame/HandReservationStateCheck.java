package com.coraline.postgame;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

/** Lease policy regression tests. No Minecraft runtime or world is started. */
public final class HandReservationStateCheck {
    private static int checks;
    private static void require(boolean ok,String reason){
        if(!ok)throw new AssertionError(reason);
        checks++;
    }
    public static void main(String[] args)throws Exception{
        var oldOwner=UUID.randomUUID();var door=UUID.randomUUID();
        var session=UUID.randomUUID();var hand=UUID.randomUUID();
        var incoming=UUID.randomUUID();var secondDoor=UUID.randomUUID();
        var secondSession=UUID.randomUUID();var secondHand=UUID.randomUUID();
        var lease=new CompoundTag();
        require(GlobalHandEncounter.replaceLease(lease,oldOwner,door,session,hand,100,false),"Initial claim failed");
        require(!GlobalHandEncounter.replaceLease(lease,incoming,secondDoor,secondSession,secondHand,101,true),"Active reservation was stolen");
        require(GlobalHandEncounter.observePresence(lease,120,false),"Absence start was not recorded");
        require(!GlobalHandEncounter.observePresence(lease,121,false),"Repeated observation restarted the timer");
        var reloaded=TagParser.parseTag(lease.toString());
        long deadline=120+GlobalHandEncounter.ABANDONMENT_TICKS;
        require(!GlobalHandEncounter.canClaim(reloaded,incoming,deadline-1,false),"Lease expired early after reload");
        require(!GlobalHandEncounter.canClaim(reloaded,incoming,deadline,true),"Present owner was displaced");
        require(GlobalHandEncounter.observePresence(reloaded,deadline,true),"Reconnection failed to clear the expiry clock");
        require(!GlobalHandEncounter.canClaim(reloaded,incoming,deadline+100,false),"A previous absence expired a reconnected owner");
        require(!GlobalHandEncounter.replaceLease(reloaded,oldOwner,door,session,secondHand,deadline,true),"Owner could substitute an original Hand");
        require(GlobalHandEncounter.replaceLease(reloaded,oldOwner,door,UUID.randomUUID(),hand,deadline,true),"Owner reconnect could not rotate the input session");
        GlobalHandEncounter.observePresence(reloaded,deadline+1,false);
        long takeover=deadline+1+GlobalHandEncounter.ABANDONMENT_TICKS;
        var oldNative=new CompoundTag();
        oldNative.putUUID("Hand",hand);oldNative.putUUID("KeyReceipt",UUID.randomUUID());oldNative.putString("KeyStage","HELD");
        var before=oldNative.copy();
        require(GlobalHandEncounter.replaceLease(reloaded,incoming,secondDoor,secondSession,secondHand,takeover,false),"Abandoned reservation blocked the next owner");
        require(oldNative.equals(before),"Changing arena access rewrote the previous owner's Hand or key");
        require(reloaded.getUUID("Hand").equals(secondHand)&&!GlobalHandEncounter.canClaim(reloaded,oldOwner,takeover,true),"Two owners obtained the arena");
        reloaded.putBoolean("Blocked",true);
        var blocked=reloaded.copy();
        require(!GlobalHandEncounter.replaceLease(reloaded,oldOwner,door,session,hand,Long.MAX_VALUE,false)&&reloaded.equals(blocked),"A malformed lease was overwritten");
        System.out.println("HAND_RESERVATION_OFFLINE_OK checks="+checks);
    }
}
