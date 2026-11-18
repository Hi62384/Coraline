package com.coraline.feast;

import com.coraline.palace.*;
import com.coraline.palace.PalaceSitePlan.Kind;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Ordered, read-back-verified custody of the actual original item. There is no
 * key factory/refund path. This is NOT an atomic cross-file filesystem commit:
 * PREPARED survives interruption and is reconciled before owner input on login.
 * Ambiguous/corrupt receipts stop here without deleting or recreating anything.
 */
@Mod.EventBusSubscriber(modid="coraline")
public final class PalaceKeyCustody {
    private static final String JOURNAL="KeyCustody",IDENTITY="PalaceKeyIdentity";
    private static PalaceWellProgress well(VisitData.Scene s,CompoundTag root){return PalaceWellProgress.load(s.owner,s.door,root.getCompound("Well"));}
    private static PalaceKeyTransaction journal(VisitData.Scene s,CompoundTag root){return PalaceKeyTransaction.load(s.owner,s.door,root.getCompound(JOURNAL));}
    private static boolean identifies(ItemStack stack,PalaceKeyTransaction tx){
        return !stack.isEmpty()&&stack.hasTag()&&stack.getTag().hasUUID(IDENTITY)&&Objects.equals(tx.key(),stack.getTag().getUUID(IDENTITY));
    }
    private static boolean boundBundle(ServerPlayer p,VisitData.Scene s,CompoundTag root,CinematicPropEntity bundle){
        return bundle!=null&&bundle.isAlive()&&bundle.owned(s)&&bundle.kind()==RescueProps.KEY_BUNDLE
                &&bundle.level()==p.server.overworld()&&root.hasUUID("BundleEntity")&&root.getUUID("BundleEntity").equals(bundle.getUUID());
    }
    static CompoundTag read(Path file,long limit)throws java.io.IOException{
        if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||Files.size(file)>limit)throw new java.io.IOException("Missing/oversized save checkpoint");
        try(var input=new java.io.DataInputStream(new java.io.BufferedInputStream(new java.util.zip.GZIPInputStream(Files.newInputStream(file))))){
            return NbtIo.read(input,new NbtAccounter(64L*1024*1024));
        }
    }
    /** Verify the specific original transaction on disk before inventory removal. */
    private static boolean saveStory(ServerPlayer p,VisitData.Scene s){
        try{
            OtherMotherLoop.changed(p);p.server.overworld().getDataStorage().save();
            var saved=read(p.server.getWorldPath(LevelResource.ROOT).resolve("data/coraline_first_visits.dat"),32L*1024*1024);
            return PalaceKeyCheckpoint.story(saved,s.owner,s.door,PalaceJourney.existing(s));
        }catch(java.io.IOException|RuntimeException unavailable){return false;}
    }
    private static boolean saveInventory(ServerPlayer p){
        try{
            p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();((com.coraline.mixin.MirrorPlayerSaveAccess)p.server.getPlayerList()).coraline$saveMirrorPlayer(p);
            return PalaceKeyCheckpoint.inventory(read(p.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat"),16L*1024*1024),p.getInventory().save(new ListTag()));
        }
        catch(java.io.IOException|RuntimeException unavailable){return false;}
    }
    private static void blocked(ServerPlayer p,CompoundTag root,String reason){
        if(reason.equals(root.getString("KeyCustodyBlocker")))return;
        root.putString("KeyCustodyBlocker",reason);OtherMotherLoop.changed(p);
    }
    /** Called only by a real owner interaction with the already wrapped bundle.
     * The boss driver must supply that entity after actual smash/wrap animation.
     */
    private static final String INVENTORY_RETRY="Could not verify original-key inventory checkpoint; no custody committed.";

    private static final String PREPARED_RETRY="Could not verify prepared original-key journal; original retained.";
    private static final String RETRY="KeyInventoryCheckpointPending";

    private static boolean originalKeyConflict(ServerPlayer p,VisitData.Scene s,ItemStack expected){
        int amount=0;
        for(int i=0;i<p.getInventory().getContainerSize();i++){
            var stack=p.getInventory().getItem(i);
            if(ParentRescueStory.key(stack,s)){
                if(stack!=expected)return true;
                amount+=stack.getCount();
            }
        }
        for(int i=0;i<p.getEnderChestInventory().getContainerSize();i++)
            if(ParentRescueStory.key(p.getEnderChestInventory().getItem(i),s))return true;
        return ParentRescueStory.key(p.containerMenu.getCarried(),s)||amount!=(expected==null?0:1);
    }

    public static boolean place(ServerPlayer p,VisitData.Scene s,CinematicPropEntity bundle){
        if(!PalaceJourney.owns(p,s)||!p.isAlive()||p.isSpectator()||p.serverLevel()!=p.server.overworld()
                ||p.containerMenu!=p.inventoryMenu||!p.containerMenu.getCarried().isEmpty())return false;
        var root=PalaceJourney.existing(s);if(root==null||root.contains(JOURNAL)
                ||root.contains("KeyCustodyBlocker")&&!root.getString("KeyCustodyBlocker").equals(INVENTORY_RETRY)
                ||!boundBundle(p,s,root,bundle)||p.distanceToSqr(bundle)>9||!p.hasLineOfSight(bundle))return false;
        var state=well(s,root);if(!com.coraline.postgame.GlobalHandEncounter.holds(p.server,s,state)
                ||state.phase()!=PalaceWellProgress.Phase.WRAPPED)return false;
        var original=p.getMainHandItem();if(!ParentRescueStory.key(original,s)||original.getCount()!=1)return false;
        if(originalKeyConflict(p,s,original)){
            blocked(p,root,"Multiple or contradictory original-key stacks; review required.");
            return false;
        }
        if(original.getOrCreateTag().contains(IDENTITY)&&!original.getOrCreateTag().hasUUID(IDENTITY))return false;
        if(!original.getOrCreateTag().hasUUID(IDENTITY))original.getOrCreateTag().putUUID(IDENTITY,UUID.randomUUID());
        if(!saveInventory(p)){
            root.putBoolean(RETRY,true);
            OtherMotherLoop.changed(p);
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("Could not save the original key. Try placing it again."),true);
            return false;
        }
        root.remove(RETRY);
        if(INVENTORY_RETRY.equals(root.getString("KeyCustodyBlocker"))){
            root.remove("KeyCustodyBlocker");
            OtherMotherLoop.changed(p);
        }
        var prepared=PalaceKeyTransaction.prepare(s.owner,s.door,bundle.getUUID(),original.save(new CompoundTag()));
        if(prepared.isEmpty())return false;
        root.put(JOURNAL,prepared.get().save());
        if(!saveStory(p,s)){blocked(p,root,PREPARED_RETRY);return false;}
        return reconcile(p,s);
    }
    /** No catch-up rewards, key synthesis or refunds. Runs before input on login
     * and on the normal owner tick, not while a foreign player's scene is active.
     */
    public static boolean reconcile(ServerPlayer p,VisitData.Scene s){
        if(!PalaceJourney.owns(p,s))return false;
        var root=PalaceJourney.existing(s);if(root==null||!root.contains(JOURNAL)
                ||!com.coraline.postgame.GlobalHandEncounter.holds(p.server,s,well(s,root)))return false;
        // Older builds persisted transient checkpoint failures as permanent
        // blockers. Re-run the journal reconciliation now that storage may be
        // available; identity conflicts remain quarantined.
        String oldBlocker=root.getString("KeyCustodyBlocker");
        if(PREPARED_RETRY.equals(oldBlocker)){
            root.remove("KeyCustodyBlocker");OtherMotherLoop.changed(p);
        }
        if(root.contains("KeyCustodyBlocker"))return false;
        if(!root.contains(JOURNAL,Tag.TAG_COMPOUND)){blocked(p,root,"Malformed original-key journal; review required.");return false;}
        var tx=journal(s,root);var state=well(s,root);
        if(!tx.blocker().isEmpty()||!state.blocker().isEmpty()||!root.hasUUID("BundleEntity")||!tx.bundle().equals(root.getUUID("BundleEntity"))){blocked(p,root,"Original-key/bundle/well receipt mismatch; no item changed.");return false;}
        int found=-1,count=0;
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(identifies(p.getInventory().getItem(i),tx)){found=i;count++;}
        // A prepared journal was saved synchronously while the original was in
        // this inventory. An external/container copy is ambiguous, never a refund.
        boolean external=identifies(p.containerMenu.getCarried(),tx);
        for(int i=0;i<p.getEnderChestInventory().getContainerSize();i++)external|=identifies(p.getEnderChestInventory().getItem(i),tx);
        if(external||count>1||originalKeyConflict(p,s,found<0?null:p.getInventory().getItem(found))){blocked(p,root,"Original-key identity appears outside its prepared slot or more than once; review required.");return false;}
        if(tx.stage()!=PalaceKeyTransaction.Stage.PREPARED){
            if(count!=0){blocked(p,root,"Held/disposed original key unexpectedly present; no duplicate consumed or returned.");return false;}
            boolean valid=state.save().hasUUID("BundleReceipt")&&tx.receipt().equals(state.save().getUUID("BundleReceipt"))
                    &&(tx.stage()==PalaceKeyTransaction.Stage.HELD?state.phase()==PalaceWellProgress.Phase.KEY_BUNDLED:state.phase().ordinal()>=PalaceWellProgress.Phase.DISPOSED.ordinal());
            if(!valid)blocked(p,root,"Custody stage and encounter checkpoint disagree; review required.");
            return valid;
        }
        if(state.phase()!=PalaceWellProgress.Phase.WRAPPED||p.containerMenu!=p.inventoryMenu||!p.containerMenu.getCarried().isEmpty())return false;
        // Verify PREPARED even on recovery before touching the original.
        if(!saveStory(p,s))return false;
        if(found>=0){
            var actual=p.getInventory().getItem(found);
            if(!actual.save(new CompoundTag()).equals(tx.originalSnapshot())){blocked(p,root,"Original serialized key changed after preparation; item retained.");return false;}
            p.getInventory().removeItemNoUpdate(found);
        }
        if(!saveInventory(p))return false; // PREPARED remains recoverable; never spendable.
        if(!tx.removedAndSaved(s.owner,tx.receipt())||!state.keyBundled(s.owner,state.session(),s.owner,s.door,tx.receipt()))return false;
        root.put(JOURNAL,tx.save());root.put("Well",state.save());
        if(!saveStory(p,s)){
            // Keep both state records non-spendable until a later verified save.
            tx=PalaceKeyTransaction.load(s.owner,s.door,root.getCompound(JOURNAL));
            var reset=tx.save();reset.putString("Stage","PREPARED");root.put(JOURNAL,reset);
            var prior=state.save();prior.putString("Phase","WRAPPED");prior.remove("BundleReceipt");root.put("Well",prior);OtherMotherLoop.changed(p);return false;
        }
        var entity=p.server.overworld().getEntity(tx.bundle());
        if(entity instanceof CinematicPropEntity bundle&&boundBundle(p,s,root,bundle))bundle.frame(1,0,1);
        p.swing(InteractionHand.MAIN_HAND,true);return true;
    }
    /** The real throw driver may commit only once the SAME bundle is physically
     * down the registered shaft and surviving E has actually finished playing.
     */
    public static boolean dispose(ServerPlayer p,VisitData.Scene s,CinematicPropEntity bundle){
        if(!PalaceJourney.owns(p,s)||!p.isAlive()||p.isSpectator()||p.serverLevel()!=p.server.overworld())return false;var root=PalaceJourney.existing(s);
        if(root==null||!boundBundle(p,s,root,bundle)||!reconcile(p,s))return false;
        var tx=journal(s,root);var state=well(s,root);var sites=PalaceSiteData.get(p.server);var at=sites.position(Kind.WELL);
        if(tx.stage()!=PalaceKeyTransaction.Stage.HELD||state.phase()!=PalaceWellProgress.Phase.KEY_BUNDLED
                ||!sites.blocker().isEmpty()||!sites.generated(Kind.WELL)||at.isEmpty())return false;
        var rim=at.get();double dx=bundle.getX()-rim.getX()-.5,dz=bundle.getZ()-rim.getZ()-.5;
        if(dx*dx+dz*dz>2.25||bundle.getY()>rim.getY()-12||p.distanceToSqr(rim.getCenter())>48*48)return false;
        boolean heard=state.wybieLife()!=PalaceWellProgress.WybieLife.ALIVE
                ||ShowPlayback.ownsSession(p,s)&&!ShowPlayback.debugSeeking(s)&&ShowPlayback.finished(s)
                &&ShowPlayback.data(s).getString("Sound").equals("crawling_hand_e")&&!ShowPlayback.data(s).getBoolean("SafetySkipped");
        var beforeTx=tx.save();var beforeWell=state.save();
        if(!state.disposed(s.owner,state.session(),tx.receipt(),true,heard)||!tx.dispose(s.owner,tx.receipt(),bundle.getUUID()))return false;
        root.put(JOURNAL,tx.save());root.put("Well",state.save());
        if(!saveStory(p,s)){root.put(JOURNAL,beforeTx);root.put("Well",beforeWell);OtherMotherLoop.changed(p);return false;}
        return true;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s!=null)reconcile(p,s);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interact(PlayerInteractEvent.EntityInteractSpecific event){
        if(!(event.getEntity() instanceof ServerPlayer p)||!(event.getTarget() instanceof CinematicPropEntity bundle)||bundle.kind()!=RescueProps.KEY_BUNDLE)return;
        var s=OtherMotherLoop.scene(p);if(s==null||!bundle.owned(s))return;
        var r=PalaceJourney.existing(s);if(r==null||!r.contains("Well")||well(s,r).phase()!=PalaceWellProgress.Phase.WRAPPED)return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.CONSUME);
        if(event.getHand()==InteractionHand.MAIN_HAND)place(p,s,bundle);
    }
    private PalaceKeyCustody(){}
}
