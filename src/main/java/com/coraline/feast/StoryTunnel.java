package com.coraline.feast;

import com.coraline.block.TinyDoorBlock;
import com.coraline.otherworld.DoorBlockEntity;
import com.coraline.otherworld.*;
import com.coraline.parents.FamilyData;
import com.coraline.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The permanent decayed entrance reuses the saved First Escape corridor, never the friendly loop. */
public final class StoryTunnel {
    public static boolean sealed(MinecraftServer server,java.util.UUID owner){var s=VisitData.get(server).scenes.get(owner);return ParentRescueStory.data(s).getCompound("Continuation").getBoolean("PermanentlySealed");}
    public static boolean permanent(MinecraftServer server,java.util.UUID owner){var s=VisitData.get(server).scenes.get(owner);return MissingParentsCat.tunnelRestored(s)&&(!ParentRescueStory.started(s)||resumable(s))&&!sealed(server,owner);}
    public static boolean restore(ServerPlayer p,VisitData.Scene s){
        if(s==null||!s.owner.equals(p.getUUID()))return false;
        var d=DoorData.get(p.server).doors.get(s.door);if(d==null||!s.owner.equals(d.owner))return false;
        var source=FamilyData.level(p.server,d.dimension);var other=p.server.getLevel(OtherWorld.KEY);
        if(source==null||other==null)return false;
        source.getChunkAt(d.pos);var state=source.getBlockState(d.pos);
        if(!state.is(ModBlocks.TINY_DOOR.get())||!(source.getBlockEntity(d.pos) instanceof DoorBlockEntity block)
            ||block.doorId!=null&&!s.door.equals(block.doorId))return false;
        var r=RealmData.get(p.server).realms.get(s.door);
        if(r==null||!r.ready||r.house==null||r.doorPos==null){
            OtherWorld.prepareStoryRealm(p,d);
            MissingParentsCat.data(s).putString("Blocker","Preparing your saved Other World passage automatically.");
            return false;
        }
        var n=FirstEscapeStory.data(s);n.putLong("House",r.house.asLong());n.putLong("LittleDoor",r.doorPos.asLong());
        if(!n.contains("Prison"))n.putLong("Prison",r.house.above(80).asLong());
        if(!MissingParentsCat.tunnelRestored(s)||!n.getBoolean("EscapeTunnelBuilt")){n.putBoolean("EscapeTunnelBuilt",false);FirstEscapeStory.ensureDecayedTunnel(p,s);}
        if(!n.getBoolean("EscapeTunnelBuilt"))return false;
        var behind=d.pos.relative(state.getValue(TinyDoorBlock.FACING).getOpposite());
        if(source.getBlockState(behind).is(Blocks.BRICKS)&&n.contains("BrickOriginal"))source.setBlock(behind,NbtUtils.readBlockState(source.holderLookup(net.minecraft.core.registries.Registries.BLOCK),n.getCompound("BrickOriginal")),2);
        n.putBoolean("DecayedTunnel",true);
        block.doorId=s.door;block.storyBricked=false;block.storyDecayed=true;
        MissingParentsCat.data(s).putBoolean("TunnelRestored",true);
        source.setBlock(d.pos,state.setValue(TinyDoorBlock.PORTAL,true).setValue(TinyDoorBlock.OPEN,true),3);
        block.refreshStoryPassageBlock();block.setChanged();source.sendBlockUpdated(d.pos,source.getBlockState(d.pos),source.getBlockState(d.pos),3);
        OtherMotherLoop.changed(p);return true;
    }
    public static void retireCrawl(ServerPlayer p,VisitData.Scene s){
        var original=FirstEscapeStory.data(s);if(!original.contains("EscapeTunnel"))return;
        var level=p.server.getLevel(OtherWorld.KEY);if(level==null)return;
        var lookup=level.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        for(var scope:java.util.List.of(original,ParentRescueStory.scenery(s))){
            var cells=scope.getCompound("Environment");
            for(int z=0;z<=22;z++)for(int x=-2;x<=2;x++)for(int y=-1;y<=3;y++){
                var at=FirstEscapeStory.tunnelCell(s,x,y,z);String key=Long.toString(at.asLong());if(!cells.contains(key))continue;
                level.getChunkAt(at);var saved=cells.getCompound(key);
                if(level.getBlockEntity(at)==null&&level.getBlockState(at).equals(NbtUtils.readBlockState(lookup,saved.getCompound("Placed"))))level.setBlock(at,NbtUtils.readBlockState(lookup,saved.getCompound("Original")),2);
                cells.remove(key);
            }
        }
        original.putBoolean("EscapeTunnelBuilt",false);OtherMotherLoop.changed(p);
    }
    public static boolean enter(ServerPlayer p,DoorBlockEntity door){
        var scene=OtherMotherLoop.scene(p);
        if(scene==null||!MissingParentsCat.tunnelRestored(scene)||!scene.door.equals(door.doorId))return false;
        if(!scene.owner.equals(p.getUUID())||sealed(p.server,scene.owner))return true;
        var source=DoorData.get(p.server).doors.get(scene.door);
        if(source==null||!scene.owner.equals(source.owner)||FamilyData.level(p.server,source.dimension)!=p.level()
                ||!source.pos.equals(door.getBlockPos())||p.distanceToSqr(source.pos.getCenter())>64)return true;
        if(!MissingParentsCat.data(scene).getBoolean("Complete")&&!MissingParentsCat.finishForReturn(p,scene)){
            FirstEscapeStory.hint(p,"The Cat is still revealing the passage.");
            return true;
        }
        if(ParentRescueStory.started(scene)){
            if(entryPhase(scene))crawl(p,scene,2);
            else resumeChapter(p,scene);
        }else ParentRescueStory.begin(p,scene);
        return true;
    }

