from pathlib import Path
root=Path('src/main/java/com/coraline')
def read(path): return (root/path).read_text()
def save(path,s):
 with (root/path).open('w',newline='\r\n') as f:f.write(s)
path='feast/PalaceTransport.java'
s=read(path).replace('import net.minecraft.server.level.*;', 'import net.minecraft.server.level.*;\nimport net.minecraft.server.MinecraftServer;')
s=s.replace('private static Vec3 parked(ServerPlayer p,CompoundTag n,boolean arrived){\n        var sites=PalaceSiteData.get(p.server);','''private static Vec3 parked(ServerPlayer p,CompoundTag n,boolean arrived){return parked(p.server,n,arrived);}
    private static Vec3 parked(MinecraftServer server,CompoundTag n,boolean arrived){
        var sites=PalaceSiteData.get(server);''')
s=s.replace('private static boolean nearSites(ServerPlayer p,BlockPos at){var sites=PalaceSiteData.get(p.server);','private static boolean nearSites(MinecraftServer server,BlockPos at){var sites=PalaceSiteData.get(server);')
s=s.replace('private static boolean reapCompleted(ServerPlayer requester,','static boolean reapCompleted(MinecraftServer server,')
s=s.replace('return retireActors(requester,ownerScene,n,true);','return retireActors(server,ownerScene,n,true);')
s=s.replace('private static boolean retireActors(ServerPlayer requester,','private static boolean retireActors(MinecraftServer server,')
s=s.replace('var level=requester.server.overworld();\n        var anchor=parked(requester,n,n.getBoolean("Arrived"));\n        var owner=requester.server.getPlayerList()', 'var level=server.overworld();\n        var anchor=parked(server,n,n.getBoolean("Arrived"));\n        var owner=server.getPlayerList()')
s=s.replace('find(requester,ownerScene,n,key)','find(server,ownerScene,n,key)').replace('nearSites(requester,last)','nearSites(server,last)').replace('OtherMotherLoop.changed(requester);','VisitData.get(server).setDirty();')
s=s.replace('reapCompleted(p,other,reservation)','reapCompleted(p.server,other,reservation)').replace('retireActors(p,s,n,false)','retireActors(p.server,s,n,false)')
s=s.replace('    private static CinematicPropEntity find(ServerPlayer p,VisitData.Scene s,CompoundTag n,String key){','''    private static CinematicPropEntity find(ServerPlayer p,VisitData.Scene s,CompoundTag n,String key){
        return find(p.server,s,n,key);
    }
    private static CinematicPropEntity find(MinecraftServer server,VisitData.Scene s,CompoundTag n,String key){''')
s=s.replace('if(!nearSites(p,at))return null;\n            p.server.overworld().getChunkSource().addRegionTicket(TicketType.PORTAL,new net.minecraft.world.level.ChunkPos(at),2,at);','''if(!nearSites(server,at))return null;
            var chunk=new net.minecraft.world.level.ChunkPos(at);
            server.overworld().getChunkSource().addRegionTicket(TicketType.PORTAL,chunk,2,chunk.getWorldPosition());''')
s=s.replace('var e=p.server.overworld().getEntity(n.getUUID(key));\n        int kind=', 'var e=server.overworld().getEntity(n.getUUID(key));\n        int kind=')
s=s.replace('if(state.phase()==COMPLETE){\n            reapCompleted(p,s,n);\n            return;\n        }', 'if(state.phase()==COMPLETE)return;')
s=s.replace('        var truck=ensure(p,s,n,"Truck"', '''        if(state.phase()==NOTICE&&p.distanceToSqr(desk(p).getCenter())>64){
            fail(p,s,n,"Registration paused because you left reception before pickup.");
            return;
        }
        var truck=ensure(p,s,n,"Truck"''',1)
