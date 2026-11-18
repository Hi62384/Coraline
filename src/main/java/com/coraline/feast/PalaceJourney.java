package com.coraline.feast;

import com.coraline.palace.PalaceSiteData;
import com.coraline.palace.PalaceSitePlan.Kind;
import com.coraline.palace.PalaceWellProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Real vanilla explorer-map delivery/recovery after the completed epilogue.
 * The Moving Company must have its verified location before a map is issued;
 * the other landmarks may finish building while the player travels. No key is granted.
 */
@Mod.EventBusSubscriber(modid="coraline")
public final class PalaceJourney {
    public static final String OBJECTIVE="You must find the magic well to throw the key into and get rid of it...";
    private static final String ROOT="PinkPalaceJourney";

    public static PalaceWellProgress.Prerequisites prerequisites(VisitData.Scene scene){
        var rescue=scene==null?new CompoundTag():scene.loop.story.getCompound("ParentRescue");
        var end=rescue.getCompound("Continuation");
        return new PalaceWellProgress.Prerequisites(end.getBoolean("Started")&&end.getString("Phase").equals("COMPLETE"),
                end.getBoolean("PermanentlySealed")&&rescue.getBoolean("DoorLocked"),end.getBoolean("ParentsReturned"),
                end.getBoolean("FarewellComplete"),rescue.getCompound("GhostEyes").getBoolean("Released"),
                end.getBoolean("FarewellCatDeparted"),end.getBoolean("HandSevered"));
    }
    /** Null means absent/corrupt/foreign, never permission to overwrite it. */
    static CompoundTag existing(VisitData.Scene scene){
        if(scene==null||!scene.loop.story.contains(ROOT,Tag.TAG_COMPOUND))return null;
        var root=scene.loop.story.getCompound(ROOT);
        if(!root.contains("Version",Tag.TAG_INT)||root.getInt("Version")!=1||!root.hasUUID("Owner")||!scene.owner.equals(root.getUUID("Owner"))
                ||!root.hasUUID("Door")||!scene.door.equals(root.getUUID("Door")))return null;
        if(root.contains("MapIssued")&&(!root.contains("MapIssued",Tag.TAG_BYTE)||root.getByte("MapIssued")<0||root.getByte("MapIssued")>1)
                ||root.contains("MapId")&&(!root.contains("MapId",Tag.TAG_INT)||root.getInt("MapId")<0)
                ||root.contains("Company")&&!root.contains("Company",Tag.TAG_LONG)
                ||root.contains("MapRecoveryAfter")&&(!root.contains("MapRecoveryAfter",Tag.TAG_LONG)||root.getLong("MapRecoveryAfter")<0)
                ||root.getBoolean("MapIssued")&&(!root.contains("MapId",Tag.TAG_INT)||!root.contains("Company",Tag.TAG_LONG)))return null;
        for(String key:new String[]{"Well","WellStage","KeyCustody","Transport"})if(root.contains(key)&&!root.contains(key,Tag.TAG_COMPOUND))return null;
        if(root.contains("BundleEntity")&&!root.hasUUID("BundleEntity"))return null;
        return root;
    }
    private static CompoundTag initialize(VisitData.Scene scene){
        if(scene.loop.story.contains(ROOT))return existing(scene);
        var root=new CompoundTag();root.putInt("Version",1);root.putUUID("Owner",scene.owner);root.putUUID("Door",scene.door);
        scene.loop.story.put(ROOT,root);return root;
    }
    static boolean owns(ServerPlayer player,VisitData.Scene scene){return scene!=null&&player.getUUID().equals(scene.owner)&&prerequisites(scene).ready();}
    private static boolean mapEligible(ServerPlayer player,VisitData.Scene scene){
        if(scene==null||!player.getUUID().equals(scene.owner))return false;
        var proof=prerequisites(scene);
        // A slow or stale Cat cleanup must not withhold the wakeup reward.
        // The later well encounter still uses the full prerequisites.
        return proof.chapterComplete()&&proof.doorPermanentlySealed()&&proof.parentsReturned()
            &&proof.farewellComplete()&&proof.eyesReleased()&&proof.handSevered();
    }
    private static boolean sitesReady(PalaceSiteData sites){
        // The map needs its marked destination, not the distant Palace and
        // well that are still being built by the background world job.
        return sites.blocker().isEmpty()&&sites.generated(Kind.COMPANY)&&sites.position(Kind.COMPANY).isPresent();
    }
    private static void issueNotice(ServerPlayer p,VisitData.Scene scene,CompoundTag root){
        if(root.getBoolean("NoticeIssued"))return;
        if(p.getInventory().getFreeSlot()<0){
            if(!root.getBoolean("NoticeInventoryWarned")){
                root.putBoolean("NoticeInventoryWarned",true);OtherMotherLoop.changed(p);
                p.sendSystemMessage(Component.literal("Make one inventory space for your Moving Company map or notice."));
            }
            return;
        }
        var notice=new ItemStack(Items.PAPER);
        notice.setHoverName(Component.literal("Moving Company Notice"));
        notice.getOrCreateTag().putUUID("PalaceNoticeOwner",scene.owner);
        notice.getOrCreateTag().putUUID("PalaceNoticeDoor",scene.door);
        if(!p.getInventory().add(notice)||!notice.isEmpty())return;
        root.putBoolean("NoticeIssued",true);p.getInventory().setChanged();p.containerMenu.broadcastChanges();OtherMotherLoop.changed(p);
        p.sendSystemMessage(Component.literal("The Moving Company is preparing your map. Keep the Little Door key; your map will arrive when its destination is ready."));
    }
    private static boolean objectiveNeeded(VisitData.Scene s,CompoundTag root){
        if(!root.contains("Well"))return true;var state=PalaceWellProgress.load(s.owner,s.door,root.getCompound("Well"));
        return !state.blocker().isEmpty()||state.phase().ordinal()<PalaceWellProgress.Phase.DISPOSED.ordinal();
    }
    private static boolean keyPresent(ServerPlayer p,VisitData.Scene scene){
        for(int slot=0;slot<p.getInventory().getContainerSize();slot++)if(ParentRescueStory.key(p.getInventory().getItem(slot),scene))return true;
        return false;
    }
    private static int noticeSlot(ServerPlayer p,VisitData.Scene scene){
        for(int slot=0;slot<p.getInventory().getContainerSize();slot++){
            var stack=p.getInventory().getItem(slot);
            if(!stack.is(Items.PAPER)||!stack.hasTag())continue;
            var tag=stack.getTag();
            if(tag.hasUUID("PalaceNoticeOwner")&&scene.owner.equals(tag.getUUID("PalaceNoticeOwner"))
                &&tag.hasUUID("PalaceNoticeDoor")&&scene.door.equals(tag.getUUID("PalaceNoticeDoor")))return slot;
        }
        return -1;
    }
    public static boolean map(ItemStack stack,VisitData.Scene scene,BlockPos company){
        if(scene==null||company==null||!stack.is(Items.FILLED_MAP)||!stack.hasTag())return false;var tag=stack.getTag();
        if(!tag.contains("map",Tag.TAG_INT)||tag.getInt("map")<0)return false;
        return tag.hasUUID("PalaceMapOwner")&&scene.owner.equals(tag.getUUID("PalaceMapOwner"))
                &&tag.hasUUID("PalaceMapDoor")&&scene.door.equals(tag.getUUID("PalaceMapDoor"))
                &&tag.contains("PalaceMapCompany",Tag.TAG_LONG)&&tag.getLong("PalaceMapCompany")==company.asLong();
    }
    private static boolean mapPresent(ServerPlayer p,VisitData.Scene scene,BlockPos company){
        var root=existing(scene);if(root==null||!root.contains("MapId",Tag.TAG_INT))return false;
        java.util.function.Predicate<ItemStack> matches=stack->map(stack,scene,company)&&stack.getTag().getInt("map")==root.getInt("MapId");
        for(int slot=0;slot<p.getInventory().getContainerSize();slot++)if(matches.test(p.getInventory().getItem(slot)))return true;
        for(int slot=0;slot<p.getEnderChestInventory().getContainerSize();slot++)if(matches.test(p.getEnderChestInventory().getItem(slot)))return true;
        if(matches.test(p.containerMenu.getCarried()))return true;
        for(var slot:p.containerMenu.slots)if(matches.test(slot.getItem()))return true;
        return false;
    }
    private static boolean boundMap(ItemStack stack,VisitData.Scene scene,BlockPos company,int oldId,int newId){
        if(!map(stack,scene,company)||stack.getTag().getInt("map")!=oldId)return false;
        stack.getTag().putInt("map",newId);return true;
    }
    private static Integer rebuildMissingMap(ServerPlayer p,VisitData.Scene scene,BlockPos company,int oldId){
        // A missing vanilla map dataset cannot be restored under the same id.
        // Allocate a new dataset only for this already owner/door/company-bound
        // story record, then retarget matching copies in the owner's storage.
        var result=MapItem.create(p.server.overworld(),company.getX(),company.getZ(),(byte)2,true,true);
        Integer replacement=MapItem.getMapId(result);if(replacement==null||p.server.overworld().getMapData("map_"+replacement)==null)return null;
        net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(result,company,"moving_company",MapDecoration.Type.RED_X);
        for(int slot=0;slot<p.getInventory().getContainerSize();slot++)boundMap(p.getInventory().getItem(slot),scene,company,oldId,replacement);
        for(int slot=0;slot<p.getEnderChestInventory().getContainerSize();slot++)boundMap(p.getEnderChestInventory().getItem(slot),scene,company,oldId,replacement);
        boundMap(p.containerMenu.getCarried(),scene,company,oldId,replacement);p.getInventory().setChanged();p.getEnderChestInventory().setChanged();p.containerMenu.broadcastChanges();
        var root=existing(scene);if(root==null)return null;root.putInt("MapId",replacement);OtherMotherLoop.changed(p);return replacement;
    }
    /** Recovery recreates a MAP with the same saved vanilla map ID, not a key.
     * No chunk search/load, pre-render scan, world landmark or reward allocation.
     */
    public static boolean giveMap(ServerPlayer p,VisitData.Scene scene,boolean recovery){
        if(!mapEligible(p,scene)||p.serverLevel()!=p.server.overworld()||p.isSpectator())return false;
        try{if(com.coraline.rebirth.StoryRunStore.status(p.server,p.getUUID()).run()!=1)return false;}catch(java.io.IOException unavailable){return false;}
        var sites=PalaceSiteData.get(p.server);if(!sitesReady(sites))return false;
        var company=sites.position(Kind.COMPANY).orElseThrow();var root=initialize(scene);if(root==null)return false;
        if(root.contains("Company")&&(!root.contains("Company",Tag.TAG_LONG)||root.getLong("Company")!=company.asLong()))return false;
        if(root.contains("MapId")&&p.server.overworld().getMapData("map_"+root.getInt("MapId"))==null){
            if(root.getBoolean("MapIssued")&&!recovery||root.contains("Company")&&root.getLong("Company")!=company.asLong())return false;
            if(rebuildMissingMap(p,scene,company,root.getInt("MapId"))==null)return false;
        }
        if(mapPresent(p,scene,company)){if(!root.getBoolean("MapIssued")){root.putLong("Company",company.asLong());root.putBoolean("MapIssued",true);OtherMotherLoop.changed(p);}return true;}
        int noticeSlot=noticeSlot(p,scene);
        if(root.getBoolean("MapIssued")&&!recovery||(!keyPresent(p,scene)&&!com.coraline.postgame.InfiniteWorldData.get(p.server).active())
            ||p.getInventory().getFreeSlot()<0&&noticeSlot<0)return false;
        ItemStack result;
        if(root.contains("MapId")){
            if(!root.contains("MapId",Tag.TAG_INT)||root.getInt("MapId")<0||p.server.overworld().getMapData("map_"+root.getInt("MapId"))==null)return false;
            result=new ItemStack(Items.FILLED_MAP);result.getOrCreateTag().putInt("map",root.getInt("MapId"));
        }else{
            result=MapItem.create(p.server.overworld(),company.getX(),company.getZ(),(byte)2,true,true);
            Integer id=MapItem.getMapId(result);if(id==null)return false;root.putInt("MapId",id);
        }
        root.putLong("Company",company.asLong());
        var tag=result.getOrCreateTag();tag.putUUID("PalaceMapOwner",scene.owner);tag.putUUID("PalaceMapDoor",scene.door);tag.putLong("PalaceMapCompany",company.asLong());
        net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(result,company,"moving_company",MapDecoration.Type.RED_X);
        result.setHoverName(Component.literal("Moving Company Explorer Map"));
        if(noticeSlot>=0)p.getInventory().setItem(noticeSlot,result);
        else if(!p.getInventory().add(result)||!result.isEmpty()){OtherMotherLoop.changed(p);return false;}
        root.putBoolean("MapIssued",true);p.getInventory().setChanged();p.containerMenu.broadcastChanges();OtherMotherLoop.changed(p);
        p.displayClientMessage(Component.literal(OBJECTIVE),true);return true;
    }
    /** The farewell calls this on the same server tick as the player's return.
     * The periodic player tick remains the recovery path after a reload. */
    static void afterWake(ServerPlayer p,VisitData.Scene scene){
        if(!mapEligible(p,scene)||p.serverLevel()!=p.server.overworld()||p.isSpectator())return;
        try{if(com.coraline.rebirth.StoryRunStore.status(p.server,p.getUUID()).run()!=1)return;}
        catch(java.io.IOException unavailable){return;}
        if(giveMap(p,scene,false))return;
        var root=initialize(scene);
        if(root!=null&&!root.getBoolean("MapIssued"))issueNotice(p,scene,root);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){if(event.player instanceof net.minecraft.server.level.ServerPlayer coopPlayer&&com.coraline.multiplayer.StoryCoop.directorBlocked(coopPlayer))return;
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||p.tickCount%20!=0||!p.isAlive()||p.isSleeping())return;
        var scene=OtherMotherLoop.scene(p);if(!mapEligible(p,scene))return;
        var root=existing(scene);
        try{
            long run=com.coraline.rebirth.StoryRunStore.status(p.server,p.getUUID()).run();
            boolean farewellEnding=run>1||com.coraline.postgame.InfiniteWorldData.get(p.server).active();
            if(farewellEnding&&(root==null||!root.contains("Well"))){
                root=initialize(scene);if(root==null)return;
                if(run==1&&!root.getBoolean("MapIssued")){giveMap(p,scene,false);return;}
                if(!root.contains("PersonalFarewell",Tag.TAG_COMPOUND)){
                    var proof=new CompoundTag();proof.putUUID("Owner",scene.owner);proof.putUUID("Door",scene.door);proof.putLong("Run",run);
                    proof.putBoolean("WorldHandComplete",com.coraline.postgame.InfiniteWorldData.get(p.server).active());
                    root.put("PersonalFarewell",proof);OtherMotherLoop.changed(p);
                }
                com.coraline.rebirth.StoryRebirth.ending(p,scene);return;
            }
        }catch(java.io.IOException unavailable){return;}
        PalaceKeyCustody.reconcile(p,scene);
        if(root==null||!root.getBoolean("MapIssued")){
            if(!giveMap(p,scene,false)&&p.serverLevel()==p.server.overworld()){
                root=initialize(scene);
                if(root!=null&&!root.getBoolean("MapIssued"))issueNotice(p,scene,root);
                if(p.tickCount%80==0)p.displayClientMessage(Component.literal("Moving Company map pending: "
                    +(PalaceSiteData.get(p.server).generated(Kind.COMPANY)?"keep your key and a free inventory slot.":"its destination is still being prepared.")),true);
            }
            return;
        }
        if(p.tickCount%80==0&&p.serverLevel()==p.server.overworld()&&objectiveNeeded(scene,root))
            p.displayClientMessage(Component.literal(OBJECTIVE),true);
    }
    @SubscribeEvent public static void recoverMap(PlayerInteractEvent.RightClickItem event){
        if(!(event.getEntity() instanceof ServerPlayer p)||!p.isShiftKeyDown())return;
        var scene=OtherMotherLoop.scene(p);if(!owns(p,scene)||!ParentRescueStory.key(event.getItemStack(),scene))return;
        var root=existing(scene);if(root==null||!root.getBoolean("MapIssued"))return;
        long now=p.server.overworld().getGameTime(),next=root.getLong("MapRecoveryAfter");
        if(next>now&&next-now<=200)return;root.putLong("MapRecoveryAfter",now+200);OtherMotherLoop.changed(p);
        giveMap(p,scene,true);event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
    }
    private PalaceJourney(){}
}
