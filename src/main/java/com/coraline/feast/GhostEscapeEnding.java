package com.coraline.feast;

import com.coraline.block.TinyDoorBlock;
import com.coraline.otherworld.DoorData;
import com.coraline.parents.FamilyData;
import com.coraline.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Physical first step of the alternate ending. Uses the normal original-key
 * issuer/custody; does not manufacture replacement keys or mark other quests done. */
@Mod.EventBusSubscriber(modid="coraline")
public final class GhostEscapeEnding {
    static boolean epilogueActive(VisitData.Scene s){return s!=null&&GhostEscapeEndingProgress.epilogueOwns(GhostVictimEntry.data(s),s.owner,s.door,RescueChapter.data(s));}
    /** A replay receipt for the complete accepted-proposal ending, including
     * the original child's escape, real-family epilogue, and detached original Hand. */
    public static boolean alternativeComplete(VisitData.Scene s){
        if(s==null||!AcceptedButtonEyes.data(s).hasUUID("Door")||!s.door.equals(AcceptedButtonEyes.data(s).getUUID("Door"))
                ||!AcceptedButtonEyes.data(s).getString("Route").equals(AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL.name()))return false;
        var ghost=GhostVictimEntry.data(s);var ending=GhostEscapeEndingProgress.existing(ghost,s.owner,s.door);
        if(ending==null||!ending.getBoolean("AlternativeBeldamVictory")||!ending.getBoolean("AlternativeEndingComplete"))return false;
        var hand=GhostEscapeHandProgress.existing(ghost,s.owner,s.door,s.mom);var scenery=ParentRescueStory.scenery(s);
        return hand!=null&&scenery.hasUUID("RescueSeveredHand")
                &&GhostEscapeHandProgress.handoffReady(ghost,s.owner,s.door,s.mom,scenery.getUUID("RescueSeveredHand"))
                &&RescueChapter.data(s).getBoolean("ParentsReturned")&&RescueChapter.data(s).getBoolean("FarewellComplete")
                &&RescueEyes.ledger(s).getBoolean("Released");
    }
    static boolean hasKey(ServerPlayer p,VisitData.Scene s){
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(ParentRescueStory.key(p.getInventory().getItem(i),s))return true;
        return false;
    }
    private static boolean obtainKey(ServerPlayer p,VisitData.Scene s){
        if(hasKey(p,s))return true;
        var held=StoryItemCustody.held(s,"Key");
        if(!held.isEmpty())return ParentRescueStory.key(held,s)&&StoryItemCustody.giveBack(p,s,"Key")&&hasKey(p,s);
        var rescue=ParentRescueStory.data(s);
        // The proposal route can precede the first normal key issuance. Use
        // that same once-only issuer; an issued/taken/destroyed key is never replaced.
        if(GhostEscapeEndingProgress.mayIssueFirstKey(s.loop.story,rescue))return ParentRescueStory.grantKey(p,s)&&hasKey(p,s);
        return false;
    }
    public static boolean tick(ServerPlayer p,VisitData.Scene s,CompoundTag ghost){
        if(!GhostVictimMirrors.owned(p,s,ghost))return false;
        boolean existed=ghost.getCompound(GhostEscapeProgress.KEY).contains(GhostEscapeEndingProgress.KEY);
        var ending=GhostEscapeEndingProgress.begin(ghost,s.owner,s.door);if(ending==null)return false;
        if(!existed)OtherMotherLoop.changed(p);
        if(ending.getString("Stage").equals(GhostEscapeEndingProgress.Stage.EPILOGUE_COMPLETE.name())){GhostEscapeSeveredHand.tick(p,s,ghost);return true;}
        if(epilogueActive(s)){
            var family=com.coraline.parents.ParentAccess.family(p.server,s.owner);
            if(family==null||!GhostEscapeEndingProgress.parentsMatch(ghost,s.owner,s.door,family.id,family.mom,family.dad)){ending.putString("Blocker","Waiting for the saved original family binding; no parents replaced.");return true;}
            RescueChapter.tick(p,s);
            if(GhostEscapeEndingProgress.finishEpilogue(ghost,s.owner,s.door,RescueChapter.data(s),RescueEyes.ledger(s).getBoolean("Released"))){
                s.loop.story.putBoolean("BeldamDefeatedByGhostEscape",true);s.loop.story.putString("MajorStoryPhase","GHOST_ESCAPE_CANONICAL_EPILOGUE_COMPLETE");OtherMotherLoop.changed(p);
            }
            return true;
        }
        var source=DoorData.get(p.server).doors.get(s.door);
        if(source==null||source.revoked||!s.owner.equals(source.owner)){ending.putString("Blocker","Your original Little Door is unavailable; no replacement created.");return true;}
        if(p.serverLevel()!=FamilyData.level(p.server,source.dimension))return true;
        if(p.tickCount%20!=0)return true;
        p.serverLevel().getChunkAt(source.pos);
        if(!p.serverLevel().getBlockState(source.pos).is(ModBlocks.TINY_DOOR.get())
            ||!(p.serverLevel().getBlockEntity(source.pos) instanceof com.coraline.otherworld.DoorBlockEntity door)||!s.door.equals(door.doorId)){
            ending.putString("Blocker","Restore your original Little Door before continuing.");FirstEscapeStory.hint(p,ending.getString("Blocker"));return true;
        }
        if(ending.getString("Stage").equals(GhostEscapeEndingProgress.Stage.DOOR_SEALED.name())){
            // Reconcile a crash between the chunk save and the story save.
            // The authenticated sealing receipt never authorizes another key.
            var level=p.serverLevel();var state=level.getBlockState(source.pos);
            if(door.passageBuilt&&(com.coraline.otherworld.TunnelPassage.contains(p,door)||com.coraline.otherworld.TunnelPassage.occupied(level,door)))return true;
            var closed=state.setValue(TinyDoorBlock.OPEN,false).setValue(TinyDoorBlock.PORTAL,false);
            if(!state.equals(closed)&&!level.setBlock(source.pos,closed,3))return true;
            if(door.passageBuilt)com.coraline.otherworld.TunnelPassage.restore(level,door);
            if(source.activeUntil!=0){source.activeUntil=0;DoorData.get(p.server).setDirty();}
            ending.remove("Blocker");RescueEpilogue.beginAlternate(p,s,ghost);return true;
        }
        if(!obtainKey(p,s)){ending.putString("Blocker","Retrieve your original Little Door key, or make an inventory space for the key held in story custody.");FirstEscapeStory.hint(p,ending.getString("Blocker"));return true;}
        if(GhostEscapeEndingProgress.keyPresent(ghost,s.owner,s.door,true))OtherMotherLoop.changed(p);
        ending.remove("Blocker");FirstEscapeStory.hint(p,"Use your Little Door key to seal the Little Door.");return true;
    }
    static boolean seal(ServerPlayer p,VisitData.Scene s,BlockPos at){
        if(s==null||!s.owner.equals(p.getUUID()))return false;var ghost=GhostVictimEntry.data(s);
        if(!GhostVictimMirrors.owned(p,s,ghost))return false;var ending=GhostEscapeEndingProgress.existing(ghost,s.owner,s.door);if(ending==null)return false;
        var source=DoorData.get(p.server).doors.get(s.door);
        if(source==null||source.revoked||!s.owner.equals(source.owner)||!source.pos.equals(at)||p.serverLevel()!=FamilyData.level(p.server,source.dimension)||p.distanceToSqr(at.getCenter())>16)return false;
        if(!(p.serverLevel().getBlockEntity(at) instanceof com.coraline.otherworld.DoorBlockEntity door)||!s.door.equals(door.doorId))return false;
        if(!java.util.Set.of(GhostEscapeEndingProgress.Stage.RETURN_KEY.name(),GhostEscapeEndingProgress.Stage.SEAL_DOOR.name()).contains(ending.getString("Stage"))){if(!GhostEscapeSeveredHand.closeOnHand(p,s,ghost))FirstEscapeStory.hint(p,"The Little Door is sealed.");return true;}
        var level=p.serverLevel();var state=level.getBlockState(at);if(!state.is(ModBlocks.TINY_DOOR.get()))return false;
        if(!hasKey(p,s)){FirstEscapeStory.hint(p,"Bring your Little Door key to seal the door.");return true;}
        if(door.passageBuilt&&(com.coraline.otherworld.TunnelPassage.contains(p,door)||com.coraline.otherworld.TunnelPassage.occupied(level,door))){FirstEscapeStory.hint(p,"Leave the tunnel clear before sealing the Little Door.");return true;}
        var closed=state.setValue(TinyDoorBlock.OPEN,false).setValue(TinyDoorBlock.PORTAL,false);
        if(!level.setBlock(at,closed,3)&&!level.getBlockState(at).equals(closed))return true;
        var actual=level.getBlockState(at);boolean sealed=actual.is(ModBlocks.TINY_DOOR.get())&&!actual.getValue(TinyDoorBlock.OPEN)&&!actual.getValue(TinyDoorBlock.PORTAL);
        if(!GhostEscapeEndingProgress.sealed(ghost,s.owner,s.door,true,hasKey(p,s),sealed))return true;
        if(door.passageBuilt)com.coraline.otherworld.TunnelPassage.restore(level,door);
        var continuation=RescueChapter.data(s);continuation.putBoolean("PermanentlySealed",true);continuation.putBoolean("KeyRecovered",true);continuation.putBoolean("CrawlReachedOverworld",true);
        ParentRescueStory.data(s).putBoolean("DoorLocked",true);s.loop.story.putString("LittleDoorKeyHolder","PLAYER");source.activeUntil=0;DoorData.get(p.server).setDirty();
        level.playSound(null,at,SoundEvents.WOODEN_DOOR_CLOSE,SoundSource.BLOCKS,1,.9F);OtherMotherLoop.changed(p);return true;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void block(PlayerInteractEvent.RightClickBlock event){
        if(event.getEntity() instanceof ServerPlayer p&&seal(p,VisitData.get(p.server).scenes.get(p.getUUID()),event.getPos())){
            event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
    private GhostEscapeEnding(){}
}
