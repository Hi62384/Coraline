package com.coraline.feast;

import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

/** Native fixtures are compiled by the small API check; run them only once RAM
 * is available. These are negative/serialization gates, not a Palace playthrough.
 */
@GameTestHolder("coraline")
@PrefixGameTestTemplate(false)
public final class PalaceJourneyGameTests {
    private static VisitData.Scene ready(){
        var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();
        var rescue=new CompoundTag();var end=new CompoundTag();end.putBoolean("Started",true);end.putString("Phase","COMPLETE");
        for(String key:new String[]{"PermanentlySealed","ParentsReturned","FarewellComplete","FarewellCatDeparted","HandSevered"})end.putBoolean(key,true);
        rescue.put("Continuation",end);rescue.putBoolean("DoorLocked",true);var eyes=new CompoundTag();eyes.putBoolean("Released",true);rescue.put("GhostEyes",eyes);s.loop.story.put("ParentRescue",rescue);return s;
    }
    @GameTest(template="empty") public static void palacePrerequisitesCannotBeSkipped(GameTestHelper h){
        var s=ready();h.assertTrue(PalaceJourney.prerequisites(s).ready(),"Actual completed-epilogue flags rejected");
        var end=s.loop.story.getCompound("ParentRescue").getCompound("Continuation");
        for(String key:new String[]{"Started","PermanentlySealed","ParentsReturned","FarewellComplete","FarewellCatDeparted","HandSevered"}){
            end.putBoolean(key,false);h.assertFalse(PalaceJourney.prerequisites(s).ready(),"Missing prerequisite ignored: "+key);end.putBoolean(key,true);
        }
        end.putString("Phase","FAREWELL_C");h.assertFalse(PalaceJourney.prerequisites(s).ready(),"Incomplete farewell accepted");end.putString("Phase","COMPLETE");
        s.loop.story.getCompound("ParentRescue").putBoolean("DoorLocked",false);h.assertFalse(PalaceJourney.prerequisites(s).ready(),"Unsealed door accepted");
        s=ready();s.loop.story.getCompound("ParentRescue").getCompound("GhostEyes").putBoolean("Released",false);h.assertFalse(PalaceJourney.prerequisites(s).ready(),"Unreleased eyes accepted");h.succeed();
    }
    @GameTest(template="empty") public static void absentForeignAndFutureJourneyAreNeverOverwritten(GameTestHelper h){
        var s=ready();var before=s.loop.story.copy();h.assertTrue(PalaceJourney.existing(s)==null,"Absent journey fabricated");
        h.assertTrue(s.loop.story.equals(before),"Read allocated journey state");
        var root=new CompoundTag();root.putInt("Version",2);root.putUUID("Owner",s.owner);root.putUUID("Door",s.door);root.putString("Evidence","preserve");s.loop.story.put("PinkPalaceJourney",root);
        before=s.loop.story.copy();h.assertTrue(PalaceJourney.existing(s)==null&&s.loop.story.equals(before),"Future journey schema overwritten");
        root.putInt("Version",1);root.putUUID("Owner",UUID.randomUUID());before=s.loop.story.copy();h.assertTrue(PalaceJourney.existing(s)==null&&s.loop.story.equals(before),"Foreign journey overwritten");
        root.putUUID("Owner",s.owner);h.assertTrue(PalaceJourney.existing(s)==root,"Valid owner journey not retained");
        for(String key:new String[]{"MapId","MapIssued","Company","MapRecoveryAfter","Well","KeyCustody","BundleEntity"}){
            root.putString(key,"bad schema");before=s.loop.story.copy();h.assertTrue(PalaceJourney.existing(s)==null&&s.loop.story.equals(before),"Malformed journey overwritten: "+key);root.remove(key);
        }
        root.putBoolean("MapIssued",true);h.assertTrue(PalaceJourney.existing(s)==null,"Issued map with no actual ID/company accepted");root.remove("MapIssued");h.succeed();
    }
    @GameTest(template="empty") public static void explorerMapBindingsSurviveActualItemSerialization(GameTestHelper h){
        var s=ready();var company=new net.minecraft.core.BlockPos(1800,74,2600);var map=new ItemStack(Items.FILLED_MAP);var n=map.getOrCreateTag();n.putInt("map",19);
        n.putUUID("PalaceMapOwner",s.owner);n.putUUID("PalaceMapDoor",s.door);n.putLong("PalaceMapCompany",company.asLong());
        net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(map,company,"moving_company",net.minecraft.world.level.saveddata.maps.MapDecoration.Type.RED_X);
        var loaded=ItemStack.of(map.save(new CompoundTag()));h.assertTrue(PalaceJourney.map(loaded,s,company),"Vanilla item reload lost company binding");
        h.assertTrue(MapItem.getMapId(loaded)==19&&loaded.getTag().getList("Decorations",Tag.TAG_COMPOUND).size()==1,"Map ID or company marker lost");
        h.assertFalse(PalaceJourney.map(loaded,s,company.east()),"Stale company coordinates accepted");
        loaded.getTag().putUUID("PalaceMapOwner",UUID.randomUUID());h.assertFalse(PalaceJourney.map(loaded,s,company),"Foreign map accepted");
        h.assertFalse(PalaceJourney.map(new ItemStack(Items.MAP),s,company),"Empty map accepted as explorer map");h.succeed();
    }
    @GameTest(template="empty") public static void cinematicKindsAreUnique(GameTestHelper h) throws ReflectiveOperationException {
        var claimed=new java.util.HashMap<Integer,String>();
        for(var type:new Class<?>[]{CinematicPropEntity.class,RescueProps.class,PalaceTransport.class,
                AcceptedButtonEyesWorld.class,com.coraline.rebirth.StoryRebirth.class}){
            for(var field:type.getDeclaredFields()){
                if(field.getType()!=int.class||!java.lang.reflect.Modifier.isStatic(field.getModifiers())||!java.lang.reflect.Modifier.isPublic(field.getModifiers()))continue;
                String name=type.getSimpleName()+"."+field.getName();
                String previous=claimed.putIfAbsent(field.getInt(null),name);
                h.assertTrue(previous==null,name+" conflicts with "+previous);
            }
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void legacyMoverIdentityAndTongsRemainDistinctAfterEntityReload(GameTestHelper h){
        var s=ready();
        var level=h.getLevel();
        var scenes=VisitData.get(level.getServer()).scenes;
        var root=new CompoundTag();
        root.putInt("Version",1);
        root.putUUID("Owner",s.owner);
        root.putUUID("Door",s.door);
        s.loop.story.put("PinkPalaceJourney",root);
        var ride=root.getCompound("Transport");
        ride.putInt("Version",1);
        ride.putUUID("Owner",s.owner);
        ride.putUUID("Door",s.door);
        ride.putInt("Bay",0);
        ride.putDouble("StartX",0);
        ride.putDouble("StartY",70);
        ride.putDouble("StartZ",0);
        ride.put("Progress",com.coraline.palace.PalaceTransportProgress.start(s.owner,s.door).save());
        root.put("Transport",ride);
        var old=CinematicPropEntity.TYPE.get().create(level);
        var mover=CinematicPropEntity.TYPE.get().create(level);
        var tongs=CinematicPropEntity.TYPE.get().create(level);
        h.assertTrue(old!=null&&mover!=null&&tongs!=null,"Cinematic entity type missing");
        old.configure(s,RescueProps.TONGS,0);
        old.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5,2,1.5)));
        mover.load(old.saveWithoutId(new CompoundTag()));
        ride.putUUID("Mover0",mover.getUUID());
        tongs.configure(s,RescueProps.TONGS,0);
        tongs.setPos(old.position().add(2,0,0));
        scenes.put(s.owner,s);
        try{
            h.assertTrue(level.addFreshEntity(mover)&&level.addFreshEntity(tongs),"Fixture spawn failed");
            h.assertTrue(PalaceTransport.migrateMover(mover),"Exact legacy mover UUID did not migrate");
            h.assertFalse(PalaceTransport.migrateMover(tongs),"A tongs entity was mistaken for the journaled mover");
            h.assertTrue(mover.kind()==PalaceTransport.MOVER&&mover.getBoundingBox().getYsize()>2.0&&!mover.isPickable(),"Legacy mover has incorrect dimensions or interaction");
            h.assertTrue(tongs.kind()==RescueProps.TONGS&&tongs.getBoundingBox().getYsize()<.25&&tongs.isPickable(),"Tongs have incorrect dimensions or interaction");
            var reload=CinematicPropEntity.TYPE.get().create(level);
            reload.load(mover.saveWithoutId(new CompoundTag()));
            h.assertTrue(reload.kind()==PalaceTransport.MOVER&&reload.getUUID().equals(mover.getUUID()),"Reload changed migrated mover identity");
        }finally{
            mover.discard();
            tongs.discard();
            scenes.remove(s.owner);
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void failedInventoryCheckpointLeavesTheSameKeyAndAllowsAnotherAttempt(GameTestHelper h){
        var level=h.getLevel();
        var s=ready();
        var p=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(s.owner,"PalaceKeyQA"));
        var at=h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5,2,2.5));
        p.setPos(at);
        var bundle=CinematicPropEntity.TYPE.get().create(level);
        bundle.configure(s,RescueProps.KEY_BUNDLE,0);
        bundle.setPos(at.add(0,1,1));
        var root=new CompoundTag();
        root.putInt("Version",1);
        root.putUUID("Owner",s.owner);
        root.putUUID("Door",s.door);
        root.putUUID("BundleEntity",bundle.getUUID());
        s.loop.story.put("PinkPalaceJourney",root);
        var well=new CompoundTag();
        well.putInt("Version",1);
        well.putUUID("Owner",s.owner);
        well.putUUID("Door",s.door);
        well.putUUID("Hand",UUID.randomUUID());
        well.putUUID("Session",UUID.randomUUID());
        well.putString("Phase","WRAPPED");
        well.putString("WybieLife","DEAD_BEFORE_WELL");
        well.putInt("Ticks",0);
        well.putInt("RescueTicks",0);
        well.putInt("Attempts",1);
        well.putLong("LastServerTick",-1);
        root.put("Well",well);
        var key=ParentRescueStory.boundKey(s);
        p.getInventory().setItem(p.getInventory().selected,key);
        h.assertFalse(PalaceKeyCustody.place(p,s,bundle),"Fake player unexpectedly committed player-data storage");
        h.assertTrue(root.getBoolean("KeyInventoryCheckpointPending"),"The actual inventory checkpoint was not reached");
        h.assertTrue(p.getMainHandItem()==key&&!root.contains("KeyCustody")&&root.getCompound("Well").equals(well),"Pre-journal failure changed custody");
        var id=key.getTag().getUUID("PalaceKeyIdentity");
        // Restore the legacy blocker alongside the same original item. A second
        // failed disk checkpoint must be reached and keep the original item.
        root.remove("KeyInventoryCheckpointPending");
        root.putString("KeyCustodyBlocker","Could not verify original-key inventory checkpoint; no custody committed.");
        h.assertFalse(PalaceKeyCustody.place(p,s,bundle),"Retry committed without a verified inventory checkpoint");
        h.assertTrue(root.getBoolean("KeyInventoryCheckpointPending")&&p.getMainHandItem()==key
                &&key.getTag().getUUID("PalaceKeyIdentity").equals(id),"Legacy pre-journal error prevented retry or changed the key");
        h.succeed();
    }
    @GameTest(template="empty") public static void dreamReturnRejectsObstructionAndHazard(GameTestHelper h){
        var level=h.getLevel();
        var p=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PalaceDreamQA"));
        var spot=new net.minecraft.core.BlockPos(2,2,2);
        var target=h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5,2,2.5));
        var pose=net.minecraft.world.entity.Pose.STANDING;
        h.setBlock(spot.below(),net.minecraft.world.level.block.Blocks.STONE);
        h.assertTrue(RescueEpilogue.safeReturn(level,p,pose,target),"Safe return spot rejected");
        h.setBlock(spot.above(),net.minecraft.world.level.block.Blocks.STONE);
        h.assertFalse(RescueEpilogue.safeReturn(level,p,pose,target),"Return could suffocate a player");
        h.setBlock(spot.above(),net.minecraft.world.level.block.Blocks.AIR);
        h.setBlock(spot.below(),net.minecraft.world.level.block.Blocks.MAGMA_BLOCK);
        h.assertFalse(RescueEpilogue.safeReturn(level,p,pose,target),"Return could land on magma");
        h.succeed();
    }
    public static final class RideSpawnFailures {
        private final UUID owner;
        private final java.util.Set<String> failed=new java.util.HashSet<>();
        RideSpawnFailures(UUID owner){this.owner=owner;}
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void join(net.minecraftforge.event.entity.EntityJoinLevelEvent event){
            if(!(event.getEntity() instanceof CinematicPropEntity prop)||!owner.equals(prop.owner()))return;
            int kind=prop.kind();
            if(kind!=PalaceTransport.TRUCK&&kind!=PalaceTransport.CARRIER&&kind!=PalaceTransport.MOVER)return;
            if(failed.add(kind+"/"+prop.slot()))event.setCanceled(true);
        }
    }
    @GameTest(template="empty",batch="moving_company",timeoutTicks=1500)
    public static void movingCompanyMovesTheOriginalRideThroughArrivalAndLegacyCleanup(GameTestHelper h){
        var level=h.getLevel();
        var server=level.getServer();
        var sites=com.coraline.palace.PalaceSiteData.get(server);
        var origin=h.absolutePos(net.minecraft.core.BlockPos.ZERO);
        var company=new net.minecraft.core.BlockPos(origin.getX()+80,270,origin.getZ()-80);
        var palace=company.offset(0,1,105);
        var arrival=palace.offset(-7,0,-55);
        var kinds=com.coraline.palace.PalaceSitePlan.Kind.values();
        var placements=new net.minecraft.core.BlockPos[]{palace,company,palace.south(250)};
        for(int i=0;i<kinds.length;i++)h.assertTrue(sites.adoptExisting(kinds[i],placements[i]),"Fixture could not reserve "+kinds[i]);
        var stone=net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        var forced=new java.util.HashSet<net.minecraft.world.level.ChunkPos>();
        for(var center:new net.minecraft.core.BlockPos[]{company,arrival}){
            int radius=center.equals(company)?25:10;
            for(var at:net.minecraft.core.BlockPos.betweenClosed(center.offset(-radius,center.equals(company)?0:-1,-radius),center.offset(radius,center.equals(company)?0:-1,radius))){
                var chunk=new net.minecraft.world.level.ChunkPos(at);
                if(forced.add(chunk))level.setChunkForced(chunk.x,chunk.z,true);
                level.setBlock(at,stone,2);
            }
        }
        var desk=company.offset(-5,1,-4);
        // Registration requires a real reception counter, not a plain stone block.
        level.setBlock(desk,com.coraline.registry.ModBlocks.ROSEWOOD_COUNTER.get().defaultBlockState(),2);
        // Use the actual transfer opening, retaining the exterior wall either side.
        for(int z=-9;z<=9;z++)for(int y=1;y<=4;y++)if(Math.abs(z)>4)level.setBlock(company.offset(11,y,z),stone,2);
        var s=ready();
        var p=new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(s.owner,"MovingCompanyQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var start=net.minecraft.world.phys.Vec3.atBottomCenterOf(company.offset(-4,1,-6));
        p.setPos(start);
        p.getInventory().setItem(0,ParentRescueStory.boundKey(s));
        var scenes=VisitData.get(server).scenes;
        scenes.put(s.owner,s);
        h.assertTrue(PalaceJourney.giveMap(p,s,false),"Verified company did not issue the real explorer map");
        var root=PalaceJourney.existing(s);
        var map=p.getInventory().items.stream().filter(stack->stack.is(Items.FILLED_MAP)).findFirst().orElseThrow();
        h.assertTrue(PalaceJourney.map(map,s,company)&&level.getMapData("map_"+MapItem.getMapId(map))!=null,"Explorer map marker or vanilla data missing");
        var phases=java.util.EnumSet.noneOf(com.coraline.palace.PalaceTransportProgress.Phase.class);
        int[] elapsed={0};
        boolean[] interruption={false};
        var faults=new RideSpawnFailures(s.owner);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(faults);
        var actorIds=new java.util.HashMap<String,UUID>();
        h.onEachTick(()->{
            if(++elapsed[0]<25)return;
            if(!root.contains("Transport")){
                if(!root.hasUUID("MoverOffer")||level.getGameTime()>root.getLong("MoverOfferUntil")){
                    var hit=new net.minecraft.world.phys.BlockHitResult(desk.getCenter(),net.minecraft.core.Direction.NORTH,desk,false);
                    PalaceTransport.desk(new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,net.minecraft.world.InteractionHand.MAIN_HAND,desk,hit));
                }
                h.assertTrue(root.hasUUID("MoverOffer"),"Real desk interaction did not offer signup");
                if(PalaceTransport.register(p,root.getUUID("MoverOffer"))==0){
                    h.assertTrue(elapsed[0]<400,"The Moving Company fixture never reached a valid signup with a loaded arrival bay");
                    return;
                }
            }
            var ride=root.getCompound("Transport");
            var progress=com.coraline.palace.PalaceTransportProgress.load(s.owner,s.door,ride.getCompound("Progress"));
            var phase=progress.phase();
            phases.add(phase);
            if(ride.getBoolean("Paused")&&!ride.getBoolean("Reconnect")){
                h.assertTrue(phase==com.coraline.palace.PalaceTransportProgress.Phase.NOTICE,"Unexpected pause: "+ride.getString("Blocker"));
                h.assertTrue(PalaceTransport.resume(p)==1,"Owner could not retry incomplete pickup: "+ride.getString("Blocker"));
            }
            if(phase==com.coraline.palace.PalaceTransportProgress.Phase.CARRY&&!interruption[0]){
                PalaceTransport.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
                h.assertTrue(ride.getBoolean("Paused"),"Transport logout did not preserve a paused checkpoint");
                p.stopRiding();
                interruption[0]=true;
            }
            if(phase==com.coraline.palace.PalaceTransportProgress.Phase.EXIT&&p.isPassenger()){
                p.stopRiding();
                p.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(0,-.25,0));
                h.assertFalse(p.isPassenger(),"Arrival exit was obstructed");
            }
            if(phase==com.coraline.palace.PalaceTransportProgress.Phase.COMPLETE){
                h.assertTrue(phases.size()==9&&interruption[0],"Physical ride skipped a phase or the reconnect fixture");
                h.assertTrue(p.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(arrival))<144,"Ride completed away from arrival bay");
                h.assertTrue(actorIds.size()==6&&faults.failed.size()==6,"Ride omitted a transport actor or its simulated spawn failure");
                ride.putBoolean("Cleaned",true);
                ride.remove("VerifiedCleanup");
                p.setPos(p.position().add(40,0,0));
                h.assertTrue(PalaceTransport.reapCompleted(server,s,ride),"Legacy cleaned ride did not reconcile");
                for(var id:actorIds.values())h.assertTrue(level.getEntity(id)==null,"Original transport actor survived cleanup");
                h.assertTrue(PalaceTransport.reapCompleted(server,s,ride),"Cleanup was not idempotent");
                verifyWrappedKeyDisposal(h,p,s,placements[2]);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(faults);
                scenes.remove(s.owner);
                for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);
                h.succeed();
                return;
            }
            if(p.getVehicle()!=null)p.getVehicle().positionRider(p);
            PalaceTransport.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
            for(String role:new String[]{"Truck","Carrier","Mover0","Mover1","Mover2","Mover3"})if(ride.hasUUID(role)){
                var original=actorIds.putIfAbsent(role,ride.getUUID(role));
                h.assertTrue(original==null||original.equals(ride.getUUID(role)),"Ride replaced "+role);
            }
            h.assertFalse(ride.getBoolean("Paused")&&!ride.getBoolean("Reconnect")
                    &&phase!=com.coraline.palace.PalaceTransportProgress.Phase.NOTICE,
                    "Ride stalled in "+phase+" with "+ride.getString("Blocker"));
            h.assertTrue(elapsed[0]<1400,"Ride never finished "+phase+" at "+progress.ticks()+". "+ride.getString("Blocker"));
        });
    }
    @SuppressWarnings("unchecked")
    private static java.util.List<net.minecraft.server.level.ServerPlayer> checkpointPlayers(net.minecraft.server.MinecraftServer server){
        try{
            var players=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");
            players.setAccessible(true);
            return (java.util.List<net.minecraft.server.level.ServerPlayer>)players.get(server.getPlayerList());
        }catch(ReflectiveOperationException unavailable){
            throw new IllegalStateException("GameTest save checkpoint fixture unavailable",unavailable);
        }
    }
    private static void verifyWrappedKeyDisposal(GameTestHelper h,net.minecraft.server.level.ServerPlayer p,VisitData.Scene s,net.minecraft.core.BlockPos rim){
        var level=p.serverLevel();
        var root=PalaceJourney.existing(s);
        var hand=UUID.randomUUID();
        var current=new CompoundTag();
        current.putInt("Version",1);
        current.putUUID("Owner",s.owner);
        current.putUUID("Door",s.door);
        current.putUUID("Hand",hand);
        current.putUUID("Session",UUID.randomUUID());
        current.putString("Phase","WRAPPED");
        current.putString("WybieLife","DEAD_BEFORE_WELL");
        current.putInt("Ticks",0);
        current.putInt("RescueTicks",0);
        current.putInt("Attempts",1);
        current.putLong("LastServerTick",-1);
        root.put("Well",current);
        level.getChunkAt(rim);
        var well=net.minecraft.world.phys.Vec3.atBottomCenterOf(rim);
        var bundle=CinematicPropEntity.TYPE.get().create(level);
        bundle.configure(s,RescueProps.KEY_BUNDLE,0);
        bundle.setPos(well.add(0,.2,2.5));
        h.assertTrue(level.addFreshEntity(bundle),"Wrapped bundle could not spawn");
        root.putUUID("BundleEntity",bundle.getUUID());
        p.setPos(well.add(0,0,3.5));
        var players=checkpointPlayers(p.server);
        players.add(p);
        try{
            h.assertTrue(bundle.interactAt(p,net.minecraft.world.phys.Vec3.ZERO,net.minecraft.world.InteractionHand.MAIN_HAND)
                    ==net.minecraft.world.InteractionResult.CONSUME,"Physical right-click did not place the original key in the wrapped bundle");
            var receipt=root.getCompound("KeyCustody").copy();
            h.assertTrue(receipt.getString("Stage").equals("HELD")&&p.getMainHandItem().isEmpty(),"Verified custody did not remove the original key");
            h.assertTrue(!PalaceKeyCustody.place(p,s,bundle)&&!PalaceKeyCustody.dispose(p,s,bundle),"Repeated interaction or an above-ground bundle completed disposal");
            int stage=2; // Inject a saved cursor whose entity position rolled back.
            for(int tick=0;tick<110;tick++)stage=PalaceWellMotion.throwBundle(level,bundle,well,stage);
            h.assertTrue(stage==2&&bundle.getY()<well.y-12,"The original bundle never physically descended through the well");
            h.assertTrue(PalaceKeyCustody.dispose(p,s,bundle),"Physical well throw did not commit disposal");
            var disposed=root.getCompound("KeyCustody").copy();
            h.assertTrue(disposed.getString("Stage").equals("DISPOSED")&&!PalaceKeyCustody.dispose(p,s,bundle)
                    &&root.getCompound("KeyCustody").equals(disposed),"Disposal was not exactly once");
        }finally{
            players.remove(p);
            bundle.discard();
        }
    }
    public static final class ProjectionSpawnFailure {
        private final UUID owner;
        private boolean blocked;
        ProjectionSpawnFailure(UUID owner){this.owner=owner;}
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void join(net.minecraftforge.event.entity.EntityJoinLevelEvent event){
            if(!blocked&&event.getEntity() instanceof CinematicPropEntity prop&&owner.equals(prop.owner())
                    &&prop.kind()==CinematicPropEntity.GHOST_CHILD&&prop.slot()==1){
                blocked=true;
                event.setCanceled(true);
            }
        }
    }
    @GameTest(template="empty",timeoutTicks=240)
    public static void dreamWaitsForAllThreeOriginalOrProjectedChildren(GameTestHelper h){
        var server=h.getLevel().getServer();
        var stage=server.getLevel(ActivityWorld.KEY);
        h.assertTrue(stage!=null,"Celestial scene dimension is missing");
        var s=ready();
        ParentRescueStory.data(s).putBoolean("Started",true);
        var continuation=RescueChapter.data(s);
        continuation.putString("Phase","ORBS");
        var player=new net.minecraft.server.level.ServerPlayer(server,stage,new com.mojang.authlib.GameProfile(s.owner,"DreamOrbsQA"));
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player);
        var center=ShowSiteData.get(server).center(s.owner,false).offset(150,90,100);
        var at=net.minecraft.world.phys.Vec3.atBottomCenterOf(center);
        player.setPos(at);
        continuation.putLong("VisionAt",center.asLong());
        continuation.put("VisionReturn",new CompoundTag());
        var original=CinematicPropEntity.TYPE.get().create(stage);
        original.configure(s,CinematicPropEntity.GHOST_CHILD,0);
        var originals=FirstEscapeStory.data(s);
        originals.putUUID("Ghost0",original.getUUID());
        originals.putUUID("Ghost1",UUID.randomUUID());
        originals.putUUID("Ghost2",UUID.randomUUID());
        var originalRefs=originals.copy();
        var scenes=VisitData.get(server).scenes;
        scenes.put(s.owner,s);
        var forced=new java.util.HashSet<net.minecraft.world.level.ChunkPos>();
        for(int dx=-2;dx<=2;dx+=2){
            var pos=center.offset(dx,0,4);
            var chunk=new net.minecraft.world.level.ChunkPos(pos);
            if(forced.add(chunk))stage.setChunkForced(chunk.x,chunk.z,true);
        }
        original.setPos(at.add(-2,0,4));
        h.assertTrue(stage.addFreshEntity(original),"Original ghost could not spawn");
        var failure=new ProjectionSpawnFailure(s.owner);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(failure);
        h.succeedWhen(()->{
            for(var chunk:forced)h.assertTrue(stage.areEntitiesLoaded(chunk.toLong()),"Waiting for the vision props to load");
            if(!failure.blocked){
                RescueEpilogue.tick(player,s,RescueChapter.Phase.ORBS,100,false);
                h.assertTrue(failure.blocked&&RescueChapter.phase(s)==RescueChapter.Phase.ORBS
                        &&continuation.getInt("Ticks")==0,"Dream advanced despite a refused projection");
            }
            var children=RescueEpilogue.visionActors(player,s);
            h.assertTrue(children!=null&&children.length==3,"All three ghost representations must be staged");
            h.assertTrue(children[0].getUUID().equals(original.getUUID())&&originals.equals(originalRefs)
                    &&children[1].slot()==1&&children[2].slot()==2,"Projection changed a canonical ghost identity");
            RescueEpilogue.tick(player,s,RescueChapter.Phase.ORBS,100,false);
            h.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.FAREWELL_C,"Prepared dream could not continue into its dialogue");
            for(var child:children)child.discard();
            scenes.remove(s.owner);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(failure);
            for(var chunk:forced)stage.setChunkForced(chunk.x,chunk.z,false);
        });
    }
}