s=s.replace('                if(p.distanceToSqr(desk(p).getCenter())>64){fail(p,s,n,"Registration paused because you left reception before pickup.");return;}\n','')
s=s.replace('                if(!movers(p,s,n,carrier.position().add(0,1,0),2,t)){fail(p,s,n,"The workers cannot follow the clear carrying route safely.");return;}', '''                boolean workersReady=movers(p,s,n,carrier.position().add(0,1,0),2,t);
                if(!workersReady&&t>=20_000){fail(p,s,n,"The workers cannot follow the carrying route. Clear their path and resume.");return;}''')
s=s.replace('else{next=true;nextPhase=THROW;}}','else if(workersReady){next=true;nextPhase=THROW;}}',1)
s=s.replace('ready=destination(p,n);if(!ready){fail(p,s,n,"Destination safety changed;', 'ready=n.getBoolean("Arrived")||destination(p,n);if(!ready){fail(p,s,n,"Destination safety changed;')
old='''                    var at=parked(p,n,true);truck.setPos(at);UNLOCKED.add(p.getUUID());try{p.stopRiding();p.connection.teleport(at.x,at.y+.88,at.z+2,p.getYRot(),p.getXRot());p.startRiding(truck,true);}finally{UNLOCKED.remove(p.getUUID());}
                    n.putBoolean("Arrived",true);truck.frame(3,0,0);
                    if(p.getVehicle()!=truck){fail(p,s,n,"Destination attachment failed; safe exit is available.");return;}'''
new='''                    var at=parked(p,n,true);
                    var before=truck.position();
                    truck.setPos(at);
                    p.connection.teleport(at.x,at.y+.88,at.z+2,p.getYRot(),p.getXRot());
                    if(p.getVehicle()!=truck&&!attached(p,truck)){
                        truck.setPos(before);
                        p.connection.teleport(before.x,before.y+.88,before.z+2,p.getYRot(),p.getXRot());
                        fail(p,s,n,"Destination attachment failed; the truck returned to its loading checkpoint.");
                        return;
                    }
                    truck.frame(3,0,0);'''
assert old in s
s=s.replace(old,new)
s=s.replace('if(state.phase().ordinal()<=TRANSIT.ordinal()&&!destination(p,n))return 0;', 'if(state.phase().ordinal()<=TRANSIT.ordinal()&&!n.getBoolean("Arrived")&&!destination(p,n))return 0;')
s=s.replace('!clearPlayer(p,vehicle.position().add(0,1,0))','!clearPlayer(p,vehicle.position().add(0,vehicle.kind()==TRUCK?.88:1,vehicle.kind()==TRUCK?2:0))')
i=s.index('    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent')
s=s[:i]+'''    @SubscribeEvent public static void cleanCompletedRides(TickEvent.ServerTickEvent event){
        var server=event.getServer();
        if(event.phase!=TickEvent.Phase.END||server.getTickCount()%20!=0||!PalaceSiteData.get(server).ready())return;
        for(var scene:VisitData.get(server).scenes.values()){
            var saved=data(scene);
            if(saved!=null)reapCompleted(server,scene,saved);
        }
    }
'''+s[i:]
save(path,s)
path='feast/PalaceCommands.java'
s=read(path).replace('Resume/leave cannot create a ride or grant a key.', 'Resume and leave act only on an existing owner-bound ride.')
s=s.replace('if(journey!=null){','''if(journey!=null){
                        var ride=journey.getCompound("Transport");
                        if(!ride.isEmpty())c.getSource().sendSuccess(()->Component.literal("Ride: "+ride.getCompound("Progress").getString("Phase")+". "+ride.getString("Blocker")),false);''')
save(path,s)
path='palace/PalaceWorldGeneration.java'
s=read(path).replace('if(chunk(l,base.getX(),base.getZ())==null||!l.areEntitiesLoaded(new ChunkPos(base).toLong()))return;', '''var signChunk=new ChunkPos(base);
        l.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL,signChunk,2,signChunk.getWorldPosition());
        if(chunk(l,base.getX(),base.getZ())==null||!l.areEntitiesLoaded(signChunk.toLong()))return;''')
save(path,s)
