from pathlib import Path
p=Path('src/wybietest/java/com/coraline/feast/PalaceJourneyGameTests.java')
s=p.read_text()
i=s.index('    @GameTest(template="empty",batch="moving_company"')
s=s[:i]+'''    public static final class RideSpawnFailures {
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
'''+s[i:]
s=s.replace('''        boolean[] interruption={false};
        var actorIds''','''        boolean[] interruption={false};
        var faults=new RideSpawnFailures(s.owner);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(faults);
        var actorIds''')
s=s.replace('''            phases.add(phase);
            if(phase''','''            phases.add(phase);
            if(ride.getBoolean("Paused")&&!ride.getBoolean("Reconnect")){
                h.assertTrue(phase==com.coraline.palace.PalaceTransportProgress.Phase.NOTICE,"Unexpected pause: "+ride.getString("Blocker"));
                h.assertTrue(PalaceTransport.resume(p)==1,"Owner could not retry incomplete pickup: "+ride.getString("Blocker"));
            }
            if(phase''')
s=s.replace('''                interruption[0]=true;
            }''','''                p.stopRiding();
                interruption[0]=true;
            }''',1)
s=s.replace('''                h.assertTrue(actorIds.size()==6,"Ride omitted recorded transport actors");''','''                h.assertTrue(actorIds.size()==6&&faults.failed.size()==6,"Ride omitted a transport actor or its simulated spawn failure");''')
s=s.replace('''                scenes.remove(s.owner);
                for(var chunk:forced)''','''                net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(faults);
                scenes.remove(s.owner);
                for(var chunk:forced)''')
s=s.replace('''            h.assertFalse(ride.getBoolean("Paused")&&!ride.getBoolean("Reconnect"),"Ride stalled in "+phase+" with "+ride.getString("Blocker"));''','''            h.assertFalse(ride.getBoolean("Paused")&&!ride.getBoolean("Reconnect")
                    &&phase!=com.coraline.palace.PalaceTransportProgress.Phase.NOTICE,
                    "Ride stalled in "+phase+" with "+ride.getString("Blocker"));''')
p.write_text(s)