    static boolean entryPhase(VisitData.Scene scene){
        if(!ParentRescueStory.started(scene)||RescueChapter.started(scene))return false;
        return StoryReturnContext.entryPhase(ParentRescueStory.phase(scene).name());
    }

    static boolean resumable(VisitData.Scene scene){
        return ParentRescueStory.started(scene)&&!RescueChapter.finished(scene)
                &&(!RescueChapter.started(scene)||RescueChapter.phase(scene).ordinal()<RescueChapter.Phase.LOCK_DOOR.ordinal());
    }

    /** Only the physical web challenges use the original authored web for
     * return-door recovery. The final crawl has its own separate journal. */
    static boolean webReturnPhase(RescueChapter.Phase phase){
        // FINAL_P/Q already take place in the web dimension. Returning
        // these to the Other World room can corrupt their saved camera/actor
        // stage even when the next tick tries to enter the web again.
        return phase==RescueChapter.Phase.FINAL_P||phase==RescueChapter.Phase.FINAL_Q
                ||phase.ordinal()>=RescueChapter.Phase.WEB_STEALTH.ordinal()
                &&phase.ordinal()<=RescueChapter.Phase.HAND_FALL.ordinal();
    }
    private static void resumeChapter(ServerPlayer p,VisitData.Scene scene){
        if(!resumable(scene)||!scene.owner.equals(p.getUUID())||!p.isAlive()||p.isPassenger()||p.isSpectator())return;
        var realm=RealmData.get(p.server).realms.get(scene.door);
        if(realm==null||!realm.ready||realm.house==null||realm.doorPos==null
                ||ParentRescueStory.scenery(scene).getLong("House")!=realm.house.asLong()){
            FirstEscapeStory.hint(p,"The original rescue realm is unavailable. Your checkpoint is preserved.");
            return;
        }
        var level=p.server.getLevel(OtherWorld.KEY);
        BlockPos landing=realm.doorPos;
        var parent=ParentRescueStory.data(scene);
        if(!RescueChapter.started(scene)&&parent.contains("SafeReturnPosition",Tag.TAG_LONG)
                &&ParentRescueStory.phase(scene).name().equals(parent.getString("SafeReturnPhase"))){
            var saved=BlockPos.of(parent.getLong("SafeReturnPosition"));
            if(FirstEscapeScene.inHouseOrPorch(scene,saved))landing=saved;
        }
        if(RescueChapter.started(scene)){
            var phase=RescueChapter.phase(scene);
            if(java.util.EnumSet.of(RescueChapter.Phase.THEATRE_SEARCH,RescueChapter.Phase.WRAPPER_REACH,
                    RescueChapter.Phase.THEATRE_GRAB,RescueChapter.Phase.THEATRE_FIGHT,RescueChapter.Phase.THEATRE_EYE).contains(phase)){
                if(!TheatreSite.prepare(p,scene))return;
                var entrance=TheatreSite.houseDoor(p,scene);
                if(entrance==null||!ShowTravel.recoverRescueReturn(p,scene,"TheatreReturn",entrance)){
                    FirstEscapeStory.hint(p,"The original theatre return checkpoint is unavailable. No transfer was made.");
                    return;
                }
                level=p.server.getLevel(ActivityWorld.KEY);landing=TheatreSite.entrance(p,scene);
            }else if(java.util.EnumSet.of(RescueChapter.Phase.BOB_E,RescueChapter.Phase.SWARM_TRANSFORM,
                    RescueChapter.Phase.SWARM_FIGHT,RescueChapter.Phase.RAT_CHASE).contains(phase)){
                var chapter=RescueChapter.data(scene);
                // The rat chase has two dimensions: legs 0/1 are the circus,
                // while leg 2 resumes with the original rat in the house.
                if(phase==RescueChapter.Phase.RAT_CHASE&&chapter.getInt("RatLeg")>=2){
                    if(!MouseCircusSite.prepareAttic(p,scene))return;
                    var tent=MouseCircusSite.tent(p,scene);
                    if(tent==null)return;
                    level=p.server.getLevel(OtherWorld.KEY);
                    landing=tent.south(1);
                }else{
                    if(!MouseCircusSite.prepare(p,scene))return;
                    var tent=MouseCircusSite.tent(p,scene);
                    if(tent==null||!ShowTravel.recoverRescueReturn(p,scene,"CircusReturn",tent.south(3))){
                        FirstEscapeStory.hint(p,"The original circus return checkpoint is unavailable. No transfer was made.");
                        return;
                    }
                    level=p.server.getLevel(ActivityWorld.KEY);
                    landing=MouseCircusSite.audience(p,scene);
                }
            }else if(phase.ordinal()>=RescueChapter.Phase.ATTIC_SEARCH.ordinal()&&phase.ordinal()<=RescueChapter.Phase.CIRCUS_TRAVEL.ordinal()){
                if(!MouseCircusSite.prepareAttic(p,scene))return;
                var tent=MouseCircusSite.tent(p,scene);
                if(tent==null)return;
                landing=tent.south(4);
            }else if(phase==RescueChapter.Phase.COLLAPSE&&RescueChapter.data(scene).contains("CollapseStart",Tag.TAG_LONG)){
                landing=BlockPos.of(RescueChapter.data(scene).getLong("CollapseStart"));
            }else if(phase.ordinal()<=RescueChapter.Phase.MANTIS_GONE.ordinal()){
                landing=RescueEncounters.garden(p,scene);
            }else if(phase==RescueChapter.Phase.TUNNEL_CHASE){
                var n=RescueChapter.data(scene);
                // A saved PassageEntered bit belongs to the previous run,
                // not this real-world door activation. Recheck the authored
                // passage exactly once per new attempt, in bounded batches.
                if(!n.getBoolean("StoryReturnCrawlPending")){
                    n.putBoolean("StoryReturnCrawlPending",true);
                    n.remove("PassageEntered");n.remove("PassageEntryRecheck");
                    OtherMotherLoop.changed(p);
                }
                if(!RescuePassage.enter(p,scene)){
                    FirstEscapeStory.hint(p,"Checking the original 52-block crawl before returning.");
                    return;
                }
                n.remove("StoryReturnCrawlPending");
                // The original player is now physically back at metre zero.
                // Reset the saved voice/chase clock as one authenticated retry;
                // otherwise a late prior pursuit timestamp can catch them
                // immediately despite a successful 52-block entry check.
                n.remove("Recover");
                RescueChapter.go(p,scene,RescueChapter.Phase.TUNNEL_CHASE);
                OtherMotherLoop.changed(p);
                return;
            }else if(webReturnPhase(phase)){
                var n=RescueChapter.data(scene);
                if(!n.getBoolean("StoryReturnWebPending")){
                    n.putBoolean("StoryReturnWebPending",true);
                    n.remove("WebVerified");n.putInt("WebVerify",0);
                    OtherMotherLoop.changed(p);
                }
                if(!RescueWebSite.prepare(p,scene)){
                    FirstEscapeStory.hint(p,"Checking the original web route before returning.");
                    return;
                }
                level=p.server.getLevel(ActivityWorld.KEY);
                boolean atWebEntrance=phase==RescueChapter.Phase.FINAL_P||phase==RescueChapter.Phase.FINAL_Q
                        ||phase==RescueChapter.Phase.WEB_STEALTH;
                landing=BlockPos.containing(RescueWebSite.at(p,scene,1,atWebEntrance?0:1)).above();
            }else if(phase.ordinal()>=RescueChapter.Phase.HOUSE_SAFE.ordinal()){
                landing=FirstEscapeScene.room(scene);
            }
        }
        if(!RescueChapter.transfer(p,level,landing)){
            FirstEscapeStory.hint(p,"The saved rescue landing is blocked. Your phase and items are preserved.");
            return;
        }
        ParentRescueStory.data(scene).putBoolean("Recover",true);
        if(RescueChapter.started(scene)){
            var n=RescueChapter.data(scene);
            n.remove("StoryReturnWebPending");n.remove("StoryReturnCrawlPending");
            // A successful return to an interrupted web checkpoint must
            // re-stage its original actors and safe challenge positions.
            if(webReturnPhase(RescueChapter.phase(scene))||RescueBosses.fighting(RescueChapter.phase(scene)))
                n.putBoolean("Recover",true);
        }
        OtherMotherLoop.changed(p);
    }

