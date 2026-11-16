package com.coraline.secrets;

import com.coraline.feast.*;
import com.coraline.otherworld.*;
import net.minecraft.server.level.ServerPlayer;

/** Priority arbitration before the ordinary visit scheduler advances. */
public final class MotherInterrupts {
    private static boolean eligible(ServerPlayer p){
        if(CatEncounter.active(p)||BlackIsTraditional.active(p))return false;
        var scene=OtherMotherLoop.active(p);if(scene==null||AcceptedButtonEyes.active(scene)||FamilyGameNight.seated(p,scene)||!OtherWorld.isOther(p.level())||p.getPersistentData().getBoolean("CoralineInTunnel"))return false;
        // active() requires completion of the first-visit scene, protecting its gift.
        return scene.loop.phase!=OtherMotherLoopState.Phase.ACTIVITY&&scene.loop.secrets.search!=SecretsState.Search.SCRIPTED_TRAP;
    }
    public static void interrupt(ServerPlayer p){
        var s=SecretTransactions.state(p);Reconciliation.cancel(p);VisitAudio.stopVoice(p);
        var scene=OtherMotherLoop.scene(p);if(scene!=null)scene.welcomeAfter=OtherMotherLoop.now(p);
        for(String key:new String[]{"Queue","QueuePool","Playing","PlayingPool","Pool","Until","Finished","ContactPreludeFollowupPool","ContactPreludeFollowupEvent","ContactPreludeFollowupState","ReconciledProposal","LoopVoice","LoopFinished"})s.dialogue.remove(key);
        // Withdraw an undecided offer without consuming its item. Already earned
        // rewards retain their receipt/offer metadata and can be claimed afterward.
        if(s.pendingReward<0)for(String key:new String[]{"Offer","OfferSpoken","Decided","Forced","TransactionState"})s.dialogue.remove(key);
        OtherMotherLoop.clearActivityWindow(p);
    }
    public static boolean blackmail(ServerPlayer p){
        if(!eligible(p))return false;var s=SecretTransactions.state(p);
        if(s.pendingReward>=0)return SecretTransactions.resumeReward(p);
        if(s.dialogue.hasUUID("Offer"))return true;
        for(int slot=0;slot<p.getInventory().getContainerSize();slot++){
            var item=p.getInventory().getItem(slot);
            if(!item.is(SecretsItems.BLACKMAIL.get())||!SecretTransactions.owned(p,item)||!s.blackmail.containsKey(item.getTag().getUUID("SecretToken")))continue;
            interrupt(p);return SecretTransactions.offer(p,item,-1);
        }return false;
    }
    public static boolean check(ServerPlayer p){
        if(!eligible(p)||Reconciliation.active(p))return false;var s=SecretTransactions.state(p);var r=SecretWorld.realm(p);var m=MotherSearch.mother(p);
        // Pursuit has its own sight/memory/contact rules: this higher-priority
        // interruption hook must never convert a distant sighting into a catch.
        if(s.searching())return false;
        if(r==null||r.house==null||m==null||s.world.getBoolean("CaughtEncounter")||s.world.getLong("CatchUntil")>OtherMotherLoop.now(p)||s.invisibleUntil>OtherMotherLoop.now(p)||p.isInvisible()||!m.hasLineOfSight(p))return false;
        boolean evidence=false;for(var item:java.util.List.of(p.getMainHandItem(),p.getOffhandItem()))if(!SecretTransactions.evidence(p,item).isEmpty()||HouseCabinets.forbiddenKey(p,item)||item.is(SecretsItems.JOURNAL.get())&&SecretTransactions.owned(p,item)){evidence=true;break;}
        boolean wandering=(!com.coraline.feast.VisitEvents.authorized(p)&&OtherWorldEstate.outsideDistance(r.house,p.blockPosition())>=40||SecretDialogue.secretArea(p,p.blockPosition()))&&m.distanceToSqr(p)<=96*96;
        if(!wandering&&!(evidence&&m.distanceToSqr(p)<=30*30))return false;
        if(!MotherSearch.visible(m,p,wandering?96:30))return false;
        MotherSearch.forbiddenAction(p,evidence?SecretDialogue.Event.RESTRICTED_ITEM_CAUGHT:SecretDialogue.Event.SECRET_AREA_CAUGHT);
        return true;
    }
    private MotherInterrupts(){}
}
