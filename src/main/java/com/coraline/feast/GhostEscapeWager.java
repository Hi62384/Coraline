package com.coraline.feast;

import java.util.UUID;
import com.coraline.secrets.SecretsNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Owner authenticated wager dialog opened only after physical interception. */
public final class GhostEscapeWager {
    static boolean conversationPending(CompoundTag e,long clock){
        return e.hasUUID("WagerConversation")&&clock<=e.getLong("WagerConversationUntil");
    }
    static boolean responseMatches(CompoundTag e,UUID nonce,String value,long clock){
        // Older prepared saves already have the expiry, from which the original
        // twenty-tick presentation delay can be recovered without a new token.
        long ready=e.contains("WagerConversationReadyAt")?e.getLong("WagerConversationReadyAt"):e.getLong("WagerConversationUntil")-580;
        return ("0".equals(value)||"1".equals(value))&&nonce!=null&&conversationPending(e,clock)
            &&nonce.equals(e.getUUID("WagerConversation"))&&clock>=ready;
    }
    public static boolean open(ServerPlayer p,VisitData.Scene s,CompoundTag ghost){
        var e=ghost.getCompound(GhostEscapeProgress.KEY);var mother=AcceptedButtonEyesWorld.mother(p,s);
        if(!GhostVictimMirrors.owned(p,s,ghost)||!GhostEscapeProgress.owns(ghost,e)||GhostEscapeProgress.phase(e)!=GhostEscapeProgress.Phase.INTERCEPT
            ||!e.getBoolean("PlayerEyesHeld")||mother==null||mother.level()!=p.level()||mother.distanceToSqr(p)>64)return false;
        long clock=GhostVictimProgress.clock(ghost);if(conversationPending(e,clock))return true;
        UUID nonce=UUID.randomUUID();e.putUUID("WagerConversation",nonce);e.putLong("WagerConversationUntil",clock+600);e.putLong("WagerConversationReadyAt",clock+20);
        var packet=new CompoundTag();packet.putBoolean("VisitConversation",true);packet.putUUID("Nonce",nonce);packet.putString("Speaker","Other Mother");
        packet.putString("Text","You have the eyes, and I have the child. Make a wager: if the child reaches the Little Door, you may leave with them.");
        packet.put("Options",com.coraline.secrets.SecretsState.strings(java.util.List.of("I accept. Let the child go.","I refuse.")));
        packet.putInt("ReadyTicks",20);SecretsNetwork.send(p,packet);return true;
    }
    public static boolean choice(ServerPlayer p,VisitData.Scene s,CompoundTag ghost,UUID nonce,String value){
        var e=ghost.getCompound(GhostEscapeProgress.KEY);var mother=AcceptedButtonEyesWorld.mother(p,s);
        if(!GhostVictimMirrors.owned(p,s,ghost)||!GhostEscapeProgress.owns(ghost,e)||GhostEscapeProgress.phase(e)!=GhostEscapeProgress.Phase.INTERCEPT
            ||!responseMatches(e,nonce,value,GhostVictimProgress.clock(ghost))
            ||!e.getBoolean("PlayerEyesHeld")||mother==null||mother.level()!=p.level()||mother.distanceToSqr(p)>64)return false;
        e.remove("WagerConversation");e.remove("WagerConversationUntil");e.remove("WagerConversationReadyAt");
        if(!"0".equals(value)){
            GhostEscapeProgress.move(e,GhostEscapeProgress.Phase.APPROACH_EXIT,GhostVictimProgress.clock(ghost));
            GhostVictimDirector.words(ghost,"Then we have no wager. I'll keep looking for another chance.");GhostVictimMirrors.close(p);OtherMotherLoop.changed(p);return true;
        }
        if(!GhostEscapeProgress.wagerAgreed(ghost,true,true))return false;
        GhostVictimDirector.words(ghost,"You have your wager. Try to reach the Little Door.");
        GhostEscapeHunt.start(p,s,ghost);OtherMotherLoop.changed(p);return true;
    }
    private GhostEscapeWager(){}
}
