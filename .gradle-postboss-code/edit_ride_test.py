from pathlib import Path
p=Path('src/wybietest/java/com/coraline/feast/PalaceJourneyGameTests.java')
s=p.read_text()
i=s.rfind('}')
s=s[:i]+'''    @GameTest(template="empty",batch="moving_company",timeoutTicks=1500)
    public static void movingCompanyMovesTheOriginalRideThroughArrivalAndLegacyCleanup(GameTestHelper h){
        var level=h.getLevel();
        var server=level.getServer();
        var sites=com.coraline.palace.PalaceSiteData.get(server);
        var origin=h.absolutePos(net.minecraft.core.BlockPos.ZERO);
        var company=new net.minecraft.core.BlockPos(origin.getX()+80,120,origin.getZ()-80);
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
        var desk=PalaceTransport.desk(new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DeskFixture")));
        level.setBlock(desk,stone,2);
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
        var actorIds=new java.util.HashMap<String,UUID>();
        h.onEachTick(()->{
            if(++elapsed[0]<25)return;
            if(!root.contains("Transport")){
                if(!root.hasUUID("MoverOffer")){
                    var hit=new net.minecraft.world.phys.BlockHitResult(desk.getCenter(),net.minecraft.core.Direction.NORTH,desk,false);
                    PalaceTransport.desk(new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,net.minecraft.world.InteractionHand.MAIN_HAND,desk,hit));
                }
                h.assertTrue(root.hasUUID("MoverOffer"),"Real desk interaction did not offer signup");
                if(PalaceTransport.register(p,root.getUUID("MoverOffer"))==0)return;
            }
            var ride=root.getCompound("Transport");
            var progress=com.coraline.palace.PalaceTransportProgress.load(s.owner,s.door,ride.getCompound("Progress"));
            var phase=progress.phase();
            phases.add(phase);
            if(phase==com.coraline.palace.PalaceTransportProgress.Phase.CARRY&&!interruption[0]){
                PalaceTransport.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
                h.assertTrue(ride.getBoolean("Paused"),"Transport logout did not preserve a paused checkpoint");
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
                h.assertTrue(actorIds.size()==6,"Ride omitted recorded transport actors");
                ride.putBoolean("Cleaned",true);
                ride.remove("VerifiedCleanup");
                p.setPos(p.position().add(40,0,0));
                h.assertTrue(PalaceTransport.reapCompleted(server,s,ride),"Legacy cleaned ride did not reconcile");
                for(var id:actorIds.values())h.assertTrue(level.getEntity(id)==null,"Original transport actor survived cleanup");
                h.assertTrue(PalaceTransport.reapCompleted(server,s,ride),"Cleanup was not idempotent");
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
            h.assertFalse(ride.getBoolean("Paused")&&!ride.getBoolean("Reconnect"),"Ride stalled in "+phase+" with "+ride.getString("Blocker"));
            h.assertTrue(elapsed[0]<1400,"Ride never finished "+phase+" at "+progress.ticks()+". "+ride.getString("Blocker"));
        });
    }
'''+s[i:]
s=s.replace('var desk=PalaceTransport.desk(new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DeskFixture")));','var desk=company.offset(-5,1,-4);')
p.write_text(s)
