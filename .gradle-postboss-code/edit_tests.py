from pathlib import Path
base=Path('src/wybietest/java/com/coraline')
p=base/'feast/PalaceJourneyGameTests.java'
s=p.read_text()
i=s.rfind('}')
s=s[:i]+'''    @GameTest(template="empty") public static void cinematicKindsAreUnique(GameTestHelper h) throws ReflectiveOperationException {
        var claimed=new java.util.HashMap<Integer,String>();
        for(var type:new Class<?>[]{CinematicPropEntity.class,RescueProps.class,PalaceTransport.class,
                AcceptedButtonEyesWorld.class,com.coraline.rebirth.StoryRebirth.class}){
            for(var field:type.getFields()){
                if(field.getType()!=int.class||!java.lang.reflect.Modifier.isStatic(field.getModifiers()))continue;
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
'''+s[i:]
p.write_text(s)
p=base/'feast/PalaceWellGameTests.java'
s=p.read_text(); i=s.rfind('}')
s=s[:i]+'''    @GameTest(template="empty") public static void mountedBicycleRefusesLowCeilingThenRidesWhenCleared(GameTestHelper h){
        var bike=com.coraline.registry.ModEntities.WYBIE_BICYCLE.get().create(h.getLevel());
        h.assertTrue(bike!=null,"Bicycle unavailable");
        var start=h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5,2,2.5));
        var goal=start.add(2,0,0);
        for(int x=0;x<=4;x++)for(int z=1;z<=3;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
        bike.setPos(start);
        var low=new BlockPos(3,4,2);
        h.setBlock(low,Blocks.STONE);
        h.assertTrue(StoryBikeRoute.toward(bike,goal)==null,"Route fit a pedestrian but lacked rider clearance");
        h.setBlock(low,Blocks.AIR);
        var route=StoryBikeRoute.toward(bike,goal);
        h.assertTrue(route!=null&&!route.isEmpty(),"Clear ground did not produce a bicycle route");
        for(var point:route)for(int tick=0;tick<4&&bike.position().distanceToSqr(point)>.02;tick++)
            h.assertTrue(bike.storyAdvance(point),"Clear route stalled during real movement");
        h.assertTrue(bike.position().distanceToSqr(goal)<.02,"Bicycle did not actually reach the destination");
        h.succeed();
    }
'''+s[i:]
p.write_text(s)
p=base/'palace/PalaceGenerationGameTests.java'
s=p.read_text();i=s.rfind('}')
s=s[:i]+'''    @GameTest(template="empty") public static void legacyPrecommitPauseReleasesClaimWithoutResettingCandidateSearch(GameTestHelper h){
        var sites=new PalaceSiteData();
        sites.initialize(42,new net.minecraft.core.BlockPos(0,70,0));
        var kind=PalaceSitePlan.Kind.PALACE;
        var original=sites.proposal(kind).orElseThrow();
        var checkpoint=sites.generation();
        checkpoint.putBoolean("PalaceClaimed",true);
        checkpoint.putInt("CandidateX",original.x());
        checkpoint.putInt("CandidateZ",original.z());
        checkpoint.putInt("ProfileBase",100);
        PalaceGenerationRules.enter(checkpoint,PalaceGenerationRules.Stage.SURVEY_REGION);
        sites.stopGeneration("Player exploration reached the pending region; job stopped to preserve explored terrain/builds. No replacement Palace allocated.");
        var reload=PalaceSiteData.load(sites.save(new CompoundTag()));
        reload.retryLegacyPause();
        h.assertTrue(reload.blocker().isEmpty()&&reload.generation().isEmpty(),"World could not continue after legacy pause");
        h.assertTrue(reload.proposal(kind).filter(original::equals).isEmpty(),"Generation returned to rejected terrain");
        h.succeed();
    }
    @GameTest(template="empty") public static void terrainCursorCoversEveryColumnInChunkOrder(GameTestHelper h){
        int side=PalaceTerrainPlan.EXTENT*2+1,minX=-119,minZ=27;
        var covered=new java.util.BitSet(side*side);
        var visitedChunks=new java.util.HashSet<Long>();
        long previous=Long.MIN_VALUE;
        for(int cursor=0;cursor<side*side;cursor++){
            var at=PalaceTerrainPlan.column(minX,minZ,cursor);
            int x=at.x()-minX,z=at.z()-minZ;
            if(x<0||z<0||x>=side||z>=side||covered.get(z*side+x))throw new AssertionError("Invalid terrain cursor "+cursor);
            covered.set(z*side+x);
            long chunk=net.minecraft.world.level.ChunkPos.asLong(at.x()>>4,at.z()>>4);
            if(chunk!=previous&&!visitedChunks.add(chunk))throw new AssertionError("Returned to an earlier chunk");
            previous=chunk;
        }
        h.assertTrue(covered.cardinality()==side*side,"Chunk order omitted terrain columns");
        h.succeed();
    }
'''+s[i:]
p.write_text(s)
