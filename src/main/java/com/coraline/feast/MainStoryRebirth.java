package com.coraline.feast;

import com.coraline.rebirth.*;
import com.coraline.parents.*;
import com.coraline.otherworld.*;
import com.coraline.secrets.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Explicit main-story reset boundary. Never deletes realms, chunks, site data, inventories or worlds. */
public final class MainStoryRebirth {
    public static final Set<String> STORY_KEYS=Set.of(
        "FirstEscape","FirstEscapeRewardPaid","ParentRescue","MissingParentsCat","MissingParentsTea",
        "PinkPalaceJourney","BlackIsTraditional","BlackIsTraditionalQueued","ParentDollsDiscovered",
        "SeeingStonePending","SeeingStoneReceipt","LittleDoorKeyIssued","LittleDoorKeyCustodyVersion",
        "LittleDoorKeyHolder","GhostChildrenFreed","MajorStoryPhase","WybieIntroductionComplete",
        "WybieVisitsForbidden","WybieBanSession","WybieDepartingActor","WybieDepartingPosition",
        "WybieDepartingDimension","WybieDepartingMissing","WybieLeaseToken","WybieCourseCleanup",
        "BobinskyIntroductionComplete","TeaIntroductionComplete","MouseCircusCompleted","TheatreCompleted",
        "CanonicalRealWybie","CanonicalRealWybieSnapshot","CanonicalStoryCat","PreparedDeliveryRoute",
        "MotherAwayDoorCleanup","NeighborVisitsDebugComplete");
    public static final Set<String> PLAYER_KEYS=Set.of(
        "CoralineVisitDoor","CoralineWaitingDoor","CoralineInTunnel","CoralineReturning","CoralineWakeAt",
        "CoralineEntryTrustDoor","CoralineEntryTrustPoints","CoralineDebugVisitCompleted","HouseUnlock",
        "CoralineSeamlessTunnel","CoralineTunnelStartedAt","CoralineRecoveryTunnel","CoralineRecovering",
        "CoralineWhiteoutAnchor","CoralineWhiteoutMoved","CoralineWhiteoutStart","CoralineExitClose","CoralineExitLead");
    public static List<String> report(){return List.of(
        "CLEAR: owner's original intro/doll generation/active Little Doors; visits, trust and suspicion use constructors' original zero defaults.",
        "CLEAR: proposal, all numbered one-time events, first escape, missing parents, eyes, rescue/finale/epilogue, transport/well/key encounter receipts.",
        "CLEAR: secret journal, issued capabilities, abilities, synergies, counters, search/chase, inspections and blackmail; terminate borrowed camera first.",
        "CLEAR: rescued-parent bonuses, tasks/delivery timers, neglect/busy cycle; restore earlier parent instances of the same family.",
        "REWIND: real Wybie's stored original identity including DEAD fate, only through this committed timeline transaction.",
        "PRESERVE: Void Hearts, reward receipts/run counts, all unrelated items/equipment, XP, advancements, inventories/storage and builds.",
        "PRESERVE: PalaceSiteData, ShowSiteData, existing landmark coordinates, biome/chunks, realm records and physical player worlds.",
        "PRESERVE: unknown story extension keys and Ocean/Festival/Temple/EndCity/Expedition ledgers; no prefix-based deletion.");}
    public static CompoundTag capture(ServerPlayer p,VisitData.Scene s){
        var n=new CompoundTag();
        var saved=VisitData.get(p.server).save(new CompoundTag());
        for(var entry:saved.getList("Scenes",10)){var own=(CompoundTag)entry;if(own.hasUUID("Owner")&&p.getUUID().equals(own.getUUID("Owner")))n.put("OldScene",own.copy());}
        var retained=s.loop.story.copy();STORY_KEYS.forEach(retained::remove);n.put("RetainedStory",retained);
        n.put("Ocean",s.loop.ocean.copy());n.put("Festival",s.loop.festival.copy());n.put("Temple",s.loop.temple.copy());
        n.put("EndCity",s.loop.endCity.copy());n.put("Expedition",s.loop.expedition.copy());
        var wybie=CanonicalRealWybie.identity(s);if(wybie!=null)n.put("Wybie",wybie.save());
        var cat=s.loop.story.getCompound("CanonicalStoryCat").copy();cat.remove("FarewellDeparted");n.put("Cat",cat);
        return n;
    }
    /** Roll-forward, repeatable while the owner's input remains locked. New opening is not started here. */
    public static void reset(ServerPlayer p,StoryRun run){
        if(run.phase()!=StoryRun.Phase.RESETTING&&run.phase()!=StoryRun.Phase.RESTORING||!p.getUUID().equals(run.owner()))throw new IllegalStateException("Uncommitted story reset");
        SecretAbilities.end(p);JournalPlacement.cancel(p);SecretsNetwork.clear(p.getUUID());SecretLocks.clear(p.getUUID());
        var scene=VisitData.get(p.server).scenes.get(p.getUUID());
        if(scene!=null){
            if(!scene.door.equals(run.id("Door")))throw new IllegalStateException("A foreign/new scene appeared during reset");
            ShowPlayback.stop(p,scene);VisitAudio.stop(p);
            // Only the old two Other Parents are retired, never the owner's real family or another family's actors.
            for(var level:p.server.getAllLevels())for(var id:new UUID[]{scene.mom,scene.dad})
                if(id!=null&&level.getEntity(id) instanceof ParentEntity parent&&parent.isOtherParent())parent.discard();
            FamilyVisitReset.forget(p.server,p.getUUID());
        }
        DoorData.get(p.server).rewindStory(p.getUUID());
        com.coraline.doll.DollBondData.get(p.server).rewindStory(p.getUUID());
        var neighbor=com.coraline.bobinsky.BobinskyVisitData.get(p.server);
        neighbor.visits.remove(p.getUUID());neighbor.teaVisits.remove(p.getUUID());neighbor.setDirty();
        PLAYER_KEYS.forEach(p.getPersistentData()::remove);
        var persisted=p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        persisted.remove("CoralineDollFamily");persisted.remove("CoralineDollGeneration");
        p.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG,persisted);
        p.getCooldowns().removeCooldown(com.coraline.registry.ModItems.DOWSING_ROD.get());
        retireInventory(p,run.id("Door"));
        p.stopRiding();p.stopUsingItem();p.setCamera(p);
        p.server.overworld().getDataStorage().save();p.server.getPlayerList().saveAll();
    }
    /** DataStorage logs IO failures internally; independent disk read-back is required before next-run advancement. */
    public static boolean verified(ServerPlayer p,StoryRun run)throws java.io.IOException{
        var root=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data");
        var scenes=savedList(root.resolve("coraline_first_visits.dat"),"Scenes");
        for(var entry:scenes)if(((CompoundTag)entry).hasUUID("Owner")&&p.getUUID().equals(((CompoundTag)entry).getUUID("Owner")))return false;
        for(var entry:savedList(root.resolve("coraline_doors.dat"),"Doors")){
            var door=(CompoundTag)entry;if(door.hasUUID("Owner")&&p.getUUID().equals(door.getUUID("Owner"))&&!door.getBoolean("Revoked"))return false;
        }
        for(var entry:savedList(root.resolve("coraline_doll_bonds.dat"),"Bonds")){
            var bond=(CompoundTag)entry;if(bond.hasUUID("Owner")&&p.getUUID().equals(bond.getUUID("Owner")))return false;
        }
        var file=StoryRunStore.read(p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat"));
        if(!file.contains("ForgeData",Tag.TAG_COMPOUND))return false;
        var persistent=file.getCompound("ForgeData");for(var key:PLAYER_KEYS)if(persistent.contains(key))return false;
        var doll=persistent.getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        return !doll.hasUUID("CoralineDollFamily")&&!doll.contains("CoralineDollGeneration")
            &&doll.hasUUID("LastVoidHeartReceipt")&&Objects.equals(run.receipt(),doll.getUUID("LastVoidHeartReceipt"))
            &&com.coraline.palace.PalaceKeyCheckpoint.inventory(file,p.getInventory().save(new ListTag()));
    }
    private static ListTag savedList(java.nio.file.Path file,String key)throws java.io.IOException{
        var root=StoryRunStore.read(file);
        if(!root.contains("data",Tag.TAG_COMPOUND)||!root.getCompound("data").contains(key,Tag.TAG_LIST))throw new java.io.IOException("Malformed reset checkpoint: "+key);
        var list=(ListTag)root.getCompound("data").get(key);if(!list.isEmpty()&&list.getElementType()!=Tag.TAG_COMPOUND)throw new java.io.IOException("Malformed reset list: "+key);return list;
    }
    private static void retireInventory(ServerPlayer p,UUID oldDoor){retireInventory(p,Set.of(oldDoor));}
    private static void retireInventory(ServerPlayer p,Set<UUID> oldDoors){
        for(var inventory:List.of(p.getInventory(),p.getEnderChestInventory()))for(int i=0;i<inventory.getContainerSize();i++)
            if(retireStack(inventory.getItem(i),p.getUUID(),oldDoors,0))inventory.setChanged();
        var cursor=p.containerMenu.getCarried();if(retireStack(cursor,p.getUUID(),oldDoors,0))p.containerMenu.setCarried(cursor.isEmpty()?net.minecraft.world.item.ItemStack.EMPTY:cursor);
        p.inventoryMenu.broadcastChanges();
    }
    /** Exact quest types AND exact owner binding; no broad inventory/namespace wipe. Nested ordinary storage is retained. */
    public static boolean retireStack(net.minecraft.world.item.ItemStack item,UUID owner,UUID oldDoor,int depth){return retireStack(item,owner,Set.of(oldDoor),depth);}
    private static boolean retireStack(net.minecraft.world.item.ItemStack item,UUID owner,Set<UUID> oldDoors,int depth){
        if(item.isEmpty()||depth>8||!item.hasTag())return false;var n=item.getTag();boolean retire=false;
        if(item.is(com.coraline.registry.ModItems.DOLL.get()))retire=owner.equals(com.coraline.doll.DollBondData.owner(item));
        if(item.is(com.coraline.registry.ModItems.TINY_DOOR.get()))retire=n.hasUUID("DoorOwner")&&owner.equals(n.getUUID("DoorOwner"));
        if(n.hasUUID("SecretOwner")&&owner.equals(n.getUUID("SecretOwner"))&&com.coraline.secrets.SecretsItems.is(item))retire=true;
        if(item.is(com.coraline.registry.ModItems.LITTLE_DOOR_STORY_KEY.get()))retire=n.hasUUID("StoryKeyOwner")&&owner.equals(n.getUUID("StoryKeyOwner"))&&n.hasUUID("StoryKeyDoor")&&oldDoors.contains(n.getUUID("StoryKeyDoor"));
        if(item.is(com.coraline.registry.ModItems.SEEING_STONE.get()))retire=n.hasUUID("StoryOwner")&&owner.equals(n.getUUID("StoryOwner"))&&n.hasUUID("StoryDoor")&&oldDoors.contains(n.getUUID("StoryDoor"));
        if(item.is(com.coraline.registry.ModItems.GHOST_EYE.get()))retire=n.hasUUID("EyeOwner")&&owner.equals(n.getUUID("EyeOwner"))&&n.hasUUID("EyeRealm")&&oldDoors.contains(n.getUUID("EyeRealm"));
        if(item.is(FirstEscapeItems.CANE.get())||item.is(FirstEscapeItems.SHOE.get()))retire=n.hasUUID("StoryOwner")&&owner.equals(n.getUUID("StoryOwner"));
        if(item.is(net.minecraft.world.item.Items.FILLED_MAP))retire=n.hasUUID("PalaceMapOwner")&&owner.equals(n.getUUID("PalaceMapOwner"))&&n.hasUUID("PalaceMapDoor")&&oldDoors.contains(n.getUUID("PalaceMapDoor"));
        if(retire){item.setCount(0);return true;}boolean changed=false;
        for(var container:new CompoundTag[]{n,item.getTagElement("BlockEntityTag")})if(container!=null){var list=container.getList("Items",10);
            for(int i=list.size()-1;i>=0;i--){var serialized=list.getCompound(i);var nested=net.minecraft.world.item.ItemStack.of(serialized);
                if(retireStack(nested,owner,oldDoors,depth+1)){changed=true;if(nested.isEmpty())list.remove(i);else nested.save(serialized);}}
        }return changed;
    }
    /** Owner records can be retired while the player is offline. The final
     * coordinator checkpoints these native SavedData stores before advancing;
     * player-only input/camera cleanup is repeated at the next login. */
    public static boolean completeEndingOwner(net.minecraft.server.MinecraftServer server,UUID owner,UUID reset)throws java.io.IOException{
        var run=StoryRunStore.get(server,owner);var original=CanonicalRealWybie.archiveForCompleteEnding(server,owner,reset);if(original.isEmpty())return false;var cat=CompleteEndingCatArchive.archive(server,owner,reset);if(cat.isEmpty()||!run.completeEnding(reset,original.orElseThrow(),cat.orElseThrow()))return false;
        var visits=VisitData.get(server);var old=visits.scenes.get(owner);
        if(old!=null){for(var world:server.getAllLevels())for(var id:new UUID[]{old.mom,old.dad})if(id!=null&&world.getEntity(id) instanceof ParentEntity parent&&parent.isOtherParent())parent.discard();}
        var doors=DoorData.get(server);var realms=RealmData.get(server);for(var door:doors.doors.values())if(owner.equals(door.owner))realms.realms.remove(door.id);realms.setDirty();
        doors.rewindStory(owner);com.coraline.doll.DollBondData.get(server).rewindStory(owner);FamilyVisitReset.forget(server,owner);
        var neighbors=com.coraline.bobinsky.BobinskyVisitData.get(server);neighbors.visits.remove(owner);neighbors.teaVisits.remove(owner);neighbors.setDirty();
        var families=FamilyData.get(server);for(var family:families.families.values())if(owner.equals(family.owner)){family.rescued=false;family.storyMissing=false;family.busy=true;family.availabilityChangesAt=0;family.momTask=family.dadTask=family.gardenPlots=0;family.momDelivery=family.dadDelivery=family.dialogueUntil=0;}families.setDirty();
        if(!returnLivingParents(server,owner,reset))return false;
        var player=server.getPlayerList().getPlayer(owner);if(player!=null)completeEndingPlayer(player);
        StoryRunStore.save(server,run);server.overworld().getDataStorage().save();return true;
    }
    private static ParentEntity endingParent(net.minecraft.server.MinecraftServer server,FamilyData.Family family,boolean mother){
        UUID id=mother?family.mom:family.dad;if(id==null)return null;var source=FamilyData.level(server,mother?family.momDim:family.dadDim);if(source!=null)LoopProps.hold(source,mother?family.momPos:family.dadPos);ParentEntity found=null;for(var level:server.getAllLevels())if(level.getEntity(id) instanceof ParentEntity parent){if(found!=null)return null;found=parent;}return found!=null&&family.id.equals(found.familyId)&&!found.isOtherParent()&&found.isAlive()&&!found.deathFrozen()?found:null;
    }
    private static boolean returnLivingParents(net.minecraft.server.MinecraftServer server,UUID owner,UUID reset){
        boolean changed=false;for(var family:FamilyData.get(server).families.values())if(owner.equals(family.owner)&&!family.dead){
            var target=FamilyData.level(server,family.homeDim);var home=family.bed;boolean retiringHome=target==null||target.dimension().location().getNamespace().equals("coraline");
            if(home==null||retiringHome){var player=server.getPlayerList().getPlayer(owner);target=server.overworld();home=player!=null&&player.level()==target?player.blockPosition():com.coraline.palace.PalaceSiteData.get(server).position(com.coraline.palace.PalaceSitePlan.Kind.PALACE).orElse(null);}if(home==null||target==null)return false;LoopProps.hold(target,home);
            for(boolean mother:new boolean[]{true,false}){var parent=endingParent(server,family,mother);if(parent==null)return false;var destination=home.offset(mother?-2:2,1,0);if(parent.level()!=target||parent.distanceToSqr(destination.getCenter())>64){com.coraline.parents.ParentEvents.transferNear(parent,target,destination);parent=endingParent(server,family,mother);if(parent==null||parent.level()!=target||parent.distanceToSqr(destination.getCenter())>100)return false;changed=true;}
                if(mother){family.momDim=parent.level().dimension().location().toString();family.momPos=parent.blockPosition();}else{family.dadDim=parent.level().dimension().location().toString();family.dadPos=parent.blockPosition();}
                changed=true;boolean dormant=server.getPlayerList().getPlayer(owner)==null;parent.stopRiding();parent.familyGesture(0,0);parent.reunionFrost(0);parent.relationship(0,0);parent.setInvisible(dormant);parent.setSilent(dormant);parent.setNoAi(dormant);parent.setNoGravity(dormant);parent.noPhysics=dormant;parent.setInvulnerable(dormant);if(dormant)parent.getPersistentData().putUUID("CoralineCompleteEndingDormant",reset);else parent.getPersistentData().remove("CoralineCompleteEndingDormant");
            }if(retiringHome){family.bed=null;family.chest=null;family.homeDim="minecraft:overworld";}FamilyData.get(server).setDirty();
        }if(changed&&!server.saveEverything(true,true,true))return false;return true;
    }
    /** Starting setup is applied only for this player's durable ending receipt.
     * Living original parents wake as the same bodies. A genuinely dead family
     * stays a tombstone; an intact owned bed may receive a fresh starting pair. */
    public static boolean prepareStartingParents(ServerPlayer player,UUID reset)throws java.io.IOException{
        if(com.coraline.postgame.TrueHeartBattle.pendingReset(player.server,player.getUUID()).filter(reset::equals).isEmpty()||!StoryRunStore.get(player.server,player.getUUID()).save().hasUUID("CompleteReset")||!reset.equals(StoryRunStore.get(player.server,player.getUUID()).save().getUUID("CompleteReset")))return false;
        var data=FamilyData.get(player.server);var active=ParentAccess.family(player.server,player.getUUID());
        if(active==null){for(var old:data.families.values().stream().filter(f->f.dead&&player.getUUID().equals(f.owner)&&f.bed!=null).toList()){
                var home=FamilyData.level(player.server,old.homeDim);if(home==null||home.dimension().location().getNamespace().equals("coraline"))continue;LoopProps.hold(home,old.bed);if(!home.hasChunkAt(old.bed))return false;if(!(home.getBlockEntity(old.bed) instanceof com.coraline.parents.ParentsBedEntity bed)||!old.id.equals(bed.familyId))continue;
                player.getPersistentData().putUUID("CoralineEndingParentFlush",reset);if(!com.coraline.parents.ParentEvents.spawnPairForCompleteEnding(player,home,old.bed.above(),reset)){long now=player.server.overworld().getGameTime();if(now-player.getPersistentData().getLong("CoralineEndingParentsHint")>=200){player.getPersistentData().putLong("CoralineEndingParentsHint",now);player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.coraline.heart_parents_space",old.bed.getX(),old.bed.getY(),old.bed.getZ()));}return false;}
                active=ParentAccess.family(player.server,player.getUUID());if(active==null)return false;active.bed=old.bed;active.chest=old.bed;active.homeDim=old.homeDim;active.momDelivery=active.dadDelivery=home.getGameTime()+com.coraline.config.CoralineConfig.TASK_INTERVAL.get();bed.familyId=active.id;bed.setChanged();data.setDirty();if(!player.server.saveEverything(true,true,true))return false;break;
            }}
        if(active!=null)for(boolean mother:new boolean[]{true,false}){var parent=endingParent(player.server,active,mother);if(parent==null)return false;if(parent.getPersistentData().hasUUID("CoralineCompleteEndingDormant")){if(!reset.equals(parent.getPersistentData().getUUID("CoralineCompleteEndingDormant")))return false;player.getPersistentData().putUUID("CoralineEndingParentFlush",reset);parent.setInvisible(false);parent.setSilent(false);parent.setNoAi(false);parent.setNoGravity(false);parent.noPhysics=false;parent.setInvulnerable(false);parent.getPersistentData().remove("CoralineCompleteEndingDormant");}}
        if(player.getPersistentData().hasUUID("CoralineEndingParentFlush")){if(!reset.equals(player.getPersistentData().getUUID("CoralineEndingParentFlush"))||!player.server.saveEverything(true,true,true))return false;player.getPersistentData().remove("CoralineEndingParentFlush");}
        player.getPersistentData().remove("CoralineEndingParentsHint");return true;
    }
    public static void completeEndingPlayer(ServerPlayer player){
        var retiredDoors=new HashSet<UUID>();for(var door:DoorData.get(player.server).doors.values())if(door.revoked&&player.getUUID().equals(door.owner))retiredDoors.add(door.id);retireInventory(player,retiredDoors);
        SecretAbilities.end(player);JournalPlacement.cancel(player);SecretsNetwork.clear(player.getUUID());SecretLocks.clear(player.getUUID());
        PLAYER_KEYS.forEach(player.getPersistentData()::remove);var persisted=player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);persisted.remove("CoralineDollFamily");persisted.remove("CoralineDollGeneration");player.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG,persisted);
        player.stopRiding();player.stopUsingItem();player.setCamera(player);player.closeContainer();
    }
    public static void newScene(ServerPlayer p,VisitData.Scene scene){
        try{
            var run=StoryRunStore.get(p.server,p.getUUID());if(run.run()==1&&!run.completeEndingTimeline())return;
            var retained=run.save().getCompound("PreviousRitual").getCompound("Retained");
            scene.loop.story.merge(retained.getCompound("RetainedStory"));
            scene.loop.ocean=retained.getCompound("Ocean").copy();scene.loop.festival=retained.getCompound("Festival").copy();
            scene.loop.temple=retained.getCompound("Temple").copy();scene.loop.endCity=retained.getCompound("EndCity").copy();scene.loop.expedition=retained.getCompound("Expedition").copy();
            if(retained.contains("Wybie",Tag.TAG_COMPOUND)){
                var old=retained.getCompound("Wybie");var identity=com.coraline.palace.PalaceWybieIdentity.create(scene.owner,scene.door,old.getUUID("Entity"),old.getString("Dimension"),net.minecraft.core.BlockPos.of(old.getLong("Position")));
                // A witnessed living fall remains a loss across ordinary
                // replay. The ritual does not retrieve a captive from the
                // postgame world or turn a missing actor into a new original.
                if(!run.completeEndingTimeline()&&"LOST".equals(old.getString("Fate"))&&old.hasUUID("LostWellSession")&&!com.coraline.postgame.WybieRescue.rescued(p.server,scene.owner,identity.entity()))identity.recordWellFall(identity.entity(),old.getUUID("LostWellSession"));
                identity.store(identity.entity());scene.loop.story.put("CanonicalRealWybie",identity.save());
                CanonicalRealWybie.rebindTimeline(p,scene,run);
            }
            if(!retained.getCompound("Cat").isEmpty())scene.loop.story.put("CanonicalStoryCat",retained.getCompound("Cat").copy());
        }catch(java.io.IOException e){throw new IllegalStateException("Cannot restore prior story-run continuity",e);}
    }
    private MainStoryRebirth(){}
}