    public static boolean controls(ServerPlayer player){
        var scene=VisitData.get(player.server).scenes.get(player.getUUID());
        if(scene==null||!ParentRescueStory.started(scene))return false;
        return resumable(scene)||OtherWorld.isOther(player.level())&&!sealed(player.server,scene.owner);
    }

    public static void tickReturnSource(ServerPlayer player){
        var scene=VisitData.get(player.server).scenes.get(player.getUUID());
        if(!resumable(scene)||OtherWorld.isOther(player.level())||ActivityWorld.isActivity(player.level()))return;
        var flags=player.getPersistentData();
        for(String flag:java.util.List.of("CoralineWaitingDoor","CoralineInTunnel","CoralineReturning","CoralineRecoveryTunnel",
                "CoralineTunnelStartedAt","CoralineWakeAt","CoralineExitLead","CoralineExitClose"))flags.remove(flag);
        var source=DoorData.get(player.server).doors.get(scene.door);
        if(source==null||!scene.owner.equals(source.owner)||FamilyData.level(player.server,source.dimension)!=player.level()
                ||player.distanceToSqr(source.pos.getCenter())>36){
            if(RescueChapter.started(scene)){
                var n=RescueChapter.data(scene);
                if(n.getBoolean("StoryReturnWebPending")||n.getBoolean("StoryReturnCrawlPending")){
                    n.remove("StoryReturnWebPending");n.remove("StoryReturnCrawlPending");
                    OtherMotherLoop.changed(player);
                }
            }
            if(flags.getBoolean("CoralineAutoCrawl")&&player.getForcedPose()==Pose.SWIMMING)player.setForcedPose(null);
            flags.remove("CoralineAutoCrawl");
            return;
        }
        refreshPortal(player,scene,source);
        if(RescueChapter.started(scene)){
            var n=RescueChapter.data(scene);
            if(n.getBoolean("StoryReturnWebPending")||n.getBoolean("StoryReturnCrawlPending")){
                if(player.tickCount%5==0)resumeChapter(player,scene);
                return;
            }
        }
        TunnelPassage.tick(player);
    }

