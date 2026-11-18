from pathlib import Path
p=Path('src/wybietest/java/com/coraline/feast/PalaceJourneyGameTests.java')
s=p.read_text();i=s.rfind('}')
s=s[:i]+'''    public static final class ProjectionSpawnFailure {
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
'''+s[i:]
p.write_text(s)
