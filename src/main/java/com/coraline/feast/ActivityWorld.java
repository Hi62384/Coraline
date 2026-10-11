package com.coraline.feast;

import com.coraline.otherworld.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Shared transport and lifecycle for the playable activity arenas. */
public final class ActivityWorld {
    private static final String DEBUG_RETURN="CoralineVillageDebugReturn";
    public static final ResourceKey<Level> KEY=ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath("coraline","activities"));
    public static boolean isActivity(Level level){return level.dimension().equals(KEY);}
    public static boolean protectedWorld(Level level){return OtherWorld.isOther(level)||isActivity(level);}
    public static BlockState persistentLeaves(Block block){var state=block.defaultBlockState();return block instanceof LeavesBlock?state.setValue(LeavesBlock.PERSISTENT,true):state;}
    private static void stabilizeExistingLeaves(ServerLevel level,BlockPos c){
        var at=new BlockPos.MutableBlockPos();
        for(int x=-50;x<=50;x++)for(int z=-50;z<=50;z++)for(int y=0;y<=12;y++){
            at.set(c.getX()+x,c.getY()+y,c.getZ()+z);var state=level.getBlockState(at);
            if(state.getBlock() instanceof LeavesBlock&&!state.getValue(LeavesBlock.PERSISTENT))
                level.setBlock(at,state.setValue(LeavesBlock.PERSISTENT,true),2);
        }
    }
    public static String introSound(OtherMotherActivities.Activity activity){return "other_father_activity_"+activity.id()+"_intro";}
    private static BlockPos center(ServerPlayer p,VisitData.Scene s){return switch(s.loop.activity){
        case TREASURE_HUNT,SHOPPING_MALL,SNOWBALL_BATTLE,OTHER_MINESHAFT,OTHER_NETHER_FORTRESS,OTHER_WOODLAND_MANSION->ExpeditionActivities.center(p,s);
        case ARCHERY_CONTEST->ArcheryActivity.center(p,s);
        case CAKE_MAKING_CONTEST->CakeContestActivity.center(p,s);
        case CARNIVAL->CarnivalActivity.center(p,s);
        case DREAM_PARKOUR->DreamParkourActivity.center(p,s);
        case FISHING_CONTEST->FishingContestActivity.center(p,s);
        case GARDEN_MAZE->GardenMazeActivity.center(p,s);
        case GRAND_FEAST->GrandFeastActivity.center(p,s);
        case ICE_BOAT_RACE->IceBoatRaceActivity.center(p,s);
        case MAGIC_SHOW->MagicShowActivity.center(p,s);
        case MAGIC_GARDEN->MagicGardenActivity.center(p,s);
        case MICE_CIRCUS->MiceCircusActivity.center(p,s);
        case OTHER_OCEAN_MONUMENT->OceanMonumentActivity.center(p,s);
        case MUSIC_FESTIVAL->MusicFestivalActivity.center(p,s);
        case OTHER_DESERT_TEMPLE->DesertTempleActivity.center(p,s);
        case OTHER_END_CITY->EndCityActivity.center(p,s);
        default->VillageActivity.center(p,s);
    };}
    /** Leaving is always available; game completion is a separate reward condition. */
    public static boolean exitReady(VisitData.Scene s){
        return s!=null&&s.loop.phase==OtherMotherLoopState.Phase.ACTIVITY;
    }
    private static void start(ServerPlayer p,VisitData.Scene s,ServerLevel level){
        ExpeditionActivities.clearOtherArenas(p,s,level);
        if(s.loop.activity!=OtherMotherActivities.Activity.MUSIC_FESTIVAL){var old=MusicFestivalActivity.center(p,s);MusicFestivalActivity.clean(level,s,old);MusicFestivalActivity.removeBuild(level,old);}
        if(s.loop.activity!=OtherMotherActivities.Activity.OTHER_DESERT_TEMPLE){var old=DesertTempleActivity.center(p,s);DesertTempleActivity.clean(level,s,old);DesertTempleActivity.removeBuild(level,old);}
        if(s.loop.activity!=OtherMotherActivities.Activity.OTHER_END_CITY){var old=EndCityActivity.center(p,s);EndCityActivity.clean(level,s,old);EndCityActivity.removeBuild(level,old);}
        if(s.loop.activity!=OtherMotherActivities.Activity.OTHER_VILLAGE){var old=VillageActivity.center(p,s);VillageActivity.clean(level,s,old);VillageActivity.removeBuild(level,old);}
        if(s.loop.activity!=OtherMotherActivities.Activity.ARCHERY_CONTEST){var old=ArcheryActivity.center(p,s);ArcheryActivity.clean(level,s,old);ArcheryActivity.removeBuild(level,old);}
        if(s.loop.activity!=OtherMotherActivities.Activity.CAKE_MAKING_CONTEST){var old=CakeContestActivity.center(p,s);CakeContestActivity.clean(level,s,old);CakeContestActivity.removeBuild(level,old);}
        if(s.loop.activity!=OtherMotherActivities.Activity.CARNIVAL){var old=CarnivalActivity.center(p,s);CarnivalActivity.clean(level,s,old);CarnivalActivity.removeBuild(level,old);}
        if(s.loop.activity!=OtherMotherActivities.Activity.DREAM_PARKOUR){var old=DreamParkourActivity.center(p,s);DreamParkourActivity.clean(level,s,old);DreamParkourActivity.removeBuild(level,old);}
        if(s.loop.activity!=OtherMotherActivities.Activity.FISHING_CONTEST){var old=FishingContestActivity.center(p,s);FishingContestActivity.clean(level,s,old);FishingContestActivity.removeBuild(level,old);}
        if(s.loop.activity!=OtherMotherActivities.Activity.GARDEN_MAZE){var old=GardenMazeActivity.center(p,s);GardenMazeActivity.clean(level,s,old);GardenMazeActivity.removeBuild(level,old);}
        if(s.loop.activity!=OtherMotherActivities.Activity.GRAND_FEAST){var old=GrandFeastActivity.center(p,s);GrandFeastActivity.clean(level,s,old);GrandFeastActivity.removeBuild(level,old);}
        if(s.loop.activity!=OtherMotherActivities.Activity.ICE_BOAT_RACE){var old=IceBoatRaceActivity.center(p,s);IceBoatRaceActivity.clean(level,s,old);IceBoatRaceActivity.removeBuild(level,old);}
        if(s.loop.activity!=OtherMotherActivities.Activity.MAGIC_SHOW){var old=MagicShowActivity.center(p,s);MagicShowActivity.clean(level,s,old);MagicShowActivity.removeBuild(level,old);}
        if(s.loop.activity!=OtherMotherActivities.Activity.MAGIC_GARDEN){var old=MagicGardenActivity.center(p,s);MagicGardenActivity.clean(level,s,old);MagicGardenActivity.removeBuild(level,old);}
        if(s.loop.activity!=OtherMotherActivities.Activity.MICE_CIRCUS){var old=MiceCircusActivity.center(p,s);MiceCircusActivity.clean(level,s,old);MiceCircusActivity.removeBuild(level,old);}
        if(s.loop.activity!=OtherMotherActivities.Activity.OTHER_OCEAN_MONUMENT){var old=OceanMonumentActivity.center(p,s);OceanMonumentActivity.clean(level,s,old);OceanMonumentActivity.removeBuild(level,old);}
        switch(s.loop.activity){
            case TREASURE_HUNT,SHOPPING_MALL,SNOWBALL_BATTLE,OTHER_MINESHAFT,OTHER_NETHER_FORTRESS,OTHER_WOODLAND_MANSION->ExpeditionActivities.start(p,s,level);
            case OTHER_OCEAN_MONUMENT->OceanMonumentActivity.start(p,s,level);
            case MUSIC_FESTIVAL->MusicFestivalActivity.start(p,s,level);
            case OTHER_DESERT_TEMPLE->DesertTempleActivity.start(p,s,level);
            case OTHER_END_CITY->EndCityActivity.start(p,s,level);
            case ARCHERY_CONTEST->ArcheryActivity.start(p,s,level);
            case CAKE_MAKING_CONTEST->CakeContestActivity.start(p,s,level);
            case CARNIVAL->CarnivalActivity.start(p,s,level);
            case DREAM_PARKOUR->DreamParkourActivity.start(p,s,level);
            case FISHING_CONTEST->FishingContestActivity.start(p,s,level);
            case GARDEN_MAZE->GardenMazeActivity.start(p,s,level);
            case GRAND_FEAST->GrandFeastActivity.start(p,s,level);
            case ICE_BOAT_RACE->IceBoatRaceActivity.start(p,s,level);
            case MAGIC_SHOW->MagicShowActivity.start(p,s,level);
            case MAGIC_GARDEN->MagicGardenActivity.start(p,s,level);
            case MICE_CIRCUS->MiceCircusActivity.start(p,s,level);
            default->VillageActivity.start(p,s,level);
        }
    }
    private static void clean(ServerPlayer p,VisitData.Scene s){
        p.stopRiding();p.setForcedPose(null);
        VisitAudio.stopVoice(p,introSound(s.loop.activity));
        VisitAudio.introOffer(p,new VisitAudio.IntroOffer(introSound(s.loop.activity),s.loop.generation,0));
        if(s.loop.activity==OtherMotherActivities.Activity.MUSIC_FESTIVAL)s.loop.festival.remove("Pool");
        FestivalNetwork.clear(p);
        ActivitySoundscape.stop(p,s);
        switch(s.loop.activity){
            case TREASURE_HUNT,SHOPPING_MALL,SNOWBALL_BATTLE,OTHER_MINESHAFT,OTHER_NETHER_FORTRESS,OTHER_WOODLAND_MANSION->ExpeditionActivities.clean(p,s);
            case MUSIC_FESTIVAL->{MusicFestivalActivity.clean(p.serverLevel(),s,center(p,s));MusicFestivalActivity.removeBuild(p.serverLevel(),center(p,s));}
            case OTHER_DESERT_TEMPLE->{DesertTempleActivity.clean(p.serverLevel(),s,center(p,s));DesertTempleActivity.removeBuild(p.serverLevel(),center(p,s));}
            case OTHER_END_CITY->{EndCityActivity.restore(p,s);EndCityActivity.clean(p.serverLevel(),s,center(p,s));EndCityActivity.removeBuild(p.serverLevel(),center(p,s));}
            case ARCHERY_CONTEST->{ArcheryActivity.clean(p.serverLevel(),s,center(p,s));ArcheryActivity.removeBuild(p.serverLevel(),center(p,s));ArcheryActivity.removeIssued(p);}
            case CAKE_MAKING_CONTEST->{CakeContestActivity.clearTimer(p);CakeContestActivity.clean(p.serverLevel(),s,center(p,s));CakeContestActivity.removeBuild(p.serverLevel(),center(p,s));CakeContestActivity.removeCake(p);}
            case CARNIVAL->{CarnivalActivity.clean(p.serverLevel(),s,center(p,s));CarnivalActivity.removeBuild(p.serverLevel(),center(p,s));CarnivalActivity.removeTokens(p);}
            case DREAM_PARKOUR->{DreamParkourActivity.clean(p.serverLevel(),s,center(p,s));DreamParkourActivity.removeBuild(p.serverLevel(),center(p,s));}
            case FISHING_CONTEST->{FishingContestActivity.clean(p.serverLevel(),s,center(p,s));FishingContestActivity.removeBuild(p.serverLevel(),center(p,s));FishingContestActivity.removeIssued(p);}
            case GARDEN_MAZE->{GardenMazeActivity.clean(p.serverLevel(),s,center(p,s));GardenMazeActivity.removeBuild(p.serverLevel(),center(p,s));}
            case GRAND_FEAST->{GrandFeastActivity.clean(p.serverLevel(),s,center(p,s));GrandFeastActivity.removeBuild(p.serverLevel(),center(p,s));}
            case ICE_BOAT_RACE->{IceBoatRaceActivity.clean(p.serverLevel(),s,center(p,s));IceBoatRaceActivity.removeBuild(p.serverLevel(),center(p,s));}
            case MAGIC_SHOW->{MagicShowActivity.clean(p.serverLevel(),s,center(p,s));MagicShowActivity.removeBuild(p.serverLevel(),center(p,s));}
            case MAGIC_GARDEN->{MagicGardenActivity.stopMusic(p);MagicGardenActivity.clean(p.serverLevel(),s,center(p,s));MagicGardenActivity.removeBuild(p.serverLevel(),center(p,s));}
            case OTHER_OCEAN_MONUMENT->{OceanMonumentActivity.restoreBreathing(p,s);OceanMonumentActivity.clean(p.serverLevel(),s,center(p,s));OceanMonumentActivity.removeBuild(p.serverLevel(),center(p,s));}
            case MICE_CIRCUS->{MiceCircusActivity.refund(p,s);MiceCircusActivity.clean(p.serverLevel(),s,center(p,s));MiceCircusActivity.removeBuild(p.serverLevel(),center(p,s));}
            default->{VillageActivity.clean(p.serverLevel(),s,center(p,s));VillageActivity.removeBuild(p.serverLevel(),center(p,s));VillageActivity.removeTokens(p);}
        }
    }
    private static void prepare(ServerPlayer p,VisitData.Scene s,ServerLevel level){
        var c=center(p,s);
        if(s.loop.activity==OtherMotherActivities.Activity.OTHER_VILLAGE&&p.tickCount%20==0)ensureExit(p,s);
        if(s.loop.activity==OtherMotherActivities.Activity.ARCHERY_CONTEST)ArcheryActivity.prepare(p,s,level,c);
        else if(s.loop.activity==OtherMotherActivities.Activity.CAKE_MAKING_CONTEST)CakeContestActivity.prepare(p,s,level,c);
        else if(s.loop.activity==OtherMotherActivities.Activity.CARNIVAL)CarnivalActivity.prepare(p,s,level,c);
        else if(s.loop.activity==OtherMotherActivities.Activity.DREAM_PARKOUR)DreamParkourActivity.prepare(p,s,level,c);
        else if(s.loop.activity==OtherMotherActivities.Activity.FISHING_CONTEST)FishingContestActivity.prepare(p,s,level,c);
        else if(s.loop.activity==OtherMotherActivities.Activity.GARDEN_MAZE)GardenMazeActivity.prepare(p,s,level,c);
        else if(s.loop.activity==OtherMotherActivities.Activity.GRAND_FEAST)GrandFeastActivity.prepare(p,s,level,c);
        else if(s.loop.activity==OtherMotherActivities.Activity.ICE_BOAT_RACE)IceBoatRaceActivity.prepare(p,s,level,c);
        else if(s.loop.activity==OtherMotherActivities.Activity.MAGIC_SHOW)MagicShowActivity.prepare(p,s,level,c);
        else if(s.loop.activity==OtherMotherActivities.Activity.MAGIC_GARDEN)MagicGardenActivity.prepare(p,s,level,c);
        else if(s.loop.activity==OtherMotherActivities.Activity.OTHER_OCEAN_MONUMENT)OceanMonumentActivity.prepare(p,s,level,c);
        else if(s.loop.activity==OtherMotherActivities.Activity.MUSIC_FESTIVAL)MusicFestivalActivity.prepare(p,s,level,c);
        else if(s.loop.activity==OtherMotherActivities.Activity.OTHER_DESERT_TEMPLE)DesertTempleActivity.prepare(p,s,level,c);
        else if(s.loop.activity==OtherMotherActivities.Activity.OTHER_END_CITY)EndCityActivity.prepare(p,s,level,c);
        else if(ExpeditionActivities.supports(s.loop.activity))ExpeditionActivities.prepare(p,s,level,c);
        else if(s.loop.activity==OtherMotherActivities.Activity.MICE_CIRCUS)MiceCircusActivity.prepare(p,s,level,c);
        else {LoopProps.hold(level,c);VillageActivity.build(level,c);VillageActivity.ensureFather(p,s,level,c);VillageActivity.spawnVillagers(p,s,level,c);}
        ensureExit(p,s);
    }
    static void ensureExit(ServerPlayer p,VisitData.Scene s){
        var level=p.server.getLevel(KEY);if(level==null)return;
        var exit=center(p,s).south(s.loop.activity==OtherMotherActivities.Activity.MICE_CIRCUS?24:s.loop.activity==OtherMotherActivities.Activity.ARCHERY_CONTEST?9:s.loop.activity==OtherMotherActivities.Activity.CAKE_MAKING_CONTEST?30:s.loop.activity==OtherMotherActivities.Activity.CARNIVAL?49:s.loop.activity==OtherMotherActivities.Activity.DREAM_PARKOUR?20:s.loop.activity==OtherMotherActivities.Activity.FISHING_CONTEST?41:s.loop.activity==OtherMotherActivities.Activity.GARDEN_MAZE?-45:s.loop.activity==OtherMotherActivities.Activity.GRAND_FEAST?42:s.loop.activity==OtherMotherActivities.Activity.ICE_BOAT_RACE?51:s.loop.activity==OtherMotherActivities.Activity.MAGIC_SHOW?21:s.loop.activity==OtherMotherActivities.Activity.MAGIC_GARDEN?45:5);
        if(s.loop.activity==OtherMotherActivities.Activity.OTHER_OCEAN_MONUMENT)exit=OceanMonumentActivity.exitPos(center(p,s),s);
        if(s.loop.activity==OtherMotherActivities.Activity.MUSIC_FESTIVAL)exit=MusicFestivalActivity.exitPos(center(p,s));
        if(s.loop.activity==OtherMotherActivities.Activity.OTHER_DESERT_TEMPLE)exit=DesertTempleActivity.exitPos(center(p,s),s);
        if(s.loop.activity==OtherMotherActivities.Activity.OTHER_END_CITY)exit=EndCityActivity.exitPos(center(p,s),s);
        if(ExpeditionActivities.supports(s.loop.activity))exit=ExpeditionActivities.exitPos(center(p,s),s);
        var existing=LoopProps.nearby(level,s,exit).stream().filter(e->e.kind()==ScenePropEntity.ACTIVITY_EXIT).toList();
        if(!exitReady(s)){existing.forEach(ScenePropEntity::discard);return;}
        if(existing.isEmpty())LoopProps.spawn(level,s,ScenePropEntity.ACTIVITY_EXIT,Vec3.atBottomCenterOf(exit),-1).setGlowingTag(true);
    }
    public static void enter(ServerPlayer p,VisitData.Scene s){
        if(s.loop.phase!=OtherMotherLoopState.Phase.READY||!OtherWorld.isOther(p.level()))return;
        var level=p.server.getLevel(KEY);if(level==null){p.displayClientMessage(net.minecraft.network.chat.Component.literal("Activity dimension unavailable."),true);return;}
        // Finish her approach before the player departs so she is already waiting on return.
        if(s.mom!=null&&p.serverLevel().getEntity(s.mom) instanceof com.coraline.parents.ParentEntity mom){
            var at=OtherWorld.safePlayer(p.serverLevel(),s.loop.portal.east(2));mom.getNavigation().stop();mom.teleportTo(at.getX()+.5,at.getY(),at.getZ()+.5);s.loop.motherPos=at;
        }
        if(!ExpeditionActivities.supports(s.loop.activity)&&s.loop.activity!=OtherMotherActivities.Activity.MUSIC_FESTIVAL&&s.loop.activity!=OtherMotherActivities.Activity.OTHER_DESERT_TEMPLE&&s.loop.activity!=OtherMotherActivities.Activity.OTHER_END_CITY&&s.loop.activity!=OtherMotherActivities.Activity.ARCHERY_CONTEST&&s.loop.activity!=OtherMotherActivities.Activity.CAKE_MAKING_CONTEST&&s.loop.activity!=OtherMotherActivities.Activity.CARNIVAL&&s.loop.activity!=OtherMotherActivities.Activity.DREAM_PARKOUR&&s.loop.activity!=OtherMotherActivities.Activity.FISHING_CONTEST&&s.loop.activity!=OtherMotherActivities.Activity.GARDEN_MAZE&&s.loop.activity!=OtherMotherActivities.Activity.GRAND_FEAST&&s.loop.activity!=OtherMotherActivities.Activity.ICE_BOAT_RACE&&s.loop.activity!=OtherMotherActivities.Activity.MAGIC_SHOW&&s.loop.activity!=OtherMotherActivities.Activity.MAGIC_GARDEN&&s.loop.activity!=OtherMotherActivities.Activity.MICE_CIRCUS&&s.loop.activity!=OtherMotherActivities.Activity.OTHER_OCEAN_MONUMENT)s.loop.activity=OtherMotherActivities.Activity.OTHER_VILLAGE;
        OtherMotherLoop.phase(p,s,OtherMotherLoopState.Phase.ACTIVITY);
        var c=center(p,s);start(p,s,level);prepare(p,s,level);stabilizeExistingLeaves(level,c);
        p.stopRiding();p.setForcedPose(null);p.teleportTo(level,c.getX()+.5,c.getY(),c.getZ()+(s.loop.activity==OtherMotherActivities.Activity.MICE_CIRCUS?20.5:s.loop.activity==OtherMotherActivities.Activity.ARCHERY_CONTEST?6.5:s.loop.activity==OtherMotherActivities.Activity.CAKE_MAKING_CONTEST?25.5:s.loop.activity==OtherMotherActivities.Activity.CARNIVAL?46.5:s.loop.activity==OtherMotherActivities.Activity.DREAM_PARKOUR?16.5:s.loop.activity==OtherMotherActivities.Activity.FISHING_CONTEST?20.5:s.loop.activity==OtherMotherActivities.Activity.GARDEN_MAZE?34.5:s.loop.activity==OtherMotherActivities.Activity.GRAND_FEAST?36.5:s.loop.activity==OtherMotherActivities.Activity.ICE_BOAT_RACE?46.5:s.loop.activity==OtherMotherActivities.Activity.MAGIC_SHOW?9.5:s.loop.activity==OtherMotherActivities.Activity.MAGIC_GARDEN?43.5:.5),
            s.loop.activity==OtherMotherActivities.Activity.OTHER_VILLAGE?0:180,0);p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);
        // READY can transition to ACTIVITY only once, so the finalized introduction
        // cannot be replayed by repeatedly touching the portal in this session.
        VisitAudio.play(p,introSound(s.loop.activity),false);
    }
    /** Creative-only direct trip; it does not advance the regular visit. */
    public static String enterDebug(ServerPlayer p){return enterDebug(p,OtherMotherActivities.Activity.OTHER_VILLAGE);}
    public static String enterDebug(ServerPlayer p,OtherMotherActivities.Activity activity){
        if(!p.isCreative())return "Activity Debug is Creative-only.";
        if(isActivity(p.level()))return "You are already in the activity world.";
        var s=OtherMotherLoop.scene(p);
        if(s==null||s.stage!=VisitData.Stage.DONE)return "Finish the first visit (or use First Visit Complete Debug) first.";
        // Older releases could save ACTIVITY before intro timing failed, without ever teleporting.
        if(s.loop.active()&&p.getPersistentData().contains(DEBUG_RETURN,10)&&!protectedWorld(p.level())){
            // start() below cleans the private activity dimension. Never clean structures in the player's overworld.
            var origin=p.getPersistentData().getCompound(DEBUG_RETURN).copy();
            p.getPersistentData().remove(DEBUG_RETURN);
            if(origin.hasUUID("VisitDoor"))p.getPersistentData().putUUID("CoralineVisitDoor",origin.getUUID("VisitDoor"));
            else p.getPersistentData().remove("CoralineVisitDoor");
            OtherMotherLoop.phase(p,s,OtherMotherLoopState.Phase.NONE);
        }
        if(s.loop.active())return "Finish or leave the current Other Mother visit before using this shortcut.";
        var door=DoorData.get(p.server).doors.get(s.door);
        if(door==null||door.revoked||!door.exists||!p.getUUID().equals(door.owner))return "Your bound Little Door is missing.";
        var level=p.server.getLevel(KEY);if(level==null)return "Activity dimension unavailable.";
        VisitAudio.length(introSound(activity)); // Resolve/repair timing BEFORE committing the trip.
        RealmData.get(p.server).realm(door);
        var tag=p.getPersistentData();var origin=new CompoundTag();
        origin.putString("Dimension",p.level().dimension().location().toString());
        origin.putDouble("X",p.getX());origin.putDouble("Y",p.getY());origin.putDouble("Z",p.getZ());
        origin.putFloat("Yaw",p.getYRot());origin.putFloat("Pitch",p.getXRot());
        if(tag.hasUUID("CoralineVisitDoor"))origin.putUUID("VisitDoor",tag.getUUID("CoralineVisitDoor"));
        tag.put(DEBUG_RETURN,origin);tag.putUUID("CoralineVisitDoor",s.door);
        s.loop.generation++;s.loop.activity=activity;
        OtherMotherLoop.phase(p,s,OtherMotherLoopState.Phase.ACTIVITY);
        var c=center(p,s);start(p,s,level);prepare(p,s,level);stabilizeExistingLeaves(level,c);
        p.stopRiding();p.setForcedPose(null);p.teleportTo(level,c.getX()+.5,c.getY(),c.getZ()+(activity==OtherMotherActivities.Activity.MICE_CIRCUS?20.5:activity==OtherMotherActivities.Activity.ARCHERY_CONTEST?6.5:activity==OtherMotherActivities.Activity.CAKE_MAKING_CONTEST?25.5:activity==OtherMotherActivities.Activity.CARNIVAL?46.5:activity==OtherMotherActivities.Activity.DREAM_PARKOUR?16.5:activity==OtherMotherActivities.Activity.FISHING_CONTEST?20.5:activity==OtherMotherActivities.Activity.GARDEN_MAZE?34.5:activity==OtherMotherActivities.Activity.GRAND_FEAST?36.5:activity==OtherMotherActivities.Activity.ICE_BOAT_RACE?46.5:activity==OtherMotherActivities.Activity.MAGIC_SHOW?9.5:activity==OtherMotherActivities.Activity.MAGIC_GARDEN?43.5:.5),
            activity==OtherMotherActivities.Activity.OTHER_VILLAGE?0:180,0);
        p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);
        VisitAudio.play(p,introSound(s.loop.activity),false);
        return switch(activity){
            case ARCHERY_CONTEST->"Teleported to the Archery Contest. Fire Father's 64 arrows to reveal the return portal.";
            case CAKE_MAKING_CONTEST->"Teleported to the Cake Contest. Wait for Father, then find the middle baking stand.";
            case CARNIVAL->"Teleported to the Carnival. Wait for Father, then spend 15 tokens at the booths.";
            case DREAM_PARKOUR->"Teleported to Dream Parkour. Wait for Father, then climb for three minutes.";
            case FISHING_CONTEST->"Teleported to the Fishing Contest. Wait for Father, then fish the lake for three minutes.";
            case GARDEN_MAZE->"Teleported to the Garden Maze. Wait for Father, then reach the hidden prize garden in two minutes.";
            case GRAND_FEAST->"Teleported to the Grand Feast. Wait for Father, then grab fresh food before the villagers for three minutes.";
            case ICE_BOAT_RACE->"Teleported to the Ice Boat Race. Father will place you in your boat after his introduction.";
            case MAGIC_SHOW->"Teleported to the Magic Show. Watch the stage after Father speaks; right-click what changed.";
            case MAGIC_GARDEN->"Teleported to the Magic Garden. Father will take you flying before the harvest begins.";
            case OTHER_OCEAN_MONUMENT->"Welcome to the Other Ocean Monument. Father is beside the entrance.";
            case MICE_CIRCUS->"Welcome to the Mice Circus! Father is waiting beside you.";
            case MUSIC_FESTIVAL->"Welcome to the Music Festival. Find your four-key performance stage after Father speaks.";
            case OTHER_DESERT_TEMPLE->"Welcome to the Other Desert Temple. Father is beside the entrance.";
            case OTHER_END_CITY->"Welcome to the Other End City. Father is beside the entrance.";
            case OTHER_MINESHAFT->"Welcome to the Other Mineshaft.";
            case OTHER_NETHER_FORTRESS->"Welcome to the Other Nether Fortress.";
            case OTHER_WOODLAND_MANSION->"Welcome to the Other Woodland Mansion.";
            case TREASURE_HUNT->"Welcome to the Treasure Hunt.";
            case SHOPPING_MALL->"Welcome to the Shopping Mall.";
            case SNOWBALL_BATTLE->"Welcome to the Snowball Battle.";
            default->"Teleported to the Other Village. Trade all 20 tokens to reveal the return portal.";
        };
    }
    public static void exit(ServerPlayer p,VisitData.Scene s){
        if(!exitReady(s)){p.displayClientMessage(net.minecraft.network.chat.Component.literal(s.loop.activity==OtherMotherActivities.Activity.ARCHERY_CONTEST?
            "Fire all 64 of Father's contest arrows before returning.":s.loop.activity==OtherMotherActivities.Activity.CAKE_MAKING_CONTEST?
            "Finish the cake contest and collect your ranked prize before returning.":s.loop.activity==OtherMotherActivities.Activity.CARNIVAL?
            "Spend all 15 carnival tokens and finish your current game before returning.":s.loop.activity==OtherMotherActivities.Activity.DREAM_PARKOUR?
            "Wait until the three-minute dream climb ends before returning.":s.loop.activity==OtherMotherActivities.Activity.FISHING_CONTEST?
            "Finish the fishing contest judging and collect your ranked prize before returning.":s.loop.activity==OtherMotherActivities.Activity.GARDEN_MAZE?
            "Reach the hidden garden or wait for the two-minute timer before returning.":s.loop.activity==OtherMotherActivities.Activity.GRAND_FEAST?
            "Wait for the three-minute feast to end before returning.":s.loop.activity==OtherMotherActivities.Activity.ICE_BOAT_RACE?
            "Finish the ice boat race before returning.":s.loop.activity==OtherMotherActivities.Activity.MAGIC_SHOW?
            "Finish the Magic Show before returning.":s.loop.activity==OtherMotherActivities.Activity.MAGIC_GARDEN?
            "Wait for Father's flight and the 80-second garden harvest to finish.":s.loop.activity==OtherMotherActivities.Activity.MICE_CIRCUS?
            "Wait for the Mice Circus finale before returning.":"Spend all 20 button tokens before returning."),true);return;}
        if(p.getPersistentData().contains(DEBUG_RETURN,10)){
            clean(p,s);
            var tag=p.getPersistentData();var origin=tag.getCompound(DEBUG_RETURN);
            tag.remove(DEBUG_RETURN);OtherMotherLoop.phase(p,s,OtherMotherLoopState.Phase.NONE);
            s.loop.villageGiftAt=0;s.loop.villageIssued=false;s.loop.villageSpawned=false;s.loop.villageTokensRemaining=0;
            s.loop.archeryGiftAt=0;s.loop.archeryIssued=false;s.loop.archeryShotsRemaining=0;
            s.loop.cakeStage=0;s.loop.cakeVoiceUntil=0;s.loop.cakeTaken=false;s.loop.cakeSubmitted=false;
            s.loop.carnivalGiftAt=0;s.loop.carnivalIssued=false;s.loop.carnivalTokensRemaining=0;s.loop.carnivalBooth=-1;
            s.loop.parkourVoiceUntil=0;s.loop.parkourEndAt=0;s.loop.parkourRewarded=false;
            s.loop.fishingStage=0;s.loop.fishingVoiceUntil=0;s.loop.fishingEndAt=0;s.loop.fishingScore=0;s.loop.fishingCatches=0;
            s.loop.gardenVoiceUntil=0;s.loop.gardenEndAt=0;s.loop.gardenFinished=false;s.loop.gardenEligible=false;s.loop.gardenClaimMask=0;
            s.loop.feastVoiceUntil=0;s.loop.feastEndAt=0;s.loop.feastFinished=false;
            s.loop.iceStage=0;s.loop.iceVoiceUntil=0;s.loop.iceCountdownUntil=0;s.loop.iceResultAt=0;s.loop.iceRewarded=false;
            s.loop.magicStage=0;s.loop.magicVoiceUntil=0;s.loop.magicPhaseUntil=0;s.loop.magicRound=0;s.loop.magicTokens=0;
            s.loop.flyingGardenStage=0;s.loop.flyingGardenVoiceUntil=0;s.loop.flyingGardenPhaseUntil=0;s.loop.flyingGardenHarvestUntil=0;s.loop.flyingGardenCollectedMask=0;
            s.loop.circusVoiceUntil=0;s.loop.circusActAt=0;s.loop.circusAct=0;s.loop.circusBetState=0;s.loop.circusWager=net.minecraft.world.item.ItemStack.EMPTY;
            var home=com.coraline.parents.FamilyData.level(p.server,origin.getString("Dimension"));
            if(home==null)home=p.server.overworld();
            if(origin.hasUUID("VisitDoor")){tag.putUUID("CoralineVisitDoor",origin.getUUID("VisitDoor"));
                if(OtherWorld.isOther(home))tag.putBoolean("CoralineDebugVisitCompleted",true);
            }else tag.remove("CoralineVisitDoor");
            p.teleportTo(home,origin.getDouble("X"),origin.getDouble("Y"),origin.getDouble("Z"),origin.getFloat("Yaw"),origin.getFloat("Pitch"));
            ShoppingMallActivity.deliver(p);MineshaftActivity.deliver(p);
            p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);OtherMotherLoop.changed(p);
            p.sendSystemMessage(Component.literal("Activity debug trip complete. Returned to your starting point."));return;
        }
        var level=p.server.getLevel(OtherWorld.KEY);if(level==null){OtherMotherLoop.fail(p,"The Other World is unavailable. Returning home.");return;}
        clean(p,s);
        LoopProps.hold(level,s.loop.portal);var at=OtherWorld.safePlayer(level,s.loop.portal);
        p.teleportTo(level,at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),p.getXRot());p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);
        ShoppingMallActivity.deliver(p);MineshaftActivity.deliver(p);
        LoopProps.removePortals(p.server,s);OtherMotherLoop.phase(p,s,OtherMotherLoopState.Phase.COMPLETE);
        if(s.mom!=null&&level.getEntity(s.mom) instanceof com.coraline.parents.ParentEntity mom){var safe=OtherWorld.safePlayer(level,at.east(2));mom.teleportTo(safe.getX()+.5,safe.getY(),safe.getZ()+.5);}
    }
    static void tick(ServerPlayer p,VisitData.Scene s){
        if(s.loop.phase!=OtherMotherLoopState.Phase.ACTIVITY){OtherMotherLoop.fail(p,"Returning you home safely.");return;}
        ActivitySoundscape.tick(p,s);
        var c=center(p,s);
        if(ExpeditionActivities.supports(s.loop.activity)){
            ExpeditionActivities.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(ExpeditionActivities.exitPos(c,s)))<1.2)exit(p,s);
        }else if(s.loop.activity==OtherMotherActivities.Activity.MUSIC_FESTIVAL){
            MusicFestivalActivity.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(MusicFestivalActivity.exitPos(c)))<1.2)exit(p,s);
        }else if(s.loop.activity==OtherMotherActivities.Activity.OTHER_DESERT_TEMPLE){
            DesertTempleActivity.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(DesertTempleActivity.exitPos(c,s)))<1.2)exit(p,s);
        }else if(s.loop.activity==OtherMotherActivities.Activity.OTHER_END_CITY){
            EndCityActivity.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(EndCityActivity.exitPos(c,s)))<1.2)exit(p,s);
        }else if(s.loop.activity==OtherMotherActivities.Activity.OTHER_OCEAN_MONUMENT){
            OceanMonumentActivity.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(OceanMonumentActivity.exitPos(c,s)))<1.2)exit(p,s);
        }else if(s.loop.activity==OtherMotherActivities.Activity.ARCHERY_CONTEST){
            if(s.loop.archeryGiftAt<=0){ArcheryActivity.start(p,s,p.serverLevel());VisitAudio.play(p,introSound(s.loop.activity),false);}
            ArcheryActivity.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(c.south(9)))<1.2)exit(p,s);
        }else if(s.loop.activity==OtherMotherActivities.Activity.CAKE_MAKING_CONTEST){
            CakeContestActivity.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(c.south(30)))<1.2)exit(p,s);
        }else if(s.loop.activity==OtherMotherActivities.Activity.CARNIVAL){
            if(s.loop.carnivalGiftAt<=0){CarnivalActivity.start(p,s,p.serverLevel());VisitAudio.play(p,introSound(s.loop.activity),false);}
            CarnivalActivity.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(c.south(49)))<1.2)exit(p,s);
        }else if(s.loop.activity==OtherMotherActivities.Activity.DREAM_PARKOUR){
            if(s.loop.parkourVoiceUntil<=0){DreamParkourActivity.start(p,s,p.serverLevel());VisitAudio.play(p,introSound(s.loop.activity),false);}
            DreamParkourActivity.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(c.south(20)))<1.2)exit(p,s);
        }else if(s.loop.activity==OtherMotherActivities.Activity.FISHING_CONTEST){
            if(s.loop.fishingVoiceUntil<=0){FishingContestActivity.start(p,s,p.serverLevel());VisitAudio.play(p,introSound(s.loop.activity),false);}
            FishingContestActivity.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(c.south(41)))<1.2)exit(p,s);
        }else if(s.loop.activity==OtherMotherActivities.Activity.GARDEN_MAZE){
            if(s.loop.gardenVoiceUntil<=0){GardenMazeActivity.start(p,s,p.serverLevel());VisitAudio.play(p,introSound(s.loop.activity),false);}
            GardenMazeActivity.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(c.north(45)))<1.2)exit(p,s);
        }else if(s.loop.activity==OtherMotherActivities.Activity.GRAND_FEAST){
            if(s.loop.feastVoiceUntil<=0){GrandFeastActivity.start(p,s,p.serverLevel());VisitAudio.play(p,introSound(s.loop.activity),false);}
            GrandFeastActivity.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(c.south(42)))<1.2)exit(p,s);
        }else if(s.loop.activity==OtherMotherActivities.Activity.ICE_BOAT_RACE){
            if(s.loop.iceVoiceUntil<=0){IceBoatRaceActivity.start(p,s,p.serverLevel());VisitAudio.play(p,introSound(s.loop.activity),false);}
            IceBoatRaceActivity.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(c.south(51)))<1.2)exit(p,s);
        }else if(s.loop.activity==OtherMotherActivities.Activity.MAGIC_SHOW){
            if(s.loop.magicVoiceUntil<=0){MagicShowActivity.start(p,s,p.serverLevel());VisitAudio.play(p,introSound(s.loop.activity),false);}
            MagicShowActivity.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(c.south(21)))<1.2)exit(p,s);
        }else if(s.loop.activity==OtherMotherActivities.Activity.MAGIC_GARDEN){
            if(s.loop.flyingGardenVoiceUntil<=0){MagicGardenActivity.start(p,s,p.serverLevel());VisitAudio.play(p,introSound(s.loop.activity),false);}
            MagicGardenActivity.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(c.south(45)))<1.2)exit(p,s);
        }else if(s.loop.activity==OtherMotherActivities.Activity.MICE_CIRCUS){
            if(s.loop.circusVoiceUntil<=0){MiceCircusActivity.start(p,s,p.serverLevel());VisitAudio.play(p,introSound(s.loop.activity),false);}
            MiceCircusActivity.tick(p,s,c);
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(c.south(24)))<1.2)exit(p,s);
        }else{
            if(s.loop.villageGiftAt<=0){VillageActivity.start(p,s,p.serverLevel());VisitAudio.play(p,introSound(OtherMotherActivities.Activity.OTHER_VILLAGE),false);}
            if(p.tickCount%20==0){prepare(p,s,p.serverLevel());VillageActivity.wander(p,s,p.serverLevel(),c);}
            if(!s.loop.villageIssued&&OtherMotherLoop.now(p)>=s.loop.villageGiftAt)VillageActivity.grantTokens(p,s);
            if(p.getY()<78||Math.abs(p.getX()-(c.getX()+.5))>48||Math.abs(p.getZ()-(c.getZ()+.5))>48){p.teleportTo(p.serverLevel(),c.getX()+.5,c.getY(),c.getZ()+.5,p.getYRot(),p.getXRot());p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);}
            if(exitReady(s)&&p.distanceToSqr(Vec3.atBottomCenterOf(c.south(5)))<1.2)exit(p,s);
        }
    }
}