    public static void tickSource(ServerPlayer p,VisitData.Scene s){
        if(!MissingParentsCat.tunnelRestored(s)||ParentRescueStory.started(s)||p.serverLevel()!=p.server.overworld())return;
        var source=DoorData.get(p.server).doors.get(s.door);
        if(source!=null&&s.owner.equals(source.owner)&&FamilyData.level(p.server,source.dimension)==p.level()
                &&p.distanceToSqr(source.pos.getCenter())<=36)refreshPortal(p,s,source);
        TunnelPassage.tick(p);
    }

    private static void refreshPortal(ServerPlayer player,VisitData.Scene scene,DoorData.Door source){
        if(sealed(player.server,scene.owner))return;
        var level=player.serverLevel();
        var state=level.getBlockState(source.pos);
        if(!state.is(ModBlocks.TINY_DOOR.get())||!(level.getBlockEntity(source.pos) instanceof DoorBlockEntity door)
                ||!scene.door.equals(door.doorId)||door.storyBricked)return;
        boolean changed=!door.storyDecayed;
        if(changed){door.storyDecayed=true;door.setChanged();}
        if(!state.getValue(TinyDoorBlock.PORTAL))level.setBlock(source.pos,state.setValue(TinyDoorBlock.PORTAL,true),3);
        else if(changed)level.sendBlockUpdated(source.pos,state,state,3);
    }

