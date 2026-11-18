from pathlib import Path
p=Path('src/wybietest/java/com/coraline/feast/PalaceJourneyGameTests.java')
s=p.read_text(); i=s.rfind('}')
s=s[:i]+'''    @GameTest(template="empty") public static void failedInventoryCheckpointLeavesTheSameKeyAndAllowsAnotherAttempt(GameTestHelper h){
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
'''+s[i:]
p.write_text(s)
