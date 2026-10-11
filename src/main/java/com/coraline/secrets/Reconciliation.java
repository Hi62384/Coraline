package com.coraline.secrets;

import com.coraline.feast.*;
import com.coraline.otherworld.OtherWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** One owner-scoped debrief, using the existing audio queue and repeat-visit scheduler. */
public final class Reconciliation {
    public enum Category { MINOR, SERIOUS }
    public static OtherMotherDialogue.Pool pool(Category c,OtherMotherWelcome.State state){return new OtherMotherDialogue.Pool("scolding/"+c.name().toLowerCase(Locale.ROOT)+"/"+SecretDialogue.stateId(state),1);}
    public static List<OtherMotherDialogue.Pool> pools(){var out=new ArrayList<OtherMotherDialogue.Pool>();for(var c:Category.values())for(var s:OtherMotherWelcome.State.values())out.add(pool(c,s));return out;}
    public static Category category(SecretDialogue.Event event){return event==SecretDialogue.Event.ORDINARY_AREA_CAUGHT?Category.MINOR:Category.SERIOUS;}
    public static boolean active(ServerPlayer p){var s=SecretTransactions.state(p);return s!=null&&s.dialogue.contains("Reconcile");}
    public static boolean permitted(ServerPlayer p){var scene=OtherMotherLoop.active(p);return scene!=null&&SecretsRuntime.active(p)&&scene.loop.secrets.search!=SecretsState.Search.SCRIPTED_TRAP;}
    public static String stage(ServerPlayer p){var s=SecretTransactions.state(p);return active(p)?s.dialogue.getCompound("Reconcile").getString("Stage"):"IDLE";}
    public static boolean begin(ServerPlayer p,Category category,OtherMotherWelcome.State state,boolean bell){
        if(active(p)||!permitted(p))return false;
        var s=SecretTransactions.state(p);var n=new CompoundTag();n.putString("Category",category.name());n.putString("State",state.id());n.putString("Stage",bell?"BELL_RETURN":"OPENING");n.putLong("Started",OtherMotherLoop.now(p));
        n.putDouble("X",p.getX());n.putDouble("Y",p.getY());n.putDouble("Z",p.getZ());s.dialogue.put("Reconcile",n);
        s.search=bell?SecretsState.Search.RETURNING_HOME:SecretsState.Search.CONFRONTING;s.pendingTicks=0;s.searchTicks=0;
        OtherMotherLoop.clearActivityWindow(p);SecretTransactions.checkpoint(p);return true;
    }
    /** Called every owner tick, including while an audio recording is still playing. */
    public static void hold(ServerPlayer p){
        if(!active(p))return;
        if(!p.isAlive()||!permitted(p)){cancel(p);return;}
        var n=SecretTransactions.state(p).dialogue.getCompound("Reconcile");
        if(n.getString("Stage").equals("SUSPENDED")){n.putString("Stage","RESUME");n.putDouble("X",p.getX());n.putDouble("Y",p.getY());n.putDouble("Z",p.getZ());SecretTransactions.state(p).search=SecretsState.Search.CONFRONTING;}
        var anchor=new Vec3(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z"));
        // Walk and look freely nearby; only an attempt to leave the conversation
        // area returns the player to its original safe position.
        if(p.distanceToSqr(anchor)>16){p.stopRiding();p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.teleportTo(p.serverLevel(),anchor.x,anchor.y,anchor.z,p.getYRot(),p.getXRot());}
        if(n.getString("Stage").equals("SCOLDING"))SecretTransactions.confiscate(p);
        var mother=MotherSearch.mother(p);if(mother!=null){approach(mother,p);face(mother,p);}
    }
    public static boolean control(com.coraline.parents.ParentEntity m,ServerPlayer p){
        if(!active(p))return false;
        if(stage(p).equals("BELL_RETURN")){m.setNoAi(false);m.setNoGravity(false);if(m.tickCount%10==0)m.getNavigation().moveTo(p,1.2);m.getLookControl().setLookAt(p,30,30);return true;}
        approach(m,p);face(m,p);return true;
    }
    private static void approach(com.coraline.parents.ParentEntity m,ServerPlayer p){
        m.setNoAi(false);m.setNoGravity(false);m.stopRiding();
        var n=m.getPersistentData();long now=OtherMotherLoop.now(p);
        if(n.contains("ApproachTick")&&n.getLong("ApproachTick")==now)return;n.putLong("ApproachTick",now);
        double distanceToPlayer=m.distanceToSqr(p);
        if(distanceToPlayer<=(n.getBoolean("ConversationClose")?3.0625:1.5625)&&m.hasLineOfSight(p)){n.putBoolean("ConversationClose",true);n.remove("ApproachStuck");m.getNavigation().stop();return;}
        n.putBoolean("ConversationClose",false);
        if(p.tickCount%10!=0)return;
        double moved=m.distanceToSqr(n.getDouble("ApproachX"),n.getDouble("ApproachY"),n.getDouble("ApproachZ"));
        n.putInt("ApproachStuck",moved<.04?n.getInt("ApproachStuck")+10:0);
        n.putDouble("ApproachX",m.getX());n.putDouble("ApproachY",m.getY());n.putDouble("ApproachZ",m.getZ());
        m.getNavigation().moveTo(p,1.2);
        // A blocked path must not leave the speaker facing a wall throughout her speech.
        // Only move the NPC, onto supported, collision-free ground near the listener.
        if(p.tickCount%40!=0||distanceToPlayer<=36&&n.getInt("ApproachStuck")<40)return;
        var forward=p.getLookAngle();double angle=Math.atan2(forward.z,forward.x);
        for(int turn:new int[]{0,1,-1,2,-2,3,-3,4}){
            double a=angle+turn*Math.PI/4;
            var at=BlockPos.containing(p.getX()+Math.cos(a)*1.35,p.getY(),p.getZ()+Math.sin(a)*1.35);
            var pos=Vec3.atBottomCenterOf(at);
            double distance=p.position().distanceToSqr(pos);
            if(distance<.81||distance>3.24||!walkable(p,at))continue;
            var box=m.getDimensions(m.getPose()).makeBoundingBox(pos);
            if(!p.serverLevel().noCollision(m,box)||p.serverLevel().containsAnyLiquid(box))continue;
            var sight=p.serverLevel().clip(new net.minecraft.world.level.ClipContext(pos.add(0,m.getEyeHeight(),0),p.getEyePosition(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,m));
            if(sight.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)continue;
            m.getNavigation().stop();m.teleportTo(pos.x,pos.y,pos.z);n.putInt("ApproachStuck",0);n.putBoolean("ConversationClose",true);break;
        }
    }
    private static void face(com.coraline.parents.ParentEntity m,ServerPlayer p){
        var delta=p.getEyePosition().subtract(m.getEyePosition());
        float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance()));
        m.setYHeadRot(yaw);m.setXRot(pitch);
        if(m.getNavigation().isDone()){m.setYRot(yaw);m.setYBodyRot(yaw);}
        m.getLookControl().setLookAt(p,360,360);
    }
    /** Invoked only after the current queued voice has actually completed (or its bounded fallback). */
    public static void advance(ServerPlayer p){
        if(!active(p)||!permitted(p))return;var s=SecretTransactions.state(p);var n=s.dialogue.getCompound("Reconcile");String stage=n.getString("Stage");
        // Voice completion, not walking distance, advances the conversation.
        if((stage.equals("OPENING")||stage.equals("RESUME"))&&n.getString("Category").equals(Category.MINOR.name())){
            // The ordinary-area recording is the whole reprimand, not an intro
            // to a second, unrelated serious-scolding recording.
            SecretTransactions.confiscate(p);
            stage="SCOLDING";
        }
        if(stage.equals("OPENING")||stage.equals("RESUME")||stage.equals("BELL_RETURN")){
            var state=OtherMotherWelcome.State.fromId(n.getString("State"));
            if(SecretDialogue.queueScolding(p,Category.SERIOUS,state)){n.putString("Stage","SCOLDING");SecretTransactions.confiscate(p);s.search=SecretsState.Search.CONFRONTING;SecretTransactions.checkpoint(p);}return;
        }
        if(stage.equals("SCOLDING")){
            if(!returnHome(p)){cancel(p);OtherWorld.leave(p,false);return;}
            // Clear only transient conversation/search state. Never touch relationship, evidence or gifts.
            s.dialogue.remove("Reconcile");s.search=SecretsState.Search.NORMAL;s.pendingTicks=0;s.searchTicks=0;s.world.remove("SnaredUntil");
            s.world.putBoolean("CaughtEncounter",true);s.world.putLong("CatchUntil",OtherMotherLoop.now(p)+200);
            if(OtherMotherLoop.scene(p).loop.suspicion>=1){
                // There is no playable max-suspicion trap yet. Finish the scold
                // and safely end this visit instead of stranding its scheduler.
                SecretTransactions.checkpoint(p);OtherWorld.leave(p,false);return;
            }
            OtherMotherLoop.afterReconciliation(p);SecretTransactions.checkpoint(p);
        }
    }
    public static BlockPos safe(ServerPlayer p,BlockPos near){
        var at=OtherWorld.safePlayer(p.serverLevel(),near);return walkable(p,at)?at:null;
    }
    private static boolean walkable(ServerPlayer p,BlockPos at){var level=p.serverLevel();var box=p.getDimensions(p.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(at));return level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP)&&level.noCollision(box)&&!level.containsAnyLiquid(box)&&!level.getBlockState(at.below()).is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK);}
    static boolean returnHome(ServerPlayer p){
        var realm=SecretWorld.realm(p);if(realm==null)return false;var home=realm.porch==null?realm.doorPos:realm.porch;
        var at=safe(p,home);if(at==null)at=safe(p,realm.doorPos);if(at==null)return false;
        if(p.distanceToSqr(home.getCenter())>144||!walkable(p,p.blockPosition())){p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),p.getXRot());p.fallDistance=0;}
        var m=MotherSearch.mother(p);var motherAt=safe(p,p.blockPosition().east(2));if(m!=null&&motherAt!=null){m.getNavigation().stop();m.teleportTo(motherAt.getX()+.5,motherAt.getY(),motherAt.getZ()+.5);OtherMotherLoop.scene(p).loop.motherPos=m.blockPosition();}
        return true;
    }
    public static void cancel(ServerPlayer p){
        var s=SecretTransactions.state(p);if(s==null)return;
        if(active(p)){String sound=s.dialogue.getString("Playing");if(!sound.isEmpty())VisitAudio.stopVoice(p,sound);s.dialogue.remove("Reconcile");clearPresentation(s);if(s.search==SecretsState.Search.CONFRONTING||s.search==SecretsState.Search.RETURNING_HOME)s.search=SecretsState.Search.NORMAL;SecretTransactions.dirty(p);}
    }
    private static void clearPresentation(SecretsState s){for(String key:new String[]{"Queue","Playing","Pool","Until","Finished"})s.dialogue.remove(key);}
    public static void suspend(ServerPlayer p){var s=SecretTransactions.state(p);if(s==null)return;
        if(active(p)){s.dialogue.getCompound("Reconcile").putString("Stage","SUSPENDED");clearPresentation(s);s.search=SecretsState.Search.NORMAL;}
        else if(s.dialogue.hasUUID("Offer")&&!s.dialogue.getBoolean("Decided")){var state=OtherMotherWelcome.State.fromId(s.dialogue.getString("TransactionState"));clearPresentation(s);s.dialogue.remove("OfferSpoken");SecretDialogue.queue(p,SecretDialogue.Event.BLACKMAIL_OFFER,state);}
        SecretTransactions.dirty(p);
    }
    public static void preview(ServerPlayer p,int category,int state){if(!p.hasPermissions(2)||!SecretsRuntime.active(p)||SecretDialogue.busy(p)||category<0||category>1||state<0||state>=5)return;SecretDialogue.queueScolding(p,Category.values()[category],OtherMotherWelcome.State.values()[state]);}
    public static void debugEvent(ServerPlayer p,int event){
        if(!p.hasPermissions(2)||!permitted(p)||SecretDialogue.busy(p)||event<0||event>6)return;
        if(event>=4&&event<=5){for(int slot=0;slot<p.getInventory().getContainerSize();slot++)if(SecretTransactions.offer(p,p.getInventory().getItem(slot),event==4?1:0))return;p.displayClientMessage(net.minecraft.network.chat.Component.literal("Carry a genuine Blackmail Item to test this flow."),true);return;}
        if(event==6){if(returnHome(p))OtherMotherLoop.afterReconciliation(p);return;}
        var s=SecretTransactions.state(p);
        boolean carrying=carryingEvidence(p);
        if(event==0&&carrying){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Store your restricted evidence to test an ordinary-area catch without an item confrontation."),true);return;}
        if(event==2&&!carrying){
            var available=SecretRegistry.ALL.keySet().stream().filter(id->!s.journal.contains(id)&&!s.issued.containsValue(id)).findFirst();
            if(available.isPresent())SecretTransactions.issue(p,available.get());
            if(!carryingEvidence(p)){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Carry a genuine restricted Secret Item to test this flow."),true);return;}
        }
        if(event==0||event==3){if(!returnHome(p))return;}
        if(event==1){var room=SecretWorld.rooms(s).stream().filter(n->n.getInt("Tier")>0).findFirst().orElse(null);if(room==null){p.displayClientMessage(net.minecraft.network.chat.Component.literal("No generated secret room is available yet."),true);return;}var at=safe(p,BlockPos.of(room.getLong("Pos")).north(7));if(at==null)return;p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),p.getXRot());}
        if(event==3){s.search=SecretsState.Search.SEARCHING;MotherSearch.bell(p);return;}
        var m=MotherSearch.mother(p);var at=safe(p,p.blockPosition().east(2));if(m==null||at==null)return;m.teleportTo(at.getX()+.5,at.getY(),at.getZ()+.5);s.world.remove("CaughtEncounter");s.world.remove("CatchUntil");MotherSearch.catchPlayer(p);
    }
    private static boolean carryingEvidence(ServerPlayer p){for(int slot=0;slot<p.getInventory().getContainerSize();slot++)if(!SecretTransactions.evidence(p,p.getInventory().getItem(slot)).isEmpty())return true;return false;}
    private Reconciliation(){}
}