    static boolean insideCrawl(ServerPlayer p,VisitData.Scene scene){
        var escape=FirstEscapeStory.data(scene);
        if(!escape.contains("EscapeTunnel",Tag.TAG_LONG)||p.level()!=p.server.getLevel(OtherWorld.KEY))return false;
        var offset=p.position().subtract(Vec3.atBottomCenterOf(BlockPos.of(escape.getLong("EscapeTunnel"))));
        return StoryReturnContext.inCorridor(offset,FirstEscapeStory.escapeDirection(scene));
    }

    static boolean crawl(ServerPlayer p,VisitData.Scene scene,double progress){
        if(!scene.owner.equals(p.getUUID())||!entryPhase(scene))return false;
        var realm=RealmData.get(p.server).realms.get(scene.door);
        var level=p.server.getLevel(OtherWorld.KEY);
        var source=DoorData.get(p.server).doors.get(scene.door);
        if(level==null||realm==null||!realm.ready||realm.house==null||realm.doorPos==null||source==null||!scene.owner.equals(source.owner)){
            if(source!=null&&scene.owner.equals(source.owner)&&level!=null&&FamilyData.level(p.server,source.dimension)==p.level())
                OtherWorld.prepareStoryRealm(p,source);
            FirstEscapeStory.hint(p,"The original Other World passage is unavailable. Your return progress is saved.");
            return false;
        }
        var escape=FirstEscapeStory.data(scene);
        if(escape.getLong("House")!=realm.house.asLong()||escape.getLong("LittleDoor")!=realm.doorPos.asLong()){
            FirstEscapeStory.hint(p,"The saved corridor does not match your Other World.");
            return false;
        }
        FirstEscapeStory.ensureDecayedTunnel(p,scene);
        if(!escape.getBoolean("EscapeTunnelBuilt")||!escape.contains("EscapeTunnel",Tag.TAG_LONG))return false;
        var origin=BlockPos.of(escape.getLong("EscapeTunnel"));
        var direction=FirstEscapeStory.escapeDirection(scene);
        var at=Vec3.atBottomCenterOf(origin.relative(direction,(int)progress));
        var feet=BlockPos.containing(at);
        level.getChunkAt(feet);
        var space=new net.minecraft.world.phys.AABB(at.x-.29,at.y+.01,at.z-.29,at.x+.29,at.y+.59,at.z+.29);
        if(!level.getWorldBorder().isWithinBounds(feet)||!level.noCollision(p,space)||!level.getFluidState(feet).isEmpty()
                ||!level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),Direction.UP)){
            FirstEscapeStory.hint(p,"The saved corridor entrance is obstructed. Clear the passage before retrying.");
            return false;
        }
        var story=ParentRescueStory.data(scene);
        var ticket=StoryReturnContext.ticket(story.getCompound("ReturnTravel"),scene.owner,scene.door,
                ParentRescueStory.phase(scene).name(),origin.asLong(),direction);
        if(ticket==null){
            FirstEscapeStory.hint(p,"The saved return checkpoint belongs to a different passage.");
            return false;
        }
        story.put("ReturnTravel",ticket);
        var visit=VisitData.get(p.server);
        try{
            visit.setDirty();
            p.server.overworld().getDataStorage().save();
            var path=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/coraline_first_visits.dat");
            if(!com.coraline.rebirth.StoryRunStore.read(path).getCompound("data").equals(visit.save(new CompoundTag())))return false;
        }catch(java.io.IOException|RuntimeException unavailable){
            FirstEscapeStory.hint(p,"The return checkpoint could not be saved. Retry at the Little Door.");
            return false;
        }
        p.closeContainer();
        p.stopRiding();
        p.setForcedPose(Pose.SWIMMING);
        p.setPose(Pose.SWIMMING);
        p.teleportTo(level,at.x,at.y,at.z,direction.toYRot(),0);
        if(p.level()!=level||p.position().distanceToSqr(at)>.25){
            p.setForcedPose(null);
            return false;
        }
        var player=p.getPersistentData();
        for(String flag:java.util.List.of("CoralineWaitingDoor","CoralineInTunnel","CoralineReturning","CoralineSeamlessTunnel",
                "CoralineRecoveryTunnel","CoralineTunnelStartedAt","CoralineWakeAt","CoralineExitLead","CoralineExitClose"))player.remove(flag);
        p.fallDistance=0;
        return true;
    }
    private StoryTunnel(){}
}
