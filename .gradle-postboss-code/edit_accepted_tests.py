from pathlib import Path
p=Path('src/wybietest/java/com/coraline/feast/GhostEscapeRecoveryGameTests.java')
s=p.read_text()
i=s.index('    private static CompoundTag session()')
s=s[:i]+'''    @GameTest(template="empty") public static void alreadyCrossedChildFinishesWithoutAnOverworldCopyOfTheOtherDoor(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);
        var s=OtherMotherLoop.scene(p);
        var level=p.serverLevel();
        var n=session();
        n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);
        AcceptedButtonEyes.data(s).put(GhostVictimProgress.KEY,n);
        var source=new com.coraline.otherworld.DoorData.Door();
        source.id=s.door;source.owner=s.owner;
        source.pos=h.absolutePos(new BlockPos(140,30,80));
        source.dimension=level.dimension().location().toString();
        var doors=com.coraline.otherworld.DoorData.get(p.server).doors;
        doors.put(s.door,source);
        var realm=new com.coraline.otherworld.RealmData.Realm();
        realm.door=s.door;realm.sourceDoor=source.pos;realm.doorPos=source.pos.offset(30,0,0);
        com.coraline.otherworld.RealmData.get(p.server).realms.put(s.door,realm);
        for(var at:BlockPos.betweenClosed(source.pos.offset(-5,-1,-2),source.pos.offset(4,2,2))){
            level.getChunkAt(at);
            level.setBlock(at,(at.getY()<source.pos.getY()?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
        }
        level.setBlock(source.pos,com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState()
                .setValue(com.coraline.block.TinyDoorBlock.FACING,net.minecraft.core.Direction.EAST),2);
        var door=(com.coraline.otherworld.DoorBlockEntity)level.getBlockEntity(source.pos);
        door.doorId=s.door;
        var child=com.coraline.tycoon.TycoonContent.NPC.get().create(level);
        child.owner=s.owner;child.resident=GhostVictimProgress.child(n).getUUID("Id");
        child.setPos(Vec3.atBottomCenterOf(source.pos.east(2)));
        child.setOnGround(true);child.setNoAi(true);
        var lease=new CompoundTag();lease.putUUID("Owner",s.owner);lease.putUUID("Door",s.door);lease.putUUID("Session",n.getUUID("Session"));
        child.getPersistentData().put(GhostVictimActors.LEASE,lease);
        child.getPersistentData().putUUID("ShowTravelOwner",s.owner);
        child.getPersistentData().put("GhostEscapeChild",new CompoundTag());
        n.putUUID("VictimActor",child.getUUID());
        h.assertTrue(level.addFreshEntity(child),"Could not stage the saved Overworld child");
        var e=GhostEscapeProgress.data(n);
        e.putUUID("Attempt",UUID.randomUUID());e.putUUID("Session",n.getUUID("Session"));e.putUUID("Victim",child.resident);
        GhostEscapeProgress.move(e,GhostEscapeProgress.Phase.CRAWL,0);
        try{
            h.assertTrue(GhostEscapeHunt.tick(p,s,n),"The current crossing was not handled");
            h.assertTrue(GhostEscapeProgress.phase(e)==GhostEscapeProgress.Phase.SAFE&&e.contains("HomeLanding"),
                    "Already-crossed child could not commit the room-side arrival");
            h.assertTrue(level.getEntity(child.getUUID())==child,"Crossing reconciliation replaced the child");
        }finally{
            child.discard();com.coraline.otherworld.TunnelPassage.restore(level,door);
            AcceptedBranchPlayers.remove(p,s);
        }
        h.succeed();
    }

'''+s[i:]
p.write_text(s)
p=Path('src/wybietest/java/com/coraline/feast/GhostVictimNavigationGameTests.java')
s=p.read_text();i=s.rfind('}')
s=s[:i]+'''    @GameTest(template="empty") public static void bedroomMirrorSightlineRejectsBackSideAndObstructions(GameTestHelper h){
        var level=h.getLevel();
        var at=h.absolutePos(new BlockPos(2,3,2));
        var child=com.coraline.tycoon.TycoonContent.NPC.get().create(level);
        var session=new CompoundTag();
        GhostVictimMirrors.register(session,"bedroom",at,0);
        var front=Vec3.atBottomCenterOf(at.north(2));
        var rear=Vec3.atBottomCenterOf(at.south(2));
        h.assertTrue(GhostVictimMirrors.childCanSeeMirror(session,"bedroom",child,front),"Mirror rejected valid front-side sightline");
        h.assertFalse(GhostVictimMirrors.childCanSeeMirror(session,"bedroom",child,rear),"Mirror accepted its back side");
        level.setBlock(at.north().above(),Blocks.STONE.defaultBlockState(),2);
        h.assertFalse(GhostVictimMirrors.childCanSeeMirror(session,"bedroom",child,front),"Mirror accepted a conversation through a wall");
        h.succeed();
    }
'''+s[i:]
p.write_text(s)
